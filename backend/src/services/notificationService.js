import Notification from "../models/notification.js";

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

  return {
    count,
    unreadCount,
    hasMore: skip + items.length < count,
    items: items.map((n) => ({
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
      read: n.read,
      createdAt: n.createdAt,
    })),
  };
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
