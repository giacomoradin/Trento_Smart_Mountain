import { XMLParser } from 'fast-xml-parser';
import Station from '../models/station.js'; 

const STATIONS_URL = 'https://dati.meteotrentino.it/service.asmx/getListOfMeteoStations';
const xmlParser = new XMLParser({ ignoreAttributes: false, trimValues: true });

let _cache = null;
let _cacheAt = null;
const CACHE_TTL_MS = 10 * 60 * 1000;

// Recupero dati grezzi da MeteoTrentino
async function fetchAllStations() {
  if (_cache && _cacheAt && Date.now() - _cacheAt < CACHE_TTL_MS) return _cache;
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

/* --- METODI REMOTE (API ESTERNA) --- */

export async function findRemoteStationsByName(query) {
  const stations = await fetchAllStations();
  const q = query.toLowerCase().trim();
  return stations.filter(s => 
    String(s.name || '').toLowerCase().includes(q) || 
    String(s.shortname || '').toLowerCase().includes(q)
  );
}

export async function findRemoteStationByCode(code) {
  const stations = await fetchAllStations();
  return stations.find(s => String(s.code).toUpperCase() === String(code).toUpperCase()) || null;
}

/* --- METODI LOCAL (MONGODB) --- */

export async function findLocalStationsByName(query) {
  return await Station.find({ name: { $regex: query, $options: 'i' } });
}

export async function findLocalStationByCode(code) {
  return await Station.findOne({ code: String(code).toUpperCase() });
}

export async function saveStationToDb(data) {
  // Crea e salva nel DB locale
  const station = new Station(data);
  return await station.save();
}

export async function deleteStationFromDb(id) {
  // Rimozione fisica dal database
  return await Station.findByIdAndDelete(id);
}

export { fetchAllStations };