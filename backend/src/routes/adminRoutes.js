import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { requireRoles } from "../middleware/authorizationMiddleware.js";
import {
  createAdmin,
  listAllUsers,
  getAnyUserById,
  updateAnyUser,
  deleteAnyUser,
} from "../services/adminService.js";

const router = express.Router();

/**
 * Route per amministratori.
 *
 *   POST   /admin/users          → crea un nuovo admin (admin only)
 *   GET    /admin/users          → lista tutti gli utenti
 *   GET    /admin/users/:id      → dettaglio qualsiasi utente
 *   PUT    /admin/users/:id      → aggiorna qualsiasi utente (incluso role)
 *   DELETE /admin/users/:id      → elimina qualsiasi utente
 *
 * Tutte le route sono protette da JWT + role=admin.
 */

router.use(authenticate);
router.use(requireRoles("admin"));

router.post("/users", createAdmin);
router.get("/users", listAllUsers);
router.get("/users/:id", getAnyUserById);
router.put("/users/:id", updateAnyUser);
router.delete("/users/:id", deleteAnyUser);

export default router;
