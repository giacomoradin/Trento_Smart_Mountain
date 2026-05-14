# Trento Smart Mountain — Stato del Progetto e Piano Implementativo

> Documento di riferimento per lo sviluppo. Descrive la codebase attuale, gli obiettivi da D1/D2, il gap tra progettazione e implementazione, e il piano delle modifiche.
>
> Ultimo aggiornamento: 2026-05-14

---

## 1. Visione e Obiettivi del Progetto

Trento Smart Mountain (TSM) e un ecosistema digitale per l'ambiente montano trentino. Supera le app di navigazione passiva (Komoot, AllTrails) integrando **sicurezza attiva dei gruppi** (mesh BLE offline, SOS), **gamification educativa** (quiz, NFC checkpoint vetta, crediti sociali) e **gestione rifugi** (IoT, telemetria).

### 1.1 Macro-obiettivi (D1 §1)

| ID  | Obiettivo                             | Descrizione                                                    |
| --- | ------------------------------------- | -------------------------------------------------------------- |
| O1  | Ecosistema digitale a valore aggiunto | Aggregatore community per tutte le stagioni                    |
| O2  | Sicurezza proattiva                   | Tracciamento e coordinamento gruppi escursione                 |
| O3  | Resilienza comunicativa               | Comunicazione tra dispositivi in assenza di rete (BLE Mesh)    |
| O4  | Sostenibilita ed Economia Circolare   | Gamification educativa, NFC checkpoint, crediti sociali        |
| O5  | Coinvolgimento multisettoriale        | Cittadini, turisti, guide, gestori rifugi, operatori ecologici |

### 1.2 Pivot D2 rispetto a D1

In D2 e stata **rimossa** la raccolta fisica dei rifiuti da parte degli escursionisti (RF15-RF18 di D1) per ragioni di sicurezza e profilassi. Il framework di sostenibilita si concentra ora su:

- **Gamification educativa** (Sustainability Paths): quiz su flora, fauna, sicurezza
- **Certificazione di vetta via NFC**: totem fisici ai checkpoint, scansione per crediti
- **Crowdsourcing segnalazioni**: manutenzione sentieri

---

## 2. Attori del Sistema (D1 §3)

| Attore                            | Ruolo                                                                   | Ruolo DB                                   |
| --------------------------------- | ----------------------------------------------------------------------- | ------------------------------------------ |
| **Partecipante (Escursionista)**  | Utente base: mappa, bussola, SOS                                        | `groupLeader` (default nel codice attuale) |
| **Capogruppo (Guida Alpina/CAI)** | Crea sessioni, dashboard tracking, gestisce emergenze                   | `groupLeader`                              |
| **Gestore Rifugio**               | Sentinella del territorio, IoT, associa rifiuti (ora: gestione rifugio) | `rifugio`                                  |
| **Operatore Ecologico**           | Validazione smaltimento via web-app                                     | Non implementato                           |
| **Amministratore**                | Gestione promozioni, crediti, permessi                                  | `admin`                                    |

**Sistemi Esterni**: Open Data Trentino/OSM (cartografia), API MeteoTrentino (meteo real-time).

---

## 3. Requisiti Funzionali (D1 §4)

### Trasversali

- **RF0**: Autenticazione per accedere al servizio

### Utente Dinamico (Partecipante + Capogruppo)

- **RF1**: Selezionare itinerario da Open Data
- **RF2**: Mostrare difficolta tecnica e dislivello
- **RF3**: Consigli equipaggiamento (meteo + itinerario)
- **RF4**: Redirect a store partner per attrezzatura mancante
- **RF5**: Messaggi locali BitChat via BLE
- **RF6**: Ottimizzazione GPS con accelerometro (Auto-Pause)

### Partecipante

- **RF7**: Unirsi a escursione tramite codice invito
- **RF8**: Tracciamento GPS in background
- **RF9**: Invio SOS con coordinate GPS precise
- **RF10**: Mappa offline, bussola, posizione relativa al gruppo

### Capogruppo

- **RF11**: Creare escursione con codice invito univoco
- **RF12**: Dashboard tracking GPS real-time di tutti i partecipanti
- **RF13**: Gestire emergenze e inviare allarmi broadcast

### Gestore Rifugio

- **RF14**: Inserimento e invio allerte push pericoli

### Amministratore

- **RF19**: Gestire promozioni partner e monitorare crediti sociali

---

## 4. Requisiti Non Funzionali (D1 §5)

| ID    | Categoria             | Vincolo                                                                           |
| ----- | --------------------- | --------------------------------------------------------------------------------- |
| RNF1  | Prestazioni API       | Risposta < 500ms al 95° percentile su 4G                                          |
| RNF2  | Architettura          | Store-and-Forward (Offline-First)                                                 |
| RNF3  | Usabilita             | Interfaccia "During-Hike" operabile con una mano, pulsanti min 48x48dp, Dark Mode |
| RNF4  | Compatibilita         | Android 9.0+ (API 28+)                                                            |
| RNF5  | Manutenibilita        | Monolite Modulare Node.js + MongoDB                                               |
| RNF6  | Integrazione          | Timeout MeteoTrentino max 3s, poi dati da cache                                   |
| RNF7  | Rete Ibrida           | Switch automatico WebSocket → BLE Mesh entro 5s                                   |
| RNF8  | Sicurezza             | Payload chat/SOS crittografati AES-256, firma ECC pre-broadcast BLE               |
| RNF9  | Efficienza Energetica | Battery drain < 8%/h su 4000mAh (GPS + BLE background)                            |
| RNF10 | Storage               | SQLite locale max 50MB, eviction FIFO                                             |

---

## 5. Architettura di Sistema

### 5.1 Pattern Architetturale Principale (D2 §1.1)

**Offline-First + Store-and-Forward**: l'app funziona senza connessione. I dati critici (telemetria GPS, eventi gamification, SOS) vengono salvati in SQLite/Room localmente. Un `SyncManager` monitora la rete e invia batch al backend quando la connessione torna disponibile. Si applica il teorema CAP posizionandosi su AP (Availability + Partition Tolerance), con Eventual Consistency gestita dal backend.

### 5.2 Stack Tecnologico

| Layer                 | Tecnologia                                              | Note                                        |
| --------------------- | ------------------------------------------------------- | ------------------------------------------- |
| **Mobile**            | Kotlin 2.0.21, Jetpack Compose (BOM 2024.12), Material3 | minSdk 28, targetSdk 35                     |
| **Mobile DB**         | Room 2.6.1 + KSP                                        | Cache profilo, sessioni, telemetria offline |
| **Mobile Networking** | Retrofit 2.11 + OkHttp 4.12                             | AuthInterceptor con Bearer JWT              |
| **Mobile Security**   | EncryptedSharedPreferences                              | JWT cifrato localmente                      |
| **Backend**           | Node.js + Express 4                                     | Monolite modulare                           |
| **Backend DB**        | MongoDB (Mongoose 8)                                    | GeoJSON 2dsphere, TTL indexes               |
| **Autenticazione**    | JWT (bcrypt hash)                                       | Deep link `tsm://auth` per verifica email   |
| **Email**             | Nodemailer (Gmail SMTP)                                 | Verifica email post-registrazione           |
| **Real-time**         | Socket.io (installato, **non integrato**)               | Per telemetria live                         |
| **IoT**               | MQTT (installato, **non integrato**)                    | Per gateway rifugio                         |
| **Infra**             | Docker Compose                                          | MongoDB + Mosquitto                         |

### 5.3 Diagramma Componenti D2

```
Dashboard (UI Compose)
    ├── Query view          ← Cache locale Room
    ├── Session Join/Quit   ← Gestore Sessione
    ├── Notifica SOS        ← Gestore SOS
    ├── Aggiorna Dashboard  ← Push locali da SOS
    ├── Aggiorna Sessione   ← Dati live dalla Sessione
    └── Sync dati           ← Sincronizzatore DB

Gestore SOS
    ├── P2P SOS             → BLE Mesh broadcast
    ├── Notifica SOS        → Dashboard
    ├── Aggiorna Dashboard  → Alert visivi con dati GPS
    ├── API soccorsi CNSAS  → Relay satellitare esterno
    └── Salvataggio Dati    → Mediatore (Gestore Persistenza)

Gestore Persistenza (Mediatore)
    ├── Auth service        → Credenziali
    ├── Servizi Sessione    → Dati gruppo + tracking
    ├── Salvataggio Dati    → CRUD generica
    ├── Query view          → Lettura per Dashboard
    ├── Dati Locali         → Room DB (Gestore DB Locale)
    └── Trigger sync        → Sincronizzatore DB

Sincronizzatore DB
    ├── Sync API            → Backend REST
    └── Dati Locali         → Room DB (lettura buffer)
```

### 5.4 Interfacce e Protocolli (D2 §1.3)

| Interfaccia     | Supplier          | Consumer                | Protocollo | Funzione                             |
| --------------- | ----------------- | ----------------------- | ---------- | ------------------------------------ |
| `IRestApi`      | Backend Server    | App Mobile, IoT Gateway | HTTPS      | Sync batch + telemetria aggregata    |
| `IAuth`         | Auth Manager      | App Mobile              | OAuth2/JWT | Credenziali + rilascio token offline |
| `ILocalMqtt`    | IoT Gateway       | Macchinari, Sensori     | MQTT       | Dati grezzi sensori rifugio          |
| `IMeshSOS`      | App Mobile        | App Mobile (Relay)      | BLE ADV    | SOS connectionless a macchia d'olio  |
| `ISatLink`      | External Sat HW   | Capogruppo (App)        | BLE/Serial | Relay SOS verso soccorsi             |
| `IExternalData` | Weather/Trail API | Checklist Manager       | REST       | Download Hike Packet pre-hike        |

### 5.5 Routing Ibrido: Chat e SOS (D1 §6.5)

1. **Modalita IP (Primary)**: sotto copertura 4G/5G, payload crittografato AES-256 via **WebSocket** al backend Node.js (broker). Delivery immediata.
2. **Modalita Mesh/Decentralizzata (Fallback)**: alla caduta del socket TCP/IP, il modulo BLE passa in broadcast. Il payload viene iniettato in broadcast locale, flooding a macchia d'olio tra dispositivi vicini come nodi rele, fino al Capogruppo.

### 5.6 Profilo BLE GATT per SOS (D2 §9.2)

| UUID Servizio | Caratteristica         | Payload                                  |
| ------------- | ---------------------- | ---------------------------------------- |
| 0xFD12        | SOS_DATA (Read/Notify) | Coordinate GPS (Lat/Lng) + Timestamp UTC |
| 0xFD13        | SOS_AUTH (Write)       | Firma crittografica ECC (64 bytes)       |
| 0xFD14        | SOS_STATUS (Read)      | Stato del Relay: PENDING / FORWARDED     |

---

## 6. Modello Dati

### 6.1 MongoDB Collections (D2 §8)

**`users`** (Escursionisti e Guide)

```json
{
  "_id": "usr_78a9b",
  "email": "giacomo.radin@example.com",
  "role": "CAPOGRUPPO",
  "eccKeyPair": { "publicKey": "0450863ad64a87..." },
  "saldoSc": 2450,
  "sequenceCounter": 142,
  "elementiSbloccati": ["badge_marmolada"]
}
```

**`hike_sessions`**

```json
{
  "_id": "hike_ses_202",
  "creatorId": "usr_78a9b",
  "status": "COMPLETED",
  "statoFailover": false,
  "lastHeartbeat": "2026-04-18T17:40:00Z",
  "participants": ["usr_78a9b", "usr_marco_11"],
  "startTime": "2026-04-18T08:00:00Z"
}
```

**`user_event_store`** (Event Sourcing per crediti sociali)

```json
{
  "userId": "usr_78a9b",
  "sequenceNumber": 143,
  "idEvento": "UUIDv4",
  "type": "SOCIAL_CREDIT_EARNED",
  "amount": 100,
  "timestamp": "2026-04-19T15:30:00Z",
  "idempotencyKey": "v4_9921_01"
}
```

**`telemetry_logs`** (IoT Time-Series)

```json
{
  "_id": "log_662a1",
  "gatewayId": "gw_tuckett_01",
  "machineryId": "COMP_PET_01",
  "rifugioId": "RIF_TUCKETT_001",
  "metrics": {
    "vol_percent": 62.1,
    "weight_kg": 12.4,
    "status": "OPERATIONAL"
  },
  "timestamp": "2026-04-21T10:00:00Z"
}
```

### 6.2 Mongoose Schemas Implementati (codice attuale)

**User** (`backend/src/models/user.js`)

- `username`, `email`, `passwordHash`, `role` (groupLeader/rifugio/admin), `isVerified`, `verificationToken`, `createdAt`
- Virtual: `mySessions` (ref HikeSession via participants)
- **Gap vs D2**: mancano `eccKeyPair`, `saldoSc`, `sequenceCounter`, `elementiSbloccati`

**HikeSession** (`backend/src/models/hikeSession.js`)

- `creatorId`, `routeDetails` (name, startPoint GeoJSON, endPoint, difficultyLevel T/E/EE/EEA, elevationGain)
- `inviteCode` (8 char, unique), `participants[]` ({userId, role, joinedAt})
- `status` (PLANNED/ACTIVE/COMPLETED/CANCELLED), `statoFailover`, `lastHeartbeat`, `startTime`, `endTime`
- 2dsphere index su `routeDetails.startPoint`
- **Gap vs D2**: mancano telemetry bucket reference, ECC public keys nei participants

**Station** (`backend/src/models/station.js`)

- `stationCode`, `stationInfo` (name, elevation, lat/lon), `air_temperature[]` (max 10), `fetchedAt`
- Collection: `temperature_lists`

### 6.3 Collezioni D2 Non Ancora Create

| Collezione          | Scopo                                                     | Priorita                  |
| ------------------- | --------------------------------------------------------- | ------------------------- |
| `emergencies`       | Log SOS con coordinate GPS, firma ECC, meshMetadata       | Alta (per RegistraScreen) |
| `telemetry_buckets` | Posizioni GPS aggregate per sessione (bucket pattern 1h)  | Alta (per tracking live)  |
| `user_event_store`  | Event Sourcing crediti sociali (QUIZ_COMPLETED, NFC_SCAN) | Media (per gamification)  |
| `managers`          | Gestori Rifugio con permessi specifici                    | Bassa                     |

---

## 7. API RESTful

### 7.1 Endpoint Implementati (codice attuale)

| Metodo   | Route                         | Auth          | Servizio                                 | Stato                          |
| -------- | ----------------------------- | ------------- | ---------------------------------------- | ------------------------------ |
| `POST`   | `/auth/login`                 | No            | authService.loginUser                    | Funzionante                    |
| `GET`    | `/auth/verify/:token`         | No            | authService.verifyEmail                  | Funzionante (deep link tsm://) |
| `POST`   | `/users`                      | No            | userService.createUser                   | Funzionante                    |
| `GET`    | `/users`                      | JWT           | userService.getAllUsers                  | Funzionante                    |
| `GET`    | `/users/:id`                  | JWT           | userService.getUserById                  | Funzionante                    |
| `PUT`    | `/users/:id`                  | JWT+admin     | userService.updateUser                   | Funzionante                    |
| `DELETE` | `/users/:id`                  | JWT+admin     | userService.deleteUser                   | Funzionante                    |
| `POST`   | `/api/v1/sessions`            | JWT           | hikeSessionService.createSession         | Funzionante                    |
| `POST`   | `/api/v1/sessions/join`       | JWT           | hikeSessionService.joinSession           | Funzionante                    |
| `GET`    | `/api/v1/sessions/my`         | JWT           | hikeSessionService.getSessionsByUser     | Funzionante                    |
| `GET`    | `/api/v1/sessions/:id`        | JWT           | hikeSessionService.getSessionById        | Funzionante                    |
| `PATCH`  | `/api/v1/sessions/:id/status` | JWT (creator) | hikeSessionService.updateSessionStatus   | Funzionante                    |
| `DELETE` | `/api/v1/sessions/:id`        | JWT (creator) | hikeSessionService.deleteSession         | Funzionante                    |
| `GET`    | `/stations/remote/search`     | No            | stationRegistry.findRemoteStationsByName | Funzionante                    |
| `GET`    | `/stations/remote/:code`      | No            | stationRegistry.findRemoteStationByCode  | Funzionante                    |
| `GET`    | `/stations/local/search`      | No            | stationRegistry.findLocalStationsByName  | Funzionante                    |
| `GET`    | `/stations/local/:code`       | No            | stationRegistry.findLocalStationByCode   | Funzionante                    |
| `POST`   | `/stations`                   | No            | stationRegistry.saveStationToDb          | Funzionante                    |
| `PUT`    | `/stations/:id`               | No            | stationRegistry.refreshStationData       | Funzionante                    |
| `DELETE` | `/stations/:id`               | No            | stationRegistry.deleteStationFromDb      | Funzionante                    |
| `GET`    | `/meteo`                      | No            | meteo.fetchMeteoAndPersist               | Funzionante                    |

### 7.2 Endpoint da D2 Non Ancora Implementati

| Metodo | Route                                 | Auth | Scopo                                         | Priorita |
| ------ | ------------------------------------- | ---- | --------------------------------------------- | -------- |
| `POST` | `/api/v1/emergencies`                 | JWT  | Ricezione SOS con firma ECC (D2 §2.2)         | **Alta** |
| `POST` | `/api/v1/sessions/:id/telemetry`      | JWT  | Ingestione posizioni GPS batch                | **Alta** |
| `GET`  | `/api/v1/sessions/:id/positions`      | JWT  | Posizioni live gruppo (per mappa)             | **Alta** |
| `POST` | `/api/v1/users/:id/gamification/sync` | JWT  | Sync eventi offline crediti sociali (D2 §2.4) | Media    |
| `POST` | `/api/v1/rifugi/:id/telemetry/batch`  | JWT  | Ingestione telemetria IoT (D2 §2.3)           | Bassa    |
| `GET`  | `/api/v1/quiz/categories`             | JWT  | Lista categorie quiz educativi                | Media    |
| `GET`  | `/api/v1/quiz/:categoryId/questions`  | JWT  | Domande per categoria                         | Media    |
| `POST` | `/api/v1/quiz/:sessionId/answer`      | JWT  | Risposta quiz con crediti                     | Media    |
| `GET`  | `/api/v1/nfc/checkpoint/:id`          | JWT  | Info checkpoint NFC                           | Bassa    |
| `POST` | `/api/v1/nfc/checkpoint/:id/scan`     | JWT  | Registrazione scansione NFC                   | Bassa    |

---

## 8. Codebase Mobile — Stato Attuale

### 8.1 Struttura File

```
mobile/app/src/main/java/it/trentosmartmountain/app/
├── MainActivity.kt                    → Entry point, Compose + deep link tsm://
├── TsmApplication.kt                  → Init TsmApiClient, Room DB, TokenStorage
├── ui/
│   ├── theme/
│   │   ├── Color.kt                   → Solo TsmPrimary (0xFF156B60)
│   │   └── Theme.kt                   → TsmTheme base
│   ├── navigation/
│   │   ├── Routes.kt                  → Destinazioni (AUTH_ENTRY, LOGIN, MAIN_HIKER, etc.)
│   │   └── TsmNavHost.kt              → Grafo navigazione: auth → main
│   └── screens/
│       ├── auth/AuthEntryScreen.kt    → Scelta: Registra utente / Registra rifugio / Accedi
│       ├── login/LoginScreen.kt       → Email + password → JWT
│       ├── register/
│       │   ├── RegisterScreen.kt      → Username/email/password → POST /users
│       │   ├── RegisterRifugioScreen.kt → Placeholder
│       │   └── EmailVerificationPendingScreen.kt → Attesa verifica
│       ├── main/HikerMainScreen.kt    → Bottom nav: Home / Sessione / Registra / Profilo
│       ├── home/HomeScreen.kt         → Tab Social + Attivita (placeholder)
│       ├── session/SessionHubScreen.kt → Tab Pianifica + Unisciti (placeholder)
│       ├── registra/RegistraScreen.kt → PLACEHOLDER: testo + FAB SOS con AlertDialog base
│       ├── profile/ProfileScreen.kt   → Username da Room+API, logout
│       └── refuge/RefugeMainScreen.kt → Dashboard rifugista (placeholder)
├── viewmodel/
│   ├── LoginViewModel.kt              → Validazione + login via AuthRepository
│   ├── RegisterViewModel.kt           → Registrazione via RegistrationRepository
│   └── ProfileViewModel.kt            → Profilo da Room + API, logout
├── repository/
│   ├── AuthRepository.kt / AuthRepositoryImpl.kt
│   ├── RegistrationRepository.kt / RegistrationRepositoryImpl.kt
│   ├── ProfileRepository.kt / ProfileRepositoryImpl.kt
│   └── AppRepository.kt
├── data/
│   ├── remote/
│   │   ├── TsmApiClient.kt            → Retrofit singleton con AuthInterceptor
│   │   ├── TsmApiService.kt           → POST /auth/login, POST /users, GET /users/{id}
│   │   ├── AuthInterceptor.kt         → Bearer JWT automatico
│   │   ├── JwtDecoder.kt              → Decodifica payload JWT (userId, role)
│   │   └── dto/                        → LoginRequest/Response, RegisterRequest/Response, etc.
│   └── local/
│       ├── TokenStorage.kt            → EncryptedSharedPreferences per JWT
│       ├── AuthSession.kt             → Check JWT validity all'avvio
│       ├── LocalDataSource.kt         → Segnaposto per cache generica
│       └── db/
│           ├── TsmDatabase.kt         → Room DB "tsm.db"
│           ├── CachedUserProfileEntity.kt
│           └── ProfileDao.kt
└── service/
    └── ForegroundTrackingPlaceholder.kt → Segnaposto GPS background
```

### 8.2 Flusso Auth Completo (funzionante)

```
AuthEntryScreen → RegisterScreen → POST /users → EmailVerificationPending
                                                    ↓ (link email)
                                        Browser → GET /auth/verify/:token
                                                    ↓ (redirect)
                                        tsm://auth/success?jwt=TOKEN
                                                    ↓
AuthEntryScreen → LoginScreen → POST /auth/login → JWT salvato
                                                    ↓
                                        TsmNavHost → HikerMainScreen (role-based)
                                                    ↓
                                        Bottom Nav: Home | Sessione | Registra | Profilo
```

### 8.3 Dipendenze Gradle (app/build.gradle.kts)

**Presenti**: Compose BOM, Material3, Navigation Compose, Lifecycle, Retrofit, OkHttp, Gson, Room+KSP, Security-Crypto, Coroutines, Material Icons Extended.

**Mancanti** (da aggiungere per le prossime feature):

- `org.osmdroid:osmdroid-android` — Mappa OSMdroid per RegistraScreen
- `com.google.android.gms:play-services-location` — FusedLocationProvider per GPS
- `androidx.work:work-runtime-ktx` — WorkManager per sync batch in background

### 8.4 Permessi Android (AndroidManifest.xml)

**Presenti**: `INTERNET`
**Mancanti**:

- `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` — GPS tracking
- `ACCESS_BACKGROUND_LOCATION` — Tracking in background
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` — Foreground Service
- `BLUETOOTH_SCAN` + `BLUETOOTH_ADVERTISE` — BLE Mesh
- `NFC` — Checkpoint vetta
- `VIBRATE` — Feedback aptico SOS/quiz

### 8.5 Token di Design (Color.kt)

**Attuale**: solo `TsmPrimary = Color(0xFF156B60)`
**Target**:

```kotlin
val TsmPrimary = Color(0xFF2E5A27)   // Verde montagna (aggiornato)
val TsmAccent  = Color(0xFF4FC3F7)   // Ciano accent
val TsmSos     = Color(0xFF880E4F)   // Rosso scuro SOS
val TsmSurface = Color(0xFF1E1E1E)   // Sfondo dark
val TsmBorder  = Color(0xFF5D4037)   // Bordo marrone
```

---

## 9. Codebase Backend — Stato Attuale

### 9.1 Struttura File

```
backend/src/
├── app.js                             → Express setup, CORS, route mounting, Swagger
├── server.js                          → MongoDB connect + listen :3000
├── middleware/
│   ├── authMiddleware.js              → JWT Bearer → req.user {userId, role}
│   ├── authorizationMiddleware.js     → requireRoles('admin', 'rifugio', ...)
│   └── errorMiddleware.js             → globalErrorHandler + 404
├── models/
│   ├── user.js                        → Schema User (username/email/hash/role/verified)
│   ├── hikeSession.js                 → Schema HikeSession (route/invite/participants/status)
│   └── station.js                     → Schema Station (MeteoTrentino temperature)
├── routes/
│   ├── authRoutes.js                  → POST /login, GET /verify/:token
│   ├── userRoutes.js                  → CRUD /users
│   ├── hikeSessionRoutes.js           → /api/v1/sessions/*
│   ├── stationRoutes.js               → /stations (remote/local)
│   └── meteoRoutes.js                 → GET /meteo
└── services/
    ├── authService.js                 → Login (JWT) + verifica email (deep link)
    ├── userService.js                 → CRUD utenti con bcrypt
    ├── hikeSessionService.js          → Sessioni: create, join, status lifecycle
    ├── emailService.js                → Nodemailer Gmail SMTP
    ├── stationRegistry.service.js     → MeteoTrentino API (XML) + cache MongoDB
    └── meteo.service.js               → Fetch + parse + upsert temperature
```

### 9.2 Configurazione (.env)

```
PORT=3000
MONGO_URI=mongodb://localhost:27017/trento_smart_mountain
JWT_SECRET=tsm-local-2026-...
JWT_EXPIRES_IN=1d
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=SmartMountain.FMG@gmail.com
SMTP_PASS=... (app password)
```

### 9.3 Pattern Backend

- **3 strati**: routes → services → models
- **Errori business** come stringhe (`throw new Error("SESSION_NOT_FOUND")`), mappati in HTTP nella route
- **JWT payload**: `{ userId, role }`
- **Swagger** auto-generato su `/api-docs`
- **Socket.io e MQTT** installati in `package.json` ma **non inizializzati** in `app.js`

---

## 10. Gap Analysis: Progettazione vs Implementazione

### 10.1 Backend — Cosa Manca

| Area                              | Stato                         | Dettaglio                                            |
| --------------------------------- | ----------------------------- | ---------------------------------------------------- |
| Auth base (login/register/verify) | Completo                      | Funzionante con deep link                            |
| CRUD Users                        | Completo                      | Con RBAC                                             |
| Sessioni (create/join/status)     | Completo                      | Lifecycle PLANNED→ACTIVE→COMPLETED                   |
| MeteoTrentino                     | Completo                      | Fetch XML + cache MongoDB                            |
| **Emergenze SOS**                 | **Mancante**                  | Model + route + service per POST /api/v1/emergencies |
| **Telemetria GPS sessione**       | **Mancante**                  | Model TelemetryBucket + endpoint POST/GET            |
| **Socket.io real-time**           | **Installato, non integrato** | Serve per posizioni live gruppo                      |
| **Event Store gamification**      | **Mancante**                  | Model + endpoint sync crediti sociali                |
| **Quiz API**                      | **Mancante**                  | Model + CRUD categorie/domande + scoring             |
| **NFC checkpoint**                | **Mancante**                  | Model + endpoint scan/registrazione                  |
| **Internal Event Bus**            | **Mancante**                  | EventEmitter per disaccoppiare moduli (D2 §10.1)     |
| MQTT integration                  | Installato, non integrato     | Per IoT gateway rifugio                              |

### 10.2 Mobile — Cosa Manca

| Schermata                | Stato           | Dettaglio                                  |
| ------------------------ | --------------- | ------------------------------------------ |
| AuthEntryScreen          | Completo        | 3 opzioni: registra utente/rifugio/accedi  |
| LoginScreen              | Completo        | Email+password, validazione, JWT           |
| RegisterScreen           | Completo        | Username/email/password, POST /users       |
| EmailVerificationPending | Completo        | Messaggio + redirect login                 |
| HikerMainScreen          | Completo        | Bottom nav 4 tab                           |
| ProfileScreen            | Completo        | Username da Room+API, logout               |
| HomeScreen               | **Placeholder** | Tab Social/Attivita: solo testo            |
| SessionHubScreen         | **Placeholder** | Tab Pianifica/Unisciti: solo testo         |
| **RegistraScreen**       | **Placeholder** | Testo + FAB SOS con AlertDialog base       |
| RefugeMainScreen         | **Placeholder** | Dashboard rifugista: solo testo            |
| **EducationalScreen**    | **Non esiste**  | Hub quiz, categorie, progress              |
| **QuizActiveScreen**     | **Non esiste**  | Domanda + 4 risposte + timer               |
| **NfcCheckpointSheet**   | **Non esiste**  | Bottom sheet post-scan NFC                 |
| Color.kt                 | **Incompleto**  | Solo TsmPrimary, mancano 4 token           |
| Theme.kt                 | **Incompleto**  | Manca darkColorScheme con bg #121212       |
| TsmApiService.kt         | **Parziale**    | Solo 3 endpoint (login, register, getUser) |
| AndroidManifest.xml      | **Parziale**    | Solo permesso INTERNET                     |

### 10.3 Vincoli OCL da Rispettare (D2 §4)

| #   | Vincolo                                                  | Contesto                |
| --- | -------------------------------------------------------- | ----------------------- |
| 1   | Saldo crediti sociali >= 0                               | Utente.saldoSc          |
| 2   | Punteggio gamification > 0 per ogni azione               | AzioneGamificata.amount |
| 3   | Coda sync senza duplicati (idempotency key)              | SyncManager             |
| 4   | SOS valido solo con firma ECC + hopCount <= 10           | SegnaleSOS              |
| 8   | Nessun evento con timestamp futuro nella coda sync       | SyncManager             |
| 9   | Un solo leader durante failover                          | SessioneEscursione      |
| 10  | Riconciliazione anti-split-brain: Distance→Battery→Sc→ID | Failover                |
| 12  | Failover timeout 10 min senza heartbeat                  | lastHeartbeat           |
| 13  | SOS richiede token sessione valido e non scaduto         | Pre-condizione          |
| 14  | Dopo inizializza(), status = ATTIVA                      | Post-condizione         |

---

## 11. Piano Implementativo per Schermata

### 11.1 RegistraScreen (Priorita 1 — Prossima)

**UI Components**:

1. StatusBar row: Pills GPS/MESH/OFFLINE + timer cronometro
2. MetricStrip: 3 colonne (Distanza / Altitudine / Gruppo)
3. MapView OSMdroid full-bleed con dark tile provider (CartoDB.DarkMatter)
4. FAB SOS 72dp cerchio, color TsmSos, animazione pulse
5. Pause/Stop buttons 52dp
6. SOS Popup custom: cerchio rosso 212dp, countdown Canvas 10s animato, button ANNULLA

**Backend necessario**:

- `POST /api/v1/emergencies` — Ricezione SOS con lat/lng/timestamp/tipo
- `POST /api/v1/sessions/:id/telemetry` — Batch posizioni GPS
- `GET /api/v1/sessions/:id/positions` — Posizioni live del gruppo

**Mobile necessario**:

- `RegistraViewModel.kt` — Stato sessione attiva, cronometro, metriche, SOS
- Aggiunta osmdroid in `build.gradle.kts`
- Aggiunta permessi GPS in `AndroidManifest.xml`
- Aggiornamento `Color.kt` con tutti i token
- Nuove stringhe in `strings.xml`

### 11.2 HomeScreen (Priorita 2)

**UI**: Tab Social (feed attivita community) + Tab Attivita (storico personale con card)
**Backend**: `GET /api/v1/sessions/my` gia esiste; servira un endpoint feed social
**Migliorie runtime**: Swipe-to-dismiss su card, pull-to-refresh, grafico barre mensile

### 11.3 SessionHubScreen (Priorita 3)

**UI**: Tab Pianifica (import GPX, crea sessione) + Tab Unisciti (inserisci codice)
**Backend**: Endpoint sessioni gia esistono; serve preview mappa GPX
**Migliorie**: Meteo per fascia oraria, checklist drag-and-drop

### 11.4 EducationalScreen + QuizActiveScreen (Priorita 4)

**UI**: Hub categorie quiz, progress bar, leaderboard; schermata quiz con 4 risposte + timer
**Backend**: CRUD quiz categorie/domande, scoring crediti
**Migliorie**: Flash verde/rosso, suono, shake haptic, confetti fine quiz

### 11.5 NFC Integration (Priorita 5 — richiede hardware)

**UI**: NfcCheckpointSheet (bottom sheet post-scan totem)
**Backend**: `GET/POST /api/v1/nfc/checkpoint/:id`
**Mobile**: NfcViewModel, permesso NFC, feedback aptico

---

## 12. State Machine del Client Mobile (D2 §6)

```
                    App Avviata (Rete OK)
                           │
                    ┌──────▼──────┐
                    │ ONLINE_SYNC │ ← Fetch Checklist/Meteo, Init Sessione
                    └──────┬──────┘
                           │ Inizio Escursione (No Rete)
                    ┌──────▼──────────┐
                    │ OFFLINE_MONITORING│
                    │                  │
          ┌─────── │  MESH_ACTIVE     │ ◄──── ACK Ricevuto / Reset
          │        │  - Passive BLE   │
          │        │  - Log Gamific.  │
          │        └─────┬──────┬────┘
          │              │      │
   Tasto SOS      Leader Offline > 10min
          │              │
   ┌──────▼──────┐ ┌────▼──────────┐
   │ EMERGENCY   │ │ FAILOVER_MODE │
   │ BROADCAST   │ │ Election:     │
   │ - Sign ECC  │ │ Dist→Batt→Sc  │
   │ - Max BLE Tx│ │ →ID           │
   └──────┬──────┘ └───────────────┘
          │
   Timeout / Max Retries
          │
   ┌──────▼──────────────┐
   │ SOS_FAILED_NO_COVER │ → UI: "Spostarsi e Ritentare"
   └─────────────────────┘

   Rete Rilevata / Sync Fallito:
   ┌──────▼──────────┐
   │ BATCH_SYNCING   │ → Upload Store-and-Forward buffer (UUIDv4)
   │                 │ → Event Sourcing Replay
   └─────────────────┘
```

---

## 13. Sequence Diagrams Chiave

### 13.1 Flusso SOS con Validazione ECC (D2 §5.1)

```
Escursionista          Capogruppo (Relay)     Sat-Hardware    Backend       CNSAS/112
     │                        │                    │             │              │
     ├─ genera Payload SOS ──►│                    │             │              │
     │  (Nome, GPS, Tipo)     │                    │             │              │
     ├─ sign(ECC_Private) ───►│                    │             │              │
     ├─ broadcast BLE_Mesh ──►│                    │             │              │
     │                        │                    │             │              │
     │         [hopCount <= 10]                    │             │              │
     │                   ┌────┤                    │             │              │
     │                   │ validate(Public_Key, Firma)           │              │
     │                   └────┤                    │             │              │
     │              [Firma Valida]                 │             │              │
     │                   ┌────┤                    │             │              │
     │                   │ UI: Conferma Capogruppo │             │              │
     │                   └────┤                    │             │              │
     │                        ├─ forwardSOS ──────►├─────────────►              │
     │                        │                    │  POST /emergencies         │
     │                        │                    │             ├─ Webhook ───►│
     │                        │                    │             │   202        │
     │                   ◄────┤ SOS_ACK            │             │              │
     │                        │                    │             │              │
     │        [Firma Non Valida / Falso Allarme]   │             │              │
     │                   ┌────┤                    │             │              │
     │                   │ silentDiscard() — scarto senza feedback              │
     │                   └────┤                    │             │              │
```

### 13.2 Batch Sync via Event Sourcing (D2 §5.2)

```
SyncManager (Mobile)    SQLite/Room    Backend REST    Event Store (NoSQL)
        │                    │              │                │
  [Fase Offline]             │              │                │
        ├─ detectNoConnection│              │                │
        ├─ storeEvent(AzioneGamificata) ──►│                │
        │                    │              │                │
  [Fase Online]              │              │                │
        ├─ detectNetwork     │              │                │
        ├─ fetchUnsyncedBatch ────────────►│                │
        │    List<Events>    │              │                │
        ├─ POST /gamification/sync ───────►│                │
        │                    │              ├─ validateToken │
        │                    │              │   [per ogni evento]
        │                    │              │   ├─ appendEvent ──────────►│
        │                    │              │   └─ recomputeBalance ─────►│
        │                    │              │                │
        │           [HTTP 200]              │                │
        ├─ markAsSynced ────►│              │                │
        │                    │              │                │
        │           [HTTP 503]              │                │
        ├─ initExponentialBackoff           │                │
```

---

## 14. Convenzioni di Sviluppo

### Backend

- Tre strati: **routes** (HTTP mapping) → **services** (business logic) → **models** (Mongoose)
- Errori business come stringhe (`"SESSION_NOT_FOUND"`), mappati in HTTP code nelle routes
- JWT payload: `{ userId, role }`
- Nessun segreto nell'app mobile; solo URL base + Bearer token

### Mobile

- Pattern **MVVM**: Screen (Compose) → ViewModel (StateFlow) → Repository → API/Room
- DI manuale (no Hilt): singleton in `TsmApplication`
- Stringhe UI in `res/values/strings.xml` (italiano)
- Token colori in `Color.kt`, tema in `Theme.kt`
- Navigation: `TsmNavHost` con `Routes` sealed class
- Deep link: `tsm://auth` gia configurato

### Generale

- Monorepo: `backend/`, `mobile/`, `iot/`, `docs/`
- Docker Compose per MongoDB + Mosquitto
- Git su GitHub, branch `main`
- Indentazione 2 spazi (vedi `.vscode/settings.json`)

---

## 15. Dipendenze Installate ma Non Usate

| Pacchetto               | Layer   | Scopo Previsto                        |
| ----------------------- | ------- | ------------------------------------- |
| `socket.io`             | Backend | Telemetria real-time posizioni gruppo |
| `mqtt`                  | Backend | Comunicazione IoT gateway rifugio     |
| Material Icons Extended | Mobile  | Icone aggiuntive (gia importato)      |

---

## 16. File di Configurazione e Infra

| File                                                      | Scopo                                            |
| --------------------------------------------------------- | ------------------------------------------------ |
| `docker-compose.yml`                                      | MongoDB + Mosquitto                              |
| `backend/.env`                                            | Variabili ambiente (JWT_SECRET, SMTP, MONGO_URI) |
| `mobile/local.properties`                                 | `tsm.api.baseUrl` per device fisico              |
| `mobile/gradle/libs.versions.toml`                        | Versioni centralizzate dipendenze                |
| `mobile/app/src/main/res/xml/network_security_config.xml` | HTTP dev localhost                               |

---

## 17. Roadmap di Riferimento

| Fase  | Schermata/Feature      | Backend                              | Mobile                                  | Priorita     |
| ----- | ---------------------- | ------------------------------------ | --------------------------------------- | ------------ |
| **A** | Color.kt + Theme.kt    | —                                    | Aggiornare token, darkColorScheme       | Prerequisito |
| **B** | RegistraScreen         | POST emergencies, POST/GET telemetry | OSMdroid, ViewModel, GPS                | **Alta**     |
| **C** | HomeScreen             | Feed social endpoint                 | Tab Social + Attivita, card, swipe      | Alta         |
| **D** | SessionHubScreen       | (endpoint gia esistono)              | Tab Pianifica + Unisciti, preview mappa | Media        |
| **E** | EducationalScreen      | CRUD quiz + scoring                  | Hub quiz, QuizActiveScreen              | Media        |
| **F** | ProfileScreen upgrade  | —                                    | Avatar, NFC badge, animazioni           | Media        |
| **G** | NFC Integration        | GET/POST checkpoint                  | NfcViewModel, bottom sheet              | Bassa        |
| **H** | Socket.io real-time    | Inizializzazione in app.js           | Client mobile                           | Trasversale  |
| **I** | Foreground Service GPS | —                                    | ForegroundTrackingService               | Trasversale  |
| **J** | Store-and-Forward      | —                                    | SyncManager + WorkManager               | Trasversale  |

---

_Documento generato il 2026-05-14. Riferimenti: D1 (Deliverable Requisiti, 27/03/2026), D2 (Deliverable Architettura, 26/04/2026), codebase al commit dfd9ba9._
