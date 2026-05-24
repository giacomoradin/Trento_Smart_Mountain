/**
 * weatherRouter.js
 *
 * Monta le route:
 *   GET /weather/locations/search?q=Bolzano&type=town
 *   GET /weather/locations/nearby?lon=11.35&lat=46.50&maxDistance=30000
 *   GET /weather/forecast/:externalId
 *   POST /weather/forecast/:externalId/refresh   (admin / cron)
 *   POST /weather/seed                           (admin / primo avvio)
 */

import { Router } from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import {
  getLocationForecast,
  refreshLocationForecast,
  findLocalVenuesByName,
  findNearbyVenues,
  seedLocations,
} from "../services/weatherService.js";

const router = Router();

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Wrapper async per evitare try/catch ripetuti in ogni handler */
const asyncHandler = (fn) => (req, res, next) =>
  Promise.resolve(fn(req, res, next)).catch(next);

// ─── Routes ───────────────────────────────────────────────────────────────────
router.get('/test', (req, res) => res.json({ ok: true }));
/**
 * GET /weather/locations/search
 * Query: q (required), type? ('town'|'poi'), limit? (default 10)
 *
 * Esempio: GET /weather/locations/search?q=Merano&type=town
 */
router.get(
  "/locations/search",
  authenticate,
  asyncHandler(async (req, res) => {
    const { q, type, limit } = req.query;

    if (!q || q.trim().length < 2) {
      return res.status(400).json({
        error: "Il parametro 'q' deve avere almeno 2 caratteri",
      });
    }

    
    const results = await findLocalVenuesByName(q.trim(), {
      type,
      limit: limit ? parseInt(limit, 10) : 10,
    });

    res.json({ count: results.length, results });
  })
);

/**
 * GET /weather/locations/nearby
 * Query: lon (required), lat (required), maxDistance? (metri, default 50000), type?, limit?
 *
 * Esempio: GET /weather/locations/nearby?lon=11.35&lat=46.50&maxDistance=20000&type=town
 */
router.get(
  "/locations/nearby",
  authenticate,
  asyncHandler(async (req, res) => {
    const { lon, lat, maxDistance, type, limit } = req.query;

    if (!lon || !lat) {
      return res.status(400).json({ error: "Parametri 'lon' e 'lat' obbligatori" });
    }

    // Now correctly passes arguments to the destructured service
    const results = await findNearbyVenues(
      parseFloat(lon),
      parseFloat(lat),
      {
        maxDistance: maxDistance ? parseInt(maxDistance, 10) : 50_000,
        type,
        limit: limit ? parseInt(limit, 10) : 5,
      }
    );

    res.json({ count: results.length, results });
  })
);

/**
 * GET /weather/forecast/:externalId
 * Query: forceRefresh? (boolean, default false)
 *
 * Restituisce il forecast per town o POI.
 * Se è un POI, risolve automaticamente la town di riferimento.
 *
 * Esempio: GET /weather/forecast/5d9e12bb-7274-483e-9acd-44bfdcb916e5
 */
router.get(
  "/forecast/:externalId",
  authenticate,
  asyncHandler(async (req, res) => {
    const { externalId } = req.params;
    const forceRefresh = req.query.forceRefresh === "true";

    const result = await getLocationForecast(externalId, { forceRefresh });

    res.json({
      location: {
        externalId: result.location.externalId,
        type:       result.location.type,
        name:       result.location.name,
        elevation:  result.location.elevation,
        coordinates: result.location.location?.coordinates,
      },
      // Se la location è un POI, espone anche la town di riferimento
      ...(result.location.type === "poi" && {
        referenceTown: {
          externalId: result.town.externalId,
          name:       result.town.name,
        },
      }),
      meta: {
        ...result.meta,
        fromCache: result.fromCache,
      },
      // Previsioni 3h: prossime 48h (le prime 16 slot)
      forecast3h:  result.slots3h.slice(0, 16),
      // Previsioni 24h: 7 giorni
      forecast24h: result.slots24h.slice(0, 7),
    });
  })
);

/**
 * POST /weather/forecast/:externalId/refresh
 * Forza il refresh del forecast ignorando la cache.
 * Solo per towns (i POI non hanno forecast propri).
 *
 * Utile per endpoint admin o job cron.
 */
router.post(
  "/forecast/:externalId/refresh",
  asyncHandler(async (req, res) => {
    const { externalId } = req.params;

    const updated = await refreshLocationForecast(externalId);

    res.json({
      message:   `Forecast aggiornato per ${updated.name}`,
      fetchedAt: updated.forecasts.fetchedAt,
      slotsCount: {
        "3h":  updated.forecasts.slots3h?.length  ?? 0,
        "24h": updated.forecasts.slots24h?.length ?? 0,
      },
    });
  })
);

/**
 * POST /weather/seed
 * Popola il DB con tutte le towns e i POI dall'API.
 * Da chiamare una volta allo startup o tramite script di init.
 *
 * Da porteggere questa route con middleware di autenticazione admin.
 */
router.post(
  "/seed",
  asyncHandler(async (req, res) => {
    const result = await seedLocations();
    res.json({
      message: "Seed completato",
      ...result,
    });
  })
);

// ─── Error handler locale ─────────────────────────────────────────────────────

router.use((err, req, res, _next) => {
  console.error(`[weatherRouter] ${req.method} ${req.path} →`, err.message);

  const status = err.message.includes("non trovata") ? 404 : 500;
  res.status(status).json({ error: err.message });
});

export default router;