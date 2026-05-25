import mongoose from "mongoose";
const { Schema } = mongoose;

const nfcTotemSchema = new Schema({
  tagId:       { type: String, required: true, unique: true, index: true },
  name:        { type: String, required: true },
  description: { type: String, maxlength: 500 },
  location: {
    type:        { type: String, enum: ["Point"], default: "Point" },
    coordinates: { type: [Number], required: true },  // [lon, lat]
  },
  altitude:      { type: Number },
  radius:        { type: Number, default: 50 },
  creditsReward: { type: Number, default: 25, min: 0, max: 500 },
  kind:          { type: String, enum: ["checkpoint", "summit", "refuge"], default: "checkpoint" },
  active:        { type: Boolean, default: true },
  createdAt:     { type: Date, default: Date.now },
});
nfcTotemSchema.index({ location: "2dsphere" });

const NfcTotem = mongoose.model("NfcTotem", nfcTotemSchema);
export default NfcTotem;
