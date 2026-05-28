import request from "supertest";
import app from "../../src/app.js";
import Activity from "../../src/models/activity.js";
import HikeSession from "../../src/models/hikeSession.js";
import Follow from "../../src/models/follow.js";
import Comment from "../../src/models/comment.js";
import { createTestHiker } from "../helpers/authHelper.js";

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
  });

  // ──────────────────────────────────────────────────────────────────
  // User posts — GET /api/v1/users/:id/posts
  // ──────────────────────────────────────────────────────────────────

  describe("GET /api/v1/users/:id/posts", () => {
    test("self can see all own posts (shared + unshared)", async () => {
      const me = await createTestHiker({
        username: "diary40",
        email: "diary40@test.com",
      });
      const sharedAct = await createTestActivity(me.user._id);
      const privateAct = await createTestActivity(me.user._id);
      await request(app)
        .post(`/api/v1/activities/${sharedAct._id}/share`)
        .set("Authorization", `Bearer ${me.token}`)
        .send({ caption: "Pubblica" });
      // privateAct resta non condivisa
      const res = await request(app)
        .get(`/api/v1/users/${me.user._id}/posts`)
        .set("Authorization", `Bearer ${me.token}`);
      expect(res.status).toBe(200);
      expect(res.body.items).toHaveLength(2);
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
});
