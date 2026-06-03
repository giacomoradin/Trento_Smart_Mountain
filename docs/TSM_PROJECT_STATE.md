# Trento Smart Mountain — Stato del progetto

## ⏱️ Aggiornamento Sprint 3 — giugno 2026

> Build mobile **`compileDebugKotlin` green**; backend **220/220 test verdi** (15 suite).

Funzionalità aggiunte dopo lo Sprint 2:

**Social (completo)**
- Ricerca/scoperta utenti, liste follower/seguiti navigabili.
- Metriche escursionistiche sul profilo (km/dislivello/uscite/punti) + classifica settimanale tra i seguiti.
- **Notifiche** in-app (follow/like/commento) con centro notifiche + badge non-letti + deep-link.
- Badge "Ti segue"; **gate privacy** profilo (`profileVisibility` applicato in `getHikerById` + feed + bacheca).

**Rifugio — Dashboard IoT (mock)**
- Modelli `EdgeNode`, `RefugeSensorReading`, `RefugePassage` + seed mock + `GET /api/v1/refuge/dashboard`.
- UI dashboard fedele al mockup (sensori, edge nodes BLE-mesh, passaggi/social-credit) + scheda profilo rifugista.
- ⚠️ Nessun ingest MQTT reale: dati generati lato server, schema definitivo.

**Bacheca rifugi**
- Modello `RefugeBoardPost` (info/avviso/pericolo, `validUntil`) + CRUD `/api/v1/board` Joi-validato.
- Composizione lato rifugista (crea/modifica/elimina) + consultazione utenti (icona in Home/Pianificazione/Registra).

**Altro**: fix bug attività (paginazione feed, cancel sfida, eliminazioni persistenti); pipeline CI; 0 vulnerabilità npm; copertura test backend 88 → 220.

> _Polish residuo (non bloccante): estrazione stringhe i18n delle schermate nuove, unificazione design-token su `TsmColors`, skeleton loaders._

### 🐛 Fix UI/UX mobile — giugno 2026 (sessione fix)

Correzioni mirate (incluse regressioni **non ancora notate dall'utente**):

- **Build mobile ripristinata**: `:app:compileDebugKotlin` non compilava più per regressioni pre-esistenti — import mancanti in `FeedCard.kt` (`fillMaxSize`/`clip`/`CircleShape`), riferimento errato `FeedUser.userId` (→ `_id`) e mismatch di tipo `List<Any>` vs `List<RoutePoint>` in `SessionHubScreen`. Risolte.
- **Traccia GPX + altimetria come due schede swipe** — nuovo componente unico `ui/components/TsmRouteElevationPager.kt`: 1ª pagina = traccia su **mappa** OpenTopoMap con **inizio = cerchio verde** e **fine = bandiera a scacchi** (via `TsmRouteMapPreview`), 2ª pagina (swipe a destra) = **profilo altimetrico** (`ElevationSparkline`). Adottato in **feed, dettaglio social, dettaglio attività e Unisciti (lista + dettaglio sessione)**: elimina l'incoerenza per cui alcune attività pianificate mostravano solo l'altimetria e altre solo la traccia, e rimuove le copie divergenti/bacate del pager.
- **Avatar utenti coerenti + tap → profilo social ovunque**: i partecipanti del **Dettaglio Attività** (prima iniziali statiche, non cliccabili) e del **Dettaglio Sessione**, e gli **autori dei commenti** (`CommentsBottomSheet`), ora usano `AvatarImage` (foto profilo) e al tap aprono il profilo social. `onUserClick` propagato fino a `SessionDetailScreen`, `ActivityDetailScreen`, `UserProfileScreen` e alle destinazioni in `TsmNavHost`. Le liste social (ricerca, follower/seguiti, classifica, notifiche, feed, hub sessioni) erano già conformi. _Fuori scope_: gli sheet di tracking live (`LiveParticipantSheet`/`GroupRosterMenu`) restano "schede" del partecipante sulla mappa, non navigano al profilo social.
- **Arresto sessione da "Unisciti" con salvataggio**: arrestando una sessione **mentre il tracking è attivo**, ora compare lo stesso dialog "Salva attività" del tasto **Termina** in Registra (Salva / Scarta / Annulla) e l'escursione viene **salvata**; prima l'attività veniva **persa** (`detachFromLiveTracking`). Implementazione: il `SessionStopCoordinator`, se c'è un tracking in corso, instrada verso `requestStopTracking()` (anziché lo stacco silenzioso); `HikerMainScreen` osserva `showStopConfirm` e porta l'utente sulla tab Registra dove vive il dialog; `SessionDetailScreen` torna alla shell allo stop così il dialog è visibile.

> Backend / API / DB **invariati** (nessuna modifica a endpoint o schema): `api_reference.md` e `database_schema.md` restano allineati.

### 🚀 Stories reali + Approvazione partecipanti + Sesso visibile — giugno 2026

> Build mobile **`compileDebugKotlin` green**; backend **238/238 test verdi (17 suite)**.

Epic a fasi (0→A→B→C→D):

**Fase 0 — Sesso partecipanti**: `getLiveLocations` invia `personalInfo.sex` a **tutti i membri** (non solo capogruppo); mostrato in `LiveParticipantSheet` e `GroupRosterMenu`.

**Fase A — Join sessione con approvazione/rimozione**
- `participants[]` esteso con `status` (`pending`/`accepted`, default accepted per retrocompat) + `approvedBy`; nuovo `removedUserIds[]` (ban locale alla sessione).
- `joinSession` ora crea una **richiesta pending** (+ check ban). Nuovi endpoint: `POST /sessions/:id/participants/:userId/approve` e `/reject` (capogruppo **o** un partecipante già accettato), `DELETE /sessions/:id/participants/:userId` (rimozione definitiva, **solo capogruppo**).
- Mobile: `ParticipantsCard` dinamica (accettati + sezione **In attesa** con accetta/rifiuta, "accettato da X", rimuovi per il capogruppo); join → feedback "richiesta inviata". +7 test backend.

**Fase B — Sistema Stories reale (sostituisce la derivazione dai post)**
- Nuovo model `Story` (TTL 24h via index): `type` (`planned_session`/`activity`), `sessionId`/`activityId`, `inviteCode` snapshot, `caption`, `media[]` (Base64 foto/video capped), `overlay` (titolo/distanza/dislivello/tempo/traccia), `viewers[]`.
- Endpoint `/api/v1/stories`: `POST`, `GET /user/:userId`, `POST /:id/view`, `DELETE /:id` (auth + rate limit + Joi con cap dimensione media). `getSocialRowForUser` ora deriva l'anello "story" dalle Story reali (+ `hasUnviewedStory`). +8 test backend.

**Fase C — Stories mobile**
- `StoryViewerScreen` riscritto: carica le storie reali dell'autore (`/stories/user/:id`), progress segmentata + auto-advance, **media reali** (foto da Base64, video breve da cache file + `VideoView`), overlay tracciamento, bottone **"Unisciti"** per le storie `planned_session`, `markViewed` per segmento.
- `StoryComposerScreen` + VM: picker foto/video (PhotoPicker, niente permessi), encoding Base64 (immagine compressa via `AvatarUtils`, video con cap), pubblicazione. Entry point **"Condividi come storia"** in `ActivityDetailScreen` (post-hike) e `SessionDetailScreen` (pre-hike).
- AvatarRow / Home / Nav ricablati: le storie si aprono **per autore** (`STORY_VIEWER = story_viewer/{userId}`); nuovo `STORY_COMPOSER` con holder `pendingStoryDraft`.

**Fase D — Media/permessi**: scelta gallery PhotoPicker → **nessun permesso** runtime aggiuntivo necessario; cap media coerenti client/server (immagine ≤ ~1.5MB, video ≤ ~3.5MB, sotto il body limit 5mb).

> Media decisi con l'utente: foto + **video breve capped** in Base64 (no object storage nello stack). Doc API/DB aggiornati in `api_reference.md` e `database_schema.md`.

---

> Snapshot dell'architettura, codebase e implementazione corrente. Aggiornato al **26 maggio 2026 (sessione serale)** — Sprint 2 con feature **Foto profilo utente** completata (privacy gate fix, componente Compose `AvatarImage` riusabile, EXIF rotation, foto visibile in ProfileScreen / ProfileViewScreen / partecipanti sessioni), oltre alle tre sessioni precedenti dello stesso giorno (notturna: discriminator persistence + anti-cheat; pomeridiana: refresh token rotation + WAL Room v5). Build mobile **`compileDebugKotlin` green**, backend **88/89 test verdi** (1 test fragile pre-esistente su `BREVO_API_KEY`).

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
- ✅ ActivityDetailScreen: metric grid, mappa preview (TsmRouteMapPreview con OSMdroid), partecipanti, profilo altimetrico, timeline split km, performance badges (Alpinista, Maratoneta), export GPX
- ✅ PostDetailScreen: dettaglio social avanzato con timeline split ogni 5km e performance badges per un'esperienza "Strava-like".
- ✅ Nuova rotta `POST_DETAIL` in `TsmNavHost.kt` e `Routes.kt` per la navigazione nel feed social.
- ✅ TsmRouteMapPreview: nuovo componente basato su OSMdroid che renderizza il tracciato GPX sopra una mappa topografica reale, sostituendo la vecchia RouteTracePreview statica. Integrato in FeedCard, ActivityDetailScreen e PostDetailScreen.
- ✅ Delete con cleanup remoto per attività libere (DELETE /activities/:id)

#### Sync engine

- ✅ SyncManager: coroutine loop 60s + backoff incrementale per record (1m → 5m → 30m → 1h cap)
- ✅ Fix bug GPX: aggiunto `parseRoutePolyline` in `SyncManager.kt` per garantire l'invio della `routePolyline` durante il sync delle attività libere (risolve tracce mancanti nel feed social).
- ✅ `enqueueImmediate()` per pull-to-refresh manuale
- ✅ Room v4 con campi `retry_count`, `last_retry_at_ms`, `remote_id`
- ✅ Marcatura `isSynced=1` post upload + tracking `remoteId` per delete cross-device

#### Foto profilo (avatar) — sessione serale 26/05

- ✅ Schema: `Hiker.personalInfo.avatarUrl` (data URI Base64, max 7 MB lato Joi su body cap 5 MB)
- ✅ Endpoint riusato: `PATCH /api/v1/users/me/personal-info` con `avatarUrl` opzionale
- ✅ Privacy gate (`userPrivacy.js`): per "other viewer" è pubblico SOLO `personalInfo.avatarUrl`; gli altri campi (sex, birthDate, heightCm, weightKg) restano privati
- ✅ Populate sessioni: `participants.userId` e `creatorId` includono `personalInfo.avatarUrl` (8 occorrenze in `hikeSessionService.js`)
- ✅ Joi validation: pattern stretto `^data:image/(jpeg|jpg|png|webp);base64,...$`, accetta `""` per il flow "rimuovi foto"
- ✅ Mobile: componente Compose riusabile `ui/components/AvatarImage.kt` (decode Base64 memoizzato via `remember(avatarUrl)`, fallback iniziali con colore deterministico, overlay loader)
- ✅ Mobile: utility `ui/util/AvatarUtils.kt` (load URI → EXIF rotation → downscale 500 px → JPEG q70 → Base64 NO_WRAP, tutto su `Dispatchers.IO`)
- ✅ UX: long-press sull'avatar in ProfileScreen → dialog "Rimuovi foto" + Toast su success/error; icona `CameraAlt` come hint di tap
- ✅ Foto visibile in: `ProfileScreen` (64 dp), `ProfileViewScreen` (88 dp header), `SessionDetailScreen` PartecipantsCard (40 dp con bordo accent per creator)
- ✅ Dipendenza aggiunta: `androidx.exifinterface:1.3.7`

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

- ⚠ Logging strutturato + Sentry (oggi solo `console.log/error`)
- ⚠ Audit trail per azioni admin (chi ha eliminato chi, change-role events)
- ⚠ Rotazione automatica JWT secret (oggi manuale, vedi SECURITY.md sez. 6)
- ⚠ Tombstone table per delete offline-first (oggi best-effort via `remoteId`)
- ⚠ Rate limit con store Redis (oggi in-memory, ok per Render single-instance)
- ⚠ CI gate su `npm audit` (oggi 6 moderate severity da risolvere)
- ⚠ Coverage Jest da estendere ai service layer (oggi: route auth/hiker/sessions/activities/weather/account/refreshToken coperte; service layer indiretto via route)
- ⚠ Test fragile `POST /auth/register/hiker` dipende da `BREVO_API_KEY` env in test → mockare `emailService.sendVerificationEmail` per renderlo hermetico (1 test su 89 in failure per questo)
- ⚠ Recovery dialog post-crash dalla WAL (`tracking_wal`): l'infra c'è (Room v5, `TrackingPersistenceRepository.finalize`), manca solo il prompt UX alla riapertura per recuperare l'attività interrotta
- ⚠ Avatar: serializzato come Base64 inline nei `participants.userId.personalInfo.avatarUrl` → payload pesante (~30-100 KB per partecipante). Tradeoff accettato per Sprint 2; in Sprint 3 considerare endpoint dedicato `/users/:id/avatar` con cache headers

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
- [x] Foto profilo utente (avatar) end-to-end — sessione serale 26/05: privacy gate fix, componente `AvatarImage` riusabile, EXIF rotation, foto visibile in ProfileScreen/ProfileViewScreen/PartecipantsCard, long-press per rimuovere
- [ ] D3 documentation (in scrittura)
- [ ] M4 (Milestone 4) — deadline 07/06/2026, scheletro `docs/M4_ID6_Ingegneria_del_Software.md` creato in worktree `.claude/`

### Sprint 3 (planned)

- [ ] BLE Mesh SOS prototype
- [ ] OAuth Google login
- [ ] Sentry integration + logging strutturato (Pino)
- [ ] CI con `npm audit` gate
- [ ] Recovery dialog post-crash WAL (UX prompt per riprendere il tracking interrotto)
- [ ] WorkManager per sync robusto anche quando OS killa l'app
- [ ] CMS web admin per quiz (oggi seed JSON in repo)
- [ ] Modalità gara quiz (timer per domanda + leaderboard)
- [ ] Endpoint dedicato `/users/:id/avatar` (estrazione blob dai populate sessione, cache headers ETag)
- [ ] Audit trail admin (chi ha eliminato chi, change-role events)
- [ ] Rate limit con store Redis (preparazione per multi-instance Render paid)

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
  - `markProfileCompleted`
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

---

## 10. Sessione serale 26/05/2026 — Feature foto profilo end-to-end

Sessione richiesta esplicitamente dall'utente ("sto cercando di implementare la
foto profilo con scarso successo, aiutami a sistemarla"). Il lavoro pregresso
sul branch `UI` (5 commit avatar) aveva già messo le fondamenta ma con problemi
strutturali che impedivano alla foto di apparire correttamente, soprattutto
nelle altre schermate. Tutti i fix completati in-session.

### 10.1 Sintomo originale e root cause

**Sintomo riportato:** "Upload OK ma foto non appare."

**Root cause (3 cause concorrenti, tutte fixate):**

1. **`BitmapFactory.decodeByteArray` ritornava `null` → Box vuoto** — il codice
   inline in `ProfileScreen.kt` aveva `if (bitmap != null) Image(...)` ma
   nessun ramo `else` con fallback. Una qualsiasi decode fallita (Base64
   corrotto, char extra) lasciava un cerchio vuoto.
2. **Decodifica Base64 ad ogni ricomposizione su main thread** — non c'era
   `remember(avatarUrl)` → ogni cambio di stato ridecodificava ~100 KB di
   bytes su UI thread (jank + spreco batteria).
3. **`personalInfo` response merge debole** — se il body del `PATCH` arrivava
   troncato o senza il sub-document completo, lo state restava col vecchio
   `personalInfo` (senza il nuovo avatarUrl) e l'UI mostrava le iniziali.

### 10.2 Fix critici backend

#### `User.avatarUrl` morto rimosso

`backend/src/models/user.js`: il campo `avatarUrl` era nello schema base ma
**nessuno scriveva/leggeva** lì (tutti gli write andavano su
`Hiker.personalInfo.avatarUrl`). Source of truth ora univoca →
`Hiker.personalInfo.avatarUrl`.

#### Privacy gate (`utils/userPrivacy.js`)

`stripPrivateFields` cancellava l'**intero** `personalInfo` per gli "other
viewer", impedendo di mostrare l'avatar nei partecipanti delle sessioni.

**Fix:** introduzione di `PERSONAL_INFO_PUBLIC_FIELDS = ["avatarUrl"]`. Per
viewer "other" ora:

- `personalInfo` mantiene solo `avatarUrl` (gli altri campi sex/birthDate/
  heightCm/weightKg restano privati)
- Se nessun campo pubblico è valorizzato → la chiave viene rimossa per non
  sporcare la response con un oggetto vuoto

#### Populate sessioni

`backend/src/services/hikeSessionService.js`: 8 occorrenze di
`populate(..., "username email")` → `"username email personalInfo.avatarUrl"`
(creatorId + participants.userId in createSession, getSessionById,
getSessionsByUser, updateSessionDetails).

#### Validazione Joi stretta

`backend/src/middleware/validationMiddleware.js`: nuovo
`avatarDataUriField` con pattern stretto
`^data:image/(jpeg|jpg|png|webp);base64,[A-Za-z0-9+/=]+$` + messaggi custom.
Accetta `""` per il flow "rimuovi foto" (`.allow(null, "")` bypassa il
pattern, by design Joi).

#### Body limit a 5 MB

`backend/src/middleware/securityMiddleware.js`: `requestSizeLimit` portato
da "2mb" a "5mb" (commit del lavoro pregresso uncommitted in branch UI).
Lascia margine per foto 500 px JPEG q70 anche a quality più alta in futuro.

### 10.3 Componente mobile riusabile

#### `ui/components/AvatarImage.kt` (nuovo)

Composable circolare riusabile in tutta l'app:

- **Decode Base64 memoizzato** con `remember(avatarUrl)` → un solo decode per
  ogni valore distinto di URL (risolve causa #2).
- **Fallback iniziali** se decode fallisce o URL è null/blank (risolve causa #1).
- **Colore di sfondo deterministico** dal hash dello username — stesso utente
  sempre stesso colore (palette 8 tonalità outdoor).
- **Overlay loader** (parametro `isLoading: Boolean`) → CircularProgressIndicator
  bianco su sfondo semi-trasparente sopra l'avatar quando il VM sta uploadando
  o rimuovendo.
- **Helpers visibili al test**: `initialsFrom(name)`, `deterministicAvatarColor(seed)`.

#### `ui/util/AvatarUtils.kt` (nuovo)

Utility per gestire la foto end-to-end:

- `loadOrientedBitmapFromUri(resolver, uri)`: legge bytes, parsea EXIF tag
  `TAG_ORIENTATION`, applica `Matrix.postRotate` (gestisce ROTATE_90/180/270 +
  FLIP_HORIZONTAL/VERTICAL + TRANSPOSE/TRANSVERSE). Risolve foto camera in
  portrait che apparivano ruotate.
- `downscaleToBox(bitmap, maxSide=500)`: usa il lato **maggiore** (non solo
  width come il vecchio codice) così foto verticali non restano enormi sull'altezza.
- `encodeToDataUri(bitmap, q=70)`: JPEG + `Base64.NO_WRAP` (no newline → safe
  per JSON + regex Joi).
- `decodeDataUri(dataUri)`: robusto sui prefissi (taglia fino a `base64,`),
  ritorna `null` invece di crashare.
- `prepareAvatarForUpload(resolver, uri)`: pipeline completa che il
  `ProfileScreen` invoca su `Dispatchers.IO`.

### 10.4 Wiring nelle schermate

#### `ui/screens/profile/ProfileScreen.kt`

- Photo picker (`ActivityResultContracts.GetContent`) ora delega ad
  `AvatarUtils.prepareAvatarForUpload` in `withContext(Dispatchers.IO)`.
- Avatar 64 dp interattivo: `combinedClickable` con tap = picker, long-press =
  dialog "Rimuovi foto" (visibile solo se `hasAvatar`).
- Icona badge cambiata da `Settings` a `CameraAlt` per chiarezza UX.
- `LaunchedEffect` su `sectionSuccess/sectionError` → Toast + clear messages.

#### `ui/screens/profile/ProfileViewScreen.kt`

- Aggiunto header row con `AvatarImage` 88 dp + username + email sopra le
  sezioni dati. Prima era solo testo.

#### `ui/screens/session/SessionDetailScreen.kt` (PartecipantsCard)

- Sostituito il Box con iniziali colorate (`avatarColorFor` rimosso) con
  `AvatarImage` 40 dp. Wrapper esterno mantiene il bordo accent per il
  creator senza interferire con il clip circolare.
- Foto reale dei partecipanti ora visibile grazie al populate aggiornato
  lato backend (sezione 10.2).

### 10.5 ViewModel changes

`ProfileV2ViewModel.kt`:

- **`uploadAvatar(dataUri)`** ora merge-safe: se `resp.body()?.personalInfo`
  è null (caso patologico response gzip troncata), fa fallback al
  `_state.personalInfo?.copy(avatarUrl = dataUri)` invece di perdere il
  nuovo URL (risolve causa #3 della root cause).
- **`removeAvatar()` (nuovo)**: optimistic update (UI mostra subito le
  iniziali) + rollback automatico se il server risponde errore. Invia
  `avatarUrl=""` (accettato da Joi via `.allow("")`).

### 10.6 Build & dipendenze

- Nuova dep: `androidx.exifinterface:1.3.7` in `libs.versions.toml` +
  `app/build.gradle.kts`.
- DTO update: `SessionUserInfo` ora ha `personalInfo: SessionUserPersonalInfo?`
  con helper `.avatarUrl` (proxy del campo nested).

### Stato finale build & test (sessione serale)

- **Backend test:** 88/89 verdi (1 test `POST /auth/register/hiker` fallisce
  per `BREVO_API_KEY` mancante in env di test — **pre-esistente**, nessuna
  delle 5 modifiche backend tocca quel path; mockare `emailService` lo
  renderebbe hermetico, è in tech debt).
- **Mobile build:** `compileDebugKotlin` BUILD SUCCESSFUL in 1m 7s, solo
  deprecation warnings pre-esistenti (TokenStorage EncryptedSharedPreferences,
  Room `fallbackToDestructiveMigration`, alcune Icons.Outlined → AutoMirrored).
- **File toccati totali:** 5 backend (user.js, userPrivacy.js,
  hikeSessionService.js, validationMiddleware.js, securityMiddleware.js)
  - 6 mobile (SessionResponse.kt, ProfileV2ViewModel.kt, ProfileScreen.kt,
    ProfileViewScreen.kt, SessionDetailScreen.kt, libs.versions.toml +
    app/build.gradle.kts) + 2 nuovi mobile (AvatarImage.kt, AvatarUtils.kt).

### Lezione di processo (consolidata da Sprint 1 + Sprint 2)

Anche stavolta vale il pattern visto nelle sessioni notturna e pomeridiana
del 26/05: **un sintomo riportato in UI (qui "foto non appare") aveva 3
cause concorrenti** (no fallback decode, no memoization, weak state merge),
solo una delle quali era ovvia dal codice. Senza l'audit a 2 passi prima
del commit, due delle tre sarebbero rimaste in produzione e il "bug" sarebbe
ricomparso a colpi singoli su utenti diversi.
