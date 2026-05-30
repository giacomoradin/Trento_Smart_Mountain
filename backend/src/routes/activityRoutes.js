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
  shareSchema,
  commentSchema,
  activityAndCommentIdParamSchema,
} from "../middleware/validationMiddleware.js";

import {
  createActivity,
  getActivitiesByUser,
  getActivityById,
  deleteActivity,
} from "../services/activityService.js";
import {
  shareActivity,
  unshareActivity,
  likeActivity,
  unlikeActivity,
} from "../services/socialService.js";
import {
  addComment,
  getComments,
  deleteComment,
} from "../services/commentService.js";

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
router.get(
  "/:id",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const activity = await getActivityById(req.params.id, req.user.userId);
      res.status(200).json(activity);
    } catch (err) {
      next(err);
    }
  },
);

// DELETE /api/v1/activities/:id — elimina attività (solo proprietario)
router.delete(
  "/:id",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      await deleteActivity(req.params.id, req.user.userId);
      res.status(200).json({ message: "Attività eliminata" });
    } catch (err) {
      next(err);
    }
  },
);

// ── Social: share + like (Sprint 2 schermata Social) ───────────────────────

// POST /api/v1/activities/:id/share — condividi attività sul feed
// Body opzionale: { caption }. Idempotente: re-share aggiorna sharedAt.
router.post(
  "/:id/share",
  validate(idParamSchema, "params"),
  validate(shareSchema),
  async (req, res, next) => {
    try {
      const result = await shareActivity(req.params.id, req.user.userId, {
        caption: req.body?.caption,
      });
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// DELETE /api/v1/activities/:id/share — rimuovi dalla condivisione
router.delete(
  "/:id/share",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const result = await unshareActivity(req.params.id, req.user.userId);
      res.status(200).json({ message: "Condivisione rimossa.", ...result });
    } catch (err) {
      next(err);
    }
  },
);

// POST /api/v1/activities/:id/like — like idempotente
router.post(
  "/:id/like",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const result = await likeActivity(req.params.id, req.user.userId);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// DELETE /api/v1/activities/:id/like — rimuovi like idempotente
router.delete(
  "/:id/like",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const result = await unlikeActivity(req.params.id, req.user.userId);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// ── Comments ──────────────────────────────────────────────────────────────

// POST /api/v1/activities/:id/comments — aggiungi commento
router.post(
  "/:id/comments",
  validate(idParamSchema, "params"),
  validate(commentSchema),
  async (req, res, next) => {
    try {
      const comment = await addComment(
        req.params.id,
        "activity",
        req.user.userId,
        req.body.text,
      );
      res.status(201).json({ comment });
    } catch (err) {
      next(err);
    }
  },
);

// GET /api/v1/activities/:id/comments?page=&limit= — lista paginata
router.get(
  "/:id/comments",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const page = Math.max(1, parseInt(req.query.page, 10) || 1);
      const limit = Math.min(100, Math.max(1, parseInt(req.query.limit, 10) || 20));
      const result = await getComments(
        req.params.id,
        "activity",
        req.user.userId,
        { page, limit },
      );
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// DELETE /api/v1/activities/:id/comments/:cid — cancella commento (solo author)
router.delete(
  "/:id/comments/:cid",
  validate(activityAndCommentIdParamSchema, "params"),
  async (req, res, next) => {
    try {
      const isAdmin = req.user?.role === "admin";
      await deleteComment(req.params.cid, req.user.userId, { isAdmin });
      res.status(200).json({ message: "Commento eliminato." });
    } catch (err) {
      next(err);
    }
  },
);

export default router;
