import express from "express";

import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  validate,
  createSessionSchema,
  updateSessionSchema,
  updateSessionStatusSchema,
  joinSessionSchema,
  completeSessionSchema,
  statsQuerySchema,
  idParamSchema,
  shareSchema,
  commentSchema,
  activityAndCommentIdParamSchema,
  liveLocationSchema,
  liveLocationsQuerySchema,
  liveTrackingSuspendSchema,
  liveTrackingResumeSchema,
} from "../middleware/validationMiddleware.js";

import {
  createSession,
  getSessionById,
  getSessionsByUser,
  updateSessionStatus,
  updateSessionDetails,
  deleteSession,
  joinSession,
  leaveSession,
  completeSession,
  getActivityStats,
  postLiveLocation,
  getLiveLocations,
  suspendLiveTracking,
  resumeLiveTracking,
} from "../services/hikeSessionService.js";
import { listSessionEmergencies } from "../services/emergencyService.js";
import {
  shareSession,
  unshareSession,
  likeSession,
  unlikeSession,
} from "../services/socialService.js";
import {
  addComment,
  getComments,
  deleteComment,
} from "../services/commentService.js";

const router = express.Router();

// Tutte le route richiedono autenticazione + rate limit per utente.
router.use(authenticate);
router.use(authenticatedLimiter);

// POST /api/v1/sessions — crea una nuova sessione
router.post("/", validate(createSessionSchema), async (req, res, next) => {
  const {
    routeDetails,
    meetingDate,
    meetingTime,
    meetingLocation,
    maxParticipants,
    minExperienceLevel,
    gpxFileName,
    gpxStats,
    sentieroCode,
    plannedRoute,
  } = req.body;

  if (!routeDetails || !routeDetails.name) {
    return res.status(400).json({ error: "routeDetails.name obbligatorio" });
  }

  try {
    const sessionMeta = {
      ...(meetingDate && { meetingDate }),
      ...(meetingTime && { meetingTime }),
      ...(meetingLocation && { meetingLocation }),
      ...(maxParticipants && { maxParticipants }),
      ...(minExperienceLevel && { minExperienceLevel }),
      ...(gpxFileName && { gpxFileName }),
      ...(gpxStats && { gpxStats }),
      ...(sentieroCode && { sentieroCode }),
      ...(plannedRoute && { plannedRoute }),
    };
    const session = await createSession(
      req.user.userId,
      routeDetails,
      sessionMeta,
    );
    res.status(201).json(session);
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/sessions/:id/live-location — upload posizione live (self)
router.post(
  "/:id/live-location",
  validate(idParamSchema, "params"),
  validate(liveLocationSchema),
  async (req, res, next) => {
    /* 
      #swagger.tags = ['Sessions']
      #swagger.description = 'Upload della posizione live (last known) dell’utente chiamante. Richiede sessione ACTIVE e utente non sospeso.'
      #swagger.parameters['id'] = { description: 'Session ID', required: true, type: 'string' }
      #swagger.requestBody = {
        required: true,
        content: {
          "application/json": {
            schema: {
              type: "object",
              required: ["lat","lon"],
              properties: {
                lat: { type: "number", example: 46.07 },
                lon: { type: "number", example: 11.12 },
                accuracyM: { type: "number", example: 8.5 },
                timestampMs: { type: "integer", example: 1716900000000 }
              }
            }
          }
        }
      }
      #swagger.responses[200] = { description: 'OK' }
      #swagger.responses[403] = { description: 'Forbidden (NOT_IN_SESSION / LIVE_TRACKING_SUSPENDED)' }
      #swagger.responses[404] = { description: 'Session not found' }
      #swagger.responses[409] = { description: 'Conflict (SESSION_NOT_ACTIVE)' }
    */
    try {
      const result = await postLiveLocation(
        req.params.id,
        req.user.userId,
        req.body,
      );
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// GET /api/v1/sessions/:id/live-locations — fetch posizioni live (polling)
router.get(
  "/:id/live-locations",
  validate(idParamSchema, "params"),
  validate(liveLocationsQuerySchema, "query"),
  async (req, res, next) => {
    /* 
      #swagger.tags = ['Sessions']
      #swagger.description = 'Fetch delle posizioni live dei partecipanti (esclude suspended e posizioni stale oltre maxAgeSec).'
      #swagger.parameters['id'] = { description: 'Session ID', required: true, type: 'string' }
      #swagger.parameters['maxAgeSec'] = { in: 'query', description: 'Età massima location in secondi', required: false, type: 'integer', example: 30 }
      #swagger.responses[200] = { description: 'OK' }
      #swagger.responses[403] = { description: 'Forbidden (NOT_IN_SESSION)' }
      #swagger.responses[404] = { description: 'Session not found' }
    */
    try {
      const result = await getLiveLocations(
        req.params.id,
        req.user.userId,
        req.query,
      );
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// POST /api/v1/sessions/:id/live-tracking/suspend — sospendi utente
router.post(
  "/:id/live-tracking/suspend",
  validate(idParamSchema, "params"),
  validate(liveTrackingSuspendSchema),
  async (req, res, next) => {
    /* 
      #swagger.tags = ['Sessions']
      #swagger.description = 'Sospende il live tracking di un utente: non può più caricare e non compare nel feed live. Solo Capogruppo.'
      #swagger.parameters['id'] = { description: 'Session ID', required: true, type: 'string' }
      #swagger.requestBody = {
        required: true,
        content: {
          "application/json": {
            schema: {
              type: "object",
              required: ["userId"],
              properties: {
                userId: { type: "string", example: "6650abcdef1234567890abcd" },
                reason: { type: "string", example: "TOO_FAR_FROM_ROUTE" }
              }
            }
          }
        }
      }
      #swagger.responses[200] = { description: 'OK' }
      #swagger.responses[400] = { description: 'Bad request (USER_NOT_PARTICIPANT)' }
      #swagger.responses[403] = { description: 'Forbidden (ONLY_CREATOR / NOT_IN_SESSION)' }
      #swagger.responses[404] = { description: 'Session not found' }
    */
    try {
      const result = await suspendLiveTracking(
        req.params.id,
        req.user.userId,
        req.body,
      );
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// POST /api/v1/sessions/:id/live-tracking/resume — riattiva utente (opzionale)
router.post(
  "/:id/live-tracking/resume",
  validate(idParamSchema, "params"),
  validate(liveTrackingResumeSchema),
  async (req, res, next) => {
    /* 
      #swagger.tags = ['Sessions']
      #swagger.description = 'Riattiva il live tracking di un utente sospeso. Solo Capogruppo.'
      #swagger.parameters['id'] = { description: 'Session ID', required: true, type: 'string' }
      #swagger.requestBody = {
        required: true,
        content: {
          "application/json": {
            schema: {
              type: "object",
              required: ["userId"],
              properties: {
                userId: { type: "string", example: "6650abcdef1234567890abcd" }
              }
            }
          }
        }
      }
      #swagger.responses[200] = { description: 'OK' }
      #swagger.responses[400] = { description: 'Bad request (USER_NOT_PARTICIPANT)' }
      #swagger.responses[403] = { description: 'Forbidden (ONLY_CREATOR / NOT_IN_SESSION)' }
      #swagger.responses[404] = { description: 'Session not found' }
    */
    try {
      const result = await resumeLiveTracking(
        req.params.id,
        req.user.userId,
        req.body,
      );
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// POST /api/v1/sessions/join — si unisce a una sessione tramite codice invito
router.post("/join", validate(joinSessionSchema), async (req, res, next) => {
  const { inviteCode } = req.body;

  if (!inviteCode) {
    return res.status(400).json({ error: "inviteCode mancante nel body" });
  }

  try {
    const session = await joinSession(req.user.userId, inviteCode);
    res.status(200).json(session);
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/sessions/stats — statistiche aggregate attività completate (per anno)
// Query: ?year=2026 (default: anno corrente)
router.get(
  "/stats",
  validate(statsQuerySchema, "query"),
  async (req, res, next) => {
    const year = req.query.year || new Date().getFullYear();
    try {
      const stats = await getActivityStats(req.user.userId, year);
      res.status(200).json(stats);
    } catch (err) {
      next(err);
    }
  },
);

// GET /api/v1/sessions/my — sessioni dell'utente loggato
router.get("/my", async (req, res, next) => {
  try {
    const sessions = await getSessionsByUser(req.user.userId);
    res.status(200).json(sessions);
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/sessions/:id/emergencies — SOS attivi per sessione
router.get(
  "/:id/emergencies",
  validate(idParamSchema, "params"),
  async (req, res) => {
    try {
      const result = await listSessionEmergencies(
        req.params.id,
        req.user.userId,
      );
      res.status(200).json(result);
    } catch (err) {
      if (err.message === "SESSION_NOT_FOUND") {
        return res.status(404).json({ error: err.message });
      }
      if (err.message === "FORBIDDEN") {
        return res.status(403).json({ error: err.message });
      }
      res.status(500).json({ error: "Errore recupero emergenze" });
    }
  },
);

// GET /api/v1/sessions/:id — dettaglio singola sessione (solo partecipanti o admin)
router.get(
  "/:id",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const session = await getSessionById(req.params.id);
      if (!session)
        return res.status(404).json({ error: "Sessione non trovata" });

      // Restringi la lettura ai partecipanti (creator incluso) e agli admin.
      // Evita che chiunque con un ObjectId valido possa leggere sessioni altrui.
      const userId = req.user.userId.toString();
      const isAdmin = req.user.role === "admin";
      const creatorId = (
        session.creatorId?._id || session.creatorId
      )?.toString();
      const isParticipant = (session.participants || []).some((p) => {
        const pid = (p.userId?._id || p.userId)?.toString();
        return pid === userId;
      });
      if (!isAdmin && creatorId !== userId && !isParticipant) {
        return res
          .status(403)
          .json({ error: "Non sei autorizzato a vedere questa sessione" });
      }
      res.status(200).json(session);
    } catch (err) {
      // CastError di Mongoose per ObjectId malformato → 400 (semantica diversa
      // dal generic 500 del global handler).
      if (err.name === "CastError")
        return res.status(400).json({ error: "ID non valido" });
      next(err);
    }
  },
);

// PATCH /api/v1/sessions/:id/status — aggiorna stato sessione
router.patch(
  "/:id/status",
  validate(idParamSchema, "params"),
  validate(updateSessionStatusSchema),
  async (req, res, next) => {
    const { status } = req.body;
    try {
      const session = await updateSessionStatus(
        req.params.id,
        req.user.userId,
        status,
      );
      res.status(200).json(session);
    } catch (err) {
      next(err);
    }
  },
);

// PATCH /api/v1/sessions/:id/complete — termina sessione e persiste statistiche reali
router.patch(
  "/:id/complete",
  validate(idParamSchema, "params"),
  validate(completeSessionSchema),
  async (req, res, next) => {
    try {
      const session = await completeSession(
        req.params.id,
        req.user.userId,
        req.body?.actualStats,
      );
      res.status(200).json(session);
    } catch (err) {
      next(err);
    }
  },
);

// POST /api/v1/sessions/:id/leave — abbandona sessione
router.post(
  "/:id/leave",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const session = await leaveSession(req.user.userId, req.params.id);
      res.status(200).json(session);
    } catch (err) {
      next(err);
    }
  },
);

// PATCH /api/v1/sessions/:id — modifica dettagli sessione (solo creator, inviteCode immutabile)
router.patch(
  "/:id",
  validate(idParamSchema, "params"),
  validate(updateSessionSchema),
  async (req, res, next) => {
    try {
      const session = await updateSessionDetails(
        req.params.id,
        req.user.userId,
        req.body,
      );
      res.status(200).json(session);
    } catch (err) {
      next(err);
    }
  },
);

// DELETE /api/v1/sessions/:id — elimina sessione
router.delete(
  "/:id",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      await deleteSession(req.params.id, req.user.userId);
      res.status(200).json({ message: "Sessione eliminata" });
    } catch (err) {
      next(err);
    }
  },
);

// ── Social: share + like (Sprint 2 schermata Social) ───────────────────────

// POST /api/v1/sessions/:id/share — condivide la sessione sul feed (solo creator)
router.post(
  "/:id/share",
  validate(idParamSchema, "params"),
  validate(shareSchema),
  async (req, res, next) => {
    try {
      const result = await shareSession(req.params.id, req.user.userId, {
        caption: req.body?.caption,
      });
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

router.delete(
  "/:id/share",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const result = await unshareSession(req.params.id, req.user.userId);
      res.status(200).json({ message: "Condivisione rimossa.", ...result });
    } catch (err) {
      next(err);
    }
  },
);

router.post(
  "/:id/like",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const result = await likeSession(req.params.id, req.user.userId);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

router.delete(
  "/:id/like",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const result = await unlikeSession(req.params.id, req.user.userId);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

// ── Comments su HikeSession ────────────────────────────────────────────────

router.post(
  "/:id/comments",
  validate(idParamSchema, "params"),
  validate(commentSchema),
  async (req, res, next) => {
    try {
      const comment = await addComment(
        req.params.id,
        "session",
        req.user.userId,
        req.body.text,
      );
      res.status(201).json({ comment });
    } catch (err) {
      next(err);
    }
  },
);

router.get(
  "/:id/comments",
  validate(idParamSchema, "params"),
  async (req, res, next) => {
    try {
      const page = Math.max(1, parseInt(req.query.page, 10) || 1);
      const limit = Math.min(
        100,
        Math.max(1, parseInt(req.query.limit, 10) || 20),
      );
      const result = await getComments(
        req.params.id,
        "session",
        req.user.userId,
        { page, limit },
      );
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
);

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
