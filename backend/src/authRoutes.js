import express from "express";
import { loginUser } from "./authService.js";

const router = express.Router();

router.post("/login", loginUser);

export default router;
