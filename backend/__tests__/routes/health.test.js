import request from "supertest";
import app from "../../src/app.js";

/**
 * Health-check: usato dal monitor del PaaS (Render) per liveness/readiness.
 * Nei test il DB (mongodb-memory-server) è connesso → readyState 1 → 200 "ok".
 */
describe("GET /health", () => {
  test("returns 200 ok with db connected", async () => {
    const res = await request(app).get("/health");
    expect(res.status).toBe(200);
    expect(res.body.status).toBe("ok");
    expect(res.body.db).toBe("connected");
    expect(typeof res.body.uptimeSec).toBe("number");
  });

  test("alias /api/v1/health works too", async () => {
    const res = await request(app).get("/api/v1/health");
    expect(res.status).toBe(200);
    expect(res.body.status).toBe("ok");
  });
});
