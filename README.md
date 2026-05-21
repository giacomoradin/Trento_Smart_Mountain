# Trento Smart Mountain — Stato del Progetto e Piano Implementativo

## 1. Visione e Obiettivi del Progetto

Trento Smart Mountain (TSM) è un ecosistema digitale per l'ambiente montano trentino. Supera le app di navigazione passiva (Komoot, AllTrails) integrando **sicurezza attiva dei gruppi** (mesh BLE offline, SOS), **gamification educativa** (quiz, NFC checkpoint vetta, crediti sociali) e **gestione rifugi** (IoT, telemetria).

**Gruppo:** ID-6 — Federico Cattelan (242111), Marco Christian Stoica (246443), Giacomo Radin (242907)

### 1.1 Macro-obiettivi (D1 §1)

| ID  | Obiettivo                             | Descrizione                                                    |
| --- | ------------------------------------- | -------------------------------------------------------------- |
| O1  | Ecosistema digitale a valore aggiunto | Aggregatore community per tutte le stagioni                    |
| O2  | Sicurezza proattiva                   | Tracciamento e coordinamento gruppi escursione                 |
| O3  | Resilienza comunicativa               | Comunicazione tra dispositivi in assenza di rete (BLE Mesh)    |
| O4  | Sostenibilità ed Economia Circolare   | Gamification educativa, NFC checkpoint, crediti sociali        |
| O5  | Coinvolgimento multisettoriale        | Cittadini, turisti, guide, gestori rifugi, operatori ecologici |

### 1.2 Pivot D2 rispetto a D1

In D2 è stata **rimossa** la raccolta fisica dei rifiuti (RF15-RF18 di D1) per ragioni di sicurezza. Il framework di sostenibilità si concentra ora su:

- **Gamification educativa** (Sustainability Paths): quiz su flora, fauna, sicurezza
- **Certificazione di vetta via NFC**: totem fisici ai checkpoint, scansione per crediti
- **Crowdsourcing segnalazioni**: manutenzione sentieri

---

## 2. Stack Tecnologico

| Layer                 | Tecnologia                                              | Note                                          |
| --------------------- | ------------------------------------------------------- | --------------------------------------------- |
| **Mobile**            | Kotlin 2.0.21, Jetpack Compose (BOM 2024.12), Material3 | minSdk 28, targetSdk 35                       |
| **Mobile DB**         | Room 2.6.1 + KSP                                        | Cache profilo, sessioni, telemetria offline   |
| **Mobile Networking** | Retrofit 2.11 + OkHttp 4.12                             | AuthInterceptor con Bearer JWT                |
| **Mobile Security**   | EncryptedSharedPreferences                              | JWT cifrato localmente                        |
| **Mappa Mobile**      | OSMdroid 6.1.20                                         | Tracking GPS con FusedLocationProvider        |
| **QR Code**           | ZXing Core 3.5.3                                        | Generazione QR sessione                       |
| **Drag-and-Drop**     | sh.calvin.reorderable 2.4.3                             | Checklist riordinabile                        |
| **Backend**           | Node.js + Express 4                                     | Monolite modulare                             |
| **Backend DB**        | MongoDB (Mongoose 8)                                    | GeoJSON 2dsphere, TTL indexes                 |
| **Meteo**             | meteo.report / TINIA API                                | Forecast 3h e 24h via weatherService di Marco |
| **Autenticazione**    | JWT (bcrypt hash)                                       | Deep link `tsm://auth` per verifica email     |
| **Email**             | Nodemailer (Gmail SMTP, retry 3x esponenziale)          | Verifica email + reset password               |
| **Infra**             | Docker Compose                                          | MongoDB + Mosquitto (MQTT)                    |

---

## 3. Requisiti Funzionali — Stato di Copertura Sprint 1

### RF coperti dal codice

| RF      | Descrizione                                            | Stato Sprint 1                                                                                                                                    |
| ------- | ------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| RF0     | Autenticazione                                         | ✅ Completo (register → SMTP verify → login JWT)                                                                                                  |
| RF7     | Unirsi a escursione tramite codice invito              | ✅ Completo (codice TSM-XXXX + UNISCITI tab)                                                                                                      |
| RF8     | Tracciamento GPS in background                         | ✅ Completo (ForegroundService + HikeTrackingEngine + auto-pause + `ACCESS_BACKGROUND_LOCATION` runtime)                                          |
| RF9     | Invio SOS con coordinate GPS                           | �� Parziale (UI SOS dialog in RegistraScreen; backend `POST /emergencies` non implementato)                                                       |
| RF10    | Mappa offline con posizione utente                     | ✅ Completo (OSMdroid + OpenTopoMapTileSource + tracking live)                                                                                    |
| RF11    | Creare escursione con codice invito univoco            | ✅ Completo (PIANIFICA tab, GPX import, generazione TSM-XXXX)                                                                                     |
| RF14    | Allerte push pericoli (Rifugio)                        | �� Parziale (RegisterRifugio funzionante, dashboard placeholder)                                                                                  |
| **NEW** | "Le Mie Attività" — storico locale + aggregato annuale | ✅ Completo (Room `completed_activities` + `GET /api/v1/sessions/stats` aggregato per anno, card metriche, lista, dettaglio con replay tracciato) |

### RF non ancora coperti

| RF        | Descrizione                                                       | Note                                           |
| --------- | ----------------------------------------------------------------- | ---------------------------------------------- |
| RF1-RF6   | Itinerari, difficoltà, equipaggiamento, BitChat, Auto-Pause meteo | RF6 auto-pause GPS implementato                |
| RF12      | Dashboard tracking real-time capogruppo                           | Richiede Socket.io (installato, non integrato) |
| RF13      | Broadcast allarmi emergenza                                       | Richiede BLE Mesh (architettura pianificata)   |
| RF15-RF18 | (Soppressi in D2)                                                 | —                                              |
| RF19      | Promozioni admin + crediti sociali                                | Backend DTO creato, UI non implementata        |

---

## 4. Deliverable Sprint 1 — Cosa è stato Implementato

### 4.1 Backend Node.js

#### Modelli MongoDB

| Modello         | File                    | Campi chiave                                                                                                                                                                                                                                                                                                                                                                  |
| --------------- | ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **User**        | `models/user.js`        | username, email, passwordHash, role, isVerified, verificationToken, passwordResetToken/Expires, rifugioDetails, **sessionRoles[]** (groupId/role/createdBy — ora dichiarato nello schema ✅ fix M1), virtual **mySessions**                                                                                                                                                   |
| **HikeSession** | `models/hikeSession.js` | creatorId, routeDetails (name/startPoint/endPoint senza default ✅ fix M3 + indice 2dsphere `sparse:true`/difficultyLevel/elevationGain), inviteCode (TSM-XXXX), participants[], status lifecycle, meetingDate/Time, maxParticipants, gpxStats (distanceKm/elevationGainM/trackPoints/**elevationProfile**/estimatedPoints), statoFailover, lastHeartbeat, startTime, endTime |
| **Location**    | `models/location.js`    | externalId, type (town/poi), name, elevation, location (GeoJSON), regionId, forecasts (slots3h/slots24h) — modulo meteo                                                                                                                                                                                                                                                       |

#### Endpoint implementati e funzionanti

| Metodo   | Route                                   | Auth               | Descrizione                                                                                                                                                                              |
| -------- | --------------------------------------- | ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `POST`   | `/auth/login`                           | No                 | Login → JWT                                                                                                                                                                              |
| `GET`    | `/auth/verify/:token`                   | No                 | Verifica email → deep link tsm://                                                                                                                                                        |
| `POST`   | `/auth/forgot-password`                 | No                 | Reset password (email link)                                                                                                                                                              |
| `GET`    | `/auth/reset-password/:token`           | No                 | Form HTML reset                                                                                                                                                                          |
| `POST`   | `/auth/reset-password/:token`           | No                 | Salva nuova password (JSON o form)                                                                                                                                                       |
| `POST`   | `/users`                                | No                 | Registrazione utente/rifugio (con rifugioDetails)                                                                                                                                        |
| `GET`    | `/users/:id`                            | JWT                | Profilo utente                                                                                                                                                                           |
| `PUT`    | `/users/:id`                            | JWT+admin          | Aggiorna utente                                                                                                                                                                          |
| `DELETE` | `/users/:id`                            | JWT+admin          | Elimina utente                                                                                                                                                                           |
| `POST`   | `/api/v1/sessions`                      | JWT                | Crea sessione (con GPX stats + estimatedPoints)                                                                                                                                          |
| `GET`    | `/api/v1/sessions/my`                   | JWT                | Le mie sessioni (populate creator + participants)                                                                                                                                        |
| `GET`    | `/api/v1/sessions/stats?year=YYYY`      | JWT                | **NUOVO** — Statistiche aggregate annuali per "Le Mie Attività" (totalActivities, totalDistanceKm, totalElevationGainM, totalPoints, monthlyActivityCount[12], monthlyAvgDifficulty[12]) |
| `GET`    | `/api/v1/sessions/:id`                  | JWT                | Dettaglio sessione (fully populated)                                                                                                                                                     |
| `POST`   | `/api/v1/sessions/join`                 | JWT                | Unisciti con codice TSM-XXXX (ora populated ✅ fix M4)                                                                                                                                   |
| `POST`   | `/api/v1/sessions/:id/leave`            | JWT                | Abbandona sessione (non-creator; `CREATOR_CANNOT_LEAVE` se creator)                                                                                                                      |
| `DELETE` | `/api/v1/sessions/:id`                  | JWT (creator)      | Elimina sessione (rimuove anche per i partecipanti)                                                                                                                                      |
| `PATCH`  | `/api/v1/sessions/:id`                  | JWT (creator)      | Modifica dettagli (populate entrambi i campi → fix crash Gson)                                                                                                                           |
| `PATCH`  | `/api/v1/sessions/:id/status`           | JWT (creator)      | PLANNED→ACTIVE→COMPLETED (vedi C1: solo creator)                                                                                                                                         |
| `GET`    | `/weather/locations/nearby`             | No                 | Stazioni meteo per coordinate (2dsphere)                                                                                                                                                 |
| `GET`    | `/weather/locations/search`             | No                 | Cerca stazioni per nome                                                                                                                                                                  |
| `GET`    | `/weather/forecast/:externalId`         | No                 | Forecast 3h + 24h (cache 1h MongoDB)                                                                                                                                                     |
| `POST`   | `/weather/forecast/:externalId/refresh` | **JWT + admin** ✅ | Forza refresh forecast (fix C2 parziale)                                                                                                                                                 |
| `POST`   | `/weather/seed`                         | **JWT + admin** ✅ | Popola DB con towns + POI da TINIA API (fix C2 parziale)                                                                                                                                 |

#### Logica business implementata

- **checkUserAlreadyInActiveSession**: blocca solo sessioni `ACTIVE` (non PLANNED) — un utente può pianificare più escursioni future
- **generateInviteCode**: formato `TSM-XXXX` (4 hex uppercase)
- **updateSessionDetails**: populate simmetrico (`creatorId` + `participants.userId`) per evitare crash Gson su client Kotlin
- **SMTP retry**: `sendMailWithRetry` con backoff esponenziale (3 tentativi: 2s, 4s, 8s)
- **Password reset**: token monouso con scadenza 1h, form HTML risponsivo

#### Endpoint non ancora implementati (Sprint 2+)

| Metodo           | Route                                 | Priorità                                                                |
| ---------------- | ------------------------------------- | ----------------------------------------------------------------------- |
| `POST`           | `/api/v1/emergencies`                 | Alta — SOS con firma ECC                                                |
| `POST`           | `/api/v1/sessions/:id/telemetry`      | Alta — GPS batch upload                                                 |
| `GET`            | `/api/v1/sessions/:id/positions`      | Alta — Posizioni live gruppo                                            |
| `POST`           | `/api/v1/users/:id/gamification/sync` | Media — Event Sourcing crediti                                          |
| `GET/POST`       | `/api/v1/quiz/...`                    | Media — Quiz educativi                                                  |
| `GET/POST`       | `/api/v1/nfc/checkpoint/...`          | Bassa — NFC totem                                                       |
| `WS`/`Socket.io` | `/sessions/:id/live-positions`        | Media — Dashboard real-time gruppo (libreria installata, non integrata) |
| `MQTT topic`     | `tsm/rifugio/{id}/telemetry`          | Media — IoT gateway (mosquitto in docker, gateway python stub vuoto)    |

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

| Schermata                          | File                                               | Stato          | Note                                                                                                                                                          |
| ---------------------------------- | -------------------------------------------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **AuthEntryScreen**                | `auth/AuthEntryScreen.kt`                          | ✅ Completo    | Logo TSM Canvas (montagna + dot ciano), tagline, 3 bottoni                                                                                                    |
| **LoginScreen**                    | `login/LoginScreen.kt`                             | ✅ Completo    | Icone campi, toggle password, "Password dimenticata?", offline badge, link Registrati                                                                         |
| **RegisterScreen**                 | `register/RegisterScreen.kt`                       | ✅ Completo    | Step indicator, checkbox ToS GPS, validazione                                                                                                                 |
| **RegisterRifugioScreen**          | `register/RegisterRifugioScreen.kt`                | ✅ Completo    | Info box, campi rifugio (nome/CAI/quota/posti/coordinate), disclaimer verifica manuale                                                                        |
| **ForgotPasswordScreen**           | `register/ForgotPasswordScreen.kt`                 | ✅ Completo    | Email input + stato "email inviata"                                                                                                                           |
| **EmailVerificationPendingScreen** | `register/EmailVerificationPendingScreen.kt`       | ✅ Completo    | Istruzioni verifica                                                                                                                                           |
| **HikerMainScreen**                | `main/HikerMainScreen.kt`                          | ✅ Completo    | Bottom nav 4 tab, auto-switch a Registra via SessionStartCoordinator                                                                                          |
| **HomeScreen**                     | `home/HomeScreen.kt`                               | �� Funzionale  | Tab Sociale (placeholder) + Tab Personale → inietta `ActivityListScreen`                                                                                      |
| **ActivityListScreen**             | `home/ActivityListScreen.kt` (NUOVO, 495 righe)    | ✅ Completo    | "Le Mie Attività": card statistiche annuali, bar chart mensile, lista cronologica con paginazione                                                             |
| **ActivityDetailScreen**           | `home/ActivityDetailScreen.kt` (NUOVO, 705 righe)  | ✅ Completo    | Dettaglio attività: replay tracciato OSMdroid, metriche complete, punti CAI finali, link a sessione di origine                                                |
| **SessionHubScreen — PIANIFICA**   | `session/SessionHubScreen.kt`                      | ✅ Completo    | GPX import (XmlPullParser), form sessione, DatePicker/TimePicker, QR preview (ZXing), inviteCode TSM-XXXX                                                     |
| **SessionHubScreen — UNISCITI**    | `session/SessionHubScreen.kt`                      | ✅ Completo    | Code boxes OTP (TextFieldValue), lista sessioni ordinata, AVVIA/Elimina/Abbandona                                                                             |
| **SessionDetailScreen**            | `session/SessionDetailScreen.kt`                   | ✅ Completo    | Profilo altimetrico GPX reale (Canvas), stima CAI, meteo reale TINIA, checklist drag-and-drop, partecipanti avatar, edit mode creator, codice invito copyable |
| **RegistraScreen**                 | `registra/RegistraScreen.kt` + 10 sotto-componenti | ✅ Completo    | OSMdroid + GPS tracking live, metriche, FAB SOS, auto-pause, OpenTopoMapTileSource, GpsSignalIndicator, salvataggio in Room a STOP                            |
| **ProfileScreen**                  | `profile/ProfileScreen.kt`                         | ✅ Completo    | Username da Room+API, hint offline, logout                                                                                                                    |
| **RefugeMainScreen**               | `refuge/RefugeMainScreen.kt`                       | �� Placeholder | Dashboard rifugista                                                                                                                                           |

#### ViewModels implementati

| ViewModel                         | Responsabilità                                                                                                                                                                          |
| --------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `LoginViewModel`                  | Validazione + login via `AuthRepository`                                                                                                                                                |
| `RegisterViewModel`               | Registrazione utente via `RegistrationRepository`                                                                                                                                       |
| `RegisterRifugioViewModel`        | Registrazione rifugio con campi specifici (`RegisterRifugioRequest` DTO dedicato)                                                                                                       |
| `ForgotPasswordViewModel`         | Richiesta reset password via email                                                                                                                                                      |
| `ProfileViewModel`                | Profilo da Room + API tramite `ProfileRepository` (offline-first state)                                                                                                                 |
| `SessionPlanViewModel`            | GPX parsing (smoothing+valley-peak+sampling), form sessione, generazione preview code                                                                                                   |
| `SessionJoinViewModel`            | Lista sessioni, join codice, leave/delete con rilevamento creator/partecipante                                                                                                          |
| `SessionDetailViewModel`          | Dettaglio sessione, checklist, edit mode, meteo TINIA, salvataggio modifiche                                                                                                            |
| `RegistraViewModel`               | `HikeTrackingEngine` + `FusedLocationPublisher`, auto-pause accelerometro, metriche live, auto-start da `SessionStartCoordinator`, salvataggio `CompletedActivityEntity` in Room a STOP |
| `ActivityListViewModel` (NUOVO)   | Carica `GET /sessions/stats` + Room `completed_activities`; sync sessioni COMPLETED dal backend in Room                                                                                 |
| `ActivityDetailViewModel` (NUOVO) | Recupera entity Room, decodifica `trackLatLng` JSON in `List<GeoPoint>`, prepara metriche e mappa replay                                                                                |

#### Data layer — DTOs principali

| DTO                              | Campi chiave                                                                                                                         |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `SessionResponse`                | \_id, inviteCode, status, routeDetails, meetingDate/Time, gpxStats, creatorId (populated), participants (populated), maxParticipants |
| `GpxStats`                       | distanceKm, elevationGainM, trackPoints, **elevationProfile** (max 50 punti), **estimatedPoints**                                    |
| `WeatherForecastResponse`        | location, forecast3h (List\<Slot>), forecast24h (List\<Slot>)                                                                        |
| `WeatherForecastSlot`            | validFrom/To, temperature, temperatureMin/Max, rainProbability, windSpeed/Direction, skyCondition                                    |
| `ApiMessageBody` (NUOVO)         | `{ message?: String }` — usato come Response body per PATCH/DELETE/leave → evita crash Gson eager su payload non popolato            |
| `ActivityStatsResponse` (NUOVO)  | year, totalActivities, totalDistanceKm, totalElevationGainM, totalPoints, monthlyActivityCount[12], monthlyAvgDifficulty[12]         |
| `RegisterRifugioRequest` (NUOVO) | username, email, password, role=`rifugio`, rifugioDetails (rifugioName/caiCode/quota/posti/coordinates)                              |
| `ForgotPasswordRequest` (NUOVO)  | `{ email }`                                                                                                                          |

#### Persistenza locale — Room v3 (`tsm.db`)

| Entity                            | Tabella                | Note                                                                                                                                                                                                                                                                                                                          |
| --------------------------------- | ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `CachedUserProfileEntity`         | `cached_user_profile`  | PK userId, username, updatedAtEpochMs — cache offline ProfileScreen                                                                                                                                                                                                                                                           |
| `CompletedActivityEntity` (NUOVO) | `completed_activities` | PK id (UUID), sessionId?, name, activityType (default "hiking"), startTimeMs, endTimeMs, movingSeconds, totalSeconds, distanceMeters, elevationGainMeters, currentAltitudeMeters?, difficultyLevel?, **trackLatLng** (JSON array di triplet [lat,lon,alt], max 200 punti), estimatedCalories?, points?, isSynced, completedAt |

Doppia sorgente di scrittura:

- `RegistraViewModel.confirmStopTracking` → attività registrate localmente con tracciato GPS reale
- `ActivityListViewModel.syncCompletedSessionsToRoom` → sessioni COMPLETED importate dal backend (`trackLatLng="[]"`, `isSynced=true`)

#### Modulo di stima escursione (HikeEstimation.kt)

| Funzione                           | Formula                                                      |
| ---------------------------------- | ------------------------------------------------------------ |
| `caiTimeHours(D, H)`               | Polinomio CAI su pendenza P=(H/M)×100 → min/km × distKm / 60 |
| `equivalentDistance(D, H)`         | D + H/100 (100m salita ≡ 1km piano)                          |
| `naismithTimeHours(D, H)`          | D/4 + H/300                                                  |
| `estimatedPoints(D, H, K=10)`      | round(K × D_eq) — pianificazione, μ=1.0                      |
| `finalPoints(D, H, T_reale, K=10)` | round(K × D_eq × clip(T_nom/T_reale, 0.8, 1.2))              |

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

| Feature                           | Descrizione                                                                                            |
| --------------------------------- | ------------------------------------------------------------------------------------------------------ |
| **Dark Theme completo**           | darkColorScheme Material3, TsmBackground #121212                                                       |
| **Token design**                  | TsmPrimary #3F7020, TsmAccent #4FC3F7, TsmSos #6B0D0D, TsmSurface #1E1E1E, TsmBorder #5D4037           |
| **Offline badge**                 | Login mostra info su token locale se offline                                                           |
| **Codice invito sempre visibile** | SessionCard + SessionDetail con copia in clipboard                                                     |
| **GPX parser robusto**            | Smoothing moving-average (w=5), valley-peak threshold 10m, interpolazione null, campionamento 50 punti |
| **Profilo altimetrico reale**     | Canvas normalizzato min/max dal GPX, area fill gradient                                                |
| **Meteo reale TINIA**             | Nearest town via 2dsphere + forecast 3h+24h, skyCondition emoji mapper                                 |
| **Checklist drag-and-drop**       | ReorderableColumn (sh.calvin.reorderable), toggle check, add/remove                                    |
| **Edit mode sessione**            | Solo creator (check JWT userId), PATCH con populate fix, auto-close su successo                        |
| **AVVIA → Registra**              | SessionStartCoordinator bus singleton, HikerMainScreen LaunchedEffect, RegistraViewModel auto-start    |
| **PATCH Gson crash fix**          | `updateSession` ora usa `Response<ApiMessageBody>` (backend popola entrambi i campi ref)               |

---

## 5. Bug Risolti nel Sprint 1

| Bug                                                | Root Cause                                                           | Fix                                                                                            |
| -------------------------------------------------- | -------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| SessionCreatedDialog OK non chiudeva               | `onDismiss` no-op; state `sessionCreated` non resettata              | `resetAfterCreation()` + callback `onSessionCreated` → `subTab = 1`                            |
| "Sei già in una sessione attiva" alla creazione    | `checkUserAlreadyInActiveSession` bloccava anche sessioni PLANNED    | Cambiato a `status: "ACTIVE"` only                                                             |
| Code box codice: cursore sbagliato + history ghost | `BasicTextField(value: String)` non sincronizzato con state esterno  | `TextFieldValue` con `LaunchedEffect(code)` per riallineamento esplicito                       |
| Tab UNISCITI non si aggiornava dopo join           | `loadSessions()` chiamato solo in `init {}`                          | `LaunchedEffect(Unit)` + `DisposableEffect(ON_RESUME)`                                         |
| Dislivello GPX sovrastimato (2-3×)                 | Parser sommava tutte le variazioni positive (noise GPS)              | Smoothing MA(5) + valley-peak threshold 10m                                                    |
| Edit mode non si chiudeva dopo save                | `saveEdit` non ricaricava la sessione; dropdown locale non resettato | `loadSession(id)` dopo save + `LaunchedEffect(uiState.editMode)` per picker locali             |
| `updateSession` crash `IllegalStateException`      | Gson deserializzazione eager: `participants.userId` era ObjectId raw | Backend: populate doppio (creatorId + participants.userId); Kotlin: `Response<ApiMessageBody>` |
| UNISCITI: host non poteva eliminare                | `leaveSession` restituiva 403 per il creator                         | `RemovalMode.DELETE` vs `LEAVE` basato su `creatorId._id == currentUserId`                     |

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

| Permesso                      | Stato                    | Uso                                                                                    |
| ----------------------------- | ------------------------ | -------------------------------------------------------------------------------------- |
| `INTERNET`                    | ✅                       | Tutte le chiamate di rete                                                              |
| `ACCESS_NETWORK_STATE`        | ✅                       | Rilevamento connettività (offline badge)                                               |
| `ACCESS_FINE_LOCATION`        | ✅                       | GPS preciso per tracking                                                               |
| `ACCESS_COARSE_LOCATION`      | ✅                       | GPS approssimativo                                                                     |
| `ACCESS_BACKGROUND_LOCATION`  | ✅ **AGGIUNTO** (fix C3) | D2 RNF9: tracking GPS a schermo spento (richiesto runtime DOPO `ACCESS_FINE_LOCATION`) |
| `FOREGROUND_SERVICE`          | ✅                       | ForegroundTrackingService                                                              |
| `FOREGROUND_SERVICE_LOCATION` | ✅                       | Tracking GPS foreground                                                                |
| `POST_NOTIFICATIONS`          | ✅                       | Notifica tracking attivo (Android 13+)                                                 |
| `WAKE_LOCK`                   | ✅ **AGGIUNTO**          | Evita drop service durante tracking lungo a schermo spento                             |
| Intent-filter `tsm://auth`    | ✅                       | Deep link verifica email                                                               |
| `BLUETOOTH_SCAN`              | ❌                       | Futuro: BLE Mesh                                                                       |
| `BLUETOOTH_ADVERTISE`         | ❌                       | Futuro: BLE Mesh                                                                       |
| `NFC`                         | ❌                       | Futuro: checkpoint vetta                                                               |
| `VIBRATE`                     | ❌                       | Futuro: feedback aptico SOS                                                            |

---

## 8. Gap residui — Da completare nei prossimi Sprint

### Sprint 2 — Priorità Alta

| Feature                                    | Layer            | Dipendenze                               |
| ------------------------------------------ | ---------------- | ---------------------------------------- |
| Backend POST /emergencies (SOS con ECC)    | Backend          | ECC key generation                       |
| GPS telemetry batch upload a fine sessione | Mobile + Backend | POST /sessions/:id/telemetry             |
| HomeScreen feed + storico attività         | Mobile           | GET /sessions/my già esistente           |
| Salvataggio attività completata in Home    | Mobile           | `confirmStopTracking` → status COMPLETED |
| BLE Mesh fallback SOS                      | Mobile           | Hardware + biblioteca BLE                |

### Sprint 3 — Priorità Media

| Feature                                      | Layer            | Note                         |
| -------------------------------------------- | ---------------- | ---------------------------- |
| Socket.io real-time posizioni gruppo         | Backend + Mobile | Installato ma non integrato  |
| EducationalScreen quiz + NFC checkpoint      | Mobile + Backend | Quiz model non ancora creato |
| Social Credits gamification (Event Sourcing) | Backend          | user_event_store collection  |
| ProfileScreen avatar + livello + badge       | Mobile           | HOME SOCIAL non implementata |
| MQTT IoT gateway rifugio                     | Backend          | Installato, non integrato    |

### Debito tecnico noto

| Problema                                                                       | File                                  | Severità | Stato 17/05                                                                              |
| ------------------------------------------------------------------------------ | ------------------------------------- | -------- | ---------------------------------------------------------------------------------------- |
| `userSchema.sessionRoles` referenziato in service ma non nello schema          | `user.js` / `hikeSessionService.js`   | Media    | �� RISOLTO (M1)                                                                          |
| `leaveSession` restituisce doc non-populated (route usa `ApiMessageBody` → ok) | `hikeSessionService.js`               | Bassa    | �� Tollerato                                                                             |
| `POST /weather/seed` non ha middleware admin                                   | `weatherRoutes.js`                    | Media    | �� RISOLTO (C2)                                                                          |
| WorkManager Store-and-Forward (sync batch offline)                             | Mobile                                | Alta     | �� Aperto — richiede dipendenza WorkManager                                              |
| `joinSession` non popola il response                                           | `hikeSessionService.js`               | Media    | �� RISOLTO (M4)                                                                          |
| Pattern Repository non applicato ai ViewModel sessione/attività                | Mobile                                | Media    | �� Aperto — `SessionRepository`/`ActivityRepository` da introdurre                       |
| `meetingDate` String invece di Date                                            | `hikeSession.js`                      | Media    | �� Aperto                                                                                |
| Test (Jest backend / JUnit mobile)                                             | tutto                                 | Alta     | �� Aperto — zero test scritti finora                                                     |
| IoT gateway `iot/gateway/src/main.py` vuoto                                    | `iot/gateway/src/main.py`             | Media    | �� Aperto — bootstrap pronto (docker-compose + mosquitto), implementazione MQTT mancante |
| `iot/mosquitto/config/mosquitto.conf` vuoto                                    | `iot/mosquitto/config/mosquitto.conf` | Media    | �� Aperto — broker parte con config default, va aggiunta autenticazione/ACL              |

---
