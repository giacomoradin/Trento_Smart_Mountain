/**
 * Route Rifiuti & Logistica del rifugio (ADR-002, MVP) — montate sotto
 * `/api/v1/refuge/waste`.
 *
 *   GET  /api/v1/refuge/waste/config   → categorie, vettori, limiti normativi
 *   POST /api/v1/refuge/waste/simulate → bilancio di massa + compliance + costi vettori
 *
 * MVP read-only: nessuna persistenza, solo calcolo. Accesso riservato a
 * rifugisti e admin (Access Control Matrix).
 */
import express from "express";

import { authenticate } from "../middleware/authMiddleware.js";
import { requireRoles } from "../middleware/authorizationMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import { validate, wasteSimulationSchema } from "../middleware/validationMiddleware.js";
import { getWasteConfig, simulateWaste } from "../services/wasteService.js";

const router = express.Router();

router.use(authenticate);
router.use(authenticatedLimiter);
router.use(requireRoles("rifugio", "admin"));

/** GET /api/v1/refuge/waste/config — configurazione del simulatore. */
router.get("/config", (req, res) => {
  res.status(200).json(getWasteConfig());
});

/** POST /api/v1/refuge/waste/simulate — esegue la simulazione stagionale. */
router.post("/simulate", validate(wasteSimulationSchema), (req, res, next) => {
  try {
    res.status(200).json(simulateWaste(req.body));
  } catch (err) {
    next(err);
  }
});

export default router;
