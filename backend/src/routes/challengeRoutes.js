import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  createChallengeSchema,
  challengeRespondSchema,
  idParamSchema,
  validate,
} from "../middleware/validationMiddleware.js";
import {
  createChallenge,
  getChallengeById,
  listMyChallenges,
  respondToInvite,
  cancelChallenge,
} from "../services/challengeService.js";

const router = express.Router();
const mw = [authenticate, authenticatedLimiter];

router.use(...mw);

router.get("/", async (req, res, next) => {
  try {
    res.json(await listMyChallenges(req.user.userId));
  } catch (err) {
    next(err);
  }
});

router.post("/", validate(createChallengeSchema), async (req, res, next) => {
  try {
    const challenge = await createChallenge(req.user.userId, req.body);
    res.status(201).json(challenge);
  } catch (err) {
    next(err);
  }
});

router.get("/:id", validate(idParamSchema, "params"), async (req, res, next) => {
  try {
    res.json(await getChallengeById(req.params.id, req.user.userId));
  } catch (err) {
    if (err.message === "CHALLENGE_NOT_FOUND") return res.status(404).json({ message: "Sfida non trovata." });
    if (err.message === "FORBIDDEN") return res.status(403).json({ message: "Non autorizzato." });
    next(err);
  }
});

router.post("/:id/respond", validate(idParamSchema, "params"), validate(challengeRespondSchema), async (req, res, next) => {
  try {
    const challenge = await respondToInvite(req.params.id, req.user.userId, req.body.accept);
    res.json(challenge);
  } catch (err) {
    if (err.message === "CHALLENGE_NOT_FOUND") return res.status(404).json({ message: "Sfida non trovata." });
    if (err.message === "NOT_INVITED") return res.status(403).json({ message: "Non invitato a questa sfida." });
    if (err.message === "ALREADY_RESPONDED") return res.status(409).json({ message: "Hai già risposto a questo invito." });
    next(err);
  }
});

router.delete("/:id", validate(idParamSchema, "params"), async (req, res, next) => {
  try {
    await cancelChallenge(req.params.id, req.user.userId);
    res.json({ message: "Sfida cancellata." });
  } catch (err) {
    if (err.message === "CHALLENGE_NOT_FOUND") return res.status(404).json({ message: "Sfida non trovata." });
    if (err.message === "FORBIDDEN") return res.status(403).json({ message: "Solo il creator può cancellare." });
    if (err.message === "CANNOT_CANCEL_RUNNING") return res.status(409).json({ message: "Impossibile cancellare una sfida attiva o completata." });
    next(err);
  }
});

export default router;
