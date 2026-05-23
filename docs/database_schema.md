# Database Schema — Trento Smart Mountain

> Documentazione delle collezioni MongoDB e dello schema Room (locale Android).
>
> **Ultima revisione**: 17/05/2026 — Fine Sprint 1.
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

Nessun campo extra (Sprint 1). Estensioni future:
- `saldoSc: Number` — saldo Social Credits
- `badges: [String]` — gamification
- `livelloEsperienza: String` — T/E/EE/EEA

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
  localField: "_id",
  foreignField: "participants.userId"
});
```

#### Indici

```javascript
// Auto-generati da unique: true
users.email_1                         // unique
users.username_1                      // unique
```

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
hikesessions.inviteCode_1             // unique

// Geospatial 2dsphere SPARSE (post-fix M3)
hikesessions.routeDetails.startPoint_2dsphere
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
locations.externalId_1                // unique
locations.location_2dsphere           // GeoJSON Point — query "nearby"
locations.type_1                      // filtri type=town
```

#### Note operative

- **Seeding**: `POST /weather/seed` (admin only post-fix C2) chiama TINIA, scarica tutte towns + POI Trentino-Alto Adige, inserisce/aggiorna.
- **Cache 1h**: `getLocationForecast()` ritorna i forecast cached se `fetchedAt > now - 1h`, altrimenti chiama TINIA e aggiorna.
- **POI risoluzione town**: i POI delegano la query forecast alla town di riferimento (`parentTownId`).

---

## 2. Mobile — Room Database

Database: `TsmDatabase` (file `tsm.db`), versione 3.
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

### 2.3 Migration strategy

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

| Cosa | Dove | Come |
|------|------|------|
| Password | MongoDB `users.passwordHash` | bcrypt cost 10 |
| Reset/verify tokens | MongoDB `users.passwordResetToken`, `users.verificationToken` | Random 32-byte hex (non hashed — token monouso) |
| JWT mobile | EncryptedSharedPreferences | AES-256 con master key in Android Keystore |
| GPX, sessioni, meteo | MongoDB plain | (Da valutare encryption at rest MongoDB Atlas Sprint 4+) |
| Telemetria Room | Plain SQLite | (Da valutare SQLCipher Sprint 3+) |

### 4.2 Encrypted in transit

- Sprint 1: HTTP plain (dev). Backend mobile-emulator via `10.0.2.2`.
- **Production todo**: HTTPS reverse proxy nginx con certificato Let's Encrypt.

---

## 5. Indici e performance

### 5.1 Indici attivi

| Collezione | Indice | Tipo | Uso |
|------------|--------|------|-----|
| `users` | `email_1` | unique | login, register check |
| `users` | `username_1` | unique | register |
| `hikesessions` | `inviteCode_1` | unique | join sessione |
| `hikesessions` | `routeDetails.startPoint_2dsphere` | 2dsphere sparse | future query "sessioni vicine" |
| `locations` | `externalId_1` | unique | upsert seed |
| `locations` | `location_2dsphere` | 2dsphere | nearby query meteo |
| `locations` | `type_1` | btree | filtri type=town/poi |

### 5.2 Indici proposti Sprint 2+

| Collezione | Indice | Motivo |
|------------|--------|--------|
| `hikesessions` | `creatorId_1` | velocizza `getSessionsByUser` |
| `hikesessions` | `participants.userId_1` | idem |
| `hikesessions` | `status_1 + meetingDate_1` (compound) | filtro "sessioni future ATTIVE" — dopo migration M2 |
| `users` | `role_1` | dashboard admin |
| nuovo `emergencies` | `userId + createdAt_-1` (compound) | history SOS |
| nuovo `emergencies` | `idempotencyKey_1` (unique, TTL 30g) | idempotenza |

### 5.3 Query pattern critici

| Query | Indici usati | Complessità |
|-------|--------------|-------------|
| Login (email lookup) | `email_1` unique | O(log n) |
| Join sessione (inviteCode) | `inviteCode_1` unique | O(log n) |
| Get my sessions (creatorId OR participants) | (collection scan attuale — proposta indice) | O(n) Sprint 1 → O(log n) Sprint 2 |
| Meteo nearby | `location_2dsphere` | O(log n) |
| Stats annuali (filter status COMPLETED) | (collection scan — Sprint 2 indice compound) | O(n) |

---

*Database schema doc — Sprint 1 chiuso 17/05/2026. Aggiornare ad ogni schema change con script migration documentato.*
