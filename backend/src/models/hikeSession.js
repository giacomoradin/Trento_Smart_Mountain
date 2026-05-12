import mongoose from "mongoose";
const { Schema } = mongoose;

const hikSessionSchema = new Schema({
  // Chi ha creato la sessione diventa automaticamente Capogruppo
  creatorId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
  },

  // Parametri del percorso
  routeDetails: {
    name: { type: String, required: true },
    startPoint: {
      type: { type: String, enum: ["Point"], default: "Point" },
      coordinates: { type: [Number], required: true }, // [lng, lat]
    },
    endPoint: {
      type: { type: String, enum: ["Point"], default: "Point" },
      coordinates: { type: [Number], required: true },
    },
    difficultyLevel: {
      type: String,
      enum: ["T", "E", "EE", "EEA"], // scala CAI standard
      required: true,
    },
    elevationGain: { type: Number }, // dislivello in metri
  },

  // Codice invito alfanumerico univoco (generato automaticamente)
  inviteCode: {
    type: String,
    required: true,
    unique: true,
    uppercase: true,
  },

  // Lista partecipanti che hanno accettato l'invito
  participants: [
    {
      userId: { type: Schema.Types.ObjectId, ref: "User", required: true },
      role: { type: String, enum: ["hiker", "groupLeader"], default: "hiker" },
      joinedAt: { type: Date, default: Date.now },
    },
  ],

  // Stato della sessione
  status: {
    type: String,
    enum: ["PLANNED", "ACTIVE", "COMPLETED", "CANCELLED"],
    default: "PLANNED",
  },

  // Failover leadership
  statoFailover: { type: Boolean, default: false },
  lastHeartbeat: { type: Date, default: Date.now },

  startTime: { type: Date },
  endTime: { type: Date },
  createdAt: { type: Date, default: Date.now },
});

// Indice geospaziale per query di prossimità
hikSessionSchema.index({ "routeDetails.startPoint": "2dsphere" });
hikSessionSchema.index({ inviteCode: 1 });

const HikeSession = mongoose.model("HikeSession", hikSessionSchema);
export default HikeSession;
