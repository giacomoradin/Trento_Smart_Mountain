import request from "supertest";
import bcrypt from "bcrypt";
import app from "../../src/app.js";
import Refuge from "../../src/models/refuge.js";
import { createTestHiker, generateValidToken } from "../helpers/authHelper.js";

/** Crea un account rifugio di test + token con role "rifugio". */
async function createTestRefuge(rifugioName = "Rifugio Test", over = {}) {
  const uniq = `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
  const passwordHash = await bcrypt.hash("TestPassword123!", 10);
  const refuge = await Refuge.create({
    username: over.username || `rif_${uniq}`,
    email: over.email || `rif_${uniq}@test.com`,
    passwordHash,
    isVerified: true,
    rifugioName,
    quota: 2243,
  });
  const token = generateValidToken(refuge._id.toString(), "rifugio");
  return { refuge: refuge.toObject(), token };
}

describe("Board Routes (Bacheca rifugi)", () => {
  test("refuge can create a post; any user can read it in the feed", async () => {
    const r = await createTestRefuge("Rifugio Vajolet");
    const create = await request(app)
      .post("/api/v1/board")
      .set("Authorization", `Bearer ${r.token}`)
      .send({ type: "pericolo", title: "Rischio valanghe", body: "Evitare il versante nord." });
    expect(create.status).toBe(201);
    expect(create.body.type).toBe("pericolo");
    expect(create.body.refugeName).toBe("Rifugio Vajolet");

    const hiker = await createTestHiker({ username: "zzboardh1", email: "zzboardh1@test.com" });
    const list = await request(app)
      .get("/api/v1/board")
      .set("Authorization", `Bearer ${hiker.token}`);
    expect(list.status).toBe(200);
    expect(list.body.items.map((i) => i.title)).toContain("Rischio valanghe");
  });

  test("hiker cannot publish to the board (403)", async () => {
    const hiker = await createTestHiker({ username: "zzboardh2", email: "zzboardh2@test.com" });
    const res = await request(app)
      .post("/api/v1/board")
      .set("Authorization", `Bearer ${hiker.token}`)
      .send({ type: "info", title: "x", body: "y" });
    expect(res.status).toBe(403);
  });

  test("empty title/body is rejected by Joi (422)", async () => {
    const r = await createTestRefuge();
    const res = await request(app)
      .post("/api/v1/board")
      .set("Authorization", `Bearer ${r.token}`)
      .send({ type: "info", title: "   ", body: "" });
    expect(res.status).toBe(422);
  });

  test("invalid type is rejected by Joi (422)", async () => {
    const r = await createTestRefuge();
    const res = await request(app)
      .post("/api/v1/board")
      .set("Authorization", `Bearer ${r.token}`)
      .send({ type: "spam", title: "Titolo", body: "Testo" });
    expect(res.status).toBe(422);
  });

  test("author refuge can edit its own post; non-author cannot (PATCH)", async () => {
    const a = await createTestRefuge("Rifugio Edit");
    const b = await createTestRefuge("Rifugio Other");
    const created = await request(app)
      .post("/api/v1/board")
      .set("Authorization", `Bearer ${a.token}`)
      .send({ type: "info", title: "Orari", body: "8-18" });
    const id = created.body._id;

    const forbidden = await request(app)
      .patch(`/api/v1/board/${id}`)
      .set("Authorization", `Bearer ${b.token}`)
      .send({ title: "Hacked" });
    expect(forbidden.status).toBe(403);

    const ok = await request(app)
      .patch(`/api/v1/board/${id}`)
      .set("Authorization", `Bearer ${a.token}`)
      .send({ type: "avviso", title: "Orari aggiornati" });
    expect(ok.status).toBe(200);
    expect(ok.body.type).toBe("avviso");
    expect(ok.body.title).toBe("Orari aggiornati");
    expect(ok.body.body).toBe("8-18");
  });

  test("only the author refuge (or admin) can delete a post", async () => {
    const a = await createTestRefuge("Rifugio A");
    const b = await createTestRefuge("Rifugio B");
    const created = await request(app)
      .post("/api/v1/board")
      .set("Authorization", `Bearer ${a.token}`)
      .send({ type: "avviso", title: "Sentiero chiuso", body: "546 chiuso per frana." });
    const id = created.body._id;

    const forbidden = await request(app)
      .delete(`/api/v1/board/${id}`)
      .set("Authorization", `Bearer ${b.token}`);
    expect(forbidden.status).toBe(403);

    const ok = await request(app)
      .delete(`/api/v1/board/${id}`)
      .set("Authorization", `Bearer ${a.token}`);
    expect(ok.status).toBe(200);
  });

  test("GET /mine returns only the refuge's own posts", async () => {
    const a = await createTestRefuge("Rifugio Mine");
    await request(app)
      .post("/api/v1/board")
      .set("Authorization", `Bearer ${a.token}`)
      .send({ type: "info", title: "Post mine-test", body: "ciao" });
    const mine = await request(app)
      .get("/api/v1/board/mine")
      .set("Authorization", `Bearer ${a.token}`);
    expect(mine.status).toBe(200);
    expect(mine.body.items.every((i) => i.refugeId === a.refuge._id.toString())).toBe(true);
    expect(mine.body.items.map((i) => i.title)).toContain("Post mine-test");
  });
});
