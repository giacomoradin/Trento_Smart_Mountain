## Realtime tracking sessione — Backend & API contract

### Scopo
Implementare il tracciamento realtime dei partecipanti di una sessione **ACTIVE** con:
- upload periodico posizione (polling lato client ogni **5s**)
- fetch periodico posizioni partecipanti (polling lato client ogni **5s**)
- marker differenziati: **capogruppo** (colore diverso) e **SOS** (rosso, visibilità già regolata da logica share/leader)
- sospensione realtime per utenti **troppo lontani dalla polyline** del percorso pianificato: smettono di caricare e non compaiono nel feed live.

Questo documento definisce **schema dati**, **endpoint**, **payload**, **codici errore** e **regole auth**.

---

## Prerequisito: polyline del percorso pianificato nella sessione
Il client deve poter calcolare la distanza dalla polyline; quindi la sessione deve includere una polyline “planned”.

### Campo da aggiungere a `HikeSession`
Proposta (minima, V1):
- `plannedRoute`
  - `source`: `"GPX"` | `"SAT"`
  - `polylinePoints`: `[{ lat: number, lon: number }, ...]` (downsample 300–800 punti)
  - `pointsCountOriginal?`: number
  - `pointsCountStored?`: number
  - `bbox?`: `{ minLat, minLon, maxLat, maxLon }`
  - `updatedAt`: Date

Nota: usare array è più semplice per V1. In V2 si può comprimere (encoded polyline).

### Come viene popolato
- Creazione sessione (POST `/api/v1/sessions`) deve accettare `plannedRoute` nel body.
- Se non presente, il realtime “distance from polyline” non funziona: decidere se bloccare l’avvio o fallback (in questo progetto: **non avviare** sospensione da polyline).

---

## Live tracking: dati per posizione realtime

### Campo da aggiungere a `HikeSession`
Mantenere **una sola posizione per utente** (last known):
- `liveLocations`: array di subdocumenti:
  - `userId`: ObjectId (ref User)
  - `lat`: number
  - `lon`: number
  - `accuracyM?`: number
  - `updatedAt`: Date

Stato realtime per utente (sospeso / attivo):
- `liveTracking`: array di subdocumenti:
  - `userId`: ObjectId (ref User)
  - `status`: `"ACTIVE"` | `"SUSPENDED"`
  - `reason?`: `"TOO_FAR_FROM_ROUTE"` | `"MANUAL"` | `"OTHER"`
  - `updatedAt`: Date

Policy: un utente può restare in `participants` (membership), ma se `liveTracking.status=SUSPENDED`:
- il server rifiuta upload posizione
- il server non lo include nel feed `GET live-locations`

TTL / pulizia:
- In `GET live-locations` escludere posizioni stale (es. `updatedAt < now-30s`).

---

## Endpoint nuovi

### 1) POST `/api/v1/sessions/:id/live-location`
Upload last location del chiamante.

**Auth**
- Richiede JWT (come altre rotte session).

**Autorizzazione**
- l’utente deve essere `creatorId` oppure in `participants.userId`
- sessione deve essere `status === "ACTIVE"`

**Request body**
```json
{
  "lat": 46.07,
  "lon": 11.12,
  "accuracyM": 8.5,
  "timestampMs": 1716900000000
}
```

**Response 200**
```json
{ "message": "Live location aggiornata." }
```

**Errori**
- `404 SESSION_NOT_FOUND`
- `403 NOT_IN_SESSION`
- `409 SESSION_NOT_ACTIVE` (oppure `400`)
- `403 LIVE_TRACKING_SUSPENDED` (include reason)
```json
{ "message": "LIVE_TRACKING_SUSPENDED", "reason": "TOO_FAR_FROM_ROUTE" }
```

**Comportamento**
- Upsert su `liveLocations` per `userId`:
  - se esiste → update lat/lon/accuracyM/updatedAt
  - se non esiste → push nuovo subdoc
- Non calcola qui la distanza dalla polyline (calcolo è lato client). Il server applica lo stato `SUSPENDED` se già impostato.

---

### 2) GET `/api/v1/sessions/:id/live-locations`
Ritorna le posizioni live di tutti i partecipanti (attivi) utili al rendering marker.

**Auth**
- Richiede JWT.

**Autorizzazione**
- l’utente deve essere `creatorId` oppure in `participants.userId`

**Query params (opzionali)**
- `maxAgeSec` default 30 (esclude stale)

**Response 200**
```json
{
  "message": "Live locations",
  "data": [
    {
      "user": {
        "id": "6650...",
        "username": "mario",
        "avatarUrl": "data:image/..../base64,...",
        "role": "groupLeader"
      },
      "location": {
        "lat": 46.07,
        "lon": 11.12,
        "accuracyM": 8.5,
        "updatedAt": "2026-05-28T15:00:00.000Z"
      }
    }
  ]
}
```

**Inclusione/esclusione**
- includere solo utenti con `liveTracking.status !== "SUSPENDED"` (default ACTIVE se non presente)
- escludere chi ha `liveLocations.updatedAt < now - maxAgeSec`

**Errori**
- `404 SESSION_NOT_FOUND`
- `403 NOT_IN_SESSION`

---

### 3) POST `/api/v1/sessions/:id/live-tracking/suspend`
Sospende il live tracking di un utente (non può più caricare e non appare nel feed live).

**Auth**
- Richiede JWT.

**Autorizzazione**
- Solo capogruppo (creator) **oppure** estendibile ad admin.

**Request body**
```json
{
  "userId": "6650...",
  "reason": "TOO_FAR_FROM_ROUTE"
}
```

**Response 200**
```json
{ "message": "Utente sospeso dal live tracking." }
```

**Errori**
- `404 SESSION_NOT_FOUND`
- `403 ONLY_CREATOR`
- `400 USER_NOT_PARTICIPANT`

Nota: il client “lontano” può auto-sospendersi chiamando questo endpoint; tuttavia per enforcement coerente conviene permetterlo anche all’utente stesso (alternativa: endpoint `/self/suspend`). Se vuoi mantenerlo semplice: lascia solo creator e fai sospensione server-side su richiesta del leader.

---

### 4) POST `/api/v1/sessions/:id/live-tracking/resume` (opzionale)
Riattiva un utente sospeso.

**Auth/Autorizzazione**
- Solo capogruppo (o admin) oppure anche self.

---

## Integrazione con SOS (visibilità)
Il backend SOS esiste già via:
- `GET /api/v1/sessions/:id/emergencies`
e la visibilità è già gestita dal client:
- leader vede tutto
- partecipanti vedono solo `SHARED_WITH_GROUP` quando il leader usa “Condividi con il gruppo”.

Il feed `GET live-locations` **non deve includere** stati SOS: il mobile calcola “chi è in SOS” incrociando con la lista emergenze già in polling (8s).

---

## Note implementative (Mongoose)
- Preferire update atomico per upsert della location:
  - `findOneAndUpdate` con positional operator su `liveLocations.userId`
  - oppure due step: cerca index e aggiorna subdoc, ma attenzione race.
- Indici utili:
  - `liveLocations.userId`
  - `liveTracking.userId`

---

## Definition of Done (Backend/API)
- **Schema**
  - `HikeSession.plannedRoute.source` e `HikeSession.plannedRoute.polylinePoints` presenti e valorizzabili in creazione sessione.
  - `HikeSession.liveLocations` mantiene **una sola location per utente** (upsert).
  - `HikeSession.liveTracking` gestisce `ACTIVE|SUSPENDED` per utente.
- **POST live-location**
  - `POST /api/v1/sessions/:id/live-location` accetta `{lat, lon, accuracyM?, timestampMs?}`.
  - Se sessione non `ACTIVE` → errore coerente (`409` o `400`) e messaggio stabile.
  - Se user non è partecipante → `403 NOT_IN_SESSION`.
  - Se user è `SUSPENDED` → `403 LIVE_TRACKING_SUSPENDED` con `reason`.
- **GET live-locations**
  - `GET /api/v1/sessions/:id/live-locations` ritorna `data[]` con `{ user{id,username,avatarUrl,role}, location{lat,lon,accuracyM?,updatedAt} }`.
  - Esclude utenti `SUSPENDED`.
  - Esclude location stale oltre `maxAgeSec` (default 30s).
- **Sospensione**
  - `POST /api/v1/sessions/:id/live-tracking/suspend` imposta lo stato `SUSPENDED` in modo persistente.
  - Un utente sospeso **non appare** più in `GET live-locations` e non può più fare upload.
- **Compatibilità con Mobile**
  - I payload sono stabili e non cambiano nomi campi (no breaking per Gson).
  - `avatarUrl` usa lo stesso campo già popolato nelle sessioni (`personalInfo.avatarUrl`).

