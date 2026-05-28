## Realtime tracking sessione — Mobile/Frontend (tab `Registra`)

### Scopo UI
In `Registra`:
- la **traccia** (polyline) visibile è **solo la mia**
- gli altri partecipanti sono visibili **solo come marker**
- marker capogruppo con stile diverso
- marker SOS rosso **solo se visibile** (leader sempre; partecipanti solo se SOS “Condiviso con il gruppo”)
- tap su marker → popup con **nome** e **avatar**
- se l’utente è troppo lontano dalla **polyline pianificata**: viene **sospeso** dal live tracking (smette di caricare la posizione al server e scompare dal feed live).

Polling: **una volta ogni 5 secondi**.

---

## Dove intervenire nel codice
- `RegistraViewModel.kt` (orchestrazione tracking + polling SOS già presente)
- `RegistraScreen.kt` (render mappa e overlay UI)
- `TsmMapView.kt` (OSMdroid wrapper) — va esteso o duplicato per supportare marker extra
- `TsmApiService.kt` + nuovi DTO

---

## Contratto API (assunto)
Si assume che il backend esponga:
- `POST /api/v1/sessions/:id/live-location`
- `GET /api/v1/sessions/:id/live-locations`
- sessione contiene `plannedRoute.polylinePoints` (array lat/lon downsample)

E che `GET /api/v1/sessions/:id/emergencies` continui a funzionare come oggi.

---

## 1) Nuovi DTO + Retrofit
### 1.1 DTO
Creare in `mobile/.../data/remote/dto/`:
- `PostLiveLocationRequest(lat, lon, accuracyM?, timestampMs?)`
- `LiveLocationsResponse(data: List<LiveLocationItemDto>, message: String?)`
  - `LiveLocationItemDto(user: LiveUserDto, location: LiveLocationDto)`
  - `LiveUserDto(id, username, avatarUrl?, role)`
  - `LiveLocationDto(lat, lon, accuracyM?, updatedAt)`

### 1.2 Retrofit
In `TsmApiService.kt`:
- `suspend fun postLiveLocation(@Path("id") sessionId: String, @Body body: PostLiveLocationRequest): Response<ApiMessageBody>`
- `suspend fun getLiveLocations(@Path("id") sessionId: String, @Query("maxAgeSec") maxAgeSec: Int? = 30): Response<LiveLocationsResponse>`

---

## 2) Stato aggiuntivo in `RegistraViewModel`
Aggiungere in `UiState`:
- `val liveLocations: List<LiveLocationItemDto> = emptyList()`
- `val selectedLiveUser: LiveUserDto? = null` (per popup)
- `val showLiveUserPopup: Boolean = false`
- `val isRealtimeSuspended: Boolean = false`
- `val realtimeSuspendReason: String? = null`
- `val plannedPolyline: List<GeoPoint> = emptyList()` (o lat/lon list)

Nota: `GeoPoint` qui è `org.osmdroid.util.GeoPoint`.

---

## 3) Loop realtime (polling 5s)
### 3.1 Start/stop dei job
Nel `init` di `RegistraViewModel` o quando cambia `activeSessionId`:
- se `activeSessionId != null`:
  - avvia `liveFetchJob` (ogni 5s) → `GET live-locations`
  - avvia `liveUploadJob` (ogni 5s) → `POST live-location` **solo se** tracking attivo e non sospeso
  - carica una volta la sessione per ottenere `plannedRoute.polylinePoints` (o estenderla in un repository)
- se `activeSessionId == null`:
  - stop job + reset `liveLocations`

### 3.2 Upload (POST live-location)
Condizioni:
- `trackingStatus != IDLE`
- `activeSessionId != null`
- `userLocation != null`
- `!isRealtimeSuspended`

Se response = `403 LIVE_TRACKING_SUSPENDED`:
- `isRealtimeSuspended = true`
- `realtimeSuspendReason = reason`
- stop `liveUploadJob` (o semplicemente non inviare più)

### 3.3 Fetch (GET live-locations)
Ogni 5s:
- aggiorna `uiState.liveLocations`
- la UI mappa usa questa lista per marker.

---

## 4) Distanza dalla polyline pianificata → sospensione
### 4.1 Caricare la polyline pianificata
All’avvio sessione (quando `activeSessionId` viene impostato):
- `GET /api/v1/sessions/{id}` e leggi `plannedRoute.polylinePoints`
- converti in `List<GeoPoint>`
- downsample se necessario (anche se backend già lo fa)

### 4.2 Calcolo distanza punto→polyline
Implementare utility:
- `distanceMetersPointToPolyline(userPoint, polylinePoints): Double`

Strategia:
- iterare sui segmenti `p[i] -> p[i+1]` e calcolare distanza del punto dal segmento (approssimata in metri).
- Per performance: pre-filtrare con bbox o saltare se polyline è vuota.

### 4.3 Regola sospensione
Parametri (da hardcodare V1, poi settings):
- `SUSPEND_THRESHOLD_M = 800` (esempio)
- `SUSPEND_CONSECUTIVE_SAMPLES = 3` (3×5s = 15s)
- `RESUME_THRESHOLD_M = 500` (isteresi) (opzionale)

Comportamento:
- Se distanza > soglia per N campioni consecutivi:
  - chiamare `POST /api/v1/sessions/:id/live-tracking/suspend` (se autorizzato) **oppure**
  - impostare `isRealtimeSuspended=true` localmente e lasciare che il server rifiuti upload.
  - mostrare banner UI: “Realtime sospeso: troppo lontano dal percorso”

Requisito story: “considerati non partecipanti alla sessione attiva” → implementare come **SUSPENDED** (non appaiono nel feed live).

---

## 5) Marker su mappa (capogruppo, partecipanti, SOS)
### 5.1 Estendere `TsmMapView`
Oggi `TsmMapView` supporta:
- marker posizione utente
- polyline `trackGeoPoints` (la tua)

Serve aggiungere:
- `liveMarkers: List<LiveMarkerSpec>`
  - lat/lon
  - tipo: `LEADER | USER | SOS`
  - label (username)
  - avatarUrl (per popup)

Oppure creare un nuovo composable:
- `TsmRealtimeSessionMapView(...)` che riusa la base OSMdroid (tile + lifecycle) e gestisce overlay marker live.

### 5.2 Determinare chi è in SOS
Fonte: `uiState.incomingEmergencies` già presente e già filtrata per visibilità (leader vs shared).
Mappare:
- `emergency.senderUserId` o `emergency.profileSnapshot` → userId
- se userId è in emergencies visibili → marker tipo SOS.

### 5.3 Click marker → popup user
Implementare callback da MapView:
- onMarkerClick(user) → set `selectedLiveUser` + `showLiveUserPopup=true`

Popup UI:
- avatar (già esiste `AvatarImage` in session detail)
- username
- badge “HOST” se role groupLeader
- indicazione “SOS” se marker tipo SOS

---

## 6) UX/Performance notes
- Polling 5s: evitare richieste se app in background (rispetta lifecycle: stop job in `onCleared` o quando tab non visibile se avete segnale).
- Marker count: se tanti partecipanti, mantenere un overlay marker per userId e aggiornarne solo la posizione (no remove+add completo ad ogni tick).
- Non disegnare polyline pianificata in V1 (non richiesto); serve solo per distanza.

---

## 7) Test plan minimo (mobile)
- Due device → avvio sessione → entrambi vedono marker reciproci in `Registra`.
- Leader marker differenziato.
- SOS:
  - partecipante lancia SOS → leader vede marker rosso; altro partecipante lo vede solo dopo “Condividi con il gruppo”.
- Sospensione:
  - mock location lontana dalla polyline → dopo ~15s banner e stop upload; il device sparisce dal feed live sugli altri.

---

## Definition of Done (Mobile/Frontend)
- **Polling 5s**
  - Upload posizione (POST live-location) ogni ~5s mentre tracking attivo e sessione `ACTIVE`.
  - Fetch posizioni gruppo (GET live-locations) ogni ~5s mentre sessione attiva.
- **Mappa in `Registra`**
  - La polyline mostrata resta **solo** quella dell’utente locale (nessuna traccia degli altri).
  - Gli altri partecipanti compaiono come marker aggiornati con `GET live-locations`.
  - Marker capogruppo differenziato (basato su `role=groupLeader`).
- **SOS**
  - Il capogruppo vede marker SOS per emergenze visibili.
  - I partecipanti vedono marker SOS **solo** se l’emergenza è `SHARED_WITH_GROUP` (già garantito da `incomingEmergencies`).
- **Tap marker**
  - Tap su marker mostra popup con `username` + `avatarUrl` (se disponibile).
- **Sospensione da polyline**
  - Il client carica `plannedRoute.polylinePoints` della sessione.
  - Se distanza dalla polyline supera soglia per \(N\) campioni → sospende realtime:
    - stop upload posizione
    - mostra banner esplicito
    - il server rifiuta eventuali upload successivi con `403 LIVE_TRACKING_SUSPENDED` (gestito senza crash)
- **Stabilità**
  - Nessun leak di job: stop polling quando sessione non attiva / VM cleared.
  - Nessun refresh “a scatti” dei marker (aggiornamento per userId, non ricostruzione completa overlay ad ogni tick).

