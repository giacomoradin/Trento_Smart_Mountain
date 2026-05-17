# RBAC — Role-Based Access Control

Documentazione del sistema di autenticazione e autorizzazione del backend **Trento Smart Mountain**.

---

## Panoramica

Il sistema RBAC è composto da due livelli distinti e separati:

1. **Autenticazione** (`authMiddleware.js`) — verifica che la richiesta porti un JWT valido.
2. **Autorizzazione** (`authorizationMiddleware.js`) — verifica che l'utente autenticato abbia il ruolo richiesto dalla rotta.

I due middleware vengono applicati in sequenza sulle singole rotte o sull'intero router.

---

## Ruoli disponibili

I ruoli sono definiti nello schema Mongoose del modello `User` (`models/user.js`):

| Ruolo | Descrizione |
|---|---|
| `groupLeader` | Utente standard — crea e gestisce sessioni di escursione. Ruolo di default alla registrazione. |
| `rifugio` | Gestore di rifugio — può avere i dettagli del rifugio nel profilo (`rifugioDetails`). |
| `admin` | Amministratore di sistema — accesso completo a tutte le operazioni privilegiate. |

Il ruolo viene assegnato al momento della registrazione tramite il campo `role` nel body della `POST /users`. Se non specificato, viene applicato il default `groupLeader`.

---

## Flusso di autenticazione

### 1. Login e generazione del JWT

Al login (`POST /auth/login`), l'`authService` genera un token JWT firmato con `JWT_SECRET` che include nel payload:

```js
{ userId: user._id, role: user.role }
```

Il token viene restituito al client, che deve includerlo in ogni richiesta protetta nell'header:

```
Authorization: Bearer <token>
```

### 2. Middleware `authenticate`

Definito in `middleware/authMiddleware.js`.

```js
export const authenticate = (req, res, next) => {
  const token = req.headers.authorization?.split(" ")[1]; // "Bearer <token>"

  if (!token) {
    return res.status(401).json({ message: "No token provided." });
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = decoded; // { userId, role } disponibile in tutti i middleware successivi
    next();
  } catch (error) {
    res.status(401).json({ message: "Invalid or expired token." });
  }
};
```

Dopo la verifica, attacca `{ userId, role }` a `req.user`, rendendolo disponibile a tutti i middleware e controller successivi nella catena.

**Risposte in caso di errore:**

| Codice | Causa |
|---|---|
| `401` | Header `Authorization` assente o token mancante |
| `401` | Token non valido o scaduto |

### 3. Middleware `requireRoles`

Definito in `middleware/authorizationMiddleware.js`.

```js
export const requireRoles = (...allowedRoles) => (req, res, next) => {
  if (!req.user || !allowedRoles.includes(req.user.role)) {
    return res.status(403).json({ message: "Forbidden." });
  }
  next();
};
```

È una **higher-order function**: riceve uno o più ruoli consentiti e restituisce un middleware. Legge `req.user.role` (popolato da `authenticate`) e blocca la richiesta se il ruolo non è tra quelli autorizzati.

Deve essere sempre applicato **dopo** `authenticate`, poiché dipende da `req.user`.

**Risposte in caso di errore:**

| Codice | Causa |
|---|---|
| `403` | Utente autenticato ma ruolo non sufficiente |

---

## Applicazione sulle rotte

### `/users` — userRoutes.js

| Metodo | Endpoint | `authenticate` | `requireRoles` | Ruoli ammessi |
|---|---|:---:|:---:|---|
| `POST` | `/users` | ✗ | ✗ | Pubblico |
| `GET` | `/users` | ✓ | ✗ | Qualsiasi ruolo autenticato |
| `GET` | `/users/:id` | ✓ | ✗ | Qualsiasi ruolo autenticato |
| `PUT` | `/users/:id` | ✓ | ✓ | `admin` |
| `DELETE` | `/users/:id` | ✓ | ✓ | `admin` |

### `/auth` — authRoutes.js

Tutte le rotte sono pubbliche — nessun middleware di autenticazione applicato.

| Metodo | Endpoint | Accesso |
|---|---|---|
| `POST` | `/auth/login` | Pubblico |
| `GET` | `/auth/verify/:token` | Pubblico |
| `POST` | `/auth/forgot-password` | Pubblico |
| `GET` | `/auth/reset-password/:token` | Pubblico |
| `POST` | `/auth/reset-password/:token` | Pubblico |

### `/api/v1/sessions` — hikeSessionRoutes.js

`authenticate` è applicato a livello di router (`router.use(authenticate)`), quindi **tutte** le rotte della sessione richiedono JWT. Non viene usato `requireRoles`: l'autorizzazione per-operazione (es. solo il creator può modificare/eliminare la sessione) è gestita internamente dal `hikeSessionService` confrontando `req.user.userId` con il campo `createdBy` della sessione.

| Metodo | Endpoint | `authenticate` | Nota |
|---|---|:---:|---|
| `POST` | `/api/v1/sessions` | ✓ | Qualsiasi utente autenticato |
| `POST` | `/api/v1/sessions/join` | ✓ | Qualsiasi utente autenticato |
| `GET` | `/api/v1/sessions/my` | ✓ | Qualsiasi utente autenticato |
| `GET` | `/api/v1/sessions/stats` | ✓ | Qualsiasi utente autenticato |
| `GET` | `/api/v1/sessions/:id` | ✓ | Qualsiasi utente autenticato |
| `PATCH` | `/api/v1/sessions/:id/status` | ✓ | Solo creator (controllo in service) |
| `PATCH` | `/api/v1/sessions/:id` | ✓ | Solo creator (controllo in service) |
| `POST` | `/api/v1/sessions/:id/leave` | ✓ | Qualsiasi partecipante (escluso creator) |
| `DELETE` | `/api/v1/sessions/:id` | ✓ | Solo creator (controllo in service) |

### `/weather` — weatherRoutes.js

| Metodo | Endpoint | `authenticate` | `requireRoles` | Ruoli ammessi |
|---|---|:---:|:---:|---|
| `GET` | `/weather/locations/search` | ✗ | ✗ | Pubblico |
| `GET` | `/weather/locations/nearby` | ✗ | ✗ | Pubblico |
| `GET` | `/weather/forecast/:externalId` | ✗ | ✗ | Pubblico |
| `POST` | `/weather/forecast/:externalId/refresh` | ✓ | ✓ | `admin` |
| `POST` | `/weather/seed` | ✓ | ✓ | `admin` |

---

## Matrice dei permessi per ruolo

| Operazione | `groupLeader` | `rifugio` | `admin` |
|---|:---:|:---:|:---:|
| Registrarsi | ✓ | ✓ | ✓ |
| Accedere ai propri dati | ✓ | ✓ | ✓ |
| Vedere lista utenti | ✓ | ✓ | ✓ |
| Modificare un utente | ✗ | ✗ | ✓ |
| Eliminare un utente | ✗ | ✗ | ✓ |
| Creare una sessione | ✓ | ✓ | ✓ |
| Unirsi a una sessione | ✓ | ✓ | ✓ |
| Modificare/eliminare la propria sessione | ✓ | ✓ | ✓ |
| Forzare refresh meteo | ✗ | ✗ | ✓ |
| Eseguire seed dei dati meteo | ✗ | ✗ | ✓ |

---

## File di riferimento

| File | Ruolo |
|---|---|
| `backend/src/middleware/authMiddleware.js` | Verifica JWT e popola `req.user` |
| `backend/src/middleware/authorizationMiddleware.js` | Controlla il ruolo su `req.user` |
| `backend/src/models/user.js` | Definisce i ruoli disponibili (`groupLeader`, `rifugio`, `admin`) |
| `backend/src/services/authService.js` | Genera il JWT con payload `{ userId, role }` al login |
| `backend/src/routes/userRoutes.js` | Applica RBAC sulle operazioni CRUD utente |
| `backend/src/routes/hikeSessionRoutes.js` | Applica `authenticate` globale; autorizzazione per-risorsa nel service |
| `backend/src/routes/weatherRoutes.js` | Applica RBAC sulle operazioni admin meteo |
