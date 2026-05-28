import request from "supertest";
import app from "../../src/app.js";
import RefreshToken from "../../src/models/refreshToken.js";
import { createTestHiker } from "../helpers/authHelper.js";

/**
 * Test suite refresh token rotation (audit 2026-05).
 *
 * Copre:
 * - POST /auth/login emette access + refresh
 * - POST /auth/refresh rota correttamente (revoca vecchio, emette nuovo)
 * - Replay attack: riusare un refresh revocato → 401 + revoca family
 * - POST /auth/logout revoca refresh corrente
 * - Refresh non valido / scaduto / mancante → 401/400
 */
describe("Refresh Token Flow", () => {
  describe("POST /auth/login", () => {
    test("login emette accessToken + refreshToken", async () => {
      await createTestHiker({
        username: "refreshtest1",
        email: "refresh1@test.com",
        password: "Password123!",
      });

      const res = await request(app)
        .post("/auth/login")
        .send({ email: "refresh1@test.com", password: "Password123!" });

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty("accessToken");
      expect(res.body).toHaveProperty("refreshToken");
      expect(res.body).toHaveProperty("refreshExpiresAt");
      // backward compat
      expect(res.body).toHaveProperty("token");
      expect(res.body.token).toBe(res.body.accessToken);
      // refresh = 96 hex chars (48 bytes)
      expect(res.body.refreshToken).toMatch(/^[a-f0-9]{96}$/);
    });

    test("ogni login emette un refresh nuovo, in family separata", async () => {
      await createTestHiker({
        username: "refreshtest2",
        email: "refresh2@test.com",
        password: "Password123!",
      });

      const res1 = await request(app)
        .post("/auth/login")
        .send({ email: "refresh2@test.com", password: "Password123!" });
      const res2 = await request(app)
        .post("/auth/login")
        .send({ email: "refresh2@test.com", password: "Password123!" });

      expect(res1.body.refreshToken).not.toBe(res2.body.refreshToken);

      // I documenti in DB devono avere family diverse
      const tokens = await RefreshToken.find({}).sort({ createdAt: 1 });
      const families = new Set(tokens.map((t) => t.family));
      expect(families.size).toBeGreaterThanOrEqual(2);
    });
  });

  describe("POST /auth/refresh", () => {
    test("scambia refresh valido con nuova coppia (rotation)", async () => {
      await createTestHiker({
        username: "refreshtest3",
        email: "refresh3@test.com",
        password: "Password123!",
      });
      const login = await request(app)
        .post("/auth/login")
        .send({ email: "refresh3@test.com", password: "Password123!" });
      const oldRefresh = login.body.refreshToken;

      const refresh = await request(app)
        .post("/auth/refresh")
        .send({ refreshToken: oldRefresh });

      expect(refresh.status).toBe(200);
      expect(refresh.body.accessToken).toBeTruthy();
      expect(refresh.body.refreshToken).toBeTruthy();
      // Il nuovo refresh deve essere DIVERSO dal vecchio (rotation)
      expect(refresh.body.refreshToken).not.toBe(oldRefresh);
    });

    test("refresh mancante → 400", async () => {
      const res = await request(app).post("/auth/refresh").send({});
      expect(res.status).toBe(400);
    });

    test("refresh inventato → 401", async () => {
      const fake = "a".repeat(96);
      const res = await request(app)
        .post("/auth/refresh")
        .send({ refreshToken: fake });
      expect(res.status).toBe(401);
    });

    test("riuso refresh già ruotato → 401 REUSED + revoca family", async () => {
      await createTestHiker({
        username: "refreshtest4",
        email: "refresh4@test.com",
        password: "Password123!",
      });
      const login = await request(app)
        .post("/auth/login")
        .send({ email: "refresh4@test.com", password: "Password123!" });
      const original = login.body.refreshToken;

      // 1° rotation: legittima
      const rotated = await request(app)
        .post("/auth/refresh")
        .send({ refreshToken: original });
      expect(rotated.status).toBe(200);
      const newRefresh = rotated.body.refreshToken;

      // 2° tentativo con il refresh ORIGINALE (replay)
      const replay = await request(app)
        .post("/auth/refresh")
        .send({ refreshToken: original });
      expect(replay.status).toBe(401);
      expect(replay.body.message).toMatch(/sicurezza|revoc/i);

      // Il refresh che era valido DEVE essere stato revocato (effetto a catena)
      const replayAfter = await request(app)
        .post("/auth/refresh")
        .send({ refreshToken: newRefresh });
      expect(replayAfter.status).toBe(401);
    });
  });

  describe("POST /auth/logout", () => {
    test("revoca il refresh token", async () => {
      await createTestHiker({
        username: "logouttest",
        email: "logout@test.com",
        password: "Password123!",
      });
      const login = await request(app)
        .post("/auth/login")
        .send({ email: "logout@test.com", password: "Password123!" });

      const refresh = login.body.refreshToken;

      const logoutRes = await request(app)
        .post("/auth/logout")
        .send({ refreshToken: refresh });
      expect(logoutRes.status).toBe(200);

      // Dopo logout, il refresh non funziona più
      const after = await request(app)
        .post("/auth/refresh")
        .send({ refreshToken: refresh });
      expect(after.status).toBe(401);
    });

    test("logout idempotente con refresh mancante", async () => {
      const res = await request(app).post("/auth/logout").send({});
      expect(res.status).toBe(200);
    });
  });
});
