import Hiker from "../models/hiker.js";
import bcrypt from "bcrypt";
import crypto from "crypto";
import { sendVerificationEmail } from "./emailService.js";

/**
 * Servizio dedicato agli utenti **escursionisti** (capogruppo).
 *
 * Le route `/hikers/*` consumano queste funzioni. Il modello Hiker
 * (discriminator di User) garantisce che venga sempre creato un documento
 * con `role: "groupLeader"`.
 */

/**
 * POST /auth/register/hiker
 * Registra un nuovo escursionista. Il role è impostato implicitamente dal modello.
 */
export const createHiker = async (req, res) => {
  /*
     #swagger.tags = ['Hikers']
     #swagger.description = 'Registra un nuovo escursionista (groupLeader).'
  */
  try {
    const { username, email, password } = req.body;

    if (!username || !email || !password) {
      return res.status(400).json({
        message: "Username, email e password sono obbligatori.",
      });
    }
    if (password.length < 8) {
      return res.status(400).json({
        message: "La password deve essere di almeno 8 caratteri.",
      });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const verificationToken = crypto.randomBytes(32).toString("hex");

    const hiker = new Hiker({
      username,
      email,
      passwordHash,
      isVerified: false,
      verificationToken,
    });

    const saved = await hiker.save();

    // Invio email asincrono (non blocca la response)
    sendVerificationEmail(email, verificationToken).catch((err) => {
      console.error("[hikerService] Invio email verifica fallito:", err.message);
    });

    const { passwordHash: _p, verificationToken: _v, __v, ...userPublic } =
      saved.toObject();

    res.status(201).json({
      message: "Account escursionista creato. Verifica la tua email per attivare l'account.",
      user: userPublic,
    });
  } catch (error) {
    if (error.code === 11000) {
      return res.status(409).json({
        message: "Email o username già registrati.",
      });
    }
    res.status(500).json({ message: error.message });
  }
};

/**
 * GET /hikers/:id — profilo escursionista.
 */
export const getHikerById = async (req, res) => {
  /*
     #swagger.tags = ['Hikers']
     #swagger.description = 'Recupera il profilo di un escursionista.'
  */
  try {
    const hiker = await Hiker.findById(req.params.id).select("-passwordHash -__v");
    if (!hiker) {
      return res.status(404).json({ message: "Escursionista non trovato." });
    }
    res.status(200).json(hiker);
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "ID utente non valido." });
    }
    res.status(500).json({ message: error.message });
  }
};

/**
 * PUT /hikers/:id — aggiorna profilo escursionista (solo i campi propri).
 */
export const updateHiker = async (req, res) => {
  /*
     #swagger.tags = ['Hikers']
     #swagger.description = 'Aggiorna i dati di un escursionista (solo self o admin).'
  */
  try {
    // Authorization: solo il proprietario o un admin può modificare un profilo.
    const isSelf = req.user?.userId?.toString() === req.params.id;
    const isAdmin = req.user?.role === "admin";
    if (!isSelf && !isAdmin) {
      return res.status(403).json({ message: "Non sei autorizzato a modificare questo profilo." });
    }

    // Campi ammessi: nessun campo specifico Hiker per ora, solo base
    const allowedUpdates = ["username", "email"];
    const updates = {};
    for (const key of allowedUpdates) {
      if (req.body[key] !== undefined) updates[key] = req.body[key];
    }

    const updated = await Hiker.findByIdAndUpdate(req.params.id, updates, {
      new: true,
      runValidators: true,
    }).select("-passwordHash -__v");

    if (!updated) {
      return res.status(404).json({ message: "Escursionista non trovato." });
    }
    res.status(200).json(updated);
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "ID utente non valido." });
    }
    if (error.code === 11000) {
      return res.status(409).json({ message: "Email o username già in uso." });
    }
    res.status(500).json({ message: error.message });
  }
};
