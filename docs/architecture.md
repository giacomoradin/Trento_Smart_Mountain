# Architettura — Trento Smart Mountain

> Documento di riferimento per gli **standard architetturali** del progetto. Descrive i pattern adottati, le decisioni progettuali e le convenzioni di codice condivise dal team.
>
> **Ultima revisione**: 17/05/2026 — Fine Sprint 1.
> **Riferimenti**: D1 §3, D2 §3-4, `TSM_PROJECT_STATE.md` §10.3 M5.

---

## 1. Visione architetturale

Trento Smart Mountain è un **ecosistema distribuito** composto da:

```
┌────────────────────┐   HTTPS/JWT    ┌──────────────────┐   MQTT    ┌──────────────┐
│   Android App      │ <─────────────>│   Backend Node   │ <────────>│  IoT Gateway │
│   (Kotlin/Compose) │                │  (Express+Mongo) │           │   (Rifugi)   │
└────────────────────┘                └──────────────────┘           └──────────────┘
         │                                     │
         │ Offline-First                       │ TINIA API (meteo)
         │ (Room + EncryptedSharedPreferences) │ SMTP Gmail (auth emails)
         ↓                                     ↓
   ┌─────────┐                          ┌──────────────┐
   │ Local   │                          │  Servizi     │
   │ Cache   │                          │  Esterni     │
   └─────────┘                          └──────────────┘
```

### Macro-principi (D2 §3.1)

1. **Offline-First**: l'app deve funzionare in modalità degradata in assenza di rete. Tutti i flussi critici (auth, tracking, salvataggio attività) sono progettati per persistere localmente prima del sync.
2. **Store-and-Forward**: telemetria GPS e eventi gamification accumulati offline, propagati in batch al ritorno della rete.
3. **Edge Computing**: gateway IoT nei rifugi eseguono aggregazione locale (Edge AI conteggio persone) per minimizzare il traffico al backend.
4. **Eventual Consistency**: il sistema accetta inconsistenze temporanee in cambio di disponibilità (CAP theorem → CP backend, AP mobile).
5. **Sicurezza by Design**: JWT firmati HS256, payload SOS firmati ECC Ed25519, AES-256 per BLE Mesh, EncryptedSharedPreferences per JWT mobile.

---

## 2. Architettura Mobile (Android)

### 2.1 Pattern MVVM (Model-View-ViewModel)

```
┌──────────────────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)                                  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Composable Screens (es. SessionDetailScreen)            │  │
│  │ - osservano StateFlow dei ViewModel                      │  │
│  │ - emettono eventi via callback ai ViewModel             │  │
│  │ - mai chiamano Retrofit/Room direttamente               │  │
│  └────────────────────────────────────────────────────────┘  │
│                       ↑↓ (collectAsStateWithLifecycle)        │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ ViewModel Layer (StateFlow + viewModelScope)            │  │
│  │ - SessionDetailViewModel, RegistraViewModel, ...        │  │
│  │ - gestiscono UI state                                    │  │
│  │ - delegano IO ai Repository                             │  │
│  └────────────────────────────────────────────────────────┘  │
│                       ↑↓ (suspend fun)                       │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Repository Layer (single source of truth)               │  │
│  │ - AuthRepositoryImpl, SessionRepositoryImpl (in arrivo) │  │
│  │ - decidono fonte: cache Room vs API Retrofit            │  │
│  └────────────────────────────────────────────────────────┘  │
│                       ↑↓                                      │
│  ┌──────────────┬───────────────────────┬───────────────┐    │
│  │ Data Local   │ Data Remote           │ Data Sensors  │    │
│  │ - Room DB    │ - Retrofit + OkHttp   │ - Fused Loc   │    │
│  │ - DAOs       │ - AuthInterceptor JWT │ - Accelero    │    │
│  │ - TokenStore │ - DTOs                │ - BLE (futuro)│    │
│  └──────────────┴───────────────────────┴───────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 Regola fondamentale (Definition of Done architetturale)

> **Una Composable NON DEVE mai chiamare `TsmApiClient.service()` o un DAO Room direttamente.**
> Tutto passa per il ViewModel. Tutto IO passa per il Repository.

#### Stato Sprint 1 — Debito tecnico M5

| ViewModel | Usa Repository? | Note |
|-----------|----------------|------|
| `LoginViewModel` | ✅ `AuthRepositoryImpl` | Pattern di riferimento |
| `RegisterViewModel` | ✅ `AuthRepositoryImpl` | OK |
| `ForgotPasswordViewModel` | ✅ `AuthRepositoryImpl` | OK |
| `ProfileViewModel` | 🟠 Parziale | Usa Room DAO direttamente + API call |
| `SessionPlanViewModel` | ❌ `TsmApiClient.service()` diretto | **Refactor Sprint 2 (US-fix-M5)** |
| `SessionJoinViewModel` | ❌ `TsmApiClient.service()` diretto | **Refactor Sprint 2** |
| `SessionDetailViewModel` | ❌ `TsmApiClient.service()` diretto | **Refactor Sprint 2** |
| `RegistraViewModel` | ❌ `TsmApiClient.service()` diretto | **Refactor Sprint 2** |
| `ActivityListViewModel` | ❌ Room Flow diretto | **Refactor Sprint 2** |

**Decisione team**: a partire da Sprint 2, **ogni nuovo ViewModel deve passare per un Repository**. Il refactor dei 5 ViewModel sopra è splittato in 1 PR / ViewModel per limitare il rischio di regressioni.

### 2.3 Layer Repository — design

```kotlin
// Interfaccia Repository (in repository/SessionRepository.kt)
interface SessionRepository {
    suspend fun getMySessions(): Result<List<SessionResponse>>
    suspend fun getSessionById(id: String): Result<SessionResponse>
    suspend fun createSession(req: CreateSessionRequest): Result<SessionResponse>
    suspend fun joinSession(inviteCode: String): Result<SessionResponse>
    suspend fun leaveSession(id: String): Result<Unit>
    suspend fun deleteSession(id: String): Result<Unit>
    suspend fun updateStatus(id: String, status: String): Result<Unit>
    suspend fun updateDetails(id: String, req: UpdateSessionRequest): Result<Unit>
}

// Implementazione (in repository/SessionRepositoryImpl.kt)
class SessionRepositoryImpl(
    private val api: TsmApiService = TsmApiClient.service(),
) : SessionRepository {

    override suspend fun joinSession(inviteCode: String): Result<SessionResponse> =
        runCatching {
            val resp = api.joinSession(JoinSessionRequest(inviteCode))
            if (resp.isSuccessful) resp.body()!! else throw HttpException(resp)
        }
}
```

**Convenzione**:
- `Result<T>` invece di throwing per gestione errori funzionale nei ViewModel.
- DI manuale via constructor default param (no Hilt per ora — overhead).
- Mock Retrofit con `MockWebServer` per unit test.

### 2.4 Persistenza Mobile

| Cosa | Dove | TTL / Refresh |
|------|------|---------------|
| JWT | `EncryptedSharedPreferences` (`TokenStorage`) | Scadenza claim JWT (lato server) |
| Profilo utente (cache) | Room `CachedUserProfileEntity` | Lazy refresh on app open |
| Attività completate | Room `CompletedActivityEntity` | Permanente fino a sync esplicito (Sprint 2 US-21) |
| Cache meteo | Server-side (no client cache yet) | 1h MongoDB |
| Cache mappa OSM tiles | OSMdroid built-in disk cache | Default OSMdroid |
| Sessioni utente | No cache (sempre API call) | (Sprint 2: cache Room + ETag) |

### 2.5 Background tracking

```
RegistraViewModel.startTracking()
       ↓
       ├─ trackingEngine.start()
       ├─ ForegroundTrackingService.start(app)        ← Notifica persistente
       │       ↓
       │       └─ FusedLocationProviderClient
       │              .requestLocationUpdates(
       │                  interval = 5s,
       │                  priority = HIGH_ACCURACY,
       │                  fastestInterval = 2s
       │              )
       │       ↓
       │       └─ TrackingLocationBus.emit(snapshot)  ← Bus locale
       │
       ├─ stationaryDetector.start()                  ← Accelerometro
       │       ↓
       │       └─ auto-pause se speed<0.5m/s + still 45s
       │
       └─ timerJob (1s tick) → uiState.elapsedSeconds
```

**Permessi richiesti (post-fix C3)**:
- `INTERNET`, `ACCESS_NETWORK_STATE`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION` ← richiesto Android 10+ per schermo spento
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`
- `POST_NOTIFICATIONS` (Android 13+)
- `WAKE_LOCK` ← previene Doze Mode kill

---

## 3. Architettura Backend (Node.js + MongoDB)

### 3.1 Layered architecture

```
┌────────────────────────────────────────────────────┐
│  Routes Layer (Express Router)                     │
│  - authRoutes, userRoutes, hikeSessionRoutes,      │
│    weatherRoutes                                    │
│  - validazione input minima + error mapping        │
│  - delegano logica ai Service                       │
└────────────────────────────────────────────────────┘
                       ↑↓
┌────────────────────────────────────────────────────┐
│  Service Layer (logica business)                   │
│  - authService, userService, hikeSessionService,   │
│    weatherService, emailService                     │
│  - non conoscono req/res; throw error codes        │
└────────────────────────────────────────────────────┘
                       ↑↓
┌────────────────────────────────────────────────────┐
│  Model Layer (Mongoose schemas)                    │
│  - User, HikeSession, Location                      │
│  - schema validation + index + virtual populate     │
└────────────────────────────────────────────────────┘
                       ↑↓
┌────────────────────────────────────────────────────┐
│  Data Layer (MongoDB)                              │
│  - 2dsphere geospatial indexes                      │
│  - TTL indexes (futuri: idempotencyKey 30g)        │
└────────────────────────────────────────────────────┘
```

### 3.2 Middleware chain

```
Request
   ↓
[cors]                          ← lib cors
   ↓
[express.json]                  ← parsing JSON body
[express.urlencoded]            ← parsing form HTML (reset password)
   ↓
Route mounting:
   /users      → userRoutes
   /auth       → authRoutes
   /api/v1/sessions → authenticate → hikeSessionRoutes
   /weather    → weatherRoutes (authenticate selettivo)
   ↓
[notFoundHandler]               ← 404 generico
[globalErrorHandler]            ← 500 + log
   ↓
Response
```

### 3.3 Convenzioni servizi backend

**Throw codes invece di status HTTP nel service:**

```javascript
// Service: throw stringa codice
export async function updateSessionStatus(sessionId, userId, status) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (session.creatorId.toString() !== userId) throw new Error("FORBIDDEN");
  // ...
}

// Route: mappa codice → status HTTP
router.patch("/:id/status", async (req, res) => {
  try {
    await updateSessionStatus(req.params.id, req.user.userId, req.body.status);
    res.status(200).json({ message: "Status aggiornato" });
  } catch (err) {
    if (err.message === "SESSION_NOT_FOUND") return res.status(404).json({ error: "..." });
    if (err.message === "FORBIDDEN") return res.status(403).json({ error: "..." });
    res.status(500).json({ error: "Errore generico" });
  }
});
```

**Populate symmetric**: tutti i `findById`/`find`/`save+return` di HikeSession devono popolare entrambi i campi ref per evitare crash Gson client-side:

```javascript
return session.populate([
  { path: "creatorId", select: "username email" },
  { path: "participants.userId", select: "username email" },
]);
```

### 3.4 Sicurezza backend

| Livello | Dove | Come |
|---------|------|------|
| Trasporto | (Produzione futura) | HTTPS via reverse proxy nginx |
| Auth | `middleware/authMiddleware.js` | JWT Bearer + verify HS256 con `JWT_SECRET` |
| Authz | `middleware/authorizationMiddleware.js` | `requireRoles("admin", "rifugio", ...)` |
| Password | `userService` | bcrypt hash con cost factor default (10) |
| Email tokens | `authService` | Random 32-byte hex per verify/reset, TTL 1h |
| SOS payload | (Sprint 2 US-19) | Ed25519 signature verify |

---

## 4. Architettura comunicazione Mobile ↔ Backend

### 4.1 API contract: status codes attesi

| Operazione | Success | Client error | Server error |
|------------|---------|--------------|--------------|
| Create resource (POST) | `201 Created` | `400 Bad Request`, `409 Conflict` | `500` |
| Read (GET) | `200 OK` | `404 Not Found` | `500` |
| Update (PATCH/PUT) | `200 OK` o `204 No Content` | `400`, `403`, `404` | `500` |
| Delete | `200 OK` o `204 No Content` | `403`, `404` | `500` |
| Auth required | (sopra) | `401 Unauthorized` | — |
| Authz fail | (sopra) | `403 Forbidden` | — |

### 4.2 Gson-safe DTO design

**Lezione appresa Sprint 1**: per evitare `IllegalStateException: Expected BEGIN_OBJECT but was STRING` quando il backend ritorna populate parziale.

#### Regola design

**A) DTO per response GET + POST**: campi ref sempre **populated** (oggetto User completo), DTO Kotlin con type `UserDto`.

**B) DTO per response PATCH/DELETE**: usa `Response<ApiMessageBody>` invece di `Response<SessionResponse>`. Il client legge solo `{ message: string }` e ricarica la risorsa con un GET separato.

```kotlin
// Mobile
data class ApiMessageBody(val message: String)

interface TsmApiService {
    @PATCH("/api/v1/sessions/{id}")
    suspend fun updateSession(
        @Path("id") id: String,
        @Body req: UpdateSessionRequest,
    ): Response<ApiMessageBody>
    // ↑ NON SessionResponse — evita crash Gson se il body ha ObjectId raw
}
```

### 4.3 Auth flow JWT

```
1. POST /auth/login { email, password }
                ↓
   200 OK { token: "Bearer xxx.yyy.zzz", userId, role }
                ↓
2. Mobile: TokenStorage.saveToken(token)   ← EncryptedSharedPreferences
                ↓
3. Ogni request: AuthInterceptor aggiunge header
                  Authorization: Bearer xxx.yyy.zzz
                ↓
4. Backend: authMiddleware verifica firma + decodifica
              req.user = { userId, role, exp, iat }
                ↓
5. Route handler accede a req.user.userId
```

### 4.4 Deep linking (auth email)

```xml
<!-- AndroidManifest.xml -->
<intent-filter>
  <action android:name="android.intent.action.VIEW" />
  <category android:name="android.intent.category.DEFAULT" />
  <category android:name="android.intent.category.BROWSABLE" />
  <data android:scheme="tsm" android:host="auth" />
</intent-filter>
```

```javascript
// Backend authService — verifica email
res.redirect(`tsm://auth/verify/${token}?email=${user.email}`);
```

Il client gestisce il deep link in `MainActivity.onCreate()` parsando l'intent data.

---

## 5. Pattern e idiomi adottati

### 5.1 SessionStartCoordinator — Bus singleton

**Problema**: AVVIA escursione dal `SessionDetailScreen` deve far partire il tracking nella tab `Registra` di `HikerMainScreen`. Coupling diretto = scarsa testabilità + difficile da seguire.

**Soluzione**: bus singleton `SessionStartCoordinator` con `MutableStateFlow<String?>` come canale di comunicazione disaccoppiato.

```kotlin
object SessionStartCoordinator {
    private val _pendingSessionStart = MutableStateFlow<String?>(null)
    val pendingSessionStart: StateFlow<String?> = _pendingSessionStart.asStateFlow()

    fun requestStart(sessionId: String) {
        _pendingSessionStart.value = sessionId
    }

    fun consume() {
        _pendingSessionStart.value = null
    }
}
```

**Pro**: zero coupling fra schermate, facile da testare.
**Contro**: stato globale ⚠️ — solo per eventi rari & idempotenti.

### 5.2 Result<T> per error handling

Repository ritornano `Result<T>` invece di `T` o di lanciare:

```kotlin
viewModelScope.launch {
    repository.joinSession(code)
        .onSuccess { session -> uiState.update { it.copy(session = session) } }
        .onFailure { err -> uiState.update { it.copy(error = err.message) } }
}
```

### 5.3 Sealed classes per stati UI complessi

```kotlin
sealed class LoginResult {
    data class Success(val token: String, val userId: String, val role: String) : LoginResult()
    data class EmailNotVerified(val email: String) : LoginResult()
    data class Failure(val message: String) : LoginResult()
}
```

### 5.4 GPX parser robusto (smoothing + valley-peak)

Parser GPX in `SessionPlanViewModel.parseGpx()`:

1. **Estrazione**: XmlPullParser estrae `(lat, lon, ele)` da tutti i `<trkpt>`.
2. **Interpolazione**: ele null → interpolato linearmente tra vicini.
3. **Smoothing**: Moving Average con window 5 punti per ridurre noise GPS.
4. **Valley-peak**: cumula dislivello solo quando la variazione supera 10m da ultimo "valley" (riduce sovrastima 2-3× da noise).
5. **Sampling**: max 50 punti per `elevationProfile` chart (riduce payload + rendering).
6. **Haversine**: distanza orizzontale.

```
GPX raw:  ~ 2000-5000 trkpt
              ↓ Haversine
distanceM = Σ haversine(p[i], p[i+1])
              ↓ Smoothing MA(5)
ele_smooth[i] = mean(ele[i-2..i+2])
              ↓ Valley-Peak
elevationGain += max(0, peak - valley) se gap > 10m
              ↓ Sampling
elevationProfile = elevationProfile.take(50 punti equidistanti)
              ↓ HikeEstimation
estimatedPoints = round(K × (D + H/100))   // K=10, μ=1.0
```

### 5.5 HikeEstimation — modello CAI custom

In `HikeEstimation.kt`:

| Funzione | Formula | Note |
|----------|---------|------|
| `caiTimeHours(D, H)` | polinomio CAI su pendenza P=(H/D×100)% → min/km × D / 60 | tempo nominale CAI |
| `equivalentDistance(D, H)` | D + H/100 | 100m salita ≡ 1km piano |
| `naismithTimeHours(D, H)` | D/4 + H/300 | regola Naismith (riferimento) |
| `estimatedPoints(D, H, K=10)` | round(K × D_eq) | pianificazione, μ=1.0 |
| `finalPoints(D, H, T_reale, K=10)` | round(K × D_eq × clip(T_nom / T_reale, 0.8, 1.2)) | post-completion, μ adattato |

---

## 6. Naming conventions

### 6.1 Kotlin / Android

| Cosa | Convenzione | Esempio |
|------|-------------|---------|
| Package | lowercase con `it.trentosmartmountain.app.<feature>` | `it.trentosmartmountain.app.viewmodel` |
| Class | PascalCase | `SessionDetailViewModel` |
| Composable | PascalCase nome | `SessionDetailScreen()` |
| ViewModel | suffix `ViewModel` | `LoginViewModel` |
| Repository | suffix `Repository(Impl)` | `AuthRepositoryImpl` |
| DTO | suffix `Request`/`Response` | `CreateSessionRequest` |
| Room Entity | suffix `Entity` | `CompletedActivityEntity` |
| Room DAO | suffix `Dao` | `CompletedActivityDao` |
| String resource | snake_case | `R.string.home_tab_social` |
| Color token | PascalCase con prefix `Tsm` | `TsmPrimary`, `TsmAccent` |

### 6.2 JavaScript / Node

| Cosa | Convenzione | Esempio |
|------|-------------|---------|
| File | camelCase + suffix tipo | `hikeSessionService.js` |
| Function | camelCase | `updateSessionStatus()` |
| Variable | camelCase | `creatorId` |
| Constant module-scope | UPPER_SNAKE | `JWT_EXPIRATION_HOURS` |
| Mongoose model | PascalCase singolare | `HikeSession`, `User` |
| Collection MongoDB | lowercase plurale | `hikesessions`, `users` |
| Route file | suffix `Routes.js` | `hikeSessionRoutes.js` |
| Error code (throw) | UPPER_SNAKE stringa | `throw new Error("SESSION_NOT_FOUND")` |

### 6.3 Git

| Cosa | Convenzione | Esempio |
|------|-------------|---------|
| Branch feature | `<numero-issue>-<slug>` o `<feature>-<area>` | `18-gestione-sessione-escursione`, `API-Meteo-Integration` |
| Branch bugfix | `bugfix/<descrizione>` | `bugfix/sprint1-c1-avvia` |
| Commit | Conventional Commits | `feat: add GPX import`, `fix: prevent silent 403 on AVVIA`, `docs: update D3`, `refactor: extract SessionRepository` |
| PR title | Conventional Commits | identico al commit principale |

---

## 7. Decisioni architetturali (ADR — Architecture Decision Records)

### ADR-001: Compose per la UI mobile

**Status**: Accepted (Sprint 0)
**Context**: Scelta tra View tradizionali (XML+Activity) e Jetpack Compose per la UI Android.
**Decision**: Compose come standard.
**Consequences**:
- ✅ Productivity team alta (UI declarativa)
- ✅ Tooling moderno (Preview, hot reload Live Edit)
- ❌ Curva di apprendimento per chi non l'aveva mai usato
- ❌ Alcuni library legacy (es. OSMdroid AndroidView wrapper) richiedono ponte

### ADR-002: Monolite modulare backend (no microservizi)

**Status**: Accepted (Sprint 0)
**Context**: Architettura backend: monolite vs microservizi.
**Decision**: Monolite modulare in Node.js + Express con cartelle separate per route/service/model.
**Consequences**:
- ✅ Deploy semplice (singolo processo)
- ✅ Team piccolo (3 persone) → coordinamento ridotto
- ❌ Scalabilità orizzontale limitata (mitigato: stateless service + MongoDB scaling)

### ADR-003: MongoDB invece di PostgreSQL

**Status**: Accepted (Sprint 0)
**Context**: DB scelto.
**Decision**: MongoDB (Mongoose).
**Consequences**:
- ✅ Schema flessibile (utile per `rifugioDetails`, `gpxStats` con campi opzionali)
- ✅ Indici geospaziali 2dsphere nativi per query prossimità (RF34)
- ❌ No FK enforced → query join manuali via populate
- ❌ Transactions limitate (ok per Sprint 1, valutare in Sprint 3+)

### ADR-004: JWT in EncryptedSharedPreferences (no Keystore)

**Status**: Accepted (Sprint 1)
**Context**: Storage sicuro del JWT mobile.
**Decision**: `EncryptedSharedPreferences` (libreria androidx.security.crypto).
**Consequences**:
- ✅ Setup semplice
- ✅ Crittografia AES-256 con master key in Keystore
- ❌ Quando l'utente reinstalla l'app perde lo storage (richiede re-login)
- ➡️ Per chiavi ECC SOS (Sprint 2 US-19) si valuterà Android Keystore diretto

### ADR-005: OSMdroid invece di Google Maps

**Status**: Accepted (Sprint 1)
**Context**: Libreria mappa.
**Decision**: OSMdroid (OpenStreetMap).
**Consequences**:
- ✅ Open source, no API key Google
- ✅ Cache tiles offline built-in (RF12)
- ❌ Look&feel meno raffinato di Google Maps
- ❌ Performance ok ma non eccellenti su device low-end

### ADR-006: TINIA / meteo.report come provider meteo

**Status**: Accepted (Sprint 1)
**Context**: API meteo da integrare per RF6.
**Decision**: TINIA (meteo.report) — open data della Provincia Autonoma di Bolzano.
**Consequences**:
- ✅ Gratis, no rate limit aggressivi
- ✅ Copertura completa Trentino-Alto Adige
- ❌ Solo italiano (responses non i18n)
- ❌ Tempi di risposta variabili (3s timeout client-side)

### ADR-007: SessionStartCoordinator come bus singleton

**Status**: Accepted (Sprint 1)
**Context**: Come triggerare il tracking GPS dalla `SessionDetailScreen` evitando coupling con `RegistraScreen`.
**Decision**: Bus singleton con `StateFlow<String?>`.
**Consequences**:
- ✅ Zero coupling tra schermate
- ✅ Facile da estendere (es. notifiche stato tracking)
- ⚠️ Stato globale — limitarne l'uso a eventi rari + idempotenti

### ADR-008: Repository pattern come standard (Sprint 2+)

**Status**: Proposed (Sprint 2 US-fix-M5)
**Context**: Sprint 1 ha violato il pattern in 4 ViewModel per velocità.
**Decision**: A partire da Sprint 2, ogni ViewModel deve passare per un Repository.
**Consequences**:
- ✅ Testabilità ViewModel con `MockWebServer`
- ✅ Single source of truth per cache/offline strategy
- ❌ Boilerplate iniziale (interfaccia + impl)
- ➡️ DI manuale via constructor default param (no Hilt per ora)

---

## 8. Riferimenti tecnici

| Tecnologia | Versione | File config |
|------------|----------|-------------|
| Kotlin | 2.0.21 | `mobile/build.gradle.kts` |
| Compose BOM | 2024.12 | idem |
| Gradle | 8.x | `gradle-wrapper.properties` |
| Node.js | 20.x | `package.json` engines |
| Express | 4.x | `backend/package.json` |
| Mongoose | 8.x | idem |
| MongoDB | 7.x | docker-compose |
| Retrofit | 2.11 | mobile dependencies |
| OkHttp | 4.12 | mobile dependencies |
| Room | 2.6.1 | mobile dependencies |
| OSMdroid | 6.1.20 | mobile dependencies |
| ZXing Core | 3.5.3 | mobile dependencies (QR) |
| sh.calvin.reorderable | 2.4.3 | mobile dependencies |

---

*Architettura documento — Sprint 1 chiuso 17/05/2026. Aggiornare ad ogni nuovo ADR o pattern adottato dal team.*
