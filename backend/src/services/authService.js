import User from "../models/user.js";
import bcrypt from "bcrypt";
import jwt from "jsonwebtoken";

export const verifyEmail = async (req, res) => {
  /* #swagger.tags = ['Auth']
     #swagger.description = 'Verifica l'indirizzo email tramite token. Esegue un redirect (Deep Link) all'app mobile passando il JWT.'
     #swagger.security = [] 
  */
  try {
    const { token } = req.params;

    // 1. Ricerca dell'hash nel database
    const user = await User.findOne({ verificationToken: token });

    if (!user) {
      // Failsafe: Token invalido o già usato -> Redirect all'app con flag di errore
      return res.redirect("tsm://auth/error?message=token_invalido_o_scaduto");
    }

    // 2. Mutazione di Stato
    user.isVerified = true;
    user.verificationToken = undefined; // Sanificazione memoria (Token monouso)
    await user.save();

    // 3. AUTO-LOGIN: Generazione del Token JWT crittografico
    const jwtToken = jwt.sign(
      { userId: user._id, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || "1d" },
    );

    // 4. DIROTTO: Chiude il browser e risveglia l'app passando il token
    res.redirect(`tsm://auth/success?jwt=${jwtToken}`);
  } catch (error) {
    console.error("Errore fatale in verifyEmail:", error);
    res.redirect("tsm://auth/error?message=errore_server_interno");
  }
};

export const loginUser = async (req, res) => {
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
