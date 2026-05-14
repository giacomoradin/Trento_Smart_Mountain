import { XMLParser } from 'fast-xml-parser';

const STATIONS_URL = 'https://dati.meteotrentino.it/service.asmx/getListOfMeteoStations';

const xmlParser = new XMLParser({
  ignoreAttributes: false,
  trimValues: true,
});

let _cache = null;
let _cacheAt = null;
const CACHE_TTL_MS = 10 * 60 * 1000; // 10 minuti

async function fetchAllStations() {
  if (_cache && _cacheAt && Date.now() - _cacheAt < CACHE_TTL_MS) {
    return _cache;
  }
  const res = await fetch(STATIONS_URL);
  if (!res.ok) throw Object.assign(new Error(`Upstream HTTP ${res.status}`), { statusCode: 502 });

  const xml = await res.text();
  const parsed = xmlParser.parse(xml);
  const root = parsed?.ArrayOfPointOfMeasureInfo?.pointOfMeasureInfo;
  const list = Array.isArray(root) ? root : root ? [root] : [];

  _cache = list;
  _cacheAt = Date.now();
  return list;
}

/**
 * Cerca stazioni il cui nome (o shortname) contiene la stringa cercata (case-insensitive).
 */
async function findStationsByName(query) {
  const stations = await fetchAllStations();
  const q = query.toLowerCase().trim();
  return stations.filter(
    (s) =>
      String(s.name || '').toLowerCase().includes(q) ||
      String(s.shortname || '').toLowerCase().includes(q)
  );
}

/**
 * Restituisce la prima stazione che corrisponde esattamente al codice.
 */
async function findStationByCode(code) {
  const stations = await fetchAllStations();
  return stations.find((s) => String(s.code).toUpperCase() === String(code).toUpperCase()) || null;
}

export { fetchAllStations, findStationsByName, findStationByCode };