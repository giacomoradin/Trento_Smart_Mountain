// Header HTTP di sicurezza, sanitization input e CORS.
import helmet from "helmet";
import mongoSanitize from "express-mongo-sanitize";
import hpp from "hpp";
import cors from "cors";

// CSP "permissiva" su unsafe-inline perché Swagger UI inietta JS inline.
// Per il resto delle API (JSON) i defaults vanno bene.
export const helmetMiddleware = helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'"],
      styleSrc: ["'self'", "'unsafe-inline'", "https:"],
      imgSrc: ["'self'", "data:", "https:"],
      frameAncestors: ["'none'"],
    },
  },
  // HSTS solo in produzione: in dev HTTP è ok e Render gestisce TLS in fronte.
  hsts: process.env.NODE_ENV === "production"
    ? { maxAge: 60 * 60 * 24 * 365, includeSubDomains: true }
    : false,
});

// Rimuove chiavi che iniziano con $ o contengono . dal body/params/query.
// Esempio: { "email": { "$ne": null } } → { "email": {} }
export const mongoSanitizeMiddleware = mongoSanitize({
  onSanitize: ({ req, key }) => {
    console.warn(`[security] operatore NoSQL rimosso da ${req.method} ${req.path}: key=${key}`);
  },
});

// Riduce array da query string a singolo valore. Nessuna route legittima usa
// array params al momento; se serve in futuro aggiungere a whitelist.
export const hppMiddleware = hpp({ whitelist: [] });

// CORS: in dev permissivo, in prod allow-list via ALLOWED_ORIGINS (CSV).
// L'app mobile (OkHttp/Retrofit) non manda Origin → passa sempre.
const allowedOrigins = (process.env.ALLOWED_ORIGINS || "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);

export const corsOptions = cors({
  origin: (origin, callback) => {
    if (!origin) return callback(null, true);
    if (process.env.NODE_ENV !== "production") return callback(null, true);
    if (allowedOrigins.includes(origin)) return callback(null, true);
    return callback(new Error(`CORS: origin "${origin}" non autorizzata.`));
  },
  methods: ["GET", "POST", "PATCH", "DELETE", "OPTIONS"],
  allowedHeaders: ["Content-Type", "Authorization"],
  exposedHeaders: ["RateLimit-Remaining", "RateLimit-Reset", "Retry-After"],
  maxAge: 86400,
  credentials: false,
});

// 100 KB è abbondante per gpxStats (elevationProfile = ~50 numeri = ~1KB) e
// blocca payload abnormi (JSON bomb).
export const requestSizeLimit = "100kb";
