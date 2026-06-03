import Notification from "../models/notification.js";
import HikeSession from "../models/hikeSession.js";
import RefugeBoardPost from "../models/refugeBoardPost.js";

/**
 * Servizio notifiche social in-app.
 *
 * `createNotification` è il punto d'aggancio per i trigger (follow/like/comment).
 * È deliberatamente "best-effort": non lancia mai (try/catch interno) così un
 * errore nella notifica non rompe l'azione principale che l'ha generata.
 */

/**
 * Crea una notifica. No-op (ritorna null) se:
 *   - recipient === actor (non notifichiamo le azioni su noi stessi);
 *   - manca recipient o actor.
 * Qualsiasi errore DB viene inghiottito (best-effort) e logato.
 */
export async function createNotification({
  recipientId,
  actorId,
  type,
  targetKind = null,
  targetId = null,
}) {
  if (!recipientId || !actorId) return null;
  if (String(recipientId) === String(actorId)) return null;
  try {
    return await Notification.create({
      recipientId,
      actorId,
      type,
      targetKind,
      targetId,
    });
  } catch (err) {
    // Non deve mai propagare: la notifica è accessoria all'azione.
    console.error("[notificationService] createNotification failed:", err.message);
    return null;
  }
}

/**
 * Lista paginata delle notifiche del destinatario (più recenti prima),
 * con actor populated (username + avatar) e conteggio non-letti.
 */
export async function getNotifications(userId, { page = 1, limit = 20 } = {}) {
  const skip = (Math.max(1, page) - 1) * limit;
  const [items, count, unreadCount] = await Promise.all([
    Notification.find({ recipientId: userId })
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit)
      .populate("actorId", "username personalInfo.avatarUrl")
      .lean(),
    Notification.countDocuments({ recipientId: userId }),
    Notification.countDocuments({ recipientId: userId, read: false }),
  ]);

  const mapped = items.map((n) => ({
    _id: String(n._id),
    type: n.type,
    actor: n.actorId
      ? {
        _id: String(n.actorId._id),
        username: n.actorId.username,
        personalInfo: n.actorId.personalInfo?.avatarUrl
          ? { avatarUrl: n.actorId.personalInfo.avatarUrl }
          : null,
      }
      : null,
    targetKind: n.targetKind,
    targetId: n.targetId ? String(n.targetId) : null,
    message: n.message ?? null,
    deletable: true,
    read: n.read,
    createdAt: n.createdAt,
  }));

  // Solo a pagina 1 anteponiamo le notifiche DINAMICHE (promemoria attività
  // entro ~24h + allerte rifugisti recenti): non vivono nel DB, quindi non
  // contano nel badge non-letti e non sono eliminabili singolarmente.
  const synthetic = page <= 1 ? await buildSyntheticNotifications(userId) : [];

  return {
    count: count + synthetic.length,
    unreadCount,
    hasMore: skip + items.length < count,
    items: [...synthetic, ...mapped],
  };
}

/** True se due date cadono nello stesso giorno solare. */
function isSameDay(a, b) {
  const da = new Date(a);
  const db = new Date(b);
  return (
    da.getFullYear() === db.getFullYear() &&
    da.getMonth() === db.getMonth() &&
    da.getDate() === db.getDate()
  );
}

/**
 * Notifiche "dinamiche" non persistite: promemoria per le proprie escursioni
 * imminenti (oggi/domani) e allerte recenti pubblicate dai rifugisti.
 * Best-effort: qualunque errore → lista vuota (non rompe il centro notifiche).
 */
async function buildSyntheticNotifications(userId) {
  try {
    const now = new Date();
    const startOfToday = new Date(now);
    startOfToday.setHours(0, 0, 0, 0);
    const inTwoDays = new Date(startOfToday.getTime() + 2 * 24 * 3600 * 1000);
    const since24h = new Date(now.getTime() - 24 * 3600 * 1000);

    const [upcoming, alerts] = await Promise.all([
      HikeSession.find({
        status: "PLANNED",
        meetingDate: { $gte: startOfToday, $lt: inTwoDays },
        $or: [
          { creatorId: userId },
          {
            participants: {
              $elemMatch: { userId, status: { $ne: "pending" } },
            },
          },
        ],
      })
        .select("routeDetails.name meetingDate meetingTime")
        .lean(),
      RefugeBoardPost.find({
        type: { $in: ["avviso", "pericolo"] },
        createdAt: { $gte: since24h },
      })
        .sort({ createdAt: -1 })
        .limit(10)
        .select("refugeName type title createdAt")
        .lean(),
    ]);

    const reminders = upcoming.map((s) => {
      const name = s.routeDetails?.name || "Escursione";
      const day = isSameDay(s.meetingDate, now) ? "oggi" : "domani";
      const time = s.meetingTime ? ` alle ${s.meetingTime}` : "";
      return {
        _id: `reminder:${s._id}`,
        type: "activity_reminder",
        actor: null,
        targetKind: "session",
        targetId: String(s._id),
        message: `Promemoria: "${name}" è ${day}${time}.`,
        deletable: false,
        read: true,
        createdAt: s.meetingDate,
      };
    });

    const alertItems = alerts.map((a) => ({
      _id: `alert:${a._id}`,
      type: "refuge_alert",
      actor: null,
      targetKind: null,
      targetId: null,
      message: `${a.type === "pericolo" ? "⚠️ Pericolo" : "Avviso"} · ${a.refugeName || "Rifugio"}: ${a.title}`,
      deletable: false,
      read: true,
      createdAt: a.createdAt,
    }));

    return [...alertItems, ...reminders].sort(
      (x, y) => new Date(y.createdAt) - new Date(x.createdAt),
    );
  } catch (err) {
    console.error(
      "[notificationService] buildSyntheticNotifications failed:",
      err.message,
    );
    return [];
  }
}

/**
 * Elimina una notifica del destinatario (solo le proprie). Le notifiche
 * dinamiche (id con prefisso reminder:/alert:) non esistono nel DB → no-op.
 */
export async function deleteNotification(userId, notificationId) {
  if (typeof notificationId === "string" && notificationId.includes(":")) {
    return { deleted: 0 };
  }
  const res = await Notification.deleteOne({
    _id: notificationId,
    recipientId: userId,
  });
  return { deleted: res.deletedCount ?? 0 };
}

/** Elimina TUTTE le notifiche persistite del destinatario. */
export async function deleteAllNotifications(userId) {
  const res = await Notification.deleteMany({ recipientId: userId });
  return { deleted: res.deletedCount ?? 0 };
}

/** Solo il conteggio dei non-letti (polling leggero per il badge). */
export async function getUnreadCount(userId) {
  const unreadCount = await Notification.countDocuments({
    recipientId: userId,
    read: false,
  });
  return { unreadCount };
}

/** Segna tutte le notifiche del destinatario come lette. */
export async function markAllRead(userId) {
  const res = await Notification.updateMany(
    { recipientId: userId, read: false },
    { $set: { read: true } },
  );
  return { updated: res.modifiedCount ?? 0 };
}
