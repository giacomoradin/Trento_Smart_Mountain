import Challenge from "../models/challenge.js";
import HikeSession from "../models/hikeSession.js";
import Activity from "../models/activity.js";

/**
 * Crea una nuova sfida. Il creator è inserito come partecipante "accepted"
 * di default. I `participantUserIds` (opzionali) sono invitati con status "invited".
 */
export async function createChallenge(creatorId, payload) {
  const participants = [
    {
      userId: creatorId,
      status: "accepted",
      invitedAt: new Date(),
      respondedAt: new Date(),
    },
  ];

  // Resolve degli invitati (per username o ObjectId). Per MVP accettiamo userIds raw;
  // l'integrazione con un picker by-username arriva nella sezione Social vera e propria.
  if (Array.isArray(payload.participantUserIds)) {
    for (const uid of payload.participantUserIds) {
      if (uid?.toString() === creatorId.toString()) continue; // dedup
      participants.push({ userId: uid, status: "invited" });
    }
  }

  const challenge = new Challenge({
    creatorId,
    title: payload.title,
    description: payload.description,
    metric: payload.metric,
    targetValue: payload.targetValue,
    startDate: new Date(payload.startDate),
    endDate: new Date(payload.endDate),
    participants,
    status: new Date(payload.startDate) <= new Date() ? "ACTIVE" : "PENDING",
  });

  await challenge.save();
  return challenge;
}

/** Aggiorna stato sfida in base a now (lazy state transition). */
function reconcileStatus(challenge, now = new Date()) {
  if (challenge.status === "CANCELLED" || challenge.status === "COMPLETED")
    return;
  if (challenge.status === "PENDING" && challenge.startDate <= now) {
    challenge.status = "ACTIVE";
  }
  if (challenge.status === "ACTIVE" && challenge.endDate <= now) {
    challenge.status = "COMPLETED";
    challenge.closedAt = now;
  }
}

/**
 * Computa il progresso di ogni partecipante "accepted" nella finestra [start, end].
 * Combina sessioni COMPLETED + attività libere per ogni utente.
 */
async function computeProgress(challenge) {
  const acceptedIds = challenge.participants
    .filter((p) => p.status === "accepted")
    .map((p) => p.userId);

  if (acceptedIds.length === 0) return [];

  const dateFilter = { $gte: challenge.startDate, $lt: challenge.endDate };

  const sessions = await HikeSession.find({
    "participants.userId": { $in: acceptedIds },
    status: "COMPLETED",
    endTime: dateFilter,
  })
    .select("participants actualStats gpxStats")
    .lean();

  const activities = await Activity.find({
    userId: { $in: acceptedIds },
    completedAt: dateFilter,
  })
    .select("userId actualStats")
    .lean();

  // Inizializzo accumulatore per ogni partecipante.
  const acc = {};
  for (const uid of acceptedIds) {
    acc[uid.toString()] = { km: 0, elevM: 0, count: 0, points: 0 };
  }

  for (const s of sessions) {
    const km = (s.actualStats?.distanceMeters ?? 0) / 1000.0;
    const elev =
      s.actualStats?.elevationGainM ?? s.gpxStats?.elevationGainM ?? 0;
    const pts = s.actualStats?.finalPoints ?? 0;
    // Una sessione conta per OGNI suo participant che è anche participant della challenge.
    for (const p of s.participants) {
      const key = p.userId.toString();
      if (acc[key]) {
        acc[key].km += km;
        acc[key].elevM += elev;
        acc[key].count += 1;
        acc[key].points += pts;
      }
    }
  }

  for (const a of activities) {
    const key = a.userId.toString();
    if (acc[key]) {
      acc[key].km += (a.actualStats?.distanceMeters ?? 0) / 1000.0;
      acc[key].elevM += a.actualStats?.elevationGainM ?? 0;
      acc[key].count += 1;
      acc[key].points += a.actualStats?.finalPoints ?? 0;
    }
  }

  // Estrai il valore della metrica corrispondente.
  return acceptedIds.map((uid) => {
    const v = acc[uid.toString()];
    const value =
      challenge.metric === "distance"
        ? v.km
        : challenge.metric === "elevation"
          ? v.elevM
          : challenge.metric === "count"
            ? v.count
            : v.points;
    return {
      userId: uid,
      value: Math.round(value * 10) / 10,
      reachedTarget: challenge.targetValue
        ? value >= challenge.targetValue
        : false,
    };
  });
}

/** GET singola sfida con progresso calcolato per partecipanti. */
export async function getChallengeById(challengeId, requesterId) {
  const challenge = await Challenge.findById(challengeId)
    .populate("creatorId", "username")
    .populate("participants.userId", "username");
  if (!challenge) throw new Error("CHALLENGE_NOT_FOUND");

  // Authorization: solo participants/creator possono vederla.
  const isParticipant = challenge.participants.some((p) => {
    const id = p.userId?._id ?? p.userId;
    return id?.toString() === requesterId.toString();
  });
  if (!isParticipant) throw new Error("FORBIDDEN");

  reconcileStatus(challenge);
  await challenge.save();

  const progress = await computeProgress(challenge);

  // Calcolo winner se COMPLETED e non ancora settato.
  if (
    challenge.status === "COMPLETED" &&
    !challenge.winnerId &&
    progress.length > 0
  ) {
    const best = progress.reduce((a, b) => (b.value > a.value ? b : a));
    if (best.value > 0) {
      challenge.winnerId = best.userId;
      await challenge.save();
    }
  }

  return { challenge: challenge.toObject(), progress };
}

/** GET tutte le sfide dell'utente (come creator o participant). */
export async function listMyChallenges(userId) {
  const challenges = await Challenge.find({
    $or: [{ creatorId: userId }, { "participants.userId": userId }],
  })
    .populate("creatorId", "username")
    .populate("participants.userId", "username")
    .sort({ startDate: -1 })
    .lean();

  // Light reconcile + minimal progress (solo il mio) per non sovraccaricare la list view.
  const now = new Date();
  for (const c of challenges) {
    if (c.status === "PENDING" && c.startDate <= now) c.status = "ACTIVE";
    if (c.status === "ACTIVE" && c.endDate <= now) c.status = "COMPLETED";
  }

  return challenges;
}

/** Accetta un invito. */
export async function respondToInvite(challengeId, userId, accept) {
  const challenge = await Challenge.findById(challengeId);
  if (!challenge) throw new Error("CHALLENGE_NOT_FOUND");

  const participant = challenge.participants.find(
    (p) => p.userId.toString() === userId.toString(),
  );
  if (!participant) throw new Error("NOT_INVITED");
  if (participant.status !== "invited") throw new Error("ALREADY_RESPONDED");

  participant.status = accept ? "accepted" : "declined";
  participant.respondedAt = new Date();
  await challenge.save();
  return challenge;
}

/** Cancella sfida (solo creator, solo prima dell'inizio). */
export async function cancelChallenge(challengeId, userId) {
  const challenge = await Challenge.findById(challengeId);
  if (!challenge) throw new Error("CHALLENGE_NOT_FOUND");
  if (challenge.creatorId.toString() !== userId.toString())
    throw new Error("ONLY_CREATOR_CAN_CANCEL_CHALLENGE");
  // Riconcilia lo stato PRIMA del controllo: una sfida con startDate già
  // passata ma ancora salvata come PENDING (transizione lazy non ancora
  // eseguita) è DI FATTO già in corso → non deve poter essere cancellata.
  // Senza questo passaggio si poteva cancellare una sfida attiva sfruttando lo
  // stato stale (bug). Vedi __tests__/routes/challenge.test.js.
  reconcileStatus(challenge);
  if (challenge.status === "ACTIVE" || challenge.status === "COMPLETED") {
    throw new Error("CANNOT_CANCEL_RUNNING");
  }
  challenge.status = "CANCELLED";
  await challenge.save();
  return challenge;
}
