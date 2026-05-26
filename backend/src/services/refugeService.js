import Refuge from "../models/refuge.js";
import bcrypt from "bcrypt";
import crypto from "crypto";
import { sendVerificationEmail } from "./emailService.js";

/**
 * Servizio dedicato agli utenti **rifugio**.
 *
 * Il modello Refuge (discriminator di User) include i metadati anagrafici
 * della struttura: rifugioName, caiCode, quota, posti, coordinates.
 */

/**
 * POST /auth/register/refuge
 * Registra un nuovo rifugio con i suoi metadati. Il role è impostato dal modello.
 */
export const createRefuge = async (req, res) => {
  /*
     #swagger.tags = ['Refuges']
     #swagger.description = 'Registra un nuovo account rifugio con metadati struttura.'
  */
  try {
    const {
      username,
      email,
      password,
      rifugioName,
      caiCode,
      quota,
      posti,
      coordinates,
    } = req.body;

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
    if (!rifugioName) {
      return res.status(400).json({
        message: "Il nome del rifugio è obbligatorio.",
      });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const verificationToken = crypto.randomBytes(32).toString("hex");

    const refuge = new Refuge({
      username,
      email,
      passwordHash,
      isVerified: false,
      verificationToken,
      rifugioName,
      caiCode,
      quota,
      posti,
      coordinates,
    });

    const saved = await refuge.save();

    // Invio email sincrono: se fallisce, cancelliamo l'utente e restituiamo errore.
    try {
      await sendVerificationEmail(email, verificationToken);
    } catch (err) {
      console.error(
        "[refugeService] Invio email verifica fallito:",
        err.message,
      );
      await refuge.deleteOne(); // Rollback creazione utente
      return res.status(500).json({
        message:
          "Errore durante l'invio dell'email di verifica. L'account non è stato creato, riprova tra qualche istante.",
      });
    }

    const {
      passwordHash: _p,
      verificationToken: _v,
      __v,
      ...refugePublic
    } = saved.toObject();

    res.status(201).json({
      message:
        "Account rifugio creato. Verifica la tua email per attivare l'account.",
      user: refugePublic,
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
 * GET /refuges/:id — profilo rifugio.
 */
export const getRefugeById = async (req, res) => {
  /*
     #swagger.tags = ['Refuges']
     #swagger.description = 'Recupera il profilo di un rifugio inclusi i metadati struttura.'
  */
  try {
    const refuge = await Refuge.findById(req.params.id).select(
      "-passwordHash -__v",
    );
    if (!refuge) {
      return res.status(404).json({ message: "Rifugio non trovato." });
    }
    res.status(200).json(refuge);
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "ID rifugio non valido." });
    }
    res.status(500).json({ message: error.message });
  }
};

/**
 * GET /refuges — lista pubblica di tutti i rifugi (per future feature di ricerca).
 */
export const listRefuges = async (req, res) => {
  /*
     #swagger.tags = ['Refuges']
     #swagger.description = 'Lista tutti i rifugi registrati (solo dati pubblici).'
  */
  try {
    const refuges = await Refuge.find()
      .select("rifugioName caiCode quota posti coordinates")
      .sort({ rifugioName: 1 });
    res.status(200).json(refuges);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

/**
 * PUT /refuges/:id — aggiorna metadati rifugio (solo self o admin).
 */
export const updateRefuge = async (req, res) => {
  /*
     #swagger.tags = ['Refuges']
     #swagger.description = 'Aggiorna i metadati di un rifugio (solo self o admin).'
  */
  try {
    // Authorization: solo il proprietario o un admin può modificare un rifugio.
    const isSelf = req.user?.userId?.toString() === req.params.id;
    const isAdmin = req.user?.role === "admin";
    if (!isSelf && !isAdmin) {
      return res
        .status(403)
        .json({ message: "Non sei autorizzato a modificare questo profilo." });
    }

    const allowedUpdates = [
      "username",
      "email",
      "rifugioName",
      "caiCode",
      "quota",
      "posti",
      "coordinates",
    ];
    const updates = {};
    for (const key of allowedUpdates) {
      if (req.body[key] !== undefined) updates[key] = req.body[key];
    }

    const updated = await Refuge.findByIdAndUpdate(req.params.id, updates, {
      new: true,
      runValidators: true,
    }).select("-passwordHash -__v");

    if (!updated) {
      return res.status(404).json({ message: "Rifugio non trovato." });
    }
    res.status(200).json(updated);
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "ID rifugio non valido." });
    }
    if (error.code === 11000) {
      return res.status(409).json({ message: "Email o username già in uso." });
    }
    res.status(500).json({ message: error.message });
  }
};
