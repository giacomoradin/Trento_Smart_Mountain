import request from "supertest";
import app from "../../src/app.js";
import { createTestHiker, generateValidToken } from "../helpers/authHelper.js";

/**
 * Test suite per le route admin — PUT e DELETE /admin/users/:id.
 *
 * Copre i test case del CSV Sprint 2:
 * - PUT  /admin/users/:id  → aggiorna role, 401/403, 404, 400, 409
 * - DELETE /admin/users/:id → elimina utente, 401/403, 404, 400
 *
 * Tutte le route richiedono JWT con role=admin.
 * Il token admin viene generato con generateValidToken(..., "admin") —
 * il middleware requireRoles("admin") legge req.user.role dal JWT,
 * non interroga il DB.
 */

describe("Admin Routes", () => {
  // Token admin riutilizzato tra i test — non tocca il DB
  const adminToken = generateValidToken("507f1f77bcf86cd799439011", "admin");

  // ══════════════════════════════════════════════════════════════════
  // PUT /admin/users/:id — Aggiorna qualsiasi utente
  // ══════════════════════════════════════════════════════════════════

  describe("PUT /admin/users/:id", () => {
    test("admin aggiorna il role di un utente esistente", async () => {
      const { user } = await createTestHiker({
        username: "targetuser1",
        email: "target1@test.com",
      });

      const response = await request(app)
        .put(`/admin/users/${user._id}`)
        .set("Authorization", `Bearer ${adminToken}`)
        .send({ role: "admin" });

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty("role", "admin");
      expect(response.body).not.toHaveProperty("passwordHash");
      expect(response.body).not.toHaveProperty("__v");
    });

    test("admin aggiorna username e email di un utente esistente", async () => {
      const { user } = await createTestHiker({
        username: "targetuser2",
        email: "target2@test.com",
      });

      const response = await request(app)
        .put(`/admin/users/${user._id}`)
        .set("Authorization", `Bearer ${adminToken}`)
        .send({ username: "nuovousername" });

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty("username", "nuovousername");
    });

    test("body vuoto non modifica nulla — 200 con utente invariato", async () => {
      const { user } = await createTestHiker({
        username: "targetuser3",
        email: "target3@test.com",
      });

      const response = await request(app)
        .put(`/admin/users/${user._id}`)
        .set("Authorization", `Bearer ${adminToken}`)
        .send({});

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty("username", "targetuser3");
    });

    test("restituisce 401 senza token", async () => {
      const { user } = await createTestHiker({
        username: "targetuser4",
        email: "target4@test.com",
      });

      const response = await request(app)
        .put(`/admin/users/${user._id}`)
        .send({ role: "admin" });

      expect(response.status).toBe(401);
    });

    test("restituisce 403 se il token non è admin", async () => {
      const { user, token: hikerToken } = await createTestHiker({
        username: "targetuser5",
        email: "target5@test.com",
      });

      const response = await request(app)
        .put(`/admin/users/${user._id}`)
        .set("Authorization", `Bearer ${hikerToken}`)
        .send({ role: "admin" });

      expect(response.status).toBe(403);
    });

    test("restituisce 404 per ID valido ma utente inesistente", async () => {
      const response = await request(app)
        .put("/admin/users/000000000000000000000000")
        .set("Authorization", `Bearer ${adminToken}`)
        .send({ role: "admin" });

      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty("message", "Utente non trovato.");
    });

    test("restituisce 400 per ID non valido (CastError)", async () => {
      const response = await request(app)
        .put("/admin/users/id-non-valido")
        .set("Authorization", `Bearer ${adminToken}`)
        .send({ role: "admin" });

      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty("message", "ID utente non valido.");
    });

    test("restituisce 409 se username è già usato da un altro utente", async () => {
      await createTestHiker({
        username: "esistente",
        email: "esistente@test.com",
      });
      const { user } = await createTestHiker({
        username: "targetuser6",
        email: "target6@test.com",
      });

      const response = await request(app)
        .put(`/admin/users/${user._id}`)
        .set("Authorization", `Bearer ${adminToken}`)
        .send({ username: "esistente" });

      expect(response.status).toBe(409);
      expect(response.body).toHaveProperty(
        "message",
        "Email o username già in uso.",
      );
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // DELETE /admin/users/:id — Elimina qualsiasi utente
  // ══════════════════════════════════════════════════════════════════

  describe("DELETE /admin/users/:id", () => {
    test("admin elimina un utente esistente", async () => {
      const { user } = await createTestHiker({
        username: "todelete1",
        email: "todelete1@test.com",
      });

      const response = await request(app)
        .delete(`/admin/users/${user._id}`)
        .set("Authorization", `Bearer ${adminToken}`);

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty(
        "message",
        "Utente eliminato con successo.",
      );
    });

    test("utente eliminato non è più nel DB", async () => {
      const { user } = await createTestHiker({
        username: "todelete2",
        email: "todelete2@test.com",
      });

      await request(app)
        .delete(`/admin/users/${user._id}`)
        .set("Authorization", `Bearer ${adminToken}`);

      // Verifica che non esista più
      const check = await request(app)
        .get(`/admin/users/${user._id}`)
        .set("Authorization", `Bearer ${adminToken}`);

      expect(check.status).toBe(404);
    });

    test("restituisce 401 senza token", async () => {
      const { user } = await createTestHiker({
        username: "todelete3",
        email: "todelete3@test.com",
      });

      const response = await request(app).delete(`/admin/users/${user._id}`);

      expect(response.status).toBe(401);
    });

    test("restituisce 403 se il token non è admin", async () => {
      const { user, token: hikerToken } = await createTestHiker({
        username: "todelete4",
        email: "todelete4@test.com",
      });

      const response = await request(app)
        .delete(`/admin/users/${user._id}`)
        .set("Authorization", `Bearer ${hikerToken}`);

      expect(response.status).toBe(403);
    });

    test("restituisce 404 per ID valido ma utente inesistente", async () => {
      const response = await request(app)
        .delete("/admin/users/000000000000000000000000")
        .set("Authorization", `Bearer ${adminToken}`);

      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty("message", "Utente non trovato.");
    });

    test("restituisce 400 per ID non valido (CastError)", async () => {
      const response = await request(app)
        .delete("/admin/users/id-non-valido")
        .set("Authorization", `Bearer ${adminToken}`);

      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty("message", "ID utente non valido.");
    });
  });
});
