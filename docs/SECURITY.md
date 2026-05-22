# Trento Smart Mountain — Security Architecture

Documento di riferimento per le scelte di sicurezza del backend Express e del client Android. Allineato a **OWASP Top 10 (2021)** e **OWASP API Security Top 10 (2023)**.

---

## 1. Threat model

### Asset principali

| Asset                                    | Sensibilità | Controlli applicati                                                           |
| ---------------------------------------- | ----------- | ----------------------------------------------------------------------------- |
| Credenziali utente (email + bcrypt hash) | **Alta**    | bcrypt cost 10, JWT secret 256-bit, password reset token one-time scadenza 1h |
| JWT token                                | **Alta**    | HMAC SHA-256, scadenza 1d configurabile, `Authorization: Bearer` (mai cookie) |
| Tracciato GPS (lat/lon/alt)              | Media       | TLS in transito, isolamento per `userId` su tutte le route Activity           |
| Email rifugio/utente                     | Media       | Validazione formato, length limit 254, normalizzazione lowercase              |
| Brevo API key (SMTP)                     | **Critica** | Solo server, `BREVO_API_KEY` env var, MAI in build mobile                     |

### Attori e capability

- **Anonimo**: può solo POST `/auth/login`, POST `/auth/register/*`, GET `/auth/verify/:token`, GET `/api-docs`.
- **Hiker autenticato (JWT)**: CRUD sulle proprie sessioni e attività; lettura/scrittura limitata da `userId`.
- **Refuge autenticato**: come hiker + endpoint refuge-specific (TBD).
- **Admin autenticato**: bypass autorizzazione, accesso `/admin/*` (gestione utenti).
- **Network attacker (MITM)**: TLS termina su Render; certificati validi gestiti dalla piattaforma.

### Trust boundaries

```
[Mobile App] ──TLS──> [Render Edge / TLS Term.] ──HTTP intranet──> [Express] ──Mongoose──> [MongoDB Atlas]
                                                          │
                                                          └──HTTPS──> [Brevo SMTP API] (server-side only)
                                                          └──HTTPS──> [meteo.report / TINIA] (server-side only)
```

Il client mobile NON parla mai direttamente con Brevo, MongoDB o le API meteo. Tutto passa dal backend.

---

## 2. Controlli OWASP applicati

### A01:2021 — Broken Access Control

- **Autenticazione**: middleware `authenticate` (jwt) protegge tutte le route `/api/v1/*`, `/hikers/:id`, `/refuges/:id`.
- **Autorizzazione**: ogni service verifica `userId === req.user.userId` prima di operare su risorse (vedi `activityService.deleteActivity`, `hikeSessionService.completeSession`).
- **Discriminator User**: hierarchy `User { Hiker | Refuge | Admin }` — non si possono escalare ruoli via API.
- **Insecure Direct Object Reference**: gli endpoint `/sessions/:id` non rivelano sessioni di cui l'utente non è creator o partecipante (filtrate via `$or: [{creatorId: userId}, {"participants.userId": userId}]`).

### A02:2021 — Cryptographic Failures

- **Password storage**: `bcrypt(password, 10 rounds)` su tutti i modelli (Hiker, Refuge, Admin).
- **JWT**: HMAC SHA-256 con secret >= 256 bit (controllo fail-fast `assertEnvironment` in `server.js`).
- **TLS**: gestito da Render (HSTS abilitato in produzione tramite helmet).
- **Token reset password**: random 32 bytes hex, scadenza 1h, monouso (eliminato dopo conferma).

### A03:2021 — Injection

- **NoSQL injection**: `express-mongo-sanitize` rimuove operatori `$` e chiavi con `.` da `req.body`, `req.params`, `req.query` (vedi `securityMiddleware.js`).
- **Schema validation**: Joi rifiuta campi non dichiarati (`.unknown(false)` di default) → niente mass-assignment.
- **Mongoose strict mode**: di default true; assegnamenti a campi sconosciuti vengono droppati silenziosamente.
- **HTML/XSS**: l'API è JSON-only eccetto `/auth/reset-password/:token` (form HTML). I valori dinamici sono escape-d.

### A04:2021 — Insecure Design

- **Rate limiting differenziato**: globale (300/15min), login (10/15min skipSuccess), register (5/h), forgot-password (5/h), authenticated (1000/15min), write-ops (200/15min).
- **Soglie sensate**: il limit di register a 5/h previene account farming; login 10/15min lascia margine all'utente onesto ma blocca brute force.
- **Body size limit**: 100 KB → DoS bomba JSON impedita.

### A05:2021 — Security Misconfiguration

- **helmet**: header HTTP di sicurezza (CSP, X-Frame-Options, Referrer-Policy, X-Content-Type-Options).
- **HSTS**: abilitato solo in produzione (NODE_ENV=production) per non rompere dev su localhost.
- **CORS**: allow-list via `ALLOWED_ORIGINS` env. Dev = permissivo; prod = strict.
- **Fail-fast su env mancanti**: il server NON parte se `JWT_SECRET` ha < 32 char o se è uno tra `secret/changeme/password/test/dev/default`.
- **Swagger UI**: pubblico ma non rivela credenziali (solo schema endpoints).

### A06:2021 — Vulnerable and Outdated Components

- **Lockfile**: `package-lock.json` committato; rebuild deterministico.
- **Dipendenze critiche**: jsonwebtoken 9.x (cve-2022-23529 fixed), bcrypt 6.x, express 4.22+, mongoose 8.x.
- **Audit**: `npm audit` come gate manuale prima di ogni release (TODO: integrazione CI).

### A07:2021 — Identification and Authentication Failures

- **Brute force**: rate limiter login + skip successful (gli errori contano, i success no).
- **Account enumeration**: forgot-password risponde sempre 200 anche se l'email non esiste — evita di rivelare l'esistenza dell'account.
- **Email verification**: account creato con `isEmailVerified=false`; login bloccato finché non confermato.
- **Token reset**: monouso, scadenza 1h, generato con `crypto.randomBytes(32)`.

### A08:2021 — Software and Data Integrity Failures

- **JWT**: signing key persistente in env, non hardcoded. Rotazione manuale (TODO: jwks rotation).
- **Email links**: contengono URL absolute basato su `BASE_URL` env — niente reflection da header `Host` (Host header injection mitigato).

### A09:2021 — Security Logging and Monitoring Failures

- **Log strutturati**: stack trace solo in `NODE_ENV=development` (vedi `errorMiddleware.js`).
- **NoSQL sanitize log**: `mongoSanitizeMiddleware` logga ogni tentativo di iniezione operatore.
- **Rate limit hit**: 429 sono visibili nei log di Express (status code).
- **TODO produzione**: integrare Sentry o un log aggregator per alerting.

### A10:2021 — Server-Side Request Forgery (SSRF)

- **Weather upstream**: `weatherService.js` chiama solo URL hardcoded (`meteo.report`, `gitlab.com/tinia-euregio`). Nessun URL costruito da input utente → no SSRF surface.
- **No fetch utente-controllato**: il backend non ha endpoint che accettano URL dall'utente.

---

## 3. API Security Top 10 (2023)

| Item                                                       | Controllo                                                                              |
| ---------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| **API1** — Broken Object Level Authorization               | Service-level `userId === req.user.userId` check; sessions filtrate per partecipazione |
| **API2** — Broken Authentication                           | bcrypt + JWT + email verify + rate limit                                               |
| **API3** — Broken Object Property Level Auth               | Joi `.unknown(false)` + Mongoose strict mode; mass-assignment bloccato                 |
| **API4** — Unrestricted Resource Consumption               | Rate limit a 5 livelli + body size 100KB                                               |
| **API5** — Broken Function Level Authorization             | Routes separate `/admin/*` + middleware role check (TODO discriminator-based)          |
| **API6** — Unrestricted Access to Sensitive Business Flows | Rate limit dedicato su register/forgot/reset password                                  |
| **API7** — SSRF                                            | Upstream URLs hardcoded, no user-controlled fetch                                      |
| **API8** — Security Misconfiguration                       | helmet + CORS allow-list + fail-fast env                                               |
| **API9** — Improper Inventory Management                   | Swagger UI sempre aggiornato (script `npm run swagger`)                                |
| **API10** — Unsafe Consumption of 3rd Party APIs           | Timeout 10-15s su tutte le fetch upstream; status code check; AbortSignal              |

---

## 4. Access Control Matrix

Matrice esplicita ruolo × risorsa × azione. Aggiornare ad ogni modifica di route.

Legenda:

- ✅ = consentito
- ❌ = vietato (403)
- 👤 = consentito solo se _owner_ della risorsa
- 🛡 = consentito solo se _partecipante_ della sessione
- 🅿 = pubblico (anche anonimo)

| Risorsa / Azione                      | Anon |      Hiker      |     Refuge      | Admin |
| ------------------------------------- | :--: | :-------------: | :-------------: | :---: |
| `POST /auth/login`                    |  🅿  |       🅿        |       🅿        |  🅿   |
| `POST /auth/register/hiker`           |  🅿  |       🅿        |       🅿        |  🅿   |
| `POST /auth/register/refuge`          |  🅿  |       🅿        |       🅿        |  🅿   |
| `GET /auth/verify/:token`             |  🅿  |       🅿        |       🅿        |  🅿   |
| `POST /auth/forgot-password`          |  🅿  |       🅿        |       🅿        |  🅿   |
| `POST /auth/reset-password/:token`    |  🅿  |       🅿        |       🅿        |  🅿   |
| `GET /hikers/:id`                     |  ❌  |       👤        |       ❌        |  ✅   |
| `GET /refuges/:id`                    |  ❌  |       ✅        |       👤        |  ✅   |
| `GET /users/:id` (legacy)             |  ❌  |       ✅        |       ✅        |  ✅   |
| `POST /api/v1/sessions`               |  ❌  |       ✅        |       ✅        |  ✅   |
| `GET /api/v1/sessions/my`             |  ❌  |       ✅        |       ✅        |  ✅   |
| `GET /api/v1/sessions/:id`            |  ❌  |       🛡        |       🛡        |  ✅   |
| `PATCH /api/v1/sessions/:id`          |  ❌  |  👤 (creator)   |  👤 (creator)   |  ✅   |
| `PATCH /api/v1/sessions/:id/status`   |  ❌  |  👤 (creator)   |  👤 (creator)   |  ✅   |
| `PATCH /api/v1/sessions/:id/complete` |  ❌  |       🛡        |       🛡        |  ✅   |
| `POST /api/v1/sessions/:id/leave`     |  ❌  | 🛡 (no creator) | 🛡 (no creator) |  ✅   |
| `POST /api/v1/sessions/join`          |  ❌  |       ✅        |       ✅        |  ✅   |
| `DELETE /api/v1/sessions/:id`         |  ❌  |  👤 (creator)   |  👤 (creator)   |  ✅   |
| `GET /api/v1/sessions/stats`          |  ❌  |  ✅ (only own)  |  ✅ (only own)  |  ✅   |
| `POST /api/v1/activities`             |  ❌  |       ✅        |       ✅        |  ✅   |
| `GET /api/v1/activities`              |  ❌  |  ✅ (only own)  |  ✅ (only own)  |  ✅   |
| `GET /api/v1/activities/:id`          |  ❌  |       👤        |       👤        |  ✅   |
| `DELETE /api/v1/activities/:id`       |  ❌  |       👤        |       👤        |  ✅   |
| `GET /weather/locations/nearby`       |  ❌  |       ✅        |       ✅        |  ✅   |
| `GET /weather/locations/search`       |  ❌  |       ✅        |       ✅        |  ✅   |
| `GET /weather/forecast/:externalId`   |  ❌  |       ✅        |       ✅        |  ✅   |
| `/admin/*`                            |  ❌  |       ❌        |       ❌        |  ✅   |
| `GET /api-docs`                       |  🅿  |       🅿        |       🅿        |  🅿   |

### Note di implementazione

- Le route `/hikers/:id` e `/refuges/:id` attualmente non differenziano role-cross (un Hiker può leggere un altro Hiker). Da rivedere se la deliverable richiede privacy stretta tra utenti.
- `/api/v1/sessions/:id` non distingue creator vs partecipante in lettura — entrambi possono vedere i dettagli della sessione condivisa.
- `/api/v1/activities/:id` ha controllo `userId === ownerId` esplicito nel service (no leak).

---

## 5. Rate Limiting policy (riepilogo)

| Layer                  | Window | Max  | Skip             | Key                  |
| ---------------------- | ------ | ---- | ---------------- | -------------------- |
| `globalLimiter`        | 15 min | 300  | —                | IP                   |
| `loginLimiter`         | 15 min | 10   | success          | IP                   |
| `registerLimiter`      | 60 min | 5    | —                | IP                   |
| `passwordResetLimiter` | 60 min | 5    | —                | IP                   |
| `authenticatedLimiter` | 15 min | 1000 | —                | userId (fallback IP) |
| `writeLimiter`         | 15 min | 200  | GET/HEAD/OPTIONS | userId (fallback IP) |

Header esposti in risposta: `RateLimit-Remaining`, `RateLimit-Reset`, `Retry-After`. Status 429 con body JSON `{ error, retryAfter }` per UX graceful sul client mobile.

---

## 6. Secret management

| Variabile            | Required       | Note                                                              |
| -------------------- | -------------- | ----------------------------------------------------------------- |
| `JWT_SECRET`         | ✅ (sempre)    | >= 32 char, generato con `crypto.randomBytes(48).toString('hex')` |
| `JWT_EXPIRES_IN`     | ⚠ default `1d` | Mai > 7d in produzione                                            |
| `MONGO_URI`          | ✅ (prod)      | Connection string Atlas con credenziali read/write                |
| `BASE_URL`           | ✅ (prod)      | URL pubblico backend per email link                               |
| `BREVO_API_KEY`      | ✅ (prod)      | API key Brevo solo "transactional emails"                         |
| `EMAIL_FROM_ADDRESS` | ✅ (prod)      | Mittente verificato su Brevo                                      |
| `EMAIL_FROM_NAME`    | ❌             | Default "Trento Smart Mountain"                                   |
| `ALLOWED_ORIGINS`    | ⚠ prod         | CSV di origin CORS allow-list                                     |
| `NODE_ENV`           | ⚠              | `production` abilita HSTS + fail-fast aggiuntivi                  |

### Procedura rotazione JWT_SECRET

1. Genera nuovo secret: `node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"`
2. Aggiorna su Render → Environment → `JWT_SECRET`
3. Render ridistribuisce il servizio automaticamente
4. **Effetto**: tutti i JWT esistenti diventano invalidi → tutti gli utenti devono rifare login
5. Comunicare ai partecipanti via in-app banner (TODO)

### Procedura rotazione BREVO_API_KEY

1. Brevo Dashboard → Settings → API Keys → "Revoke" sulla key vecchia (ANCORA NON eliminarla)
2. "Create new API key" con permessi minimi: solo "Send transactional emails"
3. Aggiorna `BREVO_API_KEY` su Render
4. Verifica con email di reset password che le email partano
5. Solo allora elimina definitivamente la key vecchia

### NO secrets nel client mobile

Audit (2026-05-22) confermato:

- `mobile/app/build.gradle.kts` legge `BASE_URL` da `local.properties` / Gradle prop. È URL pubblica, non secret.
- Nessuna API key Brevo o JWT_SECRET nel codice Kotlin.
- Le mappe usano OpenTopoMap (no key richiesta).

---

## 7. Input validation strategy

### Server-side

Tutti gli endpoint POST/PATCH/DELETE che accettano body usano un middleware `validate(schema, source)` con schema **Joi**. Convenzioni:

- `Joi.object({...}).unknown(false)`: rifiuta campi non dichiarati (default).
- Stringhe sempre con `.max(N).trim()` per limitare allocazione.
- Email: `.email({ tlds: { allow: false } }).max(254).lowercase()`.
- Password: `.min(8).max(128)` (bcrypt input limit 72 byte ma teniamo 128 per UX).
- ObjectId Mongo: pattern `/^[0-9a-fA-F]{24}$/`.

### Client-side (mobile)

Per UX: i campi sono validati anche lato Compose (es. `RegisterViewModel` controlla email regex e min 8 char password). Il backend rimane la sorgente unica di verità.

---

## 8. Logging e auditing

- **Sanitize hits**: ogni operatore NoSQL rimosso da `express-mongo-sanitize` viene loggato con `key` e `path`.
- **Rate limit hits**: gli status 429 sono visibili nei log Express di default.
- **Auth failures**: `authMiddleware` logga "Invalid or expired token" implicitamente via response 401.

### TODO (Sprint 3+)

- Integrazione Sentry per errori e exception tracking.
- Audit trail per azioni admin (chi ha eliminato chi).
- Log aggregator centralizzato (Datadog/Logtail/Render Logs export).

---

## 9. Checklist deploy

Prima di ogni deploy produzione, verifica:

- [ ] `NODE_ENV=production` settato
- [ ] `JWT_SECRET` ≥ 32 char, generato fresh per la release
- [ ] `MONGO_URI` punta al cluster prod, NON test
- [ ] `BREVO_API_KEY` valida (test con `/auth/forgot-password` su email reale)
- [ ] `BASE_URL` corrisponde al dominio pubblico
- [ ] `ALLOWED_ORIGINS` popolato con i domini app autorizzati
- [ ] `.env` NON committato (verificato in `.gitignore`)
- [ ] `npm audit` non riporta vulnerabilità High/Critical aperte
- [ ] Swagger UI raggiungibile su `/api-docs` (smoke test)
- [ ] HSTS attivo (verifica header `Strict-Transport-Security`)
- [ ] Rate limit attivo (test 11 login falliti in 15 min → 429)

---

## 10. Riferimenti

- OWASP Top 10 2021: <https://owasp.org/Top10/>
- OWASP API Security Top 10 2023: <https://owasp.org/API-Security/editions/2023/en/0x11-t10/>
- helmet docs: <https://helmetjs.github.io/>
- express-rate-limit docs: <https://express-rate-limit.mintlify.app/>
- Joi schema reference: <https://joi.dev/api/>
- bcrypt cost factor analysis: <https://stackoverflow.com/q/55927723>

---

_Documento versione 1.0 — 22 maggio 2026 — Autore: Giacomo Radin (ID-6)_
