import { downsamplePolyline } from "../../src/utils/geoPolyline.js";

/**
 * Unit test della utility di campionamento polyline usata dalla route
 * signature del feed (socialService) e dalla persistenza attività.
 */
describe("downsamplePolyline", () => {
  test("returns undefined for non-array or < 2 points", () => {
    expect(downsamplePolyline(null)).toBeUndefined();
    expect(downsamplePolyline(undefined)).toBeUndefined();
    expect(downsamplePolyline([])).toBeUndefined();
    expect(downsamplePolyline([{ lat: 1, lon: 2 }])).toBeUndefined();
  });

  test("returns input unchanged when already within maxPoints", () => {
    const pts = [
      { lat: 46.0, lon: 11.0 },
      { lat: 46.1, lon: 11.1 },
      { lat: 46.2, lon: 11.2 },
    ];
    expect(downsamplePolyline(pts, 60)).toEqual(pts);
  });

  test("downsamples a long track to exactly maxPoints", () => {
    // 1000 punti su una diagonale → 48 campioni.
    const pts = Array.from({ length: 1000 }, (_, i) => ({
      lat: 46.0 + i * 0.001,
      lon: 11.0 + i * 0.001,
    }));
    const out = downsamplePolyline(pts, 48);
    expect(out).toHaveLength(48);
    // Primo e ultimo punto sempre preservati (start/end marker corretti).
    expect(out[0]).toEqual({ lat: 46.0, lon: 11.0 });
    expect(out[47]).toEqual(pts[999]);
  });

  test("strips extra subdocument fields (only lat/lon kept)", () => {
    const pts = [
      { lat: 46.0, lon: 11.0, _id: "abc", updatedAt: "x" },
      { lat: 46.1, lon: 11.1, _id: "def" },
    ];
    const out = downsamplePolyline(pts, 60);
    expect(out).toEqual([
      { lat: 46.0, lon: 11.0 },
      { lat: 46.1, lon: 11.1 },
    ]);
  });

  test("filters out points with non-numeric coords", () => {
    const pts = [
      { lat: 46.0, lon: 11.0 },
      { lat: "bad", lon: 11.1 },
      { lat: 46.2, lon: 11.2 },
    ];
    const out = downsamplePolyline(pts, 60);
    expect(out).toHaveLength(2);
    expect(out).toEqual([
      { lat: 46.0, lon: 11.0 },
      { lat: 46.2, lon: 11.2 },
    ]);
  });
});
