import jwt from "jsonwebtoken";
import bcrypt from "bcrypt";
import Hiker from "../../src/models/hiker.js";
import User from "../../src/models/user.js";

/**
 * Helper functions per i test di autenticazione.
 *
 * Fornisce utility per:
 * - Creare utenti di test nel database
 * - Generare token JWT validi/invalidi/scaduti
 * - Simulare diversi scenari di autenticazione
 */

const JWT_SECRET =
  process.env.JWT_SECRET || "tsm-local-2026-x9Qm2pL7vN4kR8f83js055hfj2na17f";

/**
 * Crea un escursionista di test e restituisce user e token JWT.
 *
 * @param {Object} userData - Dati opzionali per personalizzare l'utente
 * @returns {Promise<{user: Object, token: string, password: string}>}
 *
 * @example
 * const { user, token, password } = await createTestHiker({
 *   username: 'mario',
 *   email: 'mario@example.com'
 * });
 */
export async function createTestHiker(userData = {}) {
  const defaultPassword = "TestPassword123!";

  const defaultUser = {
    username: "testhiker",
    email: "hiker@test.com",
    password: defaultPassword,
    ...userData,
  };

  // Hash della password come nel vero servizio
  const passwordHash = await bcrypt.hash(defaultUser.password, 10);

  // Crea l'utente Hiker (discriminator di User con role: "groupLeader")
  const user = await Hiker.create({
    username: defaultUser.username,
    email: defaultUser.email,
    passwordHash,
    isVerified: true, // Per i test, l'utente è già verificato
  });

  // Genera JWT token come nel vero servizio authService
  const token = jwt.sign(
    {
      userId: user._id.toString(),
      role: user.role, // "groupLeader" per Hiker
    },
    JWT_SECRET,
    { expiresIn: "24h" },
  );

  // Restituisci anche la password in chiaro per i test di login
  return {
    user: user.toObject(),
    token,
    password: defaultUser.password,
  };
}

/**
 * Genera un token JWT valido per un userId esistente.
 *
 * @param {string} userId - MongoDB ObjectId dell'utente
 * @param {string} role - Ruolo dell'utente (es. "groupLeader", "rifugio", "admin")
 * @returns {string} Token JWT
 */
export function generateValidToken(userId, role = "groupLeader") {
  return jwt.sign({ userId, role }, JWT_SECRET, { expiresIn: "24h" });
}

/**
 * Genera un token JWT con firma invalida (secret sbagliato).
 *
 * @returns {string} Token JWT non valido
 */
export function generateInvalidToken() {
  return jwt.sign(
    { userId: "fake-user-id", role: "groupLeader" },
    "wrong-secret-key-12345",
    { expiresIn: "24h" },
  );
}

/**
 * Genera un token JWT già scaduto.
 *
 * @returns {string} Token JWT scaduto
 */
export function generateExpiredToken() {
  return jwt.sign(
    { userId: "expired-user-id", role: "groupLeader" },
    JWT_SECRET,
    { expiresIn: "-1h" }, // Scaduto 1 ora fa
  );
}

/**
 * Genera un token JWT malformato (non è un JWT valido).
 *
 * @returns {string} Stringa che non è un JWT
 */
export function generateMalformedToken() {
  return "this-is-not-a-valid-jwt-token";
}

/**
 * Crea un utente User base (non discriminato).
 * Usato raramente, preferire createTestHiker() per i test.
 *
 * @param {Object} userData - Dati utente
 * @returns {Promise<Object>} User creato
 */
export async function createTestUser(userData = {}) {
  const defaultPassword = "TestPassword123!";

  const passwordHash = await bcrypt.hash(
    userData.password || defaultPassword,
    10,
  );

  const user = await User.create({
    username: userData.username || "testuser",
    email: userData.email || "test@example.com",
    passwordHash,
    isVerified: userData.isVerified !== undefined ? userData.isVerified : true,
    role: userData.role || "groupLeader",
  });

  return user.toObject();
}
