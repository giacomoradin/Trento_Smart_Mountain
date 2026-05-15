

# 🌦️ Weather Service & API (Modulo Meteo)

Questo branch introduce la gestione completa delle previsioni meteorologiche per **Towns** (Comuni) e **POI** (Punti di Interesse). Il sistema minimizza le chiamate alle API esterne tramite una doppia strategia di caching (in-memory per le anagrafiche e MongoDB per i forecast).

## 🚀 Funzionalità principali

* **Geospatial Search**: Ricerca di location vicine tramite coordinate (Lon/Lat) con supporto MongoDB `2dsphere`.
* **Smart Forecast Association**: I POI non memorizzano previsioni proprie; il sistema risolve automaticamente la "Town" di riferimento tramite `regionId` per recuperare i dati meteo.
* **Dual-Layer Cache**:
* **Venues**: I dati statici (nomi, coordinate) sono in RAM per 24 ore.
* **Forecasts**: Le previsioni (3h/24h) sono salvate su MongoDB con un TTL (Time To Live) di 1 ora.


* **On-Demand Population**: Il database viene popolato con i dati meteo solo quando una location viene effettivamente richiesta (Lazy Loading).

---

## 🛠️ Architettura dei Dati

Il sistema usa un modello Mongoose `Location` per due entità:

1. **Town**: Contiene l'oggetto `forecasts` (slot meteo reali).
2. **POI**: Contiene metadati geografici e punta a una Town tramite `regionId`.

---

## 🛣️ API Endpoints

Puoi testare gli endpoint tramite la dashboard **Swagger** (`/api-docs`) o utilizzando **Thunder Client / Postman**.

### Location

| Metodo | Endpoint | Descrizione |
| --- | --- | --- |
| `GET` | `/weather/locations/search?q=...` | Cerca location per nome. |
| `GET` | `/weather/locations/nearby?lon=..&lat=..` | Trova luoghi vicini (default 50km). |

### Previsioni

| Metodo | Endpoint | Descrizione |
| --- | --- | --- |
| `GET` | `/weather/forecast/:externalId` | Recupera il meteo. Salva su DB se assente o scaduto. |
| `POST` | `/weather/forecast/:id/refresh` | Forza l'aggiornamento immediato dei dati nel DB. |

### Admin

| Metodo | Endpoint | Descrizione |
| --- | --- | --- |
| `POST` | `/weather/seed` | Inizializza il DB con 700+ anagrafiche (senza forecast). |

---

## 🧪 Come Testare

### 1. Seed del Database

Invia una richiesta `POST` a `/weather/seed`.

* **Risultato**: Il database verrà popolato con i nomi e le coordinate delle città (es. Laives, Cornedo all'Isarco).
* *Nota: In questa fase il campo `forecasts` nel DB rimarrà vuoto o null.*

### 2. Test del Salvataggio (On-Demand)

Scegli una Town (es. Laives) e usa la `GET /weather/forecast/{externalId}`.

* **Cosa succede**: Il sistema vede che il DB è vuoto, scarica i dati dall'API esterna e **li salva automaticamente in MongoDB**.
* **Verifica**: Controlla il campo `meta.fromCache` nella risposta: sarà `false`. Se rifai la stessa chiamata entro un'ora, diventerà `true`.

### 3. Forza Aggiornamento (Refresh)

Usa la `POST /weather/forecast/{externalId}/refresh`.

* Questo endpoint ignora la cache e forza il sistema a riscrivere il documento sul database con dati freschi. Utile per testare la persistenza immediata.

### 4. Verifica su VS Code (MongoDB Extension)

Per vedere i dati salvati nel database:

1. Apri la barra laterale di MongoDB in VS Code.
2. **Importante**: Clicca sull'icona **Refresh** (freccia circolare) sulla collection `locations`.
3. Usa un **Playground** per cercare i documenti popolati:
```javascript
db.locations.find({ "forecasts.fetchedAt": { $exists: true } })

```



---

## 📦 Note Tecniche

* **Indici**: È richiesto l'indice `2dsphere` sul campo `location` per le ricerche geografiche.
* **Mongoose**: Viene utilizzato `location.markModified('forecasts')` per garantire che i cambiamenti agli oggetti annidati vengano sempre rilevati e salvati.
* **Performance**: I POI caricano i dati dalla Town di riferimento tramite una seconda query ottimizzata per `externalId`.

# Query utile per MongoDB
```javascript
/* global use, db */
use('trento_smart_mountain');

const citta = db.locations.findOne({ name: "Laives" });

if (!citta || !citta.forecasts) {
    print("ERRORE: Dati non trovati per Laives.");
} else {
    // FUNZIONE DI SUPPORTO PER STAMPARE I DATI
    const stampaDettagli = (slot, tipo) => {
        const dataStr = slot.validFrom.toISOString().replace('T', ' ').substring(0, 16);
        print(`\n[${tipo}] - Inizio: ${dataStr}`);
        print(`  🌡️ Temp: ${slot.temperature}°C | 🧊 Zero Termico: ${slot.freezingLevel}m`);
        print(`  💧 Pioggia: ${slot.rainFall}mm (${slot.rainProbability}%)`);
        print(`  ❄️ Neve: ${slot.freshSnow}cm (Quota: ${slot.snowLevel}m)`);
        print(`  💨 Vento: ${slot.windSpeed}km/h (Raffica: ${slot.windGust}km/h) - Dir: ${slot.windDirection}°`);
        print(`  ☀️ Sole: ${slot.sunshineDuration}h | ☁️ Sky: ${slot.skyCondition}`);
        print(`--------------------------------------------------`);
    };

    print(`========== REPORT METEO: ${citta.name} ==========`);

    // 1. PRIME 3 PREVISIONI (OGNI 3 ORE)
    print(`\n>>> PRIME 3 PREVISIONI (DETTAGLIO 3H)`);
    citta.forecasts.slots3h.slice(0, 3).forEach(slot => {
        stampaDettagli(slot, "3 ORE");
    });

    print(`\n\n`);

    // 2. PRIME 3 PREVISIONI (GIORNALIERE 24H)
    print(`\n>>> PRIME 3 PREVISIONI (GIORNALIERE 24H)`);
    citta.forecasts.slots24h.slice(0, 3).forEach(slot => {
        stampaDettagli(slot, "24 ORE");
    });
}
```