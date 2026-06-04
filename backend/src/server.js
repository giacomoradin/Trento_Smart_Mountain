import "dotenv/config";
import mongoose from "mongoose";
import { readFileSync } from "fs";
import { fileURLToPath } from "url";
import { dirname, join } from "path";
import app from "./app.js"; // ← importa app.js invece di ricreare Express
import { seedLocations } from "./services/weatherService.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

/**
 * Auto-seed quiz categories + quizzes se il DB è vuoto.
 * Idempotente (upsert): sicuro da chiamare ad ogni avvio.
 * Evita di dover eseguire manualmente `node seed/seedQuizzes.js` su Render.
 */
async function autoSeedQuizzes() {
  const QuizCategory = (await import("./models/quizCategory.js")).default;
  const Quiz = (await import("./models/quiz.js")).default;

  const existingCount = await QuizCategory.countDocuments();
  if (existingCount > 0) {
    console.log(
      `[seed] Quiz già presenti (${existingCount} categorie) — skip auto-seed.`,
    );
    return;
  }

  console.log("[seed] Collezione quiz vuota — avvio auto-seed...");
  const data = JSON.parse(
    readFileSync(join(__dirname, "../seed/quizzes.json"), "utf8"),
  );

  const categoryIdBySlug = {};
  for (const cat of data.categories) {
    const result = await QuizCategory.findOneAndUpdate(
      { slug: cat.slug },
      cat,
      { upsert: true, new: true },
    );
    categoryIdBySlug[cat.slug] = result._id;
    console.log(`[seed]  Categoria: ${cat.name}`);
  }

  for (const q of data.quizzes) {
    const categoryId = categoryIdBySlug[q.categorySlug];
    if (!categoryId) {
      console.warn(`[seed]  Categoria non trovata: ${q.categorySlug}`);
      continue;
    }
    const { categorySlug, ...quizData } = q;
    await Quiz.findOneAndUpdate(
      { title: quizData.title, categoryId },
      { ...quizData, categoryId },
      { upsert: true, new: true },
    );
    console.log(`[seed]    Quiz: ${quizData.title}`);
  }
  console.log("[seed] Auto-seed completato.");
}

// Verifica le variabili di ambiente critiche all'avvio.
// In produzione blocca il boot se mancano; in dev avverte ma parte.
function assertEnvironment() {
  const required = ["JWT_SECRET"];
  const inProduction = process.env.NODE_ENV === "production";
  if (inProduction) {
    required.push(
      "MONGO_URI",
      "BASE_URL",
      "BREVO_API_KEY",
      "EMAIL_FROM_ADDRESS",
    );
  }

  const missing = required.filter(
    (k) => !process.env[k] || !process.env[k].trim(),
  );
  if (missing.length) {
    console.error(
      `[boot] FATAL: variabili di ambiente mancanti: ${missing.join(", ")}`,
    );
    console.error(`[boot] Vedi .env.example per la lista completa.`);
    process.exit(1);
  }

  // Detect default insicuri ovvi.
  const jwt = process.env.JWT_SECRET || "";
  if (
    jwt.length < 32 ||
    /^(secret|changeme|password|test|dev|default)$/i.test(jwt)
  ) {
    console.error(
      `[boot] FATAL: JWT_SECRET troppo debole (< 32 char o valore noto).`,
    );
    console.error(
      `[boot] Genera con: node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"`,
    );
    process.exit(1);
  }

  if (!inProduction && !process.env.BREVO_API_KEY) {
    console.warn(
      `[boot] WARN: BREVO_API_KEY non impostata. Le email (verify/reset) falliranno.`,
    );
  }
}

assertEnvironment();

// ── Stabilità: handler globali di processo ──────────────────────────────────
// Un rejection/eccezione non gestita altrimenti può lasciare il processo in uno
// stato incoerente (o, su Node recenti, terminarlo). Logghiamo sempre con stack
// per la diagnosi; su uncaughtException usciamo in modo controllato così il
// process manager (Render/PM2) riavvia pulito invece di restare "zombie".
process.on("unhandledRejection", (reason) => {
  console.error("[process] UNHANDLED REJECTION:", reason);
});
process.on("uncaughtException", (err) => {
  console.error("[process] UNCAUGHT EXCEPTION:", err);
  // Spegnimento controllato: chiudiamo la connessione DB poi usciamo.
  mongoose.connection.close(false).finally(() => process.exit(1));
});

const PORT = process.env.PORT || 3000;
const MONGO_URI =
  process.env.MONGO_URI || "mongodb://localhost:27017/trento_smart_mountain";

mongoose
  .connect(MONGO_URI)
  .then(async () => {
    console.log("Connected to MongoDB");
    await seedLocations();
    await autoSeedQuizzes();
    const server = app.listen(PORT, () =>
      console.log(`Server running on port ${PORT}`),
    );

    // ── Graceful shutdown ────────────────────────────────────────────────
    // Render (e la maggior parte dei PaaS) invia SIGTERM ad ogni deploy/restart.
    // Senza handler il processo viene ucciso a metà: richieste in volo troncate e
    // connessione Mongo non chiusa. Qui smettiamo di accettare nuove connessioni,
    // lasciamo finire quelle in corso, poi chiudiamo il DB ed usciamo pulito.
    const shutdown = (signal) => {
      console.log(`[process] ${signal} ricevuto — shutdown controllato...`);
      server.close(() => {
        mongoose.connection.close(false).finally(() => {
          console.log("[process] shutdown completato.");
          process.exit(0);
        });
      });
      // Failsafe: se qualcosa resta appeso, forziamo l'uscita dopo 10s.
      setTimeout(() => process.exit(1), 10_000).unref();
    };
    process.on("SIGTERM", () => shutdown("SIGTERM"));
    process.on("SIGINT", () => shutdown("SIGINT"));
  })
  .catch((error) => {
    console.error("MongoDB connection error:", error);
    process.exit(1);
  });
