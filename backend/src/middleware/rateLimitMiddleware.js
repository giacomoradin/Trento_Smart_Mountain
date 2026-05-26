// Rate limiting su due livelli: globale per IP + per utente autenticato.
// Storage in-memory (single-instance); per multi-instance servirebbe uno store Redis.
import rateLimit from "express-rate-limit";

function rateLimitHandler(req, res, _next, options) {
  const retryAfterSec = Math.ceil(options.windowMs / 1000);
  res.status(options.statusCode).json({
    error: "Troppe richieste. Riprova fra qualche istante.",
    retryAfter: retryAfterSec,
  });
}

// Per utenti autenticati usa userId come chiave (gli utenti loggati spesso condividono IP:
// NAT mobile, WiFi pubblico). Per chi non è autenticato fallback su IP.
function userOrIpKey(req) {
  if (req.user?.userId) return `user:${req.user.userId}`;
  return req.ip;
}

// In ambiente test (Jest) bypassa SEMPRE i rate limit per evitare flakiness
// causata dall'accumulo di richieste tra test multipli nella stessa suite.
const skipInTest = () => process.env.NODE_ENV === "test";

// Limite globale generoso. L'app fa polling continuo durante il tracking
// (posizione team, meteo) quindi 1200 req/h è una soglia ragionevole.
export const globalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 300,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  handler: rateLimitHandler,
  skip: skipInTest,
});

// Protezione contro credential stuffing. Solo i tentativi falliti contano —
// se la password è giusta non si blocca l'utente legittimo.
export const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  skipSuccessfulRequests: true,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  handler: rateLimitHandler,
  skip: skipInTest,
});

// Soglia più alta per permettere testing intensivo (es. delete -> recreate flow).
export const registerLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  max: 20,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  handler: rateLimitHandler,
  skip: skipInTest,
});

// Anche per controllo costi: ogni richiesta consuma quota SMTP Brevo.
export const passwordResetLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  max: 5,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  handler: rateLimitHandler,
  skip: skipInTest,
});

// Più alto perché l'app fa polling reale durante il tracking.
export const authenticatedLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 1000,
  keyGenerator: userOrIpKey,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  handler: rateLimitHandler,
  skip: skipInTest,
});

// Limite separato per le scritture (più costose lato DB).
export const writeLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  keyGenerator: userOrIpKey,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  handler: rateLimitHandler,
  skip: (req) =>
    skipInTest()
    || req.method === "GET" || req.method === "HEAD" || req.method === "OPTIONS",
});
