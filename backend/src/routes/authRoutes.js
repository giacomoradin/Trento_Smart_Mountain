import express from "express";
import {
  loginUser,
  verifyEmail,
  forgotPassword,
  getResetPasswordForm,
  resetPassword,
} from "../services/authService.js";

const router = express.Router();

router.post("/login", loginUser);
router.get("/verify/:token", verifyEmail);
router.post("/forgot-password", forgotPassword);
router.get("/reset-password/:token", getResetPasswordForm);
router.post("/reset-password/:token", resetPassword);

export default router;
