/**
 * Migration: HikeSession.meetingDate da String → Date
 *
 * Contesto: prima del fix audit 2026-05, `meetingDate` era salvato come String
 * (formato "YYYY-MM-DD"). Questo impediva sort cronologici efficienti lato DB
 * e ordinamenti corretti se mai venissero salvati altri formati. Il nuovo
 * schema usa `type: Date` con setter che parse "YYYY-MM-DD" → Date UTC midnight.
 *
 * Cosa fa lo script:
 *  1. Si connette a MongoDB (MONGODB_URI o MONGO_URI).
 *  2. Trova tutti i documenti `hikesessions` in cui `meetingDate` è una stringa
 *     o un BSON tipo 2 (String) — escludendo quelli già Date (tipo 9).
 *  3. Per ciascuno, parse "YYYY-MM-DD" → Date UTC midnight e aggiorna in place.
 *  4. Documenti con valori non parsabili vengono loggati e SALTATI (nessun
 *     dato perso — l'admin valuta manualmente).
 *
 * Idempotente: se rieseguito, salta i documenti già migrati (Date type).
 *
 * Uso:
 *   cd backend
 *   MONGODB_URI="mongodb+srv://..." node migrations/2026-05-26-meetingDate-string-to-date.js
 *
 * Output: report finale con counter (migrated / skipped / errors).
 */
import mongoose from "mongoose";
import dotenv from "dotenv";

dotenv.config();

const IT_MONTHS = {
  gen: 0, feb: 1, mar: 2, apr: 3, mag: 4, giu: 5,
  lug: 6, ago: 7, set: 8, ott: 9, nov: 10, dic: 11,
};

function parseYmd(value) {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  if (!trimmed) return null;

  // "YYYY-MM-DD"
  const isoMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(trimmed);
  if (isoMatch) {
    return new Date(Date.UTC(+isoMatch[1], +isoMatch[2] - 1, +isoMatch[3]));
  }

  // "DD Mmm YYYY" italiano (es. "24 Mag 2026", "27 mag 2026")
  const itMatch = /^(\d{1,2})\s+([A-Za-z]{3})\s+(\d{4})$/.exec(trimmed);
  if (itMatch) {
    const month = IT_MONTHS[itMatch[2].toLowerCase()];
    if (month !== undefined) {
      return new Date(Date.UTC(+itMatch[3], month, +itMatch[1]));
    }
  }

  // Fallback: ISO/RFC generici riconosciuti da V8
  const d = new Date(trimmed);
  return Number.isNaN(d.getTime()) ? null : d;
}

async function migrate() {
  const uri = process.env.MONGODB_URI || process.env.MONGO_URI;
  if (!uri) {
    console.error("MONGODB_URI (o MONGO_URI) non impostato.");
    process.exit(1);
  }

  await mongoose.connect(uri);
  console.log("[migration] Connesso a MongoDB.");

  // Bypass mongoose model: lavoriamo sulla collection raw così possiamo
  // aggiornare campi di tipo eterogeneo senza che Mongoose validi prima.
  const coll = mongoose.connection.collection("hikesessions");

  // $type: 2 = String in BSON; $type: 9 = Date (skip-amo).
  const cursor = coll.find({ meetingDate: { $type: 2 } });

  let migrated = 0;
  let skipped = 0;
  let errors = 0;
  const errorSamples = [];

  for await (const doc of cursor) {
    const original = doc.meetingDate;
    const parsed = parseYmd(original);
    if (!parsed) {
      errors++;
      if (errorSamples.length < 10) {
        errorSamples.push({ _id: doc._id.toString(), meetingDate: original });
      }
      continue;
    }
    try {
      await coll.updateOne(
        { _id: doc._id },
        { $set: { meetingDate: parsed } },
      );
      migrated++;
    } catch (err) {
      errors++;
      if (errorSamples.length < 10) {
        errorSamples.push({ _id: doc._id.toString(), error: err.message });
      }
    }
  }

  // Documenti senza meetingDate o già Date — riportati per completezza.
  const total = await coll.countDocuments({});
  const alreadyDate = await coll.countDocuments({ meetingDate: { $type: 9 } });
  const nullOrMissing = await coll.countDocuments({
    $or: [
      { meetingDate: null },
      { meetingDate: { $exists: false } },
      { meetingDate: "" },
    ],
  });
  skipped = alreadyDate + nullOrMissing;

  console.log("\n[migration] Riepilogo:");
  console.log(`  Totale documenti hikesessions: ${total}`);
  console.log(`  Migrati (String → Date):       ${migrated}`);
  console.log(`  Già Date o null/missing:       ${skipped}`);
  console.log(`  Errori (saltati):              ${errors}`);
  if (errorSamples.length) {
    console.log("\n[migration] Sample errori:");
    for (const s of errorSamples) console.log(`  ${JSON.stringify(s)}`);
  }

  await mongoose.disconnect();
  console.log("\n[migration] Completata.");
}

migrate().catch((err) => {
  console.error("[migration] Fatale:", err);
  process.exit(1);
});
