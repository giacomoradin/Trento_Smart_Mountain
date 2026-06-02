import request from "supertest";
import app from "../../src/app.js";
import HikeSession from "../../src/models/hikeSession.js";
import { createTestHiker, generateValidToken } from "../helpers/authHelper.js";
import {
  createTestSession,
  createTestSentiero,
  addParticipantToSession,
} from "../helpers/sessionHelper.js";

/**
 * Test suite per le route della checklist di HikeSession.
 *
 * Copre:
 * - POST /api/v1/sessions/:id/checklist
 * - PUT  /api/v1/sessions/:id/checklist
 * - GET  /api/v1/sessions/:id/checklist
 *
 * Non testa la logica interna di generateChecklist (quella va in
 * services/checklistService.test.js) — testa solo il comportamento HTTP:
 * status code, autorizzazione, freeze, struttura della risposta.
 */

describe("HikeSession Checklist Routes", () => {

  // ══════════════════════════════════════════════════════════════════
  // POST /api/v1/sessions/:id/checklist
  // ══════════════════════════════════════════════════════════════════

  describe("POST /api/v1/sessions/:id/checklist", () => {

    test("genera la checklist con sentieroCode nel body", async () => {
      const sentiero = await createTestSentiero({ codice: "E001", difficolta: "E" });
      const { session, token } = await createTestSession();

      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      expect(response.status).toBe(201);
      expect(response.body).toHaveProperty("message", "Checklist generata con successo.");
      expect(response.body).toHaveProperty("checklist");
      expect(response.body.checklist).toHaveProperty("generatedAt");
      expect(response.body.checklist).toHaveProperty("categorie");
      expect(Array.isArray(response.body.checklist.categorie)).toBe(true);
      expect(response.body.checklist.categorie.length).toBeGreaterThan(0);
      expect(response.body.checklist.isFrozen).toBe(false);
    });

    test("genera la checklist con sentieroCode salvato sulla sessione", async () => {
      const sentiero = await createTestSentiero({ codice: "E002" });
      const { session, token } = await createTestSession({ sentieroCode: sentiero.codice });

      // Nessun sentieroCode nel body — viene preso da session.sentieroCode
      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({});

      expect(response.status).toBe(201);
      expect(response.body.checklist).toHaveProperty("categorie");
    });

    test("genera la checklist senza meteo (meteoDisponibile: false)", async () => {
      const sentiero = await createTestSentiero({ codice: "E003" });
      const { session, token } = await createTestSession();

      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      expect(response.status).toBe(201);
      expect(response.body.meteoDisponibile).toBe(false);
      expect(response.body.checklist.meteoSnapshot).toBeNull();
    });

    test("restituisce 400 se sentieroCode assente e sessione senza dati sufficienti", async () => {
      const { user, token } = await createTestHiker({
        username: "nodatauser",
        email: "nodata@test.com",
      });

      const session = await HikeSession.create({
        creatorId: user._id,
        inviteCode: "TSM-FFAA",
        status: "PLANNED",
        routeDetails: {
          name: "Percorso senza dati",
          // nessun difficultyLevel
        },
        participants: [{ userId: user._id, role: "groupLeader" }],
        meetingDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      });

      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({});
        console.log(response.body);
      expect(response.status).toBe(400);
    });

    test("restituisce 401 senza token", async () => {
      const { session } = await createTestSession();

      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .send({ sentieroCode: "E001" });

      expect(response.status).toBe(401);
    });

    test("restituisce 403 se non sei il creator", async () => {
      const sentiero = await createTestSentiero({ codice: "E004" });
      const { session } = await createTestSession();

      // Utente diverso dal creator
      const { token: otherToken } = await createTestHiker({
        username: "other1",
        email: "other1@test.com",
      });

      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${otherToken}`)
        .send({ sentieroCode: sentiero.codice });

      expect(response.status).toBe(403);
    });

    test("restituisce 404 se la sessione non esiste", async () => {
      const { token } = await createTestHiker({
        username: "ghost1",
        email: "ghost1@test.com",
      });
      const fakeId = "507f1f77bcf86cd799439011";

      const response = await request(app)
        .post(`/api/v1/sessions/${fakeId}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: "E001" });

      expect(response.status).toBe(404);
    });

    test("restituisce 404 se il sentieroCode non esiste nel DB", async () => {
      const { session, token } = await createTestSession();

      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: "INESISTENTE" });

      expect(response.status).toBe(404);
    });

    test("restituisce 409 se la checklist esiste già", async () => {
      const sentiero = await createTestSentiero({ codice: "E005" });
      const { session, token } = await createTestSession();

      // Prima generazione
      await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      // Seconda generazione — deve fallire con 409
      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      expect(response.status).toBe(409);
      expect(response.body.error).toMatch(/esiste già/i);
    });

    test("restituisce 409 se la sessione non è in stato PLANNED", async () => {
      const sentiero = await createTestSentiero({ codice: "E006" });
      const { session, token } = await createTestSession({ status: "ACTIVE" });

      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      expect(response.status).toBe(409);
    });

    test("la checklist generata contiene acquaLitri e calorieFabbisogno", async () => {
      const sentiero = await createTestSentiero({ codice: "E007", tempoAndata: "03:00" });
      const { session, token } = await createTestSession();

      const response = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      expect(response.status).toBe(201);
      expect(response.body.checklist.acquaLitri).toBeGreaterThan(0);
      expect(response.body.checklist.calorieFabbisogno).toBeGreaterThan(0);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // PUT /api/v1/sessions/:id/checklist
  // ══════════════════════════════════════════════════════════════════

  describe("PUT /api/v1/sessions/:id/checklist", () => {

    test("rigenera la checklist e preserva generatedAt", async () => {
      const sentiero = await createTestSentiero({ codice: "U001" });
      const { session, token } = await createTestSession();

      // Prima generazione
      const postRes = await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      const generatedAt = postRes.body.checklist.generatedAt;

      // Piccola pausa per garantire che updatedAt sia diverso da generatedAt
      await new Promise((r) => setTimeout(r, 10));

      // Aggiornamento
      const putRes = await request(app)
        .put(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      expect(putRes.status).toBe(200);
      expect(putRes.body.checklist.generatedAt).toBe(generatedAt);
      expect(putRes.body.checklist).toHaveProperty("categorie");
    });

    test("restituisce 401 senza token", async () => {
      const { session } = await createTestSession();

      const response = await request(app)
        .put(`/api/v1/sessions/${session._id}/checklist`)
        .send({ sentieroCode: "U001" });

      expect(response.status).toBe(401);
    });

    test("restituisce 403 se non sei il creator", async () => {
      const sentiero = await createTestSentiero({ codice: "U002" });
      const { session, token } = await createTestSession();

      // Genera checklist
      await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      // Utente diverso tenta update
      const { token: otherToken } = await createTestHiker({
        username: "other2",
        email: "other2@test.com",
      });

      const response = await request(app)
        .put(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${otherToken}`)
        .send({ sentieroCode: sentiero.codice });

      expect(response.status).toBe(403);
    });

    test("restituisce 404 se la checklist non è ancora stata generata", async () => {
      const sentiero = await createTestSentiero({ codice: "U003" });
      const { session, token } = await createTestSession();

      const response = await request(app)
        .put(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      expect(response.status).toBe(404);
      expect(response.body.error).toMatch(/non ancora generata/i);
    });

    test("restituisce 403 se la checklist è congelata (meetingDate passata)", async () => {
      const sentiero = await createTestSentiero({ codice: "U004" });

      // Sessione con meetingDate nel passato → freeze già scattato
      const { session, token } = await createTestSession({
        overrides: {
          meetingDate: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000), // -2 giorni
        },
      });

      // Genera checklist (POST non controlla il freeze)
      await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      // Tenta update — deve essere bloccato dal freeze
      const response = await request(app)
        .put(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      expect(response.status).toBe(403);
      expect(response.body.error).toMatch(/congelata/i);
      expect(response.body).toHaveProperty("frozenAt");
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/sessions/:id/checklist
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/sessions/:id/checklist", () => {

    test("il creator legge la checklist con stato freeze", async () => {
      const sentiero = await createTestSentiero({ codice: "G001" });
      const { session, token } = await createTestSession();

      await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      const response = await request(app)
        .get(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`);

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty("checklist");
      expect(response.body).toHaveProperty("freeze");
      expect(response.body.freeze).toHaveProperty("isFrozen", false);
      expect(response.body.freeze).toHaveProperty("frozenAt");
    });

    test("un partecipante (non creator) può leggere la checklist", async () => {
      const sentiero = await createTestSentiero({ codice: "G002" });
      const { session, token } = await createTestSession();

      // Genera checklist come creator
      await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      // Crea partecipante e aggiungilo alla sessione
      const { user: participant, token: participantToken } = await createTestHiker({
        username: "partecipante1",
        email: "partecipante1@test.com",
      });
      await addParticipantToSession(session._id, participant._id);

      const response = await request(app)
        .get(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${participantToken}`);

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty("checklist");
    });

    test("restituisce 401 senza token", async () => {
      const { session } = await createTestSession();

      const response = await request(app)
        .get(`/api/v1/sessions/${session._id}/checklist`);

      expect(response.status).toBe(401);
    });

    test("restituisce 403 se non sei partecipante", async () => {
      const sentiero = await createTestSentiero({ codice: "G003" });
      const { session, token } = await createTestSession();

      await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      const { token: strangerToken } = await createTestHiker({
        username: "stranger1",
        email: "stranger1@test.com",
      });

      const response = await request(app)
        .get(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${strangerToken}`);

      expect(response.status).toBe(403);
    });

    test("restituisce 404 se la checklist non è ancora stata generata", async () => {
      const { session, token } = await createTestSession();

      const response = await request(app)
        .get(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`);

      expect(response.status).toBe(404);
      expect(response.body.error).toMatch(/non ancora generata/i);
    });

    test("restituisce 404 se la sessione non esiste", async () => {
      const { token } = await createTestHiker({
        username: "ghost2",
        email: "ghost2@test.com",
      });
      const fakeId = "507f1f77bcf86cd799439011";

      const response = await request(app)
        .get(`/api/v1/sessions/${fakeId}/checklist`)
        .set("Authorization", `Bearer ${token}`);

      expect(response.status).toBe(404);
    });

    test("isFrozen è true se meetingDate è nel passato", async () => {
      const sentiero = await createTestSentiero({ codice: "G004" });
      const { session, token } = await createTestSession({
        overrides: {
          meetingDate: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000), // -2 giorni
        },
      });

      await request(app)
        .post(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`)
        .send({ sentieroCode: sentiero.codice });

      const response = await request(app)
        .get(`/api/v1/sessions/${session._id}/checklist`)
        .set("Authorization", `Bearer ${token}`);

      expect(response.status).toBe(200);
      expect(response.body.freeze.isFrozen).toBe(true);
    });
  });
});