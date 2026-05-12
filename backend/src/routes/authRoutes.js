import express from "express";
import { loginUser } from "../services/authService.js";
import { verifyEmail } from "../services/authService.js";

const router = express.Router();

router.post("/login", loginUser);
router.get("/verify/:token", verifyEmail);

export default router;
