// Validazione schema-based con Joi.
// Tutti gli schemi rifiutano i campi sconosciuti per evitare mass-assignment.
import Joi from "joi";

// Factory: prende uno schema e ritorna un middleware Express che valida la
// parte indicata (body/query/params). In caso di errore risponde 400 con la
// lista dei problemi.
export function validate(schema, source = "body") {
  return (req, res, next) => {
    const { error, value } = schema.validate(req[source], {
      abortEarly: false,
      stripUnknown: false,
      convert: true,
    });
    if (error) {
      return res.status(422).json({
        error: "Validazione fallita",
        details: error.details.map((d) => ({ path: d.path.join("."), message: d.message })),
      });
    }
    req[source] = value;
    next();
  };
}

// ── Auth ────────────────────────────────────────────────────────────────

const emailField = Joi.string().email({ tlds: { allow: false } }).max(254).lowercase().trim();
const passwordField = Joi.string().min(8).max(128);
const usernameField = Joi.string().alphanum().min(3).max(40).trim();
const objectIdField = Joi.string().pattern(/^[0-9a-fA-F]{24}$/).message("ID non valido");

export const loginSchema = Joi.object({
  email: emailField.required(),
  password: passwordField.required(),
});

export const registerHikerSchema = Joi.object({
  username: usernameField.required(),
  email: emailField.required(),
  password: passwordField.required(),
});

export const registerRefugeSchema = Joi.object({
  username: usernameField.required(),
  email: emailField.required(),
  password: passwordField.required(),
  rifugioName: Joi.string().min(2).max(120).trim().required(),
  caiCode: Joi.string().max(40).trim().allow("", null),
  quota: Joi.number().integer().min(0).max(5000),
  posti: Joi.number().integer().min(1).max(2000),
  coordinates: Joi.string().max(100).trim().allow("", null),
});

export const forgotPasswordSchema = Joi.object({
  email: emailField.required(),
});

export const resetPasswordSchema = Joi.object({
  password: passwordField.required(),
  confirmPassword: passwordField,
}).custom((value, helpers) => {
  if (value.confirmPassword && value.password !== value.confirmPassword) {
    return helpers.error("any.invalid", { message: "Le password non corrispondono" });
  }
  return value;
});

// ── Sessioni ────────────────────────────────────────────────────────────

const difficultyField = Joi.string().valid("T", "E", "EE", "EEA");

const geoPointSchema = Joi.object({
  type: Joi.string().valid("Point"),
  coordinates: Joi.array().items(Joi.number()).length(2),
});

const gpxStatsSchema = Joi.object({
  distanceKm: Joi.number().min(0).max(1000),
  elevationGainM: Joi.number().integer().min(0).max(15000),
  trackPoints: Joi.number().integer().min(0).max(100000),
  elevationProfile: Joi.array().items(Joi.number()).max(50),
  estimatedPoints: Joi.number().integer().min(0).max(100000),
  gpxDurationSec: Joi.number().integer().min(0).max(7 * 24 * 3600),
});

export const createSessionSchema = Joi.object({
  routeDetails: Joi.object({
    name: Joi.string().min(1).max(120).trim().required(),
    difficultyLevel: difficultyField.default("E"),
    elevationGain: Joi.number().integer().min(0).max(15000),
    startPoint: geoPointSchema,
    endPoint: geoPointSchema,
  }).required(),
  meetingDate: Joi.string().max(40).trim().allow(null, ""),
  meetingTime: Joi.string().max(20).trim().allow(null, ""),
  meetingLocation: Joi.string().max(200).trim().allow(null, ""),
  maxParticipants: Joi.number().integer().min(1).max(50),
  minExperienceLevel: difficultyField,
  gpxFileName: Joi.string().max(200),
  gpxStats: gpxStatsSchema,
});

export const updateSessionSchema = Joi.object({
  routeDetails: Joi.object({
    name: Joi.string().min(1).max(120).trim(),
    difficultyLevel: difficultyField,
  }),
  meetingDate: Joi.string().max(40).trim().allow(null, ""),
  meetingTime: Joi.string().max(20).trim().allow(null, ""),
  meetingLocation: Joi.string().max(200).trim().allow(null, ""),
  maxParticipants: Joi.number().integer().min(1).max(50),
  minExperienceLevel: difficultyField,
}).min(1);

export const updateSessionStatusSchema = Joi.object({
  status: Joi.string().valid("PLANNED", "ACTIVE", "COMPLETED", "CANCELLED").required(),
});

export const joinSessionSchema = Joi.object({
  inviteCode: Joi.string().trim().uppercase().pattern(/^TSM-[A-F0-9]{4}$/).required(),
});

// Stats opzionali — usate in PATCH /sessions/:id/complete: il client può
// completare una sessione anche senza metriche (fallback CAI server-side).
const actualStatsSchema = Joi.object({
  movingSeconds: Joi.number().integer().min(0).max(7 * 24 * 3600),
  totalSeconds: Joi.number().integer().min(0).max(7 * 24 * 3600),
  distanceMeters: Joi.number().min(0).max(1000 * 1000),
  elevationGainM: Joi.number().integer().min(0).max(15000),
  finalPoints: Joi.number().integer().min(0).max(100000),
  estimatedCalories: Joi.number().integer().min(0).max(50000),
  currentAltitudeM: Joi.number().integer().min(-500).max(10000),
});

// Stats obbligatorie — per le attività libere POST /activities la persistenza
// senza metriche non ha senso (allinea con required del modello Mongoose).
const actualStatsRequiredSchema = actualStatsSchema.fork(
  ["movingSeconds", "totalSeconds", "distanceMeters", "elevationGainM"],
  (s) => s.required(),
);

export const completeSessionSchema = Joi.object({
  actualStats: actualStatsSchema,
});

// ── Activity (libere) ───────────────────────────────────────────────────

export const createActivitySchema = Joi.object({
  name: Joi.string().min(1).max(120).trim().required(),
  activityType: Joi.string().valid("hiking", "trail", "skitouring", "trekking").default("hiking"),
  startTimeMs: Joi.number().integer().min(0).required(),
  endTimeMs: Joi.number().integer().min(0).greater(Joi.ref("startTimeMs")).required(),
  actualStats: actualStatsRequiredSchema.required(),
  difficultyLevel: difficultyField,
  elevationProfile: Joi.array().items(Joi.number()).max(200),
});

// ── Params / query ──────────────────────────────────────────────────────

export const idParamSchema = Joi.object({
  id: objectIdField.required(),
});

export const statsQuerySchema = Joi.object({
  year: Joi.number().integer().min(2000).max(3000),
});

// ── Account (self-service) ──────────────────────────────────────────────

export const updateAccountSchema = Joi.object({
  username: Joi.string().min(3).max(30).optional(),
  email:    emailField.optional(),
}).min(1);

export const changePasswordSchema = Joi.object({
  oldPassword: Joi.string().required(),
  newPassword: passwordField.required(),
});

export const deleteAccountSchema = Joi.object({
  password: Joi.string().required(),
});

export const goalsSchema = Joi.object({
  km:    Joi.number().min(0).max(500).optional(),
  elevM: Joi.number().min(0).max(20000).optional(),
  count: Joi.number().min(0).max(50).optional(),
}).min(1);

// ── Quiz ────────────────────────────────────────────────────────────────

export const quizSubmitSchema = Joi.object({
  answers: Joi.array()
    .items(
      Joi.object({
        questionId: Joi.string().required(),
        choiceIndex: Joi.number().integer().min(0).max(3).required(),
      }),
    )
    .max(50)
    .required(),
});

// ── NFC ─────────────────────────────────────────────────────────────────

export const nfcScanSchema = Joi.object({
  tagId:  Joi.string().required(),
  gpsLon: Joi.number().min(-180).max(180).required(),
  gpsLat: Joi.number().min(-90).max(90).required(),
});

export const nfcTotemCreateSchema = Joi.object({
  tagId:         Joi.string().required(),
  name:          Joi.string().required(),
  description:   Joi.string().max(500).optional(),
  lon:           Joi.number().min(-180).max(180).required(),
  lat:           Joi.number().min(-90).max(90).required(),
  altitude:      Joi.number().optional(),
  radius:        Joi.number().min(10).max(500).default(50),
  creditsReward: Joi.number().min(0).max(500).default(25),
  kind:          Joi.string().valid("checkpoint", "summit", "refuge").default("checkpoint"),
});

// ── Profilo v2 (onboarding + edit) ──────────────────────────────────────
// Tutti i campi sono opzionali a livello di singolo sub-schema: l'utente può
// salvare anche solo il sesso o solo l'altezza. `.min(1)` impedisce body vuoti.

export const personalInfoSchema = Joi.object({
  sex: Joi.string().valid("M", "F", "X", "N"),
  // Data nascita: serializzata come ISO 8601 dal client; il min cap 1900-01-01
  // evita inserimenti palesemente errati, il max (oggi) impedisce date future.
  birthDate: Joi.date().iso().min("1900-01-01").max("now"),
  heightCm: Joi.number().integer().min(100).max(230),
  weightKg: Joi.number().min(30).max(250),
}).min(1);

export const experienceSchema = Joi.object({
  caiLevel: difficultyField,
  baselineFitness: Joi.string().valid("sedentary", "active", "sport", "athlete"),
  weeklyTrainingFreq: Joi.string().valid("0-1", "2-3", "4+"),
}).min(1);

// ── Challenges ──────────────────────────────────────────────────────────

export const createChallengeSchema = Joi.object({
  title: Joi.string().min(3).max(80).trim().required(),
  description: Joi.string().max(280).trim().allow("", null),
  metric: Joi.string().valid("distance", "elevation", "count", "points").required(),
  targetValue: Joi.number().min(0).max(1000000).optional(),
  startDate: Joi.date().iso().required(),
  endDate: Joi.date().iso().greater(Joi.ref("startDate")).required(),
  participantUserIds: Joi.array().items(objectIdField).max(20).default([]),
});

export const challengeRespondSchema = Joi.object({
  accept: Joi.boolean().required(),
});

export const preferencesSchema = Joi.object({
  units: Joi.string().valid("metric", "imperial"),
  language: Joi.string().length(2), // ISO 639-1: it, en, de, ...
  notifications: Joi.object({
    pushEnabled: Joi.boolean(),
    emailDigest: Joi.boolean(),
    fcmToken: Joi.string().max(255).allow(null, ""),
  }),
  privacy: Joi.object({
    profileVisibility: Joi.string().valid("public", "friends", "private"),
  }),
}).min(1);

// ── Emergenze SOS ───────────────────────────────────────────────────────

const emergencyTypeField = Joi.string().valid(
  "INJURY",
  "LOST",
  "AVALANCHE",
  "WEATHER",
  "EQUIPMENT",
  "OTHER",
);

const profileSnapshotSchema = Joi.object({
  displayName: Joi.string().min(1).max(80).required(),
  personalInfo: Joi.object({
    sex: Joi.string().valid("M", "F", "X", "N"),
    birthDate: Joi.date().iso(),
    heightCm: Joi.number().integer().min(100).max(230),
    weightKg: Joi.number().min(30).max(250),
  }),
  experience: Joi.object({
    caiLevel: difficultyField,
    baselineFitness: Joi.string().valid("sedentary", "active", "sport", "athlete"),
    weeklyTrainingFreq: Joi.string().valid("0-1", "2-3", "4+"),
  }),
});

export const createEmergencySchema = Joi.object({
  sessionId: objectIdField.required(),
  emergencyType: emergencyTypeField.required(),
  coordinates: geoPointSchema.required(),
  beaconInstanceId: Joi.string()
    .pattern(/^[0-9a-fA-F]{12}$/)
    .required(),
  idempotencyKey: Joi.string().uuid({ version: "uuidv4" }).required(),
  signature: Joi.string().max(512).allow(null, ""),
  beaconActive: Joi.boolean().optional().default(true),
  profileSnapshot: profileSnapshotSchema,
});

export const patchEmergencySchema = Joi.object({
  action: Joi.string()
    .valid("cancel", "dismiss", "share_with_group", "unshare_with_group", "ack")
    .required(),
  reason: Joi.string().valid("MISTAKE", "RESOLVED_SELF").when("action", {
    is: "cancel",
    then: Joi.optional(),
    otherwise: Joi.forbidden(),
  }),
});
