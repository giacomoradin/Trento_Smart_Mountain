import User from "../models/user.js";
import Admin from "../models/admin.js";
import Hiker from "../models/hiker.js";
import Refuge from "../models/refuge.js";
import bcrypt from "bcrypt";

/**
 * Servizio dedicato agli **amministratori** del sistema.
 *
 * Le route admin permettono di:
 *  - creare nuovi admin (solo da altro admin)
 *  - listare tutti gli utenti (qualunque ruolo)
 *  - aggiornare ruolo/dati di qualsiasi utente
 *  - eliminare utenti
 */

/**
 * POST /admin/users — crea un nuovo admin (richiede JWT admin).
 */
export const createAdmin = async (req, res) => {
  /*
     #swagger.tags = ['Admin']
     #swagger.description = 'Crea un nuovo utente con ruolo admin. Solo admin autenticati.'
  */
  try {
    const { username, email, password } = req.body;
    if (!username || !email || !password) {
      return res.status(400).json({
        message: "Username, email e password sono obbligatori.",
      });
    }
    const passwordHash = await bcrypt.hash(password, 10);

    const admin = new Admin({
      username,
      email,
      passwordHash,
      isVerified: true, // gli admin sono creati da altri admin, no email verification
    });
    const saved = await admin.save();
    const { passwordHash: _p, __v, ...adminPublic } = saved.toObject();
    res.status(201).json({ message: "Admin creato.", user: adminPublic });
  } catch (error) {
    if (error.code === 11000) {
      return res.status(409).json({ message: "Email o username già in uso." });
    }
    res.status(500).json({ message: error.message });
  }
};

/**
 * GET /admin/users — elenco di tutti gli utenti (qualunque ruolo).
 */
export const listAllUsers = async (req, res) => {
  /*
     #swagger.tags = ['Admin']
     #swagger.description = 'Lista tutti gli utenti registrati. Solo admin.'
  */
  try {
    const users = await User.find().select("-passwordHash -__v");
    res.status(200).json(users);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

/**
 * GET /admin/users/:id — dettaglio di qualunque utente.
 */
export const getAnyUserById = async (req, res) => {
  /*
     #swagger.tags = ['Admin']
     #swagger.description = 'Recupera dati di qualunque utente. Solo admin.'
  */
  try {
    const user = await User.findById(req.params.id).select("-passwordHash -__v");
    if (!user) {
      return res.status(404).json({ message: "Utente non trovato." });
    }
    res.status(200).json(user);
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "ID utente non valido." });
    }
    res.status(500).json({ message: error.message });
  }
};

/**
 * PUT /admin/users/:id — aggiorna qualunque utente (incluso role).
 */
export const updateAnyUser = async (req, res) => {
  /*
     #swagger.tags = ['Admin']
     #swagger.description = 'Aggiorna dati di un utente (incluso il role). Solo admin.'
  */
  try {
    const allowedUpdates = [
      "username",
      "email",
      "role",
      "isVerified",
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

    // IMPORTANTE: i campi rifugioName/caiCode/quota/posti/coordinates sono
    // del discriminator Refuge. Aggiornarli via User base verrebbe scartato
    // dallo strict mode (vedi nota in creditService). Usiamo il modello del
    // discriminator corretto in base al ruolo target.
    const targetUser = await User.findById(req.params.id).select("role").lean();
    if (!targetUser) {
      return res.status(404).json({ message: "Utente non trovato." });
    }
    const ModelByRole = {
      rifugio: Refuge,
      groupLeader: Hiker,
      admin: Admin,
    };
    const TargetModel = ModelByRole[targetUser.role] || User;

    const updated = await TargetModel.findByIdAndUpdate(req.params.id, updates, {
      new: true,
      runValidators: true,
    }).select("-passwordHash -__v");

    if (!updated) {
      return res.status(404).json({ message: "Utente non trovato." });
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

/**
 * DELETE /admin/users/:id — elimina qualunque utente.
 */
export const deleteAnyUser = async (req, res) => {
  /*
     #swagger.tags = ['Admin']
     #swagger.description = 'Elimina definitivamente un utente. Solo admin.'
  */
  try {
    const deleted = await User.findByIdAndDelete(req.params.id);
    if (!deleted) {
      return res.status(404).json({ message: "Utente non trovato." });
    }
    res.status(200).json({ message: "Utente eliminato con successo." });
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "ID utente non valido." });
    }
    res.status(500).json({ message: error.message });
  }
};
