import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Modello Challenge — sfida 1-vs-1 o di gruppo tra escursionisti.
 *
 * Flusso:
 *   1. Creator chiama POST /challenges → status PENDING, partecipante creator AUTO-accepted
 *   2. Altri utenti invitati ricevono nella lista "le mie sfide" → POST /accept o /decline
 *   3. Quando startDate >= now → status ACTIVE (verificato lazy alla lettura)
 *   4. Progresso calcolato server-side dalle sessioni/attività dei partecipanti
 *      nella finestra [startDate, endDate]
 *   5. A endDate scaduto → status COMPLETED, winner = chi ha il progress più alto
 *
 * Il "tipo di metrica" determina come si calcola il progresso:
 *   - distance:  somma km percorsi
 *   - elevation: somma metri di dislivello positivo
 *   - count:     numero di escursioni completate
 *   - points:    somma finalPoints accreditati
 */
const challengeSchema = new Schema({
  creatorId: { type: Schema.Types.ObjectId, ref: "User", required: true, index: true },

  title: { type: String, required: true, maxlength: 80, trim: true },
  description: { type: String, maxlength: 280, trim: true },

  metric: { type: String, enum: ["distance", "elevation", "count", "points"], required: true },
  // Target per partecipante (es. "100 km" = ognuno deve fare 100 km).
  // Se null → modalità "competitiva": vince chi accumula di più nel periodo.
  targetValue: { type: Number, min: 0 },

  startDate: { type: Date, required: true },
  endDate: { type: Date, required: true },

  status: {
    type: String,
    enum: ["PENDING", "ACTIVE", "COMPLETED", "CANCELLED"],
    default: "PENDING",
  },

  // Lista partecipanti — il creator è incluso con status "accepted" da subito.
  participants: [{
    userId: { type: Schema.Types.ObjectId, ref: "User", required: true },
    status: { type: String, enum: ["invited", "accepted", "declined"], default: "invited" },
    invitedAt: { type: Date, default: Date.now },
    respondedAt: { type: Date },
  }],

  // Cache del winner una volta che la sfida è COMPLETED. Idempotente: setato
  // una sola volta dal job di chiusura (o calcolo lazy al primo GET dopo endDate).
  winnerId: { type: Schema.Types.ObjectId, ref: "User" },
  closedAt: { type: Date },

  createdAt: { type: Date, default: Date.now },
});

// Index per "le mie sfide" (creator OR participants.userId), ordinate per startDate.
challengeSchema.index({ "participants.userId": 1, startDate: -1 });
challengeSchema.index({ creatorId: 1, startDate: -1 });

const Challenge = mongoose.model("Challenge", challengeSchema);
export default Challenge;
