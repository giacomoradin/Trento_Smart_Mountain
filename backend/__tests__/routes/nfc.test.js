import request from "supertest";
import app from "../../src/app.js";
import NfcTotem from "../../src/models/nfcTotem.js";
import NfcScan from "../../src/models/nfcScan.js";
import { createTestHiker } from "../helpers/authHelper.js";

/**
 * Test suite NFC (Sprint 2 — checkpoint totem → crediti).
 *
 * Copre la logica critica di `nfcService.scanTotem`, finora a copertura 0%:
 *   - accredito su scan valido entro il raggio
 *   - **anti doppio-credito** giornaliero (unique partial index su scanDay):
 *     una seconda scansione lo stesso giorno NON riaccredita
 *   - rifiuto fuori raggio (OUT_OF_RANGE) senza crediti
 *   - totem inesistente → 404
 *   - auth obbligatoria + validazione body
 *
 * NB: l'anti-replay si appoggia all'unique partial index di NfcScan. In
 * mongodb-memory-server lo forziamo con `syncIndexes()` prima dei test per
 * evitare flakiness da autoIndex non ancora completato.
 */
describe("NFC Routes", () => {
  beforeAll(async () => {
    await NfcScan.syncIndexes();
  });

  // Coordinate di riferimento del totem (Trento). Lo scan "in range" usa le
  // stesse coordinate (distanza ~0); il fuori-raggio si discosta di ~1km.
  const TOTEM_LON = 11.121;
  const TOTEM_LAT = 46.072;

  async function createTotem(overrides = {}) {
    return NfcTotem.create({
      tagId: "TSM-TOTEM-DEFAULT",
      name: "Totem Test",
      location: { type: "Point", coordinates: [TOTEM_LON, TOTEM_LAT] },
      radius: 50,
      creditsReward: 25,
      ...overrides,
    });
  }

  test("scan valido entro il raggio accredita i crediti", async () => {
    const hiker = await createTestHiker({ username: "nfc1", email: "nfc1@test.com" });
    await createTotem({ tagId: "TSM-NFC-A" });

    const res = await request(app)
      .post("/api/v1/nfc/scan")
      .set("Authorization", `Bearer ${hiker.token}`)
      .send({ tagId: "TSM-NFC-A", gpsLon: TOTEM_LON, gpsLat: TOTEM_LAT });

    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      ok: true,
      alreadyScannedToday: false,
      creditsAwarded: 25,
    });
  });

  test("seconda scansione lo stesso giorno NON riaccredita (anti-replay)", async () => {
    const hiker = await createTestHiker({ username: "nfc2", email: "nfc2@test.com" });
    await createTotem({ tagId: "TSM-NFC-B" });
    const body = { tagId: "TSM-NFC-B", gpsLon: TOTEM_LON, gpsLat: TOTEM_LAT };

    const first = await request(app)
      .post("/api/v1/nfc/scan")
      .set("Authorization", `Bearer ${hiker.token}`)
      .send(body);
    expect(first.body.creditsAwarded).toBe(25);

    const second = await request(app)
      .post("/api/v1/nfc/scan")
      .set("Authorization", `Bearer ${hiker.token}`)
      .send(body);
    expect(second.status).toBe(200);
    expect(second.body).toMatchObject({
      ok: true,
      alreadyScannedToday: true,
      creditsAwarded: 0,
    });

    // Un solo scan con crediti registrato per questo utente.
    const credited = await NfcScan.countDocuments({ creditsAwarded: { $gt: 0 } });
    expect(credited).toBe(1);
  });

  test("scansione fuori raggio rifiutata (OUT_OF_RANGE), niente crediti", async () => {
    const hiker = await createTestHiker({ username: "nfc3", email: "nfc3@test.com" });
    await createTotem({ tagId: "TSM-NFC-C", radius: 50 });

    // ~1.3km dal totem → ben oltre i 50m di raggio.
    const res = await request(app)
      .post("/api/v1/nfc/scan")
      .set("Authorization", `Bearer ${hiker.token}`)
      .send({ tagId: "TSM-NFC-C", gpsLon: TOTEM_LON + 0.01, gpsLat: TOTEM_LAT + 0.01 });

    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({ ok: false, reason: "OUT_OF_RANGE" });
    expect(res.body.distance).toBeGreaterThan(50);
  });

  test("tag sconosciuto → 404", async () => {
    const hiker = await createTestHiker({ username: "nfc4", email: "nfc4@test.com" });
    const res = await request(app)
      .post("/api/v1/nfc/scan")
      .set("Authorization", `Bearer ${hiker.token}`)
      .send({ tagId: "NON-ESISTE", gpsLon: TOTEM_LON, gpsLat: TOTEM_LAT });
    expect(res.status).toBe(404);
  });

  test("scan senza autenticazione → 401", async () => {
    const res = await request(app)
      .post("/api/v1/nfc/scan")
      .send({ tagId: "X", gpsLon: TOTEM_LON, gpsLat: TOTEM_LAT });
    expect(res.status).toBe(401);
  });

  test("body senza coordinate → 422", async () => {
    const hiker = await createTestHiker({ username: "nfc5", email: "nfc5@test.com" });
    const res = await request(app)
      .post("/api/v1/nfc/scan")
      .set("Authorization", `Bearer ${hiker.token}`)
      .send({ tagId: "X" });
    expect(res.status).toBe(422);
  });
});
