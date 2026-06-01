/**
 * Route Dashboard IoT del rifugio — montate sotto `/api/v1/refuge`.
 *
 *   GET /api/v1/refuge/dashboard → sensori + edge nodes + passaggi del rifugio loggato
 *
 * Sprint mockup: i dati sono generati lato server (vedi refugeIotService).
 */
import express from "express";

import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import { getRefugeDashboard } from "../services/refugeIotService.js";

const router = express.Router();

router.use(authenticate);
router.use(authenticatedLimiter);

/** GET /api/v1/refuge/dashboard — dashboard IoT del rifugio loggato. */
router.get("/dashboard", async (req, res, next) => {
  try {
    const result = await getRefugeDashboard(req.user.userId);
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

export default router;
