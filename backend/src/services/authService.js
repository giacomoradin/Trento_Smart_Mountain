import User from "../models/user.js";
import bcrypt from "bcrypt";
import jwt from "jsonwebtoken";

export const verifyEmail = async (req, res) => {
  /* 
     #swagger.tags = ['Auth']
     #swagger.description = 'Verifica l'indirizzo email dell'utente tramite il token ricevuto via SMTP.'
     #swagger.security = [] 
  */
  try {
    const { token } = req.params;

    // Ricerca dell'hash nel database
    const user = await User.findOne({ verificationToken: token });

    if (!user) {
      return res
        .status(400)
        .json({ message: "Token non valido, corrotto o già utilizzato." });
    }

    // Mutazione di Stato
    user.isVerified = true;
    user.verificationToken = undefined; // Sanificazione memoria (Token monouso)
    await user.save();

    res.status(200).json({
      message: "Handshake completato. Identità verificata con successo.",
    });
  } catch (error) {
    res.status(500).json({ message: "Errore fatale durante la verifica." });
  }
};

export const loginUser = async (req, res) => {
  /* 
     #swagger.tags = ['Auth']
     #swagger.description = 'Autentica l'utente e restituisce un token JWT se l'email è verificata.'
     #swagger.security = [] 
  */
  try {
    const { email, password } = req.body;

    //find user by email
    const user = await User.findOne({ email });

    if (!user) {
      return res.status(401).json({ message: "Invalid email" });
    }

    //compare provided password with stored hash
    const isPasswordValid = await bcrypt.compare(password, user.passwordHash);

    if (!isPasswordValid) {
      return res.status(401).json({ message: "password is invalid" });
    }

    if (!user) {
      return res.status(401).json({ message: "Invalid email" });
    }

    // NUOVO WATCHDOG: Blocco accesso se non verificato
    if (!user.isVerified) {
      return res.status(403).json({
        message: "Accesso negato. Eseguire la verifica SMTP inviata via email.",
      });
    }

    //generate JWT token
    const token = jwt.sign(
      { userId: user._id, role: user.role },
      process.env.JWT_SECRET,
      // token expires in 1 day by default, can be configured via .env with JWT_EXPIRES_IN variable
      { expiresIn: process.env.JWT_EXPIRES_IN || "1d" },
    );

    res.status(200).json({ token }); //I used https://jwt.io to decode the token
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};