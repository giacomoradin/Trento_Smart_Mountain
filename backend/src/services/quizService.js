import QuizCategory from "../models/quizCategory.js";
import Quiz from "../models/quiz.js";
import QuizAttempt from "../models/quizAttempt.js";
import Hiker from "../models/hiker.js";
import { addCredits } from "./creditService.js";
import { evaluateAllBadges } from "./badgeService.js";

export async function listCategories(userId) {
  const categories = await QuizCategory.find().sort({ sortOrder: 1 }).lean();
  const quizzes = await Quiz.find().select("categoryId creditsReward").lean();

  const categoryMap = {};
  for (const q of quizzes) {
    const key = q.categoryId.toString();
    if (!categoryMap[key]) categoryMap[key] = { total: 0, totalCredits: 0 };
    categoryMap[key].total++;
    categoryMap[key].totalCredits += q.creditsReward ?? 0;
  }

  const passedAttempts = await QuizAttempt.find({ userId, passed: true })
    .select("quizId creditsAwarded")
    .lean();

  const passedMap = {};
  for (const a of passedAttempts) {
    const quizId = a.quizId.toString();
    if (!passedMap[quizId]) passedMap[quizId] = a.creditsAwarded ?? 0;
  }

  const quizIdToCategory = {};
  for (const q of quizzes) {
    quizIdToCategory[q._id.toString()] = q.categoryId.toString();
  }

  const earnedByCategory = {};
  const passedCountByCategory = {};
  for (const [quizId, credits] of Object.entries(passedMap)) {
    const catId = quizIdToCategory[quizId];
    if (!catId) continue;
    earnedByCategory[catId] = (earnedByCategory[catId] ?? 0) + credits;
    passedCountByCategory[catId] = (passedCountByCategory[catId] ?? 0) + 1;
  }

  return categories.map((cat) => {
    const key = cat._id.toString();
    const total = categoryMap[key]?.total ?? 0;
    const totalCredits = categoryMap[key]?.totalCredits ?? 0;
    const passed = passedCountByCategory[key] ?? 0;
    const earned = earnedByCategory[key] ?? 0;
    return {
      category: cat,
      totalQuizzes: total,
      passedByMe: passed,
      totalCredits,
      earnedByMe: earned,
      progressPct: total > 0 ? passed / total : 0,
    };
  });
}

export async function listByCategory(slug, userId) {
  const category = await QuizCategory.findOne({ slug }).lean();
  if (!category) throw new Error("CATEGORY_NOT_FOUND");

  const quizzes = await Quiz.find({ categoryId: category._id })
    .select("title passThreshold creditsReward sortOrder questions")
    .sort({ sortOrder: 1 })
    .lean();

  const passedAttempts = await QuizAttempt.find({
    userId,
    quizId: { $in: quizzes.map((q) => q._id) },
    passed: true,
  })
    .sort({ createdAt: 1 })
    .lean();

  const passedMap = {};
  for (const a of passedAttempts) {
    const key = a.quizId.toString();
    if (!passedMap[key]) passedMap[key] = a.createdAt;
  }

  return quizzes.map((q) => ({
    quiz: {
      id: q._id,
      title: q.title,
      totalQuestions: q.questions?.length ?? 0,
      creditsReward: q.creditsReward,
    },
    passedByMe: !!passedMap[q._id.toString()],
    completedAt: passedMap[q._id.toString()] ?? null,
  }));
}

/**
 * Restituisce il prossimo quiz non superato dall'utente per la categoria indicata.
 * Usato dalla FormazioneScreen mobile per saltare la lista intermedia (mockup
 * "Continua →" porta direttamente al prossimo quiz aperto).
 *
 * @returns { id, title, totalQuestions, creditsReward, allCompleted: false }
 *          oppure { allCompleted: true, id: null } se l'utente ha già superato tutti
 *          i quiz della categoria.
 */
export async function getNextQuizForCategory(slug, userId) {
  const category = await QuizCategory.findOne({ slug }).lean();
  if (!category) throw new Error("CATEGORY_NOT_FOUND");

  const quizzes = await Quiz.find({ categoryId: category._id })
    .select("title creditsReward sortOrder questions")
    .sort({ sortOrder: 1 })
    .lean();
  if (quizzes.length === 0) return { allCompleted: true, id: null };

  const passedIds = new Set(
    (
      await QuizAttempt.find({
        userId,
        quizId: { $in: quizzes.map((q) => q._id) },
        passed: true,
      })
        .select("quizId")
        .lean()
    ).map((a) => a.quizId.toString()),
  );

  const next = quizzes.find((q) => !passedIds.has(q._id.toString()));
  if (!next) return { allCompleted: true, id: null };

  return {
    id: next._id,
    title: next.title,
    totalQuestions: next.questions?.length ?? 0,
    creditsReward: next.creditsReward,
    allCompleted: false,
  };
}

export async function getQuizForClient(quizId, userId) {
  const quiz = await Quiz.findById(quizId)
    .select("title description questions categoryId creditsReward")
    .populate("categoryId", "name slug color")
    .lean();
  if (!quiz) throw new Error("QUIZ_NOT_FOUND");

  // Già superato? La UI mostra un banner "non riceverai crediti aggiuntivi"
  // così l'utente sa che sta solo ripassando il materiale.
  const alreadyPassed = userId
    ? !!(await QuizAttempt.findOne({ userId, quizId, passed: true })
        .select("_id")
        .lean())
    : false;

  // Strip correctIndex + explanation — mai esposti prima del submit
  return {
    id: quiz._id,
    title: quiz.title,
    description: quiz.description,
    category: quiz.categoryId,
    questions: quiz.questions.map((q) => ({
      id: q._id,
      text: q.text,
      choices: q.choices,
    })),
    creditsReward: quiz.creditsReward,
    alreadyPassed,
  };
}

export async function submitQuiz(quizId, userId, answers) {
  const quiz = await Quiz.findById(quizId).lean();
  if (!quiz) throw new Error("QUIZ_NOT_FOUND");

  const questionMap = {};
  for (const q of quiz.questions) {
    questionMap[q._id.toString()] = q;
  }

  let correctCount = 0;
  const seen = new Set();
  const breakdown = answers
    .filter((a) => {
      const id = a.questionId?.toString();
      if (!id || seen.has(id)) return false;
      seen.add(id);
      return true;
    })
    .map((a) => {
      const q = questionMap[a.questionId.toString()];
      if (!q) return null;
      const isCorrect = q.correctIndex === a.choiceIndex;
      if (isCorrect) correctCount++;
      return {
        questionId: q._id,
        choiceIndex: a.choiceIndex,
        isCorrect,
        correctIndex: q.correctIndex,
        explanation: q.explanation,
      };
    })
    .filter(Boolean);

  const totalQuestions = quiz.questions.length;
  const score = totalQuestions > 0 ? correctCount / totalQuestions : 0;
  const passed = score >= (quiz.passThreshold ?? 0.7);

  // ── Idempotency anti race-condition (fix audit 2026-05) ──────────────────
  // PRIMA: due submit concorrenti potevano entrambi vedere alreadyPassed=null
  // e ricevere CREDITI DOPPI. La findOne+create non era atomica.
  //
  // ORA: $addToSet su Hiker.rewardedQuizzes è atomico. Solo la PRIMA submit
  // riuscita "claima" il reward (modifiedCount === 1). Le concorrenti vedono
  // l'ObjectId già nell'array e fanno no-op (modifiedCount === 0).
  // Side benefit: il check è veloce (un solo round-trip al DB).
  let creditsAwarded = 0;
  if (passed) {
    const claim = await Hiker.updateOne(
      { _id: userId, rewardedQuizzes: { $ne: quiz._id } },
      { $addToSet: { rewardedQuizzes: quiz._id } },
    );
    if (claim.modifiedCount === 1) {
      creditsAwarded = quiz.creditsReward;
    }
  }

  const attempt = await QuizAttempt.create({
    userId,
    quizId,
    answers,
    correctCount,
    totalQuestions,
    passed,
    creditsAwarded,
  });

  let newTotalCredits = null;
  if (creditsAwarded > 0) {
    await addCredits({
      userId,
      amount: creditsAwarded,
      source: "quiz",
      refId: quiz._id,
      refKind: "Quiz",
    });
    const user = await Hiker.findById(userId).select("socialCredits").lean();
    newTotalCredits = user?.socialCredits ?? null;
  }

  // Badge evaluation post-submit. Anche se non sono stati assegnati crediti
  // (es. quiz già passato), può comunque sbloccare category_champion.
  evaluateAllBadges(userId).catch((err) => {
    console.error("[quizService] badge eval fallita:", err.message);
  });

  return {
    attemptId: attempt._id,
    score,
    correctCount,
    totalQuestions,
    passed,
    creditsAwarded,
    breakdown,
    newTotalCredits,
  };
}
