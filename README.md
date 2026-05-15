# 🌦️ Weather Service & API (Modulo Meteo)

Questo branch introduce la gestione completa delle previsioni meteorologiche per **Towns** (Comuni) e **POI** (Punti di Interesse). Il sistema minimizza le chiamate alle API esterne tramite una doppia strategia di caching.

## 🚀 Funzionalità principali

- **Geospatial Search**: ricerca di location vicine tramite coordinate (lon/lat) con indice MongoDB `2dsphere`.
- **Smart Forecast Association**: i POI non memorizzano previsioni proprie; il sistema risolve automaticamente la Town di riferimento tramite `regionId`.
- **Dual-Layer Cache**:
  - **Venues** (towns + POI): dati statici tenuti in RAM per 24 ore.
  - **Forecasts**: previsioni (3h/24h) salvate su MongoDB, considerate valide per 1 ora.
- **Lazy Loading**: il forecast viene scaricato dall'API esterna e persistito su MongoDB solo alla prima richiesta (o quando scaduto).
- **Auto-seed all'avvio**: all'avvio del server, `seedLocations()` viene chiamata automaticamente per popolare il DB con towns e POI (operazione idempotente, sicura da eseguire più volte).

---

## 🛠️ Architettura dei Dati

Il modello Mongoose `Location` gestisce due tipi di entità distinte:

| Campo | Town | POI |
|---|---|---|
| `type` | `"town"` | `"poi"` |
| `forecasts` | Contiene gli slot meteo reali | Sempre `null` |
| `regionId` | ID della regione geografica | **`externalId` della Town di riferimento** |

Quando si richiede il forecast di un POI, il sistema usa `regionId` per trovare la Town collegata e restituire le sue previsioni.

---

## 🛣️ API Endpoints

Il server gira di default su `http://localhost:3000`. La dashboard Swagger è disponibile su `/api-docs`.

> Tutti gli endpoint sono sotto il prefisso `/weather`.

---

### `GET /weather/locations/search`

Cerca locations nel DB locale per nome (ricerca case-insensitive, parziale).

**Query parameters:**

| Parametro | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|
| `q` | string | ✅ | — | Testo da cercare (min. 2 caratteri) |
| `type` | string | ❌ | tutti | Filtra per tipo: `town` oppure `poi` |
| `limit` | number | ❌ | `10` | Numero massimo di risultati |

**Esempio:**
```
GET /weather/locations/search?q=Merano&type=town&limit=5
```

**Risposta:**
```json
{
  "count": 1,
  "results": [
    {
      "externalId": "5d9e12bb-...",
      "type": "town",
      "name": "Merano",
      "elevation": 325,
      "location": { "type": "Point", "coordinates": [11.159, 46.671] },
      "regionId": "..."
    }
  ]
}
```

---

### `GET /weather/locations/nearby`

Trova locations vicine a una coordinata geografica.

**Query parameters:**

| Parametro | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|
| `lon` | number | ✅ | — | Longitudine |
| `lat` | number | ✅ | — | Latitudine |
| `maxDistance` | number | ❌ | `50000` | Raggio di ricerca in **metri** |
| `type` | string | ❌ | tutti | Filtra per tipo: `town` oppure `poi` |
| `limit` | number | ❌ | `5` | Numero massimo di risultati |

**Esempio:**
```
GET /weather/locations/nearby?lon=11.35&lat=46.50&maxDistance=20000&type=town
```

**Risposta:** stessa struttura di `/locations/search`.

---

### `GET /weather/forecast/:externalId`

Restituisce il forecast per una location (Town o POI). Se i dati sono assenti o scaduti (>1h), vengono scaricati dall'API esterna e salvati su MongoDB automaticamente.

> Se la location è un **POI**, il forecast viene risolto dalla Town collegata tramite `regionId`. La risposta includerà sia i dati del POI che quelli della Town di riferimento.

**Path parameter:**

| Parametro | Descrizione |
|---|---|
| `externalId` | UUID della location (ricavabile da `/locations/search`) |

**Query parameter opzionale:**

| Parametro | Tipo | Default | Descrizione |
|---|---|---|---|
| `forceRefresh` | boolean | `false` | Se `true`, ignora la cache e scarica dati freschi |

**Esempio:**
```
GET /weather/forecast/5d9e12bb-7274-483e-9acd-44bfdcb916e5
GET /weather/forecast/5d9e12bb-7274-483e-9acd-44bfdcb916e5?forceRefresh=true
```

**Risposta:**
```json
{
  "location": {
    "externalId": "5d9e12bb-...",
    "type": "town",
    "name": "Merano",
    "elevation": 325,
    "coordinates": [11.159, 46.671]
  },
  "referenceTown": { ... },  // presente solo se la location è un POI
  "meta": {
    "fetchedAt": "2025-05-15T10:00:00.000Z",
    "validFrom": "2025-05-15T06:00:00.000Z",
    "validTo":   "2025-05-22T06:00:00.000Z",
    "fromCache": true
  },
  "forecast3h":  [ /* fino a 16 slot → prossime ~48h */ ],
  "forecast24h": [ /* fino a 7 slot  → prossimi 7 giorni */ ]
}
```

**Campi di ogni slot meteo:**

| Campo | Unità | Descrizione |
|---|---|---|
| `validFrom` / `validTo` | ISO 8601 | Intervallo di validità dello slot |
| `temperature` | °C | Temperatura |
| `rainFall` | mm | Precipitazioni |
| `rainProbability` | % | Probabilità pioggia |
| `freshSnow` | cm | Neve fresca |
| `snowLevel` | m s.l.m. | Quota neve |
| `windSpeed` / `windGust` | km/h | Velocità e raffica vento |
| `windDirection` | 0–360° | Direzione vento |
| `freezingLevel` | m s.l.m. | Zero termico |
| `skyCondition` | codice | Condizione cielo (es. `"A"`, `"B"`, `"C"`) |
| `sunshineDuration` | ore | Ore di sole |

> `meta.fromCache: true` significa che i dati provengono dal DB (< 1h); `false` significa che sono stati appena scaricati dall'API esterna.

---

### `POST /weather/forecast/:externalId/refresh`

Forza il refresh del forecast ignorando completamente la cache. Scarica dati freschi dall'API esterna e li sovrascrive su MongoDB.

> **Solo per Towns.** Chiamare su un POI restituisce errore `400`.

**Esempio:**
```
POST /weather/forecast/5d9e12bb-7274-483e-9acd-44bfdcb916e5/refresh
```

**Risposta:**
```json
{
  "message": "Forecast aggiornato per Merano",
  "fetchedAt": "2025-05-15T10:05:00.000Z",
  "slotsCount": { "3h": 48, "24h": 7 }
}
```

---

### `POST /weather/seed`

Popola (o aggiorna) il DB con tutte le towns e i POI dall'API esterna. Operazione idempotente (usa upsert): sicura da chiamare più volte.

> Il seed viene eseguito automaticamente all'avvio del server. Questo endpoint è utile per forzare un aggiornamento manuale delle anagrafiche.

**Risposta:**
```json
{
  "message": "Seed completato",
  "towns": 712,
  "pois": 183
}
```

---

## 🧪 Come Testare (Flusso Completo)

> **Prerequisito**: server avviato e MongoDB raggiungibile. Il seed delle anagrafiche avviene in automatico all'avvio.

### Passo 1 — Trova l'`externalId` di una location

Cerca una Town per nome:
```
GET /weather/locations/search?q=Laives&type=town
```

Dalla risposta, copia il campo `externalId` del risultato. Ti servirà nei passi successivi.

### Passo 2 — Richiedi il forecast (prima volta)

```
GET /weather/forecast/<externalId-copiato>
```

- Il DB è ancora vuoto per quella location → il sistema scarica i dati dall'API esterna e li salva su MongoDB.
- Nella risposta: `meta.fromCache` sarà `false`.

### Passo 3 — Verifica la cache

Ripeti la stessa richiesta entro un'ora:
```
GET /weather/forecast/<externalId-copiato>
```
- Questa volta: `meta.fromCache` sarà `true` (dati serviti dal DB, nessuna chiamata esterna).

### Passo 4 — Forza un aggiornamento

```
POST /weather/forecast/<externalId-copiato>/refresh
```
- Ignora la cache, scarica dati freschi e sovrascrive il documento su MongoDB.

### Passo 5 — Test con un POI

Cerca un POI vicino a una coordinata:
```
GET /weather/locations/nearby?lon=11.35&lat=46.50&type=poi&limit=3
```

Poi richiedi il forecast del POI trovato:
```
GET /weather/forecast/<externalId-poi>
```
- Il campo `referenceTown` nella risposta mostra la Town da cui sono stati risolti i dati meteo.

---

## 🔍 Verifica su MongoDB (VS Code)

Per ispezionare i dati salvati direttamente nel DB:

1. Apri la barra laterale **MongoDB** in VS Code.
2. Clicca su **Refresh** (icona freccia circolare) sulla collection `locations`.
3. Usa un Playground per cercare le location che hanno già forecast:

```javascript
/* global use, db */
use('trento_smart_mountain');

db.locations.find({ "forecasts.fetchedAt": { $exists: true } });
```

Per leggere i dati meteo di una specifica città in modo leggibile:

```javascript
/* global use, db */
use('trento_smart_mountain');

const citta = db.locations.findOne({ name: "Laives" });

if (!citta?.forecasts) {
    print("ERRORE: Dati non trovati o forecast non ancora caricato.");
} else {
    const stampa = (slot, tipo) => {
        const data = slot.validFrom.toISOString().replace('T', ' ').substring(0, 16);
        print(`[${tipo}] ${data} | Temp: ${slot.temperature}°C | Neve: ${slot.freshSnow}cm | Vento: ${slot.windSpeed}km/h | Sky: ${slot.skyCondition}`);
    };

    print(`=== ${citta.name} — Forecast 3h (prime 3 slot) ===`);
    citta.forecasts.slots3h.slice(0, 3).forEach(s => stampa(s, "3h"));

    print(`\n=== ${citta.name} — Forecast 24h (prime 3 slot) ===`);
    citta.forecasts.slots24h.slice(0, 3).forEach(s => stampa(s, "24h"));
}
```

---

## 📦 Note Tecniche

- **Indice 2dsphere**: richiesto sul campo `location` per le query geospaziali (`$near`). Viene creato automaticamente dallo schema Mongoose.
- **Idempotenza del seed**: usa `bulkWrite` con `upsert: true`, quindi non duplica i dati.
- **`markModified`**: Mongoose richiede `location.markModified('forecasts')` per rilevare modifiche a oggetti annidati — già gestito nel service.
- **Fonte dati esterna**: towns e POI da [gitlab.com/tinia-euregio](https://gitlab.com/tinia-euregio/tinia-website/-/raw/main/data/venues/it/); forecast da `meteo.report`.