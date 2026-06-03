import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  updateAccountSchema,
  changePasswordSchema,
  deleteAccountSchema,
  verifyPasswordSchema,
  goalsSchema,
  personalInfoSchema,
  experienceSchema,
  preferencesSchema,
} from "../middleware/validationMiddleware.js";
import {
  updateUser,
  changePassword,
  deleteAccount,
  verifyPassword,
  updateGoals,
  updatePersonalInfo,
  updateExperience,
  updatePreferences,
  markProfileCompleted,
  getWeeklyStats,
} from "../services/accountService.js";

const router = express.Router();
const mw = [authenticate, authenticatedLimiter];

// Helper locale: applica uno schema Joi al body e ritorna { ok, value, response }.
// Manteniamo la stessa risposta 422 + { message: detail } che le route restituivano
// prima della centralizzazione (così il client mobile non deve cambiare nulla).
function validateBody(schema, body) {
  const { error, value } = schema.validate(body);
  if (error)
    return {
      ok: false,
      response: { status: 422, body: { message: error.details[0].message } },
    };
  return { ok: true, value };
}

router.patch("/me", ...mw, async (req, res, next) => {
  try {
    const v = validateBody(updateAccountSchema, req.body);
    if (!v.ok) return res.status(v.response.status).json(v.response.body);
    const result = await updateUser(req.user.userId, v.value);
    res.json(result);
  } catch (err) {
    next(err);
  }
});

router.post("/me/verify-password", ...mw, async (req, res, next) => {
  try {
    const v = validateBody(verifyPasswordSchema, req.body);
    if (!v.ok) return res.status(v.response.status).json(v.response.body);
    await verifyPassword(req.user.userId, v.value);
    res.json({ verified: true });
  } catch (err) {
    next(err);
  }
});

router.post("/change-password", ...mw, async (req, res, next) => {
  try {
    const v = validateBody(changePasswordSchema, req.body);
    if (!v.ok) return res.status(v.response.status).json(v.response.body);
    await changePassword(req.user.userId, v.value);
    res.json({ message: "Password aggiornata." });
  } catch (err) {
    next(err);
  }
});

router.delete("/me", ...mw, async (req, res, next) => {
  try {
    const v = validateBody(deleteAccountSchema, req.body);
    if (!v.ok) return res.status(v.response.status).json(v.response.body);
    await deleteAccount(req.user.userId, v.value);
    res.json({ message: "Account eliminato." });
  } catch (err) {
    next(err);
  }
});

router.patch("/me/goals", ...mw, async (req, res, next) => {
  try {
    const v = validateBody(goalsSchema, req.body);
    if (!v.ok) return res.status(v.response.status).json(v.response.body);
    const weeklyGoals = await updateGoals(req.user.userId, v.value);
    res.json({ weeklyGoals });
  } catch (err) {
    next(err);
  }
});

// ── Profilo v2: edit per-sezione ────────────────────────────────────────
// Ogni sezione ha la sua route per consentire update parziali atomici
// (l'utente in onboarding può salvare uno step alla volta, e nella schermata
// di edit ogni tab è indipendente — un errore in "preferences" non rollbacka
// "personalInfo" appena salvato).

router.patch("/me/personal-info", ...mw, async (req, res, next) => {
  try {
    const v = validateBody(personalInfoSchema, req.body);
    if (!v.ok) return res.status(v.response.status).json(v.response.body);
    const personalInfo = await updatePersonalInfo(req.user.userId, v.value);
    res.json({ personalInfo });
  } catch (err) {
    next(err);
  }
});

router.patch("/me/experience", ...mw, async (req, res, next) => {
  try {
    const v = validateBody(experienceSchema, req.body);
    if (!v.ok) return res.status(v.response.status).json(v.response.body);
    const experience = await updateExperience(req.user.userId, v.value);
    res.json({ experience });
  } catch (err) {
    next(err);
  }
});

router.patch("/me/preferences", ...mw, async (req, res, next) => {
  try {
    const v = validateBody(preferencesSchema, req.body);
    if (!v.ok) return res.status(v.response.status).json(v.response.body);
    const preferences = await updatePreferences(req.user.userId, v.value);
    res.json({ preferences });
  } catch (err) {
    next(err);
  }
});

// Stats della settimana ISO corrente (lun → dom): aggrega sessioni COMPLETED +
// attività libere. Usato dal widget "Obiettivi settimanali" nella ProfileScreen.
router.get("/me/weekly-stats", ...mw, async (req, res, next) => {
  try {
    res.json(await getWeeklyStats(req.user.userId));
  } catch (err) {
    next(err);
  }
});

// Idempotente. Chiamato da "Termina onboarding" e da "Salta tutto".
router.post("/me/profile-complete", ...mw, async (req, res, next) => {
  try {
    res.json(await markProfileCompleted(req.user.userId));
  } catch (err) {
    next(err);
  }
});

export default router;
