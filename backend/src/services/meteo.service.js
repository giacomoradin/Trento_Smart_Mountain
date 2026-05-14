import { XMLParser } from 'fast-xml-parser';
import  Station from '../models/station.js'; 
import  {findRemoteStationByCode}  from './stationRegistry.service.js'; 

const xmlParser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: '',
  trimValues: true,
});

function normalizeAirTemperature(raw) {
  if (!raw) return [];
  const list = Array.isArray(raw) ? raw : [raw];
  return list
    .map((item) => ({
      UM:    item.UM != null    ? String(item.UM)    : '°C',
      date:  item.date != null  ? String(item.date)  : '',
      value: item.value != null ? Number(item.value) : NaN,
    }))
    .filter((row) => row.date && !Number.isNaN(row.value));
}

function temperatureListFromParsed(parsed) {
  if (!parsed || typeof parsed !== 'object') return {};
  if (parsed.temperature_list) return parsed.temperature_list;
  const fromLastData = parsed.lastData?.temperature_list;
  if (fromLastData) return fromLastData;
  for (const key of Object.keys(parsed)) {
    if (key === '?xml') continue;
    const block = parsed[key];
    if (block && typeof block === 'object' && block.temperature_list) return block.temperature_list;
  }
  return {};
}

async function fetchMeteoAndPersist(codice) {
  // 1. Metadati stazione
  const stationInfo = await findRemoteStationByCode(codice);
  if (!stationInfo) {
    throw Object.assign(new Error(`Stazione ${codice} non trovata nel registry`), { statusCode: 404 });
  }

  // 2. Temperatura
  const base = process.env.METEO_SERVICE_URL || 'http://dati.meteotrentino.it/service.asmx/getLastDataOfMeteoStation';
  const url = `${base}?codice=${encodeURIComponent(codice)}`;

  const response = await fetch(url);
  if (!response.ok)
    throw Object.assign(new Error(`Upstream HTTP ${response.status}`), { statusCode: 502 });

  const xml = await response.text();
  const parsed = xmlParser.parse(xml);
  const root = temperatureListFromParsed(parsed);
  const allTemperatures = normalizeAirTemperature(root.air_temperature);
  const lastMeasure = allTemperatures.slice(-1);

  if (lastMeasure.length === 0)
    throw Object.assign(new Error("Nessun dato trovato nell'XML"), { statusCode: 404 });

  // 3. Salva tutto insieme
  const doc = await Station.findOneAndUpdate(
    { stationCode: String(codice) },
    {
      stationCode:     String(codice),
      stationInfo,              // ← metadati completi
      sourceUrl:       url,
      fetchedAt:       new Date(),
      air_temperature: lastMeasure,
    },
    { upsert: true, new: true, runValidators: true }
  );

  // 4. Console log arricchito
  const last = lastMeasure[0];
  console.log('==========================================');
  console.log(`📡 STAZIONE:   ${stationInfo.name} (${doc.stationCode})`);
  console.log(`📍 POSIZIONE:  lat ${stationInfo.latitude}, lon ${stationInfo.longitude}`);
  console.log(`⛰️  QUOTA:      ${stationInfo.elevation} m`);
  console.log(`🌡️  TEMP:       ${last.value}${last.UM}`);
  console.log(`📅 DATA:       ${last.date}`);
  console.log(`🗂️  ATTIVA DAL: ${stationInfo.startdate}${stationInfo.enddate ? ` → ${stationInfo.enddate}` : ''}`);
  console.log('==========================================');

  return { doc, count: lastMeasure.length };
}

export { fetchMeteoAndPersist };