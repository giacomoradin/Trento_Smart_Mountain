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
  getActivityStats,
  updateSessionStatus,
  updateSessionDetails,
  deleteSession,
  joinSession,
  leaveSession,
  completeSession,
} from "../services/hikeSessionService.js";

const router = express.Router();

// Tutte le route richiedono autenticazione + rate limit per utente.
router.use(authenticate);
router.use(authenticatedLimiter);

// POST /api/v1/sessions — crea una nuova sessione
router.post("/", validate(createSessionSchema), async (req, res) => {
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
    if (err.message === "USER_ALREADY_IN_SESSION")
      return res.status(409).json({ error: "Hai una sessione attualmente in corso (tracciamento ATTIVO). Concludila prima di crearne un'altra." });
    res.status(500).json({ error: "Errore creazione sessione" });
  }
});

// POST /api/v1/sessions/join — si unisce a una sessione tramite codice invito
router.post("/join", validate(joinSessionSchema), async (req, res) => {
  const { inviteCode } = req.body;

  if (!inviteCode) {
    return res.status(400).json({ error: "inviteCode mancante nel body" });
  }

  try {
    const session = await joinSession(req.user.userId, inviteCode);
    res.status(200).json(session);
  } catch (err) {
    if (err.message === "USER_ALREADY_IN_SESSION")
      return res.status(409).json({ error: "Hai una sessione attualmente in corso (tracciamento ATTIVO). Concludila prima di unirti a una nuova." });
    if (err.message === "SESSION_NOT_FOUND")
      return res.status(404).json({ error: "Codice invito non valido" });
    if (err.message === "SESSION_NOT_JOINABLE")
      return res.status(409).json({ error: "La sessione non è più aperta" });
    if (err.message === "ALREADY_IN_SESSION")
      return res.status(409).json({ error: "Sei già in questa sessione" });
    res.status(500).json({ error: "Errore durante l'accesso alla sessione" });
  }
});

// GET /api/v1/sessions/stats — statistiche aggregate attività completate (per anno)
// Query: ?year=2026 (default: anno corrente)
router.get("/stats", validate(statsQuerySchema, "query"), async (req, res) => {
  const year = req.query.year || new Date().getFullYear();
  try {
    const stats = await getActivityStats(req.user.userId, year);
    res.status(200).json(stats);
  } catch (err) {
    res.status(500).json({ error: "Errore nel calcolo statistiche" });
  }
});

// GET /api/v1/sessions/my — sessioni dell'utente loggato
router.get("/my", async (req, res) => {
  try {
    const sessions = await getSessionsByUser(req.user.userId);
    res.status(200).json(sessions);
  } catch (err) {
    res.status(500).json({ error: "Errore recupero sessioni" });
  }
});

// GET /api/v1/sessions/:id — dettaglio singola sessione
router.get("/:id", validate(idParamSchema, "params"), async (req, res) => {
  try {
    const session = await getSessionById(req.params.id);
    if (!session)
      return res.status(404).json({ error: "Sessione non trovata" });
    res.status(200).json(session);
  } catch (err) {
    res.status(400).json({ error: "ID non valido" });
  }
});

// PATCH /api/v1/sessions/:id/status — aggiorna stato sessione
router.patch(
  "/:id/status",
  validate(idParamSchema, "params"),
  validate(updateSessionStatusSchema),
  async (req, res) => {
  const { status } = req.body;
  try {
    const session = await updateSessionStatus(
      req.params.id,
      req.user.userId,
      status,
    );
    res.status(200).json(session);
  } catch (err) {
    if (err.message === "SESSION_NOT_FOUND")
      return res.status(404).json({ error: "Sessione non trovata" });
    if (err.message === "FORBIDDEN")
      return res
        .status(403)
        .json({ error: "Solo il Capogruppo può modificare la sessione" });
    res.status(500).json({ error: "Errore aggiornamento stato" });
  }
});

// PATCH /api/v1/sessions/:id/complete — termina sessione e persiste statistiche reali
router.patch(
  "/:id/complete",
  validate(idParamSchema, "params"),
  validate(completeSessionSchema),
  async (req, res) => {
    try {
      const session = await completeSession(req.params.id, req.user.userId, req.body?.actualStats);
      res.status(200).json(session);
    } catch (err) {
      if (err.message === "SESSION_NOT_FOUND") return res.status(404).json({ error: "Sessione non trovata" });
      if (err.message === "FORBIDDEN") return res.status(403).json({ error: "Non sei autorizzato a completare questa sessione" });
      res.status(500).json({ error: "Errore completamento sessione" });
    }
  },
);

// POST /api/v1/sessions/:id/leave — abbandona sessione
router.post("/:id/leave", validate(idParamSchema, "params"), async (req, res) => {
  try {
    const session = await leaveSession(req.user.userId, req.params.id);
    res.status(200).json(session);
  } catch (err) {
    if (err.message === "SESSION_NOT_FOUND") return res.status(404).json({ error: "Sessione non trovata" });
    if (err.message === "CREATOR_CANNOT_LEAVE") return res.status(403).json({ error: "Il Capogruppo non può abbandonare la sessione. Eliminala se vuoi rimuoverla." });
    res.status(500).json({ error: "Errore durante l'abbandono della sessione" });
  }
});

// PATCH /api/v1/sessions/:id — modifica dettagli sessione (solo creator, inviteCode immutabile)
router.patch(
  "/:id",
  validate(idParamSchema, "params"),
  validate(updateSessionSchema),
  async (req, res) => {
    try {
      const session = await updateSessionDetails(req.params.id, req.user.userId, req.body);
      res.status(200).json(session);
    } catch (err) {
      if (err.message === "SESSION_NOT_FOUND") return res.status(404).json({ error: "Sessione non trovata" });
      if (err.message === "FORBIDDEN") return res.status(403).json({ error: "Solo il Capogruppo può modificare la sessione" });
      res.status(500).json({ error: "Errore aggiornamento sessione" });
    }
  },
);

// DELETE /api/v1/sessions/:id — elimina sessione
router.delete("/:id", validate(idParamSchema, "params"), async (req, res) => {
  try {
    await deleteSession(req.params.id, req.user.userId);
    res.status(200).json({ message: "Sessione eliminata" });
  } catch (err) {
    if (err.message === "SESSION_NOT_FOUND")
      return res.status(404).json({ error: "Sessione non trovata" });
    if (err.message === "FORBIDDEN")
      return res
        .status(403)
        .json({ error: "Solo il Capogruppo può eliminare la sessione" });
    res.status(500).json({ error: "Errore eliminazione sessione" });
  }
});

export default router;