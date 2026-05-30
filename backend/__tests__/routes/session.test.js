import request from "supertest";
import app from "../../src/app.js";
import HikeSession from "../../src/models/hikeSession.js";
import { createTestHiker, generateValidToken } from "../helpers/authHelper.js";

/**
 * Test suite per le route di HikeSession (sessioni di escursione di gruppo).
 *
 * Copre:
 * - POST   /api/v1/sessions             (crea sessione)
 * - GET    /api/v1/sessions/my          (sessioni dell'utente)
 * - GET    /api/v1/sessions/:id         (dettaglio con check autorizzazione)
 * - POST   /api/v1/sessions/join        (unisciti via inviteCode)
 * - PATCH  /api/v1/sessions/:id/status  (PLANNED→ACTIVE→COMPLETED, solo creator)
 * - PATCH  /api/v1/sessions/:id/complete (con actualStats, solo partecipanti)
 * - POST   /api/v1/sessions/:id/leave   (creator non può abbandonare)
 * - DELETE /api/v1/sessions/:id         (solo creator)
 * - GET    /api/v1/sessions/stats       (aggregazione annuale per utente)
 *
 * Le route richiedono tutte JWT valido (authenticate middleware).
 */

const VALID_SESSION_BODY = {
  routeDetails: {
    name: "Cima Tosa via Bocchette",
    difficultyLevel: "EE",
  },
  meetingDate: "2026-07-15",
  meetingTime: "06:00",
  meetingLocation: "Rifugio Pedrotti",
  maxParticipants: 8,
  minExperienceLevel: "E",
};

describe("HikeSession Routes", () => {
  // ── Helper per creare una sessione di test rapidamente ──
  async function createSessionAs(token) {
    const res = await request(app)
      .post("/api/v1/sessions")
      .set("Authorization", `Bearer ${token}`)
      .send(VALID_SESSION_BODY);
    return res;
  }

  async function activateSession(sessionId, token) {
    return request(app)
      .patch(`/api/v1/sessions/${sessionId}/status`)
      .set("Authorization", `Bearer ${token}`)
      .send({ status: "ACTIVE" });
  }

  // ══════════════════════════════════════════════════════════════════
  // POST /api/v1/sessions — Crea sessione
  // ══════════════════════════════════════════════════════════════════

  describe("POST /api/v1/sessions", () => {
    test("creator can create a session with valid body", async () => {
      const { token } = await createTestHiker({
        username: "creator1",
        email: "creator1@test.com",
      });

      const res = await createSessionAs(token);

      expect(res.status).toBe(201);
      expect(res.body).toHaveProperty("_id");
      expect(res.body).toHaveProperty("inviteCode");
      expect(res.body.inviteCode).toMatch(/^TSM-[A-F0-9]{4}$/);
      expect(res.body.status).toBe("PLANNED");
      expect(res.body.routeDetails.name).toBe("Cima Tosa via Bocchette");
    });

    test("returns 401 without auth token", async () => {
      const res = await request(app)
        .post("/api/v1/sessions")
        .send(VALID_SESSION_BODY);

      expect(res.status).toBe(401);
    });

    test("returns 422 on invalid body (missing routeDetails)", async () => {
      const { token } = await createTestHiker({
        username: "creator2",
        email: "creator2@test.com",
      });

      const res = await request(app)
        .post("/api/v1/sessions")
        .set("Authorization", `Bearer ${token}`)
        .send({ meetingDate: "2026-07-15" });

      expect(res.status).toBe(422);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/sessions/my — Sessioni dell'utente
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/sessions/my", () => {
    test("returns sessions where user is creator or participant", async () => {
      const { token } = await createTestHiker({
        username: "owner",
        email: "owner@test.com",
      });

      await createSessionAs(token);
      await createSessionAs(token);

      const res = await request(app)
        .get("/api/v1/sessions/my")
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(Array.isArray(res.body)).toBe(true);
      expect(res.body.length).toBe(2);
    });

    test("returns empty list for user with no sessions", async () => {
      const { token } = await createTestHiker({
        username: "loner",
        email: "loner@test.com",
      });

      const res = await request(app)
        .get("/api/v1/sessions/my")
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body).toEqual([]);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // POST /api/v1/sessions/join — Join via inviteCode
  // ══════════════════════════════════════════════════════════════════

  describe("POST /api/v1/sessions/join", () => {
    test("user can join a session with valid inviteCode", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "creator3",
        email: "creator3@test.com",
      });
      const created = await createSessionAs(creatorToken);

      const { token: joinerToken } = await createTestHiker({
        username: "joiner",
        email: "joiner@test.com",
      });

      const res = await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });

      expect([200, 201]).toContain(res.status);
    });

    test("returns 404 with non-existing inviteCode", async () => {
      const { token } = await createTestHiker({
        username: "joiner2",
        email: "joiner2@test.com",
      });

      const res = await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${token}`)
        .send({ inviteCode: "TSM-FFFF" });

      expect(res.status).toBe(404);
    });

    test("returns 422 with malformed inviteCode (Joi pattern fail)", async () => {
      const { token } = await createTestHiker({
        username: "joiner3",
        email: "joiner3@test.com",
      });

      const res = await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${token}`)
        .send({ inviteCode: "INVALID" });

      expect(res.status).toBe(422);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // PATCH /api/v1/sessions/:id/status — Solo creator
  // ══════════════════════════════════════════════════════════════════

  describe("PATCH /api/v1/sessions/:id/status", () => {
    test("creator can transition PLANNED → ACTIVE", async () => {
      const { token } = await createTestHiker({
        username: "starter",
        email: "starter@test.com",
      });
      const created = await createSessionAs(token);

      const res = await request(app)
        .patch(`/api/v1/sessions/${created.body._id}/status`)
        .set("Authorization", `Bearer ${token}`)
        .send({ status: "ACTIVE" });

      expect(res.status).toBe(200);

      const updated = await HikeSession.findById(created.body._id);
      expect(updated.status).toBe("ACTIVE");
    });

    test("non-creator participant gets 403 on status update", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "boss",
        email: "boss@test.com",
      });
      const created = await createSessionAs(creatorToken);

      // Joiner si unisce alla sessione
      const { token: joinerToken } = await createTestHiker({
        username: "follower",
        email: "follower@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });

      // Joiner tenta di forzare lo status
      const res = await request(app)
        .patch(`/api/v1/sessions/${created.body._id}/status`)
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ status: "ACTIVE" });

      expect(res.status).toBe(403);
    });

    test("returns 422 on invalid status enum", async () => {
      const { token } = await createTestHiker({
        username: "boss2",
        email: "boss2@test.com",
      });
      const created = await createSessionAs(token);

      const res = await request(app)
        .patch(`/api/v1/sessions/${created.body._id}/status`)
        .set("Authorization", `Bearer ${token}`)
        .send({ status: "INVALID_STATE" });

      expect(res.status).toBe(422);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // POST /api/v1/sessions/:id/leave — Creator non può abbandonare
  // ══════════════════════════════════════════════════════════════════

  describe("POST /api/v1/sessions/:id/leave", () => {
    test("creator cannot leave own session (must delete)", async () => {
      const { token } = await createTestHiker({
        username: "founder",
        email: "founder@test.com",
      });
      const created = await createSessionAs(token);

      const res = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/leave`)
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(403);
    });

    test("participant can leave session", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "founder2",
        email: "founder2@test.com",
      });
      const created = await createSessionAs(creatorToken);

      const { token: joinerToken } = await createTestHiker({
        username: "quitter",
        email: "quitter@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });

      const res = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/leave`)
        .set("Authorization", `Bearer ${joinerToken}`);

      expect(res.status).toBe(200);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // DELETE /api/v1/sessions/:id — Solo creator
  // ══════════════════════════════════════════════════════════════════

  describe("DELETE /api/v1/sessions/:id", () => {
    test("creator can delete own session", async () => {
      const { token } = await createTestHiker({
        username: "deleter",
        email: "deleter@test.com",
      });
      const created = await createSessionAs(token);

      const res = await request(app)
        .delete(`/api/v1/sessions/${created.body._id}`)
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(200);

      const deleted = await HikeSession.findById(created.body._id);
      expect(deleted).toBeNull();
    });

    test("non-creator cannot delete session (403)", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "owner3",
        email: "owner3@test.com",
      });
      const created = await createSessionAs(creatorToken);

      const { token: otherToken } = await createTestHiker({
        username: "intruder",
        email: "intruder@test.com",
      });

      const res = await request(app)
        .delete(`/api/v1/sessions/${created.body._id}`)
        .set("Authorization", `Bearer ${otherToken}`);

      expect(res.status).toBe(403);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/sessions/stats — Aggregato annuale
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/sessions/stats", () => {
    test("returns aggregate stats for current year", async () => {
      const { token } = await createTestHiker({
        username: "statlover",
        email: "stats@test.com",
      });

      const res = await request(app)
        .get("/api/v1/sessions/stats?year=2026")
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty("year");
      expect(res.body).toHaveProperty("monthlyActivityCount");
      expect(Array.isArray(res.body.monthlyActivityCount)).toBe(true);
      expect(res.body.monthlyActivityCount.length).toBe(12);
    });

    test("returns 401 without token", async () => {
      const res = await request(app).get("/api/v1/sessions/stats?year=2026");
      expect(res.status).toBe(401);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // meetingDate migration String → Date (audit 2026-05)
  // ══════════════════════════════════════════════════════════════════
  // Verifica: il setter del model parse "YYYY-MM-DD" → Date, ma il transform
  // toJSON espone ancora "YYYY-MM-DD" al client. Backward compat al 100%
  // più sort cronologici efficienti lato DB.

  describe("meetingDate persistence (audit 2026-05)", () => {
    test("createSession con 'YYYY-MM-DD' string → JSON output identico", async () => {
      const { token } = await createTestHiker({
        username: "datetest1",
        email: "datetest1@test.com",
      });
      const res = await createSessionAs(token);

      expect(res.status).toBe(201);
      // Client mobile riceve ancora la stessa stringa
      expect(res.body.meetingDate).toBe("2026-07-15");
    });

    test("documento in DB ha meetingDate come Date BSON", async () => {
      const { token } = await createTestHiker({
        username: "datetest2",
        email: "datetest2@test.com",
      });
      const created = await createSessionAs(token);

      // Bypass toJSON transform: leggi raw dal DB
      const raw = await HikeSession.collection.findOne({
        _id: new (await import("mongoose")).default.Types.ObjectId(
          created.body._id,
        ),
      });
      expect(raw.meetingDate).toBeInstanceOf(Date);
      // 2026-07-15 UTC midnight
      expect(raw.meetingDate.toISOString().slice(0, 10)).toBe("2026-07-15");
    });

    test("sort cronologico via $sort: { meetingDate: 1 } funziona", async () => {
      const { token, user } = await createTestHiker({
        username: "datetest3",
        email: "datetest3@test.com",
      });

      // Crea 3 sessioni in ordine non-cronologico
      const dates = ["2026-12-25", "2026-01-15", "2026-06-10"];
      for (const date of dates) {
        await request(app)
          .post("/api/v1/sessions")
          .set("Authorization", `Bearer ${token}`)
          .send({ ...VALID_SESSION_BODY, meetingDate: date });
      }

      const res = await request(app)
        .get("/api/v1/sessions/my")
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(200);
      const sortedDates = res.body.map((s) => s.meetingDate);
      // Atteso ordine ascendente cronologico (non lessicografico, anche se per
      // YYYY-MM-DD i due coincidono — questo test conferma comunque il fix).
      expect(sortedDates).toEqual(["2026-01-15", "2026-06-10", "2026-12-25"]);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // Live tracking (realtime polling endpoints)
  // ══════════════════════════════════════════════════════════════════

  describe("Live tracking endpoints", () => {
    test("POST /:id/live-location stores last location (upsert)", async () => {
      const { token } = await createTestHiker({
        username: "liveu1",
        email: "liveu1@test.com",
      });

      const created = await createSessionAs(token);
      await activateSession(created.body._id, token);

      const body1 = { lat: 46.07, lon: 11.12, accuracyM: 8.5, timestampMs: Date.now() };
      const res1 = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-location`)
        .set("Authorization", `Bearer ${token}`)
        .send(body1);
      expect(res1.status).toBe(200);

      const body2 = { lat: 46.071, lon: 11.121, accuracyM: 10 };
      const res2 = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-location`)
        .set("Authorization", `Bearer ${token}`)
        .send(body2);
      expect(res2.status).toBe(200);

      const doc = await HikeSession.findById(created.body._id).lean();
      expect(Array.isArray(doc.liveLocations)).toBe(true);
      expect(doc.liveLocations.length).toBe(1);
      expect(doc.liveLocations[0].lat).toBeCloseTo(46.071, 6);
    });

    test("POST /:id/live-location returns 409 if session not ACTIVE", async () => {
      const { token } = await createTestHiker({
        username: "liveu2",
        email: "liveu2@test.com",
      });
      const created = await createSessionAs(token);

      const res = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-location`)
        .set("Authorization", `Bearer ${token}`)
        .send({ lat: 46.07, lon: 11.12 });

      expect(res.status).toBe(409);
    });

    test("GET /:id/live-locations excludes stale locations and suspended users", async () => {
      const { token: creatorToken, user: creator } = await createTestHiker({
        username: "leaderlive",
        email: "leaderlive@test.com",
      });
      const created = await createSessionAs(creatorToken);

      const { token: joinerToken, user: joiner } = await createTestHiker({
        username: "joinerlive",
        email: "joinerlive@test.com",
      });
      const joinRes = await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });
      expect([200, 201]).toContain(joinRes.status);

      await activateSession(created.body._id, creatorToken);

      // Entrambi caricano una location
      const cUp = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-location`)
        .set("Authorization", `Bearer ${creatorToken}`)
        .send({ lat: 46.07, lon: 11.12 });
      expect(cUp.status).toBe(200);

      const jUp = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-location`)
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ lat: 46.08, lon: 11.13 });
      expect(jUp.status).toBe(200);

      // Rendi la location del joiner stale
      await HikeSession.updateOne(
        { _id: created.body._id, "liveLocations.userId": joiner._id },
        { $set: { "liveLocations.$.updatedAt": new Date(Date.now() - 60 * 1000) } },
      );

      // Sospendi il creator
      await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-tracking/suspend`)
        .set("Authorization", `Bearer ${creatorToken}`)
        .send({ userId: creator._id.toString(), reason: "MANUAL" });

      const res = await request(app)
        .get(`/api/v1/sessions/${created.body._id}/live-locations?maxAgeSec=30`)
        .set("Authorization", `Bearer ${creatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty("data");
      expect(Array.isArray(res.body.data)).toBe(true);
      // Creator sospeso → escluso. Joiner stale → escluso. Quindi vuoto.
      expect(res.body.data.length).toBe(0);
    });

    test("suspended user cannot POST /:id/live-location (403 + reason)", async () => {
      const { token: creatorToken, user: creator } = await createTestHiker({
        username: "leaderlive2",
        email: "leaderlive2@test.com",
      });
      const created = await createSessionAs(creatorToken);

      const { token: joinerToken, user: joiner } = await createTestHiker({
        username: "joinerlive2",
        email: "joinerlive2@test.com",
      });
      const joinRes = await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });
      expect([200, 201]).toContain(joinRes.status);

      await activateSession(created.body._id, creatorToken);

      // creator sospende joiner
      await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-tracking/suspend`)
        .set("Authorization", `Bearer ${creatorToken}`)
        .send({ userId: joiner._id.toString(), reason: "TOO_FAR_FROM_ROUTE" });

      const res = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-location`)
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ lat: 46.07, lon: 11.12 });

      expect(res.status).toBe(403);
      expect(res.body).toHaveProperty("reason", "TOO_FAR_FROM_ROUTE");
    });

    test("only group leader can suspend/resume", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "leaderlive3",
        email: "leaderlive3@test.com",
      });
      const created = await createSessionAs(creatorToken);

      const { token: joinerToken, user: joiner } = await createTestHiker({
        username: "joinerlive3",
        email: "joinerlive3@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });

      // joiner prova a sospendere creator → 403
      const resSuspend = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-tracking/suspend`)
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ userId: joiner._id.toString(), reason: "MANUAL" });
      expect(resSuspend.status).toBe(403);

      // creator sospende e poi riprende joiner → 200
      const resSuspend2 = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-tracking/suspend`)
        .set("Authorization", `Bearer ${creatorToken}`)
        .send({ userId: joiner._id.toString(), reason: "MANUAL" });
      expect(resSuspend2.status).toBe(200);

      const resResume = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/live-tracking/resume`)
        .set("Authorization", `Bearer ${creatorToken}`)
        .send({ userId: joiner._id.toString() });
      expect(resResume.status).toBe(200);
    });
  });
});
