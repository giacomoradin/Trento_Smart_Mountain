import express from "express";
import { 
  getAllSentieri, 
  getSentieroByCode, 
  getAllDestinazioni, 
  getSentieriByDestination,
  getStats 
} from "../services/sentieroService.js";

const router = express.Router();

/**
 * Route per i sentieri SAT (mountain pathways).
 *
 *   GET    /api/v1/sentieri                      → lista sentieri (con filtri)
 *   GET    /api/v1/sentieri/stats                → statistiche
 *   GET    /api/v1/sentieri/:codice              → dettaglio sentiero
 *   GET    /api/v1/sentieri/destinazioni         → tutte le destinazioni
 *   GET    /api/v1/sentieri/destinazioni/:nome/sentieri → sentieri per destinazione
 *
 * IMPORTANTE: /stats DEVE venire prima di /:codice altrimenti "stats" viene
 * interpretato come un codice sentiero.
 */

router.get("/", getAllSentieri);
router.get("/stats", getStats);
router.get("/:codice", getSentieroByCode);
router.get("/destinazioni", getAllDestinazioni);
router.get("/destinazioni/:nome/sentieri", getSentieriByDestination);

export default router;