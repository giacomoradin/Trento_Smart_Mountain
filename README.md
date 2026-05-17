# User CRUD — Documentazione API

Questa documentazione descrive le operazioni CRUD esposte sul modello `User` all'interno del backend di **Trento Smart Mountain**.

---

## Base URL

```
/users
```

Registrato in `app.js` tramite:

```js
app.use("/users", userRoutes);
```

---

## Modello User

Il modello Mongoose è definito in `backend/src/models/user.js`.

| Campo | Tipo | Note |
|---|---|---|
| `username` | String | Obbligatorio, univoco |
| `email` | String | Obbligatorio, univoco |
| `passwordHash` | String | Obbligatorio — mai esposto nelle risposte |
| `role` | String | Enum: `groupLeader`, `rifugio`, `admin` — default: `groupLeader` |
| `isVerified` | Boolean | Default: `false` — impostato a `true` dopo verifica email |
| `verificationToken` | String | Token generato alla registrazione per la verifica via SMTP |
| `rifugioDetails` | Object | Opzionale — solo per utenti con ruolo `rifugio` (nome, codice CAI, quota, posti, coordinate) |
| `sessionRoles` | Array | Ruoli per-sessione di escursione (`groupLeader`, `hiker`) |
| `createdAt` | Date | Default: data corrente |
| `mySessions` | Virtual | Populate delle `HikeSession` a cui l'utente partecipa |

---

## Protezione degli endpoint

Gli endpoint sono gestiti da due middleware:

- **`authenticate`** — verifica il JWT nella richiesta (richiesto per tutti gli endpoint protetti).
- **`requireRoles("admin")`** — verifica che l'utente autenticato abbia ruolo `admin`.

---

## Operazioni CRUD

### CREATE — Registrazione utente

```
POST /users
```

**Accesso:** pubblico (nessun JWT richiesto).

**Body (JSON):**

```json
{
  "username": "mario_rossi",
  "email": "mario@example.com",
  "password": "SecurePass123",
  "role": "groupLeader",
  "rifugioDetails": { }
}
```

`rifugioDetails` è opzionale e va incluso solo per utenti con ruolo `rifugio`.

**Comportamento:**
- La password viene hashata con `bcrypt` (salt rounds: 10).
- Viene generato un `verificationToken` casuale (32 byte hex) con il modulo `crypto`.
- L'utente viene salvato con `isVerified: false`.
- Viene inviata un'email di verifica tramite `emailService`.

**Risposta 201:**

```json
{
  "message": "Allocazione completata. Attesa verifica email.",
  "user": {
    "_id": "...",
    "username": "mario_rossi",
    "email": "mario@example.com",
    "role": "groupLeader",
    "isVerified": false
  }
}
```

**Errori:**

| Codice | Causa |
|---|---|
| `409` | Email o username già registrati (indice univoco MongoDB) |
| `500` | Errore generico del server |

---

### READ — Lista di tutti gli utenti

```
GET /users
```

**Accesso:** protetto — richiede JWT valido (`authenticate`).

**Risposta 200:** array di oggetti utente. I campi `passwordHash` e `__v` sono sempre esclusi dalla risposta.

```json
[
  {
    "_id": "...",
    "username": "mario_rossi",
    "email": "mario@example.com",
    "role": "groupLeader",
    "isVerified": true
  }
]
```

**Errori:**

| Codice | Causa |
|---|---|
| `401` | JWT mancante o non valido |
| `500` | Errore generico del server |

---

### READ — Singolo utente per ID

```
GET /users/:id
```

**Accesso:** protetto — richiede JWT valido (`authenticate`).

**Parametro URL:** `:id` — `ObjectId` MongoDB dell'utente.

**Risposta 200:**

```json
{
  "_id": "...",
  "username": "mario_rossi",
  "email": "mario@example.com",
  "role": "groupLeader",
  "isVerified": true,
  "sessionRoles": []
}
```

**Errori:**

| Codice | Causa |
|---|---|
| `400` | Formato ID non valido (`CastError`) |
| `401` | JWT mancante o non valido |
| `404` | Utente non trovato |
| `500` | Errore generico del server |

---

### UPDATE — Aggiornamento utente

```
PUT /users/:id
```

**Accesso:** protetto — richiede JWT valido (`authenticate`) **e** ruolo `admin` (`requireRoles("admin")`).

**Parametro URL:** `:id` — `ObjectId` MongoDB dell'utente.

**Body (JSON):** solo i campi da aggiornare tra quelli consentiti:

```json
{
  "username": "nuovo_nome",
  "email": "nuova@email.com",
  "role": "admin",
  "sessionRoles": []
}
```

Campi aggiornabili: `username`, `email`, `passwordHash`, `role`, `sessionRoles`. Qualsiasi altro campo nel body viene ignorato.

**Comportamento:**
- Usa `findByIdAndUpdate` con `new: true` (restituisce il documento aggiornato) e `runValidators: true` (applica le regole dello schema).
- `passwordHash` e `__v` sono esclusi dalla risposta.

**Risposta 200:**

```json
{
  "_id": "...",
  "username": "nuovo_nome",
  "email": "nuova@email.com",
  "role": "admin"
}
```

**Errori:**

| Codice | Causa |
|---|---|
| `400` | Formato ID non valido (`CastError`) |
| `401` | JWT mancante o non valido |
| `403` | Utente non ha ruolo `admin` |
| `404` | Utente non trovato |
| `409` | Username o email già in uso |
| `500` | Errore generico del server |

---

### DELETE — Eliminazione utente

```
DELETE /users/:id
```

**Accesso:** protetto — richiede JWT valido (`authenticate`) **e** ruolo `admin` (`requireRoles("admin")`).

**Parametro URL:** `:id` — `ObjectId` MongoDB dell'utente.

**Risposta 200:**

```json
{
  "message": "User deleted successfully."
}
```

**Errori:**

| Codice | Causa |
|---|---|
| `400` | Formato ID non valido (`CastError`) |
| `401` | JWT mancante o non valido |
| `403` | Utente non ha ruolo `admin` |
| `404` | Utente non trovato |
| `500` | Errore generico del server |

---

## Riepilogo endpoint

| Metodo | Endpoint | Auth | Ruolo | Operazione |
|---|---|---|---|---|
| `POST` | `/users` | No | — | Crea un nuovo utente |
| `GET` | `/users` | JWT | qualsiasi | Legge tutti gli utenti |
| `GET` | `/users/:id` | JWT | qualsiasi | Legge un utente per ID |
| `PUT` | `/users/:id` | JWT | `admin` | Aggiorna un utente |
| `DELETE` | `/users/:id` | JWT | `admin` | Elimina un utente |

---

## File di riferimento

| File | Ruolo |
|---|---|
| `backend/src/app.js` | Registra il router `/users` nell'app Express |
| `backend/src/routes/userRoutes.js` | Definisce le rotte e applica i middleware di autenticazione/autorizzazione |
| `backend/src/services/userService.js` | Contiene la logica dei controller CRUD |
| `backend/src/models/user.js` | Schema Mongoose del modello `User` |
