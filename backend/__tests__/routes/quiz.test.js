import request from "supertest";
import app from "../../src/app.js";
import QuizCategory from "../../src/models/quizCategory.js";
import Quiz from "../../src/models/quiz.js";
import { createTestHiker } from "../helpers/authHelper.js";

/**
 * Test suite Quiz (formazione) — servizio finora a copertura 0%.
 *
 * Copre la logica critica di `quizService`:
 *   - GET /:id NON espone correctIndex/explanation (no leak delle risposte)
 *   - scoring e soglia di superamento
 *   - **anti doppio-credito**: ri-superare un quiz già passato non riaccredita
 *   - "prossimo quiz" della categoria + 404 su quiz inesistente
 */
describe("Quiz Routes", () => {
  const auth = (t) => ({ Authorization: `Bearer ${t}` });

  async function seedQuiz({ slug = "sicurezza", creditsReward = 25 } = {}) {
    const category = await QuizCategory.create({
      slug,
      name: "Sicurezza in montagna",
      color: "#E53935",
    });
    const quiz = await Quiz.create({
      categoryId: category._id,
      title: "Nodi base",
      creditsReward,
      questions: [
        { text: "Domanda 1?", choices: ["a", "b", "c", "d"], correctIndex: 0, explanation: "spiegazione 1" },
        { text: "Domanda 2?", choices: ["a", "b", "c", "d"], correctIndex: 2, explanation: "spiegazione 2" },
      ],
    });
    return { category, quiz };
  }

  const allCorrect = (quiz) => [
    { questionId: quiz.questions[0]._id.toString(), choiceIndex: 0 },
    { questionId: quiz.questions[1]._id.toString(), choiceIndex: 2 },
  ];

  test("GET /:id non espone correctIndex/explanation (no leak delle risposte)", async () => {
    const { token } = await createTestHiker({ email: "qz1@test.com", username: "qz1" });
    const { quiz } = await seedQuiz();
    const res = await request(app).get(`/api/v1/quiz/${quiz._id}`).set(auth(token));
    expect(res.status).toBe(200);
    expect(res.body.questions).toHaveLength(2);
    expect(res.body.questions[0].text).toBeDefined();
    expect(res.body.questions[0].choices).toHaveLength(4);
    expect(res.body.questions[0].correctIndex).toBeUndefined();
    expect(res.body.questions[0].explanation).toBeUndefined();
    expect(res.body.alreadyPassed).toBe(false);
  });

  test("submit tutte corrette → passed, score 1, crediti assegnati + breakdown svela le soluzioni", async () => {
    const { token } = await createTestHiker({ email: "qz2@test.com", username: "qz2" });
    const { quiz } = await seedQuiz({ creditsReward: 30 });
    const res = await request(app)
      .post(`/api/v1/quiz/${quiz._id}/submit`)
      .set(auth(token))
      .send({ answers: allCorrect(quiz) });
    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      passed: true,
      score: 1,
      correctCount: 2,
      totalQuestions: 2,
      creditsAwarded: 30,
    });
    expect(res.body.breakdown[0]).toMatchObject({ isCorrect: true, correctIndex: 0 });
    expect(res.body.breakdown[0].explanation).toBe("spiegazione 1");
  });

  test("submit sotto soglia → passed false, niente crediti", async () => {
    const { token } = await createTestHiker({ email: "qz3@test.com", username: "qz3" });
    const { quiz } = await seedQuiz();
    const answers = [
      { questionId: quiz.questions[0]._id.toString(), choiceIndex: 0 }, // corretta
      { questionId: quiz.questions[1]._id.toString(), choiceIndex: 0 }, // errata
    ];
    const res = await request(app)
      .post(`/api/v1/quiz/${quiz._id}/submit`)
      .set(auth(token))
      .send({ answers });
    expect(res.status).toBe(200);
    expect(res.body.passed).toBe(false);
    expect(res.body.score).toBeCloseTo(0.5);
    expect(res.body.creditsAwarded).toBe(0);
  });

  test("anti doppio-credito: ri-superare un quiz già passato non riaccredita", async () => {
    const { token } = await createTestHiker({ email: "qz4@test.com", username: "qz4" });
    const { quiz } = await seedQuiz({ creditsReward: 40 });
    const first = await request(app)
      .post(`/api/v1/quiz/${quiz._id}/submit`)
      .set(auth(token))
      .send({ answers: allCorrect(quiz) });
    expect(first.body.creditsAwarded).toBe(40);

    const second = await request(app)
      .post(`/api/v1/quiz/${quiz._id}/submit`)
      .set(auth(token))
      .send({ answers: allCorrect(quiz) });
    expect(second.body.passed).toBe(true);
    expect(second.body.creditsAwarded).toBe(0);
  });

  test("GET /categories/:slug/next: prossimo quiz, poi allCompleted dopo averlo passato", async () => {
    const { token } = await createTestHiker({ email: "qz5@test.com", username: "qz5" });
    const { quiz } = await seedQuiz({ slug: "meteo" });

    const before = await request(app).get("/api/v1/quiz/categories/meteo/next").set(auth(token));
    expect(before.status).toBe(200);
    expect(before.body.allCompleted).toBe(false);
    expect(String(before.body.id)).toBe(String(quiz._id));

    await request(app)
      .post(`/api/v1/quiz/${quiz._id}/submit`)
      .set(auth(token))
      .send({ answers: allCorrect(quiz) });

    const after = await request(app).get("/api/v1/quiz/categories/meteo/next").set(auth(token));
    expect(after.body.allCompleted).toBe(true);
    expect(after.body.id).toBeNull();
  });

  test("quiz inesistente → 404", async () => {
    const { token } = await createTestHiker({ email: "qz6@test.com", username: "qz6" });
    const res = await request(app)
      .get("/api/v1/quiz/507f1f77bcf86cd799439011")
      .set(auth(token));
    expect(res.status).toBe(404);
  });
});
