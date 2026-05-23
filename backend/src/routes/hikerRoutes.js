import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { createHiker, getHikerById, updateHiker } from "../services/hikerService.js";

const router = express.Router();

/**
 * Route per gli utenti **escursionisti**.
 *
 *   POST   /hikers          → registrazione nuovo escursionista (pubblico)
 *   GET    /hikers/:id      → profilo escursionista (JWT)
 *   PUT    /hikers/:id      → aggiornamento profilo (JWT)
 *
 * La registrazione è anche accessibile via POST /auth/register/hiker
 * (alias semantico nel router auth).
 */

router.post("/", createHiker);
router.get("/:id", authenticate, getHikerById);
router.put("/:id", authenticate, updateHiker);

export default router;
