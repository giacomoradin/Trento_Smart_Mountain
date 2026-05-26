import Hiker from "../models/hiker.js";

let firebaseAdmin = null;
let initAttempted = false;

async function getFirebaseAdmin() {
  if (initAttempted) return firebaseAdmin;
  initAttempted = true;

  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw?.trim()) {
    console.warn("[FCM] FIREBASE_SERVICE_ACCOUNT_JSON non configurato — push disabilitate");
    return null;
  }

  try {
    const admin = await import("firebase-admin");
    if (!admin.apps.length) {
      const cred = JSON.parse(raw);
      admin.initializeApp({ credential: admin.credential.cert(cred) });
    }
    firebaseAdmin = admin;
    return admin;
  } catch (err) {
    console.warn("[FCM] Inizializzazione fallita:", err.message);
    return null;
  }
}

async function collectFcmTokens(userIds) {
  const ids = [...new Set(userIds.map((id) => id.toString()))];
  if (!ids.length) return [];

  const hikers = await Hiker.find({ _id: { $in: ids } }).select("preferences.notifications");
  return hikers
    .map((h) => h.preferences?.notifications?.fcmToken)
    .filter((t) => typeof t === "string" && t.length > 10);
}

/**
 * Invia notifica data-only FCM (opzionale se Firebase non configurato).
 */
export async function sendPushToUsers(userIds, { title, body, data = {} }) {
  const admin = await getFirebaseAdmin();
  if (!admin) return { sent: 0, skipped: true };

  const tokens = await collectFcmTokens(userIds);
  if (!tokens.length) return { sent: 0, skipped: false };

  const message = {
    tokens,
    data: Object.fromEntries(
      Object.entries(data).map(([k, v]) => [k, v == null ? "" : String(v)]),
    ),
    notification: title ? { title, body: body || "" } : undefined,
    android: { priority: "high" },
  };

  try {
    const res = await admin.messaging().sendEachForMulticast(message);
    return { sent: res.successCount, failure: res.failureCount, skipped: false };
  } catch (err) {
    console.error("[FCM] send error:", err.message);
    return { sent: 0, error: err.message, skipped: false };
  }
}

export async function notifySessionGroupLeaders(session, { title, body, data }, excludeUserId = null) {
  const leaderIds = session.participants
    .filter((p) => p.role === "groupLeader")
    .map((p) => p.userId)
    .filter((id) => !excludeUserId || id.toString() !== excludeUserId.toString());
  return sendPushToUsers(leaderIds, { title, body, data });
}

export async function notifySessionParticipants(session, { title, body, data }, excludeUserId = null) {
  const ids = session.participants
    .map((p) => p.userId)
    .filter((id) => !excludeUserId || id.toString() !== excludeUserId.toString());
  return sendPushToUsers(ids, { title, body, data });
}
