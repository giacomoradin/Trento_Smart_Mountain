import Comment from "../models/comment.js";
import Activity from "../models/activity.js";
import HikeSession from "../models/hikeSession.js";
import { createNotification } from "./notificationService.js";

/**
 * Servizio commenti su Activity/HikeSession.
 *
 * Convenzione errori (mappati dal global error mapper):
 *   - ACTIVITY_NOT_FOUND / SESSION_NOT_FOUND  → 404
 *   - NOT_SHARED                              → 403 (commenti su target privato
 *                                                  da non-owner: anti-enum)
 *   - COMMENT_NOT_FOUND                       → 404
 *   - FORBIDDEN_NOT_AUTHOR                    → 403 (delete di commento altrui
 *                                                  non admin)
 *
 * Authorization "chi può commentare":
 *   - Activity: chiunque se sharedAt != null, altrimenti solo l'owner
 *   - HikeSession: chiunque se sharedAt != null, altrimenti solo creator
 *     o partecipante (chi è "nel" gruppo può commentare anche fuori dal feed)
 *
 * Lo stesso pattern di `socialService.assertCanInteract` per i like.
 */

/** Carica il parent + applica authorization "chi può vedere/commentare". */
async function loadAndAuthorize(activityRefId, kind, userId) {
  if (kind === "activity") {
    const a = await Activity.findById(activityRefId).select(
      "userId sharedAt",
    );
    if (!a) throw new Error("ACTIVITY_NOT_FOUND");
    if (!a.sharedAt && String(a.userId) !== String(userId)) {
      throw new Error("NOT_SHARED");
    }
    return a;
  }
  // session
  const s = await HikeSession.findById(activityRefId).select(
    "creatorId participants sharedAt",
  );
  if (!s) throw new Error("SESSION_NOT_FOUND");
  if (!s.sharedAt) {
    const isCreator = String(s.creatorId) === String(userId);
    const isParticipant = (s.participants || []).some(
      (p) => String(p.userId) === String(userId),
    );
    if (!isCreator && !isParticipant) throw new Error("NOT_SHARED");
  }
  return s;
}

/** Modello Mongoose corrispondente al `kind` per le operazioni $inc. */
function modelFor(kind) {
  return kind === "activity" ? Activity : HikeSession;
}

/**
 * Aggiunge un commento e incrementa atomicamente `commentsCount` sul parent.
 *
 * Idempotenza: NIENTE — ogni POST aggiunge un nuovo documento. L'anti-spam
 * (max N comment/utente/giorno) è demandato a una eventuale Sprint 3 quando
 * abbiamo l'audit log.
 */
export async function addComment(activityRefId, kind, userId, text) {
  const parent = await loadAndAuthorize(activityRefId, kind, userId);
  const trimmed = (text || "").trim();
  if (trimmed.length < 1) throw new Error("COMMENT_EMPTY");

  // Insert + $inc in serie. Una transazione MongoDB sarebbe più robusta a
  // crash tra le due op, ma richiede replica set: per Render free non lo
  // abbiamo. Tollerabile: in caso di drift, una nightly reconcile lo sana.
  const comment = await Comment.create({
    activityRefId,
    kind,
    userId,
    text: trimmed,
  });
  await modelFor(kind).updateOne(
    { _id: activityRefId },
    { $inc: { commentsCount: 1 } },
  );

  // Notifica l'autore del post commentato (best-effort, no-op su self-comment).
  const ownerId = kind === "activity" ? parent.userId : parent.creatorId;
  await createNotification({
    recipientId: ownerId,
    actorId: userId,
    type: "comment",
    targetKind: kind,
    targetId: activityRefId,
  });

  // Restituiamo il commento populated per la UI (avatar + username dell'autore).
  return Comment.findById(comment._id)
    .populate("userId", "username personalInfo.avatarUrl")
    .lean();
}

/**
 * Lista commenti paginata, ordinata per `createdAt` desc (più recenti prima
 * — convenzione Strava/Instagram). L'indice composto su
 * `(activityRefId, kind, createdAt: -1)` rende la sort gratis.
 */
export async function getComments(activityRefId, kind, viewerId, { page = 1, limit = 20 } = {}) {
  await loadAndAuthorize(activityRefId, kind, viewerId);
  const skip = (Math.max(1, page) - 1) * limit;
  const [items, count] = await Promise.all([
    Comment.find({ activityRefId, kind })
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit)
      .populate("userId", "username personalInfo.avatarUrl")
      .lean(),
    Comment.countDocuments({ activityRefId, kind }),
  ]);
  return { count, items };
}

/**
 * Elimina un commento. Solo l'autore può rimuoverlo (o admin via
 * `role === "admin"` — ma il check admin lo fa la route, perché qui non
 * ricevo l'oggetto utente intero).
 *
 * Decrementa `commentsCount` sul parent. `min: 0` nello schema evita
 * negative count in caso di re-delete (operazione comunque idempotente
 * a livello documento: una volta cancellato, secondo delete → 404).
 */
export async function deleteComment(commentId, userId, { isAdmin = false } = {}) {
  const comment = await Comment.findById(commentId);
  if (!comment) throw new Error("COMMENT_NOT_FOUND");
  if (!isAdmin && String(comment.userId) !== String(userId)) {
    throw new Error("FORBIDDEN_NOT_AUTHOR");
  }
  const { activityRefId, kind } = comment;
  await comment.deleteOne();
  // Atomic: il count può andare temporaneamente sotto se delete arrivano
  // concorrenti, ma min:0 nello schema lo coerce a 0 al prossimo save.
  await modelFor(kind).updateOne(
    { _id: activityRefId, commentsCount: { $gt: 0 } },
    { $inc: { commentsCount: -1 } },
  );
  return { _id: commentId };
}
