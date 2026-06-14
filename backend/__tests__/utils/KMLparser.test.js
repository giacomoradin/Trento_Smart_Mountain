import { jest } from "@jest/globals";
import fs from "fs";
import os from "os";
import path from "path";
import KMLParser from "../../src/utils/KMLparser.js";

/**
 * Unit test del parser KML dei sentieri SAT (logica pura, niente DB/network).
 * Copre ogni ramo: forme document/folder/placemark (array vs singolo), skip per
 * dati mancanti, fallback `|| ''`, edge case coordinate, i due percorsi di
 * parseKML (file valido / errore) e la statica getUniqueDestinations.
 */

// KML completo: una Folder con 3 placemark — 1 valido, 1 senza ExtendedData
// (skippato), 1 senza LineString (skippato).
const KML_WITH_FOLDER = `<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <Folder>
      <Placemark>
        <ExtendedData><SchemaData>
          <SimpleData name="numero">E323</SimpleData>
          <SimpleData name="denominaz">Sentiero Test</SimpleData>
          <SimpleData name="loc_inizio">Malga A</SimpleData>
          <SimpleData name="quota_iniz">1200</SimpleData>
          <SimpleData name="loc_fine">Rifugio B</SimpleData>
          <SimpleData name="quota_fine">2000</SimpleData>
          <SimpleData name="difficolta">E</SimpleData>
          <SimpleData name="quota_min">1200</SimpleData>
          <SimpleData name="quota_max">2000</SimpleData>
          <SimpleData name="lun_planim">3500</SimpleData>
          <SimpleData name="lun_inclin">3800</SimpleData>
          <SimpleData name="t_andata">2:30</SimpleData>
          <SimpleData name="t_ritorno">1:45</SimpleData>
          <SimpleData name="competenza">SAT</SimpleData>
          <SimpleData name="gr_mont">Brenta</SimpleData>
          <SimpleData name="comuni_toc">Trento</SimpleData>
        </SchemaData></ExtendedData>
        <LineString><coordinates>11.10,46.00,1200 11.12,46.02,1600 11.15,46.05,2000</coordinates></LineString>
      </Placemark>
      <Placemark>
        <LineString><coordinates>11.1,46.0,1000</coordinates></LineString>
      </Placemark>
      <Placemark>
        <ExtendedData><SchemaData>
          <SimpleData name="numero">E999</SimpleData>
        </SchemaData></ExtendedData>
      </Placemark>
    </Folder>
  </Document>
</kml>`;

// KML senza <Document>: i placemark stanno direttamente sotto <kml> (copre il
// ramo `result.kml?.document || result.kml`) e placemark è singolo (non array).
const KML_NO_DOCUMENT = `<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Placemark>
    <ExtendedData><SchemaData>
      <SimpleData name="numero">E1</SimpleData>
      <SimpleData name="loc_fine">Cima X</SimpleData>
      <SimpleData name="quota_fine">2500</SimpleData>
    </SchemaData></ExtendedData>
    <LineString><coordinates>11.0,46.0,1000 11.1,46.1,2500</coordinates></LineString>
  </Placemark>
</kml>`;

let tmpDir;
let logSpy;
let warnSpy;
let errorSpy;

beforeAll(() => {
  tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "tsm-kml-"));
});

afterAll(() => {
  fs.rmSync(tmpDir, { recursive: true, force: true });
});

beforeEach(() => {
  // Il parser è volutamente verboso (log/warn/error): silenziamo per tenere
  // pulito l'output dei test, ma manteniamo le spie per asserire gli skip.
  logSpy = jest.spyOn(console, "log").mockImplementation(() => {});
  warnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});
  errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
});

afterEach(() => {
  logSpy.mockRestore();
  warnSpy.mockRestore();
  errorSpy.mockRestore();
});

function writeKml(name, content) {
  const p = path.join(tmpDir, name);
  fs.writeFileSync(p, content, "utf-8");
  return p;
}

describe("KMLParser.parseKML", () => {
  test("parses a folder-based KML and returns only valid trails", async () => {
    const parser = new KMLParser();
    const trails = await parser.parseKML(writeKml("folder.kml", KML_WITH_FOLDER));

    // 3 placemark, ma solo 1 valido (2 skippati per dati/coords mancanti).
    expect(trails).toHaveLength(1);
    const t = trails[0];
    expect(t.codice).toBe("E323");
    expect(t.denominazione).toBe("Sentiero Test");
    expect(t.puntoInizio).toEqual({
      nome: "Malga A",
      quota: 1200,
      coordinate: { lat: 46.0, lon: 11.1 },
    });
    expect(t.puntoFine).toEqual({
      nome: "Rifugio B",
      quota: 2000,
      coordinate: { lat: 46.05, lon: 11.15 },
    });
    expect(t.difficolta).toBe("E");
    expect(t.quotaMinima).toBe(1200);
    expect(t.quotaMassima).toBe(2000);
    expect(t.lunghezzaPlanimetrica).toBe(3500);
    expect(t.lunghezzaInclinata).toBe(3800);
    expect(t.competenza).toBe("SAT");
    expect(t.percorsoCoordinate).toBe(
      "11.10,46.00,1200 11.12,46.02,1600 11.15,46.05,2000",
    );
  });

  test("handles a KML without <Document> wrapper and a single placemark", async () => {
    const parser = new KMLParser();
    const trails = await parser.parseKML(writeKml("nodoc.kml", KML_NO_DOCUMENT));

    expect(trails).toHaveLength(1);
    expect(trails[0].codice).toBe("E1");
    expect(trails[0].puntoFine.nome).toBe("Cima X");
  });

  test("rejects when the file cannot be read", async () => {
    const parser = new KMLParser();
    await expect(
      parser.parseKML(path.join(tmpDir, "does-not-exist.kml")),
    ).rejects.toThrow();
    expect(errorSpy).toHaveBeenCalled();
  });
});

describe("KMLParser._extractPlacemarks", () => {
  const parser = new KMLParser();

  test("single placemark is wrapped into an array", () => {
    expect(parser._extractPlacemarks({ placemark: { id: 1 } })).toHaveLength(1);
  });

  test("array of placemarks is returned as-is", () => {
    expect(parser._extractPlacemarks({ placemark: [{ id: 1 }, { id: 2 }] })).toHaveLength(2);
  });

  test("collects placemarks across a single folder", () => {
    const doc = { folder: { placemark: [{ id: 1 }, { id: 2 }] } };
    expect(parser._extractPlacemarks(doc)).toHaveLength(2);
  });

  test("collects placemarks across multiple folders (and single placemark per folder)", () => {
    const doc = {
      folder: [
        { placemark: { id: 1 } },
        { placemark: [{ id: 2 }, { id: 3 }] },
        { name: "empty folder, no placemark" },
      ],
    };
    expect(parser._extractPlacemarks(doc)).toHaveLength(3);
  });

  test("returns empty array when neither placemark nor folder present", () => {
    expect(parser._extractPlacemarks({})).toEqual([]);
  });
});

describe("KMLParser._parsePlacemark", () => {
  const parser = new KMLParser();

  const fullData = {
    extendeddata: {
      schemadata: {
        simpledata: [
          { name: "numero", _: "E10" },
          { name: "loc_inizio", _: "Start" },
          { name: "quota_iniz", _: "1000" },
          { name: "loc_fine", _: "End" },
          { name: "quota_fine", _: "1500" },
        ],
      },
    },
    linestring: { coordinates: "11.0,46.0,1000 11.1,46.1,1500" },
  };

  test("returns null when ExtendedData is missing", () => {
    expect(parser._parsePlacemark({ linestring: { coordinates: "11,46,0" } })).toBeNull();
    expect(warnSpy).toHaveBeenCalled();
  });

  test("returns null when coordinates are missing", () => {
    expect(
      parser._parsePlacemark({
        extendeddata: { schemadata: { simpledata: { name: "numero", _: "E5" } } },
      }),
    ).toBeNull();
  });

  test("returns null when coordinates string yields no valid points", () => {
    expect(
      parser._parsePlacemark({
        extendeddata: { schemadata: { simpledata: { name: "numero", _: "E6" } } },
        linestring: { coordinates: "garbage" },
      }),
    ).toBeNull();
  });

  test("applies empty-string fallbacks for optional fields", () => {
    const trail = parser._parsePlacemark(fullData);
    expect(trail.codice).toBe("E10");
    // denominaz / competenza / gr_mont / comuni_toc assenti → fallback ''
    expect(trail.denominazione).toBe("");
    expect(trail.competenza).toBe("");
    expect(trail.gruppoMontano).toBe("");
    expect(trail.comuniToccati).toBe("");
  });

  test("ignores SimpleData entries without name or value", () => {
    const trail = parser._parsePlacemark({
      extendeddata: {
        schemadata: {
          simpledata: [
            { name: "numero", _: "E11" },
            { name: "no_value" }, // manca `_` → ignorato
            { _: "orphan" }, // manca `name` → ignorato
          ],
        },
      },
      linestring: { coordinates: "11.0,46.0,0 11.2,46.2,0" },
    });
    expect(trail.codice).toBe("E11");
  });

  test("returns null and logs on unexpected error (null placemark)", () => {
    expect(parser._parsePlacemark(null)).toBeNull();
    expect(errorSpy).toHaveBeenCalled();
  });
});

describe("KMLParser._parseCoordinates", () => {
  const parser = new KMLParser();

  test("parses valid lon,lat[,alt] pairs and skips malformed ones", () => {
    const coords = parser._parseCoordinates(
      "  11.0,46.0,1000   11.5,46.5   onlyone   notanum,xyz  ",
    );
    // "11.0,46.0,1000" ok; "11.5,46.5" ok; "onlyone" (<2 parti) skip;
    // "notanum,xyz" (NaN) skip.
    expect(coords).toEqual([
      { lat: 46.0, lon: 11.0 },
      { lat: 46.5, lon: 11.5 },
    ]);
  });

  test("returns empty array for an empty string", () => {
    expect(parser._parseCoordinates("   ")).toEqual([]);
  });
});

describe("KMLParser.getUniqueDestinations", () => {
  test("groups trails by destination name and lists their codes", () => {
    const trails = [
      { codice: "A1", puntoFine: { nome: "Rifugio X", coordinate: { lat: 1, lon: 2 }, quota: 2000 } },
      { codice: "A2", puntoFine: { nome: "Rifugio X", coordinate: { lat: 1, lon: 2 }, quota: 2000 } },
      { codice: "B1", puntoFine: { nome: "Cima Y", coordinate: { lat: 3, lon: 4 }, quota: 2500 } },
    ];
    const dests = KMLParser.getUniqueDestinations(trails);

    expect(dests).toHaveLength(2);
    const rifugio = dests.find((d) => d.nome === "Rifugio X");
    expect(rifugio.sentieri).toEqual(["A1", "A2"]);
    expect(rifugio.quota).toBe(2000);
    const cima = dests.find((d) => d.nome === "Cima Y");
    expect(cima.sentieri).toEqual(["B1"]);
  });
});
