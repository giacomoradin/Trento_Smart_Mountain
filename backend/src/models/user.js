import mongoose from "mongoose";
const { Schema } = mongoose;

const userSchema = new Schema(
  {
    username: { type: String, required: true, unique: true },
    email: { type: String, required: true, unique: true },
    passwordHash: { type: String, required: true },
    role: { type: String, enum: ["user", "admin"], default: "user" },
    isVerified: { type: Boolean, default: false },
    verificationToken: { type: String },
    createdAt: { type: Date, default: Date.now },
  },
  {
    // Abilita l'inclusione dei virtuals quando converti in JSON/Object
    toJSON: { virtuals: true },
    toObject: { virtuals: true },
  },
);

// VIRTUAL POPULATE: Permette di fare User.find().populate('mySessions')
// Senza salvare nulla fisicamente nel documento User.
userSchema.virtual("mySessions", {
  ref: "HikeSession", // Modello target
  localField: "_id", // Campo in User
  foreignField: "participants.userId", // Campo in HikeSession
});

export default mongoose.model("User", userSchema);
