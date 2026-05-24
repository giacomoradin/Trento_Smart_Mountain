import request from "supertest";
import app from "../../src/app.js";
import {
  createTestHiker,
  generateValidToken,
} from "../helpers/authHelper.js";

/**
 * Test suite per le route Weather.
 *
 * Copre principalmente i requisiti di **autorizzazione**:
 * - GET  /weather/locations/nearby      → JWT richiesto
 * - GET  /weather/locations/search      → JWT richiesto
 * - POST /weather/seed                  → admin only
 * - POST /weather/forecast/:id/refresh  → admin only
 *
 * NOTA: i test NON validano i dati del forecast (richiederebbe seed completo del
 * DB delle 601 towns + 108 POI). Validano solo il layer di sicurezza, che è la
 * parte chiave per la regression dopo lo sprint 2.
 */

describe("Weather Routes — Authorization", () => {
  // ══════════════════════════════════════════════════════════════════
  // Endpoint pubblici-autenticati (qualunque utente loggato)
  // ══════════════════════════════════════════════════════════════════

  describe("GET /weather/locations/nearby", () => {
    test("returns 401 without auth token", async () => {
      const res = await request(app).get(
        "/weather/locations/nearby?lon=11.12&lat=46.07",
      );
      expect(res.status).toBe(401);
    });

    test("returns 400 if lon/lat mancanti (autenticato)", async () => {
      const { token } = await createTestHiker({
        username: "weatheruser",
        email: "weather@test.com",
      });

      const res = await request(app)
        .get("/weather/locations/nearby")
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(400);
    });

    test("hiker autenticato passa la auth (anche se nessuna location seedata)", async () => {
      const { token } = await createTestHiker({
        username: "weather2",
        email: "weather2@test.com",
      });

      const res = await request(app)
        .get("/weather/locations/nearby?lon=11.12&lat=46.07")
        .set("Authorization", `Bearer ${token}`);

      // Pass auth → 200 con results vuoti (DB in-memory pulito) o 500 se service explode
      // L'importante è che NON sia 401/403
      expect([200, 500]).toContain(res.status);
    });
  });

  describe("GET /weather/locations/search", () => {
    test("returns 401 without token", async () => {
      const res = await request(app).get("/weather/locations/search?q=Trento");
      expect(res.status).toBe(401);
    });

    test("returns 400 with q < 2 chars", async () => {
      const { token } = await createTestHiker({
        username: "searcher",
        email: "search@test.com",
      });

      const res = await request(app)
        .get("/weather/locations/search?q=T")
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(400);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // POST /weather/seed — Admin only
  // ══════════════════════════════════════════════════════════════════

  describe("POST /weather/seed (admin only)", () => {
    test("returns 401 without token", async () => {
      const res = await request(app).post("/weather/seed");
      expect(res.status).toBe(401);
    });

    test("returns 403 if user role is not admin (hiker)", async () => {
      const { token } = await createTestHiker({
        username: "wannabe_admin",
        email: "wannabe@test.com",
      });

      const res = await request(app)
        .post("/weather/seed")
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(403);
    });

    test("admin token passes authorization", async () => {
      // Admin user: usiamo un JWT con role="admin" su un userId fittizio
      // Il middleware requireRoles("admin") guarda req.user.role decodificato
      // dal JWT, non interroga il DB. Quindi un JWT firmato con role admin basta.
      const adminToken = generateValidToken(
        "507f1f77bcf86cd799439011",
        "admin",
      );

      const res = await request(app)
        .post("/weather/seed")
        .set("Authorization", `Bearer ${adminToken}`);

      // Pass auth+role → entra nel service seedLocations.
      // In test env senza network esterno il service può fallire (5xx) ma
      // NON deve essere 401/403.
      expect([200, 201, 500, 502, 503]).toContain(res.status);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // POST /weather/forecast/:id/refresh — Admin only
  // ══════════════════════════════════════════════════════════════════

  describe("POST /weather/forecast/:id/refresh (admin only)", () => {
    test("returns 401 without token", async () => {
      const res = await request(app).post(
        "/weather/forecast/test-uuid/refresh",
      );
      expect(res.status).toBe(401);
    });

    test("returns 403 for non-admin hiker token", async () => {
      const { token } = await createTestHiker({
        username: "curious",
        email: "curious@test.com",
      });

      const res = await request(app)
        .post("/weather/forecast/test-uuid/refresh")
        .set("Authorization", `Bearer ${token}`);

      expect(res.status).toBe(403);
    });

    test("admin token passes auth (404 ok perché town non esiste in DB test)", async () => {
      const adminToken = generateValidToken(
        "507f1f77bcf86cd799439012",
        "admin",
      );

      const res = await request(app)
        .post("/weather/forecast/non-existing-uuid/refresh")
        .set("Authorization", `Bearer ${adminToken}`);

      // Pass auth → service tenta refresh, fallisce con 404 perché location non in DB
      expect(res.status).not.toBe(401);
      expect(res.status).not.toBe(403);
    });
  });
});
