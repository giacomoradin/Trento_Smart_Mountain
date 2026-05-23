// Attività personale ("libera") registrata da un escursionista.
// Differenze rispetto a HikeSession: owner singolo, no lifecycle (sempre
// completata al momento del create), no inviteCode/participants.
// Le stats sono sempre reali, non c'è una fase GPX pianificata.
import mongoose from "mongoose";
const { Schema } = mongoose;

const activitySchema = new Schema({
  userId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },

  name: { type: String, required: true, maxlength: 120 },
  activityType: {
    type: String,
    enum: ["hiking", "trail", "skitouring", "trekking"],
    default: "hiking",
  },
  difficultyLevel: { type: String, enum: ["T", "E", "EE", "EEA"] },

  // epoch ms — stesso formato del client mobile per coerenza
  startTimeMs: { type: Number, required: true },
  endTimeMs: { type: Number, required: true },
  completedAt: { type: Date, default: Date.now, index: true },

  startPoint: {
    type: { type: String, enum: ["Point"] },
    coordinates: { type: [Number] }, // [lon, lat]
  },
  endPoint: {
    type: { type: String, enum: ["Point"] },
    coordinates: { type: [Number] },
  },

  actualStats: {
    movingSeconds: { type: Number, required: true },
    totalSeconds: { type: Number, required: true },
    distanceMeters: { type: Number, required: true },
    elevationGainM: { type: Number, required: true },
    finalPoints: { type: Number },
    estimatedCalories: { type: Number },
    currentAltitudeM: { type: Number },
  },

  // profilo altimetrico campionato (max 200 punti, metri assoluti)
  elevationProfile: { type: [Number], default: undefined },
});

activitySchema.index({ userId: 1, completedAt: -1 });
activitySchema.index({ startPoint: "2dsphere" }, { sparse: true });

const Activity = mongoose.model("Activity", activitySchema);
export default Activity;
