import EarnedBadge from "../models/earnedBadge.js";
import User from "../models/user.js";
import HikeSession from "../models/hikeSession.js";
import Activity from "../models/activity.js";
import QuizAttempt from "../models/quizAttempt.js";
import NfcScan from "../models/nfcScan.js";
import Quiz from "../models/quiz.js";
import QuizCategory from "../models/quizCategory.js";

/**
 * Catalogo statico dei badge disponibili. Aggiungere un badge:
 *   1. Push qui sotto con check function
 *   2. (opzionale) Chiamare evaluateAllBadges in un job per attribuirlo retroattivamente
 *
 * Ogni badge ha:
 *   - code: identificatore univoco (snake_case)
 *   - name + description + emoji: visualizzazione client
 *   - tier: bronze/silver/gold/platinum (per ordinamento + colore)
 *   - check(userId) → { earned: bool, contextValue?: number }
 *     deve essere ASYNC. Se earned=true il record viene creato (o no-op se
 *     già esistente grazie all'unique index).
 */

async function countCompletedActivities(userId) {
  const [sessionsCount, activitiesCount] = await Promise.all([
    HikeSession.countDocuments({
      "participants.userId": userId,
      status: "COMPLETED",
    }),
    Activity.countDocuments({ userId }),
  ]);
  return sessionsCount + activitiesCount;
}

async function totalCredits(userId) {
  const user = await User.findById(userId).select("socialCredits").lean();
  return user?.socialCredits ?? 0;
}

async function totalNfcScans(userId) {
  return NfcScan.countDocuments({ userId, creditsAwarded: { $gt: 0 } });
}

async function totalQuizzesPassed(userId) {
  return QuizAttempt.countDocuments({ userId, passed: true });
}

/**
 * Ritorna l'elenco delle category quiz interamente superate dall'utente.
 * Una category è "completed" se l'utente ha passato TUTTI i quiz della cat.
 */
async function getCompletedQuizCategories(userId) {
  const categories = await QuizCategory.find().select("_id slug name").lean();
  const completed = [];
  for (const cat of categories) {
    const quizzes = await Quiz.find({ categoryId: cat._id }).select("_id").lean();
    if (quizzes.length === 0) continue;
    const quizIds = quizzes.map((q) => q._id);
    const passed = await QuizAttempt.countDocuments({
      userId,
      quizId: { $in: quizIds },
      passed: true,
    });
    // Nota: countDocuments su distinct(quizId) sarebbe più corretto, ma
    // l'idempotency di QuizAttempt + il check del passed=true rende tipicamente
    // 1:1 attempt:quiz. Per essere precisi:
    const distinctPassed = await QuizAttempt.distinct("quizId", {
      userId,
      quizId: { $in: quizIds },
      passed: true,
    });
    if (distinctPassed.length >= quizzes.length) {
      completed.push(cat);
    }
  }
  return completed;
}

export const BADGE_CATALOG = [
  {
    code: "first_steps",
    name: "Primi Passi",
    description: "Hai completato la tua prima escursione",
    emoji: "👟",
    tier: "bronze",
    check: async (userId) => {
      const n = await countCompletedActivities(userId);
      return { earned: n >= 1, contextValue: n };
    },
  },
  {
    code: "trail_runner",
    name: "Trail Runner",
    description: "5 escursioni completate",
    emoji: "🏃",
    tier: "bronze",
    check: async (userId) => {
      const n = await countCompletedActivities(userId);
      return { earned: n >= 5, contextValue: n };
    },
  },
  {
    code: "mountain_lover",
    name: "Amante della Montagna",
    description: "15 escursioni completate",
    emoji: "⛰️",
    tier: "silver",
    check: async (userId) => {
      const n = await countCompletedActivities(userId);
      return { earned: n >= 15, contextValue: n };
    },
  },
  {
    code: "veteran",
    name: "Veterano",
    description: "50 escursioni completate",
    emoji: "🎖️",
    tier: "gold",
    check: async (userId) => {
      const n = await countCompletedActivities(userId);
      return { earned: n >= 50, contextValue: n };
    },
  },
  {
    code: "credit_apprentice",
    name: "Apprendista",
    description: "500 Social Credits accumulati",
    emoji: "🥉",
    tier: "bronze",
    check: async (userId) => {
      const c = await totalCredits(userId);
      return { earned: c >= 500, contextValue: c };
    },
  },
  {
    code: "credit_expert",
    name: "Esperto",
    description: "2500 Social Credits accumulati",
    emoji: "🥈",
    tier: "silver",
    check: async (userId) => {
      const c = await totalCredits(userId);
      return { earned: c >= 2500, contextValue: c };
    },
  },
  {
    code: "credit_master",
    name: "Maestro",
    description: "10000 Social Credits accumulati",
    emoji: "🥇",
    tier: "gold",
    check: async (userId) => {
      const c = await totalCredits(userId);
      return { earned: c >= 10000, contextValue: c };
    },
  },
  {
    code: "checkpoint_collector",
    name: "Collezionista Checkpoint",
    description: "10 totem NFC scansionati",
    emoji: "📍",
    tier: "silver",
    check: async (userId) => {
      const n = await totalNfcScans(userId);
      return { earned: n >= 10, contextValue: n };
    },
  },
  {
    code: "quiz_curious",
    name: "Curioso",
    description: "5 quiz superati",
    emoji: "🎓",
    tier: "bronze",
    check: async (userId) => {
      const n = await totalQuizzesPassed(userId);
      return { earned: n >= 5, contextValue: n };
    },
  },
  {
    code: "category_champion",
    name: "Campione di Categoria",
    description: "Hai completato tutti i quiz di una categoria",
    emoji: "📜",
    tier: "gold",
    check: async (userId) => {
      const completedCats = await getCompletedQuizCategories(userId);
      return { earned: completedCats.length >= 1, contextValue: completedCats.length };
    },
  },
];

/**
 * Valuta TUTTI i badge del catalogo per l'utente e crea i record mancanti.
 * Idempotente: l'unique index su (userId, badgeCode) protegge da inserimenti
 * duplicati. Da chiamare dopo eventi significativi:
 *   - completeSession → potrebbe sbloccare first_steps / veteran / ecc.
 *   - createActivity → idem
 *   - addCredits → potrebbe sbloccare credit_*
 *   - submitQuiz → potrebbe sbloccare quiz_curious / category_champion
 *   - scanTotem → potrebbe sbloccare checkpoint_collector
 *
 * Per minimo impatto sui call site, in MVP la chiamiamo da completeSession +
 * createActivity + submitQuiz + scanTotem (dove avvengono i milestone naturali).
 */
export async function evaluateAllBadges(userId) {
  const newlyEarned = [];
  for (const badge of BADGE_CATALOG) {
    try {
      const result = await badge.check(userId);
      if (!result.earned) continue;
      // Atomic upsert: catch dup-key error → significa già earned, ok.
      try {
        await EarnedBadge.create({
          userId,
          badgeCode: badge.code,
          contextValue: result.contextValue,
        });
        newlyEarned.push(badge.code);
      } catch (err) {
        if (err.code !== 11000) throw err; // 11000 = duplicate key = già earned
      }
    } catch (err) {
      console.error(`[badgeService] check fallita per ${badge.code}:`, err.message);
    }
  }
  return newlyEarned;
}

/** Lista badge dell'utente, con metadata + flag `earned`. */
export async function listMyBadges(userId) {
  const earned = await EarnedBadge.find({ userId }).lean();
  const earnedMap = {};
  for (const e of earned) earnedMap[e.badgeCode] = e;

  return BADGE_CATALOG.map((b) => {
    const e = earnedMap[b.code];
    return {
      code: b.code,
      name: b.name,
      description: b.description,
      emoji: b.emoji,
      tier: b.tier,
      earned: !!e,
      earnedAt: e?.earnedAt ?? null,
      contextValue: e?.contextValue ?? null,
    };
  });
}

/**
 * Certificati: 1 per quiz-category interamente completata. Sono "virtuali" —
 * non hanno un record dedicato, vengono calcolati on-the-fly. La data di
 * emissione è la data del QuizAttempt più recente che ha completato la cat.
 */
export async function listMyCertificates(userId) {
  const completedCats = await getCompletedQuizCategories(userId);
  if (completedCats.length === 0) return [];
  // Per ogni cat completata, trova il timestamp dell'attempt che ha chiuso la collezione.
  const result = [];
  for (const cat of completedCats) {
    const quizzes = await Quiz.find({ categoryId: cat._id }).select("_id").lean();
    const lastAttempt = await QuizAttempt.findOne({
      userId,
      quizId: { $in: quizzes.map((q) => q._id) },
      passed: true,
    })
      .sort({ createdAt: -1 })
      .select("createdAt")
      .lean();
    result.push({
      categorySlug: cat.slug,
      categoryName: cat.name,
      issuedAt: lastAttempt?.createdAt ?? new Date(),
      totalQuizzes: quizzes.length,
    });
  }
  return result;
}
