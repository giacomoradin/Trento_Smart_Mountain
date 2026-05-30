import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Schema BASE per tutti gli utenti del sistema.
 *
 * Usa il pattern Mongoose discriminator: una sola collection MongoDB (`users`)
 * contiene documenti di tipi diversi distinti dal campo `role`. Ogni ruolo
 * specifico aggiunge i propri campi tramite il discriminatore:
 *   - Hiker  (role: "groupLeader") → models/hiker.js
 *   - Refuge (role: "rifugio")      → models/refuge.js
 *   - Admin  (role: "admin")        → models/admin.js
 *
 * Vantaggi del discriminator vs collection separate:
 *  - Login, populate (HikeSession.creatorId), JWT auth restano semplici
 *    (una sola query, una sola collection da cercare)
 *  - Nessuna migrazione dati dagli utenti esistenti
 *  - Ogni schema specifico contiene solo i campi rilevanti per quel ruolo
 *    (no `rifugioDetails: null` per gli escursionisti)
 */
const userSchema = new Schema(
  {
    username: { type: String, required: true, unique: true },
    email: { type: String, required: true, unique: true },
    passwordHash: { type: String, required: true },
    isVerified: { type: Boolean, default: false },
    verificationToken: { type: String },
    passwordResetToken: { type: String },
    passwordResetExpires: { type: Date },

    /**
     * Ruoli per-sessione dell'utente (popolati da hikeSessionService).
     * Mantenuti nello schema base perché un utente di qualunque tipo
     * (escursionista, rifugio admin) può creare/partecipare a sessioni.
     *
     * NOTA: i campi rifugioDetails (rifugioName, caiCode, quota, posti, coordinates)
     * sono stati spostati nel discriminator Refuge (vedi models/refuge.js) dopo il
     * refactor 2026-05 split User → Hiker/Refuge/Admin. Non vanno reintrodotti qui.
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
    // Il campo `role` distingue i discriminatori (Hiker/Refuge/Admin)
    discriminatorKey: "role",
    // Tutti i sotto-tipi finiscono nella stessa collection "users"
    collection: "users",
    toJSON: { virtuals: true },
    toObject: { virtuals: true },
  },
);

// Virtual populate: User.find().populate('mySessions')
userSchema.virtual("mySessions", {
  ref: "HikeSession",
  localField: "_id",
  foreignField: "participants.userId",
});

const User = mongoose.model("User", userSchema);
export default User;
