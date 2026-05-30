import express from "express";
import {
  loginUser,
  verifyEmail,
  forgotPassword,
  getResetPasswordForm,
  resetPassword,
  refreshTokens,
  logout,
} from "../services/authService.js";
import { createHiker } from "../services/hikerService.js";
import { createRefuge } from "../services/refugeService.js";
import {
  loginLimiter,
  registerLimiter,
  passwordResetLimiter,
} from "../middleware/rateLimitMiddleware.js";
import {
  validate,
  loginSchema,
  registerHikerSchema,
  registerRefugeSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
} from "../middleware/validationMiddleware.js";

const router = express.Router();

/**
 * Route di autenticazione e registrazione.
 *
 *   POST  /auth/login                       → login con email/password
 *   POST  /auth/refresh                     → rota access+refresh (rotation chain)
 *   POST  /auth/logout                      → revoca refresh token
 *   GET   /auth/verify/:token               → conferma email da link → deep link tsm://
 *   POST  /auth/forgot-password             → invio link reset password
 *   GET   /auth/reset-password/:token       → form HTML reset password
 *   POST  /auth/reset-password/:token       → submit nuova password (JSON o form)
 *
 *   POST  /auth/register/hiker              → registrazione escursionista
 *   POST  /auth/register/refuge             → registrazione rifugio
 *
 * Le registrazioni sono accessibili anche tramite POST /hikers e POST /refuges
 * (route role-specific). Qui sotto /auth/* per semantica "auth flow".
 *
 * Note refresh token (audit 2026-05):
 *  - login risponde con { token (alias backward-compat), accessToken,
 *    refreshToken, refreshExpiresAt }.
 *  - /refresh accetta { refreshToken } e risponde con una nuova coppia
 *    ruotata. Il vecchio refresh viene revocato.
 *  - /logout accetta { refreshToken } e lo revoca. L'access token JWT è
 *    stateless e scadrà entro JWT_ACCESS_TTL (default 15m).
 *  - Detection replay: se un refresh già revocato viene riutilizzato, tutta
 *    la family chain viene revocata (vedi refreshTokenService.rotateRefreshToken).
 */

// Auth core — rate limit + schema validation per ogni endpoint sensibile.
router.post("/login", loginLimiter, validate(loginSchema), loginUser);
// /refresh: niente schema Joi pesante (body minimo). Rate limit con loginLimiter
// per evitare brute-force di refresh token (96-char hex, ma meglio safe).
router.post("/refresh", loginLimiter, refreshTokens);
router.post("/logout", logout);
router.get("/verify/:token", verifyEmail);
router.post(
  "/forgot-password",
  passwordResetLimiter,
  validate(forgotPasswordSchema),
  forgotPassword,
);
router.get("/reset-password/:token", getResetPasswordForm);
// resetPassword accetta sia JSON che form HTML — la validazione Joi gestisce entrambi
// (Content-Type "application/x-www-form-urlencoded" è parsato in req.body uguale al JSON).
router.post(
  "/reset-password/:token",
  passwordResetLimiter,
  validate(resetPasswordSchema),
  resetPassword,
);

// Registrazione per ruolo (alias semantici) — limitatore registrazioni + schema specifico
router.post(
  "/register/hiker",
  registerLimiter,
  validate(registerHikerSchema),
  createHiker,
);
router.post(
  "/register/refuge",
  registerLimiter,
  validate(registerRefugeSchema),
  createRefuge,
);

export default router;
