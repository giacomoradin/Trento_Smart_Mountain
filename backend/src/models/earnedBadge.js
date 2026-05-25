import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Badge "earned" — un solo record per (userId, badgeCode). L'unique index
 * impedisce doppi accrediti se un trigger viene ri-eseguito (idempotency
 * by-design, niente bisogno di lock applicativi).
 *
 * Il catalogo dei badge (code → metadata: nome, descrizione, icona, soglia)
 * è statico in services/badgeService.js — non in DB. Aggiungere/rimuovere
 * badge richiede solo un deploy, niente migration.
 */
const earnedBadgeSchema = new Schema({
  userId: { type: Schema.Types.ObjectId, ref: "User", required: true, index: true },
  badgeCode: { type: String, required: true },
  earnedAt: { type: Date, default: Date.now },
  // Snapshot opzionale del valore che ha triggerato il badge
  // (es. "10 totem scansionati" → contextValue = 10). Utile per future
  // versioni del catalogo che mostrano "hai sbloccato a 12 totem".
  contextValue: { type: Number },
});

earnedBadgeSchema.index({ userId: 1, badgeCode: 1 }, { unique: true });

const EarnedBadge = mongoose.model("EarnedBadge", earnedBadgeSchema);
export default EarnedBadge;
