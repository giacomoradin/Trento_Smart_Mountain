import mongoose from "mongoose";
import User from "./user.js";

/**
 * Discriminator per gli utenti **escursionisti** (capogruppo).
 *
 * - Discriminator key: "groupLeader"
 * - Estende lo schema base User con i campi specifici dell'escursionista:
 *   gamification (socialCredits, weeklyGoals, nfcStats) + profilo v2
 *   (personalInfo, experience, preferences, profileCompletedAt).
 *
 * Tutti i documenti Hiker hanno `role: "groupLeader"` nella collection `users`.
 *
 * NOTA scheda medica: i campi sensibili (gruppo sanguigno, allergie, patologie,
 * contatto emergenza) NON sono in questo schema. Richiedono trattamento art. 9
 * GDPR (categoria particolare) → iterazione dedicata con DPIA, consenso esplicito
 * e possibile cifratura at-rest. Aggiungere qui solo dopo quella iterazione.
 */
const hikerSchema = new mongoose.Schema({
  // ── Gamification (preesistente) ──────────────────────────────────────────
  socialCredits: { type: Number, default: 0, min: 0, index: true },

  weeklyGoals: {
    km: { type: Number, default: 0, min: 0, max: 500 },
    elevM: { type: Number, default: 0, min: 0, max: 20000 },
    count: { type: Number, default: 0, min: 0, max: 50 },
  },

  nfcStats: {
    scansCount: { type: Number, default: 0 },
    scansCredits: { type: Number, default: 0 },
  },

  // ── Idempotency claim per crediti quiz ───────────────────────────────────
  // Lista degli `Quiz._id` per cui l'utente ha già ricevuto il primo bonus
  // crediti. Usata in submitQuiz come "atomic claim" anti race condition:
  // due submit simultanei vedono entrambi `passed=true`, ma solo uno riesce
  // a fare $addToSet su questo array (l'altro è no-op) → niente doppio credito.
  rewardedQuizzes: [{ type: mongoose.Schema.Types.ObjectId, ref: "Quiz" }],

  // ── Profilo v2: dati personali ───────────────────────────────────────────
  // Usati per stime kcal, statistiche demo, baseline scoring (vedi userScoringService).
  // Tutti i campi sono opzionali — l'utente può saltare l'onboarding.
  personalInfo: {
    sex: { type: String, enum: ["M", "F", "X", "N"] }, // N = preferisco non dire
    birthDate: { type: Date }, // età derivata, non memorizzata
    heightCm: { type: Number, min: 100, max: 230 },
    weightKg: { type: Number, min: 30, max: 250 },
    avatarUrl: { type: String }, // supporta Base64 o URL esterni
  },

  // ── Profilo v2: esperienza outdoor ───────────────────────────────────────
  // caiLevel + baselineFitness alimentano il moltiplicatore μ_user_baseline
  // nel calcolo crediti delle sessioni (premia la sfida personale).
  experience: {
    caiLevel: { type: String, enum: ["T", "E", "EE", "EEA"] }, // baseline difficoltà tecnica
    baselineFitness: {
      type: String,
      enum: ["sedentary", "active", "sport", "athlete"],
    },
    weeklyTrainingFreq: { type: String, enum: ["0-1", "2-3", "4+"] },
  },

  // ── Profilo v2: preferenze app ───────────────────────────────────────────
  preferences: {
    units: { type: String, enum: ["metric", "imperial"], default: "metric" },
    language: { type: String, default: "it" },
    notifications: {
      pushEnabled: { type: Boolean, default: true },
      emailDigest: { type: Boolean, default: false },
      // FCM token registrato dal client per push notifications.
      // Salvato solo se pushEnabled === true; clearato al logout.
      fcmToken: { type: String },
    },
    privacy: {
      // Visibilità del profilo nella futura sezione Social.
      profileVisibility: {
        type: String,
        enum: ["public", "friends", "private"],
        default: "friends",
      },
    },
  },

  // Timestamp di completamento onboarding. Null = primo accesso, mostra banner
  // "Completa il tuo profilo" + flow guidato. Set anche con "Salta tutto" per
  // evitare di forzare l'utente che non vuole condividere dati.
  profileCompletedAt: { type: Date, default: null },
});

const Hiker = User.discriminator("groupLeader", hikerSchema);
export default Hiker;
