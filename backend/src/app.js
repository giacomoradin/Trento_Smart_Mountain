import express from "express";
import swaggerUI from "swagger-ui-express";
import { readFileSync } from "fs";

// Routes per ruolo (post-refactor discriminator)
import authRoutes from "./routes/authRoutes.js";
import hikerRoutes from "./routes/hikerRoutes.js";
import refugeRoutes from "./routes/refugeRoutes.js";
import adminRoutes from "./routes/adminRoutes.js";
import hikeSessionRoutes from "./routes/hikeSessionRoutes.js";
import activityRoutes from "./routes/activityRoutes.js";
import weatherRoutes from "./routes/weatherRoutes.js";
import creditsRoutes from "./routes/creditsRoutes.js";
import quizRoutes from "./routes/quizRoutes.js";
import nfcRoutes from "./routes/nfcRoutes.js";
import accountRoutes from "./routes/accountRoutes.js";
import challengeRoutes from "./routes/challengeRoutes.js";
import badgeRoutes from "./routes/badgeRoutes.js";
import emergencyRoutes from "./routes/emergencyRoutes.js";
import "./models/emergency.js";

// IMPORTANTE: importa i discriminator models per registrarli con Mongoose
// (devono essere caricati almeno una volta perché User.discriminator() venga eseguito)
import "./models/hiker.js";
import "./models/refuge.js";
import "./models/admin.js";
import "./models/creditTransaction.js";
import "./models/quizCategory.js";
import "./models/quiz.js";
import "./models/quizAttempt.js";
import "./models/nfcTotem.js";
import "./models/nfcScan.js";
import "./models/challenge.js";
import "./models/earnedBadge.js";

import { globalErrorHandler, notFoundHandler } from "./middleware/errorMiddleware.js";
import {
  helmetMiddleware,
  mongoSanitizeMiddleware,
  hppMiddleware,
  corsOptions,
  requestSizeLimit,
} from "./middleware/securityMiddleware.js";
import { globalLimiter, authenticatedLimiter, writeLimiter } from "./middleware/rateLimitMiddleware.js";

const swaggerDocument = JSON.parse(
  readFileSync(new URL("../../swagger-output.json", import.meta.url)),
);

const app = express();

// L'app gira dietro al proxy di Render → fidati di X-Forwarded-For per
// avere l'IP del client (altrimenti rate limiter vede sempre l'IP del proxy).
// Limitato al numero di hop reale per evitare spoofing dell'header.
app.set("trust proxy", 1);

// ─── Security middleware (devono essere il PRIMO layer) ──────────────────────
//   Ordine: helmet (header) → sanitize NoSQL → HPP (query) → CORS → rate limit
//   Tutti applicati globalmente prima di qualsiasi parser/route.
app.use(helmetMiddleware);
app.use(corsOptions);

// Normalizza il path collassando slash multipli (es. //auth/verify → /auth/verify).
// Difesa contro link email con BASE_URL trailing slash.
app.use((req, res, next) => {
  if (req.url.match(/\/{2,}/)) {
    const cleanUrl = req.url.replace(/\/{2,}/g, "/");
    console.log(`[app] Path normalizzato: "${req.url}" → "${cleanUrl}"`);
    return res.redirect(301, cleanUrl);
  }
  next();
});

// Body parser con limite di size (anti-DoS payload bomb)
app.use(express.json({ limit: requestSizeLimit }));
app.use(express.urlencoded({ extended: true, limit: requestSizeLimit }));

// Sanitization NoSQL e HPP devono venire DOPO i body parser
app.use(mongoSanitizeMiddleware);
app.use(hppMiddleware);

// Rate limiter globale per IP (applica a tutte le rotte, anche pubbliche)
app.use(globalLimiter);

// Limite scritture (POST/PATCH/DELETE) per utente — più stretto del read rate
app.use(writeLimiter);

// Swagger UI pubblico per l'esplorazione delle API
app.use("/api-docs", swaggerUI.serve, swaggerUI.setup(swaggerDocument));

// Route principali
app.use("/auth", authRoutes);
app.use("/hikers", hikerRoutes);
app.use("/refuges", refugeRoutes);
app.use("/admin", adminRoutes);
app.use("/api/v1/sessions", hikeSessionRoutes);
app.use("/api/v1/emergencies", emergencyRoutes);
app.use("/api/v1/activities", activityRoutes);
app.use("/weather", weatherRoutes);
app.use("/api/v1/users", creditsRoutes);
app.use("/api/v1/quiz", quizRoutes);
app.use("/api/v1/nfc", nfcRoutes);
app.use("/api/v1/users", accountRoutes);
app.use("/api/v1/challenges", challengeRoutes);
app.use("/api/v1/users/me", badgeRoutes);

// ─── Compatibility shim: /users (deprecato, mantenuto per backward-compat) ───
// Il refactor 2026-05 ha separato la collection in hikers/refuges/admins ma
// alcuni client (es. ProfileViewModel) cercano ancora /users/:id senza
// conoscere a priori il ruolo. Smistiamo internamente verso lo schema corretto.
import { authenticate as _authShim } from "./middleware/authMiddleware.js";
import User from "./models/user.js";
import { stripPrivateFields, isSelfOrAdmin } from "./utils/userPrivacy.js";

app.post("/users", async (req, res, next) => {
  const role = req.body?.role;
  console.warn(`[app] DEPRECATION: POST /users (role=${role}) → usare /auth/register/hiker o /refuge`);
  try {
    if (role === "rifugio") {
      const { createRefuge } = await import("./services/refugeService.js");
      // Mappa il vecchio formato annidato { rifugioDetails: {...} } al nuovo flat
      if (req.body.rifugioDetails) {
        Object.assign(req.body, req.body.rifugioDetails);
      }
      return createRefuge(req, res);
    }
    const { createHiker } = await import("./services/hikerService.js");
    return createHiker(req, res);
  } catch (err) {
    next(err);
  }
});

app.get("/users/:id", _authShim, async (req, res) => {
  try {
    const user = await User.findById(req.params.id).select("-passwordHash -__v");
    if (!user) return res.status(404).json({ message: "Utente non trovato." });
    // Privacy gate: dati personali (peso, sesso, preferenze, etc.) visibili
    // solo al proprietario o ad admin. Senza questo strip un utente
    // qualunque potrebbe leggere weightKg/birthDate altrui via /users/:id.
    const safe = stripPrivateFields(user, isSelfOrAdmin(req.user, req.params.id));
    res.status(200).json(safe);
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "ID utente non valido." });
    }
    res.status(500).json({ message: error.message });
  }
});

app.use(notFoundHandler);
app.use(globalErrorHandler);

export default app;
