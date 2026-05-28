import mongoose from "mongoose";
const {
  Schema,
  Types: { ObjectId },
} = mongoose;

const nfcScanSchema = new Schema({
  userId: { type: ObjectId, ref: "User", required: true, index: true },
  totemId: { type: ObjectId, ref: "NfcTotem", required: true, index: true },
  tagId: { type: String, required: true },
  scannedAt: { type: Date, default: Date.now },
  // Bucket giornaliero "YYYY-MM-DD" in UTC, calcolato lato service prima di
  // ogni create. Serve come chiave per l'unique partial index sotto, che
  // previene ATOMICAMENTE la race condition di doppio scan dello stesso
  // totem nella stessa giornata UTC (vedi fix audit 2026-05).
  scanDay: { type: String },
  gpsLocation: {
    type: { type: String, enum: ["Point"], default: "Point" },
    coordinates: { type: [Number], required: true },
  },
  distanceFromTotem: { type: Number, required: true },
  creditsAwarded: { type: Number, default: 0 },
  rejectionReason: {
    type: String,
    enum: ["OUT_OF_RANGE", "RATE_LIMIT", null],
    default: null,
  },
});

// Compound index to speed up the 24h rate-limit query in scanTotem and prevent
// concurrent scans from both passing the findOne check (MongoDB query-level serialization).
nfcScanSchema.index({
  userId: 1,
  totemId: 1,
  scannedAt: -1,
  creditsAwarded: 1,
});

// Unique partial index: solo gli scan con crediti (1 / giorno / totem / user).
// Le scansioni rejected (OUT_OF_RANGE / RATE_LIMIT) hanno creditsAwarded=0 e
// non sono coperte dall'unique → l'utente può tentarne quante vuole.
// Questo trasforma il check findOne+create in un insert atomico: una seconda
// scansione "valida" lo stesso giorno fallisce con E11000 → catturata nel service.
nfcScanSchema.index(
  { userId: 1, totemId: 1, scanDay: 1 },
  { unique: true, partialFilterExpression: { creditsAwarded: { $gt: 0 } } },
);

const NfcScan = mongoose.model("NfcScan", nfcScanSchema);
export default NfcScan;
