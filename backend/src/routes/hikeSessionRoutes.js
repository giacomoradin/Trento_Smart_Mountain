import express from "express";

import { authenticate } from "../middleware/authMiddleware.js";

import {
  createSession,
  getSessionById,
  getSessionsByUser,
  updateSessionStatus,
  deleteSession,
  joinSession,
} from "../services/hikeSessionService.js";

const router = express.Router();

// Tutte le route richiedono autenticazione
router.use(authenticate);

// POST /api/v1/sessions — crea una nuova sessione
/* #swagger.tags = ['Sessioni escursionistiche']
   #swagger.summary = Crea una nuova sessione
   #swagger.description = Il creatore diventa capogruppo; viene generato un inviteCode univoco. Richiede JWT.
   #swagger.security = [{ "bearerAuth": [] }]
   #swagger.requestBody = {
     required: true,
     content: {
       "application/json": {
         schema: {
           type: "object",
           required: ["routeDetails"],
           properties: {
             routeDetails: {
               type: "object",
               required: ["name", "startPoint", "difficultyLevel"],
               properties: {
                 name: { type: "string", example: "Rifugio Garbari" },
                 startPoint: {
                   type: "object",
                   properties: {
                     type: { type: "string", example: "Point" },
                     coordinates: {
                       type: "array",
                       items: { type: "number" },
                       example: [11.12, 46.07]
                     }
                   }
                 },
                 endPoint: {
                   type: "object",
                   properties: {
                     type: { type: "string", example: "Point" },
                     coordinates: {
                       type: "array",
                       items: { type: "number" },
                       example: [11.15, 46.09]
                     }
                   }
                 },
                 difficultyLevel: {
                   type: "string",
                   enum: ["T", "E", "EE", "EEA"],
                   example: "E"
                 },
                 elevationGain: { type: "number", example: 450 }
               }
             }
           }
         }
       }
     }
   }
   #swagger.responses[201] = { description: "Sessione creata" }
   #swagger.responses[400] = { description: "routeDetails incompleto" }
   #swagger.responses[409] = { description: "Utente già in una sessione attiva o pianificata" }
   #swagger.responses[500] = { description: "Errore creazione sessione" }
*/
router.post("/", async (req, res) => {
  const { routeDetails } = req.body;

  if (
    !routeDetails ||
    !routeDetails.name ||
    !routeDetails.startPoint ||
    !routeDetails.difficultyLevel
  ) {
    return res.status(400).json({ error: "routeDetails incompleto" });
  }

  try {
    // req.user.userId viene dal JWT middleware (Marco/Federico lo implementano)
    const session = await createSession(req.user.userId, routeDetails);
    res.status(201).json(session);
  } catch (err) {
    if (err.message === "USER_ALREADY_IN_SESSION")
      return res.status(409).json({ error: "Sei già in una sessione attiva" });
    res.status(500).json({ error: "Errore creazione sessione" });
  }
});
// POST /api/v1/sessions/join — si unisce a una sessione tramite codice invito
/* #swagger.tags = ['Sessioni escursionistiche']
   #swagger.summary = Entra in una sessione con il codice invito
   #swagger.description = Consentito solo se la sessione è in stato PLANNED. Richiede JWT.
   #swagger.security = [{ "bearerAuth": [] }]
   #swagger.requestBody = {
     required: true,
     content: {
       "application/json": {
         schema: {
           type: "object",
           required: ["inviteCode"],
           properties: {
             inviteCode: { type: "string", example: "A3F7C12B" }
           }
         }
       }
     }
   }
   #swagger.responses[200] = { description: "Sessione aggiornata; utente aggiunto ai partecipanti" }
   #swagger.responses[400] = { description: "inviteCode mancante" }
   #swagger.responses[404] = { description: "Codice invito non valido" }
   #swagger.responses[409] = {
     description: "Conflitto: già in sessione attiva, sessione non joinable, o già iscritto a questa sessione"
   }
   #swagger.responses[500] = { description: "Errore durante l'accesso alla sessione" }
*/
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
/* #swagger.tags = ['Sessioni escursionistiche']
   #swagger.summary = Elenco sessioni dell'utente corrente
   #swagger.description = Sessioni in cui l'utente è creatore o partecipante. Richiede JWT.
   #swagger.security = [{ "bearerAuth": [] }]
   #swagger.responses[200] = { description: "Lista sessioni" }
   #swagger.responses[500] = { description: "Errore recupero sessioni" }
*/
router.get("/my", async (req, res) => {
  try {
    const sessions = await getSessionsByUser(req.user.userId);
    res.status(200).json(sessions);
  } catch (err) {
    res.status(500).json({ error: "Errore recupero sessioni" });
  }
});

// GET /api/v1/sessions/:id — dettaglio singola sessione
/* #swagger.tags = ['Sessioni escursionistiche']
   #swagger.summary = Dettaglio sessione per ID
   #swagger.description = Popola creatore e partecipanti con username ed email. Richiede JWT.
   #swagger.security = [{ "bearerAuth": [] }]
   #swagger.parameters['id'] = {
     in: "path",
     required: true,
     schema: { type: "string" },
     description: "ObjectId MongoDB della sessione"
   }
   #swagger.responses[200] = { description: "Sessione trovata" }
   #swagger.responses[400] = { description: "ID non valido" }
   #swagger.responses[404] = { description: "Sessione non trovata" }
*/
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
/* #swagger.tags = ['Sessioni escursionistiche']
   #swagger.summary = Aggiorna lo stato della sessione
   #swagger.description = Solo il creatore (capogruppo) può modificare. Con ACTIVE imposta startTime; con COMPLETED imposta endTime. Richiede JWT.
   #swagger.security = [{ "bearerAuth": [] }]
   #swagger.parameters['id'] = {
     in: "path",
     required: true,
     schema: { type: "string" },
     description: "ObjectId MongoDB della sessione"
   }
   #swagger.requestBody = {
     required: true,
     content: {
       "application/json": {
         schema: {
           type: "object",
           required: ["status"],
           properties: {
             status: {
               type: "string",
               enum: ["PLANNED", "ACTIVE", "COMPLETED", "CANCELLED"],
               example: "ACTIVE"
             }
           }
         }
       }
     }
   }
   #swagger.responses[200] = { description: "Stato aggiornato" }
   #swagger.responses[400] = { description: "Status non valido" }
   #swagger.responses[403] = { description: "Solo il capogruppo può modificare" }
   #swagger.responses[404] = { description: "Sessione non trovata" }
   #swagger.responses[500] = { description: "Errore aggiornamento stato" }
*/
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

// DELETE /api/v1/sessions/:id — elimina sessione
/* #swagger.tags = ['Sessioni escursionistiche']
   #swagger.summary = Elimina una sessione
   #swagger.description = Solo il creatore può eliminare. Richiede JWT.
   #swagger.security = [{ "bearerAuth": [] }]
   #swagger.parameters['id'] = {
     in: "path",
     required: true,
     schema: { type: "string" },
     description: "ObjectId MongoDB della sessione"
   }
   #swagger.responses[200] = { description: "Sessione eliminata" }
   #swagger.responses[403] = { description: "Solo il capogruppo può eliminare" }
   #swagger.responses[404] = { description: "Sessione non trovata" }
   #swagger.responses[500] = { description: "Errore eliminazione sessione" }
*/
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
