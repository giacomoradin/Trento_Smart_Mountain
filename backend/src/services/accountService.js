import bcrypt from "bcrypt";
import crypto from "crypto";
import User from "../models/user.js";
import Hiker from "../models/hiker.js";

export async function updateUser(userId, { username, email, avatarUrl }) {
  const user = await User.findById(userId);
  if (!user) throw new Error("USER_NOT_FOUND");

  if (username !== undefined) user.username = username;
  if (avatarUrl !== undefined) user.avatarUrl = avatarUrl;

  let requiresEmailVerification = false;
  if (email !== undefined && email !== user.email) {
    const exists = await User.findOne({ email, _id: { $ne: userId } }).lean();
    if (exists) throw new Error("EMAIL_TAKEN");
    user.email = email;
    user.isVerified = false;
    requiresEmailVerification = true;
    const token = crypto.randomBytes(32).toString("hex");
    user.verificationToken = token;
  }

  await user.save();

  if (requiresEmailVerification) {
    const { sendVerificationEmail } = await import("./emailService.js");
    try {
      await sendVerificationEmail(user.email, user.verificationToken);
    } catch (err) {
      console.error("[accountService] Errore invio email verifica:", err.message);
    }
  }

  return { user: user.toJSON(), requiresEmailVerification };
}

export async function changePassword(userId, { oldPassword, newPassword }) {
  const user = await User.findById(userId);
  if (!user) throw new Error("USER_NOT_FOUND");

  const valid = await bcrypt.compare(oldPassword, user.passwordHash);
  if (!valid) throw new Error("WRONG_OLD_PASSWORD");

  user.passwordHash = await bcrypt.hash(newPassword, 10);
  await user.save();
}

export async function deleteAccount(userId, { password }) {
  const user = await User.findById(userId);
  if (!user) throw new Error("USER_NOT_FOUND");

  const valid = await bcrypt.compare(password, user.passwordHash);
  if (!valid) throw new Error("WRONG_PASSWORD");

  // Leadership transfer: for active/planned sessions where this user is creator,
  // promote the next participant to groupLeader; cancel if no other participants.
  const { default: HikeSession } = await import("../models/hikeSession.js");
  const ownedSessions = await HikeSession.find({
    creatorId: userId,
    status: { $in: ["PLANNED", "ACTIVE"] },
  });
  for (const session of ownedSessions) {
    const nextLeader = session.participants.find(
      (p) => p.userId.toString() !== userId.toString(),
    );
    if (nextLeader) {
      session.creatorId = nextLeader.userId;
      nextLeader.role = "groupLeader";
      session.participants = session.participants.filter(
        (p) => p.userId.toString() !== userId.toString(),
      );
      await session.save();
    } else {
      await session.deleteOne();
    }
  }

  // Cascade: anonimizza transaction e scan, cancella il documento utente
  const { default: CreditTransaction } = await import("../models/creditTransaction.js");
  const { default: NfcScan } = await import("../models/nfcScan.js");
  const { default: QuizAttempt } = await import("../models/quizAttempt.js");
  const { default: Activity } = await import("../models/activity.js");

  await CreditTransaction.updateMany({ userId }, { $unset: { userId: 1 } });
  await NfcScan.updateMany({ userId }, { $unset: { userId: 1 } });
  await QuizAttempt.deleteMany({ userId });
  await Activity.deleteMany({ userId });
  await user.deleteOne();
}

export async function updateGoals(userId, { km, elevM, count }) {
  const update = {};
  if (km !== undefined) update["weeklyGoals.km"] = km;
  if (elevM !== undefined) update["weeklyGoals.elevM"] = elevM;
  if (count !== undefined) update["weeklyGoals.count"] = count;
  // weeklyGoals è un campo del discriminator Hiker → usa Hiker per evitare
  // che lo strict mode di User base scarti silenziosamente l'$set.
  const user = await Hiker.findByIdAndUpdate(userId, { $set: update }, { new: true }).select("weeklyGoals").lean();
  if (!user) throw new Error("USER_NOT_FOUND");
  return user.weeklyGoals;
}

// ── Stats settimanali ─────────────────────────────────────────────────────
// Aggrega km / dislivello / numero di escursioni della settimana ISO corrente
// (lun → dom). Usato dalla schermata Profilo per il widget "obiettivi vs progresso".

/** Restituisce { start, end } della settimana ISO contenente `now`. */
function getCurrentIsoWeekRange(now = new Date()) {
  const d = new Date(now);
  // getDay(): 0=dom, 1=lun, ..., 6=sab. Convertiamo a 0=lun, 6=dom.
  const dayOfWeek = (d.getDay() + 6) % 7;
  const monday = new Date(d);
  monday.setHours(0, 0, 0, 0);
  monday.setDate(d.getDate() - dayOfWeek);
  const nextMonday = new Date(monday);
  nextMonday.setDate(monday.getDate() + 7);
  return { start: monday, end: nextMonday };
}

export async function getWeeklyStats(userId) {
  // Import dinamico per evitare cicli tra accountService e hikeSession/activity.
  const { default: HikeSession } = await import("../models/hikeSession.js");
  const { default: Activity } = await import("../models/activity.js");
  const { start, end } = getCurrentIsoWeekRange();

  // Sessioni COMPLETED nella settimana (usa endTime se presente, altrimenti createdAt).
  const sessions = await HikeSession.find({
    $or: [{ creatorId: userId }, { "participants.userId": userId }],
    status: "COMPLETED",
    endTime: { $gte: start, $lt: end },
  })
    .select("actualStats gpxStats")
    .lean();

  const activities = await Activity.find({
    userId,
    completedAt: { $gte: start, $lt: end },
  })
    .select("actualStats")
    .lean();

  let km = 0;
  let elevM = 0;
  for (const s of sessions) {
    km += (s.actualStats?.distanceMeters ?? 0) / 1000.0;
    elevM += s.actualStats?.elevationGainM ?? s.gpxStats?.elevationGainM ?? 0;
  }
  for (const a of activities) {
    km += (a.actualStats?.distanceMeters ?? 0) / 1000.0;
    elevM += a.actualStats?.elevationGainM ?? 0;
  }

  return {
    weekStart: start.toISOString(),
    weekEnd: end.toISOString(),
    km: Math.round(km * 10) / 10, // 1 decimale
    elevM: Math.round(elevM),
    count: sessions.length + activities.length,
  };
}

// ── Profilo v2 ────────────────────────────────────────────────────────────
// Update parziali: solo i campi presenti nel body vengono toccati. Niente
// "delete" implicito — per resettare un campo bisogna inviare `null` esplicito
// (interpretato come unset). Tutto è scoped sotto `groupLeader` discriminator.

// Costruisce un `$set` dot-notation a partire dall'oggetto annidato del body.
// Es. { personalInfo: { sex: "M", heightCm: 180 } } → { "personalInfo.sex": "M", "personalInfo.heightCm": 180 }.
// La dot-notation evita di sovrascrivere l'intero sub-document (perderemmo i campi non inviati).
function flattenForSet(prefix, obj) {
  const set = {};
  for (const [key, value] of Object.entries(obj)) {
    if (value !== null && typeof value === "object" && !(value instanceof Date)) {
      Object.assign(set, flattenForSet(`${prefix}.${key}`, value));
    } else {
      set[`${prefix}.${key}`] = value;
    }
  }
  return set;
}

// ── Anti-cheat: campi bloccati dopo prima impostazione ──────────────────
// Alcuni campi del profilo influenzano lo scoring/baseline (anti-cheating):
//   - personalInfo.birthDate → età → fattore baseline
//   - experience.caiLevel    → fattore esperienza
// Per evitare che l'utente abbassi l'asticella prima di una sessione "facile",
// una volta impostati questi campi diventano immutabili. Il blocco è ESPOSTO
// anche dall'UI (lock 🔒 nelle screen edit) ma DEVE essere ribadito qui:
// chiunque potrebbe altrimenti aggirare l'UI con un curl/Postman.
class LockedFieldError extends Error {
  constructor(field) {
    super(`FIELD_LOCKED:${field}`);
    this.field = field;
    this.statusCode = 409;
  }
}

// IMPORTANTE: per gli update di campi del discriminator (Hiker), usiamo
// `Hiker.findByIdAndUpdate` invece di `User.findByIdAndUpdate`. Mongoose
// rispetta lo strict mode dello schema con cui esegui la query, e i campi
// personalInfo/experience/preferences sono definiti SOLO nel sub-schema
// Hiker — se interroghiamo User base, il $set viene silenziosamente droppato.
export async function updatePersonalInfo(userId, data) {
  // Anti-cheat: blocca update di birthDate se già impostato e DIVERSO dal nuovo valore.
  if (data.birthDate !== undefined) {
    const existing = await Hiker.findById(userId).select("personalInfo.birthDate").lean();
    if (existing?.personalInfo?.birthDate) {
      // Confronto ISO string (YYYY-MM-DD) per evitare falsi positivi da oggetti Date/Timestamp.
      const existingIso = existing.personalInfo.birthDate.toISOString().split("T")[0];
      const newIso = new Date(data.birthDate).toISOString().split("T")[0];
      if (existingIso !== newIso) {
        throw new LockedFieldError("birthDate");
      }
    }
  }
  const set = flattenForSet("personalInfo", data);
  const user = await Hiker.findByIdAndUpdate(userId, { $set: set }, { new: true })
    .select("personalInfo")
    .lean();
  if (!user) throw new Error("USER_NOT_FOUND");
  return user.personalInfo || {};
}

export async function updateExperience(userId, data) {
  // Anti-cheat: blocca update di caiLevel se già impostato e DIVERSO dal nuovo valore.
  if (data.caiLevel !== undefined) {
    const existing = await Hiker.findById(userId).select("experience.caiLevel").lean();
    if (existing?.experience?.caiLevel && existing.experience.caiLevel !== data.caiLevel) {
      throw new LockedFieldError("caiLevel");
    }
  }
  const set = flattenForSet("experience", data);
  const user = await Hiker.findByIdAndUpdate(userId, { $set: set }, { new: true })
    .select("experience")
    .lean();
  if (!user) throw new Error("USER_NOT_FOUND");
  return user.experience || {};
}

export async function updatePreferences(userId, data) {
  const set = flattenForSet("preferences", data);
  const user = await Hiker.findByIdAndUpdate(userId, { $set: set }, { new: true })
    .select("preferences")
    .lean();
  if (!user) throw new Error("USER_NOT_FOUND");
  return user.preferences || {};
}

/**
 * Marca l'onboarding come completato. Idempotente: chiamate successive sono
 * no-op (mantengono il timestamp originale).
 * Anche "Salta tutto" deve chiamare questo endpoint per non mostrare più il banner.
 */
export async function markProfileCompleted(userId) {
  // profileCompletedAt è del discriminator Hiker → query con Hiker per
  // garantire che il save() persista correttamente (vedi nota sopra).
  const user = await Hiker.findById(userId).select("profileCompletedAt");
  if (!user) throw new Error("USER_NOT_FOUND");
  if (!user.profileCompletedAt) {
    user.profileCompletedAt = new Date();
    await user.save();
  }
  return { profileCompletedAt: user.profileCompletedAt };
}
