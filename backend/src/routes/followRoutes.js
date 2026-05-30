/**
 * Route per la gestione del follow asimmetrico — montate sotto `/api/v1/users`.
 *
 * Path pubblici (autenticazione richiesta tramite middleware globale):
 *   POST   /api/v1/users/:id/follow
 *   DELETE /api/v1/users/:id/follow
 *   GET    /api/v1/users/me/following
 *   GET    /api/v1/users/me/followers
 *   GET    /api/v1/users/:id/follow-stats
 *
 * Nota di mounting: `app.use("/api/v1/users", followRoutes)` viene fatto
 * DOPO le altre route /api/v1/users (creditsRoutes, accountRoutes) — Express
 * gestisce tutto via routing first-match-wins; i path qui sono unici e non
 * sovrapposti agli altri.
 */
import express from "express";

import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  validate,
  followIdParamSchema,
} from "../middleware/validationMiddleware.js";

import {
  followUser,
  unfollowUser,
  getFollowing,
  getFollowers,
  getFollowStats,
} from "../services/followService.js";
import {
  getFeedForUser,
  getPostsByUser,
  getSocialRowForUser,
} from "../services/socialService.js";

const router = express.Router();

router.use(authenticate);
router.use(authenticatedLimiter);

// ── Me-relative: precedono i :id-relative per priorità di matching ──────

/** GET /api/v1/users/me/following — lista paginata di chi seguo. */
router.get("/me/following", async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const limit = Math.min(100, Math.max(1, parseInt(req.query.limit, 10) || 20));
    const result = await getFollowing(req.user.userId, { page, limit });
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

/** GET /api/v1/users/me/followers — lista paginata di chi mi segue. */
router.get("/me/followers", async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const limit = Math.min(100, Math.max(1, parseInt(req.query.limit, 10) || 20));
    const result = await getFollowers(req.user.userId, { page, limit });
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/v1/users/me/feed — feed sociale paginato.
 *
 * Aggrega Activity e HikeSession condivise (sharedAt != null) degli utenti
 * seguiti + me stesso, ordinate per sharedAt desc. Vedi
 * `socialService.getFeedForUser` per i dettagli del merge JS-side.
 *
 * Query params:
 *   - page  (default 1, min 1)
 *   - limit (default 20, max 50)
 *
 * Response:
 *   { items: FeedItem[], hasMore: boolean }
 */
router.get("/me/feed", async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));
    const result = await getFeedForUser(req.user.userId, { page, limit });
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/v1/users/me/social-row — avatar row in cima alla HomeSocialScreen.
 *
 * Per ogni utente seguito calcola uno status in priorità (live > story > goal
 * > neutral). Vedi `socialService.getSocialRowForUser` per il dettaglio.
 *
 * Refresh suggerito ogni 30s mentre la tab Social è attiva (lo stato "live"
 * deve essere fresco). La UI tipicamente fa polling oppure pull-to-refresh.
 */
router.get("/me/social-row", async (req, res, next) => {
  try {
    const result = await getSocialRowForUser(req.user.userId);
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});

// ── Target-specific: precedono :id "naked" ─────────────────────────────

/** GET /api/v1/users/:id/follow-stats — counts + isFollowedByMe per il bottone. */
router.get(
  "/:id/follow-stats",
  validate(followIdParamSchema, "params"),
  async (req, res, next) => {
    try {
      const stats = await getFollowStats(req.params.id, req.user.userId);
      res.status(200).json(stats);
    } catch (err) {
      next(err);
    }
  },
);

/**
 * GET /api/v1/users/:id/posts — bacheca pubblica di un singolo utente.
 *
 * Viewer == author: vede anche i post non condivisi (diary completo).
 * Viewer != author: vede solo `sharedAt != null` (privacy gate).
 *
 * Stessa shape di `/me/feed` per coerenza UI (FeedCard riusabile).
 */
router.get(
  "/:id/posts",
  validate(followIdParamSchema, "params"),
  async (req, res, next) => {
    try {
      const page = Math.max(1, parseInt(req.query.page, 10) || 1);
      const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));
      const result = await getPostsByUser(req.params.id, req.user.userId, { page, limit });
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

/** POST /api/v1/users/:id/follow — segui un utente. Idempotente. */
router.post(
  "/:id/follow",
  validate(followIdParamSchema, "params"),
  async (req, res, next) => {
    try {
      await followUser(req.user.userId, req.params.id);
      res.status(201).json({ message: "Ora segui questo utente." });
    } catch (err) {
      next(err);
    }
  },
);

/** DELETE /api/v1/users/:id/follow — smetti di seguire. */
router.delete(
  "/:id/follow",
  validate(followIdParamSchema, "params"),
  async (req, res, next) => {
    try {
      await unfollowUser(req.user.userId, req.params.id);
      res.status(200).json({ message: "Hai smesso di seguire l'utente." });
    } catch (err) {
      next(err);
    }
  },
);

export default router;
