import request from "supertest";
import app from "../../src/app.js";
import { createTestHiker } from "../helpers/authHelper.js";

/**
 * Test suite per /account (profilo v2 e anti-cheat lock).
 *
 * Copre:
 * - PATCH /me/personal-info: set iniziale + verifica lock birthDate
 * - PATCH /me/experience:    set iniziale + verifica lock caiLevel
 * - PATCH /me/goals:         update parziale obiettivi settimanali
 * - PATCH /me:               update username/email con regex Italian-friendly
 *
 * Il lock anti-cheat su birthDate/caiLevel è la difesa SERVER-SIDE che
 * impedisce a un utente di abbassare l'asticella aggirando l'UI (curl/Postman).
 */

describe("Account Routes", () => {
  describe("PATCH /me/personal-info — anti-cheat birthDate", () => {
    test("primo set di birthDate è permesso", async () => {
      const { token } = await createTestHiker({ email: "anti1@test.com", username: "anti1" });

      const response = await request(app)
        .patch("/api/v1/users/me/personal-info")
        .set("Authorization", `Bearer ${token}`)
        .send({
          sex: "M",
          birthDate: "1995-03-21",
          heightCm: 180,
          weightKg: 75,
        });

      expect(response.status).toBe(200);
      expect(response.body.personalInfo.sex).toBe("M");
      expect(response.body.personalInfo.heightCm).toBe(180);
      expect(response.body.personalInfo.weightKg).toBe(75);
      expect(response.body.personalInfo.birthDate).toBeDefined();
    });

    test("update di birthDate dopo prima impostazione → 409 FIELD_LOCKED", async () => {
      const { token } = await createTestHiker({ email: "anti2@test.com", username: "anti2" });

      // Set iniziale
      await request(app)
        .patch("/api/v1/users/me/personal-info")
        .set("Authorization", `Bearer ${token}`)
        .send({ birthDate: "1995-03-21" });

      // Tentativo di modifica
      const response = await request(app)
        .patch("/api/v1/users/me/personal-info")
        .set("Authorization", `Bearer ${token}`)
        .send({ birthDate: "2010-01-01" });

      expect(response.status).toBe(409);
      expect(response.body.field).toBe("birthDate");
      expect(response.body.message).toMatch(/non.*modificabile/i);
    });

    test("update di altri campi (peso/altezza) è permesso anche se birthDate è bloccato", async () => {
      const { token } = await createTestHiker({ email: "anti3@test.com", username: "anti3" });

      await request(app)
        .patch("/api/v1/users/me/personal-info")
        .set("Authorization", `Bearer ${token}`)
        .send({ birthDate: "1995-03-21", heightCm: 180 });

      const response = await request(app)
        .patch("/api/v1/users/me/personal-info")
        .set("Authorization", `Bearer ${token}`)
        .send({ heightCm: 182, weightKg: 78 });

      expect(response.status).toBe(200);
      expect(response.body.personalInfo.heightCm).toBe(182);
      expect(response.body.personalInfo.weightKg).toBe(78);
    });
  });

  describe("PATCH /me/experience — anti-cheat caiLevel", () => {
    test("primo set di caiLevel è permesso", async () => {
      const { token } = await createTestHiker({ email: "exp1@test.com", username: "exp1" });

      const response = await request(app)
        .patch("/api/v1/users/me/experience")
        .set("Authorization", `Bearer ${token}`)
        .send({
          caiLevel: "E",
          baselineFitness: "active",
          weeklyTrainingFreq: "2-3",
        });

      expect(response.status).toBe(200);
      expect(response.body.experience.caiLevel).toBe("E");
      expect(response.body.experience.baselineFitness).toBe("active");
    });

    test("update di caiLevel dopo prima impostazione → 409 FIELD_LOCKED", async () => {
      const { token } = await createTestHiker({ email: "exp2@test.com", username: "exp2" });

      await request(app)
        .patch("/api/v1/users/me/experience")
        .set("Authorization", `Bearer ${token}`)
        .send({ caiLevel: "EE" });

      const response = await request(app)
        .patch("/api/v1/users/me/experience")
        .set("Authorization", `Bearer ${token}`)
        .send({ caiLevel: "T" }); // Tenta di abbassare a Turistico per facilitare scoring

      expect(response.status).toBe(409);
      expect(response.body.field).toBe("caiLevel");
    });

    test("update fitness/training è permesso anche con caiLevel bloccato", async () => {
      const { token } = await createTestHiker({ email: "exp3@test.com", username: "exp3" });

      await request(app)
        .patch("/api/v1/users/me/experience")
        .set("Authorization", `Bearer ${token}`)
        .send({ caiLevel: "EE", baselineFitness: "active" });

      const response = await request(app)
        .patch("/api/v1/users/me/experience")
        .set("Authorization", `Bearer ${token}`)
        .send({ baselineFitness: "sport", weeklyTrainingFreq: "4+" });

      expect(response.status).toBe(200);
      expect(response.body.experience.baselineFitness).toBe("sport");
      expect(response.body.experience.weeklyTrainingFreq).toBe("4+");
    });
  });

  describe("PATCH /me — username con caratteri italiani", () => {
    test("aggiorna username con spazi (es. 'Giacomo Radin')", async () => {
      const { token } = await createTestHiker({ email: "name1@test.com", username: "name1" });

      const response = await request(app)
        .patch("/api/v1/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ username: "Giacomo Radin" });

      expect(response.status).toBe(200);
      expect(response.body.user.username).toBe("Giacomo Radin");
    });

    test("aggiorna username con apostrofo (es. \"D'Angelo\")", async () => {
      const { token } = await createTestHiker({ email: "name2@test.com", username: "name2" });

      const response = await request(app)
        .patch("/api/v1/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ username: "D'Angelo" });

      expect(response.status).toBe(200);
      expect(response.body.user.username).toBe("D'Angelo");
    });

    test("rifiuta username con caratteri non ammessi (es. '<script>')", async () => {
      const { token } = await createTestHiker({ email: "name3@test.com", username: "name3" });

      const response = await request(app)
        .patch("/api/v1/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ username: "<script>alert(1)</script>" });

      expect(response.status).toBe(422);
    });
  });

  describe("PATCH /me/goals — obiettivi settimanali", () => {
    test("set obiettivi parziali (solo km)", async () => {
      const { token } = await createTestHiker({ email: "goals1@test.com", username: "goals1" });

      const response = await request(app)
        .patch("/api/v1/users/me/goals")
        .set("Authorization", `Bearer ${token}`)
        .send({ km: 25 });

      expect(response.status).toBe(200);
      expect(response.body.weeklyGoals.km).toBe(25);
    });

    test("rifiuta valori fuori range", async () => {
      const { token } = await createTestHiker({ email: "goals2@test.com", username: "goals2" });

      const response = await request(app)
        .patch("/api/v1/users/me/goals")
        .set("Authorization", `Bearer ${token}`)
        .send({ km: 999 }); // Joi cap a 500

      expect(response.status).toBe(422);
    });
  });

  describe("POST /me/profile-complete — idempotency", () => {
    test("primo call → sets profileCompletedAt", async () => {
      const { token } = await createTestHiker({ email: "pc1@test.com", username: "pc1" });

      const r1 = await request(app)
        .post("/api/v1/users/me/profile-complete")
        .set("Authorization", `Bearer ${token}`);

      expect(r1.status).toBe(200);
      expect(r1.body.profileCompletedAt).toBeDefined();
    });

    test("call successivo → mantiene timestamp originale (idempotent)", async () => {
      const { token } = await createTestHiker({ email: "pc2@test.com", username: "pc2" });

      const r1 = await request(app)
        .post("/api/v1/users/me/profile-complete")
        .set("Authorization", `Bearer ${token}`);
      const r2 = await request(app)
        .post("/api/v1/users/me/profile-complete")
        .set("Authorization", `Bearer ${token}`);

      expect(r2.status).toBe(200);
      expect(r2.body.profileCompletedAt).toBe(r1.body.profileCompletedAt);
    });
  });
});
