import express from "express";
import {
  loginUser,
  verifyEmail,
  forgotPassword,
  getResetPasswordForm,
  resetPassword,
} from "../services/authService.js";
import { createHiker } from "../services/hikerService.js";
import { createRefuge } from "../services/refugeService.js";

const router = express.Router();

/**
 * Route di autenticazione e registrazione.
 *
 *   POST  /auth/login                       → login con email/password
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
 */

// Auth core
router.post("/login", loginUser);
router.get("/verify/:token", verifyEmail);
router.post("/forgot-password", forgotPassword);
router.get("/reset-password/:token", getResetPasswordForm);
router.post("/reset-password/:token", resetPassword);

// Registrazione per ruolo (alias semantici)
router.post("/register/hiker", createHiker);
router.post("/register/refuge", createRefuge);

export default router;
