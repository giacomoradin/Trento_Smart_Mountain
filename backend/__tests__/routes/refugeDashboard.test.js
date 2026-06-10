import request from "supertest";
import bcrypt from "bcrypt";
import app from "../../src/app.js";
import Refuge from "../../src/models/refuge.js";
import { createTestHiker, generateValidToken } from "../helpers/authHelper.js";

async function createTestRefuge(rifugioName = "Rifugio Dash") {
  const uniq = `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
  const passwordHash = await bcrypt.hash("TestPassword123!", 10);
  const refuge = await Refuge.create({
    username: `rifd_${uniq}`,
    email: `rifd_${uniq}@test.com`,
    passwordHash,
    isVerified: true,
    rifugioName,
    quota: 2243,
  });
  return { refuge: refuge.toObject(), token: generateValidToken(refuge._id.toString(), "rifugio") };
}

describe("Refuge IoT Dashboard", () => {
  test("returns a seeded dashboard for the logged-in refuge", async () => {
    const r = await createTestRefuge("Rifugio Vajolet");
    const res = await request(app)
      .get("/api/v1/refuge/dashboard")
      .set("Authorization", `Bearer ${r.token}`);
    expect(res.status).toBe(200);
    expect(res.body.refuge.name).toBe("Rifugio Vajolet");
    expect(res.body.refuge.altitudeM).toBe(2243);
    expect(res.body.live).toBe(true);
    expect(res.body.sensors).not.toBeNull();
    expect(typeof res.body.sensors.temperature.value).toBe("number");
    expect(res.body.edgeNodesTotal).toBeGreaterThan(0);
    expect(res.body.edgeNodesOnline).toBeLessThanOrEqual(res.body.edgeNodesTotal);
    expect(Array.isArray(res.body.passages.items)).toBe(true);
    expect(res.body.passages.totalCreditsToday).toBeGreaterThanOrEqual(0);
  });

  test("seed is idempotent (no duplicate edge nodes on second call)", async () => {
    const r = await createTestRefuge();
    const first = await request(app)
      .get("/api/v1/refuge/dashboard")
      .set("Authorization", `Bearer ${r.token}`);
    const second = await request(app)
      .get("/api/v1/refuge/dashboard")
      .set("Authorization", `Bearer ${r.token}`);
    expect(first.body.edgeNodesTotal).toBe(second.body.edgeNodesTotal);
    expect(first.body.edgeNodesTotal).toBe(4);
  });

  test("requires authentication", async () => {
    const res = await request(app).get("/api/v1/refuge/dashboard");
    expect(res.status).toBe(401);
  });
});
