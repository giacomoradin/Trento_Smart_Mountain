import request from "supertest";
import bcrypt from "bcrypt";
import app from "../../src/app.js";
import User from "../../src/models/user.js";
import Activity from "../../src/models/activity.js";
import HikeSession from "../../src/models/hikeSession.js";
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

  describe("POST /me/change-password", () => {
    test("password attuale corretta → 200 e l'hash viene aggiornato", async () => {
      const { token, user, password } = await createTestHiker({ email: "cp1@test.com", username: "cp1" });
      const res = await request(app)
        .post("/api/v1/users/change-password")
        .set("Authorization", `Bearer ${token}`)
        .send({ oldPassword: password, newPassword: "NuovaPassword456!" });
      expect(res.status).toBe(200);
      // Verifica diretta su DB: il nuovo hash matcha la nuova password, non la vecchia.
      const updated = await User.findById(user._id).select("passwordHash").lean();
      expect(await bcrypt.compare("NuovaPassword456!", updated.passwordHash)).toBe(true);
      expect(await bcrypt.compare(password, updated.passwordHash)).toBe(false);
    });

    test("password attuale errata → 403 (e l'hash NON cambia)", async () => {
      // 403 (non 401): "password errata" è distinta da "token scaduto/invalid"
      // (401) → il mobile non confonde il re-auth con una sessione scaduta (bug D).
      const { token, user, password } = await createTestHiker({ email: "cp2@test.com", username: "cp2" });
      const res = await request(app)
        .post("/api/v1/users/change-password")
        .set("Authorization", `Bearer ${token}`)
        .send({ oldPassword: "Sbagliata123!", newPassword: "NuovaPassword456!" });
      expect(res.status).toBe(403);
      const updated = await User.findById(user._id).select("passwordHash").lean();
      expect(await bcrypt.compare(password, updated.passwordHash)).toBe(true);
    });

    test("nuova password che non rispetta la policy → 422", async () => {
      const { token, password } = await createTestHiker({ email: "cp3@test.com", username: "cp3" });
      const res = await request(app)
        .post("/api/v1/users/change-password")
        .set("Authorization", `Bearer ${token}`)
        .send({ oldPassword: password, newPassword: "123" });
      expect(res.status).toBe(422);
    });
  });

  describe("DELETE /me — eliminazione account", () => {
    test("password errata → 403, account NON eliminato", async () => {
      const { token, user } = await createTestHiker({ email: "del1@test.com", username: "del1" });
      const res = await request(app)
        .delete("/api/v1/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ password: "Sbagliata123!" });
      expect(res.status).toBe(403);
      expect(await User.findById(user._id)).not.toBeNull();
    });

    test("password corretta → 200, utente e sue attività eliminati (cascade)", async () => {
      const { token, user, password } = await createTestHiker({ email: "del2@test.com", username: "del2" });
      await Activity.create({
        userId: user._id,
        name: "Da eliminare",
        startTimeMs: Date.now() - 3600_000,
        endTimeMs: Date.now(),
        actualStats: { movingSeconds: 100, totalSeconds: 120, distanceMeters: 1000, elevationGainM: 10 },
      });
      const res = await request(app)
        .delete("/api/v1/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ password });
      expect(res.status).toBe(200);
      expect(await User.findById(user._id)).toBeNull();
      expect(await Activity.countDocuments({ userId: user._id })).toBe(0);
    });

    test("leadership transfer: la sessione del creator passa a un partecipante", async () => {
      const creator = await createTestHiker({ email: "del3@test.com", username: "del3" });
      const member = await createTestHiker({ email: "del3b@test.com", username: "del3b" });
      const session = await HikeSession.create({
        creatorId: creator.user._id,
        routeDetails: { name: "Giro", difficultyLevel: "E" },
        meetingDate: "2026-09-01",
        inviteCode: "TSM-DEL3",
        status: "PLANNED",
        participants: [
          { userId: creator.user._id, role: "groupLeader" },
          { userId: member.user._id, role: "hiker" },
        ],
      });
      const res = await request(app)
        .delete("/api/v1/users/me")
        .set("Authorization", `Bearer ${creator.token}`)
        .send({ password: creator.password });
      expect(res.status).toBe(200);
      const after = await HikeSession.findById(session._id).lean();
      expect(after).not.toBeNull(); // non cancellata: c'era un altro partecipante
      expect(String(after.creatorId)).toBe(String(member.user._id));
      const memberP = after.participants.find((p) => String(p.userId) === String(member.user._id));
      expect(memberP.role).toBe("groupLeader");
      expect(after.participants.some((p) => String(p.userId) === String(creator.user._id))).toBe(false);
    });
  });

  describe("PATCH /me — cambio email", () => {
    test("email già usata da un altro utente → 409", async () => {
      await createTestHiker({ email: "taken@test.com", username: "owneremail" });
      const { token } = await createTestHiker({ email: "mine@test.com", username: "mineemail" });
      const res = await request(app)
        .patch("/api/v1/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ email: "taken@test.com" });
      expect(res.status).toBe(409);
    });
  });

  describe("GET /me/weekly-stats", () => {
    test("ritorna km/elevM/count (zero senza attività nella settimana)", async () => {
      const { token } = await createTestHiker({ email: "ws1@test.com", username: "ws1" });
      const res = await request(app)
        .get("/api/v1/users/me/weekly-stats")
        .set("Authorization", `Bearer ${token}`);
      expect(res.status).toBe(200);
      expect(res.body).toMatchObject({ km: 0, elevM: 0, count: 0 });
      expect(res.body.weekStart).toBeDefined();
      expect(res.body.weekEnd).toBeDefined();
    });
  });
});
