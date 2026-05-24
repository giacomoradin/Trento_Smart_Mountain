# Setup Sviluppo Backend (Node.js + MongoDB) 🌐

> Guida operativa per configurare l'ambiente di sviluppo del **backend Trento Smart Mountain** (companion a `setup_mobile.md`).
>
> **Ultima revisione**: 17/05/2026 — Fine Sprint 1.

---

## 1. Prerequisiti

| Tool | Versione minima | Note |
|------|------|------|
| **Node.js** | 20.x LTS | Verifica con `node -v` |
| **npm** | 10.x | Incluso con Node 20 |
| **Docker Desktop** | 4.x+ | Per MongoDB + Mosquitto locali |
| **Git** | 2.x | — |
| **Postman / Insomnia** | (opzionale) | Per testing API |
| **VS Code** | (opzionale, raccomandato) | Estensioni incluse in `.vscode/extensions.json` |

---

## 2. Setup iniziale (primo avvio)

### 2.1 Clone repository

```bash
git clone https://github.com/giacomoradin/Trento_Smart_Mountain.git
cd Trento_Smart_Mountain
```

### 2.2 Install dipendenze root

```bash
# Dalla root del monorepo
npm install
```

### 2.3 Install dipendenze backend

```bash
cd backend
npm install
cd ..
```

### 2.4 Configurazione variabili d'ambiente

Crea un file `backend/.env` (non versionato — già in `.gitignore`):

```bash
# MongoDB
MONGO_URI=mongodb://localhost:27017/tsm

# JWT
JWT_SECRET=<random-256-bit-secret>
JWT_EXPIRATION_HOURS=168

# Server
PORT=3000

# SMTP Gmail (per email verifica + reset password)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=<tua-email-gmail>
SMTP_APP_PASSWORD=<gmail-app-password>
# ⚠️ NON la tua password Gmail normale!
# Genera una "App password" da: https://myaccount.google.com/apppasswords
# (richiede 2FA attivo sul tuo account)

# Frontend deep link
DEEP_LINK_SCHEME=tsm

# TINIA Weather API
TINIA_API_URL=https://meteo.report/api/...
TINIA_TOWNS_ENDPOINT=/towns
TINIA_POI_ENDPOINT=/poi
TINIA_FORECAST_ENDPOINT=/forecast
```

> 💡 **Genera un JWT_SECRET sicuro**: `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`

---

## 3. Avvio infrastruttura locale (Docker)

Il progetto include un `docker-compose.yml` nella root con MongoDB + Mosquitto (MQTT).

```bash
# Dalla root del repo
docker compose up -d

# Verifica:
docker ps
# Devi vedere: mongo + mosquitto in stato "Up"
```

### 3.1 Connettersi a MongoDB

```bash
# Via Docker exec
docker exec -it tsm-mongo-1 mongosh

# All'interno della shell mongosh:
use tsm
show collections
db.users.find().limit(5)
```

### 3.2 Reset DB (durante dev)

```bash
docker exec -it tsm-mongo-1 mongosh
> use tsm
> db.dropDatabase()
```

---

## 4. Avvio backend

### 4.1 Modalità sviluppo

```bash
cd backend
npm run dev
# ↑ usa nodemon — hot reload su modifica file
```

Console output atteso:

```
[Server] Connesso a MongoDB: mongodb://localhost:27017/tsm
[Server] In ascolto sulla porta 3000
[Server] Swagger UI: http://localhost:3000/api-docs
```

### 4.2 Modalità produzione (no hot reload)

```bash
cd backend
npm start
```

### 4.3 Verifica setup

| Test | URL |
|------|-----|
| Swagger UI | `http://localhost:3000/api-docs` |
| Endpoint dummy ping | `curl http://localhost:3000/users` (404 ok, conferma server up) |

---

## 5. Seeding iniziale del database

### 5.1 Crea utente admin (solo dev — Sprint 2 to be scripted)

Apri mongosh:

```bash
docker exec -it tsm-mongo-1 mongosh
> use tsm
> db.users.insertOne({
    username: "admin@tsm.local",
    email: "admin@tsm.local",
    passwordHash: "$2b$10$rGn8Re9I5OQYbWAFiGT0OuJ3Yd4PVqAU/iEdc1G73lQ91DJSPC4t.", // password: "admin123"
    role: "admin",
    isVerified: true,
    createdAt: new Date()
  })
```

> 💡 Per generare un nuovo hash bcrypt: `node -e "console.log(require('bcrypt').hashSync('TUAPASSWORD', 10))"`

### 5.2 Seed Locations Meteo (TINIA)

Una volta avviato il backend e creato un admin, esegui il seed da Postman:

```http
POST http://localhost:3000/auth/login
Content-Type: application/json
{
  "email": "admin@tsm.local",
  "password": "admin123"
}
```

Copia il `token` dalla response. Poi:

```http
POST http://localhost:3000/weather/seed
Authorization: Bearer <token-admin>
```

Output atteso:

```json
{
  "message": "Seed completato",
  "inserted": 145,
  "updated": 0,
  "errors": []
}
```

> ⚠️ Il seed è pesante (~1-2 minuti, dipende dall'API TINIA). Esegui solo una volta o quando il DB è stato resettato.

---

## 6. Architettura cartelle backend

```
backend/
├── src/
│   ├── app.js                          # Express app + middleware + routes mounting
│   ├── server.js                       # MongoDB connect + listen
│   │
│   ├── middleware/
│   │   ├── authMiddleware.js           # JWT Bearer → req.user
│   │   ├── authorizationMiddleware.js  # requireRoles(...)
│   │   └── errorMiddleware.js          # globalErrorHandler + notFoundHandler
│   │
│   ├── models/                         # Mongoose schemas
│   │   ├── user.js                     # User schema (+ sessionRoles post-fix M1)
│   │   ├── hikeSession.js              # HikeSession schema (+ sparse 2dsphere post-fix M3)
│   │   └── location.js                 # Location schema (meteo cache)
│   │
│   ├── routes/                         # Express routers
│   │   ├── authRoutes.js               # /auth/*
│   │   ├── userRoutes.js               # /users/*
│   │   ├── hikeSessionRoutes.js        # /api/v1/sessions/*
│   │   └── weatherRoutes.js            # /weather/* (admin-protected post-fix C2)
│   │
│   └── services/                       # Logica business
│       ├── authService.js              # login, register, verify, forgot/reset password
│       ├── userService.js              # CRUD utenti
│       ├── hikeSessionService.js       # create, join, leave, update, stats
│       ├── emailService.js             # sendMailWithRetry + templates
│       └── weatherService.js           # TINIA fetch + cache + nearby + forecast
│
├── package.json
├── .env                                # ⚠️ NON committare
└── (no tests yet — Sprint 2)
```

---

## 7. Convenzioni codice backend

### 7.1 Naming

| Cosa | Convenzione | Esempio |
|------|-------------|---------|
| File service | `<feature>Service.js` | `hikeSessionService.js` |
| File route | `<feature>Routes.js` | `hikeSessionRoutes.js` |
| File model | `<entity>.js` (lowercase singolare) | `user.js`, `hikeSession.js` |
| Function | camelCase | `createSession()` |
| Constant | UPPER_SNAKE | `JWT_EXPIRATION_HOURS` |
| Error code (throw) | UPPER_SNAKE stringa | `throw new Error("SESSION_NOT_FOUND")` |
| MongoDB collection | lowercase plurale | `users`, `hikesessions`, `locations` |

### 7.2 Pattern service → route

I **service** lanciano `Error("CODE")` (codici stringa), le **route** li mappano a status HTTP:

```javascript
// Service
export async function updateSessionStatus(id, userId, newStatus) {
  const s = await HikeSession.findById(id);
  if (!s) throw new Error("SESSION_NOT_FOUND");
  if (s.creatorId.toString() !== userId) throw new Error("FORBIDDEN");
  s.status = newStatus;
  await s.save();
  return s;
}

// Route
router.patch("/:id/status", async (req, res) => {
  try {
    await updateSessionStatus(req.params.id, req.user.userId, req.body.status);
    res.status(200).json({ message: "Status aggiornato" });
  } catch (err) {
    if (err.message === "SESSION_NOT_FOUND") return res.status(404).json({ error: "..." });
    if (err.message === "FORBIDDEN")        return res.status(403).json({ error: "..." });
    res.status(500).json({ error: "Errore generico" });
  }
});
```

### 7.3 Populate symmetric

Quando un endpoint ritorna documenti `HikeSession` al client Kotlin, **DEVE** popolare entrambi i campi ref per evitare crash Gson:

```javascript
return session.populate([
  { path: "creatorId", select: "username email" },
  { path: "participants.userId", select: "username email" },
]);
```

Vedi `docs/architecture.md` §4.2 per il razionale.

### 7.4 Async route handlers

Tutti i route handler sono `async`. Per evitare try/catch boilerplate, usa il pattern `asyncHandler` in `weatherRoutes.js`:

```javascript
const asyncHandler = (fn) => (req, res, next) =>
  Promise.resolve(fn(req, res, next)).catch(next);

router.get("/path", asyncHandler(async (req, res) => {
  const result = await service.doSomething();
  res.json(result);
}));
```

L'`errorMiddleware.js` globale catturerà l'eventuale throw.

---

## 8. Testing manuale con Postman

### 8.1 Workflow completo demo

1. **Login admin**: `POST /auth/login` → ottieni `token-admin`.
2. **Seed locations**: `POST /weather/seed` con `token-admin`.
3. **Crea utente**: `POST /users` con email/password (registrazione).
4. **Simula verifica email**: in mongosh: `db.users.updateOne({email: "..."}, { $set: { isVerified: true } })`.
5. **Login utente**: `POST /auth/login` → ottieni `token-user`.
6. **Crea sessione**: `POST /api/v1/sessions` con `token-user` (GPX stats fake ok).
7. **Lista sessioni**: `GET /api/v1/sessions/my`.
8. **Verifica meteo**: `GET /weather/forecast/<externalId>`.

### 8.2 Postman collection (Sprint 2 todo)

Pianificato: `docs/tsm.postman_collection.json` + `tsm.postman_environment.json`.

---

## 9. Debug & troubleshooting

### Problema: `Cannot connect to MongoDB`

**Causa**: Docker MongoDB non avviato o porta diversa.

**Fix**:
```bash
docker compose up -d
docker ps  # verifica mongo "Up"
```

### Problema: `JWT_SECRET is not defined`

**Causa**: `.env` mancante o non caricato.

**Fix**: Verifica che esista `backend/.env` e che `dotenv` sia caricato in `server.js` (di solito `import "dotenv/config"` a inizio file).

### Problema: SMTP `Invalid login`

**Causa**: Gmail richiede "App password" (non la password normale dell'account).

**Fix**:
1. Attiva 2FA sul tuo account Gmail.
2. Genera App Password da https://myaccount.google.com/apppasswords.
3. Inserisci la App Password (16 caratteri senza spazi) in `SMTP_APP_PASSWORD` nel `.env`.

### Problema: `403 Forbidden` su `POST /weather/seed`

**Causa**: dal 17/05 endpoint admin-only (fix C2).

**Fix**: Usa il token JWT di un utente `role: "admin"` (vedi §5.1 per crearne uno).

### Problema: `Email già registrata` durante test ripetuti

**Causa**: utente esiste dal precedente test.

**Fix**: cancella da mongosh:
```bash
docker exec -it tsm-mongo-1 mongosh
> use tsm
> db.users.deleteOne({email: "test@example.com"})
```

### Problema: Mobile app non si connette al backend localhost

**Causa**: emulator Android non vede `localhost` del PC host.

**Fix**: nel client mobile usa `http://10.0.2.2:3000` invece di `localhost`. Vedi `mobile/.../data/remote/TsmApiClient.kt`.

---

## 10. Comandi utili

```bash
# Visualizza i log MongoDB
docker logs tsm-mongo-1 --follow

# Visualizza i log backend (se runni con npm run dev)
# I log sono già nel terminale che ha lanciato npm run dev

# Reset completo dell'ambiente
docker compose down -v   # rimuove anche i volumi
docker compose up -d

# Lint backend
cd backend
npm run lint             # se configurato (Sprint 2 todo: ESLint setup)

# Update Swagger JSON
cd backend
node swagger.js          # rigenera swagger-output.json
```

---

## 11. Deployment

### Render (attualmente attivo)

Backend deployato su Render Free tier:

- **URL pubblico**: `https://trento-smart-mountain-xz7u.onrender.com`
- **Branch monitorata**: `UI` (auto-deploy su push)
- **Build command**: `npm install`
- **Start command**: `npm start` (= `node backend/src/server.js`)
- **Node version**: 26+ (definita in `package.json` engines)
- **Persistenza**: MongoDB Atlas (managed) via env `MONGO_URI`
- **Cold start**: ~30-60s sul tier free (primo hit dopo idle)

#### Env vars richieste su Render → Settings → Environment

| Variabile | Valore |
|---|---|
| `JWT_SECRET` | ≥32 char random hex |
| `JWT_EXPIRES_IN` | `7d` (per offline 3gg con margine) |
| `MONGO_URI` | Connection string Atlas |
| `BREVO_API_KEY` | API key transactional emails |
| `BASE_URL` | `https://trento-smart-mountain-xz7u.onrender.com` |
| `EMAIL_FROM_ADDRESS` | mittente verificato su Brevo |
| `ALLOWED_ORIGINS` | CSV CORS, se richiesto |
| `NODE_ENV` | `production` (abilita HSTS) |

#### Troubleshooting deploy fallito

Se Render → Events mostra "Exited with status 1":

1. Apri "Logs" del deploy fallito → cerca `SyntaxError` o `ReferenceError` lungo lo stack
2. Errori comuni storici:
   - **Merge marker non risolto** (`<<<<<<< HEAD`) in file js → grep nel repo prima di pushare
   - **Import mancante** in route file → `node -e "import('./backend/src/routes/xxx.js')"` come smoke test locale
   - **Env var mancante** (es. `JWT_SECRET`) → fail-fast su `server.js:assertEnvironment`
3. Rollback rapido: Render Dashboard → Events → click ⋮ → "Redeploy" su un commit verde precedente

### Roadmap deploy (Sprint 3+)

- Dockerfile per il backend (alternative a Render)
- Nginx reverse proxy + HTTPS Let's Encrypt (se self-hosted)
- CI/CD GitHub Actions: build → test → deploy automatico
- Health check endpoint `GET /healthz` per Render uptime monitor

---

## 12. Riferimenti

| Cosa | Dove |
|------|------|
| API reference human-readable | `docs/api_reference.md` |
| API reference Swagger UI | `http://localhost:3000/api-docs` |
| Database schema | `docs/database_schema.md` |
| Architettura generale | `docs/architecture.md` |
| Setup mobile | `docs/setup_mobile.md` |
| Project state | `docs/TSM_PROJECT_STATE.md` |

---

*Setup backend — Sprint 2 in corso. Ultimo update 2026-05-24 (sezione 11 Render deploy aggiunta).*
