import "dotenv/config";
import mongoose from "mongoose";
import app from "./app.js"; // ← importa app.js invece di ricreare Express
import { seedLocations } from './services/weatherService.js';

// Verifica le variabili di ambiente critiche all'avvio.
// In produzione blocca il boot se mancano; in dev avverte ma parte.
function assertEnvironment() {
  const required = ["JWT_SECRET"];
  const inProduction = process.env.NODE_ENV === "production";
  if (inProduction) {
    required.push("MONGO_URI", "BASE_URL", "BREVO_API_KEY", "EMAIL_FROM_ADDRESS");
  }

  const missing = required.filter((k) => !process.env[k] || !process.env[k].trim());
  if (missing.length) {
    console.error(`[boot] FATAL: variabili di ambiente mancanti: ${missing.join(", ")}`);
    console.error(`[boot] Vedi .env.example per la lista completa.`);
    process.exit(1);
  }

  // Detect default insicuri ovvi.
  const jwt = process.env.JWT_SECRET || "";
  if (jwt.length < 32 || /^(secret|changeme|password|test|dev|default)$/i.test(jwt)) {
    console.error(`[boot] FATAL: JWT_SECRET troppo debole (< 32 char o valore noto).`);
    console.error(`[boot] Genera con: node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"`);
    process.exit(1);
  }

  if (!inProduction && !process.env.BREVO_API_KEY) {
    console.warn(`[boot] WARN: BREVO_API_KEY non impostata. Le email (verify/reset) falliranno.`);
  }
}

assertEnvironment();

const PORT = process.env.PORT || 3000;
const MONGO_URI =
  process.env.MONGO_URI || "mongodb://localhost:27017/trento_smart_mountain";

mongoose
  .connect(MONGO_URI)
  .then(async() => {
    await seedLocations();
    console.log("Connected to MongoDB");
    app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
  })
  .catch((error) => {
    console.error("MongoDB connection error:", error);
    process.exit(1);
  });
