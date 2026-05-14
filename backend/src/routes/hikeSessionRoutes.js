import express from "express";

import { authenticate } from "../middleware/authMiddleware.js";

import {
  createSession,
  getSessionById,
  getSessionsByUser,
  updateSessionStatus,
  updateSessionDetails,
  deleteSession,
  joinSession,
  leaveSession,
} from "../services/hikeSessionService.js";

const router = express.Router();

// Tutte le route richiedono autenticazione
router.use(authenticate);

// POST /api/v1/sessions — crea una nuova sessione
router.post("/", async (req, res) => {
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
      return res.status(409).json({ error: "Sei già in una sessione attiva" });
    res.status(500).json({ error: "Errore creazione sessione" });
  }
});

// POST /api/v1/sessions/join — si unisce a una sessione tramite codice invito
router.post("/join", async (req, res) => {
  const { inviteCode } = req.body;

  if (!inviteCode) {
    return res.status(400).json({ error: "inviteCode mancante nel body" });
  }

  try {
    const session = await joinSession(req.user.userId, inviteCode);
    res.status(200).json(session);
  } catch (err) {
    if (err.message === "USER_ALREADY_IN_SESSION")
      return res.status(409).json({ error: "Sei già in una sessione attiva" });
    if (err.message === "SESSION_NOT_FOUND")
      return res.status(404).json({ error: "Codice invito non valido" });
    if (err.message === "SESSION_NOT_JOINABLE")
      return res.status(409).json({ error: "La sessione non è più aperta" });
    if (err.message === "ALREADY_IN_SESSION")
      return res.status(409).json({ error: "Sei già in questa sessione" });
    res.status(500).json({ error: "Errore durante l'accesso alla sessione" });
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
router.get("/:id", async (req, res) => {
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
router.patch("/:id/status", async (req, res) => {
  const { status } = req.body;
  const validStatuses = ["PLANNED", "ACTIVE", "COMPLETED", "CANCELLED"];

  if (!validStatuses.includes(status)) {
    return res.status(400).json({
      error: `Status non valido. Valori accettati: ${validStatuses.join(", ")}`,
    });
  }

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

// POST /api/v1/sessions/:id/leave — abbandona sessione
router.post("/:id/leave", async (req, res) => {
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
router.patch("/:id", async (req, res) => {
  try {
    const session = await updateSessionDetails(req.params.id, req.user.userId, req.body);
    res.status(200).json(session);
  } catch (err) {
    if (err.message === "SESSION_NOT_FOUND") return res.status(404).json({ error: "Sessione non trovata" });
    if (err.message === "FORBIDDEN") return res.status(403).json({ error: "Solo il Capogruppo può modificare la sessione" });
    res.status(500).json({ error: "Errore aggiornamento sessione" });
  }
});

// DELETE /api/v1/sessions/:id — elimina sessione
router.delete("/:id", async (req, res) => {
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