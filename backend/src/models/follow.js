import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Relazione di follow ASIMMETRICA stile Strava.
 *
 *   followerId  → utente che segue
 *   followingId → utente seguito
 *
 * `A segue B` ≠ `B segue A`: ogni direzione è un documento distinto. L'indice
 * compound unique impedisce di seguire due volte lo stesso utente. L'anti-
 * self-follow (followerId == followingId) NON è in schema validation ma viene
 * controllato nel service (vedi `followService.js`) per dare un errore più
 * leggibile (`SELF_FOLLOW` invece di duplicate-key).
 *
 * Indici separati su followerId e followingId per le due query principali:
 *   - "chi seguo io" → find({followerId: me})
 *   - "chi mi segue" → find({followingId: me})
 */
const followSchema = new Schema({
  followerId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },
  followingId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },
  createdAt: { type: Date, default: Date.now },
});

// Unique compound: un solo documento per coppia (follower, following).
// Garantisce idempotenza a livello DB anche se due POST /follow vanno in race.
followSchema.index({ followerId: 1, followingId: 1 }, { unique: true });

const Follow = mongoose.model("Follow", followSchema);
export default Follow;
