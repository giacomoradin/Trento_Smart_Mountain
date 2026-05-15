// @ts-nocheck
import Location from '../models/location.js';

const BASE_URL     = 'https://meteo.report/var/data';
const TOWNS_URL = 'https://gitlab.com/tinia-euregio/tinia-website/-/raw/main/data/venues/it/2.json';
const POI_URL   = 'https://gitlab.com/tinia-euregio/tinia-website/-/raw/main/data/venues/it/8.json';
const FORECAST_URL = (id) => `${BASE_URL}/forecasts/${id}.json`;

const CACHE_TTL_MS      = 60 * 60 * 1000; // 1 ora
const MAX_FORECAST_DAYS = 7;

/* ─── Cache in-memory per venues (towns + POI) ───────────────────────────── */
// I venues cambiano raramente: li teniamo in RAM per evitare fetch ripetuti.
// I forecast invece vengono salvati su MongoDB (sono pesanti e per-location).

let _venuesCache    = null;
let _venuesCacheAt  = null;
const VENUES_TTL_MS = 24 * 60 * 60 * 1000; // 24 ore

/* ─── Helpers interni ────────────────────────────────────────────────────── */

/**
 * Calcola validFrom e validTo di uno slot dalla time-layout key.
 *
 * La key è composta da:
 *   - 4 cifre di prefisso famiglia ("1800" = slot 3h, "1440" = slot 24h)
 *   - cifre rimanenti = offset in minuti dalla `start` del JSON
 *
 * Esempio "18000180": prefisso "1800", offset 180min → validFrom = start + 3h
 */
function resolveSlotDates(key, startDate, intervalMinutes) {
  const offsetMinutes = parseInt(key.slice(4), 10);
  const validFrom = new Date(startDate.getTime() + offsetMinutes * 60_000);
  const validTo   = new Date(validFrom.getTime()  + intervalMinutes * 60_000);
  return { validFrom, validTo };
}

function parseSlot(key, raw, startDate, intervalMinutes) {
  const { validFrom, validTo } = resolveSlotDates(key, startDate, intervalMinutes);
  return {
    timeLayoutKey:    key,
    intervalMinutes,
    validFrom,
    validTo,
    temperature:      raw.temperature       ?? null,
    rainFall:         raw.rain_fall         ?? null,
    rainProbability:  raw.rain_probability  ?? null,
    freshSnow:        raw.fresh_snow        ?? null,
    snowLevel:        raw.snow_level        ?? null,
    windSpeed:        raw.wind_speed        ?? null,
    windGust:         raw.wind_gust         ?? null,
    windDirection:    raw.wind_direction    ?? null,
    freezingLevel:    raw.freezing_level    ?? null,
    skyCondition:     raw.sky_condition     ?? null,
    sunshineDuration: raw.sunshine_duration ?? null,
  };
}

function isCacheValid(forecasts) {
  if (!forecasts?.fetchedAt) return false;
  const ageMs = Date.now() - new Date(forecasts.fetchedAt).getTime();
  if (ageMs > CACHE_TTL_MS) return false;
  if (forecasts.validTo && new Date(forecasts.validTo) < new Date()) return false;
  return true;
}

/* ─── Fetch API esterne ──────────────────────────────────────────────────── */

async function fetchVenues() {
  if (_venuesCache && _venuesCacheAt && Date.now() - _venuesCacheAt < VENUES_TTL_MS) {
    return _venuesCache;
  }

  const [townsRes, poiRes] = await Promise.all([
    fetch(TOWNS_URL, { signal: AbortSignal.timeout(15_000) }),
    fetch(POI_URL,   { signal: AbortSignal.timeout(15_000) }),
  ]);

  if (!townsRes.ok) throw Object.assign(new Error(`Upstream HTTP ${townsRes.status} (towns)`), { statusCode: 502 });
  if (!poiRes.ok)   throw Object.assign(new Error(`Upstream HTTP ${poiRes.status} (poi)`),   { statusCode: 502 });

  const towns = await townsRes.json();
  const pois  = await poiRes.json();

  _venuesCache   = { towns, pois };
  _venuesCacheAt = Date.now();
  return _venuesCache;
}

async function fetchForecastFromApi(externalId) {
  const res = await fetch(FORECAST_URL(externalId), { signal: AbortSignal.timeout(10_000) });
  if (!res.ok) throw Object.assign(new Error(`Upstream HTTP ${res.status} (forecast ${externalId})`), { statusCode: 502 });

  const data      = await res.json();
  const startDate = new Date(data.start);
  const endDate   = new Date(data.end);
  const cutoff    = new Date(startDate.getTime() + MAX_FORECAST_DAYS * 86_400_000);

  const slots3h = Object.entries(data['180']  ?? {})
    .map(([key, raw]) => parseSlot(key, raw, startDate, 180))
    .filter(s => s.validFrom < cutoff)
    .sort((a, b) => a.validFrom - b.validFrom);

  const slots24h = Object.entries(data['1440'] ?? {})
    .map(([key, raw]) => parseSlot(key, raw, startDate, 1440))
    .filter(s => s.validFrom < cutoff)
    .sort((a, b) => a.validFrom - b.validFrom);

  return {
    fetchedAt: new Date(),
    validFrom: startDate,
    validTo:   endDate < cutoff ? endDate : cutoff,
    slots3h,
    slots24h,
  };
}

/* ─── METODI REMOTE (API ESTERNA) ────────────────────────────────────────── */

export async function findRemoteVenuesByName(query) {
  const { towns, pois } = await fetchVenues();
  const q = query.toLowerCase().trim();
  const match = (item) => String(item.name || '').toLowerCase().includes(q);
  return {
    towns: towns.filter(match),
    pois:  pois.filter(match),
  };
}

export async function findRemoteVenueById(externalId) {
  const { towns, pois } = await fetchVenues();
  return (
    towns.find(t => t.id === externalId) ||
    pois.find(p  => p.id === externalId) ||
    null
  );
}

export { fetchVenues };



/* ─── METODI LOCAL (MONGODB) ─────────────────────────────────────────────── */

// UPDATED: Now accepts an options object for type and limit
export async function findLocalVenuesByName(query, { type, limit = 10 } = {}) {
  const filter = { name: { $regex: query, $options: 'i' } };
  if (type) filter.type = type;

  return Location.find(filter)
    .select('externalId type name elevation location regionId')
    .limit(limit)
    .lean();
}


// UPDATED: Now accepts destructuring to prevent the "Cast to Number" error
export async function findNearbyVenues(lon, lat, { maxDistance = 50_000, type = null, limit = 10 } = {}) {
  const filter = {
    location: {
      $near: {
        $geometry:    { type: 'Point', coordinates: [lon, lat] },
        $maxDistance: maxDistance,
      },
    },
    ...(type ? { type } : {}),
  };
  return Location.find(filter)
    .select('externalId type name elevation location regionId')
    .limit(limit)
    .lean();
}

export async function findLocalVenueById(externalId) {
  return Location.findOne({ externalId }).lean();
}



/**
 * Popola (o aggiorna) il DB con tutti i venues dall'API.
 * Idempotente grazie all'upsert — sicuro da chiamare più volte.
 */
export async function seedLocations() {
  const { towns, pois } = await fetchVenues();

  const toOp = (item, type) => ({
    updateOne: {
      filter: { externalId: item.id },
      update: {
        $set: {
          externalId: item.id,
          type,
          name:      item.name,
          elevation: item.elevation ?? null,
          location: {
            type:        'Point',
            coordinates: [item.lon, item.lat],
          },
          regionId: item.id_region,
        },
        $setOnInsert: { forecasts: null },
      },
      upsert: true,
    },
  });

  const ops = [
    ...towns.map(t => toOp(t, 'town')),
    ...pois.map(p  => toOp(p, 'poi')),
  ];

  if (!ops.length) {
    console.warn('[weatherService] seedLocations: nessun dato ricevuto');
    return { towns: 0, pois: 0 };
  }

  await Location.bulkWrite(ops, { ordered: false });
  console.log(`[weatherService] Seed: ${towns.length} towns, ${pois.length} POI`);
  return { towns: towns.length, pois: pois.length };
}

/**
 * Forza il refresh del forecast per una town specifica.
 * Da non chiamare su POI: non hanno forecast propri.
 */
export async function refreshLocationForecast(externalId) {
  const location = await Location.findOne({ externalId });
  if (!location) return null;
  
  if (location.type !== 'town') {
    throw Object.assign(new Error(`${externalId} is a POI`), { statusCode: 400 });
  }

  // Fetch from external API
  const forecastData = await fetchForecastFromApi(externalId);
  
  // Update the document
  location.forecasts = forecastData;
  
  // Save to MongoDB
  const savedLocation = await location.save(); 
  
  console.log(`[weatherService] Forecast saved to DB: ${location.name}`);
  return savedLocation; // Return the saved version!
}

/**
 * Restituisce il forecast per qualsiasi location (town o POI).
 * Se il dato è stale o assente, fetcha e aggiorna automaticamente.
 *
 * @returns {{ location, town, slots3h, slots24h, fromCache, meta }}
 */
export async function getLocationForecast(externalId, { forceRefresh = false } = {}) {
  let location = await Location.findOne({ externalId });
  if (!location) return null;

  let town;
  if (location.type === 'town') {
    town = location;
  } else {
    town = await Location.findOne({ externalId: location.regionId });
    if (!town) throw new Error(`Reference town not found for POI ${externalId}`);
  }

  let fromCache = true;
  // If stale or forced, refresh and catch the returned object
  if (forceRefresh || !isCacheValid(town.forecasts)) {
    town = await refreshLocationForecast(town.externalId); 
    fromCache = false;
  }

  const now = new Date();
  const futureSlots3h  = (town.forecasts?.slots3h  ?? []).filter(s => new Date(s.validTo) > now);
  const futureSlots24h = (town.forecasts?.slots24h ?? []).filter(s => new Date(s.validTo) > now);

  return {
    location,
    town,
    slots3h:  futureSlots3h,
    slots24h: futureSlots24h,
    fromCache,
    meta: {
      fetchedAt: town.forecasts?.fetchedAt,
      validFrom: town.forecasts?.validFrom,
      validTo:   town.forecasts?.validTo,
    },
  };
}