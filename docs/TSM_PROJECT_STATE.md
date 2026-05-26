# Trento Smart Mountain — Stato del progetto

> Snapshot dell'architettura, codebase e implementazione corrente. Aggiornato al **26 maggio 2026** (Sprint 2, dopo batch fix critico discriminator persistence + anti-cheat server-side, ProfileViewScreen, build mobile + 78 test verdi).

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
- [x] Tests Jest su route principali (auth + hiker + sessions + activities + weather auth + account v2 + discriminator persistence) — **78/78 verde**
- [x] Profilo v2 completo (personalInfo/experience/preferences/goals) + onboarding 3-step skippable
- [x] ProfileViewScreen read-only con indicatori 🔒 sui campi anti-cheat
- [x] Anti-cheat enforcement server-side (birthDate, caiLevel) — fix critico 26/05
- [x] Discriminator persistence (Hiker fields via $set/$inc) — fix critico 26/05
- [x] Auto-seed quizzes al boot del server (idempotente)
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

_Last update: 2026-05-26 — Giacomo Radin (ID-6)_

---

## 8. Cronologia bug fix critici (26 maggio 2026 — sessione autonoma)

Sessione di hardening notturna dopo che l'utente ha segnalato 6 bug post-deploy
Render. Durante l'audit sono stati scoperti **3 bug critici** prima nascosti
oltre ai 6 originali. Test passati da 60/61 a **78/78**, build mobile verde.

### 8.1 [CRITICO] Discriminator persistence (root cause del bug "menu data not persistent")

**Sintomo:** L'utente compilava i dati nel profilo (peso, altezza, livello CAI,
preferenze), tornava indietro e ritrovava i valori di default. Inoltre i crediti
NFC/quiz/sessioni non sembravano cumularsi correttamente.

**Causa:** Tutti i write usavano `User.findByIdAndUpdate(...)` (modello base).
I campi `personalInfo`, `experience`, `preferences`, `weeklyGoals`,
`profileCompletedAt`, `socialCredits`, `nfcStats.*` sono però **solo nel
sub-schema Hiker** (discriminator). Lo strict mode di Mongoose applicato al
modello base scartava silenziosamente l'`$set`/`$inc` — la response tornava
200 OK ma il DB non veniva toccato. Bug invisibile perché:
- Le response 200 facevano pensare a un save riuscito
- I successivi `findById` ritornavano comunque i dati esistenti (per i campi
  letti, la projection MongoDB funziona indipendentemente dal modello)
- Le ViewModel scoped-to-Activity mostravano gli ultimi valori in memoria

**Fix:** Tutti i write su campi discriminator usano ora `Hiker.findByIdAndUpdate`
(o il modello corretto via lookup `role`). File toccati:
- `services/accountService.js` — `updatePersonalInfo/Experience/Preferences/Goals`
  + `markProfileCompleted`
- `services/creditService.js` — `addCredits` ($inc socialCredits)
- `services/nfcService.js` — $inc nfcStats.scansCount/scansCredits
- `services/adminService.js` — `updateAnyUser` con lookup discriminator

**Coverage:** Test in `__tests__/services/discriminator.test.js` (4 test) +
`__tests__/routes/account.test.js` (13 test) che fissano il contratto.

### 8.2 [CRITICO] Anti-cheat enforcement server-side

**Sintomo:** Frontend mostrava lucchetto 🔒 su `birthDate` e `caiLevel` dopo
prima impostazione, ma chiunque poteva aggirare con curl/Postman e abbassare il
livello CAI per farmare crediti facili.

**Fix:** `updatePersonalInfo` / `updateExperience` ora rilanciano
`LockedFieldError` → HTTP 409 se il campo è già impostato. Mobile parsea il
campo `message` del body di errore per UX leggibile.

### 8.3 [QoL] Username con caratteri italiani

`updateAccountSchema.username` riusa ora il regex `usernameField` permissivo
(`/^[a-zA-ZÀ-ÿ0-9\s''.\-]+$/`) della registrazione. Prima era `.min(3)` raw
senza pattern → "Giacomo Radin" passava la registrazione ma falliva il PATCH.

### 8.4 [DX] Test affidabili: rate limit bypass in NODE_ENV=test

I rate limiter ora hanno `skip: () => process.env.NODE_ENV === "test"`.
`__tests__/setup.js` forza `NODE_ENV=test`. Risolve i 429 random nei test
con molte richieste consecutive.

### 8.5 [F13/F14] Schermata profilo read-only + auto-seed quiz

- Nuova `ProfileViewScreen.kt` con 4 sezioni (Dati personali, Esperienza,
  Preferenze, Obiettivi) e indicatori 🔒 sui campi anti-cheat. Route
  `PROFILE_VIEW` + icona AccountCircle nella `ProfileScreen` header.
- `server.js` esegue `autoSeedQuizzes()` al boot se la collection
  `quizcategories` è vuota — risolve il problema "Formazione blank" su Render.
- `FormazioneScreen` ha empty state grafico quando nessuna categoria disponibile.

### 8.6 ViewModel scoping (Activity-wide invece di NavBackStackEntry)

8 schermate (4 edit + 3 onboarding + ProfileScreen) ora usano
`viewModelStoreOwner = LocalContext.current as ComponentActivity` per
condividere un singolo `ProfileV2ViewModel` a livello Activity. Senza Hilt
era l'unica soluzione per evitare che ogni NavBackStackEntry creasse una
nuova istanza con stato perso al popBackStack.

### 8.7 Lock visuale nei campi profilo (lato UI)

`SegmentedChips` (caiLevel) e `BirthDateField` (data nascita) accettano ora
`locked`/`enabled` per disabilitare interazione + alpha(0.6) quando il valore
è già stato salvato. Coerente con il blocco server.

### Stato build & test

- **Backend test:** 78/78 verdi (5 suite + 2 nuove: account.test.js, discriminator.test.js)
- **Mobile build:** `BUILD SUCCESSFUL` debug APK, 39 task
- **Lint:** non eseguito (SSL handshake fallisce in ambiente offline)

---

## 9. Sessione pomeridiana 26/05/2026 — Tier 1-4 (Audit Gemini closure)

Continuazione esplicita dall'utente per implementare i 4 tier rimasti pending
nell'`Audit_Tecnico_Jack.md`. Tutto green a fine sessione: **89/89 test backend,
`compileDebugKotlin` green mobile**, zero breaking change per client esistente.

### 9.1 Tier 1A — Route cleanup ridondante (post Global Error Mapper)

`BUSINESS_ERROR_MAP` esteso da 18 a 24 codici (aggiunti contestuali:
`WRONG_OLD_PASSWORD`, `INVITE_CODE_INVALID`, `ONLY_CREATOR_CAN_UPDATE_SESSION`,
`ONLY_CREATOR_CAN_DELETE_SESSION`, `ONLY_CREATOR_CAN_COMPLETE_SESSION`,
`ONLY_CREATOR_CAN_CANCEL_CHALLENGE`, `TOTEM_TAG_DUPLICATE`).

Fix bug in `resolveBusinessError`: il check parametrico `FIELD_LOCKED:*` veniva
dopo il `if (!mapped) return null` che faceva exit immediato → 2 test
anti-cheat fallivano. Spostato il check `FIELD_LOCKED` PRIMA del lookup map.

Service aggiornati per emettere i nuovi codici: `accountService.js`,
`hikeSessionService.js`, `challengeService.js`.

Route ridotti: **35+ blocchi `if (err.message === "...")` rimossi** dai 6 file
(accountRoutes, activityRoutes, nfcRoutes, quizRoutes, challengeRoutes,
hikeSessionRoutes). Tutti sostituiti da `next(err)` puro. Aggiunti `next` ai
signature handler dove mancava. Mantenuti i 2 blocchi semanticamente unici:
- `err.code === 11000` (E11000 duplicate key) in nfcRoutes per il messaggio
  contestuale "tagId già esistente"
- `err.name === "CastError"` in hikeSessionRoutes per 400 su ObjectId
  malformato (semantica diversa dal generic 500)

### 9.2 Tier 1B — Room migration helper pattern

Nuovo file `data/local/db/TsmMigrations.kt` come single source of truth per
le migration esplicite. Pattern documentato per i futuri bump:
1. Aggiungere `val MIGRATION_N_M = object : Migration(N, M) { ... }`
2. Aggiungerla all'array `ALL`
3. `Room.databaseBuilder.addMigrations(*TsmMigrations.ALL)` la prende automaticamente

`TsmApplication.kt` aggiornato per chiamare `.addMigrations(*TsmMigrations.ALL)`
PRIMA di `.fallbackToDestructiveMigration()`, così Room preferisce sempre la
migration esplicita al wipe distruttivo.

### 9.3 Tier 2 — `HikeSession.meetingDate` String → Date (con backward compat 100%)

Cambio schema: `meetingDate: { type: Date, set: parseMeetingDate }` con setter
che accetta sia "YYYY-MM-DD" (formato legacy del mobile) sia ISO 8601 / Date.
Aggiunto transform `toJSON`/`toObject` che converte Date → "YYYY-MM-DD" in
output, così il client mobile riceve esattamente la stessa stringa di prima.

Indice composto `{ status: 1, meetingDate: 1 }` per la query frequente
"sessioni dell'utente ordinate per data".

Joi validation: `meetingDateField` con `Joi.alternatives` accetta entrambi i
formati. Applicato a `createSessionSchema` e `updateSessionSchema`.

Migration script `backend/migrations/2026-05-26-meetingDate-string-to-date.js`:
- Connessione MongoDB via `MONGODB_URI` (o `MONGO_URI` fallback)
- Cursor su `hikesessions` con `meetingDate: { $type: 2 }` (BSON String)
- Parse "YYYY-MM-DD" → UTC midnight, update in place
- Idempotente (skip doc già Date, $type: 9)
- Report finale con counter migrated/skipped/errors + sample errori
- Lo script va eseguito MANUALMENTE prima di deployare il backend in prod
  (non runna in CI/server boot per evitare effetti collaterali)

3 nuovi test in `session.test.js`: formato output identico, BSON Date in DB,
sort cronologico funziona via `$sort: { meetingDate: 1 }`.

### 9.4 Tier 3 — Refresh token rotation con replay detection

**Backend:**
- Nuovo model `models/refreshToken.js`: hash SHA-256 (mai raw in DB),
  `family` UUID per rotation chain, `replacedBy` link per detection replay,
  TTL index 30 giorni (auto-cleanup MongoDB).
- Nuovo service `services/refreshTokenService.js`:
  - `generateAccessToken(user)` — JWT con `type: "access"` claim
  - `issueRefreshToken(userId, {family, userAgent})` — random 96 hex
  - `rotateRefreshToken(raw, {userAgent})` — valida, revoca, emette nuova
    coppia. **Detection replay**: se il token già revocato (replacedBy != null)
    viene riusato, revoca tutta la family → user deve fare re-login
    (assumiamo esfiltrazione).
  - `revokeRefreshToken(raw)` — logout single device (idempotente)
  - `revokeAllForUser(userId)` — logout su tutti i device
- `authService.js`:
  - `loginUser` ora emette `{ token, accessToken, refreshToken, refreshExpiresAt }`.
    Il campo `token` è alias backward-compat di `accessToken`.
  - Nuove `refreshTokens(req, res)` e `logout(req, res)`.
- `authRoutes.js`: `POST /auth/refresh` (con `loginLimiter` rate limit anti-brute)
  e `POST /auth/logout`.
- ACCESS_TTL configurabile via `JWT_ACCESS_TTL` env (default "15m"). Fallback
  su `JWT_EXPIRES_IN` per backward compat con env Render attuale.
- 8 nuovi test in `__tests__/routes/refreshToken.test.js`: login emit, family
  diverse per login multipli, rotation valida, refresh inventato → 401,
  refresh mancante → 400, replay attack → 401 + revoca family, logout idempotente.

**Mobile:**
- `TokenStorage.kt`: nuova `saveTokens(access, refresh, expiresAtIso)`,
  `getRefreshToken()`, `getRefreshExpiresAtIso()`. Backward compat con
  `saveToken(token)` legacy.
- `LoginResponse.kt`: campi nullable `accessToken`, `refreshToken`,
  `refreshExpiresAt`. Aggiunti `RefreshRequest` e `LogoutRequest`.
- `TsmApiService.kt`: `refresh()` e `logout()` endpoints.
- Nuovo `TsmAuthenticator.kt` (OkHttp `Authenticator`): intercetta 401, fa
  refresh sincrono via `OkHttpClient` interno (NO Authenticator chain →
  evita loop), salva la nuova coppia, ritenta la request originale con
  nuovo Bearer. **Trasparente per i ViewModel.** Mutex su refresh per
  evitare N refresh paralleli se più request scadono insieme.
- `TsmApiClient.kt`: `.authenticator(TsmAuthenticator(tokenStorage))`.
- `AuthRepositoryImpl.kt`: usa `saveTokens` invece di `saveToken`.

### 9.5 Tier 4 — `RegistraViewModel` refactor + WAL Room v5

**Bump Room v4 → v5** con migration esplicita `TsmMigrations.MIGRATION_4_5`
che CREATE TABLE `tracking_wal` + INDEX su `track_id`. Preserva tutti i dati
esistenti (zero perdita di `completed_activities` con `isSynced=0`).

Nuovo `data/local/db/TrackingWalEntity.kt` + `TrackingWalDao.kt`: WAL per i
punti GPS durante un tracking attivo. **Risolve crash-safety**: prima i punti
GPS vivevano SOLO in memoria nel `HikeTrackingEngine` — un crash perdeva
TUTTO. Ora ogni snapshot è un INSERT immediato.

**Estratti 2 repository:**
- `repository/TrackingPersistenceRepository.kt`: `startTrack()` (UUID),
  `appendPoint()` (insert WAL), `finalize(snapshot)` (legge WAL, sample 200pt,
  insert in `completed_activities`, cleanup WAL), `discardTrack()`.
- `repository/SessionCommandRepository.kt`: `markSessionActive(id)` (fire-and-
  forget PATCH status ACTIVE), `completeOrUpload(...)` (PATCH /complete o
  POST /activities a seconda di `sessionId`, con fallback a SyncManager se
  upload fallisce). Ritorna `SyncResult.Synced(remoteId?)` o `Pending`.

**`RegistraViewModel` refactor:**
- Da 547 → 501 righe + responsabilità chiare (orchestrator UI/lifecycle only).
- Nuovo field `currentTrackId: String?` — non-null sse `trackingStatus != IDLE`.
- `startTracking()` → `persistence.startTrack()`.
- `applyLocation()` → `persistence.appendPoint()` se RECORDING.
- `discardTracking()` → `persistence.discardTrack(orphanId)`.
- `confirmStopTracking()` → `persistence.finalize(snapshot)` → 
  `sessionCommands.completeOrUpload(...)` → `dao.markSynced()` se OK.
- Rimossi import non più usati: `Gson`, `CompletedActivityEntity`,
  `CreateActivityRequest`, `CompleteSessionRequest`, `UpdateSessionStatusRequest`,
  `ActualStats`, `HikeEstimation`, `SimpleDateFormat`, `Locale`, `Date`,
  `UUID`, `TsmApiClient`, `SyncManager`.

`LocationRepository` non estratto: la VM non aveva logica di search/nearby
(vive in `WeatherViewModel` e altri). Recovery dialog post-crash dalla WAL
rimane TODO Sprint 3.

### Stato finale build & test

- **Backend test:** 89/89 verdi (+11 vs sessione notturna: 3 meetingDate + 8 refresh)
- **Mobile build:** `compileDebugKotlin` BUILD SUCCESSFUL, solo deprecation warnings
- **Audit Gemini:** 9/9 azioni richieste implementate (8 fatte tra notte+pomeriggio,
  1 parziale già documentata)
