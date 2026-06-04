import request from "supertest";
import app from "../../src/app.js";
import Activity from "../../src/models/activity.js";
import HikeSession from "../../src/models/hikeSession.js";
import Follow from "../../src/models/follow.js";
import Comment from "../../src/models/comment.js";
import Hiker from "../../src/models/hiker.js";
import { createTestHiker } from "../helpers/authHelper.js";

/**
 * Imposta la visibilità del profilo di un utente (gate Social a livello account).
 * Default schema = "friends"; molti test della bacheca presuppongono visibilità
 * pubblica verso un viewer non-follower, quindi la forziamo esplicitamente.
 */
async function setVisibility(userId, visibility) {
  await Hiker.updateOne(
    { _id: userId },
    { "preferences.privacy.profileVisibility": visibility },
  );
}

/**
 * Test suite Social (Sprint 2 — schermata Social).
 *
 * Copre 3 famiglie di endpoint:
 *
 * 1. Follow (`/api/v1/users/:id/follow`, `/me/following`, `/me/followers`,
 *    `/:id/follow-stats`).
 * 2. Share (`/api/v1/activities/:id/share`, `/api/v1/sessions/:id/share`).
 * 3. Like (`/api/v1/activities/:id/like`, `/api/v1/sessions/:id/like`).
 *
 * Tutti gli endpoint richiedono JWT valido + sono soggetti al privacy gate
 * (vedi userPrivacy.js): le condivisioni mostrano avatar e username degli
 * autori, ma NIENTE birthDate/peso/preferenze.
 *
 * Pattern di authorization testato esplicitamente:
 *   - Share activity: solo owner
 *   - Share session: solo creator
 *   - Like su attività non condivisa: 403 NOT_SHARED (anti-enumeration)
 *   - Like su attività condivisa: chiunque
 */

describe("Social Routes", () => {
  // ──────────────────────────────────────────────────────────────────
  // Follow — POST/DELETE /api/v1/users/:id/follow
  // ──────────────────────────────────────────────────────────────────

  describe("POST /api/v1/users/:id/follow", () => {
    test("can follow another user", async () => {
      const a = await createTestHiker({
        username: "alice",
        email: "alice@test.com",
      });
      const b = await createTestHiker({
        username: "bob",
        email: "bob@test.com",
      });
      const res = await request(app)
        .post(`/api/v1/users/${b.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(201);
      // Verifica DB: documento Follow esiste
      const f = await Follow.findOne({
        followerId: a.user._id,
        followingId: b.user._id,
      });
      expect(f).not.toBeNull();
    });

    test("follow is idempotent (second POST does not create duplicate)", async () => {
      const a = await createTestHiker({
        username: "alice2",
        email: "alice2@test.com",
      });
      const b = await createTestHiker({
        username: "bob2",
        email: "bob2@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${b.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      const second = await request(app)
        .post(`/api/v1/users/${b.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      expect(second.status).toBe(201);
      const count = await Follow.countDocuments({
        followerId: a.user._id,
        followingId: b.user._id,
      });
      expect(count).toBe(1);
    });

    test("self-follow returns 400 SELF_FOLLOW", async () => {
      const a = await createTestHiker({
        username: "alice3",
        email: "alice3@test.com",
      });
      const res = await request(app)
        .post(`/api/v1/users/${a.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(400);
      expect(res.body.message).toMatch(/te stesso/i);
    });

    test("follow non-existing user returns 404", async () => {
      const a = await createTestHiker({
        username: "alice4",
        email: "alice4@test.com",
      });
      const fakeId = "507f1f77bcf86cd799439011";
      const res = await request(app)
        .post(`/api/v1/users/${fakeId}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(404);
    });

    test("rejects malformed user id with 422", async () => {
      const a = await createTestHiker({
        username: "alice5",
        email: "alice5@test.com",
      });
      const res = await request(app)
        .post("/api/v1/users/not-an-id/follow")
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(422);
    });
  });

  describe("DELETE /api/v1/users/:id/follow", () => {
    test("can unfollow after following", async () => {
      const a = await createTestHiker({
        username: "alice6",
        email: "alice6@test.com",
      });
      const b = await createTestHiker({
        username: "bob6",
        email: "bob6@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${b.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      const res = await request(app)
        .delete(`/api/v1/users/${b.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(200);
      const f = await Follow.findOne({
        followerId: a.user._id,
        followingId: b.user._id,
      });
      expect(f).toBeNull();
    });

    test("unfollow when not following returns 404 NOT_FOLLOWING", async () => {
      const a = await createTestHiker({
        username: "alice7",
        email: "alice7@test.com",
      });
      const b = await createTestHiker({
        username: "bob7",
        email: "bob7@test.com",
      });
      const res = await request(app)
        .delete(`/api/v1/users/${b.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(404);
    });
  });

  describe("GET /api/v1/users/me/following + /me/followers + /:id/follow-stats", () => {
    test("following list shows whom I follow with paginated count", async () => {
      const a = await createTestHiker({
        username: "alice8",
        email: "alice8@test.com",
      });
      const b = await createTestHiker({
        username: "bob8",
        email: "bob8@test.com",
      });
      const c = await createTestHiker({
        username: "carla8",
        email: "carla8@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${b.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      await request(app)
        .post(`/api/v1/users/${c.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      const res = await request(app)
        .get("/api/v1/users/me/following")
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(200);
      expect(res.body.count).toBe(2);
      expect(res.body.items).toHaveLength(2);
      // Verifica che username arrivi (populate) e che NON ci sia birthDate (privacy gate)
      expect(res.body.items[0].user).toHaveProperty("username");
      expect(res.body.items[0].user.personalInfo?.birthDate).toBeUndefined();
    });

    test("follow-stats shows counts + isFollowedByMe correctly", async () => {
      const a = await createTestHiker({
        username: "alice9",
        email: "alice9@test.com",
      });
      const b = await createTestHiker({
        username: "bob9",
        email: "bob9@test.com",
      });
      // Pre-follow
      const pre = await request(app)
        .get(`/api/v1/users/${b.user._id}/follow-stats`)
        .set("Authorization", `Bearer ${a.token}`);
      expect(pre.status).toBe(200);
      expect(pre.body).toMatchObject({
        followers: 0,
        following: 0,
        isFollowedByMe: false,
      });
      // Follow
      await request(app)
        .post(`/api/v1/users/${b.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);
      const post = await request(app)
        .get(`/api/v1/users/${b.user._id}/follow-stats`)
        .set("Authorization", `Bearer ${a.token}`);
      expect(post.body).toMatchObject({
        followers: 1,
        following: 0,
        isFollowedByMe: true,
      });
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Share — POST/DELETE /api/v1/activities/:id/share + /sessions/:id/share
  // ──────────────────────────────────────────────────────────────────

  async function createTestActivity(ownerId) {
    return Activity.create({
      userId: ownerId,
      name: "Test escursione",
      startTimeMs: Date.now() - 3600_000,
      endTimeMs: Date.now(),
      actualStats: {
        movingSeconds: 3000,
        totalSeconds: 3600,
        distanceMeters: 5000,
        elevationGainM: 300,
      },
    });
  }

  describe("POST /api/v1/activities/:id/share", () => {
    test("owner can share own activity with caption", async () => {
      const a = await createTestHiker({
        username: "alice10",
        email: "alice10@test.com",
      });
      const activity = await createTestActivity(a.user._id);
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${a.token}`)
        .send({ caption: "Bellissima giornata in Brenta!" });
      expect(res.status).toBe(200);
      expect(res.body.sharedAt).toBeTruthy();
      expect(res.body.caption).toBe("Bellissima giornata in Brenta!");
      // Verifica persistenza DB
      const fresh = await Activity.findById(activity._id).lean();
      expect(fresh.sharedAt).not.toBeNull();
    });

    test("owner can share with empty body (no caption)", async () => {
      const a = await createTestHiker({
        username: "alice11",
        email: "alice11@test.com",
      });
      const activity = await createTestActivity(a.user._id);
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${a.token}`)
        .send({});
      expect(res.status).toBe(200);
      expect(res.body.sharedAt).toBeTruthy();
      expect(res.body.caption).toBeNull();
    });

    test("non-owner gets 403 FORBIDDEN_NOT_OWNER", async () => {
      const a = await createTestHiker({
        username: "alice12",
        email: "alice12@test.com",
      });
      const b = await createTestHiker({
        username: "bob12",
        email: "bob12@test.com",
      });
      const activity = await createTestActivity(a.user._id);
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${b.token}`)
        .send({ caption: "Provo a rubare!" });
      expect(res.status).toBe(403);
    });

    test("caption > 200 char rejected with 422", async () => {
      const a = await createTestHiker({
        username: "alice13",
        email: "alice13@test.com",
      });
      const activity = await createTestActivity(a.user._id);
      const longCaption = "a".repeat(201);
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${a.token}`)
        .send({ caption: longCaption });
      expect(res.status).toBe(422);
    });
  });

  describe("DELETE /api/v1/activities/:id/share", () => {
    test("owner can unshare → sharedAt back to null", async () => {
      const a = await createTestHiker({
        username: "alice14",
        email: "alice14@test.com",
      });
      const activity = await createTestActivity(a.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${a.token}`)
        .send({ caption: "Bel giro" });
      const res = await request(app)
        .delete(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(200);
      const fresh = await Activity.findById(activity._id).lean();
      expect(fresh.sharedAt).toBeNull();
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Like — POST/DELETE /api/v1/activities/:id/like
  // ──────────────────────────────────────────────────────────────────

  describe("POST /api/v1/activities/:id/like", () => {
    test("any user can like a SHARED activity", async () => {
      const owner = await createTestHiker({
        username: "owner15",
        email: "owner15@test.com",
      });
      const liker = await createTestHiker({
        username: "liker15",
        email: "liker15@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      // Owner condivide
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      // Other utente mette like
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/like`)
        .set("Authorization", `Bearer ${liker.token}`);
      expect(res.status).toBe(200);
      expect(res.body).toMatchObject({ likesCount: 1, likedByMe: true });
    });

    test("like is idempotent (second POST does not double count)", async () => {
      const owner = await createTestHiker({
        username: "owner16",
        email: "owner16@test.com",
      });
      const liker = await createTestHiker({
        username: "liker16",
        email: "liker16@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      await request(app)
        .post(`/api/v1/activities/${activity._id}/like`)
        .set("Authorization", `Bearer ${liker.token}`);
      const second = await request(app)
        .post(`/api/v1/activities/${activity._id}/like`)
        .set("Authorization", `Bearer ${liker.token}`);
      expect(second.body.likesCount).toBe(1);
    });

    test("like on UNSHARED activity by non-owner returns 403 NOT_SHARED", async () => {
      const owner = await createTestHiker({
        username: "owner17",
        email: "owner17@test.com",
      });
      const stranger = await createTestHiker({
        username: "stranger17",
        email: "stranger17@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      // NON viene condivisa
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/like`)
        .set("Authorization", `Bearer ${stranger.token}`);
      expect(res.status).toBe(403);
    });

    test("owner can like own unshared activity (preferiti privati)", async () => {
      const owner = await createTestHiker({
        username: "owner18",
        email: "owner18@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/like`)
        .set("Authorization", `Bearer ${owner.token}`);
      expect(res.status).toBe(200);
      expect(res.body.likedByMe).toBe(true);
    });
  });

  describe("DELETE /api/v1/activities/:id/like", () => {
    test("can remove own like → count decreases", async () => {
      const owner = await createTestHiker({
        username: "owner19",
        email: "owner19@test.com",
      });
      const liker = await createTestHiker({
        username: "liker19",
        email: "liker19@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      await request(app)
        .post(`/api/v1/activities/${activity._id}/like`)
        .set("Authorization", `Bearer ${liker.token}`);
      const res = await request(app)
        .delete(`/api/v1/activities/${activity._id}/like`)
        .set("Authorization", `Bearer ${liker.token}`);
      expect(res.status).toBe(200);
      expect(res.body).toMatchObject({ likesCount: 0, likedByMe: false });
    });

    test("unlike idempotent (deleting non-existing like is no-op)", async () => {
      const owner = await createTestHiker({
        username: "owner20",
        email: "owner20@test.com",
      });
      const liker = await createTestHiker({
        username: "liker20",
        email: "liker20@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      const res = await request(app)
        .delete(`/api/v1/activities/${activity._id}/like`)
        .set("Authorization", `Bearer ${liker.token}`);
      expect(res.status).toBe(200);
      expect(res.body.likesCount).toBe(0);
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Feed — GET /api/v1/users/me/feed
  // ──────────────────────────────────────────────────────────────────

  describe("GET /api/v1/users/me/feed", () => {
    test("empty feed for new user with no follows", async () => {
      const a = await createTestHiker({
        username: "alone22",
        email: "alone22@test.com",
      });
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(200);
      expect(res.body).toMatchObject({ items: [], hasMore: false });
    });

    test("user sees own shared activity in their feed", async () => {
      const a = await createTestHiker({
        username: "solo23",
        email: "solo23@test.com",
      });
      const act = await createTestActivity(a.user._id);
      await request(app)
        .post(`/api/v1/activities/${act._id}/share`)
        .set("Authorization", `Bearer ${a.token}`)
        .send({ caption: "La mia uscita" });
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${a.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items).toHaveLength(1);
      expect(res.body.items[0]).toMatchObject({
        kind: "activity",
        title: "Test escursione",
        caption: "La mia uscita",
        likesCount: 0,
        likedByMe: false,
      });
      expect(res.body.items[0].user.username).toBe("solo23");
    });

    test("feed shows shared activities from followed users only", async () => {
      const me = await createTestHiker({
        username: "viewer24",
        email: "viewer24@test.com",
      });
      const followed = await createTestHiker({
        username: "friend24",
        email: "friend24@test.com",
      });
      const stranger = await createTestHiker({
        username: "stranger24",
        email: "stranger24@test.com",
      });
      // Follow uno solo
      await request(app)
        .post(`/api/v1/users/${followed.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      // Entrambi condividono
      const followedAct = await createTestActivity(followed.user._id);
      const strangerAct = await createTestActivity(stranger.user._id);
      await request(app)
        .post(`/api/v1/activities/${followedAct._id}/share`)
        .set("Authorization", `Bearer ${followed.token}`)
        .send({ caption: "Da amico" });
      await request(app)
        .post(`/api/v1/activities/${strangerAct._id}/share`)
        .set("Authorization", `Bearer ${stranger.token}`)
        .send({ caption: "Da estraneo" });
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items).toHaveLength(1);
      expect(res.body.items[0].caption).toBe("Da amico");
    });

    test("feed items sorted by sharedAt desc", async () => {
      const me = await createTestHiker({
        username: "viewer25",
        email: "viewer25@test.com",
      });
      const friend = await createTestHiker({
        username: "friend25",
        email: "friend25@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${friend.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      const old = await createTestActivity(friend.user._id);
      const recent = await createTestActivity(friend.user._id);
      await request(app)
        .post(`/api/v1/activities/${old._id}/share`)
        .set("Authorization", `Bearer ${friend.token}`)
        .send({ caption: "Old" });
      // Sleep di 30 ms per assicurare che `sharedAt` del secondo share sia
      // strettamente > del primo (resolution dell'orologio Mongo è 1 ms ma
      // sotto CI condivisa abbiamo visto flaky a < 25 ms — 30 dà margine).
      await new Promise((r) => setTimeout(r, 30));
      await request(app)
        .post(`/api/v1/activities/${recent._id}/share`)
        .set("Authorization", `Bearer ${friend.token}`)
        .send({ caption: "Recent" });
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items).toHaveLength(2);
      expect(res.body.items[0].caption).toBe("Recent");
      expect(res.body.items[1].caption).toBe("Old");
    });

    test("feed exposes likedByMe correctly", async () => {
      const me = await createTestHiker({
        username: "viewer26",
        email: "viewer26@test.com",
      });
      const friend = await createTestHiker({
        username: "friend26",
        email: "friend26@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${friend.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      const act = await createTestActivity(friend.user._id);
      await request(app)
        .post(`/api/v1/activities/${act._id}/share`)
        .set("Authorization", `Bearer ${friend.token}`)
        .send({});
      await request(app)
        .post(`/api/v1/activities/${act._id}/like`)
        .set("Authorization", `Bearer ${me.token}`);
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items[0]).toMatchObject({
        likesCount: 1,
        likedByMe: true,
      });
    });

    test("unshared activities are NOT in the feed", async () => {
      const me = await createTestHiker({
        username: "viewer27",
        email: "viewer27@test.com",
      });
      const friend = await createTestHiker({
        username: "friend27",
        email: "friend27@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${friend.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      // L'amico ha un'attività ma NON la condivide
      await createTestActivity(friend.user._id);
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items).toHaveLength(0);
    });

    test("pagination: page=2 returns next batch + hasMore false at end", async () => {
      const me = await createTestHiker({
        username: "pager28",
        email: "pager28@test.com",
      });
      // 3 attività mie condivise (mi vedo da solo nel feed)
      for (let i = 0; i < 3; i++) {
        const act = await createTestActivity(me.user._id);
        await request(app)
          .post(`/api/v1/activities/${act._id}/share`)
          .set("Authorization", `Bearer ${me.token}`)
          .send({ caption: `n${i}` });
        // Sleep 20 ms tra le 3 share così la paginazione vede sharedAt distinti
        // anche in CI lenta (vedi nota sopra sul flaky a 5 ms).
        await new Promise((r) => setTimeout(r, 20));
      }
      const page1 = await request(app)
        .get("/api/v1/users/me/feed?page=1&limit=2")
        .set("Authorization", `Bearer ${me.token}`);
      expect(page1.body.items).toHaveLength(2);
      expect(page1.body.hasMore).toBe(true);
      const page2 = await request(app)
        .get("/api/v1/users/me/feed?page=2&limit=2")
        .set("Authorization", `Bearer ${me.token}`);
      expect(page2.body.items).toHaveLength(1);
      expect(page2.body.hasMore).toBe(false);
    });

    // ── Route signature + metadata (redesign Strava-style) ──────────────
    test("feed item exposes routePolyline + activityType + difficulty for activity", async () => {
      const me = await createTestHiker({
        username: "route29",
        email: "route29@test.com",
      });
      // Attività con traccia GPS registrata + tipo + difficoltà.
      const act = await Activity.create({
        userId: me.user._id,
        name: "Cima con traccia",
        activityType: "trail",
        difficultyLevel: "EE",
        startTimeMs: Date.now() - 3600_000,
        endTimeMs: Date.now(),
        actualStats: {
          movingSeconds: 3000,
          totalSeconds: 3600,
          distanceMeters: 5000,
          elevationGainM: 300,
        },
        routePolyline: [
          { lat: 46.0, lon: 11.0 },
          { lat: 46.01, lon: 11.01 },
          { lat: 46.02, lon: 11.015 },
        ],
      });
      await request(app)
        .post(`/api/v1/activities/${act._id}/share`)
        .set("Authorization", `Bearer ${me.token}`)
        .send({});
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      const item = res.body.items[0];
      expect(item.activityType).toBe("trail");
      expect(item.difficultyLevel).toBe("EE");
      expect(Array.isArray(item.routePolyline)).toBe(true);
      expect(item.routePolyline).toHaveLength(3);
      // Primo e ultimo punto preservati (start/end marker).
      expect(item.routePolyline[0]).toMatchObject({ lat: 46.0, lon: 11.0 });
      expect(item.routePolyline[2]).toMatchObject({ lat: 46.02, lon: 11.015 });
    });

    test("feed item has null routePolyline for activity without GPS track", async () => {
      const me = await createTestHiker({
        username: "noroute30",
        email: "noroute30@test.com",
      });
      const act = await createTestActivity(me.user._id); // no routePolyline
      await request(app)
        .post(`/api/v1/activities/${act._id}/share`)
        .set("Authorization", `Bearer ${me.token}`)
        .send({});
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items[0].routePolyline).toBeNull();
    });

    test("pagination across mixed sources: global order, no loss/dupes, exact hasMore", async () => {
      const me = await createTestHiker({
        username: "pager40",
        email: "pager40@test.com",
      });
      // 3 attività + 3 sessioni, condivise alternate con sharedAt crescente.
      // Ordine di condivisione: act0, sess0, act1, sess1, act2, sess2.
      for (let i = 0; i < 3; i++) {
        const act = await createTestActivity(me.user._id);
        await request(app)
          .post(`/api/v1/activities/${act._id}/share`)
          .set("Authorization", `Bearer ${me.token}`)
          .send({ caption: `act${i}` });
        await new Promise((r) => setTimeout(r, 15));
        const sess = await HikeSession.create({
          creatorId: me.user._id,
          routeDetails: { name: `sess${i}`, difficultyLevel: "E" },
          meetingDate: "2026-08-01",
          inviteCode: `TSM-P4${i}`,
          participants: [{ userId: me.user._id, role: "groupLeader" }],
        });
        await request(app)
          .post(`/api/v1/sessions/${sess._id}/share`)
          .set("Authorization", `Bearer ${me.token}`)
          .send({ caption: `sess${i}` });
        await new Promise((r) => setTimeout(r, 15));
      }

      // Raccogli tutte le pagine (limit 2) seguendo hasMore.
      const seen = [];
      const hasMoreFlags = [];
      let page = 1;
      let guard = 0;
      while (guard++ < 10) {
        const res = await request(app)
          .get(`/api/v1/users/me/feed?page=${page}&limit=2`)
          .set("Authorization", `Bearer ${me.token}`);
        expect(res.status).toBe(200);
        seen.push(...res.body.items.map((it) => it.caption));
        hasMoreFlags.push(res.body.hasMore);
        if (!res.body.hasMore) break;
        page += 1;
      }

      // 6 post totali, nessuna perdita, nessun duplicato.
      expect(seen).toHaveLength(6);
      expect(new Set(seen).size).toBe(6);
      // Ordine globale sharedAt desc: l'ultimo condiviso è in cima.
      expect(seen[0]).toBe("sess2");
      expect(seen[5]).toBe("act0");
      // hasMore: true sulle prime due pagine, false esattamente sulla terza.
      expect(hasMoreFlags).toEqual([true, true, false]);
    });

    test("feed item derives routePolyline from session plannedRoute", async () => {
      const me = await createTestHiker({
        username: "sessroute31",
        email: "sessroute31@test.com",
      });
      const session = await HikeSession.create({
        creatorId: me.user._id,
        routeDetails: { name: "Percorso pianificato", difficultyLevel: "EEA" },
        meetingDate: "2026-09-01",
        inviteCode: "TSM-R031",
        participants: [{ userId: me.user._id, role: "groupLeader" }],
        plannedRoute: {
          source: "GPX",
          polylinePoints: [
            { lat: 46.1, lon: 11.1 },
            { lat: 46.11, lon: 11.12 },
            { lat: 46.12, lon: 11.13 },
            { lat: 46.13, lon: 11.14 },
          ],
        },
      });
      await request(app)
        .post(`/api/v1/sessions/${session._id}/share`)
        .set("Authorization", `Bearer ${me.token}`)
        .send({});
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${me.token}`);
      const item = res.body.items.find((i) => i.kind === "session");
      expect(item).toBeDefined();
      expect(item.difficultyLevel).toBe("EEA");
      expect(Array.isArray(item.routePolyline)).toBe(true);
      expect(item.routePolyline.length).toBeGreaterThanOrEqual(2);
      expect(item.routePolyline[0]).toMatchObject({ lat: 46.1, lon: 11.1 });
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Social Row — GET /api/v1/users/me/social-row
  // ──────────────────────────────────────────────────────────────────

  describe("GET /api/v1/users/me/social-row", () => {
    test("empty row when not following anyone", async () => {
      const me = await createTestHiker({
        username: "lonely60",
        email: "lonely60@test.com",
      });
      const res = await request(app)
        .get("/api/v1/users/me/social-row")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items).toEqual([]);
    });

    test("followed user with no activity → neutral status", async () => {
      const me = await createTestHiker({
        username: "viewer61",
        email: "viewer61@test.com",
      });
      const friend = await createTestHiker({
        username: "friend61",
        email: "friend61@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${friend.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      const res = await request(app)
        .get("/api/v1/users/me/social-row")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items).toHaveLength(1);
      expect(res.body.items[0].status).toBe("neutral");
      expect(res.body.items[0].user.username).toBe("friend61");
    });

    test("followed user with a non-expired story → story status (unviewed)", async () => {
      const me = await createTestHiker({
        username: "viewer62",
        email: "viewer62@test.com",
      });
      const friend = await createTestHiker({
        username: "friend62",
        email: "friend62@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${friend.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      // friend crea una sessione e una storia reale (planned_session) su di essa.
      const session = await request(app)
        .post("/api/v1/sessions")
        .set("Authorization", `Bearer ${friend.token}`)
        .send({ routeDetails: { name: "Story route", difficultyLevel: "E" }, meetingDate: "2026-08-01" });
      await request(app)
        .post("/api/v1/stories")
        .set("Authorization", `Bearer ${friend.token}`)
        .send({ type: "planned_session", sessionId: session.body._id });
      const res = await request(app)
        .get("/api/v1/users/me/social-row")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items[0].status).toBe("story");
      expect(res.body.items[0].hasUnviewedStory).toBe(true);
    });

    test("followed user with ACTIVE session → live status with sessionId", async () => {
      const me = await createTestHiker({
        username: "viewer63",
        email: "viewer63@test.com",
      });
      const friend = await createTestHiker({
        username: "friend63",
        email: "friend63@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${friend.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      const session = await HikeSession.create({
        creatorId: friend.user._id,
        routeDetails: { name: "Live escursione", difficultyLevel: "E" },
        meetingDate: "2026-08-01",
        inviteCode: "TSM-LIVE",
        status: "ACTIVE",
        participants: [{ userId: friend.user._id, role: "groupLeader" }],
      });
      const res = await request(app)
        .get("/api/v1/users/me/social-row")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items[0].status).toBe("live");
      expect(res.body.items[0].liveSessionId).toBe(String(session._id));
    });

    test("live takes priority over story when both apply", async () => {
      const me = await createTestHiker({
        username: "viewer64",
        email: "viewer64@test.com",
      });
      const friend = await createTestHiker({
        username: "friend64",
        email: "friend64@test.com",
      });
      await request(app)
        .post(`/api/v1/users/${friend.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      // Story
      const act = await createTestActivity(friend.user._id);
      await request(app)
        .post(`/api/v1/activities/${act._id}/share`)
        .set("Authorization", `Bearer ${friend.token}`)
        .send({});
      // Live (priority sopra)
      await HikeSession.create({
        creatorId: friend.user._id,
        routeDetails: { name: "Live", difficultyLevel: "E" },
        meetingDate: "2026-08-01",
        inviteCode: "TSM-LIV2",
        status: "ACTIVE",
        participants: [{ userId: friend.user._id, role: "groupLeader" }],
      });
      const res = await request(app)
        .get("/api/v1/users/me/social-row")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items[0].status).toBe("live");
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // User posts — GET /api/v1/users/:id/posts
  // ──────────────────────────────────────────────────────────────────

  describe("GET /api/v1/users/:id/posts", () => {
    test("self profile shows ONLY published (shared) posts — unshared sparisce", async () => {
      const me = await createTestHiker({
        username: "diary40",
        email: "diary40@test.com",
      });
      const sharedAct = await createTestActivity(me.user._id);
      // privateAct resta NON condivisa → non deve comparire sul profilo social
      await createTestActivity(me.user._id);
      await request(app)
        .post(`/api/v1/activities/${sharedAct._id}/share`)
        .set("Authorization", `Bearer ${me.token}`)
        .send({ caption: "Pubblica" });
      const res = await request(app)
        .get(`/api/v1/users/${me.user._id}/posts`)
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      // Solo il post condiviso (il profilo è la bacheca dei pubblicati).
      expect(res.body.items).toHaveLength(1);
      expect(res.body.items[0].id).toBe(String(sharedAct._id));
    });

    test("post rimosso dal feed (unshare) sparisce dal profilo del proprietario", async () => {
      const me = await createTestHiker({
        username: "diary41",
        email: "diary41@test.com",
      });
      const act = await createTestActivity(me.user._id);
      await request(app)
        .post(`/api/v1/activities/${act._id}/share`)
        .set("Authorization", `Bearer ${me.token}`)
        .send({ caption: "Pubblica" });
      // Unshare → "Rimuovi dal feed"
      await request(app)
        .delete(`/api/v1/activities/${act._id}/share`)
        .set("Authorization", `Bearer ${me.token}`);
      const res = await request(app)
        .get(`/api/v1/users/${me.user._id}/posts`)
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items).toHaveLength(0);
    });

    test("other viewer sees only shared posts of target user", async () => {
      const author = await createTestHiker({
        username: "author41",
        email: "author41@test.com",
      });
      const viewer = await createTestHiker({
        username: "viewer41",
        email: "viewer41@test.com",
      });
      // Profilo pubblico → la bacheca è visibile anche a un viewer non-follower.
      await setVisibility(author.user._id, "public");
      const sharedAct = await createTestActivity(author.user._id);
      await createTestActivity(author.user._id); // privata
      await request(app)
        .post(`/api/v1/activities/${sharedAct._id}/share`)
        .set("Authorization", `Bearer ${author.token}`)
        .send({ caption: "Visibile" });
      const res = await request(app)
        .get(`/api/v1/users/${author.user._id}/posts`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items).toHaveLength(1);
      expect(res.body.items[0].caption).toBe("Visibile");
    });

    test("likedByMe reflects viewer's like state, not author's", async () => {
      const author = await createTestHiker({
        username: "author42",
        email: "author42@test.com",
      });
      const viewer = await createTestHiker({
        username: "viewer42",
        email: "viewer42@test.com",
      });
      await setVisibility(author.user._id, "public");
      const act = await createTestActivity(author.user._id);
      await request(app)
        .post(`/api/v1/activities/${act._id}/share`)
        .set("Authorization", `Bearer ${author.token}`)
        .send({});
      await request(app)
        .post(`/api/v1/activities/${act._id}/like`)
        .set("Authorization", `Bearer ${viewer.token}`);
      const res = await request(app)
        .get(`/api/v1/users/${author.user._id}/posts`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.body.items[0].likedByMe).toBe(true);
      expect(res.body.items[0].likesCount).toBe(1);
    });

    test("invalid user id returns 422", async () => {
      const me = await createTestHiker({
        username: "u43",
        email: "u43@test.com",
      });
      const res = await request(app)
        .get("/api/v1/users/not-an-id/posts")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(422);
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Visibilità profilo (account-level) — modello "amici = follower"
  //   public  → visibile a chiunque
  //   friends → visibile solo ai propri follower
  //   private → visibile solo a se stessi
  // ──────────────────────────────────────────────────────────────────

  describe("Visibilità profilo nel Social", () => {
    async function shareActivityAs(owner) {
      const act = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${act._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({ caption: "post" });
      return act;
    }

    test("bacheca: autore private → viewer non vede nulla", async () => {
      const author = await createTestHiker({ username: "priv1", email: "priv1@test.com" });
      const viewer = await createTestHiker({ username: "pv1", email: "pv1@test.com" });
      await setVisibility(author.user._id, "private");
      await shareActivityAs(author);
      const res = await request(app)
        .get(`/api/v1/users/${author.user._id}/posts`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items).toHaveLength(0);
    });

    test("bacheca: autore private → l'autore vede comunque i propri post", async () => {
      const author = await createTestHiker({ username: "priv2", email: "priv2@test.com" });
      await setVisibility(author.user._id, "private");
      await shareActivityAs(author);
      const res = await request(app)
        .get(`/api/v1/users/${author.user._id}/posts`)
        .set("Authorization", `Bearer ${author.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items).toHaveLength(1);
    });

    test("bacheca: autore friends → non-follower non vede, follower sì", async () => {
      const author = await createTestHiker({ username: "fr1", email: "fr1@test.com" });
      const viewer = await createTestHiker({ username: "fv1", email: "fv1@test.com" });
      await setVisibility(author.user._id, "friends");
      await shareActivityAs(author);

      // Non-follower → bacheca vuota
      let res = await request(app)
        .get(`/api/v1/users/${author.user._id}/posts`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.body.items).toHaveLength(0);

      // Diventa follower → ora vede i post condivisi
      await request(app)
        .post(`/api/v1/users/${author.user._id}/follow`)
        .set("Authorization", `Bearer ${viewer.token}`);
      res = await request(app)
        .get(`/api/v1/users/${author.user._id}/posts`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.body.items).toHaveLength(1);
    });

    test("bacheca: autore public → anche un non-follower vede i post condivisi", async () => {
      const author = await createTestHiker({ username: "pub1", email: "pub1@test.com" });
      const viewer = await createTestHiker({ username: "puv1", email: "puv1@test.com" });
      await setVisibility(author.user._id, "public");
      await shareActivityAs(author);
      const res = await request(app)
        .get(`/api/v1/users/${author.user._id}/posts`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.body.items).toHaveLength(1);
    });

    test("feed: un autore private seguito NON compare nel feed", async () => {
      const me = await createTestHiker({ username: "feedme1", email: "feedme1@test.com" });
      const privateAuthor = await createTestHiker({ username: "fpriv1", email: "fpriv1@test.com" });
      const publicAuthor = await createTestHiker({ username: "fpub1", email: "fpub1@test.com" });
      await setVisibility(privateAuthor.user._id, "private");
      await setVisibility(publicAuthor.user._id, "public");
      // Seguo entrambi
      await request(app)
        .post(`/api/v1/users/${privateAuthor.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      await request(app)
        .post(`/api/v1/users/${publicAuthor.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      // Entrambi condividono
      await shareActivityAs(privateAuthor);
      await shareActivityAs(publicAuthor);
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      // Solo il post dell'autore pubblico è presente.
      expect(res.body.items).toHaveLength(1);
      expect(res.body.items[0].user.username).toBe("fpub1");
    });

    test("feed: un autore friends seguito compare (il viewer ne è follower)", async () => {
      const me = await createTestHiker({ username: "feedme2", email: "feedme2@test.com" });
      const friendAuthor = await createTestHiker({ username: "ffr1", email: "ffr1@test.com" });
      await setVisibility(friendAuthor.user._id, "friends");
      await request(app)
        .post(`/api/v1/users/${friendAuthor.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);
      await shareActivityAs(friendAuthor);
      const res = await request(app)
        .get("/api/v1/users/me/feed")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.items).toHaveLength(1);
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Comments — POST/GET/DELETE su Activity
  // ──────────────────────────────────────────────────────────────────

  describe("POST /api/v1/activities/:id/comments", () => {
    test("any user can comment on SHARED activity", async () => {
      const owner = await createTestHiker({
        username: "owner30",
        email: "owner30@test.com",
      });
      const commenter = await createTestHiker({
        username: "commenter30",
        email: "commenter30@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${commenter.token}`)
        .send({ text: "Bellissimo giro!" });
      expect(res.status).toBe(201);
      expect(res.body.comment).toMatchObject({
        text: "Bellissimo giro!",
        kind: "activity",
      });
      expect(res.body.comment.userId.username).toBe("commenter30");
      // Verifica $inc commentsCount sul parent
      const fresh = await Activity.findById(activity._id).lean();
      expect(fresh.commentsCount).toBe(1);
    });

    test("comment is trimmed before save", async () => {
      const owner = await createTestHiker({
        username: "owner31",
        email: "owner31@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({ text: "   spazi extra   " });
      expect(res.status).toBe(201);
      expect(res.body.comment.text).toBe("spazi extra");
    });

    test("comment text > 500 char rejected with 422", async () => {
      const owner = await createTestHiker({
        username: "owner32",
        email: "owner32@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({ text: "x".repeat(501) });
      expect(res.status).toBe(422);
    });

    test("empty text rejected with 422", async () => {
      const owner = await createTestHiker({
        username: "owner33",
        email: "owner33@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({ text: "" });
      expect(res.status).toBe(422);
    });

    test("comment on UNSHARED activity by non-owner returns 403 NOT_SHARED", async () => {
      const owner = await createTestHiker({
        username: "owner34",
        email: "owner34@test.com",
      });
      const stranger = await createTestHiker({
        username: "stranger34",
        email: "stranger34@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${stranger.token}`)
        .send({ text: "Sto provando a entrare" });
      expect(res.status).toBe(403);
    });

    test("owner can comment on own unshared activity (private diary)", async () => {
      const owner = await createTestHiker({
        username: "owner35",
        email: "owner35@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      const res = await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({ text: "Nota privata" });
      expect(res.status).toBe(201);
    });
  });

  describe("GET /api/v1/activities/:id/comments", () => {
    test("list comments ordered by createdAt desc (latest first)", async () => {
      const owner = await createTestHiker({
        username: "owner36",
        email: "owner36@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({ text: "Primo" });
      await new Promise((r) => setTimeout(r, 30));
      await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({ text: "Secondo" });
      const res = await request(app)
        .get(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${owner.token}`);
      expect(res.status).toBe(200);
      expect(res.body.count).toBe(2);
      // Più recente in cima
      expect(res.body.items[0].text).toBe("Secondo");
      expect(res.body.items[1].text).toBe("Primo");
    });
  });

  describe("DELETE /api/v1/activities/:id/comments/:cid", () => {
    test("author can delete own comment + count decremented", async () => {
      const owner = await createTestHiker({
        username: "owner37",
        email: "owner37@test.com",
      });
      const activity = await createTestActivity(owner.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({});
      const create = await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${owner.token}`)
        .send({ text: "Da cancellare" });
      const cid = create.body.comment._id;
      const res = await request(app)
        .delete(`/api/v1/activities/${activity._id}/comments/${cid}`)
        .set("Authorization", `Bearer ${owner.token}`);
      expect(res.status).toBe(200);
      const fresh = await Activity.findById(activity._id).lean();
      expect(fresh.commentsCount).toBe(0);
      const exists = await Comment.findById(cid);
      expect(exists).toBeNull();
    });

    test("non-author cannot delete (403 FORBIDDEN_NOT_AUTHOR)", async () => {
      const a = await createTestHiker({
        username: "owner38",
        email: "owner38@test.com",
      });
      const b = await createTestHiker({
        username: "other38",
        email: "other38@test.com",
      });
      const activity = await createTestActivity(a.user._id);
      await request(app)
        .post(`/api/v1/activities/${activity._id}/share`)
        .set("Authorization", `Bearer ${a.token}`)
        .send({});
      const create = await request(app)
        .post(`/api/v1/activities/${activity._id}/comments`)
        .set("Authorization", `Bearer ${a.token}`)
        .send({ text: "Mio" });
      const res = await request(app)
        .delete(`/api/v1/activities/${activity._id}/comments/${create.body.comment._id}`)
        .set("Authorization", `Bearer ${b.token}`);
      expect(res.status).toBe(403);
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Share + Like su HikeSession — solo smoke (la logica è la stessa)
  // ──────────────────────────────────────────────────────────────────

  describe("HikeSession share/like (smoke)", () => {
    test("creator can share session, non-creator cannot", async () => {
      const creator = await createTestHiker({
        username: "leader21",
        email: "leader21@test.com",
      });
      const stranger = await createTestHiker({
        username: "stranger21",
        email: "stranger21@test.com",
      });
      // Crea sessione direttamente nel DB (skip route POST per velocità)
      const session = await HikeSession.create({
        creatorId: creator.user._id,
        routeDetails: {
          name: "Test peak",
          difficultyLevel: "E",
        },
        meetingDate: "2026-08-01",
        inviteCode: "TSM-A001",
        participants: [
          { userId: creator.user._id, role: "groupLeader" },
        ],
      });
      const okRes = await request(app)
        .post(`/api/v1/sessions/${session._id}/share`)
        .set("Authorization", `Bearer ${creator.token}`)
        .send({ caption: "Pronti per la prossima!" });
      expect(okRes.status).toBe(200);
      const forbidden = await request(app)
        .post(`/api/v1/sessions/${session._id}/share`)
        .set("Authorization", `Bearer ${stranger.token}`)
        .send({});
      expect(forbidden.status).toBe(403);
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Discovery — GET /api/v1/users/search
  // ──────────────────────────────────────────────────────────────────

  describe("GET /api/v1/users/search", () => {
    test("finds users by partial username, case-insensitive, excludes self", async () => {
      const me = await createTestHiker({
        username: "zzsrchviewer",
        email: "zzsrchviewer@test.com",
      });
      await createTestHiker({ username: "zzsrchmarco", email: "zzsrchmarco@test.com" });
      await createTestHiker({ username: "zzsrchmarta", email: "zzsrchmarta@test.com" });
      await createTestHiker({ username: "zzsrchgianni", email: "zzsrchgianni@test.com" });

      // Query uppercase contro username lowercase → match case-insensitive.
      const res = await request(app)
        .get("/api/v1/users/search")
        .query({ q: "ZZSRCHMAR" })
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      const names = res.body.items.map((i) => i.user.username).sort();
      expect(names).toEqual(["zzsrchmarco", "zzsrchmarta"]);
      // Self mai incluso anche se matcha (qui non matcha, ma controllo difensivo).
      expect(names).not.toContain("zzsrchviewer");
    });

    test("sets isFollowedByMe per result", async () => {
      const me = await createTestHiker({
        username: "zzfviewer",
        email: "zzfviewer@test.com",
      });
      const marco = await createTestHiker({ username: "zzfmarco", email: "zzfmarco@test.com" });
      await createTestHiker({ username: "zzfmarta", email: "zzfmarta@test.com" });
      await request(app)
        .post(`/api/v1/users/${marco.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);

      const res = await request(app)
        .get("/api/v1/users/search")
        .query({ q: "zzfmar" })
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      const byName = Object.fromEntries(
        res.body.items.map((i) => [i.user.username, i.isFollowedByMe]),
      );
      expect(byName.zzfmarco).toBe(true);
      expect(byName.zzfmarta).toBe(false);
    });

    test("returns empty for term shorter than 2 chars", async () => {
      const me = await createTestHiker({ username: "zzshort", email: "zzshort@test.com" });
      const res = await request(app)
        .get("/api/v1/users/search")
        .query({ q: "z" })
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items).toEqual([]);
    });

    test("escapes regex metacharacters (no injection / match-all)", async () => {
      const me = await createTestHiker({ username: "zzrxviewer", email: "zzrxviewer@test.com" });
      await createTestHiker({ username: "zzrxuser", email: "zzrxuser@test.com" });
      // ".*" matcherebbe tutto se interpretato come regex; escapato → letterale
      // "\.\*" che nessun username contiene → 0 risultati.
      const res = await request(app)
        .get("/api/v1/users/search")
        .query({ q: ".*" })
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items.length).toBe(0);
    });

    test("requires authentication", async () => {
      const res = await request(app).get("/api/v1/users/search").query({ q: "zz" });
      expect(res.status).toBe(401);
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Social graph — GET /api/v1/users/:id/followers and /following
  // ──────────────────────────────────────────────────────────────────

  describe("GET /api/v1/users/:id/followers and /following", () => {
    test("lists followers and following of an arbitrary user", async () => {
      const alice = await createTestHiker({ username: "zzgalice", email: "zzgalice@test.com" });
      const bob = await createTestHiker({ username: "zzgbob", email: "zzgbob@test.com" });
      const carol = await createTestHiker({ username: "zzgcarol", email: "zzgcarol@test.com" });

      // bob e carol seguono alice; alice segue carol.
      await request(app)
        .post(`/api/v1/users/${alice.user._id}/follow`)
        .set("Authorization", `Bearer ${bob.token}`);
      await request(app)
        .post(`/api/v1/users/${alice.user._id}/follow`)
        .set("Authorization", `Bearer ${carol.token}`);
      await request(app)
        .post(`/api/v1/users/${carol.user._id}/follow`)
        .set("Authorization", `Bearer ${alice.token}`);

      const followers = await request(app)
        .get(`/api/v1/users/${alice.user._id}/followers`)
        .set("Authorization", `Bearer ${bob.token}`);
      expect(followers.status).toBe(200);
      expect(followers.body.count).toBe(2);
      const followerNames = followers.body.items.map((i) => i.user.username).sort();
      expect(followerNames).toEqual(["zzgbob", "zzgcarol"]);

      const following = await request(app)
        .get(`/api/v1/users/${alice.user._id}/following`)
        .set("Authorization", `Bearer ${bob.token}`);
      expect(following.status).toBe(200);
      expect(following.body.count).toBe(1);
      expect(following.body.items[0].user.username).toBe("zzgcarol");
    });

    test("rejects malformed user id with 422", async () => {
      const me = await createTestHiker({ username: "zzgbad", email: "zzgbad@test.com" });
      const res = await request(app)
        .get("/api/v1/users/not-an-id/followers")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(422);
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Profilo — GET /api/v1/users/:id/hiking-stats
  // ──────────────────────────────────────────────────────────────────

  describe("GET /api/v1/users/:id/hiking-stats", () => {
    test("aggregates all-time totals from sessions + free activities", async () => {
      const me = await createTestHiker({ username: "zzhstats", email: "zzhstats@test.com" });
      // Attività libera: 5km, 300m, 50pt
      await Activity.create({
        userId: me.user._id,
        name: "Libera",
        startTimeMs: Date.now() - 3600_000,
        endTimeMs: Date.now(),
        actualStats: {
          movingSeconds: 3000,
          totalSeconds: 3600,
          distanceMeters: 5000,
          elevationGainM: 300,
          finalPoints: 50,
        },
      });
      // Sessione COMPLETED: 8km, 600m, 80pt
      await HikeSession.create({
        creatorId: me.user._id,
        routeDetails: { name: "Cima", difficultyLevel: "EE" },
        meetingDate: "2026-08-01",
        inviteCode: "TSM-HS01",
        status: "COMPLETED",
        participants: [{ userId: me.user._id, role: "groupLeader" }],
        actualStats: {
          movingSeconds: 5000,
          totalSeconds: 6000,
          distanceMeters: 8000,
          elevationGainM: 600,
          finalPoints: 80,
        },
      });

      const res = await request(app)
        .get(`/api/v1/users/${me.user._id}/hiking-stats`)
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      expect(res.body.totalActivities).toBe(2);
      expect(res.body.totalDistanceKm).toBeCloseTo(13.0, 5);
      expect(res.body.totalElevationGainM).toBe(900);
      expect(res.body.totalPoints).toBe(130);
    });

    test("returns zeros for a user with no activities", async () => {
      const me = await createTestHiker({ username: "zzhempty", email: "zzhempty@test.com" });
      const res = await request(app)
        .get(`/api/v1/users/${me.user._id}/hiking-stats`)
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      expect(res.body).toMatchObject({
        totalActivities: 0,
        totalDistanceKm: 0,
        totalElevationGainM: 0,
        totalPoints: 0,
      });
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Classifica — GET /api/v1/users/me/weekly-leaderboard
  // ──────────────────────────────────────────────────────────────────

  describe("GET /api/v1/users/me/weekly-leaderboard", () => {
    test("ranks viewer + followed users by weekly km, excludes non-followed", async () => {
      const me = await createTestHiker({ username: "zzlbme", email: "zzlbme@test.com" });
      const friend = await createTestHiker({ username: "zzlbfriend", email: "zzlbfriend@test.com" });
      const stranger = await createTestHiker({ username: "zzlbstr", email: "zzlbstr@test.com" });
      await request(app)
        .post(`/api/v1/users/${friend.user._id}/follow`)
        .set("Authorization", `Bearer ${me.token}`);

      // friend: 10km — me: 5km — stranger (non seguito): 99km
      await Activity.create({
        userId: friend.user._id, name: "F",
        startTimeMs: Date.now() - 3600_000, endTimeMs: Date.now(),
        actualStats: { movingSeconds: 4000, totalSeconds: 4500, distanceMeters: 10000, elevationGainM: 500, finalPoints: 100 },
      });
      await Activity.create({
        userId: me.user._id, name: "M",
        startTimeMs: Date.now() - 3600_000, endTimeMs: Date.now(),
        actualStats: { movingSeconds: 3000, totalSeconds: 3600, distanceMeters: 5000, elevationGainM: 200, finalPoints: 40 },
      });
      await Activity.create({
        userId: stranger.user._id, name: "S",
        startTimeMs: Date.now() - 3600_000, endTimeMs: Date.now(),
        actualStats: { movingSeconds: 30000, totalSeconds: 33000, distanceMeters: 99000, elevationGainM: 9000, finalPoints: 900 },
      });

      const res = await request(app)
        .get("/api/v1/users/me/weekly-leaderboard")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      const names = res.body.items.map((i) => i.user.username);
      expect(names).toContain("zzlbme");
      expect(names).toContain("zzlbfriend");
      expect(names).not.toContain("zzlbstr");
      // friend (10km) primo, me (5km) dopo
      expect(res.body.items[0].user.username).toBe("zzlbfriend");
      expect(res.body.items[0].km).toBeCloseTo(10.0, 5);
      const meEntry = res.body.items.find((i) => i.user.username === "zzlbme");
      expect(meEntry.isMe).toBe(true);
    });

    test("excludes activities older than 7 days", async () => {
      const me = await createTestHiker({ username: "zzlbold", email: "zzlbold@test.com" });
      const tenDaysAgo = Date.now() - 10 * 24 * 3600 * 1000;
      await Activity.create({
        userId: me.user._id, name: "Old",
        startTimeMs: tenDaysAgo, endTimeMs: tenDaysAgo,
        completedAt: new Date(tenDaysAgo),
        actualStats: { movingSeconds: 8000, totalSeconds: 9000, distanceMeters: 20000, elevationGainM: 1000, finalPoints: 200 },
      });
      const res = await request(app)
        .get("/api/v1/users/me/weekly-leaderboard")
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items.find((i) => i.user.username === "zzlbold")).toBeUndefined();
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Notifiche — /api/v1/users/me/notifications
  // ──────────────────────────────────────────────────────────────────

  describe("Notifications", () => {
    test("follow creates a 'follow' notification for the followed user", async () => {
      const a = await createTestHiker({ username: "zznfa", email: "zznfa@test.com" });
      const b = await createTestHiker({ username: "zznfb", email: "zznfb@test.com" });
      await request(app)
        .post(`/api/v1/users/${b.user._id}/follow`)
        .set("Authorization", `Bearer ${a.token}`);

      const res = await request(app)
        .get("/api/v1/users/me/notifications")
        .set("Authorization", `Bearer ${b.token}`);
      expect(res.status).toBe(200);
      expect(res.body.unreadCount).toBe(1);
      expect(res.body.items[0].type).toBe("follow");
      expect(res.body.items[0].actor.username).toBe("zznfa");
      expect(res.body.items[0].read).toBe(false);
    });

    test("liking a shared activity notifies the owner; no self-notification", async () => {
      const owner = await createTestHiker({ username: "zznfowner", email: "zznfowner@test.com" });
      const liker = await createTestHiker({ username: "zznfliker", email: "zznfliker@test.com" });
      const act = await Activity.create({
        userId: owner.user._id,
        name: "Post",
        startTimeMs: Date.now() - 3600_000,
        endTimeMs: Date.now(),
        actualStats: { movingSeconds: 3000, totalSeconds: 3600, distanceMeters: 5000, elevationGainM: 300 },
        sharedAt: new Date(),
      });
      // owner mette like al proprio post → nessuna notifica (self)
      await request(app)
        .post(`/api/v1/activities/${act._id}/like`)
        .set("Authorization", `Bearer ${owner.token}`);
      // liker mette like → notifica all'owner
      await request(app)
        .post(`/api/v1/activities/${act._id}/like`)
        .set("Authorization", `Bearer ${liker.token}`);

      const res = await request(app)
        .get("/api/v1/users/me/notifications")
        .set("Authorization", `Bearer ${owner.token}`);
      expect(res.body.unreadCount).toBe(1);
      expect(res.body.items[0].type).toBe("like");
      expect(res.body.items[0].targetKind).toBe("activity");
      expect(res.body.items[0].actor.username).toBe("zznfliker");
    });

    test("mark-all-read zeroes the unread count", async () => {
      const a = await createTestHiker({ username: "zznfra", email: "zznfra@test.com" });
      const b = await createTestHiker({ username: "zznfrb", email: "zznfrb@test.com" });
      await request(app)
        .post(`/api/v1/users/${a.user._id}/follow`)
        .set("Authorization", `Bearer ${b.token}`);

      let cnt = await request(app)
        .get("/api/v1/users/me/notifications/unread-count")
        .set("Authorization", `Bearer ${a.token}`);
      expect(cnt.body.unreadCount).toBe(1);

      await request(app)
        .post("/api/v1/users/me/notifications/read")
        .set("Authorization", `Bearer ${a.token}`);

      cnt = await request(app)
        .get("/api/v1/users/me/notifications/unread-count")
        .set("Authorization", `Bearer ${a.token}`);
      expect(cnt.body.unreadCount).toBe(0);
    });
  });

  // ──────────────────────────────────────────────────────────────────
  // Privacy gate profilo — GET /hikers/:id
  // ──────────────────────────────────────────────────────────────────

  describe("GET /hikers/:id visibility gate", () => {
    test("friends-default profile is restricted for a non-follower", async () => {
      const viewer = await createTestHiker({ username: "zzpgv", email: "zzpgv@test.com" });
      const target = await createTestHiker({ username: "zzpgt", email: "zzpgt@test.com" });
      const res = await request(app)
        .get(`/hikers/${target.user._id}`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.status).toBe(200);
      expect(res.body.restricted).toBe(true);
      expect(res.body.username).toBe("zzpgt"); // identità sempre visibile
    });

    test("following a friends-profile unlocks the full profile", async () => {
      const viewer = await createTestHiker({ username: "zzpgv2", email: "zzpgv2@test.com" });
      const target = await createTestHiker({ username: "zzpgt2", email: "zzpgt2@test.com" });
      await request(app)
        .post(`/api/v1/users/${target.user._id}/follow`)
        .set("Authorization", `Bearer ${viewer.token}`);
      const res = await request(app)
        .get(`/hikers/${target.user._id}`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.status).toBe(200);
      expect(res.body.restricted).toBeUndefined();
      expect(res.body.username).toBe("zzpgt2");
    });

    test("public profile is visible to non-followers", async () => {
      const viewer = await createTestHiker({ username: "zzpgv3", email: "zzpgv3@test.com" });
      const target = await createTestHiker({ username: "zzpgt3", email: "zzpgt3@test.com" });
      await setVisibility(target.user._id, "public");
      const res = await request(app)
        .get(`/hikers/${target.user._id}`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.status).toBe(200);
      expect(res.body.restricted).toBeUndefined();
    });

    test("private profile is restricted even for a follower", async () => {
      const viewer = await createTestHiker({ username: "zzpgv4", email: "zzpgv4@test.com" });
      const target = await createTestHiker({ username: "zzpgt4", email: "zzpgt4@test.com" });
      await setVisibility(target.user._id, "private");
      await request(app)
        .post(`/api/v1/users/${target.user._id}/follow`)
        .set("Authorization", `Bearer ${viewer.token}`);
      const res = await request(app)
        .get(`/hikers/${target.user._id}`)
        .set("Authorization", `Bearer ${viewer.token}`);
      expect(res.body.restricted).toBe(true);
    });

    test("self always sees the full profile regardless of visibility", async () => {
      const me = await createTestHiker({ username: "zzpgself", email: "zzpgself@test.com" });
      await setVisibility(me.user._id, "private");
      const res = await request(app)
        .get(`/hikers/${me.user._id}`)
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.body.restricted).toBeUndefined();
      expect(res.body.username).toBe("zzpgself");
    });
  });
});
