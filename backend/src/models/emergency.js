import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Segnalazione SOS legata a una sessione ACTIVE.
 * Il payload sensibile viaggia solo via HTTPS; il beacon BLE espone solo beaconInstanceId.
 */
const emergencySchema = new Schema(
  {
    sessionId: {
      type: Schema.Types.ObjectId,
      ref: "HikeSession",
      required: true,
      index: true,
    },
    senderUserId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },
    emergencyType: {
      type: String,
      enum: ["INJURY", "LOST", "AVALANCHE", "WEATHER", "EQUIPMENT", "OTHER"],
      required: true,
    },
    coordinates: {
      type: { type: String, enum: ["Point"], default: "Point" },
      coordinates: {
        type: [Number],
        required: true,
      },
    },
    profileSnapshot: {
      displayName: { type: String, required: true },
      personalInfo: {
        sex: { type: String },
        birthDate: { type: Date },
        heightCm: { type: Number },
        weightKg: { type: Number },
      },
      experience: {
        caiLevel: { type: String },
        baselineFitness: { type: String },
        weeklyTrainingFreq: { type: String },
      },
    },
    status: {
      type: String,
      enum: ["ACTIVE", "SHARED_WITH_GROUP", "DISMISSED", "CANCELLED_BY_SENDER"],
      default: "ACTIVE",
      index: true,
    },
    /** Identificatore beacon (6 byte hex) generato dal client; usato in iBeacon minor/UID. */
    beaconInstanceId: {
      type: String,
      required: true,
      match: /^[0-9a-fA-F]{12}$/,
    },
    idempotencyKey: {
      type: String,
      required: true,
    },
    /** Riservato firma Ed25519 (Sprint successivo). */
    signature: { type: String, default: null },
    cancelReason: {
      type: String,
      enum: ["MISTAKE", "RESOLVED_SELF"],
    },
    leaderAckAt: { type: Date, default: null },
    sharedAt: { type: Date, default: null },
    dismissedAt: { type: Date, default: null },
    dismissedBy: { type: Schema.Types.ObjectId, ref: "User", default: null },
    cancelledAt: { type: Date, default: null },
    cancelledBy: { type: Schema.Types.ObjectId, ref: "User", default: null },
    createdAt: { type: Date, default: Date.now },
  },
  { versionKey: false },
);

emergencySchema.index({ sessionId: 1, status: 1, createdAt: -1 });
emergencySchema.index({ idempotencyKey: 1 }, { unique: true });

const Emergency = mongoose.model("Emergency", emergencySchema);
export default Emergency;
