import mongoose from "mongoose";
import User from "./user.js";

/**
 * Discriminator per gli utenti **rifugio**.
 *
 * - Discriminator key: "rifugio"
 * - Estende User con i metadati anagrafici della struttura
 *   (in precedenza erano in un subdocument `rifugioDetails`,
 *    ora sono campi flat per query più semplici).
 *
 * Tutti i documenti Refuge hanno `role: "rifugio"` nella collection `users`.
 */
const refugeSchema = new mongoose.Schema({
  rifugioName: { type: String, required: true },
  caiCode: { type: String },
  quota: { type: Number }, // metri s.l.m.
  posti: { type: Number }, // capienza posti letto
  /**
   * Coordinate testuali "lat lng" inserite dall'operatore.
   * In futuro potrebbero diventare GeoJSON Point per query 2dsphere.
   */
  coordinates: { type: String },
});

const Refuge = User.discriminator("rifugio", refugeSchema);
export default Refuge;
