## Piano implementazione — Selezione tracciato in “Sessione → Pianifica”

### Contesto e obiettivo
Nella schermata **Sessione → Pianifica** (tab “Pianifica” in `SessionHubScreen`) il capogruppo deve poter scegliere il percorso in **due modalità alternative**:

1) **Import GPX** (già presente) ma con **anteprima del tracciato su mappa** in un **popup/dialog** (senza navigare alla schermata “Registra”).
2) **Scelta da database** (da implementare) tramite **popup/dialog** con mappa che consente:
   - visualizzazione di tutte le **destinazioni** (marker),
   - selezione di una destinazione → visualizzazione dei **sentieri** che la raggiungono + marker dei punti di partenza,
   - selezione di un sentiero → visualizzazione del **tracciato completo** + scheda info + conferma,
   - conferma → ritorno alla schermata “Pianifica” con il tracciato impostato.

Vincolo UX: evitare la navigazione verso “Registra” per non introdurre bug legati a tracking attivo.

---

### Dove intervenire (punti di ingresso nel codice)

- **UI Pianifica**: `mobile/app/src/main/java/it/trentosmartmountain/app/ui/screens/session/SessionHubScreen.kt`
  - `SessionPlanTab(...)` contiene già la sezione **Tracciato** con import GPX e bottone “Sfoglia”.

- **ViewModel Pianifica**: `mobile/app/src/main/java/it/trentosmartmountain/app/viewmodel/SessionPlanViewModel.kt`
  - gestisce parsing GPX (`onGpxFileSelected` → `parseGpx`) e creazione sessione (`onCreateSession`).

- **Componente mappa già esistente (riusabile)**: `mobile/app/src/main/java/it/trentosmartmountain/app/ui/screens/registra/TsmMapView.kt`
  - usa **OSMdroid**, supporta polyline (`trackGeoPoints`) e marker posizione utente.

- **Client API Retrofit**: `mobile/app/src/main/java/it/trentosmartmountain/app/data/remote/TsmApiService.kt`
  - attualmente NON contiene gli endpoint `/api/v1/sentieri/...` → da aggiungere per la modalità (2).

- **Backend (già pronto)**:
  - Router: `backend/src/routes/sentieroRoutes.js`
  - Service: `backend/src/services/sentieroService.js`
  - Endpoints disponibili:
    - `GET /api/v1/sentieri/destinazioni`
    - `GET /api/v1/sentieri/destinazioni/:nome/sentieri`
    - `GET /api/v1/sentieri/:codice`

---

## Modalità 1 — GPX: anteprima tracciato su mappa (popup)

### A) Estendere il modello del parsing GPX per ottenere una polyline
File: `SessionPlanViewModel.kt`

**Problema attuale**
`GpxParseResult` contiene metriche (km, dislivello, ecc.) ma **non** espone i punti del tracciato, quindi la UI non può disegnare una polyline.

**Cosa fare**
- In `GpxParseResult` aggiungere un campo con la lista dei punti del tracciato:
  - Opzione 1 (consigliata): `val trackLatLon: List<Pair<Double, Double>>`
  - Opzione 2: `val trackGeoPoints: List<org.osmdroid.util.GeoPoint>`

**Nota performance (importante)**
I GPX lunghi possono avere migliaia di punti → prevedere un **downsampling** (es. max 300–800 punti) prima di salvarli nello state, per evitare lag in Compose/OSMdroid.

**Output atteso**
Quando `uiState.gpxData != null`, devono esserci:
- metriche già presenti
- una lista punti utilizzabile per disegnare il tracciato nella mappa del popup

### B) Aggiungere il bottone “Visualizza nella mappa” accanto a “Sfoglia”
File: `SessionHubScreen.kt` (sezione “Importa file GPX”)

**Cosa fare**
- Nel box import GPX, dove c’è `OutlinedButton` “Sfoglia”, aggiungere un secondo bottone **a fianco**:
  - label: **“Visualizza nella mappa”**
  - enabled solo se `uiState.gpxData != null`
  - onClick: `showGpxPreviewDialog = true`

### C) Implementare il dialog di anteprima GPX
File: può rimanere in `SessionHubScreen.kt` oppure estrarre in un componente dedicato (es. `GpxPreviewDialog.kt`).

**Contenuto del dialog**
- Mappa: riuso di `TsmMapView` passando:
  - `userLocation = null`
  - `hasLocationPermission = false`
  - `trackGeoPoints = <punti dal GPX>`
  - `centerOnUserTick = 0`
- Sotto la mappa: scheda info (km, dislivello, punti, start/end).
- CTA: “Chiudi”.

**Nota UX**
Non serve attivare tracking né chiedere permessi.

---

## Modalità 2 — Scelta da database: “Scegli percorso sulla mappa” (popup)

### A) Aggiungere gli endpoint Sentieri nel client mobile
File: `TsmApiService.kt`

**Aggiungere**
- `GET api/v1/sentieri/destinazioni`
- `GET api/v1/sentieri/destinazioni/{nome}/sentieri`
- `GET api/v1/sentieri/{codice}`

> Nota: gli endpoint del backend rispondono con wrapper `{ message, count, data, ... }` (non direttamente array/oggetto). I DTO devono rispecchiare questa forma oppure usare un generic wrapper.

### B) Creare i DTO (mobile)
Directory: `mobile/.../data/remote/dto/`

**DTO consigliati**
- `SentieroDestinazioneDto`
  - `nome: String`
  - `quota: Int`
  - `numeroSentieri: Int`
  - `coordinate: { lat: Double, lon: Double }`

- `SentieroListItemDto` (risposta da “sentieri per destinazione”, coordinate escluse)
  - `codice: String`
  - `denominazione: String?`
  - `puntoInizio: { nome, quota, coordinate }`
  - `puntoFine: { nome, quota, coordinate }`
  - `difficolta: String`
  - `lunghezzaPlanimetrica: Int`
  - `lunghezzaInclinata: Int`
  - `tempoAndata: String`
  - `tempoRitorno: String`
  - (no `percorsoCoordinate`)

- `SentieroDettaglioDto` (risposta da `/sentieri/:codice`, include coordinate)
  - uguale a `SentieroListItemDto` + `percorsoCoordinate: String`

**Wrapper response**
Poiché backend risponde con `{ data: ... }`, creare:
- `ApiListResponse<T>(val data: List<T>, val message: String?, val count: Int?)`
- `ApiItemResponse<T>(val data: T, val message: String?)`
oppure response specifiche.

### C) Utility: parsing `percorsoCoordinate` → polyline OSMdroid
Il backend salva `percorsoCoordinate` in formato stringa:
- `"lon1,lat1 lon2,lat2 lon3,lat3 ..."`

**Cosa fare**
Creare una funzione (mapper/util) che converte in:
- `List<org.osmdroid.util.GeoPoint>`

**Attenzione all’ordine**
OSMdroid usa `GeoPoint(lat, lon)` ma la stringa è `lon,lat`.

**Performance**
Se la stringa ha molti punti, considerare downsampling anche qui (es. max 1000) prima di disegnare.

### D) ViewModel per il popup di scelta percorso
Nuovo ViewModel (esempio nome): `SessionRoutePickerViewModel.kt`

**Stato (state machine)**
- `step: enum { Destinations, TrailsForDestination, TrailDetail }`
- `destinations: List<SentieroDestinazioneDto>`
- `selectedDestination: SentieroDestinazioneDto?`
- `trailsForDestination: List<SentieroListItemDto>`
- `selectedTrailCode: String?`
- `selectedTrailDetail: SentieroDettaglioDto?`
- `selectedTrailPolyline: List<GeoPoint>` (solo nello step detail)
- `isLoading: Boolean`
- `error: String?`

**Eventi**
- `onOpen()` → carica destinazioni (1 sola volta)
- `onDestinationClick(dest)` → step TrailsForDestination + fetch `GET /destinazioni/:nome/sentieri`
- `onTrailClick(codice)` → step TrailDetail + fetch `GET /sentieri/:codice` + parse polyline
- `onBack()`:
  - da TrailDetail → TrailsForDestination
  - da TrailsForDestination → Destinations (ripristina tutte le destinazioni visibili)
- `onConfirm()` → emette il “risultato” verso la schermata Pianifica

### E) UI: bottone “Scegli percorso sulla mappa” nel box Tracciato
File: `SessionHubScreen.kt` (sezione Tracciato)

**Cosa fare**
Sotto (o accanto) all’import GPX, aggiungere una seconda opzione:
- bottone: **“Scegli percorso sulla mappa”**
- apre un `Dialog` full/large come per QR/time picker.

### F) UI: dialog “Scegli percorso sulla mappa”
File: `SessionHubScreen.kt` (o nuovo file `SessionRoutePickerDialog.kt`)

**Comportamento per step**
- Step Destinations:
  - mostra marker per tutte le destinazioni
  - click marker → `onDestinationClick`

- Step TrailsForDestination:
  - mostra SOLO la destinazione selezionata (marker)
  - mostra marker dei punti di partenza dei sentieri disponibili
  - pannello elenco sentieri (codice + difficoltà + tempi/distanze)
  - click su sentiero → `onTrailClick`

- Step TrailDetail:
  - disegna polyline completa
  - mostra scheda info dettagliata
  - CTA: **“Conferma tracciato”**

**Suggerimento tecnico**
`TsmMapView` oggi gestisce solo:
- polyline live track
- marker user

Per questa feature servirà una variante/estensione, ad esempio:
- permettere **markers custom** (destinazioni, start points, selected destination)
- permettere **polyline** del sentiero selezionato

Approccio consigliato:
- creare un nuovo composable `TsmSentieriMapView(...)` che riusa le stesse basi di OSMdroid (tile, lifecycle) ma gestisce:
  - lista markers generici
  - 0/1 polyline
In alternativa: estendere `TsmMapView` aggiungendo parametri opzionali per markers extra e polyline “preview”.

### G) Collegare la selezione al form Pianifica (mutua esclusione)
File: `SessionPlanViewModel.kt` + `SessionHubScreen.kt`

**Requisito**
Le due modalità sono alternative:
- se seleziono un sentiero da DB → reset `gpxData = null`
- se importo GPX → reset `selectedSentiero = null` (o equivalente)

**Cosa salvare nello state Pianifica**
Opzioni:
- aggiungere in `UiState` un `selectedSentiero: SentieroDettaglioDto?` (o un model più piccolo “RouteSelection”).

**Mapping verso CreateSessionRequest**
Quando si crea la sessione, valorizzare:
- `routeDetails.name`: da `denominazione` (se vuota → usare `codice`)
- `routeDetails.difficultyLevel`: da `difficolta`
- `routeDetails.startPoint/endPoint`: da `puntoInizio.coordinate` e `puntoFine.coordinate` (attenzione: GeoJSON nel request usa `[lon, lat]`)
- `elevationGain`: opzionale
  - possibile stima: `quotaMassima - quotaMinima`
  - oppure null
- `gpxStats.distanceKm`: opzionale
  - `lunghezzaPlanimetrica` è in metri → km = metri / 1000.0
  - anche se il campo si chiama “gpxStats”, è usato come “route stats” nella UI → ok riutilizzarlo.

---

## Checklist qualità (anti-bug / anti-lag)

- **Downsampling**: applicarlo sia per GPX import sia per `percorsoCoordinate` dei sentieri.
- **Dialog senza navigazione**: usare `Dialog` Compose, non cambiare screen.
- **Gestione errori rete**:
  - destinazioni: mostra “Riprova”
  - sentieri per destinazione: 404 → mostra messaggio e rimani nello step, o torna alle destinazioni
  - dettaglio sentiero: 404 → messaggio e torna all’elenco
- **Visibilità marker**:
  - dopo selezione destinazione: nascondere le altre destinazioni
  - prevedere un tasto “Indietro” nel dialog per cambiare destinazione
- **Coerenza coordinate**:
  - Backend `percorsoCoordinate`: `lon,lat`
  - OSMdroid `GeoPoint(lat, lon)`
  - Request `GeoPoint(coordinates=[lon,lat])` (già usato in `SessionPlanViewModel`)

---

## Deliverable attesi (per review)

- Nuovo bottone “Visualizza nella mappa” nella UI import GPX + dialog con mappa e tracciato.
- Nuovo bottone “Scegli percorso sulla mappa” + dialog completo con flusso destinazioni → sentieri → dettaglio → conferma.
- Client mobile aggiornato:
  - Retrofit endpoints per sentieri
  - DTO + wrapper response
  - parsing `percorsoCoordinate` in polyline
  - ViewModel e stato dedicato al dialog picker
- In `SessionPlanViewModel`, gestione chiara della modalità scelta (GPX vs DB) e mapping verso `CreateSessionRequest`.

