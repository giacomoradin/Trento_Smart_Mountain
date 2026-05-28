import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Commento su un'attività libera (Activity) o su una sessione di gruppo
 * (HikeSession).
 *
 * Pattern "polymorphic ref" via coppia (`activityRefId`, `kind`):
 *   - `activityRefId` punta al `_id` del documento (Activity o HikeSession)
 *   - `kind` distingue il tipo per il join lato service.
 *
 * Trade-off scelto vs collection separate:
 *   - Pro: una sola collection da indicizzare, query feed-side più semplice
 *   - Contro: niente `ref` Mongoose esplicito → niente populate auto sul
 *     parent. Il service fa il lookup manuale (e ne ha bisogno comunque per
 *     l'authorization "è condivisa?").
 *
 * Validazione testo:
 *   - 1..500 caratteri (Joi lato route + maxlength schema come safety net)
 *   - Trim applicato lato Joi prima dell'insert (vedi commentSchema in
 *     validationMiddleware.js)
 *
 * `commentsCount` denormalizzato sul parent (Activity.commentsCount /
 * HikeSession.commentsCount) viene aggiornato via $inc nel service. Verità
 * autorevole rimane il count effettivo su questa collection — uno script
 * nightly potrà riconciliare in caso di drift.
 */
const commentSchema = new Schema({
  activityRefId: {
    type: Schema.Types.ObjectId,
    required: true,
    index: true,
  },
  kind: {
    type: String,
    enum: ["activity", "session"],
    required: true,
  },
  userId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },
  text: {
    type: String,
    required: true,
    minlength: 1,
    maxlength: 500,
  },
  createdAt: { type: Date, default: Date.now, index: true },
});

// Indice composto per la query "tutti i commenti di un certo target, ordinati
// per data desc" — è la query di base della CommentsBottomSheet lato mobile.
// Mettiamo `createdAt: -1` direttamente nell'indice così evitiamo un sort
// in memoria durante la paginazione.
commentSchema.index({ activityRefId: 1, kind: 1, createdAt: -1 });

const Comment = mongoose.model("Comment", commentSchema);
export default Comment;
