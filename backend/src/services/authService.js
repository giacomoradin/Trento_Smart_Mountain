import User from "../models/user.js";
import bcrypt from "bcrypt";
import jwt from "jsonwebtoken";
import crypto from "crypto";
import { sendPasswordResetEmail } from "./emailService.js";

export const verifyEmail = async (req, res) => {
  try {
    const { token } = req.params;
    const user = await User.findOne({ verificationToken: token });
    if (!user) {
      return res.redirect("tsm://auth/error?message=token_invalido_o_scaduto");
    }
    user.isVerified = true;
    user.verificationToken = undefined;
    await user.save();
    const jwtToken = jwt.sign(
      { userId: user._id, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || "1d" },
    );
    res.redirect(`tsm://auth/success?jwt=${jwtToken}`);
  } catch (error) {
    console.error("Errore fatale in verifyEmail:", error);
    res.redirect("tsm://auth/error?message=errore_server_interno");
  }
};

export const loginUser = async (req, res) => {
  try {
    const { email, password } = req.body;
    const user = await User.findOne({ email });
    if (!user) return res.status(401).json({ message: "Credenziali non valide." });

    const isPasswordValid = await bcrypt.compare(password, user.passwordHash);
    if (!isPasswordValid) return res.status(401).json({ message: "Credenziali non valide." });

    if (!user.isVerified) {
      return res.status(403).json({
        message: "Accesso negato. Eseguire la verifica SMTP inviata via email.",
      });
    }
    const token = jwt.sign(
      { userId: user._id, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || "1d" },
    );
    res.status(200).json({ token });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

export const forgotPassword = async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) return res.status(400).json({ message: "Email obbligatoria." });

    const user = await User.findOne({ email });
    if (user) {
      const token = crypto.randomBytes(32).toString("hex");
      user.passwordResetToken = token;
      user.passwordResetExpires = new Date(Date.now() + 60 * 60 * 1000);
      await user.save();
      try {
        await sendPasswordResetEmail(email, token);
      } catch (err) {
        console.error("Errore invio email reset:", err.message);
      }
    }
    // Risposta generica per evitare user enumeration
    res.status(200).json({ message: "Se l'indirizzo è registrato, riceverai un link per il reset." });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

export const getResetPasswordForm = (req, res) => {
  const { token } = req.params;
  res.send(`<!DOCTYPE html>
<html><head><title>Reset Password - TSM</title>
<style>
  body{font-family:sans-serif;background:#121212;color:#fff;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0}
  .card{background:#1e1e1e;padding:32px;border-radius:12px;width:100%;max-width:360px}
  h2{color:#4FC3F7;margin-top:0}label{display:block;font-size:12px;color:#aaa;margin-bottom:4px}
  input{width:100%;padding:12px;margin-bottom:16px;border:1px solid #3a3a3a;background:#2a2a2a;color:#fff;border-radius:6px;box-sizing:border-box}
  button{width:100%;padding:14px;background:#2E5A27;color:white;border:none;border-radius:6px;font-size:15px;cursor:pointer;font-weight:700}
  .error{color:#cf6679;font-size:13px;margin-bottom:12px}
</style></head>
<body><div class="card">
  <h2>Trento Smart Mountain</h2>
  <h3 style="margin-top:0">Nuova Password</h3>
  <form method="POST" action="/auth/reset-password/${token}">
    <label>Nuova password (min. 8 caratteri)</label>
    <input type="password" name="password" required minlength="8" placeholder="••••••••"/>
    <label>Conferma password</label>
    <input type="password" name="confirmPassword" required minlength="8" placeholder="••••••••"/>
    <button type="submit">Salva nuova password</button>
  </form>
</div></body></html>`);
};

export const resetPassword = async (req, res) => {
  const isJson = req.is('application/json');
  try {
    const { token } = req.params;
    const password = req.body.password;
    const confirmPassword = req.body.confirmPassword;

    if (!isJson && password !== confirmPassword) {
      return res.status(400).send('<p style="color:red">Le password non corrispondono.</p>');
    }
    if (!password || password.length < 8) {
      if (isJson) return res.status(400).json({ message: "Password di almeno 8 caratteri." });
      return res.status(400).send('<p style="color:red">Password di almeno 8 caratteri richiesta.</p>');
    }

    const user = await User.findOne({
      passwordResetToken: token,
      passwordResetExpires: { $gt: new Date() },
    });
    if (!user) {
      if (isJson) return res.status(400).json({ message: "Token non valido o scaduto." });
      return res.status(400).send('<p style="color:red">Token non valido o scaduto. Richiedi un nuovo link.</p>');
    }

    user.passwordHash = await bcrypt.hash(password, 10);
    user.passwordResetToken = undefined;
    user.passwordResetExpires = undefined;
    await user.save();

    if (isJson) return res.status(200).json({ message: "Password aggiornata con successo." });
    res.send(`<html><body style="font-family:sans-serif;background:#121212;color:#fff;text-align:center;padding:60px">
      <h2 style="color:#2E5A27">✓ Password aggiornata!</h2>
      <p>Torna all'app Trento Smart Mountain per accedere.</p>
    </body></html>`);
  } catch (error) {
    if (isJson) return res.status(500).json({ message: error.message });
    res.status(500).send('<p style="color:red">Errore server. Riprova.</p>');
  }
};
