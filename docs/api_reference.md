# API Reference — Trento Smart Mountain

> Reference human-readable degli endpoint REST del backend TSM, complementare al Swagger autogenerato (`swagger-output.json` → UI su `/api-docs`).
>
> **Ultima revisione**: 01/06/2026 — Sprint 3: Social completo, Dashboard IoT rifugio, Bacheca rifugi (vedi §"Sprint 3" sotto).
> **Base URL produzione**: `https://trento-smart-mountain-xz7u.onrender.com`
> **Base URL dev**: `http://10.0.2.2:3000` (emulator) / `http://localhost:3000` (Postman dev).

---

## ⚠️ Breaking change refactor 2026-05

Il modello `User` è stato suddiviso in **3 discriminatori Mongoose** (Hiker, Refuge, Admin) su **1 sola collection** `users`. Le route sono state riorganizzate per ruolo:

| Vecchio endpoint                                    | Nuovo endpoint                                                  | Note                                              |
| --------------------------------------------------- | --------------------------------------------------------------- | ------------------------------------------------- |
| `POST /users` con `role=groupLeader`                | `POST /auth/register/hiker`                                     | Body senza campo `role`                           |
| `POST /users` con `role=rifugio` + `rifugioDetails` | `POST /auth/register/refuge`                                    | Campi rifugio **flat** (no più subdocument)       |
| — (non esisteva)                                    | `POST /admin/users`                                             | Solo admin autenticati                            |
| `GET /users`                                        | `GET /admin/users`                                              | Solo admin                                        |
| `GET /users/:id`                                    | `GET /hikers/:id` o `GET /refuges/:id` o `GET /admin/users/:id` | Resta `GET /users/:id` come alias backward-compat |
| `PUT /users/:id` (admin)                            | `PUT /admin/users/:id`                                          | Solo admin                                        |
| `DELETE /users/:id` (admin)                         | `DELETE /admin/users/:id`                                       | Solo admin                                        |
| —                                                   | `GET /refuges`                                                  | Lista pubblica rifugi (nuovo)                     |

Il vecchio `POST /users` è mantenuto come **shim deprecato** che smista internamente (logga un warning). Verrà rimosso in Sprint 3.

---

## 🆕 Sprint 3 — Social completo, IoT rifugio, Bacheca (giugno 2026)

Tutti gli endpoint richiedono JWT. Spec completa auto-generata in `swagger-output.json` (UI `/api-docs`).

### Social — scoperta & grafo

| Metodo | Endpoint                                         | Descrizione                                      |
| ------ | ------------------------------------------------ | ------------------------------------------------ |
| GET    | `/api/v1/users/search?q=&limit=`                 | Ricerca utenti per username (+ `isFollowedByMe`) |
| GET    | `/api/v1/users/:id/followers` · `/:id/following` | Follower / seguiti di un utente                  |
| GET    | `/api/v1/users/:id/hiking-stats`                 | Totali ALL-TIME (km/dislivello/uscite/punti)     |
| GET    | `/api/v1/users/me/weekly-leaderboard`            | Classifica settimanale tra i seguiti             |
| GET    | `/api/v1/users/:id/follow-stats`                 | Ora include `followsViewer` (badge "Ti segue")   |

### Dettaglio Social (Mobile)

| Route         | Componente            | Descrizione                                                                                                                        |
| ------------- | --------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `POST_DETAIL` | `PostDetailScreen.kt` | Dettaglio avanzato attività social con mappa OSMdroid, performance badges (Alpinista, Maratoneta) e timeline degli split ogni 5km. |

### Notifiche

| Metodo | Endpoint                                      | Descrizione                            |
| ------ | --------------------------------------------- | -------------------------------------- |
| GET    | `/api/v1/users/me/notifications?page=`        | Centro notifiche (follow/like/comment) |
| GET    | `/api/v1/users/me/notifications/unread-count` | Conteggio non-letti (badge)            |
| POST   | `/api/v1/users/me/notifications/read`         | Segna tutte come lette                 |

### Privacy profilo

`GET /hikers/:id` applica `profileVisibility`: per viewer non-self, profili `private` (o `friends` senza follow) restituiscono un profilo **limitato** `{ _id, username, isVerified, personalInfo.avatarUrl, restricted: true, visibility }`.

### Rifugio — Dashboard IoT (dati mock)

> Da giugno 2026 le route `/api/v1/refuge/*` sono protette da `requireRoles("rifugio", "admin")` (prima bastava il JWT di qualsiasi utente).

| Metodo | Endpoint                   | Descrizione                                                                                                                                        |
| ------ | -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| GET    | `/api/v1/refuge/dashboard` | Sensori + edge nodes BLE-mesh + passaggi del rifugio loggato (incl. `refuge.avatarUrl`)                                                            |
| PATCH  | `/api/v1/refuge/profile`   | Foto della struttura: `avatarUrl` data URI Base64 (stesso formato/limiti dell'avatar hiker, `""` per rimuoverla) — Joi `refugeProfileUpdateSchema` |

### Rifugio — Rifiuti & Logistica (ADR-002, MVP read-only)

> Porta dentro TSM il modello del _Simulatore Gestione Rifiuti — Rifugi Alpini_ (elaborato OGA ID-22): bilancio di massa stagionale su 6 categorie merceologiche + grigliato, alert di compliance (art. 185-bis D.Lgs. 152/2006: max 30 m³ / 90 giorni di giacenza) e confronto costi tra 4 vettori outbound con `C_kg = (C_fix + c_var·t)/(P·S_t)`. Nessuna persistenza (calcolo puro); auth + rate limit + `requireRoles("rifugio","admin")` + Joi `wasteSimulationSchema`.

| Metodo | Endpoint                        | Descrizione                                                                                                                 |
| ------ | ------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| GET    | `/api/v1/refuge/waste/config`   | Categorie merceologiche, vettori e limiti normativi                                                                         |
| POST   | `/api/v1/refuge/waste/simulate` | Simulazione stagionale: totali pre/post trattamento, breakdown, alert compliance, costi per vettore + vettore più economico |

### Bacheca rifugi

| Metodo | Endpoint                          | Descrizione                                             |
| ------ | --------------------------------- | ------------------------------------------------------- |
| GET    | `/api/v1/board?type=&activeOnly=` | Feed bacheca (tutti gli utenti)                         |
| GET    | `/api/v1/board/mine`              | Post del rifugio loggato                                |
| POST   | `/api/v1/board`                   | Crea (solo rifugio/admin) — Joi `createBoardPostSchema` |
| PATCH  | `/api/v1/board/:id`               | Modifica (autore/admin)                                 |
| DELETE | `/api/v1/board/:id`               | Elimina (autore/admin)                                  |

### Sessione — approvazione partecipanti (giugno 2026)

> `joinSession` ora crea una richiesta **`pending`** (non più ingresso immediato). Il sub-doc `participants[]` ha `status` (`pending`/`accepted`) + `approvedBy`; la sessione ha `removedUserIds[]` (ban locale).

| Metodo | Endpoint                                            | Descrizione                                                         |
| ------ | --------------------------------------------------- | ------------------------------------------------------------------- |
| POST   | `/api/v1/sessions/:id/participants/:userId/approve` | Approva un pending (capogruppo **o** un partecipante già accettato) |
| POST   | `/api/v1/sessions/:id/participants/:userId/reject`  | Rifiuta/rimuove un pending (capogruppo o partecipante accettato)    |
| DELETE | `/api/v1/sessions/:id/participants/:userId`         | Rimuove **definitivamente** + ban (solo capogruppo)                 |

Errori: `403 FORBIDDEN_NOT_MEMBER`/`FORBIDDEN_NOT_LEADER`, `404 PARTICIPANT_NOT_FOUND`, `409 PARTICIPANT_NOT_PENDING`/`PARTICIPANT_BANNED`, `400 CANNOT_REMOVE_CREATOR`.

### Stories (`/api/v1/stories`) — giugno 2026

> Storie effimere (TTL **24h**). `type`: `planned_session` (pre-hike + `inviteCode` per "Unisciti") o `activity` (preview post-hike). Media foto/video **Base64** capped (no object storage). Auth + rate limit + Joi `createStorySchema`.

| Metodo | Endpoint                       | Descrizione                                                                                                                              |
| ------ | ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| POST   | `/api/v1/stories`              | Crea storia (`type`, `sessionId`/`activityId`, `caption`, `media[]`, `overlay`) — 413 se media troppo grande, 403 se ref non autorizzato |
| GET    | `/api/v1/stories/user/:userId` | Storie non scadute di un autore (gate visibilità) + `viewedByMe`                                                                         |
| GET    | `/api/v1/stories/:id`          | Singola storia (deep-link)                                                                                                               |
| POST   | `/api/v1/stories/:id/view`     | Marca come vista (idempotente)                                                                                                           |
| DELETE | `/api/v1/stories/:id`          | Elimina (solo autore)                                                                                                                    |

> Avatar Row: `getSocialRowForUser` espone `status: "story"` + `hasUnviewedStory` quando l'autore ha ≥1 Story non scaduta; il viewer mobile si apre **per autore**.

### Sessione — completamento "Ibrido" + failover (chiusura Sprint 2)

> Ridisegno della logica capogruppo/partecipanti.

| Metodo | Endpoint                               | Descrizione                                                                                                                                                                                                                                                                                                                                                                                    |
| ------ | -------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| PATCH  | `/api/v1/sessions/:id/complete`        | Conclusione **individuale** del chiamante (ADR-001: `participants[].participationState = "finished"` + crediti per-utente). La sessione passa a `COMPLETED` solo quando **tutti gli accettati** sono `finished`/`left` (es. sessione in solitaria → chiusura immediata). Accettata anche a sessione già `COMPLETED` (chi si ferma dopo il force-close del capogruppo non perde crediti/stato). |
| POST   | `/api/v1/sessions/:id/close`           | **Chiusura del capogruppo** (`COMPLETED` per tutti) con **auto-finalize** dei membri ancora `live` (niente sessioni "ghost"). Solo capogruppo/leader effettivo (`403 FORBIDDEN_NOT_LEADER`). È ciò che invoca "Arresta" del leader.                                                                                                                                                            |
| DELETE | `/api/v1/sessions/:id/from-activities` | Nasconde una sessione COMPLETED dalla lista "Le mie attività" del chiamante (`hiddenForUsers[]`, hide per-utente; il documento resta per gli altri). `403 NOT_IN_SESSION` se non membro.                                                                                                                                                                                                       |

### Attività — copia personale di sessione (giugno 2026)

> `POST /api/v1/activities` accetta ora `sourceSessionId` (objectId opzionale): il client la invia per **ogni membro** al termine di una sessione di gruppo, così anche i partecipanti non-creator hanno una _propria_ Activity condivisibile sul feed (prima la share rispondeva `404 ACTIVITY_NOT_FOUND` perché il loro `remoteId` era il sessionId). Garanzie lato service:
>
> - **idempotente** per coppia `(userId, sourceSessionId)`: i retry del SyncManager aggiornano la copia esistente, niente duplicati;
> - **nessun ri-accredito crediti** (già accreditati da `PATCH /sessions/:id/complete`);
> - **esclusa dalle statistiche aggregate** (`GET /sessions/stats`): la HikeSession di origine conta già una volta.

**Failover capogruppo** (nessun nuovo endpoint, logica lato `getLiveLocations`/`postLiveLocation`):

- L'upload di posizione del **leader effettivo** aggiorna `lastHeartbeat` (heartbeat).
- Se il leader è inattivo **>90s** e la sessione è `ACTIVE`, al successivo fetch di live-locations viene eletto leader il **partecipante accettato più anziano** ancora live (posizione <35s) → `currentLeaderId`, `statoFailover=true` + notifica al neo-capogruppo.
- Se il **creator originale** rientra (riprende a inviare posizione) → **reclaim** automatico (`statoFailover=false`).
- `isSessionGroupLeader` riconosce sia il `currentLeaderId` (sostituto) sia il `creatorId`.

> Story `overlay.editorDecor.textFont` (`classic`/`elegant`/`mono`/`handwritten`): font del testo sticker scelto nell'editor, riprodotto identico nel viewer e nell'export bitmap.

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

| Code                        | Quando                                                             |
| --------------------------- | ------------------------------------------------------------------ |
| `200 OK`                    | GET/PATCH/DELETE success                                           |
| `201 Created`               | POST resource creata                                               |
| `400 Bad Request`           | Body malformato o parametri mancanti                               |
| `401 Unauthorized`          | JWT mancante, scaduto o invalido                                   |
| `403 Forbidden`             | JWT valido ma utente non autorizzato (es. non creator)             |
| `404 Not Found`             | Risorsa non esistente                                              |
| `409 Conflict`              | Vincolo violato (es. email già usata, codice invito già esistente) |
| `500 Internal Server Error` | Errore generico non mappato                                        |

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

Login utente. Il campo `email` accetta **email O username** (lookup `$or` lato server):
il login funziona con entrambi. La validazione non impone più il formato email.

| Campo            | Tipo | Note                                                     |
| ---------------- | ---- | -------------------------------------------------------- |
| **Auth**         | —    | Pubblico                                                 |
| **Body**         | JSON | `{ email, password }` — `email` = email **o** username   |
| **Response 200** | JSON | `{ token, accessToken, refreshToken, refreshExpiresAt }` |
| **Response 401** | JSON | `{ message: "Credenziali non valide." }`                 |
| **Response 403** | JSON | `{ message: "Accesso negato. ... verifica SMTP ..." }`   |

#### Esempio request

```http
POST /auth/login
Content-Type: application/json

{
  "email": "mariorossi",   // email OPPURE username
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

| Campo            | Tipo     | Note                                      |
| ---------------- | -------- | ----------------------------------------- |
| **Auth**         | —        | Pubblico (token nel path è la sicurezza)  |
| **Param**        | string   | `token` random 32-byte hex                |
| **Response 302** | redirect | `tsm://auth/verify/<token>?email=<email>` |
| **Response 400** | redirect | `tsm://auth/verify/<token>?error=invalid` |

---

### `POST /auth/forgot-password`

Richiede l'invio dell'email di reset password.

| Campo            | Tipo | Note                                                        |
| ---------------- | ---- | ----------------------------------------------------------- |
| **Auth**         | —    | Pubblico                                                    |
| **Body**         | JSON | `{ email }`                                                 |
| **Response 200** | JSON | `{ message: "Email di reset inviata se l'account esiste" }` |

**Nota**: la response è sempre 200 (anche se l'email non esiste) per non permettere user enumeration.

---

### `GET /auth/reset-password/:token`

Pagina HTML responsive per inserire nuova password (servita inline dal backend).

| Campo            | Tipo | Note                        |
| ---------------- | ---- | --------------------------- |
| **Auth**         | —    | Pubblico                    |
| **Response 200** | HTML | Form responsive             |
| **Response 400** | HTML | "Link scaduto o non valido" |

---

### `POST /auth/reset-password/:token`

Salva la nuova password. Accetta JSON (API) o form-urlencoded (HTML form).

| Campo            | Tipo           | Note                            |
| ---------------- | -------------- | ------------------------------- |
| **Auth**         | —              | Pubblico (token nel path)       |
| **Body JSON**    | `{ password }` | Min 8 caratteri                 |
| **Body form**    | `password=...` | Idem                            |
| **Response 200** | JSON o HTML    | Success page                    |
| **Response 400** | idem           | Token scaduto / password debole |

---

## 3. Registrazione utenti (post-refactor 2026-05)

Tre route distinte, una per ruolo. Tutte pubbliche (no auth richiesta per registrazione hiker/refuge).

### `POST /auth/register/hiker`

Registra un nuovo **escursionista** (groupLeader).

| Campo            | Tipo | Note                                                                                     |
| ---------------- | ---- | ---------------------------------------------------------------------------------------- |
| **Auth**         | —    | Pubblico                                                                                 |
| **Body**         | JSON | `{ username, email, password }`                                                          |
| **Response 201** | JSON | `{ message, user: { _id, username, email, role:"groupLeader", isVerified:false, ... } }` |
| **Response 400** | JSON | Campi obbligatori mancanti o password < 8 caratteri                                      |
| **Response 409** | JSON | Email o username già in uso                                                              |

---

### `POST /auth/register/refuge`

Registra un nuovo **rifugio** con metadati struttura (campi flat).

#### Body

```json
{
  "username": "rifugio.bicchiere@example.com",
  "email": "rifugio.bicchiere@example.com",
  "password": "password123",
  "rifugioName": "Rifugio Bolzano al Bicchiere",
  "caiCode": "B046",
  "quota": 2541,
  "posti": 30,
  "coordinates": "46.6231 11.4583"
}
```

#### Response 201

```json
{
  "message": "Account rifugio creato. Verifica la tua email per attivare l'account.",
  "user": {
    "_id": "...",
    "username": "...",
    "email": "...",
    "role": "rifugio",
    "isVerified": false,
    "rifugioName": "Rifugio Bolzano al Bicchiere",
    "caiCode": "B046",
    "quota": 2541,
    "posti": 30,
    "coordinates": "46.6231 11.4583"
  }
}
```

> **Cambio rispetto al pre-refactor**: i campi rifugio sono ora **flat** sul documento, non più in un subdocument `rifugioDetails`. Il vecchio `POST /users` con formato annidato è ancora accettato come compatibility shim.

---

## 4. Hikers (`/hikers`)

### `GET /hikers/:id`

Profilo escursionista. La response è filtrata dal **privacy gate**
(`utils/userPrivacy.js`): self/admin riceve il documento intero, gli altri
ricevono una versione ridotta che conserva solo i campi pubblici
(`username`, `email`, `isVerified`, `socialCredits`, `nfcStats`, e
`personalInfo.avatarUrl`). I campi privati di `personalInfo` (sex,
birthDate, heightCm, weightKg), `experience`, `preferences`, `weeklyGoals`,
`profileCompletedAt` sono rimossi per i viewer "other".

| Campo            | Tipo   | Note                                                                               |
| ---------------- | ------ | ---------------------------------------------------------------------------------- |
| **Auth**         | 🔐 JWT | —                                                                                  |
| **Response 200** | JSON   | User document filtrato (no `passwordHash`, no token reset, privacy gate applicato) |
| **Response 404** | JSON   | Escursionista non trovato                                                          |

### `PUT /hikers/:id`

Aggiorna profilo escursionista (campi base: `username`, `email`). Autorizzazione: solo self o admin.

| Campo    | Tipo                    | Note                |
| -------- | ----------------------- | ------------------- |
| **Auth** | 🔐 JWT + (self ‖ admin) | 403 altrimenti      |
| **Body** | JSON                    | Campi da aggiornare |

### `PATCH /api/v1/users/me/personal-info`

Aggiorna il sotto-documento `personalInfo` dell'utente loggato (sex, birthDate,
heightCm, weightKg, **avatarUrl**). Update parziale: invia solo i campi che
vuoi cambiare.

| Campo            | Tipo            | Note                                                                                                                                          |
| ---------------- | --------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| **Auth**         | 🔐 JWT          | —                                                                                                                                             |
| **Body**         | JSON            | `{ sex?, birthDate?, heightCm?, weightKg?, avatarUrl? }`                                                                                      |
| **avatarUrl**    | data URI Base64 | Pattern: `^data:image/(jpeg\|jpg\|png\|webp);base64,...$`, max 7 MB string. Body limit Express: 5 MB. Inviare `""` per **rimuovere** la foto. |
| **Response 200** | JSON            | `{ personalInfo: { ... } }` con tutti i campi aggiornati                                                                                      |
| **Response 409** | JSON            | `LockedFieldError` se si tenta di modificare `birthDate` già impostato (anti-cheat)                                                           |
| **Response 422** | JSON            | Joi validation fallita (pattern data URI non rispettato o size eccessiva)                                                                     |

**NB**: anche `experience.caiLevel` è anti-cheat. Per modificarlo va usato
`PATCH /api/v1/users/me/experience`, che applica lo stesso lock pattern.

---

## 5. Refuges (`/refuges`)

### `GET /refuges`

Lista pubblica di tutti i rifugi registrati (dati pubblici: nome, CAI, quota, posti, coordinate).

| Campo            | Tipo | Note                                       |
| ---------------- | ---- | ------------------------------------------ |
| **Auth**         | —    | Pubblico                                   |
| **Response 200** | JSON | Array di rifugi ordinato per `rifugioName` |

### `GET /refuges/:id`

Dettaglio rifugio.

| Campo    | Tipo   | Note |
| -------- | ------ | ---- |
| **Auth** | 🔐 JWT | —    |

### `PUT /refuges/:id`

Aggiorna metadati struttura (`username`, `email`, `rifugioName`, `caiCode`, `quota`, `posti`, `coordinates`).

| Campo    | Tipo   | Note |
| -------- | ------ | ---- |
| **Auth** | 🔐 JWT | —    |

---

## 6. Admin (`/admin`)

Tutte le route richiedono **JWT + role=admin**.

| Method   | Path               | Note                                                                             |
| -------- | ------------------ | -------------------------------------------------------------------------------- |
| `POST`   | `/admin/users`     | Crea un nuovo admin (`isVerified:true` automatico)                               |
| `GET`    | `/admin/users`     | Lista tutti gli utenti (qualunque ruolo)                                         |
| `GET`    | `/admin/users/:id` | Dettaglio qualsiasi utente                                                       |
| `PUT`    | `/admin/users/:id` | Aggiorna qualsiasi utente (incluso `role`, `isVerified` e tutti i campi rifugio) |
| `DELETE` | `/admin/users/:id` | Elimina qualsiasi utente                                                         |

---

## 7. Compatibility shim `/users` (deprecato)

### `POST /users`

Registrazione nuovo utente o rifugio.

| Campo    | Tipo | Note       |
| -------- | ---- | ---------- |
| **Auth** | —    | Pubblico   |
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

| Campo            | Tipo   | Note                                 |
| ---------------- | ------ | ------------------------------------ |
| **Auth**         | 🔐 JWT | —                                    |
| **Response 200** | JSON   | User document (senza `passwordHash`) |
| **Response 404** | JSON   | Utente non trovato                   |

---

### `PUT /users/:id`

Aggiorna utente. Solo admin.

| Campo            | Tipo                | Note                    |
| ---------------- | ------------------- | ----------------------- |
| **Auth**         | 🔐 JWT + role=admin | `requireRoles("admin")` |
| **Body**         | JSON                | Campi da aggiornare     |
| **Response 200** | JSON                | User aggiornato         |
| **Response 403** | JSON                | Solo admin              |

---

### `DELETE /users/:id`

Elimina utente. Solo admin.

| Campo            | Tipo                | Note                              |
| ---------------- | ------------------- | --------------------------------- |
| **Auth**         | 🔐 JWT + role=admin | —                                 |
| **Response 200** | JSON                | `{ message: "Utente eliminato" }` |
| **Response 403** | JSON                | Solo admin                        |

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

| Code  | Trigger                                                   |
| ----- | --------------------------------------------------------- |
| `400` | `routeDetails.name` mancante                              |
| `409` | `USER_ALREADY_IN_SESSION` — utente ha già sessione ACTIVE |

---

### `GET /api/v1/sessions/my`

Lista sessioni dell'utente loggato (sia come creator sia come participant).

| Campo            | Tipo | Note                                                       |
| ---------------- | ---- | ---------------------------------------------------------- |
| **Response 200** | JSON | `[ Session, Session, ... ]` ordinato per `meetingDate ASC` |

---

### `GET /api/v1/sessions/:id`

Dettaglio sessione con populate completo.

| Campo            | Tipo | Note                            |
| ---------------- | ---- | ------------------------------- |
| **Response 200** | JSON | SessionResponse fully populated |
| **Response 404** | JSON | Sessione non trovata            |

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

| Code  | Trigger                                                                   |
| ----- | ------------------------------------------------------------------------- |
| `400` | `inviteCode` mancante                                                     |
| `404` | Codice non valido                                                         |
| `409` | `USER_ALREADY_IN_SESSION` / `SESSION_NOT_JOINABLE` / `ALREADY_IN_SESSION` |

---

### `POST /api/v1/sessions/:id/leave`

Abbandona sessione. NON disponibile per il creator (deve usare DELETE).

| Campo            | Tipo | Note                         |
| ---------------- | ---- | ---------------------------- |
| **Response 200** | JSON | `{ message: "Abbandonata" }` |
| **Response 403** | JSON | `CREATOR_CANNOT_LEAVE`       |

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
{ "status": "ACTIVE" } // o "COMPLETED", "CANCELLED"
```

#### Response 200

Session updated con `startTime`/`endTime` popolato se applicabile.

#### Errori specifici

| Code  | Trigger                          |
| ----- | -------------------------------- |
| `400` | Status non valido                |
| `403` | `FORBIDDEN` — non sei il creator |
| `404` | Sessione non trovata             |

---

### `DELETE /api/v1/sessions/:id`

Elimina sessione. **Solo creator**. Rimuove anche per tutti i partecipanti.

| Campo            | Tipo | Note                                |
| ---------------- | ---- | ----------------------------------- |
| **Response 200** | JSON | `{ message: "Sessione eliminata" }` |
| **Response 403** | JSON | `FORBIDDEN`                         |

---

### `PATCH /api/v1/sessions/:id/complete`

Termina la sessione e persiste le statistiche reali del tracking. Chiamato dal mobile (`RegistraViewModel.confirmStopTracking`) al termine di una sessione live.

| Campo            | Tipo   | Note                                                                                                                                     |
| ---------------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| **Auth**         | 🔐 JWT | Partecipante o creator                                                                                                                   |
| **Body**         | JSON   | `{ actualStats?: { movingSeconds, totalSeconds, distanceMeters, elevationGainM, finalPoints?, estimatedCalories?, currentAltitudeM? } }` |
| **Response 200** | JSON   | Sessione aggiornata con `status: "COMPLETED"`, `endTime`, `actualStats`                                                                  |
| **Response 403** | JSON   | `FORBIDDEN` (non partecipante né creator)                                                                                                |
| **Response 404** | JSON   | Sessione non trovata                                                                                                                     |

Idempotente: secondo `complete` su sessione già COMPLETED restituisce 200 senza re-incrementare i crediti.

---

## 5. Attività libere (`/api/v1/activities`)

Attività personali senza componente di gruppo (no inviteCode, no participants). Owner singolo.

### `POST /api/v1/activities`

Crea una nuova attività libera (chiamato dal mobile dopo lo stop di un tracking senza sessione collegata).

| Campo            | Tipo   | Note                                                                                                                                                                                                                                            |
| ---------------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Auth**         | 🔐 JWT | Owner = `req.user.userId`                                                                                                                                                                                                                       |
| **Body**         | JSON   | `{ name, activityType?, difficultyLevel?, startTimeMs, endTimeMs, actualStats: {movingSeconds, totalSeconds, distanceMeters, elevationGainM, finalPoints?, estimatedCalories?, currentAltitudeM?}, startPoint?, endPoint?, elevationProfile? }` |
| **Response 201** | JSON   | Attività creata, include `_id` Mongo                                                                                                                                                                                                            |
| **Response 422** | JSON   | Validation Joi (es. `actualStats` mancante)                                                                                                                                                                                                     |

#### Esempio body

```json
{
  "name": "Escursione – 24 mag 2026",
  "activityType": "hiking",
  "startTimeMs": 1748000000000,
  "endTimeMs": 1748010000000,
  "actualStats": {
    "movingSeconds": 9000,
    "totalSeconds": 10000,
    "distanceMeters": 5200,
    "elevationGainM": 320,
    "finalPoints": 18,
    "estimatedCalories": 310,
    "currentAltitudeM": 1450
  }
}
```

---

### `GET /api/v1/activities`

Lista delle attività libere dell'utente loggato (no leak: il service filtra per `userId === req.user.userId`).

| Campo            | Tipo   | Note                                              |
| ---------------- | ------ | ------------------------------------------------- |
| **Auth**         | 🔐 JWT | Owner-scoped                                      |
| **Response 200** | JSON   | `Array<Activity>` ordinato per `completedAt` desc |

---

### `GET /api/v1/activities/:id`

Dettaglio di una singola attività libera.

| Campo            | Tipo   | Note                                                     |
| ---------------- | ------ | -------------------------------------------------------- |
| **Auth**         | 🔐 JWT | Owner check via service                                  |
| **Response 200** | JSON   | Activity completa con `actualStats` + `elevationProfile` |
| **Response 403** | JSON   | `FORBIDDEN` (non owner)                                  |
| **Response 404** | JSON   | Non trovata                                              |

---

### `DELETE /api/v1/activities/:id`

Elimina un'attività libera.

| Campo            | Tipo   | Note                                |
| ---------------- | ------ | ----------------------------------- |
| **Auth**         | 🔐 JWT | Solo owner                          |
| **Response 200** | JSON   | `{ message: "Attività eliminata" }` |
| **Response 403** | JSON   | Non owner                           |
| **Response 404** | JSON   | Non trovata                         |

---

## 6. Emergenze SOS (`/api/v1/emergencies`)

Segnalazioni in sessione **ACTIVE** only. Flusso mobile e beacon: `docs/sos_feature.md`.

**Auth:** 🔐 JWT su tutti gli endpoint. Partecipante sessione richiesto.

### Stati `status`

| Valore                | Descrizione                                       |
| --------------------- | ------------------------------------------------- |
| `ACTIVE`              | Nuovo SOS; visibile al capogruppo (e al mittente) |
| `SHARED_WITH_GROUP`   | Condiviso con tutti i partecipanti                |
| `DISMISSED`           | Chiuso dal capogruppo                             |
| `CANCELLED_BY_SENDER` | Annullato dal mittente                            |

### `POST /api/v1/emergencies`

Crea SOS (idempotente). Il server costruisce `profileSnapshot` dall’utente autenticato se omesso nel body.

#### Body

```json
{
  "sessionId": "67d8...",
  "emergencyType": "INJURY",
  "coordinates": { "type": "Point", "coordinates": [11.12, 46.07] },
  "beaconInstanceId": "a1b2c3d4e5f6",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "beaconActive": true,
  "signature": null
}
```

| Campo              | Tipo          | Obbligatorio | Note                                                           |
| ------------------ | ------------- | ------------ | -------------------------------------------------------------- |
| `sessionId`        | ObjectId      | sì           | Sessione `ACTIVE`                                              |
| `emergencyType`    | string        | sì           | `INJURY`, `LOST`, `AVALANCHE`, `WEATHER`, `EQUIPMENT`, `OTHER` |
| `coordinates`      | GeoJSON Point | sì           | **Snapshot** GPS all’invio (non aggiornato dopo)               |
| `beaconInstanceId` | string        | sì           | 12 caratteri hex                                               |
| `idempotencyKey`   | UUID v4       | sì           | Retry sicuro                                                   |
| `beaconActive`     | boolean       | no           | default `true`; `false` = SOS senza beacon BLE                 |
| `signature`        | string        | no           | Riservato Ed25519 (non verificato)                             |

#### Response

| Code  | Body                                                    |
| ----- | ------------------------------------------------------- |
| `201` | Emergenza creata (`status: ACTIVE`)                     |
| `200` | Stessa `idempotencyKey` già usata dallo stesso mittente |
| `403` | `FORBIDDEN`                                             |
| `404` | `SESSION_NOT_FOUND`                                     |
| `409` | `SESSION_NOT_ACTIVE`                                    |
| `422` | Validazione Joi fallita                                 |

---

### `GET /api/v1/emergencies/:id`

Dettaglio singola emergenza (populate `senderUserId`, `sessionId`).

| Code  | Note                            |
| ----- | ------------------------------- |
| `200` | Documento emergenza             |
| `403` | `FORBIDDEN` (regole visibilità) |
| `404` | `EMERGENCY_NOT_FOUND`           |

---

### `PATCH /api/v1/emergencies/:id`

#### Body

```json
{ "action": "share_with_group" }
```

```json
{ "action": "cancel", "reason": "MISTAKE" }
```

| `action`             | Chi        | Effetto                                                               |
| -------------------- | ---------- | --------------------------------------------------------------------- |
| `cancel`             | Mittente   | `CANCELLED_BY_SENDER`; `reason` opzionale: `MISTAKE`, `RESOLVED_SELF` |
| `dismiss`            | Capogruppo | `DISMISSED`                                                           |
| `share_with_group`   | Capogruppo | `ACTIVE` → `SHARED_WITH_GROUP`                                        |
| `unshare_with_group` | Capogruppo | `SHARED_WITH_GROUP` → `ACTIVE`                                        |
| `ack`                | Capogruppo | Imposta `leaderAckAt` (non cambia `status`)                           |

| Code  | Note                                                   |
| ----- | ------------------------------------------------------ |
| `200` | Emergenza aggiornata                                   |
| `403` | `FORBIDDEN`                                            |
| `409` | `EMERGENCY_ALREADY_CLOSED`, `INVALID_STATE_TRANSITION` |

---

### `GET /api/v1/sessions/:id/emergencies`

Lista emergenze **aperte** per la sessione (route su `hikeSessionRoutes`).

#### Response 200

```json
{
  "emergencies": [
    {
      /* Emergency populated */
    }
  ],
  "isGroupLeader": true,
  "hasUnacked": true
}
```

| Campo         | Note                                                                                        |
| ------------- | ------------------------------------------------------------------------------------------- |
| `emergencies` | Capo: `ACTIVE` + `SHARED_WITH_GROUP`. Partecipante: `SHARED_WITH_GROUP` + proprie `ACTIVE`. |
| `hasUnacked`  | `true` se capogruppo e almeno un SOS senza `leaderAckAt`                                    |

---

## 7. Meteo (`/weather`)

### `GET /weather/locations/nearby`

Stazioni meteo vicine a una coordinata.

| Campo     | Tipo                                                                                        | Note                                       |
| --------- | ------------------------------------------------------------------------------------------- | ------------------------------------------ |
| **Auth**  | 🔐 JWT                                                                                      | Da sprint 2: autenticato (no più pubblico) |
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

| Campo     | Tipo                                | Note                     |
| --------- | ----------------------------------- | ------------------------ |
| **Auth**  | 🔐 JWT                              | Da sprint 2: autenticato |
| **Query** | `q` (min 2 char), `type?`, `limit?` |

---

### `GET /weather/forecast/:externalId`

Forecast 3h (16 slot, prossime 48h) + 24h (7 giorni).

| Campo     | Tipo                                     | Note                     |
| --------- | ---------------------------------------- | ------------------------ |
| **Auth**  | 🔐 JWT                                   | Da sprint 2: autenticato |
| **Query** | `forceRefresh?` (boolean, default false) |
| **Param** | `externalId` UUID v4                     |

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
    }
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
    }
    // ... 6 altri giorni
  ]
}
```

---

### `POST /weather/forecast/:externalId/refresh`

Forza il refresh del forecast (ignora cache).

| Campo            | Tipo                | Note                                 |
| ---------------- | ------------------- | ------------------------------------ |
| **Auth**         | 🔐 JWT + role=admin | Post-fix C2 (17/05)                  |
| **Response 200** | JSON                | `{ message, fetchedAt, slotsCount }` |
| **Response 403** | JSON                | Solo admin                           |

---

### `POST /weather/seed`

Popola il DB con towns + POI da API TINIA. Da chiamare al primo avvio.

| Campo            | Tipo                | Note                                                        |
| ---------------- | ------------------- | ----------------------------------------------------------- |
| **Auth**         | 🔐 JWT + role=admin | Post-fix C2 (17/05)                                         |
| **Response 200** | JSON                | `{ message: "Seed completato", inserted, updated, errors }` |

---

## 8. Errori comuni & troubleshooting

| Sintomo                                        | Causa probabile                      | Fix                                                         |
| ---------------------------------------------- | ------------------------------------ | ----------------------------------------------------------- |
| `401 Invalid token`                            | JWT scaduto / firma errata           | Login di nuovo                                              |
| `403 Forbidden`                                | Operazione richiede ruolo specifico  | Verificare `role` utente                                    |
| `409 USER_ALREADY_IN_SESSION`                  | Utente ha sessione ACTIVE            | Chiudere quella esistente prima                             |
| `404 Codice invito non valido`                 | Typo o sessione cancellata           | Verificare codice TSM-XXXX (4 caratteri hex)                |
| Gson crash `IllegalStateException` lato Kotlin | Backend non popola campi ref         | Verificare che il service faccia `populate` symmetric       |
| Tracking GPS si interrompe a schermo spento    | Manca `ACCESS_BACKGROUND_LOCATION`   | Già fixato — verificare manifest + runtime grant (Sprint 2) |
| `500` su `POST /weather/seed`                  | Manca env var `TINIA_API_URL` o rete | Verificare `.env`                                           |

---

## 9. Account & Profilo v2 (`/api/v1/users/me`)

Tutti gli endpoint richiedono **🔐 JWT** + rate limit `authenticatedLimiter`.

| Method   | Path                                | Body                                                   | Risposta                                   | Note                                                                                         |
| -------- | ----------------------------------- | ------------------------------------------------------ | ------------------------------------------ | -------------------------------------------------------------------------------------------- |
| `PATCH`  | `/api/v1/users/me`                  | `{ username?, email? }`                                | `{ user, requiresEmailVerification }`      | Cambio email richiede nuova verifica                                                         |
| `POST`   | `/api/v1/users/me/change-password`  | `{ oldPassword, newPassword }`                         | `{ message }`                              | 401 se oldPassword errata                                                                    |
| `DELETE` | `/api/v1/users/me`                  | `{ password }`                                         | `{ message }`                              | Leadership transfer per sessioni ACTIVE/PLANNED; cascade su transactions/scans/quiz/activity |
| `PATCH`  | `/api/v1/users/me/personal-info`    | `{ sex?, birthDate?, heightCm?, weightKg? }`           | `{ personalInfo }`                         | **409** se `birthDate` già impostato (anti-cheat)                                            |
| `PATCH`  | `/api/v1/users/me/experience`       | `{ caiLevel?, baselineFitness?, weeklyTrainingFreq? }` | `{ experience }`                           | **409** se `caiLevel` già impostato (anti-cheat)                                             |
| `PATCH`  | `/api/v1/users/me/preferences`      | `{ units?, language?, notifications?, privacy? }`      | `{ preferences }`                          | F12: mobile sync `PreferencesHolder`                                                         |
| `PATCH`  | `/api/v1/users/me/goals`            | `{ km?, elevM?, count? }`                              | `{ weeklyGoals }`                          | `.min(1)`: body con almeno 1 campo                                                           |
| `GET`    | `/api/v1/users/me/weekly-stats`     | —                                                      | `{ weekStart, weekEnd, km, elevM, count }` | Settimana ISO corrente                                                                       |
| `POST`   | `/api/v1/users/me/profile-complete` | —                                                      | `{ profileCompletedAt }`                   | Idempotente (timestamp originale preservato)                                                 |

### Esempio: anti-cheat 409 su campo lockato

```http
PATCH /api/v1/users/me/experience
Authorization: Bearer <jwt>
Content-Type: application/json

{ "caiLevel": "T" }
```

Se `caiLevel` era stato precedentemente impostato a `EE`:

```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "message": "Il campo \"caiLevel\" non è modificabile dopo la prima impostazione.",
  "field": "caiLevel"
}
```

Il client mobile (ProfileV2ViewModel) intercetta il `message` e lo mostra
nella sezione `sectionError`.

---

## 10. Endpoint pianificati Sprint 2+

| Method  | Path                               | US    | Note                                 |
| ------- | ---------------------------------- | ----- | ------------------------------------ |
| `POST`  | `/api/v1/emergencies`              | US-19 | SOS con firma Ed25519                |
| `PATCH` | `/api/v1/emergencies/:id`          | US-19 | Validazione/cancellazione capogruppo |
| `GET`   | `/api/v1/sessions/:id/emergencies` | US-19 | Lista SOS sessione                   |
| `POST`  | `/api/v1/sessions/:id/telemetry`   | US-21 | Batch GPS upload                     |
| `GET`   | `/api/v1/sessions/:id/positions`   | US-22 | (Socket.io per real-time)            |
| `GET`   | `/api/v1/feed/public`              | US-20 | Social feed home                     |
| `POST`  | `/api/v1/users/:id/publicKey`      | US-19 | Registrazione chiave pubblica ECC    |

---

## 11. Riferimento alternativo

- **Swagger UI**: `http://localhost:3000/api-docs` (interattivo, prova le request inline)
- **Swagger JSON**: `swagger-output.json` nella root del repo
- **Postman collection** (Sprint 2 todo): `docs/tsm.postman_collection.json`

---

_API Reference — Sprint 1 chiuso 17/05/2026. Sprint 2: profilo v2 + social/badge/quiz/NFC + anti-cheat (aggiornato 26/05/2026). Aggiornare insieme a Swagger ad ogni nuova route o cambio contract._
