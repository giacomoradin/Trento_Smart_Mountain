import mongoose from "mongoose";
const { Schema, Types: { ObjectId } } = mongoose;

const creditTransactionSchema = new Schema({
  userId:    { type: ObjectId, ref: "User", required: true, index: true },
  amount:    { type: Number, required: true },
  source:    {
    type: String,
    enum: ["session", "free_activity", "quiz", "nfc", "admin_adjust"],
    required: true,
  },
  refId:     { type: ObjectId, required: false },
  refKind:   { type: String, required: false },
  note:      { type: String, maxlength: 200 },
  createdAt: { type: Date, default: Date.now, index: true },
});
creditTransactionSchema.index({ userId: 1, createdAt: -1 });

const CreditTransaction = mongoose.model("CreditTransaction", creditTransactionSchema);
export default CreditTransaction;
