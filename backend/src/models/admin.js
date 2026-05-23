import mongoose from "mongoose";
import User from "./user.js";

/**
 * Discriminator per gli utenti **amministratore**.
 *
 * - Discriminator key: "admin"
 * - Riservato per la gestione utenti, dashboard sistema, broadcast allerte.
 * - In futuro: permessi granulari, log audit, scope.
 *
 * Tutti i documenti Admin hanno `role: "admin"` nella collection `users`.
 */
const adminSchema = new mongoose.Schema({
  // Spazio riservato per estensioni admin (es. permessi granulari)
});

const Admin = User.discriminator("admin", adminSchema);
export default Admin;
