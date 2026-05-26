/**
 * RefreshTokenService — emissione, rotation e revoca dei refresh token.
 *
 * Architettura: vedi commento iniziale di models/refreshToken.js.
 *
 * TTL configurabili via env:
 *   - JWT_ACCESS_TTL          (default "15m", formato jsonwebtoken)
 *   - JWT_REFRESH_TTL_DAYS    (default 30)
 */
import crypto from "crypto";
import jwt from "jsonwebtoken";
import RefreshToken from "../models/refreshToken.js";

// Backward compat: il TTL access era controllato da JWT_EXPIRES_IN (default 7d).
// Ora preferiamo JWT_ACCESS_TTL (default 15m) ma manteniamo il fallback per
// ambienti che non hanno ancora aggiornato l'env. In produzione l'env Render
// dovrebbe impostare JWT_ACCESS_TTL="15m" dopo il rollout del mobile con
// Authenticator (refresh trasparente).
const ACCESS_TTL =
  process.env.JWT_ACCESS_TTL || process.env.JWT_EXPIRES_IN || "15m";
const REFRESH_TTL_DAYS = parseInt(process.env.JWT_REFRESH_TTL_DAYS || "30", 10);
const REFRESH_TTL_MS = REFRESH_TTL_DAYS * 24 * 60 * 60 * 1000;

function hashToken(raw) {
  return crypto.createHash("sha256").update(raw).digest("hex");
}

function generateRawToken() {
  return crypto.randomBytes(48).toString("hex"); // 96 hex chars
}

/**
 * Emette un access token JWT firmato.
 * Payload: { userId, role, type: "access" } — il claim type evita ambiguità
 * fra access e (eventuali future) altri token JWT.
 */
export function generateAccessToken(user) {
  return jwt.sign(
    { userId: user._id, role: user.role, type: "access" },
    process.env.JWT_SECRET,
    { expiresIn: ACCESS_TTL },
  );
}

/**
 * Emette un nuovo refresh token e lo persiste come hash.
 * Se `family` è fornito, lo riusa (= step di rotation nella stessa chain).
 * Altrimenti crea una nuova family UUID (= nuovo login).
 *
 * @returns {{ raw: string, doc: RefreshToken, expiresAt: Date }}
 */
export async function issueRefreshToken(userId, { family, userAgent } = {}) {
  const raw = generateRawToken();
  const tokenHash = hashToken(raw);
  const expiresAt = new Date(Date.now() + REFRESH_TTL_MS);
  const familyId = family || crypto.randomUUID();

  const doc = await RefreshToken.create({
    userId,
    tokenHash,
    family: familyId,
    expiresAt,
    userAgent: userAgent || null,
  });

  return { raw, doc, expiresAt };
}

/**
 * Valida un refresh token raw, lo revoca, ed emette una coppia nuova
 * (access + refresh) nella stessa family.
 *
 * Detect replay attack: se il token presentato è già stato revocato E
 * sostituito (replacedBy != null), assumiamo esfiltrazione → revoca l'intera
 * family e ritorna 401. L'utente legittimo dovrà rifare login.
 *
 * @throws Error con code "REFRESH_TOKEN_INVALID" se non trovato/scaduto.
 * @throws Error con code "REFRESH_TOKEN_REUSED" se replay detected.
 */
export async function rotateRefreshToken(rawToken, { userAgent } = {}) {
  if (!rawToken || typeof rawToken !== "string") {
    throw Object.assign(new Error("REFRESH_TOKEN_INVALID"), { statusCode: 401 });
  }
  const tokenHash = hashToken(rawToken);
  const existing = await RefreshToken.findOne({ tokenHash });

  if (!existing) {
    throw Object.assign(new Error("REFRESH_TOKEN_INVALID"), { statusCode: 401 });
  }

  // Replay detection: token già revocato e sostituito = qualcuno sta riusando
  // un token vecchio della chain. Revoca tutto.
  if (existing.replacedBy) {
    await RefreshToken.updateMany(
      { family: existing.family, revokedAt: null },
      { $set: { revokedAt: new Date() } },
    );
    throw Object.assign(new Error("REFRESH_TOKEN_REUSED"), { statusCode: 401 });
  }

  if (existing.revokedAt) {
    throw Object.assign(new Error("REFRESH_TOKEN_INVALID"), { statusCode: 401 });
  }

  if (existing.expiresAt < new Date()) {
    throw Object.assign(new Error("REFRESH_TOKEN_INVALID"), { statusCode: 401 });
  }

  // Carica user per emettere access token con role aggiornato (potrebbe essere
  // cambiato dall'admin mentre l'utente era loggato).
  const User = (await import("../models/user.js")).default;
  const user = await User.findById(existing.userId).select("_id role");
  if (!user) {
    throw Object.assign(new Error("REFRESH_TOKEN_INVALID"), { statusCode: 401 });
  }

  // Emetti il nuovo refresh nella stessa family
  const issued = await issueRefreshToken(user._id, {
    family: existing.family,
    userAgent,
  });

  // Revoca il vecchio e linkalo al nuovo
  existing.revokedAt = new Date();
  existing.replacedBy = issued.doc._id;
  await existing.save();

  const accessToken = generateAccessToken(user);

  return {
    accessToken,
    refreshToken: issued.raw,
    expiresIn: ACCESS_TTL, // stringa "15m" — il client può parsarla se utile
    refreshExpiresAt: issued.expiresAt,
  };
}

/**
 * Revoca un singolo refresh token (chiamato da /auth/logout).
 * Idempotente: se il token non esiste o è già revocato, ritorna comunque ok.
 */
export async function revokeRefreshToken(rawToken) {
  if (!rawToken || typeof rawToken !== "string") return;
  const tokenHash = hashToken(rawToken);
  await RefreshToken.updateOne(
    { tokenHash, revokedAt: null },
    { $set: { revokedAt: new Date() } },
  );
}

/**
 * Revoca TUTTI i refresh token di un utente (logout su tutti i device).
 */
export async function revokeAllForUser(userId) {
  await RefreshToken.updateMany(
    { userId, revokedAt: null },
    { $set: { revokedAt: new Date() } },
  );
}
