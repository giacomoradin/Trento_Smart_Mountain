# Trento Smart Mountain — Stato del Progetto e Piano Implementativo

> Documento di riferimento per lo sviluppo. Descrive la codebase attuale, gli obiettivi da D1/D2, il gap tra progettazione e implementazione, e il piano delle modifiche.
>
> **Ultimo aggiornamento: 2026-05-16 — Fine Sprint 1**

---

## 1. Visione e Obiettivi del Progetto

Trento Smart Mountain (TSM) è un ecosistema digitale per l'ambiente montano trentino. Supera le app di navigazione passiva (Komoot, AllTrails) integrando **sicurezza attiva dei gruppi** (mesh BLE offline, SOS), **gamification educativa** (quiz, NFC checkpoint vetta, crediti sociali) e **gestione rifugi** (IoT, telemetria).

**Gruppo:** ID-6 — Federico Cattelan (242111), Marco Christian Stoica (246443), Giacomo Radin (242907)

### 1.1 Macro-obiettivi (D1 §1)

| ID | Obiettivo | Descrizione |
|----|-----------|-------------|
| O1 | Ecosistema digitale a valore aggiunto | Aggregatore community per tutte le stagioni |
| O2 | Sicurezza proattiva | Tracciamento e coordinamento gruppi escursione |
| O3 | Resilienza comunicativa | Comunicazione tra dispositivi in assenza di rete (BLE Mesh) |
| O4 | Sostenibilità ed Economia Circolare | Gamification educativa, NFC checkpoint, crediti sociali |
| O5 | Coinvolgimento multisettoriale | Cittadini, turisti, guide, gestori rifugi, operatori ecologici |

### 1.2 Pivot D2 rispetto a D1

In D2 è stata **rimossa** la raccolta fisica dei rifiuti (RF15-RF18 di D1) per ragioni di sicurezza. Il framework di sostenibilità si concentra ora su:

- **Gamification educativa** (Sustainability Paths): quiz su flora, fauna, sicurezza
- **Certificazione di vetta via NFC**: totem fisici ai checkpoint, scansione per crediti
- **Crowdsourcing segnalazioni**: manutenzione sentieri

---

## 2. Stack Tecnologico

| Layer | Tecnologia | Note |
|-------|-----------|------|
| **Mobile** | Kotlin 2.0.21, Jetpack Compose (BOM 2024.12), Material3 | minSdk 28, targetSdk 35 |
| **Mobile DB** | Room 2.6.1 + KSP | Cache profilo, sessioni, telemetria offline |
| **Mobile Networking** | Retrofit 2.11 + OkHttp 4.12 | AuthInterceptor con Bearer JWT |
| **Mobile Security** | EncryptedSharedPreferences | JWT cifrato localmente |
| **Mappa Mobile** | OSMdroid 6.1.20 | Tracking GPS con FusedLocationProvider |
| **QR Code** | ZXing Core 3.5.3 | Generazione QR sessione |
| **Drag-and-Drop** | sh.calvin.reorderable 2.4.3 | Checklist riordinabile |
| **Backend** | Node.js + Express 4 | Monolite modulare |
| **Backend DB** | MongoDB (Mongoose 8) | GeoJSON 2dsphere, TTL indexes |
| **Meteo** | meteo.report / TINIA API | Forecast 3h e 24h via weatherService di Marco |
| **Autenticazione** | JWT (bcrypt hash) | Deep link `tsm://auth` per verifica email |
| **Email** | Nodemailer (Gmail SMTP, retry 3x esponenziale) | Verifica email + reset password |
| **Infra** | Docker Compose | MongoDB + Mosquitto (MQTT) |

---

## 3. Requisiti Funzionali — Stato di Copertura Sprint 1

### RF coperti dal codice

| RF | Descrizione | Stato Sprint 1 |
|----|-------------|----------------|
| RF0 | Autenticazione | ✅ Completo (register → SMTP verify → login JWT) |
| RF7 | Unirsi a escursione tramite codice invito | ✅ Completo (codice TSM-XXXX + UNISCITI tab) |
| RF8 | Tracciamento GPS in background | ✅ Completo (ForegroundService + HikeTrackingEngine + auto-pause) |
| RF9 | Invio SOS con coordinate GPS | ✅ Parziale (UI SOS dialog in RegistraScreen; backend POST /emergencies non implementato) |
| RF10 | Mappa offline con posizione utente | ✅ Completo (OSMdroid con tracking live in RegistraScreen) |
| RF11 | Creare escursione con codice invito univoco | ✅ Completo (PIANIFICA tab, GPX import, generazione TSM-XXXX) |
| RF14 | Allerte push pericoli (Rifugio) | 🟡 Parziale (RegisterRifugio funzionante, dashboard placeholder) |

### RF non ancora coperti

| RF | Descrizione | Note |
|----|-------------|------|
| RF1-RF6 | Itinerari, difficoltà, equipaggiamento, BitChat, Auto-Pause meteo | RF6 auto-pause GPS implementato |
| RF12 | Dashboard tracking real-time capogruppo | Richiede Socket.io (installato, non integrato) |
| RF13 | Broadcast allarmi emergenza | Richiede BLE Mesh (architettura pianificata) |
| RF15-RF18 | (Soppressi in D2) | — |
| RF19 | Promozioni admin + crediti sociali | Backend DTO creato, UI non implementata |

---

## 4. Deliverable Sprint 1 — Cosa è stato Implementato

### 4.1 Backend Node.js

#### Modelli MongoDB

| Modello | File | Campi chiave |
|---------|------|--------------|
| **User** | `models/user.js` | username, email, passwordHash, role, isVerified, verificationToken, passwordResetToken/Expires, rifugioDetails |
| **HikeSession** | `models/hikeSession.js` | routeDetails (name/startPoint/endPoint/difficultyLevel/elevationGain), inviteCode (TSM-XXXX), participants[], status lifecycle, meetingDate/Time, maxParticipants, gpxStats (distanceKm/elevationGainM/trackPoints/**elevationProfile**/estimatedPoints), statoFailover |
| **Location** | `models/location.js` | externalId, type (town/poi), name, elevation, location (GeoJSON), regionId, forecasts (slots3h/slots24h) — **nuovo, per meteo** |

#### Endpoint implementati e funzionanti

| Metodo | Route | Auth | Descrizione |
|--------|-------|------|-------------|
| `POST` | `/auth/login` | No | Login → JWT |
| `GET` | `/auth/verify/:token` | No | Verifica email → deep link tsm:// |
| `POST` | `/auth/forgot-password` | No | Reset password (email link) |
| `GET` | `/auth/reset-password/:token` | No | Form HTML reset |
| `POST` | `/auth/reset-password/:token` | No | Salva nuova password (JSON o form) |
| `POST` | `/users` | No | Registrazione utente/rifugio (con rifugioDetails) |
| `GET` | `/users/:id` | JWT | Profilo utente |
| `PUT` | `/users/:id` | JWT+admin | Aggiorna utente |
| `DELETE` | `/users/:id` | JWT+admin | Elimina utente |
| `POST` | `/api/v1/sessions` | JWT | Crea sessione (con GPX stats + estimatedPoints) |
| `GET` | `/api/v1/sessions/my` | JWT | Le mie sessioni (populate creator + participants) |
| `GET` | `/api/v1/sessions/:id` | JWT | Dettaglio sessione (fully populated) |
| `POST` | `/api/v1/sessions/join` | JWT | Unisciti con codice TSM-XXXX |
| `POST` | `/api/v1/sessions/:id/leave` | JWT | Abbandona sessione (non-creator) |
| `DELETE` | `/api/v1/sessions/:id` | JWT (creator) | Elimina sessione (rimuove anche per i partecipanti) |
| `PATCH` | `/api/v1/sessions/:id` | JWT (creator) | Modifica dettagli (populate entrambi i campi → fix crash Gson) |
| `PATCH` | `/api/v1/sessions/:id/status` | JWT (creator) | PLANNED→ACTIVE→COMPLETED |
| `GET` | `/weather/locations/nearby` | No | Stazioni meteo per coordinate (2dsphere) |
| `GET` | `/weather/locations/search` | No | Cerca stazioni per nome |
| `GET` | `/weather/forecast/:externalId` | No | Forecast 3h + 24h (cache 1h MongoDB) |
| `POST` | `/weather/forecast/:externalId/refresh` | No | Forza refresh forecast |
| `POST` | `/weather/seed` | No | Popola DB con towns + POI da TINIA API |

#### Logica business implementata

- **checkUserAlreadyInActiveSession**: blocca solo sessioni `ACTIVE` (non PLANNED) — un utente può pianificare più escursioni future
- **generateInviteCode**: formato `TSM-XXXX` (4 hex uppercase)
- **updateSessionDetails**: populate simmetrico (`creatorId` + `participants.userId`) per evitare crash Gson su client Kotlin
- **SMTP retry**: `sendMailWithRetry` con backoff esponenziale (3 tentativi: 2s, 4s, 8s)
- **Password reset**: token monouso con scadenza 1h, form HTML risponsivo

#### Endpoint non ancora implementati (Sprint 2+)

| Metodo | Route | Priorità |
|--------|-------|----------|
| `POST` | `/api/v1/emergencies` | Alta — SOS con firma ECC |
| `POST` | `/api/v1/sessions/:id/telemetry` | Alta — GPS batch upload |
| `GET` | `/api/v1/sessions/:id/positions` | Alta — Posizioni live gruppo |
| `POST` | `/api/v1/users/:id/gamification/sync` | Media — Event Sourcing crediti |
| `GET/POST` | `/api/v1/quiz/...` | Media — Quiz educativi |
| `GET/POST` | `/api/v1/nfc/checkpoint/...` | Bassa — NFC totem |

---

### 4.2 Mobile Kotlin/Compose

#### Architettura delle dipendenze (build.gradle.kts)

```
Compose BOM 2024.12 + Material3 + Material Icons Extended
Navigation Compose 2.8.5
Retrofit 2.11 + OkHttp 4.12 + Gson
Room 2.6.1 + KSP
EncryptedSharedPreferences (security-crypto 1.1)
OSMdroid 6.1.20
FusedLocationProvider (play-services-location 21.3)
ZXing Core 3.5.3
sh.calvin.reorderable 2.4.3
```

#### Schermate implementate

| Schermata | File | Stato | Note |
|-----------|------|-------|------|
| **AuthEntryScreen** | `auth/AuthEntryScreen.kt` | ✅ Completo | Logo TSM Canvas (montagna + dot ciano), tagline, 3 bottoni |
| **LoginScreen** | `login/LoginScreen.kt` | ✅ Completo | Icone campi, toggle password, "Password dimenticata?", offline badge, link Registrati |
| **RegisterScreen** | `register/RegisterScreen.kt` | ✅ Completo | Step indicator, checkbox ToS GPS, validazione |
| **RegisterRifugioScreen** | `register/RegisterRifugioScreen.kt` | ✅ Completo | Info box, campi rifugio (nome/CAI/quota/posti/coordinate), disclaimer verifica manuale |
| **ForgotPasswordScreen** | `register/ForgotPasswordScreen.kt` | ✅ Completo | Email input + stato "email inviata" |
| **EmailVerificationPendingScreen** | `register/EmailVerificationPendingScreen.kt` | ✅ Completo | Istruzioni verifica |
| **HikerMainScreen** | `main/HikerMainScreen.kt` | ✅ Completo | Bottom nav 4 tab, auto-switch a Registra via SessionStartCoordinator |
| **HomeScreen** | `home/HomeScreen.kt` | 🟡 Placeholder | Tab Social + Attività, layout base |
| **SessionHubScreen — PIANIFICA** | `session/SessionHubScreen.kt` | ✅ Completo | GPX import (XmlPullParser), form sessione, DatePicker/TimePicker, QR preview (ZXing), inviteCode TSM-XXXX |
| **SessionHubScreen — UNISCITI** | `session/SessionHubScreen.kt` | ✅ Completo | Code boxes OTP (TextFieldValue), lista sessioni ordinata, AVVIA/Elimina/Abbandona |
| **SessionDetailScreen** | `session/SessionDetailScreen.kt` | ✅ Completo | Profilo altimetrico GPX reale (Canvas), stima CAI, meteo reale TINIA, checklist drag-and-drop, partecipanti avatar, edit mode creator, codice invito copyable |
| **RegistraScreen** | `registra/RegistraScreen.kt` | ✅ Completo | OSMdroid + GPS tracking live, metriche (distanza/dislivello/quota/tempo), FAB SOS, auto-pause |
| **ProfileScreen** | `profile/ProfileScreen.kt` | ✅ Completo | Username da Room+API, hint offline, logout |
| **RefugeMainScreen** | `refuge/RefugeMainScreen.kt` | 🟡 Placeholder | Dashboard rifugista |

#### ViewModels implementati

| ViewModel | Responsabilità |
|-----------|---------------|
| `LoginViewModel` | Validazione + login via AuthRepository |
| `RegisterViewModel` | Registrazione utente |
| `RegisterRifugioViewModel` | Registrazione rifugio con campi specifici |
| `ForgotPasswordViewModel` | Richiesta reset password via email |
| `ProfileViewModel` | Profilo da Room + API, logout |
| `SessionPlanViewModel` | GPX parsing (smoothing+valley-peak+sampling), form sessione, generazione preview code |
| `SessionJoinViewModel` | Lista sessioni, join codice, leave/delete con rilevamento creator/partecipante |
| `SessionDetailViewModel` | Dettaglio sessione, checklist, edit mode, meteo TINIA, salvataggio modifhe |
| `RegistraViewModel` | GPS tracking engine, auto-pause accelerometro, metriche live, auto-start da SessionDetail |

#### Data layer — DTOs principali

| DTO | Campi chiave |
|-----|-------------|
| `SessionResponse` | _id, inviteCode, status, routeDetails, meetingDate/Time, gpxStats, creatorId (populated), participants (populated), maxParticipants |
| `GpxStats` | distanceKm, elevationGainM, trackPoints, **elevationProfile** (max 50 punti), **estimatedPoints** |
| `WeatherForecastResponse` | location, forecast3h (List\<Slot>), forecast24h (List\<Slot>) |
| `WeatherForecastSlot` | validFrom/To, temperature, temperatureMin/Max, rainProbability, windSpeed/Direction, skyCondition |

#### Modulo di stima escursione (HikeEstimation.kt)

| Funzione | Formula |
|----------|---------|
| `caiTimeHours(D, H)` | Polinomio CAI su pendenza P=(H/M)×100 → min/km × distKm / 60 |
| `equivalentDistance(D, H)` | D + H/100 (100m salita ≡ 1km piano) |
| `naismithTimeHours(D, H)` | D/4 + H/300 |
| `estimatedPoints(D, H, K=10)` | round(K × D_eq) — pianificazione, μ=1.0 |
| `finalPoints(D, H, T_reale, K=10)` | round(K × D_eq × clip(T_nom/T_reale, 0.8, 1.2)) |

#### Navigazione

```
TsmNavHost:
  AUTH_ENTRY → REGISTER / REGISTER_RIFUGIO / LOGIN
  LOGIN      → FORGOT_PASSWORD
  LOGIN      → MAIN_HIKER (role: groupLeader) | MAIN_RIFUGIO (role: rifugio)
  MAIN_HIKER → SESSION_DETAIL (full-screen sopra bottom nav)
  SESSION_DETAIL (AVVIA) → SessionStartCoordinator → HikerMainScreen switcha a Registra tab
                                                    → RegistraViewModel auto-start tracking
                                                    → PATCH /sessions/:id/status = ACTIVE
```

#### Feature trasversali implementate

| Feature | Descrizione |
|---------|-------------|
| **Dark Theme completo** | darkColorScheme Material3, TsmBackground #121212 |
| **Token design** | TsmPrimary #3F7020, TsmAccent #4FC3F7, TsmSos #6B0D0D, TsmSurface #1E1E1E, TsmBorder #5D4037 |
| **Offline badge** | Login mostra info su token locale se offline |
| **Codice invito sempre visibile** | SessionCard + SessionDetail con copia in clipboard |
| **GPX parser robusto** | Smoothing moving-average (w=5), valley-peak threshold 10m, interpolazione null, campionamento 50 punti |
| **Profilo altimetrico reale** | Canvas normalizzato min/max dal GPX, area fill gradient |
| **Meteo reale TINIA** | Nearest town via 2dsphere + forecast 3h+24h, skyCondition emoji mapper |
| **Checklist drag-and-drop** | ReorderableColumn (sh.calvin.reorderable), toggle check, add/remove |
| **Edit mode sessione** | Solo creator (check JWT userId), PATCH con populate fix, auto-close su successo |
| **AVVIA → Registra** | SessionStartCoordinator bus singleton, HikerMainScreen LaunchedEffect, RegistraViewModel auto-start |
| **PATCH Gson crash fix** | `updateSession` ora usa `Response<ApiMessageBody>` (backend popola entrambi i campi ref) |

---

## 5. Bug Risolti nel Sprint 1

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| SessionCreatedDialog OK non chiudeva | `onDismiss` no-op; state `sessionCreated` non resettata | `resetAfterCreation()` + callback `onSessionCreated` → `subTab = 1` |
| "Sei già in una sessione attiva" alla creazione | `checkUserAlreadyInActiveSession` bloccava anche sessioni PLANNED | Cambiato a `status: "ACTIVE"` only |
| Code box codice: cursore sbagliato + history ghost | `BasicTextField(value: String)` non sincronizzato con state esterno | `TextFieldValue` con `LaunchedEffect(code)` per riallineamento esplicito |
| Tab UNISCITI non si aggiornava dopo join | `loadSessions()` chiamato solo in `init {}` | `LaunchedEffect(Unit)` + `DisposableEffect(ON_RESUME)` |
| Dislivello GPX sovrastimato (2-3×) | Parser sommava tutte le variazioni positive (noise GPS) | Smoothing MA(5) + valley-peak threshold 10m |
| Edit mode non si chiudeva dopo save | `saveEdit` non ricaricava la sessione; dropdown locale non resettato | `loadSession(id)` dopo save + `LaunchedEffect(uiState.editMode)` per picker locali |
| `updateSession` crash `IllegalStateException` | Gson deserializzazione eager: `participants.userId` era ObjectId raw | Backend: populate doppio (creatorId + participants.userId); Kotlin: `Response<ApiMessageBody>` |
| UNISCITI: host non poteva eliminare | `leaveSession` restituiva 403 per il creator | `RemovalMode.DELETE` vs `LEAVE` basato su `creatorId._id == currentUserId` |

---

## 6. Struttura File Corrente

### Backend

```
backend/src/
├── app.js                          → Express, CORS, routes: /users /auth /api/v1/sessions /weather
├── server.js                       → MongoDB connect :3000
├── middleware/
│   ├── authMiddleware.js           → JWT Bearer → req.user
│   ├── authorizationMiddleware.js  → requireRoles(...)
│   └── errorMiddleware.js
├── models/
│   ├── user.js                     → +passwordResetToken/Expires, +rifugioDetails
│   ├── hikeSession.js              → +meetingDate/Time, +gpxStats.elevationProfile/estimatedPoints
│   └── location.js                 → NUOVO: town/poi, GeoJSON 2dsphere, forecasts slots3h/24h
├── routes/
│   ├── authRoutes.js               → login, verify, forgot-password, reset-password
│   ├── userRoutes.js               → CRUD /users
│   ├── hikeSessionRoutes.js        → sessioni: create/join/leave/delete/status/update
│   └── weatherRoutes.js            → NUOVO: locations/nearby/search, forecast/:id
└── services/
    ├── authService.js              → +forgotPassword, +resetPassword (form HTML + JSON)
    ├── userService.js              → +rifugioDetails passthrough
    ├── hikeSessionService.js       → +leaveSession, +updateSessionDetails (populate fix), codice TSM-XXXX
    ├── emailService.js             → +sendPasswordResetEmail, +sendMailWithRetry (backoff 3x)
    └── weatherService.js           → NUOVO: TINIA API, fetchVenues, findNearbyVenues, getLocationForecast
```

### Mobile

```
mobile/app/src/main/java/it/trentosmartmountain/app/
├── data/
│   ├── estimation/
│   │   └── HikeEstimation.kt       → NUOVO: formule CAI, Naismith, modello punti TSM
│   ├── location/                   → HikeTrackingEngine, UserLocationTracker, TrackingLocationBus,
│   │                                  StationaryDetector, ForegroundTrackingService
│   ├── remote/
│   │   ├── TsmApiService.kt        → 20 endpoint; PATCH usa ApiMessageBody (Gson safe)
│   │   └── dto/
│   │       ├── SessionResponse.kt  → +GpxStatsResponse.elevationProfile/estimatedPoints
│   │       ├── CreateSessionRequest.kt → +GpxStats.elevationProfile/estimatedPoints
│   │       ├── MeteoResponse.kt    → RISCRITTO: WeatherForecastResponse/Slot/LocationResult
│   │       ├── JoinSessionRequest.kt → +UpdateSessionStatusRequest, +UpdateSessionRequest
│   │       └── [altri DTO invariati]
│   ├── session/
│   │   └── SessionStartCoordinator.kt → NUOVO: bus singleton AVVIA→Registra
│   └── local/                       → TokenStorage, AuthSession, Room DB, ProfileDao
├── ui/
│   ├── theme/
│   │   ├── Color.kt                 → Tutti i token TSM (TsmPrimary/Accent/Sos/Surface/Border/Background)
│   │   └── Theme.kt                 → darkColorScheme completo
│   ├── navigation/
│   │   ├── Routes.kt                → +FORGOT_PASSWORD, +SESSION_DETAIL + helper sessionDetailRoute()
│   │   └── TsmNavHost.kt            → +ForgotPassword, +SessionDetail, +SessionStartCoordinator
│   └── screens/
│       ├── auth/AuthEntryScreen.kt  → RISCRITTO: logo Canvas, dark theme, 3 bottoni
│       ├── login/LoginScreen.kt     → RISCRITTO: icone, forgot password, offline badge
│       ├── register/
│       │   ├── RegisterScreen.kt    → RISCRITTO: step indicator, ToS checkbox
│       │   ├── RegisterRifugioScreen.kt → RISCRITTO: form completo con ViewModel
│       │   └── ForgotPasswordScreen.kt  → NUOVO
│       ├── main/HikerMainScreen.kt  → +SessionStartCoordinator observer
│       ├── session/
│       │   ├── SessionHubScreen.kt  → RISCRITTO: PIANIFICA + UNISCITI completi
│       │   └── SessionDetailScreen.kt → NUOVO: elevation chart, meteo TINIA, checklist D&D
│       ├── registra/RegistraScreen.kt → RISCRITTO: OSMdroid + tracking GPS + auto-start
│       └── [HomeScreen/ProfileScreen/RefugeMainScreen: parziali]
├── viewmodel/
│   ├── [Login/Register/Profile ViewModels: invariati]
│   ├── RegisterRifugioViewModel.kt  → NUOVO
│   ├── ForgotPasswordViewModel.kt   → NUOVO
│   ├── SessionPlanViewModel.kt      → +GPX parser robusto, +elevationProfile, +estimatedPoints
│   ├── SessionJoinViewModel.kt      → RISCRITTO: AndroidViewModel, RemovalMode LEAVE/DELETE
│   ├── SessionDetailViewModel.kt    → NUOVO: edit mode, meteo TINIA, checklist, AVVIA
│   └── RegistraViewModel.kt         → RISCRITTO: HikeTrackingEngine, auto-start da Coordinator
└── service/
    └── ForegroundTrackingService.kt  → Foreground GPS service funzionante
```

---

## 7. Permessi Android (AndroidManifest.xml) — Stato attuale

| Permesso | Stato | Uso |
|----------|-------|-----|
| `INTERNET` | ✅ | Tutte le chiamate di rete |
| `ACCESS_NETWORK_STATE` | ✅ | Rilevamento connettività (offline badge) |
| `ACCESS_FINE_LOCATION` | ✅ | GPS preciso per tracking |
| `ACCESS_COARSE_LOCATION` | ✅ | GPS approssimativo |
| `ACCESS_BACKGROUND_LOCATION` | 🔴 **MANCANTE** | D2 RNF9: tracking GPS quando schermo spento → da aggiungere |
| `FOREGROUND_SERVICE` | ✅ | ForegroundTrackingService |
| `FOREGROUND_SERVICE_LOCATION` | ✅ | Tracking GPS foreground |
| `POST_NOTIFICATIONS` | ✅ | Notifica tracking attivo (Android 13+) |
| `WAKE_LOCK` | 🟠 **MANCANTE** | Consigliato per evitare drop del service durante tracking lungo |
| `BLUETOOTH_SCAN` | ❌ | Futuro: BLE Mesh |
| `BLUETOOTH_ADVERTISE` | ❌ | Futuro: BLE Mesh |
| `NFC` | ❌ | Futuro: checkpoint vetta |
| `VIBRATE` | ❌ | Futuro: feedback aptico SOS |

---

## 8. Gap residui — Da completare nei prossimi Sprint

### Sprint 2 — Priorità Alta

| Feature | Layer | Dipendenze |
|---------|-------|-----------|
| Backend POST /emergencies (SOS con ECC) | Backend | ECC key generation |
| GPS telemetry batch upload a fine sessione | Mobile + Backend | POST /sessions/:id/telemetry |
| HomeScreen feed + storico attività | Mobile | GET /sessions/my già esistente |
| Salvataggio attività completata in Home | Mobile | `confirmStopTracking` → status COMPLETED |
| BLE Mesh fallback SOS | Mobile | Hardware + biblioteca BLE |

### Sprint 3 — Priorità Media

| Feature | Layer | Note |
|---------|-------|------|
| Socket.io real-time posizioni gruppo | Backend + Mobile | Installato ma non integrato |
| EducationalScreen quiz + NFC checkpoint | Mobile + Backend | Quiz model non ancora creato |
| Social Credits gamification (Event Sourcing) | Backend | user_event_store collection |
| ProfileScreen avatar + livello + badge | Mobile | HOME SOCIAL non implementata |
| MQTT IoT gateway rifugio | Backend | Installato, non integrato |

### Debito tecnico — risolto in audit 17/05

| Problema | File | Risolto |
|----------|------|---------|
| `userSchema.sessionRoles` non nello schema | `user.js` | ✅ Campo aggiunto con subdocument ref |
| `POST /weather/seed` e `/refresh` non protetti | `weatherRoutes.js` | ✅ `authenticate + requireRoles("admin")` |
| `ACCESS_BACKGROUND_LOCATION` mancante dal manifest | `AndroidManifest.xml` | ✅ Aggiunto + `WAKE_LOCK` |
| AVVIA visibile a tutti (403 silent per non-creator) | `SessionDetailScreen.kt` | ✅ Gating `isCreator`; partecipanti vedono info-chip |
| `joinSession` ritorna doc non-populated | `hikeSessionService.js` | ✅ `session.populate([creatorId, participants.userId])` |
| `activityDetailRoute(id, null)` → URL malformato | `Routes.kt` | ✅ Rimosso `?sessionId=` vuoto |
| `startPoint.coordinates default [0,0]` inquina 2dsphere | `hikeSession.js` | ✅ Rimosso default, indice `sparse: true` |
| `AppRepository.kt` interfaccia vuota (dead code) | mobile | ✅ File cancellato |
| `LocalDataSource.kt` singleton vuoto (dead code) | mobile | ✅ File cancellato |
| Endpoint debug `GET /weather/test` esposto | `weatherRoutes.js` | ✅ Rimosso |

### Debito tecnico residuo (Sprint 2)

| Problema | File | Severità |
|----------|------|----------|
| `meetingDate` come `String` invece di `Date` (sort lessicografico) | `hikeSession.js` | 🟠 Media — richiede backfill MongoDB |
| Pattern Repository violato in 4 ViewModel | mobile | 🟠 Media — testabilità |
| `leaveSession` restituisce doc non-populated (route usa `ApiMessageBody` → ok per ora) | `hikeSessionService.js` | 🟡 Bassa |
| WorkManager Store-and-Forward (sync batch offline) | Mobile | 🟠 Alta — richiede dipendenza WorkManager |
| KDoc stale in `TsmApplication.kt` e `TsmApiService.kt` | mobile | 🟡 Cosmetic |
| Zero unit test (JUnit ViewModel + Jest hikeSessionService) | tutto | 🟠 Sprint 2 priority |

---

## 9. Note per la Documentazione D3 (Sprint 1)

### Cosa dimostrare

1. **Flusso auth completo**: register → SMTP verify → login JWT → deep link `tsm://`
2. **Flusso sessione**: crea con GPX → inviteCode TSM-XXXX → condividi QR → altri si uniscono → AVVIA → tracking GPS → stop → COMPLETED
3. **Sessione detail**: profilo altimetrico reale, meteo TINIA, checklist drag-and-drop, punti stimati CAI
4. **Edit mode**: creator modifica sessione → salva → pannello si chiude automaticamente
5. **Reset password**: forgot password → email → link → nuovo form HTML → login

### API implementate (per la tabella RF→API del D3)

```
Auth:         POST /auth/login, /auth/forgot-password, /auth/reset-password/:token
              GET  /auth/verify/:token
Users:        POST /users, GET /users/:id
Sessions:     POST /api/v1/sessions, GET /my, GET /:id
              POST /:id/join, /:id/leave, PATCH /:id, /:id/status
              DELETE /:id
Weather:      GET /weather/locations/nearby, /locations/search, /forecast/:id
              POST /weather/seed, /forecast/:id/refresh
```

### Pattern architetturali da citare nel D3

- **Offline-First parziale**: JWT cifrato in EncryptedSharedPreferences, Room DB per profilo, `TrackingLocationBus` locale
- **Store-and-Forward**: struttura predisposta (ForegroundService + WorkManager TODO), non ancora operativa per upload batch
- **MVVM rigoroso**: nessuna chiamata Retrofit dalle Composable; tutte le chiamate passano per ViewModel
- **Eventual Consistency**: Event Sourcing per crediti sociali progettato in D2 §3.2.1, non ancora implementato
- **Sicurezza contratti API**: `Response<ApiMessageBody>` per PATCH che non necessita del payload SessionResponse completo (Gson-safe)

---

*Documento aggiornato il 2026-05-17 — Fine Sprint 1. Tutti i bug critici (C1/C2/C3) fixati. Branch: `UI` (ultimo merge: 2026-05-17). Prossima milestone: Sprint 2 — SOS backend + HomeScreen feed + BLE planning.*

---

## 10. Audit Codebase Pre-Consegna D3 (2026-05-16)

> Analisi end-to-end mobile + backend prima della consegna D3. Le voci sono ordinate per severità decrescente e includono **file:linea**, **causa** e **impatto** sull'utente o sul team.

### 10.1 Verdetto di Sprint 1

**🟠 PRONTI A CONSEGNARE D3 con riserve.** Il flusso principale (auth → crea sessione → unisciti → AVVIA → tracking → COMPLETED → "Le mie attività") è dimostrabile, ma sono presenti **3 bug critici** che vanno almeno **documentati** nel D3 (Sprint Retrospective) anche se non si fa in tempo a correggerli prima del 17/05. Il resto è debito tecnico minore e codice morto da pulire in Sprint 2.

| Stato release | Conteggio |
|----|----|
| 🔴 Critici (bloccano un flusso utente o sicurezza) | **3** |
| 🟠 Medi (degradano UX o introducono incoerenze) | **7** |
| 🟡 Minori (codice morto, KDoc stale, naming) | **6** |

### 10.2 Problemi 🔴 CRITICI

| # | Problema | File:linea | Causa tecnica | Impatto |
|---|----------|-----------|---------------|---------|
| C1 | **Partecipante non-creator non può AVVIA da SessionDetail** | `backend/src/services/hikeSessionService.js:229` (`updateSessionStatus`) ↔ `mobile/.../viewmodel/SessionDetailViewModel.kt` (AVVIA action) | Il service blocca il PATCH `/sessions/:id/status` con `FORBIDDEN` se `creatorId !== userId`. Il client mostra solo un toast generico → silent 403. | Un partecipante che apre la sessione e preme AVVIA vede caricamento perpetuo. Solo il creator può davvero passare a `ACTIVE`. Contraddice il pattern "SessionStartCoordinator + RegistraScreen auto-start" descritto in §4.2. |
| C2 | **Endpoint Weather completamente non protetti** | `backend/src/routes/weatherRoutes.js:22-185` | Il router non chiama mai `router.use(authenticate)`. Anche `POST /seed` e `POST /forecast/:externalId/refresh` (marcati come "admin / cron" nei commenti) sono pubblici. | Chiunque può forzare un re-seed pesante dell'API TINIA (DoS economico, rate-limit TINIA → blacklist IP del server) e leggere/refresh dei forecast senza JWT. Da risolvere prima del deploy demo. |
| C3 | **`ACCESS_BACKGROUND_LOCATION` mancante nel manifest** | `mobile/app/src/main/AndroidManifest.xml:4-10` | Il permesso non è dichiarato; `FOREGROUND_SERVICE_LOCATION` da solo non basta su Android 10+ per ricevere update GPS a schermo spento dopo qualche minuto. | Quando lo schermo si spegne durante un'escursione il `FusedLocationProvider` interrompe gli update → tracciato GPS troncato → metriche/punti CAI sbagliati. Viola **D2 RNF9** ("tracking continuo background"). |

### 10.3 Problemi 🟠 MEDI

| # | Problema | File:linea | Causa | Impatto |
|---|----------|-----------|-------|---------|
| M1 | `User.sessionRoles` non esiste nello schema, ma `hikeSessionService` fa `$push` su quel campo | `backend/src/models/user.js:5-26` ↔ `backend/src/services/hikeSessionService.js:50-57, 88-96` | Mongoose con `strict: true` (default) silenziosamente scarta i `$push` su campi non dichiarati. Lo schema User non ha `sessionRoles`. | Il sistema dei ruoli per-sessione progettato in D2 §4.2 non sta venendo persistito. Tutte le query future basate su `User.sessionRoles` falliranno. Resterà un silent bug fino a quando qualcuno non interrogherà il campo. |
| M2 | `meetingDate` è `String` invece di `Date` | `backend/src/models/hikeSession.js:32` ↔ `hikeSessionService.js:178` (`.sort({ meetingDate: 1 })`) | Salvato come stringa ISO `"YYYY-MM-DD"` dal client. Il sort `meetingDate: 1` è quindi lessicografico — funziona finché tutti i client mandano lo stesso formato, ma rompe se uno manda `"16/05/2026"` o un timestamp. | Ordinamento sessioni instabile cross-client. Calendari/filtri "prossime escursioni" potrebbero sballarsi. Da migrare a `Date` con backfill. |
| M3 | `routeDetails.startPoint.coordinates` default `[0,0]` inquina l'indice 2dsphere | `backend/src/models/hikeSession.js:17-22` | Se il GPX non viene fornito, il documento entra nell'indice geospaziale a Null Island (lat 0, lng 0, oceano Atlantico). | Future query "sessioni vicine a me" possono restituire sessioni senza GPS o falliranno con error 16755 (Mongo) per Point invalido. Rimuovere il default e usare `sparse: true` sull'indice. |
| M4 | `joinSession` restituisce documento NON popolato | `backend/src/services/hikeSessionService.js:85-98` | Il service salva e ritorna `session` raw; il route `POST /join` lo manda al client come SessionResponse. | Quando il client Kotlin (`SessionJoinViewModel`) deserializza il body con Gson, i campi `creatorId` / `participants.userId` sono ObjectId stringa → potenziale `IllegalStateException` come quello già visto su `updateSession` (Bug §5 row 7). Mitigato oggi solo perché il client non legge subito il body, ma è il prossimo crash latente. |
| M5 | Pattern Repository violato in 4 ViewModel | `mobile/.../viewmodel/ActivityListViewModel.kt`, `SessionPlanViewModel.kt`, `SessionJoinViewModel.kt`, `SessionDetailViewModel.kt` | Tutti chiamano direttamente `TsmApiClient.service().xxx()`. Solo `LoginViewModel`/`RegisterViewModel` usano `AuthRepository`. | Impossibile fare unit test sui ViewModel senza mock di Retrofit. Cambi all'API obbligano a toccare N file invece di 1. Definition-of-Done futura: nuovo codice deve passare per Repository. |
| M6 | `Routes.activityDetailRoute(id, sessionId=null)` produce URL malformato | `mobile/.../ui/navigation/Routes.kt:30-32` | Il ramo `else` ritorna `"activity_detail/$activityId?sessionId="` con valore vuoto. La query string c'è ma non ha contenuto. | Su alcuni device Compose Navigation interpreta `""` come stringa vuota (non null) → la destinazione riceve `sessionId=""` invece di non riceverlo. La logica `if (sessionId.isNullOrEmpty())` mitiga ma è fragile: meglio omettere la query del tutto. |
| M7 | KDoc stale in `TsmApplication.kt` e `TsmApiService.kt` | `mobile/.../TsmApplication.kt` (commento "profilo utente in cache"), `mobile/.../data/remote/TsmApiService.kt:121-127` (KDoc orfano weather davanti a `getActivityStats`) | Refactor successivi (CompletedActivityEntity, getActivityStats spostato) non hanno aggiornato i commenti. | I prossimi developer del team troveranno commenti che contraddicono il codice. Da ripulire prima del freeze D3 (impressione di "code rot" alla prof.). |

### 10.4 Problemi 🟡 MINORI

| # | Problema | File | Note |
|---|----------|------|------|
| L1 | `AppRepository.kt` interfaccia marker vuota (dead code) | `mobile/.../repository/AppRepository.kt` | Mai implementata, mai referenziata. Cancellare. |
| L2 | `LocalDataSource.kt` singleton vuoto (dead code) | `mobile/.../data/local/LocalDataSource.kt` | Stub MVVM mai usato. Cancellare. |
| L3 | Uso misto di `@Query` e `@retrofit2.http.Query` fully-qualified | `mobile/.../data/remote/TsmApiService.kt` | Solo coerenza stilistica. Normalizzare. |
| L4 | `hikeSessionRoutes.js` POST `/` ha error mapping incompleto | `backend/src/routes/hikeSessionRoutes.js:51-55` | Solo `USER_ALREADY_IN_SESSION` mappato, tutti gli altri error → 500 generico. Migliorare error mapping per UX. |
| L5 | `weatherRoutes.js` ha endpoint debug `/test` esposto | `backend/src/routes/weatherRoutes.js:31` | `router.get('/test', (req, res) => res.json({ ok: true }))` da rimuovere in prod. |
| L6 | TODO espliciti in `SessionPlanViewModel` (348-383) e `SessionDetailViewModel` (63-85, 291-294) | `mobile/.../viewmodel/SessionPlanViewModel.kt`, `SessionDetailViewModel.kt` | Documentare nel D3 come "Refinement Sprint 2" (sistema analisi tracciato, checklist auto-gen, meteo per slot). |

### 10.5 Codice Morto Confermato (da cancellare)

- `mobile/.../repository/AppRepository.kt` (interfaccia vuota)
- `mobile/.../data/local/LocalDataSource.kt` (singleton vuoto)
- `backend/src/routes/weatherRoutes.js:31` (`/test` endpoint debug)

### 10.6 Riepilogo verifiche svolte

| Layer | File letti integralmente | Risultato |
|-------|--------------------------|-----------|
| Backend | `app.js`, `models/user.js`, `models/hikeSession.js`, `routes/hikeSessionRoutes.js`, `routes/weatherRoutes.js`, `services/hikeSessionService.js`, `middleware/authMiddleware.js` | 3 critici + 4 medi sopra |
| Mobile | `TsmApplication.kt`, `AndroidManifest.xml`, `Routes.kt`, `AuthRepository(Impl).kt`, `LoginViewModel`, `SessionPlanViewModel`, `SessionDetailViewModel`, `RegisterRifugioViewModel`, `HomeScreen.kt` (read previously: `RegistraScreen.kt`) | 1 critico + 3 medi + 6 minori |

---

## 11. D3 Sprint 1 — Struttura Documentazione

> Template allineato ai requisiti del docente. Ogni sezione qui è una traccia: il team la espande nel documento Word/PDF finale prima del 17/05.

### 11.1 Sezione introduttiva

#### Team members

| Nome | Cognome | Matricola | Account GitHub |
|------|---------|-----------|----------------|
| Federico | Cattelan | 242111 | _(da inserire)_ |
| Marco Christian | Stoica | 246443 | _(da inserire)_ |
| Giacomo | Radin | 242907 | giacomoradin |

#### Project idea (3–5 righe)

> Trento Smart Mountain (TSM) è un ecosistema digitale per l'escursionismo in Trentino-Alto Adige che integra **sicurezza attiva di gruppo** (tracciamento GPS in background, codici invito sessione, fallback SOS), **gamification educativa** (modello CAI di stima sforzo, punti per attività completate, futuri quiz NFC ai checkpoint di vetta) e **gestione rifugi** (account dedicati, telemetria IoT prevista). Il sistema combina app Android (Kotlin/Compose, MVVM, offline-first), backend Node.js + MongoDB (geospatial 2dsphere, JWT) e un'integrazione meteo reale (TINIA / meteo.report) per supportare l'escursionista dalla pianificazione fino al ritorno.

#### Links

- **Repository GitHub**: `https://github.com/giacomoradin/TrentoSmartMountain` _(verificare URL definitivo)_
- **Apiary / API docs**: Swagger UI esposto su `http://<host>:3000/api-docs` (file `swagger-output.json` nel repo). Apiary placeholder da pubblicare prima della consegna.

### 11.2 Sezione generale

#### Branching strategy (NO Master-only)

Il repo segue un **GitFlow semplificato** (verificare con `git branch -a`):

```
main           ← solo merge da release/develop (protetta)
develop        ← integration branch (futuro: oggi sviluppo diretto su UI)
UI             ← branch attivo di Sprint 1 (feature mobile + integrazione)
API-Meteo-Integration  ← feature branch meteo (Marco)
auth-login-jwt         ← feature branch auth (storico)
crud-mongodb           ← feature branch User CRUD (storico, mergiato)
Reorganizatio-Repo-Structure ← refactor cartelle (storico)
Swagger-setup          ← feature branch docs API
18-gestione-sessione-escursione ← feature branch issue #18
```

Convenzioni operative del team:

- Una branch per ogni Issue GitHub (`<numero>-<slug>` o `<feature>-<area>`).
- PR obbligatoria verso `develop` (futuro) o `UI` (corrente) — **mai push diretto su `main`**.
- Commit semantici stile `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`.
- Merge tramite Pull Request con revisione di almeno un altro membro del team.

**Evidenza GitHub**: storia commit + grafico branch da catturare via `git log --graph --all --oneline` e screenshot della pagina /branches.

#### Product backlog (Sprint 1 scope)

> Backlog espresso come **User Story** con priorità (MoSCoW) e mapping ai RF di D1/D2.

| ID | User Story | Priority | RF D1/D2 | Stato |
|----|------------|----------|----------|-------|
| US-01 | Come **escursionista** voglio **registrarmi** e **verificare la mia email** così da accedere al sistema in sicurezza. | Must | RF0 | ✅ Done |
| US-02 | Come **escursionista** voglio **fare login con JWT** persistito offline così da non ri-inserire credenziali. | Must | RF0 | ✅ Done |
| US-03 | Come **rifugista** voglio un **flusso di registrazione dedicato** (nome rifugio, CAI, quota, posti, coordinate) così che il backend riconosca il mio ruolo. | Should | RF0 estesa | ✅ Done |
| US-04 | Come **escursionista** voglio poter **resettare la password** via email se la dimentico. | Should | RF0 estesa | ✅ Done |
| US-05 | Come **capogruppo** voglio **pianificare un'escursione** importando un GPX e generando un **codice invito TSM-XXXX**. | Must | RF11 | ✅ Done |
| US-06 | Come **partecipante** voglio **unirmi a una sessione** inserendo il codice invito. | Must | RF7 | ✅ Done |
| US-07 | Come **utente in una sessione** voglio vedere il **dettaglio sessione** con profilo altimetrico reale, meteo attuale e checklist gestibile. | Must | RF1 + RF11 | ✅ Done |
| US-08 | Come **capogruppo** voglio **modificare** i dettagli della sessione (data, ora, difficoltà, partecipanti max). | Should | RF11 | ✅ Done |
| US-09 | Come **capogruppo** voglio **avviare la sessione** ("AVVIA") passando lo status a `ACTIVE` e attivando il tracking GPS. | Must | RF8 | 🟠 Done con bug C1 (vedi §10.2) |
| US-10 | Come **utente in sessione** voglio che il **tracking GPS** registri distanza, dislivello, quota e tempo anche con schermo spento. | Must | RF8, RNF9 | 🟠 Done con bug C3 (manca permesso background) |
| US-11 | Come **utente in sessione** voglio una **mappa** con la mia posizione live (OSMdroid). | Must | RF10 | ✅ Done |
| US-12 | Come **utente** voglio premere un **SOS** dalla schermata di tracciamento. | Could (Sprint 1) | RF9 | 🟡 UI only (backend `/emergencies` Sprint 2) |
| US-13 | Come **utente** voglio vedere la **lista delle mie attività completate** con statistiche per anno. | Should | nuovo (post-D2) | ✅ Done (Room + API stats) |
| US-14 | Come **utente** voglio vedere il **dettaglio di un'attività completata** (mappa + metriche + punti CAI). | Should | nuovo | ✅ Done |
| US-15 | Come **utente** voglio vedere **meteo reale** per la località della sessione (TINIA). | Should | RF6 (auto-pause meteo parziale) | ✅ Done |

#### Definition of "Done"

Una user story è **Done** se rispetta tutti i seguenti criteri:

1. **Funzionale**: implementata su mobile **e** backend (se richiede entrambi), demo-bile su device reale.
2. **Test**: almeno 1 test case manuale documentato (passi + risultato atteso) — Sprint 1 accetta solo *design* dei test secondo la consegna del docente.
3. **Build**: l'APK si builda senza errori (`./gradlew assembleDebug`) e il backend parte (`npm run dev`) senza eccezioni a runtime.
4. **API contract**: ogni endpoint nuovo è descritto in Swagger (`swagger-output.json`).
5. **Sicurezza minima**: ogni endpoint che tocca dati utente è dietro `authenticate` middleware (vedi C2: violazione Sprint 1, da rimediare).
6. **Code review**: PR mergiata con almeno 1 review esplicita di un altro membro del team.
7. **Documentazione**: KDoc/JSDoc sulle funzioni pubbliche; bug noti registrati in `TSM_PROJECT_STATE.md` §10.
8. **No regressions**: il flusso "happy path" demo (auth → crea sessione → join → AVVIA → tracking → stop) resta funzionante.

### 11.3 Sprint #1

#### Sprint Goal

> *"Consegnare un'app TSM dimostrabile end-to-end che permetta a un capogruppo di pianificare un'escursione da GPX, condividere un codice invito, accogliere partecipanti, avviare il tracking GPS, completare l'attività e ritrovarla in 'Le mie attività', con un backend Node.js + MongoDB autenticato via JWT e integrato con un servizio meteo reale (TINIA)."*

#### Sprint Planning

- **Durata**: 4 settimane (16/04/2026 – 16/05/2026).
- **Capacità team**: 3 membri × ~10h/settimana = ~120h totali.
- **Story points totali pianificati**: 55 (riferimento Fibonacci 1/2/3/5/8/13).
- **Story points completati**: ~50 (US-12 lasciata UI-only, US-09 e US-10 done con bug noti).

##### Distribuzione (proposta da inserire nel D3 con burndown chart)

| Settimana | Pianificato | Completato | Note |
|-----------|-------------|-----------|------|
| W1 16/04–22/04 | 13 | 12 | Auth + register completi, register rifugio in progress |
| W2 23/04–29/04 | 16 | 17 | Session hub PIANIFICA + UNISCITI; meteo integrato |
| W3 30/04–06/05 | 14 | 14 | Session detail + edit + altimetric chart + tracking GPS engine |
| W4 07/05–16/05 | 12 | 7 | Activity list + activity detail Room + bugfix; restano 5 SP in debito (SOS backend, refinements) |

> 📊 Il **burndown chart** va generato a partire dalla tabella sopra (Excel/Sheets → curva ideale vs actual). Salvare come PNG `docs/sprint1_burndown.png`.

#### Test cases (design — sufficiente per Sprint 1)

> Solo *design*, non esecuzione automatica. Tabella copertura RF/US ↔ caso di test.

| TC | User Story | Tipo | Precondizione | Passi | Risultato atteso |
|----|-----------|------|---------------|-------|------------------|
| TC-01 | US-01 | E2E manuale | App pulita, server up | 1) Tap "Registrati" 2) inserire email/pwd 3) ricevere mail SMTP 4) tap link `tsm://auth/verify/...` 5) login | Login riuscito, JWT in EncryptedSharedPreferences |
| TC-02 | US-04 | E2E manuale | Utente esistente verificato | 1) tap "Password dimenticata" 2) email 3) form HTML 4) nuova pwd 5) login | Login con nuova pwd ok |
| TC-03 | US-05 | E2E manuale | Login fatto, GPX `.gpx` valido | 1) Sessione → PIANIFICA 2) Carica GPX 3) compila form 4) Crea | TSM-XXXX visualizzato + QR + sessione in UNISCITI |
| TC-04 | US-06 | E2E manuale | 2 account, sessione creata da A | 1) account B login 2) UNISCITI 3) inserire codice | Sessione visibile in lista UNISCITI di B |
| TC-05 | US-07 | UI manuale | Sessione con GPX | 1) Tap sessione | Profilo altimetrico, meteo, partecipanti, checklist drag-and-drop |
| TC-06 | US-09 | E2E manuale | Sessione PLANNED, utente = creator | 1) Apri sessione 2) AVVIA | Status = ACTIVE, switch a tab Registra, GPS tracking attivo |
| TC-07 | US-09 (bug C1) | E2E manuale | Sessione PLANNED, utente ≠ creator | 1) Join 2) AVVIA | **Atteso**: status ACTIVE per il partecipante. **Attuale**: 403 silent (BUG). |
| TC-08 | US-10 | E2E device fisico | Tracking attivo | 1) AVVIA 2) bloccare schermo 5 min 3) sbloccare | Distanza/quota aggiornate. **Attuale**: con bug C3, troncato dopo X min. |
| TC-09 | US-13 | UI manuale | Almeno 1 attività COMPLETED | 1) Home → "Le mie attività" | Card statistiche anno + lista attività |
| TC-10 | US-15 | E2E manuale | Sessione con coordinate Trentino | 1) Apri SessionDetail | Card meteo con forecast 3h e 24h |
| TC-11 | Sicurezza C2 | API manuale (Postman) | Server up | 1) `POST /weather/seed` SENZA Bearer | **Atteso**: 401. **Attuale**: 200 OK (BUG). |
| TC-12 | US-08 | UI manuale | Creator su sessione PLANNED | 1) Modifica 2) Salva | Pannello edit si chiude, dati persistiti |
| TC-13 | Reg US-03 | E2E manuale | App pulita | 1) Registrati rifugio 2) compila campi 3) submit | Account creato con role=rifugio, redirect verifica email |

#### Sprint Review (cosa portare in demo)

Demo live di 10 minuti che copre:

1. **0:00–1:30** Login → Auth entry → register utente nuovo → verifica email deep link `tsm://`.
2. **1:30–3:30** Capogruppo: PIANIFICA con import GPX reale (es. `Catinaccio.gpx`) → mostra altimetria reale generata → codice TSM-XXXX + QR.
3. **3:30–5:00** Partecipante (secondo account): UNISCITI con codice → SessionDetail con meteo TINIA reale + checklist drag-and-drop.
4. **5:00–7:00** Capogruppo: AVVIA → switch automatico a Registra → tracking GPS live (mappa OSMdroid) → ferma → COMPLETED.
5. **7:00–8:30** Home → "Le mie attività" → card statistiche + dettaglio attività con replay tracciato.
6. **8:30–10:00** Q&A + mostra Swagger su `/api-docs`.

#### Product backlog refinement (output per Sprint 2)

Story nuove o ri-prioritizzate emerse durante Sprint 1:

- US-16 (Must, Sprint 2): **fix bug C1** — permettere ai partecipanti di AVVIA / ricevere stato ACTIVE coordinato.
- US-17 (Must, Sprint 2): **fix bug C2** — proteggere route `/weather/*` con `authenticate` (e `requireRoles("admin")` per `/seed` e `/refresh`).
- US-18 (Must, Sprint 2): **fix bug C3** — aggiungere `ACCESS_BACKGROUND_LOCATION` e flusso di richiesta runtime.
- US-19 (Should, Sprint 2): **POST /emergencies** backend per chiudere RF9 (oggi UI-only).
- US-20 (Could, Sprint 2): **HomeScreen feed Sociale** (oggi placeholder).
- US-21 (Should, Sprint 2): **WorkManager store-and-forward** per upload telemetria GPS offline.
- US-22 (Could, Sprint 3): **Socket.io real-time positions** (RF12).
- US-23 (Could, Sprint 3): **BLE Mesh fallback SOS** (RF13).

#### Sprint Retrospective

**Cosa è andato bene**

- Integrazione GPX → altimetria reale → modello CAI: lavoro di Marco + Giacomo si è incastrato bene.
- Refactor backend → populate simmetrico ha sbloccato il PATCH editing che crashava.
- L'idea di un `SessionStartCoordinator` singleton bus ha tolto coupling tra SessionDetail e RegistraScreen.

**Cosa è andato male / da migliorare**

- 3 bug critici scoperti solo durante l'audit di fine sprint → introdurre un **Definition of Done check** prima della chiusura settimanale.
- Pattern Repository applicato solo a Login/Register: i 4 ViewModel scritti dopo bypassano Retrofit dal layer giusto → **standard architetturale** da concordare in Sprint 2 kick-off.
- Schema `User.sessionRoles` è stato dimenticato durante un refactor → un **modello = una PR** dovrebbe includere update di service+route+modello insieme.
- Nessun unit test scritto: Sprint 2 introdurre almeno una test class JUnit per ViewModel + un test Jest per `hikeSessionService`.

**Action items per Sprint 2 (commit immediato)**

1. Triage dei 3 bug critici nelle prime 2 giornate di Sprint 2 (US-16/17/18).
2. Pulizia codice morto (`AppRepository.kt`, `LocalDataSource.kt`, `/test` endpoint).
3. Setup CI minimale (GitHub Actions: build APK + lint backend) per evitare regressioni.
4. Aggiornare KDoc orfani identificati al §10.3 (M7).
5. Migrare `meetingDate` a `Date` (M2) con script di backfill.

---

## 12. Quick-reference: stato consegna 17/05

| Output | File | Stato |
|--------|------|-------|
| Documento D3 LaTeX | `docs/T6_D3_Ingegneria_Del_Software.md` | ✅ Completo (vedere §13 per 4 correzioni da fare) |
| Burndown Chart | integrato come TikZ nel D3 (dati da CSV) | ✅ Già nel LaTeX |
| Product Backlog CSV | `docs/Backlog V1 - Product Backlog.csv` | ✅ Nel repo |
| Sprint 1 Backlog CSV | `docs/Backlog V1 - Sprint 1 Backlog.csv` | ✅ Nel repo |
| Definition of Done CSV | `docs/Backlog V1 - Definition Of Done.csv` | ✅ Nel repo |
| Test Cases CSV | in lavorazione dal team | ⏳ Da aggiungere quando pronto |
| swagger-output.json | `swagger-output.json` in root | ✅ |
| Apiary link | `https://trentosmartmountain.docs.apiary.io` | ✅ nel D3 |
| Bug critici C1/C2/C3 | fixati nel codice il 17/05 | ✅ Fix applicati |

---

## 13. Stato documento D3 (17/05/2026)

### Valutazione `docs/T6_D3_Ingegneria_Del_Software.md`

**Qualità globale: 🟢 Eccellente — 90% pronto per la consegna.**

Il documento è un file LaTeX completo con stile coerente a D1/D2 (colori primary/secondary, fancyhdr, tcolorbox, tabularx, longtable, TikZ burndown). Contiene tutte le sezioni richieste dal docente.

#### Correzioni da fare prima della consegna

| # | Problema | Dove nel LaTeX | Fix |
|---|----------|----------------|-----|
| 1 | Bug C1/C2/C3: descritti come "Fix: Sprint 2" ma **già fixati** | Retrospective + Refinement | ✅ Aggiornati come "Fix applicato il 17/05 durante audit interno" |
| 2 | Action items retrospective (cancella dead code) già eseguiti | `\subsubsection*{Action items}` | ✅ Marcati come completati |

> **Nota T6**: il prefisso `T6_` nel nome file è corretto per la consegna del gruppo 6, Milestone 3.
> **Nota durata sprint**: 1 settimana (09/05–17/05) con ~70h/sett è conforme alla realtà del team (~12h/giorno × 3 membri).

#### Sezioni già corrette e complete

- ✅ Team Members con GitHub account corretti (`@federicocattelan`, `@STUSSY-user`, `@giacomoradin`)
- ✅ Project idea (3-5 righe)
- ✅ Links (GitHub + Swagger + Apiary)
- ✅ Branching table con tutti i branch reali + nota "non cancellati per docenti"
- ✅ Product Backlog US-01→US-15 con stati reali
- ✅ Definition of Done 8 criteri
- ✅ Sprint Goal in tcolorbox
- ✅ Sprint Backlog longtable con volunteer + stima + status
- ✅ Burndown Chart TikZ con dati reali dal CSV (effort iniziale 252 → normalizzato 200)
- ✅ Test Cases TC-01→TC-13 (inclusi bug espliciti TC-07/08/11)
- ✅ Sprint Review (demo 6 punti, 15 minuti)
- ✅ Backlog Refinement US-16→US-23
- ✅ Retrospective: what worked / bug critici / debito tecnico / action items
- ✅ Appendice A con tutti gli endpoint API (21 route)

### Statistiche commit GitHub (17/05/2026)

| Membro | GitHub | Commit |
|--------|--------|--------|
| Marco Christian Stoica | `@STUSSY-user` | ~66 |
| Federico Cattelan | `@federicoca` | ~38 |
| Giacomo Radin | `@giacomoradin` | ~40 |

**Tutti e tre i componenti hanno contribuito attivamente** (commit visibili su GitHub → Insights → Contributors). Il D3 già lo dichiara correttamente a §1.

### Come mostrare i commit al docente

1. **GitHub web** → repository → `Insights` → `Contributors` → screenshot del grafico
2. **GitHub web** → repository → `Commits` → mostra lista per branch
3. **Comando locale** per PDF/report: `git log --all --format="%ad | %an | %s" --date=short > docs/commit_log.txt`
