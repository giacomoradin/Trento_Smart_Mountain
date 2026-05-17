import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { requireRoles } from "../middleware/authorizationMiddleware.js";
import {
  createUser,
  getAllUsers,
  getUserById,
  updateUser,
  deleteUser,
} from "../services/userService.js";

const router = express.Router();

// Public Endpoint
router.post("/", createUser); // Registrazione (Sign up)

// Protected Endpoints (Richiedono JWT)
router.get("/", authenticate, getAllUsers); // Lettura di tutti gli utenti
router.get("/:id", authenticate, getUserById); // Lettura utente specifico per ID
router.put("/:id", authenticate, requireRoles("admin"), updateUser); // Aggiornamento payload utente
router.delete("/:id", authenticate, requireRoles("admin"), deleteUser); // Rimozione utente

export default router;
