# API Reference — Trento Smart Mountain

> Reference human-readable degli endpoint REST del backend TSM, complementare al Swagger autogenerato (`swagger-output.json` → UI su `http://<host>:3000/api-docs`).
>
> **Ultima revisione**: 17/05/2026 — Fine Sprint 1.
> **Base URL dev**: `http://10.0.2.2:3000` (emulator) / `http://localhost:3000` (Postman dev).

---

## 1. Convenzioni generali

### 1.1 Authentication

Per tutti gli endpoint marcati **🔐 JWT**, includere header:

```http
Authorization: Bearer <jwt-token>
```

Il JWT è restituito da `POST /auth/login`. Scade dopo il tempo configurato in `JWT_EXPIRATION_HOURS` (default backend: 7 giorni).

### 1.2 Content-Type

- Request body: `application/json` (eccetto `POST /auth/reset-password/:token` che accetta anche `application/x-www-form-urlencoded` per il form HTML).
- Response: sempre `application/json` (eccetto `GET /auth/reset-password/:token` che ritorna HTML).

### 1.3 Status codes utilizzati

| Code | Quando |
|------|--------|
| `200 OK` | GET/PATCH/DELETE success |
| `201 Created` | POST resource creata |
| `400 Bad Request` | Body malformato o parametri mancanti |
| `401 Unauthorized` | JWT mancante, scaduto o invalido |
| `403 Forbidden` | JWT valido ma utente non autorizzato (es. non creator) |
| `404 Not Found` | Risorsa non esistente |
| `409 Conflict` | Vincolo violato (es. email già usata, codice invito già esistente) |
| `500 Internal Server Error` | Errore generico non mappato |

### 1.4 Formato errori

```json
{ "error": "Descrizione human-readable" }
```

oppure

```json
{ "message": "Operazione completata" }
```

---

## 2. Autenticazione (`/auth`)

### `POST /auth/login`

Login utente.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | — | Pubblico |
| **Body** | JSON | `{ email, password }` |
| **Response 200** | JSON | `{ token, userId, role, username, email }` |
| **Response 401** | JSON | `{ error: "Credenziali non valide" }` |
| **Response 403** | JSON | `{ error: "Email non verificata" }` |

#### Esempio request

```http
POST /auth/login
Content-Type: application/json

{
  "email": "mario.rossi@example.com",
  "password": "password123"
}
```

#### Esempio response

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "67d8a1f4e2b3c4d5e6f7g8h9",
  "role": "groupLeader",
  "username": "mario.rossi@example.com",
  "email": "mario.rossi@example.com"
}
```

---

### `GET /auth/verify/:token`

Verifica email tramite token ricevuto in mail. Restituisce un redirect a deep link `tsm://`.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | — | Pubblico (token nel path è la sicurezza) |
| **Param** | string | `token` random 32-byte hex |
| **Response 302** | redirect | `tsm://auth/verify/<token>?email=<email>` |
| **Response 400** | redirect | `tsm://auth/verify/<token>?error=invalid` |

---

### `POST /auth/forgot-password`

Richiede l'invio dell'email di reset password.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | — | Pubblico |
| **Body** | JSON | `{ email }` |
| **Response 200** | JSON | `{ message: "Email di reset inviata se l'account esiste" }` |

**Nota**: la response è sempre 200 (anche se l'email non esiste) per non permettere user enumeration.

---

### `GET /auth/reset-password/:token`

Pagina HTML responsive per inserire nuova password (servita inline dal backend).

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | — | Pubblico |
| **Response 200** | HTML | Form responsive |
| **Response 400** | HTML | "Link scaduto o non valido" |

---

### `POST /auth/reset-password/:token`

Salva la nuova password. Accetta JSON (API) o form-urlencoded (HTML form).

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | — | Pubblico (token nel path) |
| **Body JSON** | `{ password }` | Min 8 caratteri |
| **Body form** | `password=...` | Idem |
| **Response 200** | JSON o HTML | Success page |
| **Response 400** | idem | Token scaduto / password debole |

---

## 3. Utenti (`/users`)

### `POST /users`

Registrazione nuovo utente o rifugio.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | — | Pubblico |
| **Body** | JSON | Vedi sotto |

#### Body utente standard

```json
{
  "username": "mario.rossi@example.com",
  "email": "mario.rossi@example.com",
  "password": "password123",
  "role": "groupLeader"
}
```

#### Body rifugio

```json
{
  "username": "rifugio.bicchiere@example.com",
  "email": "rifugio.bicchiere@example.com",
  "password": "password123",
  "role": "rifugio",
  "rifugioDetails": {
    "rifugioName": "Rifugio Bolzano al Bicchiere",
    "caiCode": "B046",
    "quota": 2541,
    "posti": 30,
    "coordinates": "46.6231 11.4583"
  }
}
```

#### Response 201

```json
{
  "userId": "67d8a1f4e2b3c4d5e6f7g8h9",
  "message": "Account creato. Controlla la tua email per verificare."
}
```

#### Response 409

```json
{ "error": "Email già registrata" }
```

---

### `GET /users/:id`

Profilo utente per ID.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | 🔐 JWT | — |
| **Response 200** | JSON | User document (senza `passwordHash`) |
| **Response 404** | JSON | Utente non trovato |

---

### `PUT /users/:id`

Aggiorna utente. Solo admin.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | 🔐 JWT + role=admin | `requireRoles("admin")` |
| **Body** | JSON | Campi da aggiornare |
| **Response 200** | JSON | User aggiornato |
| **Response 403** | JSON | Solo admin |

---

### `DELETE /users/:id`

Elimina utente. Solo admin.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | 🔐 JWT + role=admin | — |
| **Response 200** | JSON | `{ message: "Utente eliminato" }` |
| **Response 403** | JSON | Solo admin |

---

## 4. Sessioni Escursione (`/api/v1/sessions`)

> ⚠️ Tutte le route sotto richiedono `🔐 JWT` (middleware `router.use(authenticate)`).

### `POST /api/v1/sessions`

Crea una nuova sessione. Il creator diventa automaticamente `groupLeader`.

#### Body

```json
{
  "routeDetails": {
    "name": "Catinaccio – Domenica",
    "difficultyLevel": "EE",
    "startPoint": { "type": "Point", "coordinates": [11.62, 46.43] },
    "endPoint":   { "type": "Point", "coordinates": [11.65, 46.42] }
  },
  "meetingDate": "2026-05-18",
  "meetingTime": "06:30",
  "meetingLocation": "Parcheggio Vajolet",
  "maxParticipants": 8,
  "minExperienceLevel": "E",
  "gpxFileName": "catinaccio.gpx",
  "gpxStats": {
    "distanceKm": 12.4,
    "elevationGainM": 850,
    "trackPoints": 1834,
    "elevationProfile": [1230, 1245, 1267, ...],
    "estimatedPoints": 196
  }
}
```

#### Response 201

```json
{
  "_id": "67d8...",
  "inviteCode": "TSM-7A4F",
  "creatorId": { "_id": "...", "username": "..." },
  "participants": [{ "userId": {...}, "role": "groupLeader" }],
  "status": "PLANNED",
  ...
}
```

#### Errori specifici

| Code | Trigger |
|------|---------|
| `400` | `routeDetails.name` mancante |
| `409` | `USER_ALREADY_IN_SESSION` — utente ha già sessione ACTIVE |

---

### `GET /api/v1/sessions/my`

Lista sessioni dell'utente loggato (sia come creator sia come participant).

| Campo | Tipo | Note |
|-------|------|------|
| **Response 200** | JSON | `[ Session, Session, ... ]` ordinato per `meetingDate ASC` |

---

### `GET /api/v1/sessions/:id`

Dettaglio sessione con populate completo.

| Campo | Tipo | Note |
|-------|------|------|
| **Response 200** | JSON | SessionResponse fully populated |
| **Response 404** | JSON | Sessione non trovata |

---

### `GET /api/v1/sessions/stats?year=2026`

Statistiche aggregate delle attività COMPLETED per anno.

#### Response 200

```json
{
  "year": 2026,
  "totalActivities": 5,
  "totalDistanceKm": 62.5,
  "totalElevationGainM": 3240,
  "totalPoints": 1180,
  "monthlyActivityCount": [0, 1, 0, 1, 2, 1, 0, 0, 0, 0, 0, 0],
  "monthlyAvgDifficulty": [0, 0.5, 0, 0.75, 0.625, 0.5, 0, 0, 0, 0, 0, 0]
}
```

---

### `POST /api/v1/sessions/join`

Unisciti a una sessione tramite codice invito.

#### Body

```json
{ "inviteCode": "TSM-7A4F" }
```

#### Response 200

```json
{ Session fully populated, con userId tra participants }
```

#### Errori specifici

| Code | Trigger |
|------|---------|
| `400` | `inviteCode` mancante |
| `404` | Codice non valido |
| `409` | `USER_ALREADY_IN_SESSION` / `SESSION_NOT_JOINABLE` / `ALREADY_IN_SESSION` |

---

### `POST /api/v1/sessions/:id/leave`

Abbandona sessione. NON disponibile per il creator (deve usare DELETE).

| Campo | Tipo | Note |
|-------|------|------|
| **Response 200** | JSON | `{ message: "Abbandonata" }` |
| **Response 403** | JSON | `CREATOR_CANNOT_LEAVE` |

---

### `PATCH /api/v1/sessions/:id`

Modifica dettagli sessione. **Solo creator**.

#### Body (tutti i campi opzionali)

```json
{
  "routeDetails": { "name": "...", "difficultyLevel": "E" },
  "meetingDate": "2026-05-19",
  "meetingTime": "07:00",
  "meetingLocation": "...",
  "maxParticipants": 10,
  "minExperienceLevel": "E"
}
```

#### Response 200

```json
{ Session fully populated dopo update }
```

> **Nota implementativa Sprint 1**: il client Kotlin usa `Response<ApiMessageBody>` invece di `Response<SessionResponse>` per evitare crash Gson — vedere ADR-007 in `architecture.md`. Il populate symmetric backend rende safe entrambe le opzioni.

---

### `PATCH /api/v1/sessions/:id/status`

Cambia status della sessione. **Solo creator** (Bug C1 fix Sprint 2).

#### Body

```json
{ "status": "ACTIVE" }   // o "COMPLETED", "CANCELLED"
```

#### Response 200

Session updated con `startTime`/`endTime` popolato se applicabile.

#### Errori specifici

| Code | Trigger |
|------|---------|
| `400` | Status non valido |
| `403` | `FORBIDDEN` — non sei il creator |
| `404` | Sessione non trovata |

---

### `DELETE /api/v1/sessions/:id`

Elimina sessione. **Solo creator**. Rimuove anche per tutti i partecipanti.

| Campo | Tipo | Note |
|-------|------|------|
| **Response 200** | JSON | `{ message: "Sessione eliminata" }` |
| **Response 403** | JSON | `FORBIDDEN` |

---

## 5. Meteo (`/weather`)

### `GET /weather/locations/nearby`

Stazioni meteo vicine a una coordinata.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | — | Pubblico (dati meteo non sensibili) |
| **Query** | `lon`, `lat`, `maxDistance?` (default 50000m), `type?` (`town`/`poi`), `limit?` (default 5) |

#### Esempio

```http
GET /weather/locations/nearby?lon=11.35&lat=46.50&type=town&limit=1
```

#### Response 200

```json
{
  "count": 1,
  "results": [
    {
      "externalId": "5d9e12bb-...",
      "type": "town",
      "name": "Bolzano",
      "elevation": 262,
      "coordinates": [11.354, 46.499],
      "distanceMeters": 145
    }
  ]
}
```

---

### `GET /weather/locations/search?q=Bolzano`

Cerca stazioni per nome.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | — | Pubblico |
| **Query** | `q` (min 2 char), `type?`, `limit?` |

---

### `GET /weather/forecast/:externalId`

Forecast 3h (16 slot, prossime 48h) + 24h (7 giorni).

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | — | Pubblico |
| **Query** | `forceRefresh?` (boolean, default false) |
| **Param** | `externalId` UUID v4 |

#### Response 200

```json
{
  "location": {
    "externalId": "...",
    "type": "town",
    "name": "Bolzano",
    "elevation": 262,
    "coordinates": [11.354, 46.499]
  },
  "referenceTown": { "externalId": "...", "name": "Bolzano" },
  "meta": { "fromCache": true, "fetchedAt": "2026-05-17T08:30:00Z" },
  "forecast3h": [
    {
      "validFrom": "2026-05-17T09:00:00Z",
      "validTo": "2026-05-17T12:00:00Z",
      "temperature": 18.5,
      "rainProbability": 20,
      "windSpeed": 12,
      "windDirection": 270,
      "skyCondition": "3"
    },
    // ... 15 altri slot
  ],
  "forecast24h": [
    {
      "validFrom": "2026-05-17T00:00:00Z",
      "validTo": "2026-05-18T00:00:00Z",
      "temperatureMin": 12,
      "temperatureMax": 22,
      "rainProbability": 30,
      "skyCondition": "4"
    },
    // ... 6 altri giorni
  ]
}
```

---

### `POST /weather/forecast/:externalId/refresh`

Forza il refresh del forecast (ignora cache).

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | 🔐 JWT + role=admin | Post-fix C2 (17/05) |
| **Response 200** | JSON | `{ message, fetchedAt, slotsCount }` |
| **Response 403** | JSON | Solo admin |

---

### `POST /weather/seed`

Popola il DB con towns + POI da API TINIA. Da chiamare al primo avvio.

| Campo | Tipo | Note |
|-------|------|------|
| **Auth** | 🔐 JWT + role=admin | Post-fix C2 (17/05) |
| **Response 200** | JSON | `{ message: "Seed completato", inserted, updated, errors }` |

---

## 6. Errori comuni & troubleshooting

| Sintomo | Causa probabile | Fix |
|---------|-----------------|-----|
| `401 Invalid token` | JWT scaduto / firma errata | Login di nuovo |
| `403 Forbidden` | Operazione richiede ruolo specifico | Verificare `role` utente |
| `409 USER_ALREADY_IN_SESSION` | Utente ha sessione ACTIVE | Chiudere quella esistente prima |
| `404 Codice invito non valido` | Typo o sessione cancellata | Verificare codice TSM-XXXX (4 caratteri hex) |
| Gson crash `IllegalStateException` lato Kotlin | Backend non popola campi ref | Verificare che il service faccia `populate` symmetric |
| Tracking GPS si interrompe a schermo spento | Manca `ACCESS_BACKGROUND_LOCATION` | Già fixato — verificare manifest + runtime grant (Sprint 2) |
| `500` su `POST /weather/seed` | Manca env var `TINIA_API_URL` o rete | Verificare `.env` |

---

## 7. Endpoint pianificati Sprint 2+

| Method | Path | US | Note |
|--------|------|-----|------|
| `POST` | `/api/v1/emergencies` | US-19 | SOS con firma Ed25519 |
| `PATCH` | `/api/v1/emergencies/:id` | US-19 | Validazione/cancellazione capogruppo |
| `GET` | `/api/v1/sessions/:id/emergencies` | US-19 | Lista SOS sessione |
| `POST` | `/api/v1/sessions/:id/telemetry` | US-21 | Batch GPS upload |
| `GET` | `/api/v1/sessions/:id/positions` | US-22 | (Socket.io per real-time) |
| `GET` | `/api/v1/feed/public` | US-20 | Social feed home |
| `POST` | `/api/v1/users/:id/publicKey` | US-19 | Registrazione chiave pubblica ECC |

---

## 8. Riferimento alternativo

- **Swagger UI**: `http://localhost:3000/api-docs` (interattivo, prova le request inline)
- **Swagger JSON**: `swagger-output.json` nella root del repo
- **Postman collection** (Sprint 2 todo): `docs/tsm.postman_collection.json`

---

*API Reference — Sprint 1 chiuso 17/05/2026. Aggiornare insieme a Swagger ad ogni nuova route o cambio contract.*
