import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { requireRoles } from "../middleware/authorizationMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  nfcScanSchema,
  nfcTotemCreateSchema,
} from "../middleware/validationMiddleware.js";
import {
  listTotems,
  scanTotem,
  getNfcHistory,
  createTotem,
} from "../services/nfcService.js";

const router = express.Router();
const mw = [authenticate, authenticatedLimiter];

router.get("/totems", ...mw, async (req, res, next) => {
  try {
    const lon = req.query.lon ? parseFloat(req.query.lon) : undefined;
    const lat = req.query.lat ? parseFloat(req.query.lat) : undefined;
    const maxDistance = req.query.maxDistance
      ? parseInt(req.query.maxDistance)
      : undefined;
    res.json(await listTotems({ lon, lat, maxDistance }));
  } catch (err) {
    next(err);
  }
});

router.post("/scan", ...mw, async (req, res, next) => {
  try {
    const { error, value } = nfcScanSchema.validate(req.body);
    if (error)
      return res.status(422).json({ message: error.details[0].message });
    res.json(await scanTotem(req.user.userId, value));
  } catch (err) {
    next(err);
  }
});

router.get("/history", ...mw, async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page) || 1);
    res.json(await getNfcHistory(req.user.userId, page));
  } catch (err) {
    next(err);
  }
});

// Admin: crea nuovo totem
router.post(
  "/totems",
  authenticate,
  requireRoles("admin"),
  async (req, res, next) => {
    try {
      const { error, value } = nfcTotemCreateSchema.validate(req.body);
      if (error)
        return res.status(422).json({ message: error.details[0].message });
      const { lon, lat, ...rest } = value;
      const totem = await createTotem({
        ...rest,
        location: { type: "Point", coordinates: [lon, lat] },
      });
      res.status(201).json(totem);
    } catch (err) {
      // E11000 duplicate key (tagId univoco) → 409 con messaggio user-friendly.
      // Manteniamo il check qui perché 11000 può essere lanciato da molteplici
      // collection: il messaggio dipende dal contesto.
      if (err.code === 11000)
        return res.status(409).json({ message: "tagId già esistente." });
      next(err);
    }
  },
);

export default router;
