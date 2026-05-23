import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import {
  createRefuge,
  getRefugeById,
  listRefuges,
  updateRefuge,
} from "../services/refugeService.js";

const router = express.Router();

/**
 * Route per gli utenti **rifugio**.
 *
 *   POST   /refuges          → registrazione nuovo rifugio (pubblico)
 *   GET    /refuges          → lista pubblica rifugi (no auth)
 *   GET    /refuges/:id      → dettaglio rifugio (JWT)
 *   PUT    /refuges/:id      → aggiornamento metadati struttura (JWT)
 *
 * La registrazione è anche accessibile via POST /auth/register/refuge.
 */

router.post("/", createRefuge);
router.get("/", listRefuges); // pubblico: future ricerche
router.get("/:id", authenticate, getRefugeById);
router.put("/:id", authenticate, updateRefuge);

export default router;
