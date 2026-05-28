import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import { getCreditsWithLevel, getCreditHistory } from "../services/creditService.js";
import { getNfcHistory } from "../services/nfcService.js";

const router = express.Router();

router.get("/me/credits", authenticate, authenticatedLimiter, async (req, res, next) => {
  try {
    const data = await getCreditsWithLevel(req.user.userId);
    res.json(data);
  } catch (err) {
    next(err);
  }
});

router.get("/me/credits/history", authenticate, authenticatedLimiter, async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const limit = Math.min(100, Math.max(1, parseInt(req.query.limit) || 20));
    const source = req.query.source || undefined;
    const data = await getCreditHistory(req.user.userId, { page, limit, source });
    res.json(data);
  } catch (err) {
    next(err);
  }
});

router.get("/me/nfc-history", authenticate, authenticatedLimiter, async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page) || 1);
    res.json(await getNfcHistory(req.user.userId, page));
  } catch (err) {
    next(err);
  }
});

export default router;
