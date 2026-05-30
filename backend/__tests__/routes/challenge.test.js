import request from "supertest";
import app from "../../src/app.js";
import Challenge from "../../src/models/challenge.js";
import { createTestHiker } from "../helpers/authHelper.js";

/**
 * Test suite Challenge (sfide social) — servizio finora a copertura 0%.
 *
 * Copre il ciclo di vita: creazione, invito/risposta, lettura con progresso,
 * autorizzazioni e cancellazione. Include un **test di regressione** per il bug
 * di `cancelChallenge` (cancellava una sfida di fatto già iniziata sfruttando
 * lo stato `PENDING` non ancora riconciliato).
 */
describe("Challenge Routes", () => {
  const auth = (t) => ({ Authorization: `Bearer ${t}` });

  function futureWindow() {
    return {
      startDate: new Date(Date.now() + 24 * 3600 * 1000).toISOString(), // domani
      endDate: new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString(), // +7g
    };
  }

  async function createViaApi(token, overrides = {}) {
    const { startDate, endDate } = futureWindow();
    return request(app)
      .post("/api/v1/challenges")
      .set(auth(token))
      .send({
        title: "Sfida km settimanale",
        metric: "distance",
        targetValue: 50,
        startDate,
        endDate,
        ...overrides,
      });
  }

  test("crea sfida: 201, creator auto-accepted, status PENDING (start futuro)", async () => {
    const creator = await createTestHiker({ username: "ch1", email: "ch1@test.com" });
    const res = await createViaApi(creator.token);
    expect(res.status).toBe(201);
    expect(res.body.status).toBe("PENDING");
    const me = res.body.participants.find(
      (p) => p.userId === String(creator.user._id),
    );
    expect(me?.status).toBe("accepted");
  });

  test("invito: l'invitato può accettare", async () => {
    const creator = await createTestHiker({ username: "ch2", email: "ch2@test.com" });
    const friend = await createTestHiker({ username: "ch2b", email: "ch2b@test.com" });
    const created = await createViaApi(creator.token, {
      participantUserIds: [String(friend.user._id)],
    });
    const res = await request(app)
      .post(`/api/v1/challenges/${created.body._id}/respond`)
      .set(auth(friend.token))
      .send({ accept: true });
    expect(res.status).toBe(200);
    const fp = res.body.participants.find(
      (p) => p.userId === String(friend.user._id),
    );
    expect(fp?.status).toBe("accepted");
  });

  test("respond senza essere invitati → 403", async () => {
    const creator = await createTestHiker({ username: "ch3", email: "ch3@test.com" });
    const stranger = await createTestHiker({ username: "ch3b", email: "ch3b@test.com" });
    const created = await createViaApi(creator.token);
    const res = await request(app)
      .post(`/api/v1/challenges/${created.body._id}/respond`)
      .set(auth(stranger.token))
      .send({ accept: true });
    expect(res.status).toBe(403);
  });

  test("respond due volte → 409 (già risposto)", async () => {
    const creator = await createTestHiker({ username: "ch4", email: "ch4@test.com" });
    const friend = await createTestHiker({ username: "ch4b", email: "ch4b@test.com" });
    const created = await createViaApi(creator.token, {
      participantUserIds: [String(friend.user._id)],
    });
    const id = created.body._id;
    await request(app).post(`/api/v1/challenges/${id}/respond`).set(auth(friend.token)).send({ accept: true });
    const second = await request(app)
      .post(`/api/v1/challenges/${id}/respond`)
      .set(auth(friend.token))
      .send({ accept: false });
    expect(second.status).toBe(409);
  });

  test("lettura: partecipante OK con progress, estraneo 403", async () => {
    const creator = await createTestHiker({ username: "ch5", email: "ch5@test.com" });
    const stranger = await createTestHiker({ username: "ch5b", email: "ch5b@test.com" });
    const created = await createViaApi(creator.token);
    const id = created.body._id;
    const ok = await request(app).get(`/api/v1/challenges/${id}`).set(auth(creator.token));
    expect(ok.status).toBe(200);
    expect(Array.isArray(ok.body.progress)).toBe(true);
    const forbidden = await request(app).get(`/api/v1/challenges/${id}`).set(auth(stranger.token));
    expect(forbidden.status).toBe(403);
  });

  test("cancel: il creator può cancellare una sfida PENDING", async () => {
    const creator = await createTestHiker({ username: "ch6", email: "ch6@test.com" });
    const created = await createViaApi(creator.token);
    const res = await request(app).delete(`/api/v1/challenges/${created.body._id}`).set(auth(creator.token));
    expect(res.status).toBe(200);
    const after = await Challenge.findById(created.body._id).lean();
    expect(after.status).toBe("CANCELLED");
  });

  test("cancel: un non-creator non può cancellare → 403", async () => {
    const creator = await createTestHiker({ username: "ch7", email: "ch7@test.com" });
    const other = await createTestHiker({ username: "ch7b", email: "ch7b@test.com" });
    const created = await createViaApi(creator.token);
    const res = await request(app).delete(`/api/v1/challenges/${created.body._id}`).set(auth(other.token));
    expect(res.status).toBe(403);
  });

  // ── Regressione: cancel di una sfida di fatto già iniziata ────────────────
  test("cancel: una PENDING con startDate passata (di fatto ACTIVE) è rifiutata", async () => {
    const creator = await createTestHiker({ username: "ch8", email: "ch8@test.com" });
    // Stato "stale": PENDING ma con startDate ieri → reconcile la rende ACTIVE.
    const challenge = await Challenge.create({
      creatorId: creator.user._id,
      title: "Stale pending",
      metric: "distance",
      startDate: new Date(Date.now() - 24 * 3600 * 1000),
      endDate: new Date(Date.now() + 24 * 3600 * 1000),
      status: "PENDING",
      participants: [{ userId: creator.user._id, status: "accepted" }],
    });
    const res = await request(app)
      .delete(`/api/v1/challenges/${challenge._id}`)
      .set(auth(creator.token));
    expect(res.status).toBe(409); // CANNOT_CANCEL_RUNNING
    const after = await Challenge.findById(challenge._id).lean();
    expect(after.status).not.toBe("CANCELLED");
  });

  test("validazione: endDate <= startDate → 422", async () => {
    const creator = await createTestHiker({ username: "ch9", email: "ch9@test.com" });
    const res = await request(app)
      .post("/api/v1/challenges")
      .set(auth(creator.token))
      .send({
        title: "Finestra invalida",
        metric: "distance",
        startDate: new Date(Date.now() + 2 * 24 * 3600 * 1000).toISOString(),
        endDate: new Date(Date.now() + 24 * 3600 * 1000).toISOString(),
      });
    expect(res.status).toBe(422);
  });

  test("creazione senza auth → 401", async () => {
    const res = await request(app).post("/api/v1/challenges").send({ title: "X", metric: "distance", ...futureWindow() });
    expect(res.status).toBe(401);
  });
});
