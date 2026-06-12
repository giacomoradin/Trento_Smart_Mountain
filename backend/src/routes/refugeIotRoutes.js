/**
 * Route Dashboard IoT del rifugio — montate sotto `/api/v1/refuge`.
 *
 *   GET /api/v1/refuge/dashboard → sensori + edge nodes + passaggi del rifugio loggato
 *
 * Sprint mockup: i dati sono generati lato server (vedi refugeIotService).
 */
import express from "express";

import { authenticate } from "../middleware/authMiddleware.js";
import { requireRoles } from "../middleware/authorizationMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  validate,
  refugeProfileUpdateSchema,
} from "../middleware/validationMiddleware.js";
import {
  getRefugeDashboard,
  updateRefugeProfile,
} from "../services/refugeIotService.js";

const router = express.Router();

router.use(authenticate);
router.use(authenticatedLimiter);
// La dashboard IoT espone telemetria operativa del rifugio: accesso riservato
// al rifugista loggato (e admin), come da Access Control Matrix.
router.use(requireRoles("rifugio", "admin"));

/** GET /api/v1/refuge/dashboard — dashboard IoT del rifugio loggato. */
router.get("/dashboard", async (req, res, next) => {
  try {
    const result = await getRefugeDashboard(req.user.userId);
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

/** PATCH /api/v1/refuge/profile — aggiorna la foto della struttura (Joi + role gate). */
router.patch(
  "/profile",
  validate(refugeProfileUpdateSchema),
  async (req, res, next) => {
    try {
      const result = await updateRefugeProfile(req.user.userId, req.body);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

export default router;
