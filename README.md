# Autenticazione & JWT — Documentazione

Documentazione del sistema di login, verifica email, gestione token JWT e reset password nel backend **Trento Smart Mountain**.

---

## Panoramica

Il flusso di autenticazione si articola in tre macro-aree:

1. **Registrazione + verifica email** — l'utente si registra tramite `POST /users` (gestita da `userService`), riceve un'email con un token di verifica e lo conferma via deep link.
2. **Login e JWT** — l'utente verifica le credenziali e ottiene un token JWT da usare nelle richieste successive.
3. **Reset password** — flusso autonomo basato su un token temporaneo inviato via email.

---

## Registrazione in `app.js`

```js
app.use("/auth", authRoutes);
```

Tutte le rotte di autenticazione sono montate sul prefisso `/auth`. Nessun middleware globale è applicato — gli endpoint sono tutti pubblici per definizione.

---

## Rotte — `authRoutes.js`

```js
router.post("/login",                    loginUser);
router.get("/verify/:token",             verifyEmail);
router.post("/forgot-password",          forgotPassword);
router.get("/reset-password/:token",     getResetPasswordForm);
router.post("/reset-password/:token",    resetPassword);
```

| Metodo | Endpoint | Handler | Descrizione |
|---|---|---|---|
| `POST` | `/auth/login` | `loginUser` | Verifica credenziali e restituisce il JWT |
| `GET` | `/auth/verify/:token` | `verifyEmail` | Conferma l'email e rilascia un JWT via deep link |
| `POST` | `/auth/forgot-password` | `forgotPassword` | Avvia il flusso di reset password |
| `GET` | `/auth/reset-password/:token` | `getResetPasswordForm` | Serve il form HTML per la nuova password |
| `POST` | `/auth/reset-password/:token` | `resetPassword` | Salva la nuova password hashata |

---

## Modello User — campi rilevanti per l'auth

Dal modello `models/user.js`:

| Campo | Tipo | Ruolo nell'autenticazione |
|---|---|---|
| `email` | String, univoco | Identificatore per il login |
| `passwordHash` | String | Hash bcrypt della password (mai esposto nelle risposte) |
| `role` | `groupLeader` \| `rifugio` \| `admin` | Incluso nel payload JWT |
| `isVerified` | Boolean | Il login è bloccato finché è `false` |
| `verificationToken` | String | Token one-time per la verifica email (rimosso dopo l'uso) |
| `passwordResetToken` | String | Token one-time per il reset password (rimosso dopo l'uso) |
| `passwordResetExpires` | Date | Scadenza del token di reset (1 ora) |

---

## Payload JWT

In entrambi i punti di emissione (login e verifica email) il token viene firmato con:

```js
jwt.sign(
  { userId: user._id, role: user.role },
  process.env.JWT_SECRET,
  { expiresIn: process.env.JWT_EXPIRES_IN || "1d" }
)
```

| Campo payload | Valore |
|---|---|
| `userId` | `_id` MongoDB dell'utente |
| `role` | Ruolo dell'utente (`groupLeader`, `rifugio`, `admin`) |
| Scadenza | Variabile d'ambiente `JWT_EXPIRES_IN`, default `1d` |

Il JWT viene verificato dal middleware `authenticate` in `authMiddleware.js` su ogni endpoint protetto, che attacca il payload decodificato a `req.user`.

---

## Flusso login — `loginUser`

```
POST /auth/login
Body: { email, password }
```

**Passi interni:**

1. Cerca l'utente per `email` nel database.
2. Confronta la password in chiaro con `user.passwordHash` usando `bcrypt.compare`.
3. Controlla che `user.isVerified === true` — se non verificato, blocca con `403`.
4. Genera e restituisce il JWT.

**Risposte:**

| Codice | Causa |
|---|---|
| `200` | Login riuscito — body: `{ token: "..." }` |
| `401` | Email non trovata o password errata (messaggio generico per non esporre quale campo è sbagliato) |
| `403` | Utente registrato ma email non ancora verificata |
| `500` | Errore generico del server |

---

## Flusso verifica email — `verifyEmail`

```
GET /auth/verify/:token
```

Questo endpoint è il destinatario del link inviato via SMTP al momento della registrazione.

**Passi interni:**

1. Cerca l'utente con `verificationToken === :token`.
2. Imposta `isVerified = true` e cancella `verificationToken` dal documento.
3. Genera un JWT (stessa struttura del login).
4. Reindirizza l'utente all'app mobile via deep link: `tsm://auth/success?jwt=<token>`.

**Redirect:**

| Esito | Destinazione |
|---|---|
| Verifica riuscita | `tsm://auth/success?jwt=<token>` |
| Token non valido o già usato | `tsm://auth/error?message=token_invalido_o_scaduto` |
| Errore server | `tsm://auth/error?message=errore_server_interno` |

Il JWT viene passato direttamente nel query string del deep link così l'app mobile può salvarlo localmente senza un ulteriore login.

---

## Flusso reset password

### 1. Richiesta reset — `forgotPassword`

```
POST /auth/forgot-password
Body: { email }
```

**Passi interni:**

1. Cerca l'utente per email.
2. Se trovato, genera un token casuale con `crypto.randomBytes(32)` e lo salva su `passwordResetToken`. Imposta `passwordResetExpires` a **1 ora** dalla richiesta (`Date.now() + 60 * 60 * 1000`).
3. Invia l'email con il link di reset tramite `emailService`.
4. Risponde sempre con `200` e un messaggio generico — anche se l'email non esiste nel database — per prevenire la **user enumeration**.

**Risposta:**

```json
{ "message": "Se l'indirizzo è registrato, riceverai un link per il reset." }
```

### 2. Form di reset — `getResetPasswordForm`

```
GET /auth/reset-password/:token
```

Serve una pagina HTML con un form per inserire e confermare la nuova password. Il form esegue una `POST` sullo stesso URL. Usato principalmente da client non-mobile (browser).

### 3. Salvataggio nuova password — `resetPassword`

```
POST /auth/reset-password/:token
Body (form o JSON): { password, confirmPassword }
```

**Passi interni:**

1. Verifica che `password` e `confirmPassword` coincidano (solo se la richiesta non è JSON).
2. Controlla che la password abbia almeno 8 caratteri.
3. Cerca l'utente con `passwordResetToken === :token` **e** `passwordResetExpires > now` — il token scaduto viene rifiutato.
4. Hasha la nuova password con `bcrypt` (salt rounds: 10) e la salva su `passwordHash`.
5. Cancella `passwordResetToken` e `passwordResetExpires` dal documento.

**Doppia modalità di risposta** — l'handler rileva il `Content-Type` della richiesta (`req.is('application/json')`) e risponde in JSON per le chiamate API o in HTML per le submission del form browser.

| Codice | Causa |
|---|---|
| `200` | Password aggiornata con successo |
| `400` | Password troppo corta, non coincidente, o token non valido/scaduto |
| `500` | Errore generico del server |

---

## Variabili d'ambiente richieste

| Variabile | Utilizzo |
|---|---|
| `JWT_SECRET` | Chiave di firma e verifica del JWT |
| `JWT_EXPIRES_IN` | Durata del JWT (es. `"1d"`, `"7d"`) — default `"1d"` se non impostata |

---

## File di riferimento

| File | Ruolo |
|---|---|
| `backend/src/routes/authRoutes.js` | Definisce le rotte pubbliche `/auth` |
| `backend/src/services/authService.js` | Logica di login, verifica email, reset password e generazione JWT |
| `backend/src/models/user.js` | Campi `isVerified`, `verificationToken`, `passwordResetToken`, `passwordResetExpires`, `role` |
| `backend/src/app.js` | Monta `authRoutes` su `/auth` |
| `backend/src/middleware/authMiddleware.js` | Verifica il JWT su ogni endpoint protetto (fuori scope di questo documento) |
