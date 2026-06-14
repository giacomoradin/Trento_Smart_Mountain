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

// ID ObjectId valido come formato ma assente dal DB (per i 404 deterministici).
const ABSENT_ID = "507f1f77bcf86cd799439099";

// Sessione ACTIVE con leader + N membri (oltre al leader). `members[0]` è di norma
// il MITTENTE del SOS, `members[1]` un OSSERVATORE (partecipante non-leader,
// non-mittente) usato per i test di IDOR / visibilità.
async function createActiveSessionWithMembers(memberSpecs) {
  const leader = await createTestHiker({ username: "leader", email: "leader@test.com" });

  const createRes = await request(app)
    .post("/api/v1/sessions")
    .set("Authorization", `Bearer ${leader.token}`)
    .send(VALID_SESSION_BODY);
  const sessionId = createRes.body._id;
  const inviteCode = createRes.body.inviteCode;

  const members = [];
  for (const spec of memberSpecs) {
    const m = await createTestHiker(spec);
    await request(app)
      .post("/api/v1/sessions/join")
      .set("Authorization", `Bearer ${m.token}`)
      .send({ inviteCode });
    members.push(m);
  }

  await request(app)
    .patch(`/api/v1/sessions/${sessionId}/status`)
    .set("Authorization", `Bearer ${leader.token}`)
    .send({ status: "ACTIVE" });

  return { leader, members, sessionId };
}

// Invia un SOS dal token indicato e restituisce la response (status + body).
function createSos(senderToken, sessionId, overrides = {}) {
  return request(app)
    .post("/api/v1/emergencies")
    .set("Authorization", `Bearer ${senderToken}`)
    .send(VALID_EMERGENCY_BODY(sessionId, overrides));
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
    test("sender cancel MISTAKE deletes document immediately", async () => {
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
      expect(res.body._id).toBe(created.body._id);
      const remaining = await Emergency.findById(created.body._id);
      expect(remaining).toBeNull();
    });

    test("sender cancel RESOLVED_SELF keeps record for TTL cleanup", async () => {
      const { member, sessionId } = await createActiveSessionWithParticipant();

      const created = await request(app)
        .post("/api/v1/emergencies")
        .set("Authorization", `Bearer ${member.token}`)
        .send(VALID_EMERGENCY_BODY(sessionId));

      const res = await request(app)
        .patch(`/api/v1/emergencies/${created.body._id}`)
        .set("Authorization", `Bearer ${member.token}`)
        .send({ action: "cancel", reason: "RESOLVED_SELF" });

      expect(res.status).toBe(200);
      expect(res.body.status).toBe("CANCELLED_BY_SENDER");
      expect(res.body.cancelReason).toBe("RESOLVED_SELF");
      const remaining = await Emergency.findById(created.body._id);
      expect(remaining).not.toBeNull();
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

  // ──────────────────────────────────────────────────────────────────────
  // Sicurezza: il SOS espone dati sensibili (posizione + snapshot medico). I
  // test seguenti bloccano le regressioni sulle protezioni che la security
  // review ha verificato a mano (authN, authZ per ruolo, IDOR, validazione).
  // ──────────────────────────────────────────────────────────────────────

  describe("Authentication", () => {
    test("POST without token returns 401", async () => {
      const res = await request(app)
        .post("/api/v1/emergencies")
        .send(VALID_EMERGENCY_BODY(ABSENT_ID));
      expect(res.status).toBe(401);
    });

    test("GET without token returns 401", async () => {
      const res = await request(app).get(`/api/v1/emergencies/${ABSENT_ID}`);
      expect(res.status).toBe(401);
    });

    test("PATCH without token returns 401", async () => {
      const res = await request(app)
        .patch(`/api/v1/emergencies/${ABSENT_ID}`)
        .send({ action: "dismiss" });
      expect(res.status).toBe(401);
    });
  });

  describe("Validation (createEmergencySchema)", () => {
    test("rejects malformed coordinates (empty object) with 422", async () => {
      const { members, sessionId } = await createActiveSessionWithMembers([
        { username: "m1", email: "m1@test.com" },
      ]);
      const res = await createSos(members[0].token, sessionId, { coordinates: {} });
      expect(res.status).toBe(422);
    });

    test("rejects out-of-range coordinates with 422", async () => {
      const { members, sessionId } = await createActiveSessionWithMembers([
        { username: "m1", email: "m1@test.com" },
      ]);
      const res = await createSos(members[0].token, sessionId, {
        coordinates: { type: "Point", coordinates: [999, 46.07] },
      });
      expect(res.status).toBe(422);
    });

    test("rejects missing emergencyType with 422", async () => {
      const { members, sessionId } = await createActiveSessionWithMembers([
        { username: "m1", email: "m1@test.com" },
      ]);
      const res = await createSos(members[0].token, sessionId, { emergencyType: undefined });
      expect(res.status).toBe(422);
    });
  });

  describe("Authorization — create", () => {
    test("a non-participant cannot create a SOS on the session (403)", async () => {
      const { sessionId } = await createActiveSessionWithMembers([
        { username: "m1", email: "m1@test.com" },
      ]);
      const outsider = await createTestHiker({ username: "outsider", email: "out@test.com" });

      const res = await createSos(outsider.token, sessionId, {
        idempotencyKey: "770e8400-e29b-41d4-a716-446655440002",
      });
      expect(res.status).toBe(403);
    });

    test("reusing an idempotencyKey from another user is rejected (403)", async () => {
      const { members, sessionId } = await createActiveSessionWithMembers([
        { username: "m1", email: "m1@test.com" },
        { username: "m2", email: "m2@test.com" },
      ]);
      const key = "880e8400-e29b-41d4-a716-446655440003";

      const first = await createSos(members[0].token, sessionId, { idempotencyKey: key });
      expect(first.status).toBe(201);

      const second = await createSos(members[1].token, sessionId, { idempotencyKey: key });
      expect(second.status).toBe(403);
    });
  });

  describe("Authorization & IDOR — GET /emergencies/:id", () => {
    test("a non-sender, non-leader participant cannot read an ACTIVE (unshared) SOS (403)", async () => {
      const { members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
        { username: "observer", email: "observer@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);
      expect(sos.status).toBe(201);

      const res = await request(app)
        .get(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${members[1].token}`);
      expect(res.status).toBe(403);
    });

    test("the same participant CAN read it once the leader shares it (200)", async () => {
      const { leader, members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
        { username: "observer", email: "observer@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);

      await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "share_with_group" });

      const res = await request(app)
        .get(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${members[1].token}`);
      expect(res.status).toBe(200);
      expect(res.body.status).toBe("SHARED_WITH_GROUP");
    });

    test("the sender can always read their own open SOS (200)", async () => {
      const { members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);

      const res = await request(app)
        .get(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${members[0].token}`);
      expect(res.status).toBe(200);
      expect(res.body.profileSnapshot.displayName).toBe("sender");
    });

    test("an outsider (non-participant) cannot read it (403)", async () => {
      const { members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);
      const outsider = await createTestHiker({ username: "outsider", email: "out@test.com" });

      const res = await request(app)
        .get(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${outsider.token}`);
      expect(res.status).toBe(403);
    });

    test("an unknown emergency id returns 404", async () => {
      const { members } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
      ]);
      const res = await request(app)
        .get(`/api/v1/emergencies/${ABSENT_ID}`)
        .set("Authorization", `Bearer ${members[0].token}`);
      expect(res.status).toBe(404);
    });
  });

  describe("Visibility — GET /sessions/:id/emergencies", () => {
    test("a non-sender participant does not see another member's ACTIVE SOS, but sees it after sharing", async () => {
      const { leader, members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
        { username: "observer", email: "observer@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);

      const before = await request(app)
        .get(`/api/v1/sessions/${sessionId}/emergencies`)
        .set("Authorization", `Bearer ${members[1].token}`);
      expect(before.status).toBe(200);
      expect(before.body.isGroupLeader).toBe(false);
      expect(before.body.emergencies).toHaveLength(0);

      await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "share_with_group" });

      const after = await request(app)
        .get(`/api/v1/sessions/${sessionId}/emergencies`)
        .set("Authorization", `Bearer ${members[1].token}`);
      expect(after.body.emergencies).toHaveLength(1);
    });
  });

  describe("Authorization & state machine — PATCH /emergencies/:id", () => {
    test("a participant who is not the sender cannot cancel the SOS (403)", async () => {
      const { members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
        { username: "other", email: "other@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);

      const res = await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${members[1].token}`)
        .send({ action: "cancel", reason: "MISTAKE" });
      expect(res.status).toBe(403);
    });

    test("a non-leader cannot dismiss the SOS (403)", async () => {
      const { members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);

      const res = await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${members[0].token}`)
        .send({ action: "dismiss" });
      expect(res.status).toBe(403);
    });

    test("patching an already-closed SOS returns 409", async () => {
      const { leader, members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);

      await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "dismiss" });

      const res = await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "dismiss" });
      expect(res.status).toBe(409);
    });

    test("unshare on an ACTIVE (never-shared) SOS returns 409", async () => {
      const { leader, members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);

      const res = await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "unshare_with_group" });
      expect(res.status).toBe(409);
    });

    test("sharing an already-shared SOS returns 409", async () => {
      const { leader, members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);

      await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "share_with_group" });

      const res = await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "share_with_group" });
      expect(res.status).toBe(409);
    });

    test("leader ack marks the SOS acknowledged and clears hasUnacked", async () => {
      const { leader, members, sessionId } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
      ]);
      const sos = await createSos(members[0].token, sessionId);

      const ack = await request(app)
        .patch(`/api/v1/emergencies/${sos.body._id}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "ack" });
      expect(ack.status).toBe(200);
      expect(ack.body.leaderAckAt).toBeTruthy();

      const list = await request(app)
        .get(`/api/v1/sessions/${sessionId}/emergencies`)
        .set("Authorization", `Bearer ${leader.token}`);
      expect(list.body.hasUnacked).toBe(false);
    });

    test("patching an unknown emergency id returns 404", async () => {
      const { leader } = await createActiveSessionWithMembers([
        { username: "sender", email: "sender@test.com" },
      ]);
      const res = await request(app)
        .patch(`/api/v1/emergencies/${ABSENT_ID}`)
        .set("Authorization", `Bearer ${leader.token}`)
        .send({ action: "dismiss" });
      expect(res.status).toBe(404);
    });
  });
});
