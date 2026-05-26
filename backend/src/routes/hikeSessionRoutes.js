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
} from "../services/hikeSessionService.js";

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
    };
    const session = await createSession(req.user.userId, routeDetails, sessionMeta);
    res.status(201).json(session);
  } catch (err) {
    next(err);
  }
});

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
router.get("/stats", validate(statsQuerySchema, "query"), async (req, res, next) => {
  const year = req.query.year || new Date().getFullYear();
  try {
    const stats = await getActivityStats(req.user.userId, year);
    res.status(200).json(stats);
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/sessions/my — sessioni dell'utente loggato
router.get("/my", async (req, res, next) => {
  try {
    const sessions = await getSessionsByUser(req.user.userId);
    res.status(200).json(sessions);
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/sessions/:id — dettaglio singola sessione (solo partecipanti o admin)
router.get("/:id", validate(idParamSchema, "params"), async (req, res, next) => {
  try {
    const session = await getSessionById(req.params.id);
    if (!session)
      return res.status(404).json({ error: "Sessione non trovata" });

    // Restringi la lettura ai partecipanti (creator incluso) e agli admin.
    // Evita che chiunque con un ObjectId valido possa leggere sessioni altrui.
    const userId = req.user.userId.toString();
    const isAdmin = req.user.role === "admin";
    const creatorId = (session.creatorId?._id || session.creatorId)?.toString();
    const isParticipant = (session.participants || []).some((p) => {
      const pid = (p.userId?._id || p.userId)?.toString();
      return pid === userId;
    });
    if (!isAdmin && creatorId !== userId && !isParticipant) {
      return res.status(403).json({ error: "Non sei autorizzato a vedere questa sessione" });
    }
    res.status(200).json(session);
  } catch (err) {
    // CastError di Mongoose per ObjectId malformato → 400 (semantica diversa
    // dal generic 500 del global handler).
    if (err.name === "CastError") return res.status(400).json({ error: "ID non valido" });
    next(err);
  }
});

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
});

// PATCH /api/v1/sessions/:id/complete — termina sessione e persiste statistiche reali
router.patch(
  "/:id/complete",
  validate(idParamSchema, "params"),
  validate(completeSessionSchema),
  async (req, res, next) => {
    try {
      const session = await completeSession(req.params.id, req.user.userId, req.body?.actualStats);
      res.status(200).json(session);
    } catch (err) {
      next(err);
    }
  },
);

// POST /api/v1/sessions/:id/leave — abbandona sessione
router.post("/:id/leave", validate(idParamSchema, "params"), async (req, res, next) => {
  try {
    const session = await leaveSession(req.user.userId, req.params.id);
    res.status(200).json(session);
  } catch (err) {
    next(err);
  }
});

// PATCH /api/v1/sessions/:id — modifica dettagli sessione (solo creator, inviteCode immutabile)
router.patch(
  "/:id",
  validate(idParamSchema, "params"),
  validate(updateSessionSchema),
  async (req, res, next) => {
    try {
      const session = await updateSessionDetails(req.params.id, req.user.userId, req.body);
      res.status(200).json(session);
    } catch (err) {
      next(err);
    }
  },
);

// DELETE /api/v1/sessions/:id — elimina sessione
router.delete("/:id", validate(idParamSchema, "params"), async (req, res, next) => {
  try {
    await deleteSession(req.params.id, req.user.userId);
    res.status(200).json({ message: "Sessione eliminata" });
  } catch (err) {
    next(err);
  }
});

export default router;