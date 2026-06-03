import mongoose from "mongoose";
import HikeSession from "../models/hikeSession.js";
import User from "../models/user.js";
import { getCombinedActivityStats } from "./activityService.js";
import { addCredits } from "./creditService.js";
import { applyBaselineMultiplier } from "./userScoringService.js";
import { evaluateAllBadges } from "./badgeService.js";
import { isSessionParticipant, isSessionGroupLeader } from "./emergencyService.js";
import { createNotification } from "./notificationService.js";
import Follow from "../models/follow.js";
import {
  canViewerSeeSexInGroupContext,
  collectSessionMemberUsers,
} from "../utils/userPrivacy.js";
import crypto from "crypto";

// Populate condiviso per le risposte sessione: creator + partecipanti + chi ha
// approvato ciascun partecipante (per mostrare "accettato da X" nel client).
// Centralizzato così tutte le route restituiscono la stessa shape al client Kotlin.
const SESSION_POPULATE = [
  { path: "creatorId", select: "username email personalInfo.avatarUrl" },
  { path: "participants.userId", select: "username email personalInfo.avatarUrl" },
  { path: "participants.approvedBy", select: "username" },
];

// Genera codice invito nel formato "TSM-XXXX" (4 hex uppercase)
function generateInviteCode() {
  return "TSM-" + crypto.randomBytes(2).toString("hex").toUpperCase(); // es. "TSM-7A4F"
}

// Numero massimo di tentativi per trovare un codice unico. Con 65k combinazioni
// possibili (16^4), se non riusciamo in 20 tentativi è perché il namespace è
// quasi saturo o c'è un bug nel generatore — meglio fail-fast che loopare per sempre.
const MAX_INVITE_CODE_ATTEMPTS = 20;

// Blocca solo se l'utente è in una sessione ATTIVA (tracciamento in corso).
// Più sessioni PLANNED in parallelo sono consentite: l'utente può pianificare
// più escursioni future e accettare diversi inviti, ma può essere attivo
// in una sola alla volta (vincolo OCL D2 §4: una sola sessione live per user).
async function checkUserAlreadyInActiveSession(userId) {
  const conflict = await HikeSession.findOne({
    status: "ACTIVE",
    // Solo i partecipanti ACCETTATI bloccano: una richiesta ancora "pending" non
    // costituisce partecipazione effettiva. ($ne pending include i doc legacy
    // senza campo status, trattati come accepted.)
    participants: { $elemMatch: { userId, status: { $ne: "pending" } } },
  });

  if (conflict) {
    throw new Error("USER_ALREADY_IN_SESSION");
  }
}

// Crea una nuova sessione — il creator diventa automaticamente Capogruppo.
//
// Nota: NON imponiamo qui il check "una sola sessione ACTIVE alla volta".
// La creazione produce una sessione in stato PLANNED — è sempre lecita: gli
// utenti pianificano spesso più escursioni future in parallelo (D2 §4 vincola
// SOLO le sessioni effettivamente in tracking live, non quelle pianificate).
// Il check resta su `joinSession` e sul passaggio PLANNED → ACTIVE, dove
// l'esclusività ha senso pratico (un partecipante che tracka in due gruppi
// simultaneamente non avrebbe coordinate coerenti).
export async function createSession(creatorId, routeDetails, sessionMeta = {}) {
  let inviteCode = null;

  // Loop limitato: vedi MAX_INVITE_CODE_ATTEMPTS sopra. Se sforiamo, lanciamo
  // un errore esplicito invece di bloccare il thread Express in un while(true).
  for (let attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS; attempt++) {
    const candidate = generateInviteCode();
    const existing = await HikeSession.findOne({ inviteCode: candidate })
      .select("_id")
      .lean();
    if (!existing) {
      inviteCode = candidate;
      break;
    }
  }
  if (!inviteCode) {
    throw new Error("INVITE_CODE_GENERATION_FAILED");
  }

  const session = new HikeSession({
    creatorId,
    routeDetails,
    inviteCode,
    participants: [
      { userId: creatorId, role: "groupLeader", status: "accepted" },
    ],
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
    throw new Error("INVITE_CODE_INVALID");
  }

  if (session.status !== "PLANNED") {
    throw new Error("SESSION_NOT_JOINABLE");
  }

  // Ban locale: chi è stato rimosso definitivamente dal capogruppo non può rientrare.
  const banned = (session.removedUserIds || []).some(
    (id) => id.toString() === userId.toString(),
  );
  if (banned) {
    throw new Error("PARTICIPANT_BANNED");
  }

  const existing = session.participants.find(
    (p) => (p.userId?._id || p.userId).toString() === userId.toString(),
  );
  if (existing) {
    // Distinguo "richiesta già in attesa" da "già membro accettato": il client
    // mostra il messaggio corretto e la richiesta resta idempotente (no doppio
    // pending reinviando lo stesso codice).
    if (existing.status === "pending") throw new Error("JOIN_REQUEST_PENDING");
    throw new Error("ALREADY_IN_SESSION");
  }

  // L'ingresso crea una RICHIESTA in attesa: capogruppo o un partecipante già
  // accettato dovrà approvarla prima che l'utente diventi membro effettivo.
  session.participants.push({ userId, status: "pending" });
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

  // Notifica al capogruppo: nuova richiesta di partecipazione da approvare.
  await createNotification({
    recipientId: session.creatorId,
    actorId: userId,
    type: "join_request",
    targetKind: "session",
    targetId: session._id,
  });

  return session.populate(SESSION_POPULATE);
}

/** True se l'utente è un membro ACCETTATO della sessione (incluso il capogruppo). */
function isAcceptedMember(session, userId) {
  return (session.participants || []).some(
    (p) =>
      (p.userId?._id || p.userId).toString() === userId.toString() &&
      p.status !== "pending",
  );
}

/**
 * Approva una richiesta di partecipazione in attesa.
 * Autorizzazione: capogruppo OPPURE un partecipante già accettato (basta 1).
 * Traccia chi ha approvato in `approvedBy` ("accettato da X").
 */
export async function approveParticipant(sessionId, callerId, targetUserId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (!isAcceptedMember(session, callerId)) throw new Error("FORBIDDEN_NOT_MEMBER");
  const target = session.participants.find(
    (p) => (p.userId?._id || p.userId).toString() === targetUserId.toString(),
  );
  if (!target) throw new Error("PARTICIPANT_NOT_FOUND");
  if (target.status !== "pending") throw new Error("PARTICIPANT_NOT_PENDING");
  target.status = "accepted";
  target.approvedBy = callerId;
  await session.save();
  // Notifica all'utente: la sua richiesta è stata accettata.
  await createNotification({
    recipientId: targetUserId,
    actorId: callerId,
    type: "join_accepted",
    targetKind: "session",
    targetId: session._id,
  });
  return session.populate(SESSION_POPULATE);
}

/**
 * Rifiuta una richiesta in attesa (la rimuove). Non è un ban: l'utente potrà
 * eventualmente ri-richiedere. Autorizzazione come approve.
 */
export async function rejectParticipant(sessionId, callerId, targetUserId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (!isAcceptedMember(session, callerId)) throw new Error("FORBIDDEN_NOT_MEMBER");
  const target = session.participants.find(
    (p) => (p.userId?._id || p.userId).toString() === targetUserId.toString(),
  );
  if (!target) throw new Error("PARTICIPANT_NOT_FOUND");
  if (target.status !== "pending") throw new Error("PARTICIPANT_NOT_PENDING");
  session.participants = session.participants.filter(
    (p) => (p.userId?._id || p.userId).toString() !== targetUserId.toString(),
  );
  await session.save();
  await User.findByIdAndUpdate(targetUserId, {
    $pull: { sessionRoles: { groupId: session._id } },
  });
  return session.populate(SESSION_POPULATE);
}

/**
 * Rimuove DEFINITIVAMENTE un partecipante (accepted o pending) e lo banna da
 * QUESTA sessione (non potrà più ri-unirsi). Riservato al solo capogruppo.
 * Il creator non è rimovibile.
 */
export async function removeParticipant(sessionId, leaderId, targetUserId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (!isSessionGroupLeader(session, leaderId)) throw new Error("FORBIDDEN_NOT_LEADER");
  if (session.creatorId.toString() === targetUserId.toString()) {
    throw new Error("CANNOT_REMOVE_CREATOR");
  }
  const wasIn = session.participants.some(
    (p) => (p.userId?._id || p.userId).toString() === targetUserId.toString(),
  );
  if (!wasIn) throw new Error("PARTICIPANT_NOT_FOUND");
  session.participants = session.participants.filter(
    (p) => (p.userId?._id || p.userId).toString() !== targetUserId.toString(),
  );
  if (
    !(session.removedUserIds || []).some(
      (id) => id.toString() === targetUserId.toString(),
    )
  ) {
    session.removedUserIds.push(targetUserId);
  }
  await session.save();
  await User.findByIdAndUpdate(targetUserId, {
    $pull: { sessionRoles: { groupId: session._id } },
  });
  // Notifica all'utente: è stato rimosso dalla sessione dal capogruppo.
  await createNotification({
    recipientId: targetUserId,
    actorId: leaderId,
    type: "removed",
    targetKind: "session",
    targetId: session._id,
  });
  return session.populate(SESSION_POPULATE);
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
    const actualDistKm =
      s.actualStats?.distanceMeters != null
        ? s.actualStats.distanceMeters / 1000.0
        : s.gpxStats?.distanceKm || 0;
    const actualElev =
      s.actualStats?.elevationGainM ?? s.gpxStats?.elevationGainM ?? 0;
    const actualPts =
      s.actualStats?.finalPoints ?? s.gpxStats?.estimatedPoints ?? 0;
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
}

function getLiveTrackingEntry(session, userId) {
  const uid = userId.toString();
  return (session.liveTracking || []).find(
    (t) => t.userId?.toString?.() === uid,
  );
}

function assertInSession(session, userId) {
  const isIn = isSessionParticipant(session, userId);
  if (!isIn) throw new Error("NOT_IN_SESSION");
}

/** Username registrato come "Nome Cognome" → campi separati per la UI. */
function splitDisplayName(username) {
  if (!username || typeof username !== "string") {
    return { firstName: null, lastName: null };
  }
  const trimmed = username.trim();
  const spaceIdx = trimmed.indexOf(" ");
  if (spaceIdx === -1) {
    return { firstName: trimmed, lastName: null };
  }
  return {
    firstName: trimmed.slice(0, spaceIdx),
    lastName: trimmed.slice(spaceIdx + 1).trim() || null,
  };
}

/**
 * Upload last known live location for calling user (upsert per userId).
 */
export async function postLiveLocation(sessionId, userId, payload) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  assertInSession(session, userId);
  if (session.status !== "ACTIVE") throw new Error("SESSION_NOT_ACTIVE");

  const tracking = getLiveTrackingEntry(session, userId);
  if (tracking?.status === "SUSPENDED") {
    const err = new Error("LIVE_TRACKING_SUSPENDED");
    err.reason = tracking.reason || "OTHER";
    throw err;
  }

  const now = new Date();
  const { lat, lon, accuracyM, altitudeM, trackingStatus } = payload;
  const uid = new mongoose.Types.ObjectId(userId);
  const status = trackingStatus === "PAUSED" ? "PAUSED" : "MOVING";

  const locationSet = {
    "liveLocations.$.lat": lat,
    "liveLocations.$.lon": lon,
    "liveLocations.$.trackingStatus": status,
    "liveLocations.$.updatedAt": now,
  };
  if (accuracyM !== undefined) locationSet["liveLocations.$.accuracyM"] = accuracyM;
  if (altitudeM !== undefined) locationSet["liveLocations.$.altitudeM"] = altitudeM;

  // Atomic: update existing subdoc if present, otherwise push new.
  const updated = await HikeSession.findOneAndUpdate(
    { _id: sessionId, "liveLocations.userId": uid },
    { $set: locationSet },
    { new: true },
  );

  if (!updated) {
    await HikeSession.findByIdAndUpdate(sessionId, {
      $push: {
        liveLocations: {
          userId: uid,
          lat,
          lon,
          trackingStatus: status,
          ...(accuracyM !== undefined ? { accuracyM } : {}),
          ...(altitudeM !== undefined ? { altitudeM } : {}),
          updatedAt: now,
        },
      },
    });
  }

  return { message: "Live location aggiornata." };
}

/**
 * Fetch live locations of ACTIVE (non-suspended) participants, excluding stale.
 */
export async function getLiveLocations(sessionId, userId, { maxAgeSec = 30 } = {}) {
  const sessionDoc = await HikeSession.findById(sessionId);
  if (!sessionDoc) throw new Error("SESSION_NOT_FOUND");
  assertInSession(sessionDoc, userId);
  const viewerIsLeader = isSessionGroupLeader(sessionDoc, userId);

  const participantFields =
    "username personalInfo.avatarUrl personalInfo.sex preferences.privacy.profileVisibility";

  const session = await HikeSession.findById(sessionId)
    .populate("participants.userId", participantFields)
    .populate("creatorId", participantFields);
  if (!session) throw new Error("SESSION_NOT_FOUND");

  const memberUsers = collectSessionMemberUsers(session);
  const viewerStr = userId.toString();
  const friendsCheckIds = memberUsers
    .map((u) => (u._id || u).toString())
    .filter((uid) => {
      if (uid === viewerStr) return false;
      const vis = memberUsers.find((u) => (u._id || u).toString() === uid)
        ?.preferences?.privacy?.profileVisibility ?? "friends";
      return vis === "friends";
    });
  const followedSet = new Set(
    friendsCheckIds.length > 0
      ? (
          await Follow.find({
            followerId: viewerStr,
            followingId: { $in: friendsCheckIds },
          }).distinct("followingId")
        ).map((id) => id.toString())
      : [],
  );
  const sexVisibleFor = (targetUser) =>
    canViewerSeeSexInGroupContext(
      viewerStr,
      targetUser,
      followedSet.has((targetUser?._id || targetUser).toString()),
    );

  const cutoff = new Date(Date.now() - maxAgeSec * 1000);

  const suspendedIds = new Set(
    (session.liveTracking || [])
      .filter((t) => t.status === "SUSPENDED")
      .map((t) => t.userId.toString()),
  );

  const participantRoleById = new Map(
    (session.participants || []).map((p) => [
      (p.userId?._id || p.userId).toString(),
      p.role,
    ]),
  );

  const buildUserPayload = (uid, roleOverride) => {
    const role = roleOverride || participantRoleById.get(uid) || "hiker";
    const participant = (session.participants || []).find(
      (p) => (p.userId?._id || p.userId).toString() === uid,
    );
    const u = participant?.userId;
    const { firstName, lastName } = splitDisplayName(u?.username);
    return {
      id: uid,
      username: u?.username,
      firstName,
      lastName,
      avatarUrl: u?.personalInfo?.avatarUrl,
      role,
      ...(u && sexVisibleFor(u) && u.personalInfo?.sex
        ? { sex: u.personalInfo.sex }
        : {}),
    };
  };

  const locations = (session.liveLocations || [])
    .filter((l) => !suspendedIds.has(l.userId.toString()))
    .filter((l) => l.updatedAt && l.updatedAt >= cutoff)
    .map((l) => {
      const uid = l.userId.toString();
      return {
        user: buildUserPayload(uid),
        location: {
          lat: l.lat,
          lon: l.lon,
          ...(l.accuracyM !== undefined ? { accuracyM: l.accuracyM } : {}),
          ...(l.altitudeM !== undefined ? { altitudeM: l.altitudeM } : {}),
          trackingStatus: l.trackingStatus || "MOVING",
          updatedAt: l.updatedAt,
        },
      };
    });

  const activeIds = new Set(locations.map((l) => l.user.id));
  let excluded = [];

  if (viewerIsLeader) {
    const suspendedByUser = new Map(
      (session.liveTracking || [])
        .filter((t) => t.status === "SUSPENDED")
        .map((t) => [t.userId.toString(), t.reason || "OTHER"]),
    );

    for (const p of session.participants || []) {
      const uid = (p.userId?._id || p.userId).toString();
      if (activeIds.has(uid)) continue;

      let reason;
      if (suspendedByUser.has(uid)) {
        reason = suspendedByUser.get(uid);
      } else {
        const liveLoc = (session.liveLocations || []).find(
          (l) => l.userId.toString() === uid,
        );
        if (!liveLoc) {
          reason = "NO_SIGNAL";
        } else if (!liveLoc.updatedAt || liveLoc.updatedAt < cutoff) {
          reason = "STALE";
        } else {
          continue;
        }
      }

      excluded.push({
        user: buildUserPayload(uid, p.role),
        reason,
      });
    }
  }

  return { message: "Live locations", data: locations, excluded };
}

export async function suspendLiveTracking(sessionId, callerUserId, { userId, reason }) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  assertInSession(session, callerUserId);

  // Solo capogruppo (groupLeader)
  if (!isSessionGroupLeader(session, callerUserId)) throw new Error("ONLY_CREATOR");

  // Puoi sospendere solo partecipanti della sessione
  const targetIsParticipant = isSessionParticipant(session, userId);
  if (!targetIsParticipant) throw new Error("USER_NOT_PARTICIPANT");

  const now = new Date();
  const uid = new mongoose.Types.ObjectId(userId);

  // update existing entry if present
  const updated = await HikeSession.findOneAndUpdate(
    { _id: sessionId, "liveTracking.userId": uid },
    {
      $set: {
        "liveTracking.$.status": "SUSPENDED",
        "liveTracking.$.reason": reason,
        "liveTracking.$.updatedAt": now,
      },
    },
    { new: true },
  );

  if (!updated) {
    await HikeSession.findByIdAndUpdate(sessionId, {
      $push: {
        liveTracking: { userId: uid, status: "SUSPENDED", reason, updatedAt: now },
      },
    });
  }

  return { message: "Utente sospeso dal live tracking." };
}

export async function resumeLiveTracking(sessionId, callerUserId, { userId }) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  assertInSession(session, callerUserId);

  if (!isSessionGroupLeader(session, callerUserId)) throw new Error("ONLY_CREATOR");

  const targetIsParticipant = isSessionParticipant(session, userId);
  if (!targetIsParticipant) throw new Error("USER_NOT_PARTICIPANT");

  const now = new Date();
  const uid = new mongoose.Types.ObjectId(userId);
  const updated = await HikeSession.findOneAndUpdate(
    { _id: sessionId, "liveTracking.userId": uid },
    {
      $set: {
        "liveTracking.$.status": "ACTIVE",
        "liveTracking.$.reason": undefined,
        "liveTracking.$.updatedAt": now,
      },
    },
    { new: true },
  );

  if (!updated) {
    await HikeSession.findByIdAndUpdate(sessionId, {
      $push: { liveTracking: { userId: uid, status: "ACTIVE", updatedAt: now } },
    });
  }

  return { message: "Utente riattivato nel live tracking." };
}

// Recupera una sessione per ID
export async function getSessionById(sessionId) {
  /* 
     #swagger.tags = ['Sessions']
     #swagger.description = 'Recupera i dettagli completi di una sessione, inclusi i dati di partecipanti e creatore.'
  */
  return HikeSession.findById(sessionId).populate(SESSION_POPULATE);
}

// Recupera tutte le sessioni di un utente (come creator o partecipante)
export async function getSessionsByUser(userId) {
  return HikeSession.find({
    $or: [{ creatorId: userId }, { "participants.userId": userId }],
  })
    .populate(SESSION_POPULATE)
    .sort({ meetingDate: 1 });
}

// Abbandona una sessione (non disponibile per il creator)
export async function leaveSession(userId, sessionId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (session.creatorId.toString() === userId.toString())
    throw new Error("CREATOR_CANNOT_LEAVE");
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
  if (session.creatorId.toString() !== userId.toString())
    throw new Error("ONLY_CREATOR_CAN_UPDATE_SESSION");

  if (updates.routeDetails?.name)
    session.routeDetails.name = updates.routeDetails.name;
  if (updates.routeDetails?.difficultyLevel)
    session.routeDetails.difficultyLevel = updates.routeDetails.difficultyLevel;
  if (updates.meetingDate !== undefined)
    session.meetingDate = updates.meetingDate;
  if (updates.meetingTime !== undefined)
    session.meetingTime = updates.meetingTime;
  if (updates.meetingLocation !== undefined)
    session.meetingLocation = updates.meetingLocation;
  if (updates.maxParticipants !== undefined)
    session.maxParticipants = updates.maxParticipants;
  if (updates.minExperienceLevel !== undefined)
    session.minExperienceLevel = updates.minExperienceLevel;
  // inviteCode is never updated
  await session.save();
  // Popola i ref in modo simmetrico a getSessionById/getSessionsByUser.
  // Senza populate("participants.userId"), la risposta contiene ObjectId raw (string)
  // invece dell'oggetto User → Gson crash: "Expected BEGIN_OBJECT but was STRING".
  return session.populate(SESSION_POPULATE);
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
  if (!isCreator && !isParticipant)
    throw new Error("ONLY_CREATOR_CAN_COMPLETE_SESSION");

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

  // Accredito crediti per-utente con idempotency atomic: ogni partecipante riceve
  // i propri crediti UNA volta sola, indipendentemente da quante volte chiama /complete.
  // Il $ne + $push in un solo round-trip impedisce race condition (doppio tap).
  const basePoints = session.actualStats?.finalPoints ?? 0;
  if (basePoints > 0) {
    // Lookup PER-UTENTE del profilo: ogni partecipante può avere baseline diverso
    // (es. capogruppo atleta, partecipante sedentario → boost diversi per la stessa sessione).
    const user = await User.findById(userId).select("experience").lean();
    const credits = applyBaselineMultiplier(
      basePoints,
      user,
      session.routeDetails?.difficultyLevel,
    );

    const claimed = await HikeSession.findOneAndUpdate(
      { _id: sessionId, creditsAwardedTo: { $ne: userId } },
      {
        $addToSet: { creditsAwardedTo: userId },
        $setOnInsert: {},
        ...(session.creditsAwardedAt
          ? {}
          : { $set: { creditsAwardedAt: new Date() } }),
      },
      { new: true },
    );
    if (claimed) {
      await addCredits({
        userId,
        amount: credits,
        source: "session",
        refId: session._id,
        refKind: "HikeSession",
        // Note diagnostica: se in futuro vediamo crediti diversi tra utenti per
        // la stessa sessione, il log conferma che è atteso (baseline differenti).
        note:
          credits !== basePoints
            ? `baseline μ applicato (base=${basePoints}, final=${credits})`
            : undefined,
      });
    }
  }

  // Badge evaluation post-completion: completare la sessione può sbloccare
  // first_steps / veteran / credit_*. Fire-and-forget — un errore qui non
  // deve bloccare la response del complete.
  evaluateAllBadges(userId).catch((err) => {
    console.error("[hikeSessionService] badge eval fallita:", err.message);
  });

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
    throw new Error("ONLY_CREATOR_CAN_UPDATE_SESSION");
  }

  // L'esclusività "una sola sessione ACTIVE" si applica QUI (transizione
  // PLANNED → ACTIVE), non in createSession: pianificare più escursioni in
  // parallelo è lecito; tracciare due gruppi simultaneamente no.
  if (newStatus === "ACTIVE" && session.status !== "ACTIVE") {
    const conflict = await HikeSession.findOne({
      _id: { $ne: session._id },
      status: "ACTIVE",
      participants: { $elemMatch: { userId: creatorId, status: { $ne: "pending" } } },
    });
    if (conflict) throw new Error("USER_ALREADY_IN_SESSION");
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
    throw new Error("ONLY_CREATOR_CAN_DELETE_SESSION");
  }

  await session.deleteOne();
}
