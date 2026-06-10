import mongoose from "mongoose";
const { Schema } = mongoose;

/**
 * Post della "Bacheca rifugi": un rifugista pubblica informazioni, avvisi o
 * segnalazioni di pericolo, consultabili da tutti gli escursionisti.
 *
 *   type: "info"     → comunicazione generica (es. orari, disponibilità posti)
 *         "avviso"   → avviso operativo (es. sentiero chiuso, acqua non potabile)
 *         "pericolo" → segnalazione di pericolo (es. rischio valanghe, frana)
 *
 * `refugeName` è denormalizzato per mostrare l'autore nel feed utenti senza
 * un populate ad ogni lettura. `validUntil` opzionale: oltre quella data il
 * post è considerato scaduto (filtrabile via `activeOnly`).
 */
const refugeBoardPostSchema = new Schema({
  refugeId: {
    type: Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },
  refugeName: { type: String, default: "Rifugio" },
  type: {
    type: String,
    enum: ["info", "avviso", "pericolo"],
    default: "info",
    index: true,
  },
  title: { type: String, required: true, maxlength: 120 },
  body: { type: String, required: true, maxlength: 2000 },
  // Scadenza opzionale: null = sempre valido.
  validUntil: { type: Date, default: null },
  createdAt: { type: Date, default: Date.now, index: true },
});

// Query principale del feed utenti: tutti i post ordinati per data desc.
refugeBoardPostSchema.index({ createdAt: -1 });

const RefugeBoardPost = mongoose.model("RefugeBoardPost", refugeBoardPostSchema);
export default RefugeBoardPost;
