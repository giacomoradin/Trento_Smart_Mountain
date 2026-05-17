# Swagger — Documentazione API Setup

Documentazione della configurazione e del workflow di generazione della UI Swagger nel backend **Trento Smart Mountain**.

---

## Stack utilizzato

| Pacchetto | Versione | Ruolo |
|---|---|---|
| `swagger-autogen` | `^2.23.7` (devDependency) | Genera `swagger-output.json` analizzando le route e i commenti nel codice |
| `swagger-ui-express` | `^5.0.1` (dependency) | Serve la UI interattiva Swagger all'interno dell'app Express |

---

## File coinvolti

| File | Ruolo |
|---|---|
| `swagger.js` | Script di generazione — definisce la configurazione del documento e avvia `swagger-autogen` |
| `swagger-output.json` | Artefatto generato — documento OpenAPI 3.0 letto a runtime da Express |
| `backend/src/app.js` | Monta la UI Swagger sull'endpoint `/api-docs` |
| `backend/src/services/*.js` | Contengono i commenti `#swagger` che arricchiscono la documentazione |

---

## Configurazione — `swagger.js`

```js
import swaggerAutogen from 'swagger-autogen';

const doc = {
  info: {
    title: 'Trento Smart Mountain API',
    description: 'Documentazione API',
  },
  host: 'localhost:3000',
  schemes: ['http'],
  components: {
    securitySchemes: {
      bearerAuth: {
        type: 'http',
        scheme: 'bearer',
        bearerFormat: 'JWT',
      }
    }
  },
  security: [{ bearerAuth: [] }]
};

const outputFile = './swagger-output.json';
const endpointsFiles = ['./backend/src/app.js'];

const options = {
  openapi: '3.0.0',
  autoHeaders: false, // rimuove il campo "authorization" dagli header automatici
  autoQuery: true,
  autoBody: true
};

swaggerAutogen(options)(outputFile, endpointsFiles, doc);
```

**Punti chiave della configurazione:**

- **OpenAPI 3.0.0** — il documento generato è conforme allo standard OpenAPI 3.
- **Entry point `app.js`** — `swagger-autogen` parte da `app.js` e ne segue tutte le rotte registrate per individuare gli endpoint.
- **`autoHeaders: false`** — disabilita la generazione automatica del parametro `Authorization` come header, poiché la sicurezza è gestita globalmente tramite `bearerAuth`.
- **`autoQuery: true` / `autoBody: true`** — rileva automaticamente i parametri query e il body dagli handler.
- **`security: [{ bearerAuth: [] }]`** — applica il JWT Bearer come schema di sicurezza predefinito su tutti gli endpoint. I singoli endpoint che non richiedono autenticazione lo sovrascrivono con `#swagger.security = []`.

---

## Integrazione in Express — `app.js`

```js
import swaggerUI from "swagger-ui-express";
import { readFileSync } from 'fs';

const swaggerDocument = JSON.parse(
  readFileSync(new URL('../../swagger-output.json', import.meta.url))
);

app.use("/api-docs", swaggerUI.serve, swaggerUI.setup(swaggerDocument));
```

Il file `swagger-output.json` viene letto **a runtime** con `readFileSync` usando `import.meta.url` per risolvere il path relativo correttamente in ESM. La UI è disponibile all'indirizzo:

```
http://localhost:3000/api-docs
```

---

## Commenti `#swagger` nel codice

`swagger-autogen` supporta annotazioni inline scritte come commenti JSDoc all'interno degli handler. Nel progetto sono usati nei file di servizio.

### Direttive utilizzate

| Direttiva | Effetto |
|---|---|
| `#swagger.tags = ['NomeTag']` | Raggruppa l'endpoint sotto un tag nella UI |
| `#swagger.description = '...'` | Aggiunge una descrizione testuale all'operazione |
| `#swagger.security = []` | Sovrascrive la sicurezza globale — rende l'endpoint pubblico (nessun JWT richiesto) |

### Esempio — endpoint pubblico (`userService.js`)

```js
export const createUser = async (req, res) => {
  /* 
     #swagger.tags = ['Auth']
     #swagger.description = 'Registra un nuovo utente e invia l'email di verifica.'
     #swagger.security = []   // <- sovrascrive il bearerAuth globale
  */
  // ...
};
```

### Esempio — endpoint protetto (`userService.js`)

```js
export const getAllUsers = async (req, res) => {
  /* 
     #swagger.tags = ['Users']
     #swagger.description = 'Ottiene la lista di tutti gli utenti.'
  */
  // ...
};
```

### Tag utilizzati nel progetto

| Tag | Endpoint raggruppati |
|---|---|
| `Auth` | Registrazione utente (`POST /users`) |
| `Users` | CRUD utenti (`GET`, `PUT`, `DELETE /users`) |
| `Sessions` | Gestione sessioni di escursione (`/api/v1/sessions`) |

---

## Workflow di generazione

La rigenerazione del documento va eseguita **ogni volta che si aggiungono, modificano o rimuovono endpoint**. Il comando è definito in `package.json`:

```bash
npm run swagger
```

Questo esegue `node swagger.js`, che analizza `app.js` e tutte le rotte collegate, e sovrascrive `swagger-output.json`.

```
swagger.js
   └─► legge app.js
         └─► segue i router registrati (userRoutes, authRoutes, hikeSessionRoutes, weatherRoutes)
               └─► raccoglie endpoint + commenti #swagger
                     └─► scrive swagger-output.json
```

`swagger-output.json` è un file generato — non va modificato a mano e può essere aggiunto al `.gitignore` se si preferisce rigenerarlo in fase di build, oppure committato per averlo disponibile senza dover rieseguire lo script.

---

## Accesso alla UI

Con il server in esecuzione (`npm run dev` o `npm start`):

```
http://localhost:3000/api-docs
```

La UI permette di esplorare tutti gli endpoint, leggere descrizioni e testare le chiamate direttamente dal browser. Per gli endpoint protetti, usare il pulsante **Authorize** in alto a destra e inserire il JWT ottenuto dal login (`POST /auth/login`).

---

## File di riferimento

| File | Ruolo |
|---|---|
| `swagger.js` | Script di generazione (da eseguire con `npm run swagger`) |
| `swagger-output.json` | Documento OpenAPI generato, letto da Express a runtime |
| `backend/src/app.js` | Monta `swagger-ui-express` su `/api-docs` |
| `backend/src/services/userService.js` | Commenti `#swagger` per tag `Auth` e `Users` |
| `backend/src/services/hikeSessionService.js` | Commenti `#swagger` per tag `Sessions` |
