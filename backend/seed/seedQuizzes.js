/**
 * Seed iniziale quiz e categorie.
 * Esecuzione: node backend/seed/seedQuizzes.js
 * Richiede MONGODB_URI (o MONGO_URI come fallback) in .env o come variabile d'ambiente.
 */
import mongoose from "mongoose";
import { readFileSync } from "fs";
import { fileURLToPath } from "url";
import { dirname, join } from "path";
import dotenv from "dotenv";

dotenv.config();

const __dirname = dirname(fileURLToPath(import.meta.url));
const data = JSON.parse(readFileSync(join(__dirname, "quizzes.json"), "utf8"));

async function seed() {
  // Accetta MONGODB_URI (convenzione TSM) o MONGO_URI (alias comune in altri progetti).
  // Tornare utile a chi clona il repo con la propria .env senza dover indovinare il nome.
  const uri = process.env.MONGODB_URI || process.env.MONGO_URI;
  if (!uri) {
    console.error("MONGODB_URI (o MONGO_URI) non impostato.");
    process.exit(1);
  }

  await mongoose.connect(uri);
  console.log("Connesso a MongoDB.");

  const QuizCategory = (await import("../src/models/quizCategory.js")).default;
  const Quiz = (await import("../src/models/quiz.js")).default;

  // Upsert categorie
  const categoryIdBySlug = {};
  for (const cat of data.categories) {
    const result = await QuizCategory.findOneAndUpdate(
      { slug: cat.slug },
      cat,
      { upsert: true, new: true },
    );
    categoryIdBySlug[cat.slug] = result._id;
    console.log(`Categoria: ${cat.name}`);
  }

  // Upsert quiz
  for (const q of data.quizzes) {
    const categoryId = categoryIdBySlug[q.categorySlug];
    if (!categoryId) {
      console.warn(`Categoria non trovata: ${q.categorySlug}`);
      continue;
    }
    const { categorySlug, ...quizData } = q;
    await Quiz.findOneAndUpdate(
      { title: quizData.title, categoryId },
      { ...quizData, categoryId },
      { upsert: true, new: true },
    );
    console.log(`  Quiz: ${quizData.title}`);
  }

  await mongoose.disconnect();
  console.log("Seed completato.");
}

seed().catch((err) => {
  console.error("Errore seed:", err);
  process.exit(1);
});
