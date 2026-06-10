import request from "supertest";
import app from "../../src/app.js";
import Follow from "../../src/models/follow.js";
import Hiker from "../../src/models/hiker.js";
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
  // Approvazione partecipanti (Fase A) — pending / approve / reject / remove+ban
  // ══════════════════════════════════════════════════════════════════

  describe("Participant approval workflow", () => {
    async function setupSessionWithPendingJoiner(creatorName, joinerName) {
      const { token: creatorToken } = await createTestHiker({
        username: creatorName,
        email: `${creatorName}@test.com`,
      });
      const created = await createSessionAs(creatorToken);
      const { user: joiner, token: joinerToken } = await createTestHiker({
        username: joinerName,
        email: `${joinerName}@test.com`,
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });
      return {
        sessionId: created.body._id,
        inviteCode: created.body.inviteCode,
        creatorToken,
        joiner,
        joinerToken,
      };
    }

    function findParticipant(body, userId) {
      return (body.participants || []).find(
        (p) => (p.userId?._id || p.userId)?.toString() === userId.toString(),
      );
    }

    test("join puts the user in 'pending' status", async () => {
      const { sessionId, creatorToken, joiner } =
        await setupSessionWithPendingJoiner("apprc1", "apprj1");
      const res = await request(app)
        .get(`/api/v1/sessions/${sessionId}`)
        .set("Authorization", `Bearer ${creatorToken}`);
      expect(res.status).toBe(200);
      const p = findParticipant(res.body, joiner._id);
      expect(p).toBeDefined();
      expect(p.status).toBe("pending");
    });

    test("leader approves a pending participant", async () => {
      const { sessionId, creatorToken, joiner } =
        await setupSessionWithPendingJoiner("apprc2", "apprj2");
      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${joiner._id}/approve`)
        .set("Authorization", `Bearer ${creatorToken}`);
      expect(res.status).toBe(200);
      const p = findParticipant(res.body, joiner._id);
      expect(p.status).toBe("accepted");
      expect(p.approvedBy).toBeTruthy();
    });

    test("an already-accepted participant can approve another pending", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "apprc3",
        email: "apprc3@test.com",
      });
      const created = await createSessionAs(creatorToken);
      const code = created.body.inviteCode;
      const sessionId = created.body._id;

      const { user: m1, token: m1Token } = await createTestHiker({
        username: "apprm1",
        email: "apprm1@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${m1Token}`)
        .send({ inviteCode: code });
      await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${m1._id}/approve`)
        .set("Authorization", `Bearer ${creatorToken}`);

      const { user: m2, token: m2Token } = await createTestHiker({
        username: "apprm2",
        email: "apprm2@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${m2Token}`)
        .send({ inviteCode: code });

      // m1, già accettato, approva m2.
      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${m2._id}/approve`)
        .set("Authorization", `Bearer ${m1Token}`);
      expect(res.status).toBe(200);
      const p = findParticipant(res.body, m2._id);
      expect(p.status).toBe("accepted");
    });

    test("a non-member cannot approve (403)", async () => {
      const { sessionId, joiner } = await setupSessionWithPendingJoiner(
        "apprc4",
        "apprj4",
      );
      const { token: strangerToken } = await createTestHiker({
        username: "stranger4",
        email: "stranger4@test.com",
      });
      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${joiner._id}/approve`)
        .set("Authorization", `Bearer ${strangerToken}`);
      expect(res.status).toBe(403);
    });

    test("leader rejects a pending participant (removed from list)", async () => {
      const { sessionId, creatorToken, joiner } =
        await setupSessionWithPendingJoiner("apprc5", "apprj5");
      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${joiner._id}/reject`)
        .set("Authorization", `Bearer ${creatorToken}`);
      expect(res.status).toBe(200);
      expect(findParticipant(res.body, joiner._id)).toBeUndefined();
    });

    test("leader removes a participant and bans re-join (409)", async () => {
      const { sessionId, creatorToken, joiner, joinerToken } =
        await setupSessionWithPendingJoiner("apprc6", "apprj6");
      await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${joiner._id}/approve`)
        .set("Authorization", `Bearer ${creatorToken}`);
      const del = await request(app)
        .delete(`/api/v1/sessions/${sessionId}/participants/${joiner._id}`)
        .set("Authorization", `Bearer ${creatorToken}`);
      expect(del.status).toBe(200);
      expect(findParticipant(del.body, joiner._id)).toBeUndefined();
      // Re-join bloccato dal ban locale.
      const rejoin = await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: del.body.inviteCode });
      expect(rejoin.status).toBe(409);
    });

    test("a non-leader member cannot remove participants (403)", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "apprc7",
        email: "apprc7@test.com",
      });
      const created = await createSessionAs(creatorToken);
      const code = created.body.inviteCode;
      const sessionId = created.body._id;

      const { user: m1, token: m1Token } = await createTestHiker({
        username: "apprm71",
        email: "apprm71@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${m1Token}`)
        .send({ inviteCode: code });
      await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${m1._id}/approve`)
        .set("Authorization", `Bearer ${creatorToken}`);

      const { user: m2, token: m2Token } = await createTestHiker({
        username: "apprm72",
        email: "apprm72@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${m2Token}`)
        .send({ inviteCode: code });

      const res = await request(app)
        .delete(`/api/v1/sessions/${sessionId}/participants/${m2._id}`)
        .set("Authorization", `Bearer ${m1Token}`);
      expect(res.status).toBe(403);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // Completamento "Ibrido" — per-utente + auto-close + force-close leader
  // ══════════════════════════════════════════════════════════════════

  describe("Hybrid completion model", () => {
    async function fetchSession(token, sessionId) {
      const res = await request(app)
        .get(`/api/v1/sessions/${sessionId}`)
        .set("Authorization", `Bearer ${token}`);
      return res.body;
    }

    test("solo session: creator completes → COMPLETED immediately", async () => {
      const { token } = await createTestHiker({
        username: "soloc",
        email: "soloc@test.com",
      });
      const created = await createSessionAs(token);
      const res = await request(app)
        .patch(`/api/v1/sessions/${created.body._id}/complete`)
        .set("Authorization", `Bearer ${token}`)
        .send({ actualStats: { movingSeconds: 3600, totalSeconds: 3600, distanceMeters: 8000, elevationGainM: 600 } });
      expect(res.status).toBe(200);
      expect(res.body.status).toBe("COMPLETED");
    });

    test("group: leader finish keeps ACTIVE; last participant finish → COMPLETED", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "grpc",
        email: "grpc@test.com",
      });
      const created = await createSessionAs(creatorToken);
      const sessionId = created.body._id;
      const { user: joiner, token: joinerToken } = await createTestHiker({
        username: "grpj",
        email: "grpj@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });
      await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${joiner._id}/approve`)
        .set("Authorization", `Bearer ${creatorToken}`);

      // Leader finisce: la sessione resta ACTIVE (il joiner non ha finito).
      const r1 = await request(app)
        .patch(`/api/v1/sessions/${sessionId}/complete`)
        .set("Authorization", `Bearer ${creatorToken}`)
        .send({ actualStats: { movingSeconds: 3600, totalSeconds: 3600, distanceMeters: 8000, elevationGainM: 600 } });
      expect(r1.status).toBe(200);
      expect(r1.body.status).not.toBe("COMPLETED");

      // Joiner finisce: ora tutti gli accettati hanno finito → COMPLETED.
      const r2 = await request(app)
        .patch(`/api/v1/sessions/${sessionId}/complete`)
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ actualStats: { movingSeconds: 3000, totalSeconds: 3000, distanceMeters: 7000, elevationGainM: 500 } });
      expect(r2.status).toBe(200);
      expect(r2.body.status).toBe("COMPLETED");
    });

    test("leader force-close (POST /close) → COMPLETED even with stragglers", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "fcc",
        email: "fcc@test.com",
      });
      const created = await createSessionAs(creatorToken);
      const sessionId = created.body._id;
      const { token: joinerToken } = await createTestHiker({
        username: "fcj",
        email: "fcj@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/close`)
        .set("Authorization", `Bearer ${creatorToken}`);
      expect(res.status).toBe(200);
      expect(res.body.status).toBe("COMPLETED");
    });

    test("force-close auto-finalizza i membri ancora live (ADR-001, no ghost)", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "afc",
        email: "afc@test.com",
      });
      const created = await createSessionAs(creatorToken);
      const sessionId = created.body._id;
      const { token: joinerToken, user: joiner } = await createTestHiker({
        username: "afj",
        email: "afj@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });
      await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${joiner._id}/approve`)
        .set("Authorization", `Bearer ${creatorToken}`);
      await activateSession(sessionId, creatorToken);
      // Il joiner è "live" (invia posizione) ma NON conclude → ghost.
      await request(app)
        .post(`/api/v1/sessions/${sessionId}/live-location`)
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ lat: 46.07, lon: 11.12 });

      // Il leader chiude: COMPLETED + il ghost viene auto-finalizzato.
      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/close`)
        .set("Authorization", `Bearer ${creatorToken}`);
      expect(res.status).toBe(200);
      expect(res.body.status).toBe("COMPLETED");

      const after = await HikeSession.findById(sessionId);
      const joinerP = after.participants.find(
        (p) => p.userId.toString() === joiner._id.toString(),
      );
      expect(joinerP.participationState).toBe("finished");
    });

    test("non-leader cannot force-close (403)", async () => {
      const { token: creatorToken } = await createTestHiker({
        username: "fcc2",
        email: "fcc2@test.com",
      });
      const created = await createSessionAs(creatorToken);
      const { token: otherToken } = await createTestHiker({
        username: "fco2",
        email: "fco2@test.com",
      });
      const res = await request(app)
        .post(`/api/v1/sessions/${created.body._id}/close`)
        .set("Authorization", `Bearer ${otherToken}`);
      expect(res.status).toBe(403);
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
  // DELETE /api/v1/sessions/:id/from-activities — hide per-utente
  // ══════════════════════════════════════════════════════════════════

  describe("DELETE /api/v1/sessions/:id/from-activities", () => {
    test("member hides session from own activity list; it no longer appears in /my", async () => {
      const { token } = await createTestHiker({
        username: "hideowner",
        email: "hideowner@test.com",
      });
      const created = await createSessionAs(token);

      const hideRes = await request(app)
        .delete(`/api/v1/sessions/${created.body._id}/from-activities`)
        .set("Authorization", `Bearer ${token}`);
      expect(hideRes.status).toBe(200);

      // Documento NON cancellato (resta nel DB per gli altri partecipanti).
      const stillThere = await HikeSession.findById(created.body._id);
      expect(stillThere).not.toBeNull();

      // Non compare più nella lista "Le mie attività" del chiamante.
      const myRes = await request(app)
        .get("/api/v1/sessions/my")
        .set("Authorization", `Bearer ${token}`);
      expect(myRes.status).toBe(200);
      expect(myRes.body.find((s) => s._id === created.body._id)).toBeUndefined();
    });

    test("non-member cannot hide (403 NOT_IN_SESSION)", async () => {
      const { token: ownerToken } = await createTestHiker({
        username: "hideowner2",
        email: "hideowner2@test.com",
      });
      const created = await createSessionAs(ownerToken);
      const { token: strangerToken } = await createTestHiker({
        username: "stranger",
        email: "stranger@test.com",
      });

      const res = await request(app)
        .delete(`/api/v1/sessions/${created.body._id}/from-activities`)
        .set("Authorization", `Bearer ${strangerToken}`);
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
    async function setProfileVisibility(userId, visibility) {
      await Hiker.updateOne(
        { _id: userId },
        { "preferences.privacy.profileVisibility": visibility },
      );
    }

    async function setUserSex(userId, sex) {
      await Hiker.updateOne({ _id: userId }, { "personalInfo.sex": sex });
    }

    test("GET /:id/live-locations: sex rispetta profileVisibility (anche per capogruppo)", async () => {
      const { token: leaderToken, user: leader } = await createTestHiker({
        username: "sexvis_leader",
        email: "sexvis_leader@test.com",
      });
      await setUserSex(leader._id, "M");

      const created = await createSessionAs(leaderToken);

      const { token: memberToken, user: member } = await createTestHiker({
        username: "sexvis_member",
        email: "sexvis_member@test.com",
      });
      await setUserSex(member._id, "F");

      const joinRes = await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${memberToken}`)
        .send({ inviteCode: created.body.inviteCode });
      expect([200, 201]).toContain(joinRes.status);

      await activateSession(created.body._id, leaderToken);

      const postLoc = async (token) => {
        const res = await request(app)
          .post(`/api/v1/sessions/${created.body._id}/live-location`)
          .set("Authorization", `Bearer ${token}`)
          .send({ lat: 46.07, lon: 11.12 });
        expect(res.status).toBe(200);
      };
      await postLoc(leaderToken);
      await postLoc(memberToken);

      const fetchSexAsLeader = async () => {
        const res = await request(app)
          .get(`/api/v1/sessions/${created.body._id}/live-locations?maxAgeSec=120`)
          .set("Authorization", `Bearer ${leaderToken}`);
        expect(res.status).toBe(200);
        const row = res.body.data.find((l) => l.user.id === member._id.toString());
        return row?.user?.sex;
      };

      // Profilo pubblico → il capogruppo vede il sesso del membro.
      await setProfileVisibility(member._id, "public");
      expect(await fetchSexAsLeader()).toBe("F");

      // Profilo privato → nessun sesso esposto.
      await setProfileVisibility(member._id, "private");
      expect(await fetchSexAsLeader()).toBeUndefined();

      // Solo amici: visibile solo se il capogruppo segue il membro.
      await setProfileVisibility(member._id, "friends");
      expect(await fetchSexAsLeader()).toBeUndefined();

      await Follow.create({ followerId: leader._id, followingId: member._id });
      expect(await fetchSexAsLeader()).toBe("F");
    });

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

  // ══════════════════════════════════════════════════════════════════
  // Failover leadership — elezione automatica + reclaim del creator
  // ══════════════════════════════════════════════════════════════════

  describe("Leader failover", () => {
    test("leader inattivo → elezione del partecipante; rientro creator → reclaim", async () => {
      const { token: creatorToken, user: creator } = await createTestHiker({
        username: "fo_leader",
        email: "fo_leader@test.com",
      });
      const created = await createSessionAs(creatorToken);
      const sessionId = created.body._id;

      const { token: joinerToken, user: joiner } = await createTestHiker({
        username: "fo_joiner",
        email: "fo_joiner@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });
      // Il partecipante deve essere ACCETTATO per poter essere eletto.
      await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${joiner._id}/approve`)
        .set("Authorization", `Bearer ${creatorToken}`);

      await activateSession(sessionId, creatorToken);

      // Simula: leader inattivo (heartbeat vecchio) + partecipante "live" (posizione fresca).
      const now = new Date();
      await HikeSession.findByIdAndUpdate(sessionId, {
        $set: {
          lastHeartbeat: new Date(now.getTime() - 5 * 60 * 1000),
          liveLocations: [
            {
              userId: joiner._id,
              lat: 46.07,
              lon: 11.12,
              trackingStatus: "MOVING",
              updatedAt: now,
            },
          ],
        },
      });

      // Il fetch live-locations del partecipante innesca l'elezione.
      const fetchRes = await request(app)
        .get(`/api/v1/sessions/${sessionId}/live-locations?maxAgeSec=120`)
        .set("Authorization", `Bearer ${joinerToken}`);
      expect(fetchRes.status).toBe(200);

      const afterElection = await HikeSession.findById(sessionId);
      expect(afterElection.currentLeaderId.toString()).toBe(joiner._id.toString());
      expect(afterElection.statoFailover).toBe(true);

      // Il creator rientra (invia posizione) → reclaim della leadership.
      const reclaimRes = await request(app)
        .post(`/api/v1/sessions/${sessionId}/live-location`)
        .set("Authorization", `Bearer ${creatorToken}`)
        .send({ lat: 46.07, lon: 11.12 });
      expect(reclaimRes.status).toBe(200);

      const afterReclaim = await HikeSession.findById(sessionId);
      expect(afterReclaim.currentLeaderId.toString()).toBe(creator._id.toString());
      expect(afterReclaim.statoFailover).toBe(false);
    });

    test("nessuna elezione se il leader è attivo (heartbeat recente)", async () => {
      const { token: creatorToken, user: creator } = await createTestHiker({
        username: "fo_leader2",
        email: "fo_leader2@test.com",
      });
      const created = await createSessionAs(creatorToken);
      const sessionId = created.body._id;
      const { token: joinerToken, user: joiner } = await createTestHiker({
        username: "fo_joiner2",
        email: "fo_joiner2@test.com",
      });
      await request(app)
        .post("/api/v1/sessions/join")
        .set("Authorization", `Bearer ${joinerToken}`)
        .send({ inviteCode: created.body.inviteCode });
      await request(app)
        .post(`/api/v1/sessions/${sessionId}/participants/${joiner._id}/approve`)
        .set("Authorization", `Bearer ${creatorToken}`);
      await activateSession(sessionId, creatorToken);

      // Heartbeat recente del leader → niente failover.
      const now = new Date();
      await HikeSession.findByIdAndUpdate(sessionId, {
        $set: {
          lastHeartbeat: now,
          liveLocations: [
            { userId: joiner._id, lat: 46, lon: 11, trackingStatus: "MOVING", updatedAt: now },
          ],
        },
      });

      await request(app)
        .get(`/api/v1/sessions/${sessionId}/live-locations?maxAgeSec=120`)
        .set("Authorization", `Bearer ${joinerToken}`);

      const after = await HikeSession.findById(sessionId);
      expect(after.statoFailover).toBe(false);
      expect(after.currentLeaderId.toString()).toBe(creator._id.toString());
    });
  });
});
