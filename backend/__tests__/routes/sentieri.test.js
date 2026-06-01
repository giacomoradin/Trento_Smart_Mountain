import request from "supertest";
import app from "../../src/app.js";
import { createTestSentiero } from "../helpers/sessionHelper.js";

/**
 * Test suite per le route dei sentieri SAT.
 *
 * Copre:
 * - GET /api/v1/sentieri                            (lista con filtri)
 * - GET /api/v1/sentieri/stats                      (statistiche aggregate)
 * - GET /api/v1/sentieri/:codice                    (dettaglio sentiero)
 * - GET /api/v1/sentieri/destinazioni               (tutte le destinazioni)
 * - GET /api/v1/sentieri/destinazioni/:nome/sentieri (sentieri per destinazione)
 *
 * Le route sono pubbliche — nessun token richiesto.
 */

describe("Sentieri Routes", () => {
  // ── Dati di test condivisi ────────────────────────────────────────
  // Creati una volta per describe, usati nei test figli.
  // afterEach del setup.js pulisce il DB dopo ogni test,
  // quindi ogni test ricrea i sentieri di cui ha bisogno.

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/sentieri — Lista con filtri
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/sentieri", () => {
    test("restituisce tutti i sentieri senza filtri", async () => {
      await createTestSentiero({ codice: "E001", difficolta: "E" });
      await createTestSentiero({ codice: "T001", difficolta: "T" });
      await createTestSentiero({ codice: "EE001", difficolta: "EE" });

      const response = await request(app).get("/api/v1/sentieri");

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty("count", 3);
      expect(Array.isArray(response.body.data)).toBe(true);
      expect(response.body.data.length).toBe(3);
    });

    test("filtra per difficoltà", async () => {
      await createTestSentiero({ codice: "E002", difficolta: "E" });
      await createTestSentiero({ codice: "E003", difficolta: "E" });
      await createTestSentiero({ codice: "EE002", difficolta: "EE" });

      const response = await request(app).get("/api/v1/sentieri?difficolta=E");

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(2);
      response.body.data.forEach((s) => expect(s.difficolta).toBe("E"));
    });

    test("filtra per difficoltà case-insensitive", async () => {
      await createTestSentiero({ codice: "EE003", difficolta: "EE" });

      const response = await request(app).get("/api/v1/sentieri?difficolta=ee");

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(1);
      expect(response.body.data[0].difficolta).toBe("EE");
    });

    test("filtra per destinazione (ricerca parziale)", async () => {
      await createTestSentiero({
        codice: "E004",
        puntoFine: { nome: "Rifugio Pedrotti", quota: 2491 },
      });
      await createTestSentiero({
        codice: "E005",
        puntoFine: { nome: "Rifugio Tuckett", quota: 2272 },
      });
      await createTestSentiero({
        codice: "E006",
        puntoFine: { nome: "Cima Tosa", quota: 3173 },
      });

      const response = await request(app).get(
        "/api/v1/sentieri?destinazione=Rifugio",
      );

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(2);
    });

    test("filtra per dislivelloMax", async () => {
      await createTestSentiero({
        codice: "E007",
        quotaMinima: 800,
        quotaMassima: 1200,
      }); // dislivello 400
      await createTestSentiero({
        codice: "E008",
        quotaMinima: 800,
        quotaMassima: 1900,
      }); // dislivello 1100
      await createTestSentiero({
        codice: "E009",
        quotaMinima: 500,
        quotaMassima: 1300,
      }); // dislivello 800

      const response = await request(app).get(
        "/api/v1/sentieri?dislivelloMax=800",
      );

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(2);
      response.body.data.forEach((s) => {
        const dislivello = s.quotaMassima - s.quotaMinima;
        expect(dislivello).toBeLessThanOrEqual(800);
      });
    });

    test("filtra per distanzaMax", async () => {
      await createTestSentiero({ codice: "E010", lunghezzaPlanimetrica: 4000 });
      await createTestSentiero({ codice: "E011", lunghezzaPlanimetrica: 8000 });
      await createTestSentiero({
        codice: "E012",
        lunghezzaPlanimetrica: 12000,
      });

      const response = await request(app).get(
        "/api/v1/sentieri?distanzaMax=8000",
      );

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(2);
      response.body.data.forEach((s) => {
        expect(s.lunghezzaPlanimetrica).toBeLessThanOrEqual(8000);
      });
    });

    test("filtra per tempoMax", async () => {
      await createTestSentiero({ codice: "E013", tempoAndata: "02:00" });
      await createTestSentiero({ codice: "E014", tempoAndata: "03:30" });
      await createTestSentiero({ codice: "E015", tempoAndata: "05:00" });

      const response = await request(app).get(
        "/api/v1/sentieri?tempoMax=03:30",
      );

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(2);
    });

    test("combina più filtri insieme (AND)", async () => {
      await createTestSentiero({
        codice: "E016",
        difficolta: "E",
        lunghezzaPlanimetrica: 4000,
        tempoAndata: "02:00",
      });
      await createTestSentiero({
        codice: "EE004",
        difficolta: "EE",
        lunghezzaPlanimetrica: 4000,
        tempoAndata: "02:00",
      });
      await createTestSentiero({
        codice: "E017",
        difficolta: "E",
        lunghezzaPlanimetrica: 15000,
        tempoAndata: "06:00",
      });

      const response = await request(app).get(
        "/api/v1/sentieri?difficolta=E&distanzaMax=5000&tempoMax=03:00",
      );

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(1);
      expect(response.body.data[0].codice).toBe("E016");
    });

    test("restituisce lista vuota se nessun sentiero matcha i filtri", async () => {
      await createTestSentiero({ codice: "E018", difficolta: "E" });

      const response = await request(app).get(
        "/api/v1/sentieri?difficolta=EEA",
      );

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(0);
      expect(response.body.data).toEqual([]);
    });

    test("rispetta il limite (limit query param)", async () => {
      await createTestSentiero({ codice: "E019" });
      await createTestSentiero({ codice: "E020" });
      await createTestSentiero({ codice: "E021" });

      const response = await request(app).get("/api/v1/sentieri?limit=2");

      expect(response.status).toBe(200);
      expect(response.body.data.length).toBeLessThanOrEqual(2);
    });

    test("non espone percorsoCoordinate nella lista", async () => {
      await createTestSentiero({ codice: "E022" });

      const response = await request(app).get("/api/v1/sentieri");

      expect(response.status).toBe(200);
      response.body.data.forEach((s) => {
        expect(s).not.toHaveProperty("percorsoCoordinate");
      });
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/sentieri/stats — Statistiche aggregate
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/sentieri/stats", () => {
    test("restituisce statistiche aggregate corrette", async () => {
      await createTestSentiero({ codice: "S001", difficolta: "T" });
      await createTestSentiero({ codice: "S002", difficolta: "E" });
      await createTestSentiero({ codice: "S003", difficolta: "E" });
      await createTestSentiero({ codice: "S004", difficolta: "EE" });

      const response = await request(app).get("/api/v1/sentieri/stats");

      expect(response.status).toBe(200);
      expect(response.body.data).toHaveProperty("totalTrails", 4);
      expect(response.body.data).toHaveProperty("totalDestinations");
      expect(response.body.data).toHaveProperty("byDifficulty");
      expect(response.body.data.byDifficulty).toHaveProperty("T", 1);
      expect(response.body.data.byDifficulty).toHaveProperty("E", 2);
      expect(response.body.data.byDifficulty).toHaveProperty("EE", 1);
    });

    test("restituisce zero totali se non ci sono sentieri", async () => {
      const response = await request(app).get("/api/v1/sentieri/stats");

      expect(response.status).toBe(200);
      expect(response.body.data.totalTrails).toBe(0);
      expect(response.body.data.totalDestinations).toBe(0);
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/sentieri/:codice — Dettaglio sentiero
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/sentieri/:codice", () => {
    test("restituisce il sentiero completo con coordinate", async () => {
      await createTestSentiero({ codice: "C001", difficolta: "E" });

      const response = await request(app).get("/api/v1/sentieri/C001");

      expect(response.status).toBe(200);
      expect(response.body.data).toHaveProperty("codice", "C001");
      expect(response.body.data).toHaveProperty("difficolta", "E");
      // Il dettaglio include le coordinate (a differenza della lista)
      expect(response.body.data).toHaveProperty("percorsoCoordinate");
    });

    test("è case-insensitive sul codice", async () => {
      await createTestSentiero({ codice: "C002" });

      const response = await request(app).get("/api/v1/sentieri/c002");

      expect(response.status).toBe(200);
      expect(response.body.data.codice).toBe("C002");
    });

    test("restituisce 404 per codice inesistente", async () => {
      const response = await request(app).get("/api/v1/sentieri/INESISTENTE");

      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty("message");
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/sentieri/destinazioni — Tutte le destinazioni
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/sentieri/destinazioni", () => {
    test("restituisce le destinazioni uniche con statistiche", async () => {
      await createTestSentiero({
        codice: "D001",
        puntoFine: { nome: "Rifugio Pedrotti", quota: 2491 },
      });
      await createTestSentiero({
        codice: "D002",
        puntoFine: { nome: "Rifugio Pedrotti", quota: 2491 },
      });
      await createTestSentiero({
        codice: "D003",
        puntoFine: { nome: "Cima Tosa", quota: 3173 },
      });

      const response = await request(app).get("/api/v1/sentieri/destinazioni");

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(2); // 2 destinazioni uniche
      expect(Array.isArray(response.body.data)).toBe(true);

      const pedrotti = response.body.data.find(
        (d) => d.nome === "Rifugio Pedrotti",
      );
      expect(pedrotti).toBeDefined();
      expect(pedrotti.numeroSentieri).toBe(2);
    });

    test("restituisce lista vuota se non ci sono sentieri", async () => {
      const response = await request(app).get("/api/v1/sentieri/destinazioni");

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(0);
      expect(response.body.data).toEqual([]);
    });

    test("le destinazioni sono ordinate alfabeticamente", async () => {
      await createTestSentiero({
        codice: "D004",
        puntoFine: { nome: "Zebra Peak", quota: 2000 },
      });
      await createTestSentiero({
        codice: "D005",
        puntoFine: { nome: "Alpe di Siusi", quota: 1800 },
      });
      await createTestSentiero({
        codice: "D006",
        puntoFine: { nome: "Monte Bondone", quota: 2180 },
      });

      const response = await request(app).get("/api/v1/sentieri/destinazioni");

      expect(response.status).toBe(200);
      const nomi = response.body.data.map((d) => d.nome);
      expect(nomi).toEqual([...nomi].sort());
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // GET /api/v1/sentieri/destinazioni/:nome/sentieri
  // ══════════════════════════════════════════════════════════════════

  describe("GET /api/v1/sentieri/destinazioni/:nome/sentieri", () => {
    test("restituisce i sentieri che portano alla destinazione", async () => {
      await createTestSentiero({
        codice: "DN001",
        puntoFine: { nome: "Rifugio Pedrotti", quota: 2491 },
      });
      await createTestSentiero({
        codice: "DN002",
        puntoFine: { nome: "Rifugio Pedrotti", quota: 2491 },
      });
      await createTestSentiero({
        codice: "DN003",
        puntoFine: { nome: "Cima Tosa", quota: 3173 },
      });

      const response = await request(app).get(
        "/api/v1/sentieri/destinazioni/Rifugio%20Pedrotti/sentieri",
      );

      expect(response.status).toBe(200);
      expect(response.body.count).toBe(2);
      expect(response.body.destinazione).toBe("Rifugio Pedrotti");
      response.body.data.forEach((s) => {
        expect(s.puntoFine.nome).toBe("Rifugio Pedrotti");
      });
    });

    test("non espone percorsoCoordinate nella lista per destinazione", async () => {
      await createTestSentiero({
        codice: "DN004",
        puntoFine: { nome: "Monte Altissimo", quota: 2079 },
      });

      const response = await request(app).get(
        "/api/v1/sentieri/destinazioni/Monte%20Altissimo/sentieri",
      );

      expect(response.status).toBe(200);
      response.body.data.forEach((s) => {
        expect(s).not.toHaveProperty("percorsoCoordinate");
      });
    });

    test("restituisce 404 per destinazione inesistente", async () => {
      const response = await request(app).get(
        "/api/v1/sentieri/destinazioni/DestinazioneInesistente/sentieri",
      );

      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty("message");
    });
  });
});
