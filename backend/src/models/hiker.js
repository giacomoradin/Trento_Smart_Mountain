import mongoose from "mongoose";
import User from "./user.js";

/**
 * Discriminator per gli utenti **escursionisti** (capogruppo).
 *
 * - Discriminator key: "groupLeader"
 * - Estende lo schema base User senza aggiungere campi al momento
 * - In futuro: badge gamification, livello CAI, social credits, ecc.
 *
 * Tutti i documenti Hiker hanno `role: "groupLeader"` nella collection `users`.
 */
const hikerSchema = new mongoose.Schema({
  // Spazio riservato per future estensioni specifiche dell'escursionista
  // (es. saldoSc, badge[], livelloEsperienza, etc.)
});

const Hiker = User.discriminator("groupLeader", hikerSchema);
export default Hiker;
