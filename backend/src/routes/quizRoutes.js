import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import { quizSubmitSchema } from "../middleware/validationMiddleware.js";
import {
  listCategories,
  listByCategory,
  getNextQuizForCategory,
  getQuizForClient,
  submitQuiz,
} from "../services/quizService.js";

const router = express.Router();
const mw = [authenticate, authenticatedLimiter];

router.get("/categories", ...mw, async (req, res, next) => {
  try {
    res.json(await listCategories(req.user.userId));
  } catch (err) {
    next(err);
  }
});

router.get("/categories/:slug/quizzes", ...mw, async (req, res, next) => {
  try {
    res.json(await listByCategory(req.params.slug, req.user.userId));
  } catch (err) {
    if (err.message === "CATEGORY_NOT_FOUND") return res.status(404).json({ message: "Categoria non trovata." });
    next(err);
  }
});

// Restituisce il prossimo quiz non superato (entry point "Continua →" del mockup)
router.get("/categories/:slug/next", ...mw, async (req, res, next) => {
  try {
    res.json(await getNextQuizForCategory(req.params.slug, req.user.userId));
  } catch (err) {
    if (err.message === "CATEGORY_NOT_FOUND") return res.status(404).json({ message: "Categoria non trovata." });
    next(err);
  }
});

router.get("/:id", ...mw, async (req, res, next) => {
  try {
    res.json(await getQuizForClient(req.params.id, req.user.userId));
  } catch (err) {
    if (err.message === "QUIZ_NOT_FOUND") return res.status(404).json({ message: "Quiz non trovato." });
    next(err);
  }
});

router.post("/:id/submit", ...mw, async (req, res, next) => {
  try {
    const { error, value } = quizSubmitSchema.validate(req.body);
    if (error) return res.status(422).json({ message: error.details[0].message });
    res.json(await submitQuiz(req.params.id, req.user.userId, value.answers));
  } catch (err) {
    if (err.message === "QUIZ_NOT_FOUND") return res.status(404).json({ message: "Quiz non trovato." });
    next(err);
  }
});

export default router;
