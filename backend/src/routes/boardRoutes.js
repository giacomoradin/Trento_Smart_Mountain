/**
 * Route Bacheca rifugi — montate sotto `/api/v1/board`.
 *
 *   GET    /api/v1/board        → feed bacheca (tutti gli utenti autenticati)
 *   GET    /api/v1/board/mine   → post del rifugio loggato (gestione)
 *   POST   /api/v1/board        → crea post (solo rifugio/admin)
 *   DELETE /api/v1/board/:id    → elimina (autore o admin)
 *
 * Validazione input: lunghezze + whitelist `type` applicate nel boardService
 * (titolo ≤120, testo ≤2000, type ∈ {info,avviso,pericolo}); la sanitizzazione
 * globale (mongo-sanitize, hpp, size-limit) è già attiva a monte in app.js.
 */
import express from "express";

import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  createBoardPost,
  listBoardPosts,
  getMyBoardPosts,
  deleteBoardPost,
} from "../services/boardService.js";

const router = express.Router();

router.use(authenticate);
router.use(authenticatedLimiter);

/** GET / — feed bacheca consultabile da tutti gli escursionisti. */
router.get("/", async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));
    const type = req.query.type || null;
    const activeOnly = req.query.activeOnly !== "false";
    const result = await listBoardPosts({ page, limit, type, activeOnly });
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

/** GET /mine — post pubblicati dal rifugio loggato. */
router.get("/mine", async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));
    const result = await getMyBoardPosts(req.user.userId, { page, limit });
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

/** POST / — crea un post in bacheca (solo account rifugio/admin). */
router.post("/", async (req, res, next) => {
  try {
    const { type, title, body, validUntil } = req.body || {};
    const post = await createBoardPost(req.user.userId, req.user.role, {
      type,
      title,
      body,
      validUntil,
    });
    res.status(201).json(post);
  } catch (err) {
    next(err);
  }
});

/** DELETE /:id — elimina un post (autore o admin). */
router.delete("/:id", async (req, res, next) => {
  try {
    const result = await deleteBoardPost(req.params.id, req.user.userId, {
      isAdmin: req.user.role === "admin",
    });
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

export default router;
