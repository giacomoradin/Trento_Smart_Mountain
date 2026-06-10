import request from "supertest";
import app from "../../src/app.js";
import { createTestHiker } from "../helpers/authHelper.js";

/**
 * Test suite per le route Stories (Fase B).
 *
 * Copre:
 * - POST   /api/v1/stories            (crea storia planned_session / activity)
 * - GET    /api/v1/stories/user/:id   (storie non scadute di un autore)
 * - POST   /api/v1/stories/:id/view   (marca vista, idempotente)
 * - DELETE /api/v1/stories/:id        (solo autore)
 * - validazione Joi (ref mancante → 422) + autorizzazione ref (403)
 */

const SESSION_BODY = {
  routeDetails: { name: "Test Route", difficultyLevel: "E" },
  meetingDate: "2026-08-01",
  meetingTime: "07:00",
};

// 1x1 PNG trasparente (data URI valido, minuscolo) per esercitare il path media.
const TINY_IMAGE =
  "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

async function createSession(token) {
  return request(app)
    .post("/api/v1/sessions")
    .set("Authorization", `Bearer ${token}`)
    .send(SESSION_BODY);
}

describe("Story Routes", () => {
  test("creates a planned_session story for own session", async () => {
    const { token } = await createTestHiker({
      username: "storyc1",
      email: "storyc1@test.com",
    });
    const session = await createSession(token);
    const res = await request(app)
      .post("/api/v1/stories")
      .set("Authorization", `Bearer ${token}`)
      .send({
        type: "planned_session",
        sessionId: session.body._id,
        caption: "Venite con noi!",
        media: [{ kind: "image", dataUri: TINY_IMAGE }],
        overlay: { title: "Test Route", difficultyLevel: "E" },
      });
    expect(res.status).toBe(201);
    expect(res.body.type).toBe("planned_session");
    expect(res.body.inviteCode).toBe(session.body.inviteCode);
    expect(res.body.media.length).toBe(1);
  });

  test("rejects planned_session without sessionId (422)", async () => {
    const { token } = await createTestHiker({
      username: "storyc2",
      email: "storyc2@test.com",
    });
    const res = await request(app)
      .post("/api/v1/stories")
      .set("Authorization", `Bearer ${token}`)
      .send({ type: "planned_session" });
    expect(res.status).toBe(422);
  });

  test("rejects activity story without activityId or sessionId (422)", async () => {
    const { token } = await createTestHiker({
      username: "storyc3",
      email: "storyc3@test.com",
    });
    const res = await request(app)
      .post("/api/v1/stories")
      .set("Authorization", `Bearer ${token}`)
      .send({ type: "activity" });
    expect(res.status).toBe(422);
  });

  test("forbids creating a story for a session the user is not a member of (403)", async () => {
    const { token: ownerToken } = await createTestHiker({
      username: "storyowner",
      email: "storyowner@test.com",
    });
    const session = await createSession(ownerToken);
    const { token: strangerToken } = await createTestHiker({
      username: "storystranger",
      email: "storystranger@test.com",
    });
    const res = await request(app)
      .post("/api/v1/stories")
      .set("Authorization", `Bearer ${strangerToken}`)
      .send({ type: "planned_session", sessionId: session.body._id });
    expect(res.status).toBe(403);
  });

  test("lists an author's non-expired stories", async () => {
    const { user, token } = await createTestHiker({
      username: "storylist",
      email: "storylist@test.com",
    });
    const session = await createSession(token);
    await request(app)
      .post("/api/v1/stories")
      .set("Authorization", `Bearer ${token}`)
      .send({ type: "planned_session", sessionId: session.body._id });
    const res = await request(app)
      .get(`/api/v1/stories/user/${user._id}`)
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.items.length).toBeGreaterThanOrEqual(1);
  });

  test("marks a story as viewed (idempotent)", async () => {
    const { user, token } = await createTestHiker({
      username: "storyview",
      email: "storyview@test.com",
    });
    const session = await createSession(token);
    const created = await request(app)
      .post("/api/v1/stories")
      .set("Authorization", `Bearer ${token}`)
      .send({ type: "planned_session", sessionId: session.body._id });
    const v1 = await request(app)
      .post(`/api/v1/stories/${created.body._id}/view`)
      .set("Authorization", `Bearer ${token}`);
    expect(v1.status).toBe(200);
    const v2 = await request(app)
      .post(`/api/v1/stories/${created.body._id}/view`)
      .set("Authorization", `Bearer ${token}`);
    expect(v2.status).toBe(200);
    const list = await request(app)
      .get(`/api/v1/stories/user/${user._id}`)
      .set("Authorization", `Bearer ${token}`);
    expect(list.body.items[0].viewedByMe).toBe(true);
  });

  test("only the author can delete a story", async () => {
    const { token } = await createTestHiker({
      username: "storydel",
      email: "storydel@test.com",
    });
    const session = await createSession(token);
    const created = await request(app)
      .post("/api/v1/stories")
      .set("Authorization", `Bearer ${token}`)
      .send({ type: "planned_session", sessionId: session.body._id });
    const { token: otherToken } = await createTestHiker({
      username: "storydel2",
      email: "storydel2@test.com",
    });
    const forbidden = await request(app)
      .delete(`/api/v1/stories/${created.body._id}`)
      .set("Authorization", `Bearer ${otherToken}`);
    expect(forbidden.status).toBe(403);
    const ok = await request(app)
      .delete(`/api/v1/stories/${created.body._id}`)
      .set("Authorization", `Bearer ${token}`);
    expect(ok.status).toBe(200);
  });

  test("returns 401 without auth token", async () => {
    const res = await request(app).post("/api/v1/stories").send({ type: "activity" });
    expect(res.status).toBe(401);
  });
});
