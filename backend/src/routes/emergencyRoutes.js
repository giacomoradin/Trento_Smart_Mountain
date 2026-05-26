import express from "express";
import { authenticate } from "../middleware/authMiddleware.js";
import { authenticatedLimiter } from "../middleware/rateLimitMiddleware.js";
import {
  validate,
  createEmergencySchema,
  patchEmergencySchema,
  idParamSchema,
} from "../middleware/validationMiddleware.js";
import {
  createEmergency,
  getEmergencyById,
  patchEmergency,
} from "../services/emergencyService.js";

const router = express.Router();

router.use(authenticate);
router.use(authenticatedLimiter);

const ERROR_MAP = {
  SESSION_NOT_FOUND: 404,
  SESSION_NOT_ACTIVE: 409,
  EMERGENCY_NOT_FOUND: 404,
  EMERGENCY_ALREADY_CLOSED: 409,
  INVALID_STATE_TRANSITION: 409,
  INVALID_ACTION: 400,
  USER_NOT_FOUND: 404,
  FORBIDDEN: 403,
};

function handleEmergencyError(err, res) {
  const status = ERROR_MAP[err.message] || 500;
  if (status === 500) console.error("[emergencyRoutes]", err);
  return res.status(status).json({ error: err.message });
}

// POST /api/v1/emergencies
router.post("/", validate(createEmergencySchema), async (req, res) => {
  try {
    const { emergency, isNew } = await createEmergency(req.user.userId, req.body);
    res.status(isNew ? 201 : 200).json(emergency);
  } catch (err) {
    handleEmergencyError(err, res);
  }
});

// GET /api/v1/emergencies/:id
router.get("/:id", validate(idParamSchema, "params"), async (req, res) => {
  try {
    const emergency = await getEmergencyById(req.params.id, req.user.userId);
    res.status(200).json(emergency);
  } catch (err) {
    handleEmergencyError(err, res);
  }
});

// PATCH /api/v1/emergencies/:id
router.patch(
  "/:id",
  validate(idParamSchema, "params"),
  validate(patchEmergencySchema),
  async (req, res) => {
    try {
      const { action, reason } = req.body;
      const emergency = await patchEmergency(req.params.id, req.user.userId, action, { reason });
      res.status(200).json(emergency);
    } catch (err) {
      handleEmergencyError(err, res);
    }
  },
);

export default router;
