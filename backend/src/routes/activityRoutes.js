/**
 * Route per attività personali (libere) — `/api/v1/activities`.
 *
 * Tutte le route richiedono autenticazione + rate limit per utente. Schema
 * validation Joi su body/params. Authorization a livello service (verifica owner).
 */
import express from "express";

import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  validate,
  createActivitySchema,
  idParamSchema,
} from "../middleware/validationMiddleware.js";

import {
  createActivity,
  getActivitiesByUser,
  getActivityById,
  deleteActivity,
} from "../services/activityService.js";

const router = express.Router();

router.use(authenticate);
router.use(authenticatedLimiter);

// POST /api/v1/activities — crea una nuova attività libera
router.post("/", validate(createActivitySchema), async (req, res, next) => {
  try {
    const activity = await createActivity(req.user.userId, req.body);
    res.status(201).json(activity);
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/activities — lista attività dell'utente
router.get("/", async (req, res, next) => {
  try {
    const activities = await getActivitiesByUser(req.user.userId);
    res.status(200).json(activities);
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/activities/:id — dettaglio singola attività
router.get("/:id", validate(idParamSchema, "params"), async (req, res, next) => {
  try {
    const activity = await getActivityById(req.params.id, req.user.userId);
    res.status(200).json(activity);
  } catch (err) {
    next(err);
  }
});

// DELETE /api/v1/activities/:id — elimina attività (solo proprietario)
router.delete("/:id", validate(idParamSchema, "params"), async (req, res, next) => {
  try {
    await deleteActivity(req.params.id, req.user.userId);
    res.status(200).json({ message: "Attività eliminata" });
  } catch (err) {
    next(err);
  }
});

export default router;
