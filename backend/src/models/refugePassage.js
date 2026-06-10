import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Passaggio di un escursionista rilevato dalla rete del rifugio (BLE-mesh / NFC),
 * con i social credit accreditati. Alimenta la sezione "PASSAGGI OGGI" della
 * Dashboard IoT.
 *
 * `userId` è opzionale: i passaggi mock (o quelli di utenti non registrati
 * rilevati solo via beacon) hanno solo `displayName`.
 */
const refugePassageSchema = new Schema({
  refugeId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },
  userId: { type: Schema.Types.ObjectId, ref: "User", default: null },
  displayName: { type: String, required: true },
  via: { type: String, enum: ["mesh", "nfc", "manual"], default: "mesh" },
  credits: { type: Number, default: 0 },
  passedAt: { type: Date, default: Date.now, index: true },
});

refugePassageSchema.index({ refugeId: 1, passedAt: -1 });

const RefugePassage = mongoose.model("RefugePassage", refugePassageSchema);
export default RefugePassage;
