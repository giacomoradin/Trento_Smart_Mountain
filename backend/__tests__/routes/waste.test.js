import request from "supertest";
import bcrypt from "bcrypt";
import app from "../../src/app.js";
import Refuge from "../../src/models/refuge.js";
import { createTestHiker, generateValidToken } from "../helpers/authHelper.js";

async function createTestRefuge() {
  const uniq = `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
  const passwordHash = await bcrypt.hash("TestPassword123!", 10);
  const refuge = await Refuge.create({
    username: `rifw_${uniq}`,
    email: `rifw_${uniq}@test.com`,
    passwordHash,
    isVerified: true,
    rifugioName: "Rifugio Waste",
    quota: 2600,
  });
  return { refuge: refuge.toObject(), token: generateValidToken(refuge._id.toString(), "rifugio") };
}

// Input di riferimento (default del simulatore web):
//   ospitiNotte = 50 × 0,6 = 30
//   W_base      = 30×60×0,2 + 30×60×0,1 = 540 kg
//   W_grigliato = 30×60×0,2 = 360 kg  → totale 900 kg
//   Elicottero: payload eff. 450 kg → 2 viaggi × (450 + 30×15) € = 1.800 € → 2,00 €/kg
const BASE_INPUT = {
  periodDays: 60,
  beds: 50,
  bedOccupancy: 0.6,
  dayVisitors: 30,
  wastePerGuestKg: 0.2,
  wastePerVisitorKg: 0.1,
  screeningPerGuestKg: 0.2,
};

describe("Refuge Waste & Logistics (ADR-002 MVP)", () => {
  describe("GET /api/v1/refuge/waste/config", () => {
    test("returns categories, vectors and regulatory limits", async () => {
      const r = await createTestRefuge();
      const res = await request(app)
        .get("/api/v1/refuge/waste/config")
        .set("Authorization", `Bearer ${r.token}`);
      expect(res.status).toBe(200);
      expect(res.body.categories).toHaveLength(6);
      expect(res.body.vectors).toHaveLength(4);
      expect(res.body.limits.storageLimitM3).toBe(30);
      expect(res.body.limits.storageLimitDays).toBe(90);
    });

    test("requires authentication", async () => {
      const res = await request(app).get("/api/v1/refuge/waste/config");
      expect(res.status).toBe(401);
    });

    test("hikers are forbidden (403)", async () => {
      const uniq = `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
      const { token } = await createTestHiker({
        username: `hkw_${uniq}`,
        email: `hkw_${uniq}@test.com`,
      });
      const res = await request(app)
        .get("/api/v1/refuge/waste/config")
        .set("Authorization", `Bearer ${token}`);
      expect(res.status).toBe(403);
    });
  });

  describe("POST /api/v1/refuge/waste/simulate", () => {
    test("computes seasonal mass balance and vector costs (reference values)", async () => {
      const r = await createTestRefuge();
      const res = await request(app)
        .post("/api/v1/refuge/waste/simulate")
        .set("Authorization", `Bearer ${r.token}`)
        .send(BASE_INPUT);
      expect(res.status).toBe(200);

      expect(res.body.input.overnightGuests).toBe(30);
      expect(res.body.totals.preMassKg).toBe(900);
      expect(res.body.totals.postMassKg).toBe(900); // nessun compattatore

      // Categorie: 6 base + Grigliato
      expect(res.body.breakdown).toHaveLength(7);
      const organico = res.body.breakdown.find((d) => d.name === "Organico");
      expect(organico.preMassKg).toBe(243); // 540 × 45%

      // Elicottero: 2 viaggi, 1.800 €, 2,00 €/kg (coerente col c_kg dell'elaborato OGA)
      const heli = res.body.vectors.find((v) => v.name === "Elicottero");
      expect(heli.trips).toBe(2);
      expect(heli.totalCostEur).toBe(1800);
      expect(heli.costPerKgEur).toBe(2.0);

      // Il vettore più economico con questi numeri è la teleferica (3 viaggi × 10 €)
      expect(res.body.cheapestVector).toBe("Teleferica");
    });

    test("compactor reductions lower mass/volume and costs", async () => {
      const r = await createTestRefuge();
      const res = await request(app)
        .post("/api/v1/refuge/waste/simulate")
        .set("Authorization", `Bearer ${r.token}`)
        .send({
          ...BASE_INPUT,
          compactorEnabled: true,
          reductions: [
            { category: "Organico", massReductionPct: 40, volumeReductionPct: 40 },
            { category: "Plastica", massReductionPct: 0, volumeReductionPct: 70 },
          ],
        });
      expect(res.status).toBe(200);
      // Organico: 243 → 145,8 kg; il totale scende di 97,2 kg
      expect(res.body.totals.postMassKg).toBeCloseTo(802.8, 1);
      expect(res.body.totals.massReductionPct).toBeGreaterThan(0);
      const plastica = res.body.breakdown.find((d) => d.name === "Plastica");
      expect(plastica.postVolumeM3).toBeCloseTo(plastica.preVolumeM3 * 0.3, 3);
    });

    test("flags the 90-day regulatory limit when season exceeds it", async () => {
      const r = await createTestRefuge();
      const res = await request(app)
        .post("/api/v1/refuge/waste/simulate")
        .set("Authorization", `Bearer ${r.token}`)
        .send({ ...BASE_INPUT, periodDays: 120 });
      expect(res.status).toBe(200);
      const types = res.body.compliance.alerts.map((a) => a.type);
      expect(types).toContain("STORAGE_DAYS_LIMIT");
      expect(res.body.compliance.criticalDay).not.toBeNull();
    });

    test("rejects invalid input with 422 (Joi)", async () => {
      const r = await createTestRefuge();
      const res = await request(app)
        .post("/api/v1/refuge/waste/simulate")
        .set("Authorization", `Bearer ${r.token}`)
        .send({ ...BASE_INPUT, bedOccupancy: 1.5 });
      expect(res.status).toBe(422);
    });

    test("hikers are forbidden (403)", async () => {
      const uniq = `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
      const { token } = await createTestHiker({
        username: `hkw2_${uniq}`,
        email: `hkw2_${uniq}@test.com`,
      });
      const res = await request(app)
        .post("/api/v1/refuge/waste/simulate")
        .set("Authorization", `Bearer ${token}`)
        .send(BASE_INPUT);
      expect(res.status).toBe(403);
    });
  });
});
