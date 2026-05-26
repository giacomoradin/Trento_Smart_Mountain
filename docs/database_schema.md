# Database Schema — Trento Smart Mountain

> Documentazione delle collezioni MongoDB e dello schema Room (locale Android).
>
> **Ultima revisione**: 24/05/2026 — Sprint 2 in corso (aggiunta collezione `activities`).
> **Riferimenti**: `backend/src/models/`, `mobile/.../data/local/db/`, D2 §4.1.

---

## 1. Backend — MongoDB

Database: `tsm` (configurabile via `MONGO_URI` in `.env`).

### 1.1 Collezione `users` — pattern Discriminator

**Modelli**:

- `backend/src/models/user.js` — schema BASE condiviso (con `discriminatorKey: "role"`)
- `backend/src/models/hiker.js` — discriminator `groupLeader`
- `backend/src/models/refuge.js` — discriminator `rifugio` (con campi struttura)
- `backend/src/models/admin.js` — discriminator `admin`

**Collection MongoDB**: `users` (unica, condivisa da tutti e 3 i discriminatori)

#### Refactor 2026-05: perché discriminator

Prima del refactor lo schema User era "fat" e conteneva `rifugioDetails: null` anche per gli escursionisti. Con i discriminator Mongoose:

- Una sola collection MongoDB → login, populate, JWT auth invariati
- Schema specializzato per ruolo → no campi inutili nei documenti
- Route separate (`/hikers`, `/refuges`, `/admin`) → API pulite
- Nessuna migrazione dati: documenti esistenti restano compatibili

#### Schema BASE (`user.js`)

Tutti i discriminatori ereditano questi campi:

```javascript
{
  _id: ObjectId,
  username: String,                   // unique, required
  email: String,                      // unique, required
  passwordHash: String,               // bcrypt cost 10, required
  role: String,                       // discriminator key → "groupLeader" | "rifugio" | "admin"
  isVerified: Boolean,                // default false
  verificationToken: String,
  passwordResetToken: String,
  passwordResetExpires: Date,

  sessionRoles: [
    {
      groupId: ObjectId,              // ref HikeSession
      role: String,                   // enum ["groupLeader", "hiker"]
      createdBy: ObjectId,            // ref User
    }
  ],

  createdAt: Date,
}
```

#### Discriminator `Hiker` (`role: "groupLeader"`)

Estensione Sprint 2 con profilo v2, gamification, foto profilo:

```javascript
{
  // ... tutti i campi base User, e in più:
  socialCredits: Number,                 // default 0, indexed
  weeklyGoals: { km, elevM, count },     // obiettivi settimanali
  nfcStats: { scansCount, scansCredits },// telemetria scansioni NFC
  rewardedQuizzes: [ObjectId],           // idempotency claim crediti quiz

  personalInfo: {                        // profilo v2 — opzionali, skippable
    sex: "M"|"F"|"X"|"N",
    birthDate: Date,                     // anti-cheat lock (LockedFieldError 409)
    heightCm: Number,
    weightKg: Number,
    avatarUrl: String,                   // ★ data URI Base64 (foto profilo, Sprint 2 serale)
                                         //   pattern Joi: data:image/(jpeg|png|webp);base64,...
                                         //   max ~7 MB stringa (body cap 5 MB)
                                         //   pubblico anche per other-view (vedi userPrivacy.js)
  },
  experience: {
    caiLevel: "T"|"E"|"EE"|"EEA",        // anti-cheat lock
    baselineFitness: "sedentary"|"active"|"sport"|"athlete",
    weeklyTrainingFreq: "0-1"|"2-3"|"4+",
  },
  preferences: {
    units: "metric"|"imperial",
    language: String,                    // ISO 639-1
    notifications: { pushEnabled, emailDigest, fcmToken },
    privacy: { profileVisibility },
  },
  profileCompletedAt: Date,              // null = primo accesso, mostra banner onboarding
}
```

**NB importante (lesson learned 26/05):** tutti i write su questi campi devono
usare il modello `Hiker` (es. `Hiker.findByIdAndUpdate(...)`), MAI il modello
base `User`. Lo strict mode di Mongoose applica lo schema del modello con cui
esegui la query, e l'update via `User.*` viene droppato silenziosamente per
i campi del discriminator. Vedi sezione `discriminator.test.js` per il
contratto fissato in test.

#### Discriminator `Refuge` (`role: "rifugio"`)

Campi specifici **flat sul documento** (non più subdocument):

```javascript
{
  // ... tutti i campi base User, e in più:
  rifugioName: String,                // required per Refuge
  caiCode: String,                    // es. "B046"
  quota: Number,                      // metri s.l.m.
  posti: Number,                      // capienza posti letto
  coordinates: String,                // "lat lng" testuale
}
```

#### Discriminator `Admin` (`role: "admin"`)

Nessun campo extra. Estensioni future:

- `permissions: [String]` — permessi granulari
- `auditLog: [...]` — tracciamento azioni

// Virtual populate (NON salvato sul documento):
userSchema.virtual("mySessions", {
ref: "HikeSession",
localField: "\_id",
foreignField: "participants.userId"
});

````

#### Indici

```javascript
// Auto-generati da unique: true
users.email_1                         // unique
users.username_1                      // unique
````

#### Note operative

- **Password reset**: il token in `passwordResetToken` viene cancellato dopo l'uso (`undefined` set via `$unset`).
- **Email verification**: dopo successful `GET /auth/verify/:token`, `isVerified` viene messo `true` e `verificationToken` cancellato.
- **Sprint 2 todo**: aggiungere `publicKey: String` (Ed25519 base64) per firma SOS payload (US-19).

---

### 1.2 Collezione `hikesessions`

**Modello**: `backend/src/models/hikeSession.js`
**Collection MongoDB**: `hikesessions`

#### Schema

```javascript
{
  _id: ObjectId,
  creatorId: ObjectId,                // ref User, required
  inviteCode: String,                 // unique, uppercase, required (formato "TSM-XXXX" 4 hex)

  routeDetails: {
    name: String,                     // required (es. "Catinaccio – Domenica")
    startPoint: {                     // GeoJSON Point — popolato solo se GPX importato
      type: { type: String, enum: ["Point"] },
      coordinates: [Number],          // [lng, lat]
    },
    endPoint: {                       // GeoJSON Point — popolato solo se GPX importato
      type: { type: String, enum: ["Point"] },
      coordinates: [Number],
    },
    difficultyLevel: String,          // enum ["T", "E", "EE", "EEA"], default "E"
    elevationGain: Number,            // alias storico, non più usato (usa gpxStats.elevationGainM)
  },

  // Metadati pianificazione
  meetingDate: String,                // ⚠️ Tipo String per ora (M2 — migrazione Sprint 2)
  meetingTime: String,                // "HH:mm"
  meetingLocation: String,            // testo libero (es. "Parcheggio Vajolet")
  maxParticipants: Number,
  minExperienceLevel: String,         // enum ["T", "E", "EE", "EEA"]

  // GPX import
  gpxFileName: String,
  gpxStats: {
    distanceKm: Number,
    elevationGainM: Number,           // dopo smoothing MA(5) + valley-peak threshold 10m
    trackPoints: Number,              // numero totale punti GPX raw
    elevationProfile: [Number],       // campionamento 50 punti per chart UI
    estimatedPoints: Number,          // K × D_eq, model CAI
  },

  participants: [
    {
      userId: ObjectId,               // ref User, required
      role: String,                   // enum ["hiker", "groupLeader"], default "hiker"
      joinedAt: Date,                 // default Date.now
    }
  ],

  status: String,                     // enum ["PLANNED", "ACTIVE", "COMPLETED", "CANCELLED"], default "PLANNED"

  // Failover leadership (D2 §3.2.3) — Sprint 3+
  statoFailover: Boolean,             // default false
  lastHeartbeat: Date,                // default Date.now

  startTime: Date,                    // popolato quando status = ACTIVE
  endTime: Date,                      // popolato quando status = COMPLETED
  createdAt: Date,                    // default Date.now
}
```

#### Indici

```javascript
// Generato da unique: true su inviteCode
hikesessions.inviteCode_1; // unique

// Geospatial 2dsphere SPARSE (post-fix M3)
hikesessions.routeDetails.startPoint_2dsphere;
// sparse: true → documenti senza startPoint non indicizzati
// evita pollution Null Island [0,0]
```

#### Note operative

- **Codice invito** `TSM-XXXX`: generato in `hikeSessionService.generateInviteCode()` con loop retry su collision.
- **Status lifecycle**:
  ```
  PLANNED ──AVVIA──> ACTIVE ──Stop──> COMPLETED
              ↓
          CANCELLED (futuro)
  ```
- **OCL constraint** (D2 §4.2): un utente può avere al massimo 1 sessione `status: "ACTIVE"`. Implementato in `checkUserAlreadyInActiveSession()`.
- **Sprint 2 todo (M2)**: migrare `meetingDate` da `String` a `Date` con script di backfill, per ordinamento `sort({ meetingDate: 1 })` corretto.

---

### 1.3 Collezione `locations`

**Modello**: `backend/src/models/location.js`
**Collection MongoDB**: `locations`

> Aggiunta in Sprint 1 per il meteo (TINIA cache).

#### Schema

```javascript
{
  _id: ObjectId,
  externalId: String,                 // UUID v4 da TINIA API (unique)
  type: String,                       // enum ["town", "poi"]
  name: String,                       // es. "Bolzano", "Rifugio Pian dell'Orso"
  elevation: Number,                  // metri s.l.m.

  location: {                         // GeoJSON Point — required
    type: { type: String, enum: ["Point"], default: "Point" },
    coordinates: [Number],            // [lng, lat]
  },

  regionId: String,                   // es. "IT-BZ" (per filtri futuri)

  // Solo per type = "poi":
  parentTownId: ObjectId,             // ref Location (town di riferimento)

  // Cache forecasts:
  forecasts: {
    fetchedAt: Date,                  // timestamp ultimo refresh
    slots3h: [
      {
        validFrom: Date,
        validTo: Date,
        temperature: Number,
        rainProbability: Number,      // 0-100
        rainFall: Number,             // mm
        windSpeed: Number,            // km/h
        windDirection: Number,        // gradi 0-360
        skyCondition: String,         // codice TINIA 1-13 o A-G
      }
    ],
    slots24h: [                       // stessa struttura, con temperatureMin/Max
      {
        validFrom: Date,
        validTo: Date,
        temperatureMin: Number,
        temperatureMax: Number,
        // ... altri campi
      }
    ],
  },

  createdAt: Date,
  updatedAt: Date,
}
```

#### Indici

```javascript
locations.externalId_1; // unique
locations.location_2dsphere; // GeoJSON Point — query "nearby"
locations.type_1; // filtri type=town
```

#### Note operative

- **Seeding**: `POST /weather/seed` (admin only post-fix C2) chiama TINIA, scarica tutte towns + POI Trentino-Alto Adige, inserisce/aggiorna.
- **Cache 1h**: `getLocationForecast()` ritorna i forecast cached se `fetchedAt > now - 1h`, altrimenti chiama TINIA e aggiorna.
- **POI risoluzione town**: i POI delegano la query forecast alla town di riferimento (`parentTownId`).

---

### 1.4 Collezione `activities`

**Modello**: `backend/src/models/activity.js`
**Collection MongoDB**: `activities`

> Aggiunta in Sprint 2 per le attività "libere" registrate senza una sessione di gruppo.
> Differenze rispetto a `hikesessions`: owner singolo, no `inviteCode`/`participants`, lifecycle assente (sempre completa al create), nessuna pianificazione GPX preventiva.

#### Schema

```javascript
{
  _id: ObjectId,
  userId: ObjectId,                   // ref User, required, indexed
  name: String,                       // max 120 char, required
  activityType: String,               // enum ["hiking", "trail", "skitouring", "trekking"], default "hiking"
  difficultyLevel: String,            // enum ["T", "E", "EE", "EEA"], opzionale

  startTimeMs: Number,                // epoch ms (coerente col client mobile)
  endTimeMs: Number,
  completedAt: Date,                  // default Date.now, indexed

  startPoint: {                       // GeoJSON Point, opzionale
    type: { type: String, enum: ["Point"] },
    coordinates: [Number],            // [lon, lat]
  },
  endPoint: { /* idem */ },

  actualStats: {                      // sempre presenti (no fase pianificata)
    movingSeconds: Number,            // required
    totalSeconds: Number,             // required
    distanceMeters: Number,           // required
    elevationGainM: Number,           // required
    finalPoints: Number,
    estimatedCalories: Number,
    currentAltitudeM: Number,
  },

  elevationProfile: [Number],         // max 200 punti campionati, metri assoluti
}
```

#### Indici

```javascript
activities.userId_1_completedAt_ - 1; // query "Le mie attività" ordinate per data
activities.startPoint_2dsphere; // sparse, per query geografiche future
```

#### Note operative

- **Ownership** verificato esplicitamente nei service (`getActivityById`, `deleteActivity`) tramite `userId === req.user.userId` → 403 altrimenti.
- **Idempotency**: niente check di duplicate sul backend. Il mobile genera un UUID locale, il backend assegna `_id` Mongo proprio; il client traccia `remoteId` per cross-device delete.
- **Lifecycle**: niente status. Una attività esiste o non esiste (DELETE hard).
- **Integrazione con feed Social** (Sprint 2 piano): aggiungerà `sharedAt: Date?` + `likes[]` + denormalizzato `commentsCount` — vedi `docs/sprint2_social.md`.

---

## 2. Mobile — Room Database

Database: `TsmDatabase` (file `tsm.db`), **versione 4** (bump Sprint 2: campi `retry_count`, `last_retry_at_ms`, `remote_id` aggiunti a `completed_activities` per il SyncManager con backoff incrementale).

### 3. Collezione `emergencies`

**Modello**: `backend/src/models/emergency.js`  
**Collection MongoDB**: `emergencies`  
**Branch**: `SOS` (US-19 MVP)

Segnalazioni SOS legate a una `hikesessions` in stato `ACTIVE`. Il payload sensibile viaggia su HTTPS; il beacon BLE espone solo `beaconInstanceId` (vedi `docs/sos_feature.md`).

#### Schema

```javascript
{
  _id: ObjectId,
  sessionId: ObjectId,              // ref HikeSession, required, indexed
  senderUserId: ObjectId,           // ref User, required, indexed
  emergencyType: String,          // INJURY | LOST | AVALANCHE | WEATHER | EQUIPMENT | OTHER
  coordinates: {                    // GeoJSON Point — snapshot all'invio, non aggiornato
    type: "Point",
    coordinates: [Number],          // [longitude, latitude]
  },
  profileSnapshot: {
    displayName: String,            // required (username al momento SOS)
    personalInfo: { sex, birthDate, heightCm, weightKg },
    experience: { caiLevel, baselineFitness, weeklyTrainingFreq },
  },
  status: String,                   // ACTIVE | SHARED_WITH_GROUP | DISMISSED | CANCELLED_BY_SENDER
  beaconInstanceId: String,         // 12 hex, required
  beaconActive: Boolean,            // default true — false se mittente senza beacon BLE
  idempotencyKey: String,           // UUID v4, unique
  signature: String,                // null — riservato Ed25519 (Sprint successivo)
  cancelReason: String,             // MISTAKE | RESOLVED_SELF (solo cancel)
  leaderAckAt: Date,
  sharedAt: Date,
  dismissedAt: Date,
  dismissedBy: ObjectId,
  cancelledAt: Date,
  cancelledBy: ObjectId,
  createdAt: Date,
}
```

#### Indici

```javascript
emergencies.sessionId_1_status_1_createdAt_ - 1; // lista SOS per sessione
emergencies.idempotencyKey_1; // unique — idempotenza POST
```

#### Note operative

- **Visibilità**: capogruppo vede `ACTIVE` + `SHARED_WITH_GROUP`; partecipante vede `SHARED_WITH_GROUP` e proprie `ACTIVE`.
- **Idempotenza**: stesso `idempotencyKey` + stesso `senderUserId` → `200` con documento esistente.
- **Nessun TTL** su `idempotencyKey` nel modello attuale (la tabella proposta sotto con TTL 30g non è ancora applicata).

---

## 2. Mobile — Room Database

Database: `TsmDatabase` (file `tsm.db`), **versione 5** (coda `pending_emergencies` per SOS offline). Versione 4: campi sync su `completed_activities`.
**File**: `mobile/.../data/local/db/TsmDatabase.kt`

### 2.1 Entità `CachedUserProfileEntity`

```kotlin
@Entity(tableName = "cached_user_profile")
data class CachedUserProfileEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val email: String,
    val role: String,                 // "groupLeader", "rifugio", "admin"
    val cachedAt: Long,               // System.currentTimeMillis()
)
```

**Uso**: cache del profilo per `ProfileScreen` quando offline. Lazy refresh on app open via `ProfileViewModel.refreshFromApi()`.

**DAO**: `ProfileDao` con `upsert()`, `getByUserId(id)`, `clearAll()`.

---

### 2.2 Entità `CompletedActivityEntity`

```kotlin
@Entity(tableName = "completed_activities")
data class CompletedActivityEntity(
    @PrimaryKey val id: String,        // UUID v4 generato lato client
    val sessionId: String?,            // null se attività libera (no session)
    val name: String,                  // editabile dall'utente
    val activityType: String,          // "hiking" per Sprint 1
    val startTimeMs: Long,
    val endTimeMs: Long,
    val movingSeconds: Long,
    val totalSeconds: Long,
    val distanceMeters: Double,
    val elevationGainMeters: Int,
    val currentAltitudeMeters: Int?,   // ultima quota registrata
    val difficultyLevel: String?,      // T/E/EE/EEA (opzionale)
    val trackLatLng: String,           // JSON array [[lat, lon, alt], ...] max 200 punti
    val estimatedCalories: Int,
    val points: Int,                   // HikeEstimation.finalPoints()
    val isSynced: Boolean,             // false fino al sync backend (Sprint 2 US-21)
    val completedAt: Long,             // timestamp salvataggio
)
```

**Uso**: storia attività dell'utente, visibile in `HomeScreen → "Le Mie Attività"` tab.

**DAO**: `CompletedActivityDao` con `upsert()`, `getAll(): Flow<List<...>>`, `getById(id)`, `getStatsForYear(year)`.

**Sprint 2 todo (US-21)**: aggiungere `TelemetryEntity` (batch GPS) per sync offline → backend.

---

### 2.3 Entità `PendingEmergencyEntity`

```kotlin
@Entity(tableName = "pending_emergencies")
data class PendingEmergencyEntity(
    @PrimaryKey val idempotencyKey: String,
    val sessionId: String,
    val emergencyType: String,
    val longitude: Double,
    val latitude: Double,
    val beaconInstanceId: String,
    val createdAtMs: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,
)
```

**Uso**: `POST /emergencies` in coda se offline; flush via `EmergencyUploadWorker` (WorkManager) e retry in `RegistraViewModel`.

**DAO**: `PendingEmergencyDao` — `upsert`, `getAll`, `deleteByKey`, `count`.

---

### 2.4 Migration strategy

Attualmente Room è configurato con `fallbackToDestructiveMigration()` in `TsmDatabase.kt` — accettabile in dev, **da rimuovere prima del Play Store** (Sprint 3+).

Quando si arriva al primo Play Store deploy:

1. Migration esplicita per ogni schema change.
2. Test su device con dati reali.

---

## 3. Relazioni e flusso dati

### 3.1 Diagramma ER (semplificato)

```
┌────────────────┐
│     User       │
│ (users)        │
│                │
│ - email        │
│ - role         │
│ - rifugioDet.  │◄──────┐
│ - sessionRoles │       │
└────────────────┘       │
        │                │
        │ creatorId      │ participants.userId
        ▼                │
┌────────────────┐       │
│  HikeSession   │       │
│ (hikesessions) │───────┘
│                │
│ - inviteCode   │
│ - routeDetails │ ─────────► (GeoJSON 2dsphere)
│ - participants │
│ - gpxStats     │
│ - status       │
└────────────────┘
        │
        │ (logical link al meteo via coords)
        ▼
┌────────────────┐
│   Location     │
│ (locations)    │
│                │
│ - externalId   │
│ - location     │ ─────────► (GeoJSON 2dsphere)
│ - forecasts    │
└────────────────┘
```

### 3.2 Flow scrittura tipica (creazione sessione)

```
Mobile: SessionPlanViewModel.createSession()
   ↓
[Mobile] Parse GPX (smoothing + valley-peak) → CreateSessionRequest
   ↓
HTTPS POST /api/v1/sessions  + Bearer JWT
   ↓
[Backend] hikeSessionService.createSession(creatorId, routeDetails, sessionMeta)
   ├─ checkUserAlreadyInActiveSession(creatorId)
   ├─ generateInviteCode() → loop fino a unique
   ├─ new HikeSession({...}).save()
   └─ User.findByIdAndUpdate({ $push: { sessionRoles: { groupId, role, createdBy } } })
   ↓
[MongoDB] hikesessions.insertOne()
[MongoDB] users.updateOne({_id: creatorId}, { $push: { sessionRoles: {...} } })
   ↓
Response 201 Created { _id, inviteCode, ... }
   ↓
Mobile: aggiorna uiState con codice invito
```

### 3.3 Flow lettura tipica (dettaglio sessione)

```
Mobile: SessionDetailViewModel.loadSession(id)
   ↓
HTTPS GET /api/v1/sessions/:id  + Bearer JWT
   ↓
[Backend] hikeSessionService.getSessionById(id)
   └─ HikeSession.findById(id)
        .populate("creatorId", "username email")
        .populate("participants.userId", "username email")
   ↓
[MongoDB] hikesessions.findOne({_id}) + 2 lookup (creator + participants)
   ↓
Response 200 OK SessionResponse (fully populated)
   ↓
Mobile: parsing Gson → SessionResponse → uiState
   ↓
Trigger automatico: SessionDetailViewModel.loadMeteo(session)
   ├─ GET /weather/locations/nearby?lon=&lat= → town più vicina
   └─ GET /weather/forecast/:externalId → forecast 3h + 24h
```

---

## 4. Sicurezza dati

### 4.1 Encrypted at rest

| Cosa                 | Dove                                                          | Come                                                     |
| -------------------- | ------------------------------------------------------------- | -------------------------------------------------------- |
| Password             | MongoDB `users.passwordHash`                                  | bcrypt cost 10                                           |
| Reset/verify tokens  | MongoDB `users.passwordResetToken`, `users.verificationToken` | Random 32-byte hex (non hashed — token monouso)          |
| JWT mobile           | EncryptedSharedPreferences                                    | AES-256 con master key in Android Keystore               |
| GPX, sessioni, meteo | MongoDB plain                                                 | (Da valutare encryption at rest MongoDB Atlas Sprint 4+) |
| Telemetria Room      | Plain SQLite                                                  | (Da valutare SQLCipher Sprint 3+)                        |

### 4.2 Encrypted in transit

- Sprint 1: HTTP plain (dev). Backend mobile-emulator via `10.0.2.2`.
- **Production todo**: HTTPS reverse proxy nginx con certificato Let's Encrypt.

---

## 5. Indici e performance

### 5.1 Indici attivi

| Collezione     | Indice                             | Tipo            | Uso                            |
| -------------- | ---------------------------------- | --------------- | ------------------------------ |
| `users`        | `email_1`                          | unique          | login, register check          |
| `users`        | `username_1`                       | unique          | register                       |
| `hikesessions` | `inviteCode_1`                     | unique          | join sessione                  |
| `hikesessions` | `routeDetails.startPoint_2dsphere` | 2dsphere sparse | future query "sessioni vicine" |
| `locations`    | `externalId_1`                     | unique          | upsert seed                    |
| `locations`    | `location_2dsphere`                | 2dsphere        | nearby query meteo             |
| `locations`    | `type_1`                           | btree           | filtri type=town/poi           |

### 5.2 Indici proposti Sprint 2+

| Collezione          | Indice                                | Motivo                                              |
| ------------------- | ------------------------------------- | --------------------------------------------------- |
| `hikesessions`      | `creatorId_1`                         | velocizza `getSessionsByUser`                       |
| `hikesessions`      | `participants.userId_1`               | idem                                                |
| `hikesessions`      | `status_1 + meetingDate_1` (compound) | filtro "sessioni future ATTIVE" — dopo migration M2 |
| `users`             | `role_1`                              | dashboard admin                                     |
| nuovo `emergencies` | `userId + createdAt_-1` (compound)    | history SOS                                         |
| nuovo `emergencies` | `idempotencyKey_1` (unique, TTL 30g)  | idempotenza                                         |

### 5.3 Query pattern critici

| Query                                       | Indici usati                                 | Complessità                       |
| ------------------------------------------- | -------------------------------------------- | --------------------------------- |
| Login (email lookup)                        | `email_1` unique                             | O(log n)                          |
| Join sessione (inviteCode)                  | `inviteCode_1` unique                        | O(log n)                          |
| Get my sessions (creatorId OR participants) | (collection scan attuale — proposta indice)  | O(n) Sprint 1 → O(log n) Sprint 2 |
| Meteo nearby                                | `location_2dsphere`                          | O(log n)                          |
| Stats annuali (filter status COMPLETED)     | (collection scan — Sprint 2 indice compound) | O(n)                              |

---

_Database schema doc — Sprint 1 chiuso 17/05/2026. Aggiornare ad ogni schema change con script migration documentato._
