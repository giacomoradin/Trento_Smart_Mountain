import mongoose from "mongoose";
import { readFileSync } from "fs";
import { fileURLToPath } from "url";
import { dirname, join } from "path";
import dotenv from "dotenv";

dotenv.config();

const __dirname = dirname(fileURLToPath(import.meta.url));
const totems = JSON.parse(readFileSync(join(__dirname, "totems.json"), "utf8"));

async function seed() {
  // Accetta sia MONGODB_URI (convenzione TSM) che MONGO_URI come fallback.
  const uri = process.env.MONGODB_URI || process.env.MONGO_URI;
  if (!uri) { console.error("MONGODB_URI (o MONGO_URI) non impostato."); process.exit(1); }

  await mongoose.connect(uri);
  console.log("Connesso a MongoDB.");

  const NfcTotem = (await import("../src/models/nfcTotem.js")).default;

  for (const t of totems) {
    const { lon, lat, ...rest } = t;
    await NfcTotem.findOneAndUpdate(
      { tagId: t.tagId },
      { ...rest, location: { type: "Point", coordinates: [lon, lat] } },
      { upsert: true, new: true },
    );
    console.log(`Totem: ${t.name}`);
  }

  await mongoose.disconnect();
  console.log("Seed totem completato.");
}

seed().catch((err) => { console.error(err); process.exit(1); });
