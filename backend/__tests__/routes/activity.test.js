import request from "supertest";
import app from "../../src/app.js";
import { createTestHiker } from "../helpers/authHelper.js";

/**
 * Test suite per le route delle attività libere (Activity).
 *
 * Copre:
 * - POST   /api/v1/activities             (crea attività libera)
 * - GET    /api/v1/activities             (lista propria)
 * - GET    /api/v1/activities/:id         (dettaglio con ownership check)
 * - DELETE /api/v1/activities/:id         (solo proprietario)
 *
 * Sicurezza: tutte le route richiedono JWT + verifica owner via service.
 */

const VALID_ACTIVITY = {
  name: "Escursione – 24 mag 2026",
  activityType: "hiking",
  startTimeMs: 1748000000000,
  endTimeMs: 1748010000000,
  actualStats: {
    movingSeconds: 9000,
    totalSeconds: 10000,
    distanceMeters: 5200,
    elevationGainM: 320,
    finalPoints: 18,
    estimatedCalories: 310,
    currentAltitudeM: 1450,
  },
};

describe("Activity Routes", () => {
  // ══════════════════════════════════════════════════════════════════
  // POST /api/v1/activities — Crea attività libera
  // ══════════════════════════════════════════════════════════════════

  describe("POST /api/v1/activities", () => {
    test("authenticated user can create a free activity", async () => {
      const { token } = await createTestHiker({
        username: "freerunner",
        email: "freerunner@test.com",
      });

      const res = await request(app)
        .post("/api/v1/activities")
        .set("Authorization", `Bearer ${token}`)
        .send(VALID_ACTIVITY);

      expect(res.status).toBe(201);
      expect(res.body).toHaveProperty("_id");
      expect(res.body.name).toBe(VALID_ACTIVITY.name);
      expect(res.body.actualStats.distanceMeters).toBe(5200);
    });

    test("returns 401 without auth token", async () => {
      const res = await request(app)
        .post("/api/v1/activities")
        .send(VALID_ACTIVITY);

      expect(res.status).toBe(401);
    });

    test("returns 422 on missing required actualStats", async () => {
      const { token } = await createTestHiker({
        username: "broken",
        email: "broken@test.com",
      });

      const res = await request(app)
        .post("/api/v1/activities")
        .set("Authorization", `Bearer ${token}`)
        .send({ name: "Solo nome" });

      expect(res.status).toBe(422);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/activities — Lista propria
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/activities", () => {
    test("returns only own activities (no leak across users)", async () => {
      const { token: aliceToken } = await createTestHiker({
        username: "alice",
        email: "alice@test.com",
      });
      const { token: bobToken } = await createTestHiker({
        username: "bob",
        email: "bob@test.com",
      });

      // Alice crea 2 attività
      await request(app)
        .post("/api/v1/activities")
        .set("Authorization", `Bearer ${aliceToken}`)
        .send(VALID_ACTIVITY);
      await request(app)
        .post("/api/v1/activities")
        .set("Authorization", `Bearer ${aliceToken}`)
        .send({ ...VALID_ACTIVITY, name: "Seconda di Alice" });

      // Bob crea 1 attività
      await request(app)
        .post("/api/v1/activities")
        .set("Authorization", `Bearer ${bobToken}`)
        .send({ ...VALID_ACTIVITY, name: "Unica di Bob" });

      // Bob vede solo la sua
      const bobList = await request(app)
        .get("/api/v1/activities")
        .set("Authorization", `Bearer ${bobToken}`);

      expect(bobList.status).toBe(200);
      expect(bobList.body.length).toBe(1);
      expect(bobList.body[0].name).toBe("Unica di Bob");
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/activities/:id — Ownership check
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/activities/:id", () => {
    test("owner can read own activity", async () => {
      const { token } = await createTestHiker({
        username: "owner4",
        email: "owner4@test.com",
      });

      const created = await request(app)
        .post("/api/v1/activities")
        .set("Authorization", `Bearer ${token}`)
        .send(VALID_ACTIVITY);

      const res = await request(app)
        .get(`/api/v1/activities/${created.body._id}`)
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body._id).toBe(created.body._id);
    });

    test("non-owner gets 403 on activity detail", async () => {
      const { token: ownerToken } = await createTestHiker({
        username: "owner5",
        email: "owner5@test.com",
      });
      const created = await request(app)
        .post("/api/v1/activities")
        .set("Authorization", `Bearer ${ownerToken}`)
        .send(VALID_ACTIVITY);

      const { token: intruderToken } = await createTestHiker({
        username: "snoop",
        email: "snoop@test.com",
      });

      const res = await request(app)
        .get(`/api/v1/activities/${created.body._id}`)
        .set("Authorization", `Bearer ${intruderToken}`);

      expect(res.status).toBe(403);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // DELETE /api/v1/activities/:id — Solo owner
  // ══════════════════════════════════════════════════════════════════

  describe("DELETE /api/v1/activities/:id", () => {
    test("owner can delete own activity", async () => {
      const { token } = await createTestHiker({
        username: "owner6",
        email: "owner6@test.com",
      });

      const created = await request(app)
        .post("/api/v1/activities")
        .set("Authorization", `Bearer ${token}`)
        .send(VALID_ACTIVITY);

      const del = await request(app)
        .delete(`/api/v1/activities/${created.body._id}`)
        .set("Authorization", `Bearer ${token}`);

      expect(del.status).toBe(200);

      // Verifica che il GET ora restituisca 404
      const check = await request(app)
        .get(`/api/v1/activities/${created.body._id}`)
        .set("Authorization", `Bearer ${token}`);
      expect(check.status).toBe(404);
    });

    test("non-owner cannot delete (403)", async () => {
      const { token: ownerToken } = await createTestHiker({
        username: "owner7",
        email: "owner7@test.com",
      });
      const created = await request(app)
        .post("/api/v1/activities")
        .set("Authorization", `Bearer ${ownerToken}`)
        .send(VALID_ACTIVITY);

      const { token: intruderToken } = await createTestHiker({
        username: "thief",
        email: "thief@test.com",
      });

      const res = await request(app)
        .delete(`/api/v1/activities/${created.body._id}`)
        .set("Authorization", `Bearer ${intruderToken}`);

      expect(res.status).toBe(403);
    });
  });
});
