import Emergency from "../models/emergency.js";
import HikeSession from "../models/hikeSession.js";
import User from "../models/user.js";

const OPEN_STATUSES = ["ACTIVE", "SHARED_WITH_GROUP"];
const TERMINAL_STATUSES = ["DISMISSED", "CANCELLED_BY_SENDER"];

export function isSessionGroupLeader(session, userId) {
  return session.participants.some(
    (p) =>
      (p.userId?._id || p.userId).toString() === userId.toString() &&
      p.role === "groupLeader",
  );
}

export function isSessionParticipant(session, userId) {
  return session.participants.some(
    (p) => (p.userId?._id || p.userId).toString() === userId.toString(),
  );
}

async function loadActiveSession(sessionId, userId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (session.status !== "ACTIVE") throw new Error("SESSION_NOT_ACTIVE");
  if (!isSessionParticipant(session, userId)) throw new Error("FORBIDDEN");
  return session;
}

async function buildProfileSnapshot(userId) {
  const user = await User.findById(userId).select("username personalInfo experience");
  if (!user) throw new Error("USER_NOT_FOUND");
  return {
    displayName: user.username,
    personalInfo: user.personalInfo
      ? {
          sex: user.personalInfo.sex,
          birthDate: user.personalInfo.birthDate,
          heightCm: user.personalInfo.heightCm,
          weightKg: user.personalInfo.weightKg,
        }
      : undefined,
    experience: user.experience
      ? {
          caiLevel: user.experience.caiLevel,
          baselineFitness: user.experience.baselineFitness,
          weeklyTrainingFreq: user.experience.weeklyTrainingFreq,
        }
      : undefined,
  };
}

/**
 * Crea SOS (idempotente su idempotencyKey).
 */
export async function createEmergency(senderUserId, payload) {
  const {
    sessionId,
    emergencyType,
    coordinates,
    beaconInstanceId,
    idempotencyKey,
    signature = null,
    beaconActive = true,
  } = payload;

  const existing = await Emergency.findOne({ idempotencyKey });
  if (existing) {
    if (existing.senderUserId.toString() !== senderUserId.toString()) {
      throw new Error("FORBIDDEN");
    }
    const populated = await existing.populate([
      { path: "senderUserId", select: "username email" },
      { path: "sessionId", select: "routeDetails.name inviteCode status" },
    ]);
    return { emergency: populated, isNew: false };
  }

  await loadActiveSession(sessionId, senderUserId);

  const profileSnapshot = await buildProfileSnapshot(senderUserId);

  const emergency = await Emergency.create({
    sessionId,
    senderUserId,
    emergencyType,
    coordinates,
    profileSnapshot,
    beaconInstanceId: beaconInstanceId.toLowerCase(),
    idempotencyKey,
    signature: signature || null,
    beaconActive,
    status: "ACTIVE",
  });

  const populated = await emergency.populate([
    { path: "senderUserId", select: "username email" },
    { path: "sessionId", select: "routeDetails.name inviteCode status" },
  ]);

  return { emergency: populated, isNew: true };
}

export async function getEmergencyById(emergencyId, userId) {
  const emergency = await Emergency.findById(emergencyId).populate([
    { path: "senderUserId", select: "username email" },
    { path: "sessionId", select: "routeDetails.name inviteCode status participants" },
  ]);
  if (!emergency) throw new Error("EMERGENCY_NOT_FOUND");

  const session = await HikeSession.findById(emergency.sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");

  assertCanViewEmergency(emergency, session, userId);
  return emergency;
}

function assertCanViewEmergency(emergency, session, userId) {
  if (!isSessionParticipant(session, userId)) throw new Error("FORBIDDEN");

  const leader = isSessionGroupLeader(session, userId);
  if (leader) return;

  if (emergency.status === "SHARED_WITH_GROUP") return;

  if (emergency.senderUserId.toString() === userId.toString() && OPEN_STATUSES.includes(emergency.status)) {
    return;
  }

  throw new Error("FORBIDDEN");
}

/**
 * Lista emergenze aperte per sessione (capo: ACTIVE+SHARED; partecipante: SHARED only).
 */
export async function listSessionEmergencies(sessionId, userId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (!isSessionParticipant(session, userId)) throw new Error("FORBIDDEN");

  const leader = isSessionGroupLeader(session, userId);

  const filter = leader
    ? { sessionId, status: { $in: OPEN_STATUSES } }
    : {
        sessionId,
        $or: [
          { status: "SHARED_WITH_GROUP" },
          { status: "ACTIVE", senderUserId: userId },
        ],
      };

  const emergencies = await Emergency.find(filter)
    .sort({ createdAt: -1 })
    .populate([
      { path: "senderUserId", select: "username email" },
      { path: "sessionId", select: "routeDetails.name inviteCode status" },
    ]);

  return {
    emergencies,
    isGroupLeader: leader,
    hasUnacked: leader && emergencies.some((e) => !e.leaderAckAt),
  };
}

export async function patchEmergency(emergencyId, userId, action, extras = {}) {
  const emergency = await Emergency.findById(emergencyId);
  if (!emergency) throw new Error("EMERGENCY_NOT_FOUND");

  const session = await HikeSession.findById(emergency.sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");

  if (TERMINAL_STATUSES.includes(emergency.status)) {
    throw new Error("EMERGENCY_ALREADY_CLOSED");
  }

  switch (action) {
    case "cancel": {
      if (emergency.senderUserId.toString() !== userId.toString()) throw new Error("FORBIDDEN");
      emergency.status = "CANCELLED_BY_SENDER";
      emergency.cancelledAt = new Date();
      emergency.cancelledBy = userId;
      if (extras.reason) emergency.cancelReason = extras.reason;
      break;
    }
    case "dismiss": {
      if (!isSessionGroupLeader(session, userId)) throw new Error("FORBIDDEN");
      emergency.status = "DISMISSED";
      emergency.dismissedAt = new Date();
      emergency.dismissedBy = userId;
      break;
    }
    case "share_with_group": {
      if (!isSessionGroupLeader(session, userId)) throw new Error("FORBIDDEN");
      if (emergency.status !== "ACTIVE") throw new Error("INVALID_STATE_TRANSITION");
      emergency.status = "SHARED_WITH_GROUP";
      emergency.sharedAt = new Date();
      break;
    }
    case "unshare_with_group": {
      if (!isSessionGroupLeader(session, userId)) throw new Error("FORBIDDEN");
      if (emergency.status !== "SHARED_WITH_GROUP") throw new Error("INVALID_STATE_TRANSITION");
      emergency.status = "ACTIVE";
      break;
    }
    case "ack": {
      if (!isSessionGroupLeader(session, userId)) throw new Error("FORBIDDEN");
      emergency.leaderAckAt = new Date();
      break;
    }
    default:
      throw new Error("INVALID_ACTION");
  }

  await emergency.save();
  const populated = await emergency.populate([
    { path: "senderUserId", select: "username email" },
    { path: "sessionId", select: "routeDetails.name inviteCode status" },
  ]);

  return populated;
}
