import mongoose from "mongoose";
const { Schema } = mongoose;

const userSchema = new Schema(
  {
    username: { type: String, required: true, unique: true },
    email: { type: String, required: true, unique: true },
    passwordHash: { type: String, required: true },
    role: {
      type: String,
      enum: ["groupLeader", "rifugio", "admin"],
      default: "groupLeader",
    },
    isVerified: { type: Boolean, default: false },
    verificationToken: { type: String },
    passwordResetToken: { type: String },
    passwordResetExpires: { type: Date },
    rifugioDetails: {
      rifugioName: { type: String },
      caiCode: { type: String },
      quota: { type: Number },
      posti: { type: Number },
      coordinates: { type: String },
    },
    /**
     * Ruoli per-sessione dell'utente.
     * Aggiornato da hikeSessionService alla creazione/join di ogni sessione.
     * Usato per la futura dashboard gamification e il controllo OCL
     * "una sola sessione ACTIVE per utente".
     */
    sessionRoles: [
      {
        groupId: { type: Schema.Types.ObjectId, ref: "HikeSession" },
        role: { type: String, enum: ["groupLeader", "hiker"] },
        createdBy: { type: Schema.Types.ObjectId, ref: "User" },
      },
    ],
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
