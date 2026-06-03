import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Notifica social in-app.
 *
 *   recipientId → chi RICEVE la notifica (la vede nel suo centro notifiche)
 *   actorId     → chi ha generato l'evento (es. chi ha messo like / ti segue)
 *   type        → "follow" | "like" | "comment"
 *   targetKind  → per like/comment: "activity" | "session" (null per follow)
 *   targetId    → _id dell'Activity/HikeSession coinvolta (null per follow)
 *   read        → false finché l'utente non apre il centro notifiche
 *
 * Le notifiche sono "best-effort": vengono create dopo l'azione principale
 * (follow/like/comment) e un eventuale errore di creazione NON deve mai far
 * fallire l'azione (vedi notificationService.createNotification).
 */
const notificationSchema = new Schema({
  recipientId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },
  actorId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
  },
  type: {
    type: String,
    // social: follow/like/comment · sessione: join_request/join_accepted/removed
    enum: [
      "follow",
      "like",
      "comment",
      "join_request",
      "join_accepted",
      "removed",
    ],
    required: true,
  },
  targetKind: {
    type: String,
    enum: ["activity", "session"],
    default: null,
  },
  targetId: {
    type: Schema.Types.ObjectId,
    default: null,
  },
  // Testo precomputato opzionale (per notifiche non-social; le social derivano
  // il testo client-side da type+actor).
  message: { type: String, default: null },
  read: { type: Boolean, default: false },
  createdAt: { type: Date, default: Date.now },
});

// Query principale: notifiche di un utente ordinate per data desc.
notificationSchema.index({ recipientId: 1, createdAt: -1 });
// Conteggio badge non-letti.
notificationSchema.index({ recipientId: 1, read: 1 });

const Notification = mongoose.model("Notification", notificationSchema);
export default Notification;
