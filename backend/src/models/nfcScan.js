import mongoose from "mongoose";
const { Schema, Types: { ObjectId } } = mongoose;

const nfcScanSchema = new Schema({
  userId:            { type: ObjectId, ref: "User", required: true, index: true },
  totemId:           { type: ObjectId, ref: "NfcTotem", required: true, index: true },
  tagId:             { type: String, required: true },
  scannedAt:         { type: Date, default: Date.now },
  gpsLocation: {
    type:        { type: String, enum: ["Point"], default: "Point" },
    coordinates: { type: [Number], required: true },
  },
  distanceFromTotem: { type: Number, required: true },
  creditsAwarded:    { type: Number, default: 0 },
  rejectionReason:   { type: String, enum: ["OUT_OF_RANGE", "RATE_LIMIT", null], default: null },
});

// Compound index to speed up the 24h rate-limit query in scanTotem and prevent
// concurrent scans from both passing the findOne check (MongoDB query-level serialization).
nfcScanSchema.index({ userId: 1, totemId: 1, scannedAt: -1, creditsAwarded: 1 });

const NfcScan = mongoose.model("NfcScan", nfcScanSchema);
export default NfcScan;
