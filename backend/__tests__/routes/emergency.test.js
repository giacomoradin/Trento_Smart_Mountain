import request from "supertest";
import app from "../../src/app.js";
import HikeSession from "../../src/models/hikeSession.js";
import Emergency from "../../src/models/emergency.js";
import { createTestHiker } from "../helpers/authHelper.js";

const VALID_SESSION_BODY = {
  routeDetails: { name: "Test Route", difficultyLevel: "E" },
};

const VALID_EMERGENCY_BODY = (sessionId, overrides = {}) => ({
  sessionId,
  emergencyType: "INJURY",
  coordinates: { type: "Point", coordinates: [11.12, 46.07] },
  beaconInstanceId: "a1b2c3d4e5f6",
  idempotencyKey: "550e8400-e29b-41d4-a716-446655440000",
  ...overrides,
});

async function createActiveSessionWithParticipant() {
  const leader = await createTestHiker({ username: "leader", email: "leader@test.com" });
  const member = await createTestHiker({ username: "member", email: "member@test.com" });

  const createRes = await request(app)
    .post("/api/v1/sessions")
    .set("Authorization", `Bearer ${leader.token}`)
    .send(VALID_SESSION_BODY);

  const sessionId = createRes.body._id;
  const inviteCode = createRes.body.inviteCode;

  await request(app)
    .post("/api/v1/sessions/join")
    .set("Authorization", `Bearer ${member.token}`)
    .send({ inviteCode });

  await request(app)
    .patch(`/api/v1/sessions/${sessionId}/status`)
    .set("Authorization", `Bearer ${leader.token}`)
    .send({ status: "ACTIVE" });

  return { leader, member, sessionId };
}

describe("Emergency Routes", () => {
  beforeEach(async () => {
    await Emergency.deleteMany({});
    await HikeSession.deleteMany({});
  });

  describe("POST /api/v1/emergencies", () => {
    test("participant can create SOS on ACTIVE session", async () => {
      const { member, sessionId } = await createActiveSessionWithParticipant();

      const res = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(VALID_EMERGENCY_BODY(sessionId));

      expect(res.status).toBe(201);
      expect(res.body.status).toBe("ACTIVE");
      expect(res.body.emergencyType).toBe("INJURY");
      expect(res.body.beaconInstanceId).toBe("a1b2c3d4e5f6");
      expect(res.body.profileSnapshot.displayName).toBe("member");
    });

    test("accepts beaconActive false (SOS senza beacon)", async () => {
      const { member, sessionId } = await createActiveSessionWithParticipant();

      const res = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(
          VALID_EMERGENCY_BODY(sessionId, {
            idempotencyKey: "660e8400-e29b-41d4-a716-446655440001",
            beaconActive: false,
          }),
        );

      expect(res.status).toBe(201);
      expect(res.body.beaconActive).toBe(false);
    });

    test("idempotent on same idempotencyKey returns 200", async () => {
      const { member, sessionId } = await createActiveSessionWithParticipant();
      const body = VALID_EMERGENCY_BODY(sessionId);

      const first = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(body);
      const second = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(body);

      expect(first.status).toBe(201);
      expect(second.status).toBe(200);
      expect(second.body._id).toBe(first.body._id);
    });

    test("rejects when session is PLANNED", async () => {
      const { token } = await createTestHiker({ username: "solo", email: "solo@test.com" });
      const createRes = await request(app)
        .post("/api/v1/sessions")
        .set("Authorization", `Bearer ${token}`)
        .send(VALID_SESSION_BODY);

      const res = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${token}`)
        .send(VALID_EMERGENCY_BODY(createRes.body._id, { idempotencyKey: "550e8400-e29b-41d4-a716-446655440001" }));

      expect(res.status).toBe(409);
    });
  });

  describe("GET /api/v1/sessions/:id/emergencies", () => {
    test("leader sees ACTIVE emergencies", async () => {
      const { leader, member, sessionId } = await createActiveSessionWithParticipant();

      await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(VALID_EMERGENCY_BODY(sessionId));

      const res = await request(app)
        .get(`/api/v1/sessions/${sessionId}/emergencies`)
        .set("Authorization", `Bearer ${leader.token}`);

      expect(res.status).toBe(200);
      expect(res.body.emergencies).toHaveLength(1);
      expect(res.body.isGroupLeader).toBe(true);
      expect(res.body.hasUnacked).toBe(true);
    });
  });

  describe("PATCH /api/v1/emergencies/:id", () => {
    test("sender can cancel SOS", async () => {
      const { member, sessionId } = await createActiveSessionWithParticipant();

      const created = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(VALID_EMERGENCY_BODY(sessionId));

      const res = await request(app)
        .patch(`/api/v1/emergencies/${created.body._id}`)
        .set("Authorization", `Bearer ${member.token}`)
        .send({ action: "cancel", reason: "MISTAKE" });

      expect(res.status).toBe(200);
      expect(res.body.status).toBe("CANCELLED_BY_SENDER");
    });

    test("leader can dismiss SOS", async () => {
      const { leader, member, sessionId } = await createActiveSessionWithParticipant();

      const created = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(VALID_EMERGENCY_BODY(sessionId));

      const res = await request(app)
        .patch(`/api/v1/emergencies/${created.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "dismiss" });

      expect(res.status).toBe(200);
      expect(res.body.status).toBe("DISMISSED");
    });

    test("leader can share with group", async () => {
      const { leader, member, sessionId } = await createActiveSessionWithParticipant();

      const created = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(VALID_EMERGENCY_BODY(sessionId));

      const res = await request(app)
        .patch(`/api/v1/emergencies/${created.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "share_with_group" });

      expect(res.status).toBe(200);
      expect(res.body.status).toBe("SHARED_WITH_GROUP");
    });

    test("leader can unshare from group", async () => {
      const { leader, member, sessionId } = await createActiveSessionWithParticipant();

      const created = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(VALID_EMERGENCY_BODY(sessionId));

      await request(app)
        .patch(`/api/v1/emergencies/${created.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "share_with_group" });

      const res = await request(app)
        .patch(`/api/v1/emergencies/${created.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "unshare_with_group" });

      expect(res.status).toBe(200);
      expect(res.body.status).toBe("ACTIVE");
    });
  });
});
