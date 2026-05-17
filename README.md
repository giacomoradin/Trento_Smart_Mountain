# Gestione Sessione Escursione — Documentazione API

Documentazione delle operazioni sulla risorsa `HikeSession` nel backend **Trento Smart Mountain**.

---

## Base URL

```
/api/v1/sessions
```

Registrato in `app.js` tramite:

```js
app.use("/api/v1/sessions", hikeSessionRoutes);
```

Tutti gli endpoint richiedono autenticazione JWT — `authenticate` è applicato a livello di router (`router.use(authenticate)`).

---

## Modello HikeSession

Definito in `backend/src/models/hikeSession.js`.

### Campi principali

| Campo | Tipo | Note |
|---|---|---|
| `creatorId` | ObjectId → User | Chi ha creato la sessione. Diventa automaticamente `groupLeader`. |
| `inviteCode` | String | Codice alfanumerico univoco nel formato `TSM-XXXX` — generato automaticamente, **mai modificabile**. |
| `status` | String | Enum: `PLANNED`, `ACTIVE`, `COMPLETED`, `CANCELLED`. Default: `PLANNED`. |
| `participants` | Array | Lista dei partecipanti con `userId`, `role` (`groupLeader` o `hiker`) e `joinedAt`. |
| `startTime` | Date | Popolato automaticamente quando lo status diventa `ACTIVE`. |
| `endTime` | Date | Popolato automaticamente quando lo status diventa `COMPLETED`. |
| `createdAt` | Date | Timestamp di creazione del documento. |

### Dettagli percorso — `routeDetails`

| Campo | Tipo | Note |
|---|---|---|
| `name` | String | Obbligatorio. |
| `difficultyLevel` | String | Enum CAI: `T`, `E`, `EE`, `EEA`. Default: `E`. |
| `startPoint` / `endPoint` | GeoJSON Point | Opzionali — popolati solo se viene importato un file GPX. L'indice `2dsphere` è `sparse: true` per escludere documenti senza coordinate. |
| `elevationGain` | Number | Dislivello in metri. |

### Dati tracciato GPX — `gpxStats` (opzionale)

| Campo | Tipo | Note |
|---|---|---|
| `distanceKm` | Number | Distanza totale del percorso. |
| `elevationGainM` | Number | Dislivello positivo totale. |
| `trackPoints` | Number | Numero di punti nel tracciato. |
| `elevationProfile` | Array di Number | Profilo altimetrico campionato (max 50 punti) per il rendering del grafico nell'app. |
| `estimatedPoints` | Number | Punteggio stimato col modello CAI in fase di pianificazione. Sostituito dal punteggio finale al `COMPLETED`. |

### Failover leadership

| Campo | Tipo | Note |
|---|---|---|
| `statoFailover` | Boolean | `true` se il `groupLeader` è inattivo e la leadership è passata a un altro partecipante. |
| `lastHeartbeat` | Date | Timestamp dell'ultimo segnale di vita ricevuto dal `groupLeader`. |

---

## Ciclo di vita di una sessione

```
PLANNED ──► ACTIVE ──► COMPLETED
   │                       
   └──────────────────► CANCELLED
```

| Transizione | Cosa succede internamente |
|---|---|
| `PLANNED → ACTIVE` | `startTime` viene impostato alla data/ora corrente |
| `ACTIVE → COMPLETED` | `endTime` viene impostato alla data/ora corrente |

Solo il creator può cambiare lo stato — verificato nel service confrontando `session.creatorId` con `req.user.userId`.

---

## Vincolo sessioni attive (OCL D2 §4)

Un utente può partecipare a più sessioni in stato `PLANNED` contemporaneamente, ma può essere attivo in **una sola sessione `ACTIVE` alla volta**. Il controllo viene eseguito prima della creazione e del join:

```js
// Blocca solo se esiste una sessione ACTIVE con l'utente tra i partecipanti
HikeSession.findOne({ "participants.userId": userId, status: "ACTIVE" })
```

Se il vincolo è violato, il service lancia `USER_ALREADY_IN_SESSION`.

---

## Codice invito

Il codice invito segue il formato `TSM-XXXX` dove `XXXX` è una stringa esadecimale uppercase di 4 caratteri generata con `crypto.randomBytes(2)`. La generazione è in un ciclo di retry che garantisce l'unicità nel database prima di salvare la sessione.

---

## Operazioni API

### CREATE — Crea una nuova sessione

```
POST /api/v1/sessions
```

**Body (JSON):**

```json
{
  "routeDetails": {
    "name": "Sentiero della Paganella",
    "difficultyLevel": "E"
  },
  "meetingDate": "2025-07-15",
  "meetingTime": "07:30",
  "meetingLocation": "Parcheggio Andalo",
  "maxParticipants": 10,
  "minExperienceLevel": "E",
  "gpxFileName": "paganella.gpx",
  "gpxStats": { "distanceKm": 12.4, "elevationGainM": 850 }
}
```

`routeDetails.name` è l'unico campo obbligatorio. Tutti gli altri sono opzionali.

**Comportamento:**
- Verifica che l'utente non sia già in una sessione `ACTIVE`.
- Genera un `inviteCode` univoco nel formato `TSM-XXXX`.
- Inserisce il creator nei `participants` con ruolo `groupLeader`.
- Aggiorna `sessionRoles` nel documento User del creator.

**Risposta 201:** oggetto `HikeSession` completo.

**Errori:**

| Codice | Causa |
|---|---|
| `400` | `routeDetails.name` mancante |
| `401` | JWT mancante o non valido |
| `409` | Utente già in una sessione `ACTIVE` |
| `500` | Errore generico del server |

---

### JOIN — Unirsi a una sessione tramite codice invito

```
POST /api/v1/sessions/join
Body: { "inviteCode": "TSM-7A4F" }
```

**Comportamento:**
- Verifica che l'utente non sia già in una sessione `ACTIVE`.
- Cerca la sessione per `inviteCode`.
- Accetta il join solo se la sessione è in stato `PLANNED`.
- Aggiunge l'utente ai `participants` con ruolo `hiker`.
- Aggiorna `sessionRoles` nel documento User.
- Restituisce la sessione con `creatorId` e `participants.userId` popolati (username, email) — necessario per evitare ObjectId raw nella risposta al client Kotlin.

**Errori:**

| Codice | Causa |
|---|---|
| `400` | `inviteCode` mancante nel body |
| `401` | JWT mancante o non valido |
| `404` | Codice invito non valido |
| `409` | Utente già in sessione `ACTIVE`, sessione non più aperta, o già presente nella sessione |
| `500` | Errore generico del server |

---

### READ — Sessioni dell'utente autenticato

```
GET /api/v1/sessions/my
```

Restituisce tutte le sessioni in cui l'utente è creator o partecipante, ordinate per `meetingDate` ascendente.

**Risposta 200:** array di sessioni con `creatorId` e `participants.userId` popolati.

---

### READ — Dettaglio singola sessione

```
GET /api/v1/sessions/:id
```

**Risposta 200:** sessione con `creatorId` e `participants.userId` popolati.

**Errori:**

| Codice | Causa |
|---|---|
| `400` | ID non valido |
| `401` | JWT mancante o non valido |
| `404` | Sessione non trovata |

---

### READ — Statistiche attività

```
GET /api/v1/sessions/stats?year=2025
```

Restituisce le statistiche aggregate delle sessioni `COMPLETED` dell'utente per l'anno specificato (default: anno corrente).

**Risposta 200:**

```json
{
  "year": 2025,
  "totalActivities": 8,
  "totalDistanceKm": 94.3,
  "totalElevationGainM": 5200,
  "totalPoints": 1340,
  "monthlyActivityCount": [0, 0, 1, 2, 1, 2, 1, 1, 0, 0, 0, 0],
  "monthlyAvgDifficulty": [0, 0, 0.5, 0.63, 0.5, 0.75, 0.5, 0.5, 0, 0, 0, 0]
}
```

`monthlyAvgDifficulty` mappa i livelli CAI su scala 0–1: `T=0.25`, `E=0.5`, `EE=0.75`, `EEA=1.0`.

---

### UPDATE — Aggiorna stato sessione

```
PATCH /api/v1/sessions/:id/status
Body: { "status": "ACTIVE" }
```

Valori accettati: `PLANNED`, `ACTIVE`, `COMPLETED`, `CANCELLED`.

**Accesso:** solo il creator della sessione.

**Risposta 200:** sessione aggiornata.

**Errori:**

| Codice | Causa |
|---|---|
| `400` | Valore `status` non valido |
| `401` | JWT mancante o non valido |
| `403` | L'utente non è il creator della sessione |
| `404` | Sessione non trovata |
| `500` | Errore generico del server |

---

### UPDATE — Aggiorna dettagli sessione

```
PATCH /api/v1/sessions/:id
```

**Accesso:** solo il creator della sessione.

**Campi aggiornabili:** `routeDetails.name`, `routeDetails.difficultyLevel`, `meetingDate`, `meetingTime`, `meetingLocation`, `maxParticipants`, `minExperienceLevel`.

`inviteCode` non è mai modificabile.

**Risposta 200:** sessione aggiornata con `creatorId` e `participants.userId` popolati.

**Errori:**

| Codice | Causa |
|---|---|
| `401` | JWT mancante o non valido |
| `403` | L'utente non è il creator della sessione |
| `404` | Sessione non trovata |
| `500` | Errore generico del server |

---

### LEAVE — Abbandona una sessione

```
POST /api/v1/sessions/:id/leave
```

**Accesso:** qualsiasi partecipante **tranne** il creator — il creator non può abbandonare la sessione, può solo eliminarla.

**Risposta 200:** `{ "message": "..." }`

**Errori:**

| Codice | Causa |
|---|---|
| `401` | JWT mancante o non valido |
| `403` | Il creator sta cercando di abbandonare la sessione |
| `404` | Sessione non trovata |
| `500` | Errore generico del server |

---

### DELETE — Elimina una sessione

```
DELETE /api/v1/sessions/:id
```

**Accesso:** solo il creator della sessione.

**Risposta 200:** `{ "message": "Sessione eliminata" }`

**Errori:**

| Codice | Causa |
|---|---|
| `401` | JWT mancante o non valido |
| `403` | L'utente non è il creator della sessione |
| `404` | Sessione non trovata |
| `500` | Errore generico del server |

---

## Riepilogo endpoint

| Metodo | Endpoint | Accesso | Operazione |
|---|---|---|---|
| `POST` | `/api/v1/sessions` | JWT | Crea una nuova sessione |
| `POST` | `/api/v1/sessions/join` | JWT | Unirsi a una sessione via inviteCode |
| `GET` | `/api/v1/sessions/my` | JWT | Sessioni dell'utente autenticato |
| `GET` | `/api/v1/sessions/stats` | JWT | Statistiche attività completate |
| `GET` | `/api/v1/sessions/:id` | JWT | Dettaglio singola sessione |
| `PATCH` | `/api/v1/sessions/:id/status` | JWT + creator | Aggiorna stato sessione |
| `PATCH` | `/api/v1/sessions/:id` | JWT + creator | Aggiorna dettagli sessione |
| `POST` | `/api/v1/sessions/:id/leave` | JWT (non creator) | Abbandona una sessione |
| `DELETE` | `/api/v1/sessions/:id` | JWT + creator | Elimina una sessione |

---

## File di riferimento

| File | Ruolo |
|---|---|
| `backend/src/models/hikeSession.js` | Schema Mongoose della sessione |
| `backend/src/services/hikeSessionService.js` | Logica di business — creazione, join, aggiornamento, eliminazione |
| `backend/src/routes/hikeSessionRoutes.js` | Definizione rotte e middleware `authenticate` globale |
| `backend/src/app.js` | Monta il router su `/api/v1/sessions` |
