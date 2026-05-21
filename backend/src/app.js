import express from "express";
import cors from "cors";
import swaggerUI from "swagger-ui-express";
import { readFileSync } from "fs";

// Routes per ruolo (post-refactor discriminator)
import authRoutes from "./routes/authRoutes.js";
import hikerRoutes from "./routes/hikerRoutes.js";
import refugeRoutes from "./routes/refugeRoutes.js";
import adminRoutes from "./routes/adminRoutes.js";
import hikeSessionRoutes from "./routes/hikeSessionRoutes.js";
import weatherRoutes from "./routes/weatherRoutes.js";

// IMPORTANTE: importa i discriminator models per registrarli con Mongoose
// (devono essere caricati almeno una volta perché User.discriminator() venga eseguito)
import "./models/hiker.js";
import "./models/refuge.js";
import "./models/admin.js";

import { globalErrorHandler, notFoundHandler } from "./middleware/errorMiddleware.js";

const swaggerDocument = JSON.parse(
  readFileSync(new URL("../../swagger-output.json", import.meta.url)),
);

const app = express();

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

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cors());

// Swagger UI pubblico per l'esplorazione delle API
app.use("/api-docs", swaggerUI.serve, swaggerUI.setup(swaggerDocument));

// Route principali
app.use("/auth", authRoutes);
app.use("/hikers", hikerRoutes);
app.use("/refuges", refugeRoutes);
app.use("/admin", adminRoutes);
app.use("/api/v1/sessions", hikeSessionRoutes);
app.use("/weather", weatherRoutes);

// ─── Compatibility shim: /users (deprecato, mantenuto per backward-compat) ───
// Il refactor 2026-05 ha separato la collection in hikers/refuges/admins ma
// alcuni client (es. ProfileViewModel) cercano ancora /users/:id senza
// conoscere a priori il ruolo. Smistiamo internamente verso lo schema corretto.
import { authenticate as _authShim } from "./middleware/authMiddleware.js";
import User from "./models/user.js";

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
    res.status(200).json(user);
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
