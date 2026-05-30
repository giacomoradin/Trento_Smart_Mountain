/**
 * RefreshToken — whitelist server-side per refresh token rotation.
 *
 * Pattern: rotation con "family chain" per detection di replay attack.
 *
 *   /auth/login   → emette { access(15m), refresh(30d, family=F1#1) }
 *   /auth/refresh → valida F1#1, revoca, emette { access, refresh(family=F1#2) }
 *   /auth/refresh → valida F1#2, revoca, emette { access, refresh(family=F1#3) }
 *   ...
 *
 * Se qualcuno tenta di riusare F1#1 (già revocato e replaced), assumiamo
 * un'esfiltrazione e revochiamo tutta la family — l'utente legittimo deve
 * fare re-login. Vedi `rotateRefreshToken` in refreshTokenService.js.
 *
 * NOTA: il token raw NON è mai salvato in DB — solo il SHA-256. Se il DB
 * viene compromesso, l'attaccante non può usare gli hash come token.
 */
import mongoose from "mongoose";

const refreshTokenSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "User",
    required: true,
    index: true,
  },
  // sha256(raw) — il raw esiste solo nella response al client. Indice per
  // lookup veloce in /auth/refresh.
  tokenHash: { type: String, required: true, unique: true },
  // UUID che lega tutti i refresh emessi in una catena di rotation. Se uno
  // viene riutilizzato dopo essere stato sostituito, revochiamo tutta la
  // family per limitare il danno di un eventuale furto.
  family: { type: String, required: true, index: true },
  expiresAt: { type: Date, required: true },
  revokedAt: { type: Date, default: null },
  // Link al refresh che ha sostituito questo (rotation step successivo).
  // Permette di detectare replay: se replacedBy != null E qualcuno usa il
  // token, è un replay → revoca family.
  replacedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "RefreshToken",
    default: null,
  },
  userAgent: { type: String, default: null },
  createdAt: { type: Date, default: Date.now },
});

// TTL index: MongoDB cancella automaticamente i documenti dopo expiresAt
// (best-effort, ~60s di lag). Risparmia cleanup manuale e tiene la collection
// piccola in produzione.
refreshTokenSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

const RefreshToken = mongoose.model("RefreshToken", refreshTokenSchema);
export default RefreshToken;
