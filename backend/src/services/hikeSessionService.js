import HikeSession from "../models/hikeSession.js";
import User from "../models/user.js";
import { getCombinedActivityStats } from "./activityService.js";
import crypto from "crypto";

// Genera codice invito nel formato "TSM-XXXX" (4 hex uppercase)
function generateInviteCode() {
  return "TSM-" + crypto.randomBytes(2).toString("hex").toUpperCase(); // es. "TSM-7A4F"
}

// Blocca solo se l'utente è in una sessione ATTIVA (tracciamento in corso).
// Più sessioni PLANNED in parallelo sono consentite: l'utente può pianificare
// più escursioni future e accettare diversi inviti, ma può essere attivo
// in una sola alla volta (vincolo OCL D2 §4: una sola sessione live per user).
async function checkUserAlreadyInActiveSession(userId) {
  const conflict = await HikeSession.findOne({
    "participants.userId": userId,
    status: "ACTIVE",
  });

  if (conflict) {
    throw new Error("USER_ALREADY_IN_SESSION");
  }
}

// Crea una nuova sessione — il creator diventa automaticamente Capogruppo
export async function createSession(creatorId, routeDetails, sessionMeta = {}) {
  await checkUserAlreadyInActiveSession(creatorId);
  let inviteCode;
  let isUnique = false;

  while (!isUnique) {
    inviteCode = generateInviteCode();
    const existing = await HikeSession.findOne({ inviteCode });
    if (!existing) isUnique = true;
  }

  const session = new HikeSession({
    creatorId,
    routeDetails,
    inviteCode,
    participants: [{ userId: creatorId, role: "groupLeader" }],
    status: "PLANNED",
    ...sessionMeta,
  });

  await session.save();

  // Diamo i ruoli di "groupLeader" a chi ha creato la sessione.
  await User.findByIdAndUpdate(creatorId, {
    $push: {
      sessionRoles: {
        groupId: session._id,
        role: "groupLeader",
        createdBy: creatorId, 
      },
    },
  });

  return session;
}

// Logica per l'ingresso in sessione tramite inviteCode
export async function joinSession(userId, inviteCode) {
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Permette a un utente di unirsi a una sessione esistente tramite codice invito.'
  */
  await checkUserAlreadyInActiveSession(userId);
  const session = await HikeSession.findOne({ inviteCode });
  if (!session) {
    throw new Error("SESSION_NOT_FOUND");
  }

  if (session.status !== "PLANNED") {
    throw new Error("SESSION_NOT_JOINABLE");
  }

  const alreadyIn = session.participants.some(
    (p) => p.userId.toString() === userId.toString()
  );
  if (alreadyIn) {
    throw new Error("ALREADY_IN_SESSION");
  }

  session.participants.push({ userId });
  await session.save();

  await User.findByIdAndUpdate(userId, {
    $push: {
      sessionRoles: {
        groupId: session._id,
        role: "hiker",
        createdBy: session.creatorId,
      },
    },
  });

<<<<<<< HEAD
  return session;
=======
  // Populate simmetrico (come getSessionById) per evitare che il client Kotlin
  // riceva ObjectId raw nei campi ref → potenziale Gson IllegalStateException.
  return session.populate([
    { path: "creatorId", select: "username email" },
    { path: "participants.userId", select: "username email" },
  ]);
}

/**
 * Statistiche aggregate delle sessioni COMPLETATE per un dato anno.
 * Usato dalla schermata "Le Mie Attività" per le card metriche e il grafico mensile.
 *
 * @returns {{
 *   year: number,
 *   totalActivities: number,
 *   totalDistanceKm: number,
 *   totalElevationGainM: number,
 *   totalPoints: number,
 *   monthlyActivityCount: number[],   // 12 elementi (Jan=0 ... Dec=11)
 *   monthlyAvgDifficulty: number[],   // 0.0–1.0 per mese (T=0.25, E=0.5, EE=0.75, EEA=1.0)
 * }}
 */
export async function getActivityStats(userId, year) {
  const sessions = await HikeSession.find({
    $or: [{ creatorId: userId }, { "participants.userId": userId }],
    status: "COMPLETED",
  }).lean();

  const diffScore = { T: 0.25, E: 0.5, EE: 0.75, EEA: 1.0 };
  const monthlyCount = new Array(12).fill(0);
  const monthlyDiffSum = new Array(12).fill(0);
  const monthlyDiffN = new Array(12).fill(0);

  let totalDist = 0;
  let totalElev = 0;
  let totalPoints = 0;
  let yearCount = 0;

  sessions.forEach((s) => {
    const ref = s.endTime || s.createdAt;
    if (!ref) return;
    const d = new Date(ref);
    if (d.getFullYear() !== year) return;
    const m = d.getMonth(); // 0-11
    monthlyCount[m]++;
    yearCount++;
    // Preferisci sempre i dati REALI registrati dal client (actualStats);
    // fallback alle stime CAI del GPX quando il client non li ha caricati.
    const actualDistKm = s.actualStats?.distanceMeters != null
      ? s.actualStats.distanceMeters / 1000.0
      : s.gpxStats?.distanceKm || 0;
    const actualElev = s.actualStats?.elevationGainM ?? s.gpxStats?.elevationGainM ?? 0;
    const actualPts = s.actualStats?.finalPoints ?? s.gpxStats?.estimatedPoints ?? 0;
    totalDist += actualDistKm;
    totalElev += actualElev;
    totalPoints += actualPts;
    const score = diffScore[s.routeDetails?.difficultyLevel] ?? 0.5;
    monthlyDiffSum[m] += score;
    monthlyDiffN[m]++;
  });

  const sessionStats = {
    year,
    totalActivities: yearCount,
    totalDistanceKm: Math.round(totalDist * 10) / 10,
    totalElevationGainM: totalElev,
    totalPoints,
    monthlyActivityCount: monthlyCount,
    monthlyAvgDifficulty: monthlyCount.map((_, i) =>
      monthlyDiffN[i] > 0 ? monthlyDiffSum[i] / monthlyDiffN[i] : 0,
    ),
  };

  // Unifica con le attività libere (Activity collection): le card "Le Mie Attività"
  // mostrano un totale che include sia le sessioni di gruppo che le escursioni personali.
  return getCombinedActivityStats(userId, year, sessionStats);
>>>>>>> 7c170be742c0ca0f16c4c6df6f5c273d643d4a7a
}

// Recupera una sessione per ID
export async function getSessionById(sessionId) {
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Recupera i dettagli completi di una sessione, inclusi i dati di partecipanti e creatore.'
  */
  return HikeSession.findById(sessionId)
    .populate("creatorId", "username email")
    .populate("participants.userId", "username email");
}

// Recupera tutte le sessioni di un utente (come creator o partecipante)
export async function getSessionsByUser(userId) {
  return HikeSession.find({
    $or: [{ creatorId: userId }, { "participants.userId": userId }],
  })
    .populate("creatorId", "username email")
    .populate("participants.userId", "username email")
    .sort({ meetingDate: 1 });
}

// Abbandona una sessione (non disponibile per il creator)
export async function leaveSession(userId, sessionId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (session.creatorId.toString() === userId.toString()) throw new Error("CREATOR_CANNOT_LEAVE");
  session.participants = session.participants.filter(
    (p) => p.userId.toString() !== userId.toString(),
  );
  await session.save();
  // Route handler invia solo { message } quindi il body non viene deserializzato come SessionResponse.
  // Restituiamo l'oggetto grezzo — il client Kotlin usa Response<ApiMessageBody> per questa route.
  return session;
}

// Aggiorna i dettagli della sessione (solo il creator, inviteCode immutabile)
export async function updateSessionDetails(sessionId, userId, updates) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (session.creatorId.toString() !== userId.toString()) throw new Error("FORBIDDEN");

  if (updates.routeDetails?.name) session.routeDetails.name = updates.routeDetails.name;
  if (updates.routeDetails?.difficultyLevel) session.routeDetails.difficultyLevel = updates.routeDetails.difficultyLevel;
  if (updates.meetingDate !== undefined) session.meetingDate = updates.meetingDate;
  if (updates.meetingTime !== undefined) session.meetingTime = updates.meetingTime;
  if (updates.meetingLocation !== undefined) session.meetingLocation = updates.meetingLocation;
  if (updates.maxParticipants !== undefined) session.maxParticipants = updates.maxParticipants;
  if (updates.minExperienceLevel !== undefined) session.minExperienceLevel = updates.minExperienceLevel;
  // inviteCode is never updated
  await session.save();
  // Popola entrambi i campi ref in modo simmetrico a getSessionById/getSessionsByUser.
  // Senza populate("participants.userId"), la risposta contiene ObjectId raw (string)
  // invece dell'oggetto User → Gson crash: "Expected BEGIN_OBJECT but was STRING".
  return session.populate([
    { path: "creatorId", select: "username email" },
    { path: "participants.userId", select: "username email" },
  ]);
}

/**
 * Marca la sessione come COMPLETED e persiste le metriche reali registrate dal client.
 *
 * Accetta il payload opzionale `actualStats` (movingSeconds, totalSeconds, distanceMeters,
 * elevationGainM, finalPoints, estimatedCalories, currentAltitudeM). Se assente, la
 * sessione viene completata con la sola transizione di stato (fallback CAI sul client).
 *
 * Autorizzato sia per il creator che per i partecipanti — un utente che si è unito
 * con codice invito deve poter chiudere il proprio tracking anche se il creator è offline.
 */
export async function completeSession(sessionId, userId, actualStats = null) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");

  const isCreator = session.creatorId.toString() === userId.toString();
  const isParticipant = session.participants.some(
    (p) => p.userId.toString() === userId.toString(),
  );
  if (!isCreator && !isParticipant) throw new Error("FORBIDDEN");

  session.status = "COMPLETED";
  session.endTime = new Date();

  if (actualStats && typeof actualStats === "object") {
    session.actualStats = {
      movingSeconds: actualStats.movingSeconds,
      totalSeconds: actualStats.totalSeconds,
      distanceMeters: actualStats.distanceMeters,
      elevationGainM: actualStats.elevationGainM,
      finalPoints: actualStats.finalPoints,
      estimatedCalories: actualStats.estimatedCalories,
      currentAltitudeM: actualStats.currentAltitudeM,
    };
  }

  await session.save();
  return session;
}

// Aggiorna lo stato della sessione (es. PLANNED → ACTIVE)
export async function updateSessionStatus(sessionId, creatorId, newStatus) {
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Aggiorna lo stato della sessione. Solo il creatore (Group Leader) può eseguire questa operazione.'
  */
  const session = await HikeSession.findById(sessionId);

  if (!session) throw new Error("SESSION_NOT_FOUND");

  if (session.creatorId.toString() !== creatorId) {
    throw new Error("FORBIDDEN");
  }

  session.status = newStatus;
  if (newStatus === "ACTIVE") session.startTime = new Date();
  if (newStatus === "COMPLETED") session.endTime = new Date();

  await session.save();
  return session;
}

// Elimina una sessione (solo il creator può farlo)
export async function deleteSession(sessionId, creatorId) {
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Elimina definitivamente una sessione. Operazione riservata al creatore.'
  */
  const session = await HikeSession.findById(sessionId);

  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (session.creatorId.toString() !== creatorId) {
    throw new Error("FORBIDDEN");
  }

  await session.deleteOne();
}