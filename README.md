# Trento Smart Mountain 🏔️

> **Branch `UI` — Sprint 1 Release**
>
> Ecosistema digitale per l'escursionismo sicuro in Trentino-Alto Adige.
> App Android nativa (Kotlin/Compose) + Backend Node.js + MongoDB + Meteo TINIA.

**Gruppo ID-6** — Federico Cattelan · Marco Christian Stoica · Giacomo Radin
**Corso**: Ingegneria del Software — Università di Trento · A.A. 2025/2026

---

## Indice

- [Cosa fa TSM](#cosa-fa-tsm)
- [Architettura](#architettura)
- [Stack tecnologico](#stack-tecnologico)
- [Prerequisiti](#prerequisiti)
- [Setup rapido](#setup-rapido)
- [Avvio backend](#avvio-backend)
- [Avvio app Android](#avvio-app-android)
- [Flusso demo Sprint 1](#flusso-demo-sprint-1)
- [API Endpoints](#api-endpoints)
- [Struttura repository](#struttura-repository)
- [Branching strategy](#branching-strategy)
- [Documentazione](#documentazione)

---

## Cosa fa TSM

Trento Smart Mountain supera le app di navigazione passiva (Komoot, AllTrails) integrando tre pilastri:

| Pilastro | Cosa fa | Sprint 1 |
|----------|---------|-----------|
| 🛡️ **Sicurezza attiva** | Tracciamento GPS in background, codici invito sessione, SOS con coordinate | ✅ GPS tracking, ⏳ SOS backend |
| 🎮 **Gamification** | Modello CAI di stima sforzo, punti per attività, quiz NFC vetta | ✅ Modello CAI, ⏳ Quiz NFC |
| 🏠 **Gestione rifugi** | Account rifugista, telemetria IoT, allerte meteo push | ✅ Account rifugio, ⏳ IoT |

### Flusso principale implementato (Sprint 1)

```
Registrazione → Verifica email (deep link tsm://) → Login JWT
      ↓
PIANIFICA escursione: importa GPX → statistiche automatiche → codice TSM-XXXX + QR
      ↓
UNISCITI (secondo utente): inserisci codice → vedi dettagli + meteo TINIA + checklist
      ↓
AVVIA → tracking GPS live (OSMdroid) → auto-pause da fermo → Stop → COMPLETED
      ↓
"Le mie attività": statistiche annuali + dettaglio con punti CAI calcolati
```

---

## Architettura

```
┌─────────────────────────┐   HTTPS/JWT    ┌──────────────────────┐
│   Android App           │ ◄────────────► │   Backend Node.js    │
│   Kotlin + Compose      │                │   Express + MongoDB   │
│   MVVM + Room offline   │                │   + Meteo TINIA       │
└─────────────────────────┘                └──────────────────────┘
         │                                           │
    EncryptedSharedPrefs                     Docker Compose
    Room SQLite cache                        (MongoDB + Mosquitto)
    ForegroundService GPS
```

**Principi**: Offline-First · Store-and-Forward · MVVM · JWT Bearer auth · 2dsphere geospatial

---

## Stack tecnologico

| Layer | Tecnologia | Versione |
|-------|-----------|---------|
| **Android** | Kotlin + Jetpack Compose + Material3 | Kotlin 2.0.21, BOM 2024.12 |
| **Navigation** | Jetpack Navigation Compose | 2.8.5 |
| **Networking** | Retrofit + OkHttp + Gson | 2.11 / 4.12 |
| **Local DB** | Room + KSP | 2.6.1 |
| **Security** | EncryptedSharedPreferences | security-crypto 1.1 |
| **Maps** | OSMdroid (OpenStreetMap) | 6.1.20 |
| **QR Code** | ZXing Core | 3.5.3 |
| **Drag & Drop** | sh.calvin.reorderable | 2.4.3 |
| **Backend** | Node.js + Express | 20.x / 4.x |
| **Database** | MongoDB + Mongoose | 7.x / 8.x |
| **Auth** | JWT (HS256) + bcrypt | — |
| **Email** | Nodemailer (Gmail SMTP) | — |
| **Meteo** | TINIA / meteo.report | — |
| **Infra** | Docker Compose | — |

---

## Prerequisiti

### Backend
- **Node.js** ≥ 20.x LTS
- **Docker Desktop** ≥ 4.x (MongoDB + Mosquitto)
- Account Gmail con **App Password** attiva (per SMTP email)

### Mobile
- **Android Studio** Hedgehog+ (Ladybug raccomandato)
- **JDK 17**
- Device/emulator Android API 28+ (Android 9.0+)

---

## Setup rapido

### 1. Clone

```bash
git clone https://github.com/giacomoradin/Trento_Smart_Mountain.git
cd Trento_Smart_Mountain
git checkout UI
```

### 2. Variabili d'ambiente backend

Crea `backend/.env`:

```env
MONGO_URI=mongodb://localhost:27017/tsm
JWT_SECRET=<genera-con: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))">
JWT_EXPIRATION_HOURS=168

PORT=3000

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=<tua-email-gmail>
SMTP_APP_PASSWORD=<app-password-16-chars>

DEEP_LINK_SCHEME=tsm
TINIA_API_URL=https://meteo.report/api
```

> **App Password Gmail**: attiva 2FA → `myaccount.google.com/apppasswords` → genera password per "Altra app".

### 3. Avvia MongoDB con Docker

```bash
docker compose up -d
```

### 4. Install dipendenze

```bash
npm install          # root
cd backend && npm install
```

---

## Avvio backend

```bash
cd backend
npm run dev
```

Output atteso:
```
[Server] Connesso a MongoDB: mongodb://localhost:27017/tsm
[Server] In ascolto sulla porta 3000
[Server] Swagger UI: http://localhost:3000/api-docs
```

### Seed database meteo (una tantum)

Il seeding meteo richiede un account admin. Crea l'utente admin una volta sola:

```bash
docker exec -it <mongo-container> mongosh
> use tsm
> db.users.insertOne({
    username: "admin@tsm.local",
    email: "admin@tsm.local",
    passwordHash: "$2b$10$rGn8Re9I5OQYbWAFiGT0OuJ3Yd4PVqAU/iEdc1G73lQ91DJSPC4t.",
    role: "admin",
    isVerified: true,
    createdAt: new Date()
  })
```
*(hash corrisponde a `admin123` — cambialo in produzione)*

Poi esegui il seed tramite Postman/cURL:
```bash
# 1. Login admin
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tsm.local","password":"admin123"}'

# 2. Seed con il token ottenuto
curl -X POST http://localhost:3000/weather/seed \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Avvio app Android

1. Apri la cartella `mobile/` con Android Studio.
2. Attendi la sincronizzazione Gradle.
3. Configura l'URL del backend in `mobile/app/src/main/java/.../data/remote/TsmApiClient.kt`:
   - **Emulator**: `http://10.0.2.2:3000` (già configurato di default)
   - **Device fisico sulla stessa rete**: `http://<ip-pc>:3000`
4. Run su device/emulator API 28+.

> **Permessi richiesti al primo avvio**: Posizione precisa · Posizione in background (Android 10+) · Notifiche (Android 13+)

---

## Flusso demo Sprint 1

### Prerequisiti demo
- Backend avviato + MongoDB seeded
- 2 account (capogruppo + partecipante) o un device + emulator
- File `.gpx` valido (es. `Catinaccio.gpx`)

### Passo 1 — Autenticazione
1. App → **Registrati** → inserisci email + password → tap **Conferma**
2. Apri l'email ricevuta → tap sul link `tsm://auth/verify/...` → app si apre con email precompilata → **Accedi**

### Passo 2 — Pianifica escursione (account capogruppo)
1. Tab **Sessione** → sotto-tab **PIANIFICA**
2. **Carica GPX** → seleziona file → vedi stats calcolate (distanza, dislivello, durata CAI, punti stimati)
3. Compila: nome, data, ora, difficoltà, max partecipanti → **Crea sessione**
4. Dialog: codice `TSM-XXXX` + QR → copia codice

### Passo 3 — Unisciti (account partecipante)
1. Tab **Sessione** → sotto-tab **UNISCITI**
2. Inserisci il codice `TSM-XXXX` → **Unisciti**
3. Tap sulla sessione → **SessionDetailScreen**: profilo altimetrico reale, meteo TINIA 3h/24h, checklist drag-and-drop, partecipanti con avatar

### Passo 4 — Avvia tracking (capogruppo)
1. Apri la sessione come **creator** → tap **▶ AVVIA ESCURSIONE**
2. Switch automatico alla tab **Registra** → mappa OSMdroid + metriche live
3. Cammina → vedi distanza/dislivello/quota/tempo aggiornati in real-time
4. Tap **Stop** → dialog salvataggio → conferma nome attività → **Salva**

### Passo 5 — Le mie attività
1. Tab **Home** → sotto-tab **Le mie attività**
2. Card statistiche annuali + lista attività con metriche + punti CAI calcolati

### Passo 6 — Swagger (facoltativo)
Apri `http://localhost:3000/api-docs` → esplora tutti gli endpoint implementati.

---

## API Endpoints

Documentazione completa → Swagger: `http://localhost:3000/api-docs`
Reference human-readable → [`docs/api_reference.md`](./docs/api_reference.md)

### Panoramica

| Categoria | Endpoint | Auth |
|-----------|----------|------|
| **Auth** | `POST /auth/login` | — |
| | `GET /auth/verify/:token` | — |
| | `POST /auth/forgot-password` | — |
| | `GET/POST /auth/reset-password/:token` | — |
| **Users** | `POST /users` | — |
| | `GET /users/:id` | JWT |
| | `PUT/DELETE /users/:id` | JWT + admin |
| **Sessions** | `POST /api/v1/sessions` | JWT |
| | `GET /api/v1/sessions/my` | JWT |
| | `GET /api/v1/sessions/:id` | JWT |
| | `GET /api/v1/sessions/stats?year=` | JWT |
| | `POST /api/v1/sessions/join` | JWT |
| | `POST /api/v1/sessions/:id/leave` | JWT |
| | `PATCH /api/v1/sessions/:id` | JWT (creator) |
| | `PATCH /api/v1/sessions/:id/status` | JWT (creator) |
| | `DELETE /api/v1/sessions/:id` | JWT (creator) |
| **Weather** | `GET /weather/locations/nearby` | — |
| | `GET /weather/locations/search` | — |
| | `GET /weather/forecast/:externalId` | — |
| | `POST /weather/forecast/:externalId/refresh` | JWT + admin |
| | `POST /weather/seed` | JWT + admin |

---

## Struttura repository

```
Trento_Smart_Mountain/
├── backend/
│   └── src/
│       ├── app.js                  # Express app + middleware + routing
│       ├── server.js               # Entry point + MongoDB connect
│       ├── middleware/             # authMiddleware, authorizationMiddleware, errorMiddleware
│       ├── models/                 # Mongoose: user.js, hikeSession.js, location.js
│       ├── routes/                 # authRoutes, userRoutes, hikeSessionRoutes, weatherRoutes
│       └── services/               # authService, hikeSessionService, weatherService, emailService
│
├── mobile/
│   └── app/src/main/java/it/trentosmartmountain/app/
│       ├── data/
│       │   ├── estimation/         # HikeEstimation.kt (formule CAI + punti TSM)
│       │   ├── location/           # HikeTrackingEngine, UserLocationTracker, TrackingLocationBus,
│       │   │                         StationaryDetector, ForegroundTrackingService
│       │   ├── remote/             # TsmApiService.kt, TsmApiClient.kt, DTO
│       │   ├── session/            # SessionStartCoordinator.kt (bus AVVIA→Registra)
│       │   └── local/              # TokenStorage, AuthSession, Room DB, DAOs
│       ├── ui/
│       │   ├── navigation/         # Routes.kt, TsmNavHost.kt
│       │   ├── screens/            # AuthEntry, Login, Register, SessionHub, SessionDetail,
│       │   │                         Registra, Home, Profile, Refuge, ...
│       │   └── theme/              # TsmPrimary, TsmAccent, TsmSos, darkColorScheme
│       └── viewmodel/              # LoginVM, RegisterVM, SessionPlanVM, SessionJoinVM,
│                                     SessionDetailVM, RegistraVM, ActivityListVM, ...
│
├── docs/
│   ├── T6_D3_Ingegneria_Del_Software.md  # Deliverable D3 (LaTeX)
│   ├── TSM_PROJECT_STATE.md               # Stato progetto + audit + roadmap
│   ├── api_reference.md                   # API reference human-readable
│   ├── architecture.md                    # MVVM, patterns, ADR
│   ├── database_schema.md                 # Schemi MongoDB + Room
│   ├── setup_backend.md                   # Setup backend passo-passo
│   ├── setup_mobile.md                    # Setup mobile Android Studio
│   ├── test_cases_sprint1.md              # 20 Test Cases Sprint 1 (design)
│   └── sprint2_plan.md                    # Piano Sprint 2
│
├── iot/                            # Gateway MQTT rifugio (Sprint 3+)
├── docker-compose.yml              # MongoDB + Mosquitto
├── swagger.js                      # Generazione swagger-output.json
└── swagger-output.json             # API spec OpenAPI 3.0
```

---

## Branching strategy

Il progetto segue **Git Flow semplificato**:

| Branch | Scopo | Stato |
|--------|-------|-------|
| `main` | Release stabili — solo merge da PR approvate | Stabile |
| `UI` | Branch di integrazione Sprint 1 | **Attivo** |
| `API-Meteo-Integration` | Feature branch meteo TINIA | Mergiato |
| `auth-login-jwt` | Feature branch autenticazione JWT | Mergiato |
| `crud-mongodb` | Feature branch User CRUD | Mergiato |
| `Swagger-setup` | Feature branch documentazione OpenAPI | Mergiato |
| `18-gestione-sessione-escursione` | Feature branch Issue #18 | Mergiato |
| `Reorganizatio-Repo-Structure` | Refactor struttura cartelle | Mergiato |
| `bugfix/*` | Branch bugfix dedicati | Sprint 2+ |

> **Nota**: i branch non vengono eliminati dopo il merge per permettere la verifica della storia di sviluppo.

**Convenzioni commit**: [Conventional Commits](https://www.conventionalcommits.org/) — `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`

---

## Documentazione

| Documento | Descrizione |
|-----------|-------------|
| [`docs/api_reference.md`](./docs/api_reference.md) | Reference completa di tutti gli endpoint con esempi |
| [`docs/architecture.md`](./docs/architecture.md) | Pattern MVVM, Repository, ADR architetturali |
| [`docs/database_schema.md`](./docs/database_schema.md) | Schema MongoDB (users, hikesessions, locations) + Room |
| [`docs/setup_backend.md`](./docs/setup_backend.md) | Setup backend passo-passo con troubleshooting |
| [`docs/setup_mobile.md`](./docs/setup_mobile.md) | Setup Android Studio e configurazione |
| [`docs/test_cases_sprint1.md`](./docs/test_cases_sprint1.md) | 20 Test Cases Sprint 1 con esiti |
| [`docs/sprint2_plan.md`](./docs/sprint2_plan.md) | Backlog e plan Sprint 2 |
| [`docs/TSM_PROJECT_STATE.md`](./docs/TSM_PROJECT_STATE.md) | Stato progetto, audit codebase, roadmap |
| [`swagger-output.json`](./swagger-output.json) | Spec OpenAPI 3.0 (UI su `/api-docs`) |

---

## Team

| Nome | Ruolo Sprint 1 | GitHub |
|------|---------------|--------|
| Federico Cattelan (242111) | Mobile: GPS tracking, OSMdroid, ForegroundService, Room | [@federicocattelan](https://github.com/federicocattelan) |
| Marco Christian Stoica (246443) | Backend: API, MongoDB, meteo TINIA, Swagger | [@STUSSY-user](https://github.com/STUSSY-user) |
| Giacomo Radin (242907) | Mobile: UI/UX, sessioni, GPX parser, auth flow | [@giacomoradin](https://github.com/giacomoradin) |

---

## Licenza

© 2026 Gruppo ID-6 — Università di Trento.
Progetto accademico — tutti i diritti riservati.
