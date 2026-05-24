# Trento Smart Mountain — Stato del progetto

> Snapshot dell'architettura, codebase e implementazione corrente. Aggiornato al **24 maggio 2026** (Sprint 2, dopo il batch fix bug post-merge + deploy Render develop + piani social/profilo+formazione).

---

## 1. Identità progetto

- **Corso**: Ingegneria del Software, UniTrento
- **Gruppo**: ID-6
  - Giacomo Radin (242907) — backend + mobile + lead
  - Federico Cattelan (242111)
  - Marco Christian Stoica (246443) — weather integration
- **Deliverable**: D1 (requisiti, 27/03/2026) ✅ — D2 (architettura, 26/04/2026) ✅ — D3 in progress
- **Stack**:
  - Backend: Node.js 18+, Express 4, Mongoose 8, MongoDB Atlas
  - Mobile: Kotlin 2.0.21, Compose BOM 2024.12.01, Room 2.6.1, Retrofit, OSMdroid
  - IoT: Mosquitto MQTT + Python gateway (placeholder)
- **Hosting**: Backend su Render Free tier (cold start ~30-60s); MongoDB Atlas free

---

## 2. Struttura monorepo

```
trento-smart-mountain/
├─ backend/src/
│  ├─ app.js                # bootstrap Express + security stack
│  ├─ server.js             # avvio + assertEnvironment fail-fast
│  ├─ middleware/
│  │  ├─ authMiddleware.js          # JWT verify
│  │  ├─ authorizationMiddleware.js # role check (Admin only)
│  │  ├─ errorMiddleware.js
│  │  ├─ rateLimitMiddleware.js     # 5 limiter differenziati
│  │  ├─ securityMiddleware.js      # helmet, mongo-sanitize, hpp, CORS
│  │  └─ validationMiddleware.js    # Joi schemas + factory
│  ├─ models/
│  │  ├─ user.js                    # discriminator base
│  │  ├─ hiker.js / refuge.js / admin.js
│  │  ├─ hikeSession.js             # sessioni di gruppo (gpxStats + actualStats)
│  │  ├─ activity.js                # attività libere personali (nuovo)
│  │  └─ location.js                # weather venues (towns/POI)
│  ├─ routes/                       # mapping URL → service
│  │  ├─ authRoutes.js              # rate limit + validate per ogni endpoint sensibile
│  │  ├─ hikerRoutes.js / refugeRoutes.js / adminRoutes.js
│  │  ├─ hikeSessionRoutes.js
│  │  ├─ activityRoutes.js          # nuovo: CRUD attività libere
│  │  └─ weatherRoutes.js
│  └─ services/                     # logica business
│
├─ mobile/app/src/main/java/it/trentosmartmountain/app/
│  ├─ TsmApplication.kt             # DI manuale: TokenStorage, Room, Retrofit, SyncManager
│  ├─ data/
│  │  ├─ estimation/HikeEstimation.kt   # CAI/Naismith formulas
│  │  ├─ local/
│  │  │  ├─ TokenStorage.kt              # EncryptedSharedPreferences (JWT)
│  │  │  └─ db/                          # Room v4 (con retry fields)
│  │  ├─ location/                       # GPS tracking engine
│  │  ├─ remote/
│  │  │  ├─ TsmApiClient.kt / TsmApiService.kt
│  │  │  └─ dto/                         # CreateActivityRequest, ActualStats, ...
│  │  ├─ session/SessionStartCoordinator.kt
│  │  └─ sync/SyncManager.kt             # poll loop 60s + backoff 1m→5m→30m→1h
│  ├─ service/ForegroundTrackingService.kt
│  ├─ ui/
│  │  ├─ navigation/
│  │  ├─ screens/{auth,home,login,main,profile,refuge,register,registra,session}
│  │  └─ theme/
│  └─ viewmodel/
│
├─ docs/
│  ├─ SECURITY.md           # nuovo: threat model, OWASP, ACM
│  ├─ TSM_PROJECT_STATE.md  # questo file
│  ├─ D1_*.pdf / D2_*.pdf
│  ├─ api_reference.md / architecture.md / database_schema.md
│  ├─ mobile_app_base.md / setup_backend.md / setup_mobile.md
│  ├─ android_server_communication.md
│  ├─ sprint2_plan.md
│  └─ test_cases_sprint1.md
│
├─ iot/                              # placeholder Python gateway
├─ scripts/                          # adb-reverse, ecc.
├─ docker-compose.yml                # MongoDB + Mosquitto locali
├─ swagger.js / swagger-output.json
├─ .env.example                      # nuovo: template variabili
└─ .gitignore                        # .env già escluso
```

---

## 3. Cosa è implementato

### Backend

#### Auth

- ✅ POST `/auth/register/hiker` — schema Joi, rate limit 5/h, bcrypt
- ✅ POST `/auth/register/refuge` — flat schema con rifugioName/address/altitudeMeters
- ✅ POST `/auth/login` — rate limit 10/15min (skip success)
- ✅ GET `/auth/verify/:token` — deep link `tsm://` con auto-login
- ✅ POST `/auth/forgot-password` — email link via Brevo, rate limit 5/h
- ✅ GET / POST `/auth/reset-password/:token` — form HTML + JSON, token monouso 1h

#### Sessions (gruppo)

- ✅ CRUD sessione + invite code TSM-XXXX univoco
- ✅ Join via codice invito (vincolo: una sola ACTIVE per utente)
- ✅ PATCH `/status` per lifecycle PLANNED→ACTIVE→COMPLETED
- ✅ PATCH `/complete` con actualStats (movingSec, distMeters, finalPoints, ecc.)
- ✅ GET `/stats?year=N` aggregate annuali (unifica HikeSession.COMPLETED + Activity libere)
- ✅ Modello gpxStats con gpxDurationSec (durata effettiva dal `<time>` GPX)
- ✅ Modello actualStats per dati registrati live

#### Activities (libere)

- ✅ POST `/api/v1/activities` — creazione attività personale
- ✅ GET `/api/v1/activities` — lista per utente
- ✅ GET `/api/v1/activities/:id` — dettaglio con check owner
- ✅ DELETE `/api/v1/activities/:id` — solo proprietario
- ✅ Indici `(userId, completedAt)` + 2dsphere sparse su startPoint

#### Weather

- ✅ GET `/weather/locations/nearby?lon=&lat=&maxDistance=&type=` (2dsphere, auth)
- ✅ GET `/weather/locations/search?q=&type=&limit=` (regex case-insensitive, auth)
- ✅ GET `/weather/forecast/:externalId?forceRefresh=` (cache 1h, auth)
- ✅ POST `/weather/seed` — admin only (auth + requireRoles)
- ✅ POST `/weather/forecast/:id/refresh` — admin only (auth + requireRoles)
- ✅ Seed automatico da meteo.report + gitlab tinia-euregio

#### Security

- ✅ helmet (CSP custom per Swagger UI, HSTS in prod)
- ✅ CORS allow-list via `ALLOWED_ORIGINS` env
- ✅ express-mongo-sanitize (NoSQL injection)
- ✅ hpp (HTTP Parameter Pollution)
- ✅ Body size 100 KB
- ✅ Rate limit a 5 livelli (global, login, register, password reset, authenticated, write)
- ✅ Joi validation su tutti gli endpoint POST/PATCH/DELETE
- ✅ fail-fast su env vars mancanti / JWT_SECRET debole
- ✅ `trust proxy = 1` per IP corretto dietro Render
- ✅ JWT expiry esteso a 7d (default) per supportare requisito offline 3 giorni
- ✅ Tutte le route weather admin protette (`/weather/seed`, `/weather/forecast/:id/refresh`)

### Mobile

#### Auth

- ✅ AuthEntry → Login / Register / RegisterRifugio / ForgotPassword
- ✅ JWT in EncryptedSharedPreferences (TokenStorage)
- ✅ Deep link auto-login post email verify

#### Sessions

- ✅ SessionHubScreen: tab PIANIFICA (GPX import + form + QR) / UNISCITI (code box + lista)
- ✅ Tab UNISCITI mostra solo sessioni PLANNED/ACTIVE (COMPLETED filtrate → "Le mie attività")
- ✅ SessionDetailScreen: elevation chart, meteo reale TINIA, checklist drag-and-drop, partecipanti, edit creator
- ✅ SessionPlanViewModel con parser GPX (haversine + smoothing + valley-peak + `<time>` per durata effettiva)
- ✅ SessionStartCoordinator: SharedFlow(replay=1) per consegna affidabile a HikerMainScreen + RegistraVM

#### Registra (tracking GPS)

- ✅ TsmMapView (OSMdroid + OpenTopoMap tiles)
- ✅ HikeTrackingEngine + StationaryDetector (auto-pause)
- ✅ ForegroundTrackingService (persistenza GPS in background)
- ✅ Check GPS hardware abilitato dentro `RegistraViewModel.startTracking()` (copre tutte le route: REC button, autoStart da Detail/Hub)
- ✅ Dialog "GPS spento" con link a `Settings.ACTION_LOCATION_SOURCE_SETTINGS`
- ✅ Dialog "Salva Attività" con KPI strip (Distanza/Durata/Dislivello/Punti) + nome editabile pre-popolato "Escursione – [data]"
- ✅ Dialog "Attività troppo corta" per libere < 50m con 3 opzioni distinte: Salva comunque / Continua / Cancella
- ✅ Upload immediato post-stop (PATCH /complete o POST /activities)
- ✅ Fallback SyncManager se la rete fallisce

#### Home / Le Mie Attività

- ✅ HomeScreen sotto-tab "Personale" cabla `ActivityListScreen` (prima placeholder)
- ✅ Tap su card attività apre `ActivityDetailScreen` via `Routes.ACTIVITY_DETAIL` (navigazione cablata)
- ✅ ActivityListScreen con yearly stats card (HorizontalPager 5 anni)
- ✅ MonthlyBarChart cliccabile (filtro mese)
- ✅ Sort: recente / vecchia / A-Z / distanza / difficoltà / durata
- ✅ Bottone "Risincronizza ($n)" bypassa il backoff (`SyncManager.enqueueImmediate(ignoreBackoff=true)`)
- ✅ Empty state contestuale (no attività vs no attività per periodo)
- ✅ ElevationProfileChart con assi disegnati nel canvas (no più sovrapposizione)
- ✅ ActivityDetailScreen: metric grid, mappa preview, partecipanti, profilo altimetrico, timeline split km, badge dinamici, export GPX
- ✅ Delete con cleanup remoto per attività libere (DELETE /activities/:id)

#### Sync engine

- ✅ SyncManager: coroutine loop 60s + backoff incrementale per record (1m → 5m → 30m → 1h cap)
- ✅ `enqueueImmediate()` per pull-to-refresh manuale
- ✅ Room v4 con campi `retry_count`, `last_retry_at_ms`, `remote_id`
- ✅ Marcatura `isSynced=1` post upload + tracking `remoteId` per delete cross-device

### Convenzioni codice

- Backend: routes → services → models (3 layer); errori business come `throw new Error("CODE")` mappati in HTTP
- Mobile: MVVM (Compose UI + StateFlow + Repository). DI manuale (no Hilt)
- JWT payload: `{ userId, role }`
- ID Mongo serializzati come `_id` (string ObjectId)

---

## 4. Gap analysis (cosa manca)

### Non ancora implementato

- ❌ SOS via BLE Mesh — UC4 da D1 (Sprint 3)
- 📋 NFC check-in vetta — UC5 da D1 — **piano scritto** in [sprint2_profilo_formazione.md](sprint2_profilo_formazione.md) (fase C+G)
- 📋 Social Credits + livelli + feed — **piano scritto** in [sprint2_social.md](sprint2_social.md) e [sprint2_profilo_formazione.md](sprint2_profilo_formazione.md)
- 📋 Educational mode + Quiz — **piano scritto** in [sprint2_profilo_formazione.md](sprint2_profilo_formazione.md) (fase B+F)
- ❌ MQTT mobile client + IoT gateway Python — placeholder (Sprint 3)
- ❌ OAuth Google login — solo email/password (Sprint 3)
- ❌ Socket.io live tracking partecipanti — dipendenza installata ma non usata (Sprint 3)
- ❌ Admin dashboard web — solo API
- ❌ Refuge dashboard mobile — placeholder (post-Sprint 3)

> Legend: ❌ = non pianificato per Sprint 2; 📋 = piano operativo scritto, codice da produrre

### Tech debt / TODO security

- ⚠ Logging strutturato + Sentry (oggi solo console)
- ⚠ Audit trail per azioni admin (chi ha eliminato chi)
- ⚠ Rotazione automatica JWT secret (oggi manuale, vedi SECURITY.md sez. 6)
- ⚠ Tombstone table per delete offline-first (oggi best-effort)
- ⚠ Refresh token rotation (oggi JWT singolo con expiry 7d — basta per offline 3gg, ma Sprint 3 vuole access+refresh)
- ⚠ Rate limit con store Redis (oggi in-memory, ok per single-instance)
- ⚠ CI gate su `npm audit` (oggi 6 moderate severity da risolvere)
- ⚠ Coverage Jest da estendere ai service layer (oggi: route auth/hiker/sessions/activities/weather coperte; service layer indiretto via route)

### Limiti noti sync mobile

- ⚠ Senza WorkManager: il sync funziona solo se il process è vivo (foreground o cached background). Se l'OS killa l'app, niente retry finché non si riapre. Workaround: alla riapertura il loop riparte e processa il backlog.
- ⚠ Niente Redis distribuito: rate limit è per-instance (Render free tier single instance, accettabile).

---

## 5. Roadmap Sprint 2-3

### Sprint 2 (in corso, deadline ~giugno 2026)

#### Chiuso
- [x] Bug fix tempi GPX (durata effettiva da `<time>`)
- [x] Sync attività locali/cloud con actualStats
- [x] Profilo altimetrico corretto (no overlap)
- [x] Bar chart mese + cards anno funzionanti
- [x] Attività libere collection + endpoint
- [x] Security hardening completo (rate limit, validation, secrets)
- [x] Retry incrementale sync (1m → 5m → 30m → 1h)
- [x] UI re-sync manuale + dialog soglia 50m
- [x] Risolti merge conflicts user.js/hikeSessionRoutes.js/hikeSessionService.js (bloccavano deploy Render)
- [x] Render develop deploy live (auto-build su push branch UI)
- [x] Wiring ActivityListScreen in HomeScreen + nav verso ActivityDetailScreen
- [x] Dialog "Salva Attività" con KPI strip (Distanza/Durata/Dislivello/Punti) + nome pre-popolato
- [x] Dialog "Attività troppo corta" UX a 3 opzioni (Salva/Continua/Cancella)
- [x] Check GPS hardware spostato nel VM (copre tutte le route di start tracking)
- [x] Fix AVVIA tab switch via SharedFlow (StateFlow conflated saltava emit)
- [x] Fix bottone Risincronizza che ignorava il backoff retry
- [x] JWT expiry esteso a 7d per requisito offline 3 giorni
- [x] Auth admin su `POST /weather/seed` e `POST /weather/forecast/:id/refresh`
- [x] Filtro tab UNISCITI: solo sessioni PLANNED/ACTIVE
- [x] Endpoint mobile stats corretto (`/api/v1/sessions/stats` non `/activities/stats`)

#### Piani approvati, codice da scrivere
- [ ] Schermata SOCIAL (Home → tab Social) — piano in [sprint2_social.md](sprint2_social.md)
- [ ] Schermata Profilo rinnovata + Formazione + NFC — piano in [sprint2_profilo_formazione.md](sprint2_profilo_formazione.md)

#### Ancora aperto
- [x] Tests Jest su route principali (auth + hiker + sessions + activities + weather auth)
- [ ] D3 documentation (in scrittura)

### Sprint 3 (planned)

- [ ] BLE Mesh SOS prototype
- [ ] OAuth Google
- [ ] Sentry integration
- [ ] CI con npm audit gate
- [ ] Migration Room esplicita (no più fallbackToDestructive)
- [ ] Refresh token rotation (access 15min + refresh 30d)
- [ ] WorkManager per sync robusto quando OS killa l'app
- [ ] CMS web admin per quiz (oggi seed JSON)
- [ ] Modalità gara quiz (timer per domanda + leaderboard)

---

## 6. Setup rapido

### Backend

```bash
# Prima volta
npm install
cp .env.example .env
# Edita .env e popola JWT_SECRET, MONGO_URI, BREVO_API_KEY, BASE_URL

# Avvio dev
docker compose up -d mongodb        # MongoDB locale
npm run dev                          # nodemon su localhost:3000
```

### Mobile

```bash
cd mobile
# crea mobile/local.properties:
#   sdk.dir=/path/to/Android/Sdk
#   tsm.api.baseUrl=http://10.0.2.2:3000/   (emulatore Android)

./gradlew compileDebugKotlin         # check sintassi
./gradlew installDebug               # deploy su emulatore connesso
```

### Variabili env critiche (vedi `.env.example`)

- `JWT_SECRET` ≥ 32 char random
- `MONGO_URI` connection string MongoDB
- `BREVO_API_KEY` per email transazionali
- `BASE_URL` URL pubblico backend
- `ALLOWED_ORIGINS` CSV CORS (solo prod)

---

## 7. Riferimenti documenti correlati

- **Sicurezza dettagliata**: [docs/SECURITY.md](SECURITY.md) — threat model, OWASP, ACM, secret management
- **API endpoint**: [docs/api_reference.md](api_reference.md)
- **Architettura componenti**: [docs/architecture.md](architecture.md)
- **MongoDB schema**: [docs/database_schema.md](database_schema.md)
- **Setup mobile**: [docs/setup_mobile.md](setup_mobile.md)
- **Setup backend**: [docs/setup_backend.md](setup_backend.md)
- **Comunicazione client-server**: [docs/android_server_communication.md](android_server_communication.md)
- **Sprint plan generale**: [docs/sprint2_plan.md](sprint2_plan.md)
- **Sprint 2 — Social**: [docs/sprint2_social.md](sprint2_social.md) — feed Strava-like, follow, like, commenti
- **Sprint 2 — Profilo+Formazione**: [docs/sprint2_profilo_formazione.md](sprint2_profilo_formazione.md) — Social Credits con 10 livelli alpini, quiz, NFC totem

---

_Last update: 2026-05-24 — Giacomo Radin (ID-6)_
