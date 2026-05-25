import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import { listMyBadges, listMyCertificates, evaluateAllBadges } from "../services/badgeService.js";

const router = express.Router();
const mw = [authenticate, authenticatedLimiter];

router.use(...mw);

/** Tutti i badge del catalogo con flag `earned` per l'utente corrente. */
router.get("/badges", async (req, res, next) => {
  try {
    res.json(await listMyBadges(req.user.userId));
  } catch (err) {
    next(err);
  }
});

/** Certificati: 1 per quiz-category interamente completata. */
router.get("/certificates", async (req, res, next) => {
  try {
    res.json(await listMyCertificates(req.user.userId));
  } catch (err) {
    next(err);
  }
});

/**
 * Trigger esplicito di re-evaluation di tutti i badge. Utile per:
 *  - retroattività dopo l'aggiunta di nuovi badge al catalogo
 *  - debug / supporto utente
 * Idempotente (unique index su EarnedBadge).
 */
router.post("/badges/evaluate", async (req, res, next) => {
  try {
    const newlyEarned = await evaluateAllBadges(req.user.userId);
    res.json({ newlyEarned });
  } catch (err) {
    next(err);
  }
});

export default router;
