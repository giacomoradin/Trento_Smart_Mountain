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
    // Snapshot GPS al momento dell'invio SOS. Non aggiornato in tempo reale;
    // posizione live dei partecipanti → US mappa sessione (US-22). Vedi docs/sos_feature.md.
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
    /** true se il mittente ha avviato il beacon BLE al momento dell'invio. */
    beaconActive: { type: Boolean, default: true },
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
  {
    versionKey: false,
    timestamps: true, // Abilita createdAt e updatedAt automatici
  },
);

// TTL Index: Eliminazione automatica per evitare saturazione DB (Sprint 3 proattivo).
// 1. Emergenze risolte (DISMISSED o CANCELLED) rimosse dopo 3 giorni dall'ultimo aggiornamento.
emergencySchema.index(
  { updatedAt: 1 },
  {
    expireAfterSeconds: 3 * 24 * 3600,
    partialFilterExpression: {
      status: { $in: ["DISMISSED", "CANCELLED_BY_SENDER"] },
    },
  },
);

// 2. Emergenze che rimangono "attive" o "condivise" (non gestite) rimosse dopo 7 giorni dalla creazione.
emergencySchema.index(
  { createdAt: 1 },
  {
    expireAfterSeconds: 7 * 24 * 3600,
    partialFilterExpression: {
      status: { $in: ["ACTIVE", "SHARED_WITH_GROUP"] },
    },
  },
);

emergencySchema.index({ sessionId: 1, status: 1, createdAt: -1 });
emergencySchema.index({ idempotencyKey: 1 }, { unique: true });

const Emergency = mongoose.model("Emergency", emergencySchema);
export default Emergency;
