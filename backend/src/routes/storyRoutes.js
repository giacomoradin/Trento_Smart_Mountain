import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  validate,
  createStorySchema,
  idParamSchema,
  userIdParamSchema,
} from "../middleware/validationMiddleware.js";
import {
  createStory,
  getStoriesByAuthor,
  markStoryViewed,
  deleteStory,
  getStoryById,
} from "../services/storyService.js";

const router = express.Router();

// Tutte le route richiedono autenticazione + rate limit per utente.
router.use(authenticate);
router.use(authenticatedLimiter);

// POST /api/v1/stories — crea una storia (foto/video brevi + overlay tracciamento)
router.post("/", validate(createStorySchema), async (req, res, next) => {
  /*
    #swagger.tags = ['Stories']
    #swagger.description = 'Crea una storia (24h). type planned_session (con sessionId + link join) o activity (preview post-hike). Media foto/video Base64 capped.'
  */
  try {
    const story = await createStory(req.user.userId, req.body);
    res.status(201).json(story);
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/stories/user/:userId — tutte le storie non scadute di un autore
router.get(
  "/user/:userId",
  validate(userIdParamSchema, "params"),
  async (req, res, next) => {
    /*
      #swagger.tags = ['Stories']
      #swagger.description = 'Storie non scadute di un autore, ordine cronologico, con media e overlay. Applica il gate di visibilità.'
    */
    try {
      const result = await getStoriesByAuthor(req.user.userId, req.params.userId);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// GET /api/v1/stories/:id — singola storia (deep-link)
router.get("/:id", validate(idParamSchema, "params"), async (req, res, next) => {
  /*
    #swagger.tags = ['Stories']
    #swagger.description = 'Recupera una singola storia per id (deep-link da notifica).'
  */
  try {
    const story = await getStoryById(req.user.userId, req.params.id);
    res.status(200).json(story);
  } catch (err) {
    if (err.name === "CastError") return res.status(400).json({ error: "ID non valido" });
    next(err);
  }
});

// POST /api/v1/stories/:id/view — marca come vista
router.post(
  "/:id/view",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    /*
      #swagger.tags = ['Stories']
      #swagger.description = 'Marca la storia come vista dal viewer (idempotente).'
    */
    try {
      const result = await markStoryViewed(req.params.id, req.user.userId);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// DELETE /api/v1/stories/:id — elimina (solo autore)
router.delete(
  "/:id",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    /*
      #swagger.tags = ['Stories']
      #swagger.description = 'Elimina una storia. Riservato all'autore.'
    */
    try {
      const result = await deleteStory(req.params.id, req.user.userId);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

export default router;
