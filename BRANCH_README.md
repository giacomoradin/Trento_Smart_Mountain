# Branch: Implementazione Security Best Practice & Fix Bug Sprint 1

Branch con focus su **sicurezza backend**, **fix dei bug emersi dal primo sprint** di test e **sincronizzazione delle attività** verso il cloud.

---

## Cosa contiene questa branch

### Bug fix (Sprint 1)

| # | Problema | Soluzione |
|---|----------|-----------|
| 1 | Tempi delle escursioni mostravano sempre la stima CAI invece della durata effettiva del file GPX | Parser GPX esteso per leggere i tag `<time>` dei trackpoint; il campo `gpxDurationSec` viene persistito su backend e propagato all'app |
| 2 | Attività locali non sincronizzate con il cloud | Nuovo endpoint `PATCH /sessions/:id/complete` con `actualStats`; SyncManager con backoff incrementale |
| 3 | Sovrapposizione dei testi nel profilo altimetrico (Activity Detail) | ElevationProfileChart riscritto: asse X disegnato dentro al canvas con padding riservato, label allineati LEFT/CENTER/RIGHT |
| 4 | Grafici e storici HOME → Attività: anno errato, bar chart con testo tagliato | Filtro anno esatto (rimosso fallback confusionario), topPadding nel bar chart, empty state contestuale per mese/anno senza attività |
| 5 | Attività avviata da sessione pianificata non veniva salvata | Soglia 50m applicata solo alle attività libere; sessioni di gruppo sempre salvate; dialog "Attività troppo corta" per chiedere conferma |

### Security hardening

**Nuove dipendenze backend**: `helmet`, `joi`, `express-rate-limit`, `express-mongo-sanitize`, `hpp`.

**Rate limiting differenziato** (`backend/src/middleware/rateLimitMiddleware.js`):
- Globale: 300 req / 15 min per IP
- Login: 10 tentativi / 15 min (solo fallimenti contano)
- Registrazione: 5 / ora
- Password reset: 5 / ora
- Utenti autenticati: 1000 req / 15 min (keyed su userId)
- Scritture autenticate: 200 / 15 min

**Validazione schema Joi** (`backend/src/middleware/validationMiddleware.js`):
- Tutti gli endpoint POST/PATCH/DELETE validati con schema dedicato
- Campi sconosciuti rifiutati (no mass-assignment)
- Length limit su ogni campo stringa

**Altri controlli** (`backend/src/middleware/securityMiddleware.js`):
- `helmet` con CSP custom (Swagger UI richiede unsafe-inline)
- CORS allow-list via `ALLOWED_ORIGINS` env (prod strict, dev permissivo)
- `express-mongo-sanitize` contro NoSQL injection
- `hpp` contro HTTP Parameter Pollution
- Body size limit a 100 KB

**Fail-fast all'avvio** (`backend/src/server.js`): il server non parte se `JWT_SECRET` è debole o se mancano le variabili critiche in produzione.

### Activity collection (attività libere)

Nuova collection MongoDB separata da `HikeSession` per le escursioni personali (no gruppo, no lifecycle).

- `POST /api/v1/activities` — crea attività
- `GET /api/v1/activities` — lista personale
- `GET /api/v1/activities/:id` — dettaglio
- `DELETE /api/v1/activities/:id` — elimina (solo proprietario)

Le statistiche annuali (`GET /sessions/stats`) ora aggregano sia le sessioni di gruppo che le attività libere.

### Sync cloud bidirezionale (mobile)

- Upload immediato post-tracking: sessioni via `PATCH /complete`, libere via `POST /activities`
- **SyncManager** (`data/sync/SyncManager.kt`): coroutine loop ogni 60s con backoff per record (1 min → 5 min → 30 min → 1 ora)
- Bottone "Risincronizza" nell'header di *Le Mie Attività* quando ci sono record in sospeso
- Delete cross-device: eliminare un'attività libera la rimuove anche dal backend
- Room v4 con nuovi campi: `retry_count`, `last_retry_at_ms`, `remote_id`

---

## Come testare

### Backend

```bash
# Dipendenze (include le nuove: helmet, joi, express-rate-limit, ecc.)
npm install

# Copia e compila .env
cp .env.example .env
# Imposta almeno JWT_SECRET (≥32 char), MONGO_URI

npm run dev
```

**Test rate limit**:
```bash
# 11 login falliti in rapida successione → HTTP 429 con { error, retryAfter }
for i in {1..11}; do
  curl -s -X POST http://localhost:3000/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"x@x.com","password":"wrong"}' | jq .
done
```

**Test validazione**:
```bash
# Campo sconosciuto → 400
curl -s -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"x@x.com","password":"pass","__proto__":"injected"}' | jq .

# Invite code malformato → 400
curl -s -X POST http://localhost:3000/api/v1/sessions/join \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"inviteCode":"AAAA"}' | jq .
```

**Test Activity libere**:
```bash
curl -s -X POST http://localhost:3000/api/v1/activities \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Test Passeggiatina",
    "startTimeMs": 1716000000000,
    "endTimeMs": 1716003600000,
    "actualStats": {
      "movingSeconds": 3600,
      "totalSeconds": 3600,
      "distanceMeters": 5200,
      "elevationGainM": 180,
      "finalPoints": 72
    }
  }' | jq .
```

### Mobile (Android)

1. `local.properties`: `tsm.api.baseUrl=http://10.0.2.2:3000/`
2. Build: `./gradlew installDebug`
3. Verificare:
   - Tracciamento GPS → stop → dati reali mostrati (no stima CAI)
   - Sessioni di gruppo: avvio → stop → attività salvata + sync cloud
   - *Le Mie Attività*: filtro anno corretto, bar chart leggibile, bottone "Risincronizza" visibile se ci sono pending

---

## File modificati / aggiunti

```
backend/
  src/middleware/
    rateLimitMiddleware.js     ← nuovo
    validationMiddleware.js    ← nuovo
    securityMiddleware.js      ← nuovo
  src/models/
    activity.js                ← nuovo
    hikeSession.js             ← aggiunto actualStats, gpxDurationSec
  src/services/
    activityService.js         ← nuovo
    hikeSessionService.js      ← completeSession, getActivityStats unificato
  src/routes/
    activityRoutes.js          ← nuovo
    authRoutes.js              ← rate limit + Joi
    hikeSessionRoutes.js       ← rate limit + Joi + /complete endpoint
  src/app.js                   ← stack sicurezza integrato
  src/server.js                ← assertEnvironment fail-fast

mobile/
  data/sync/SyncManager.kt    ← nuovo
  data/remote/dto/
    ActivityDto.kt             ← nuovo
    CreateSessionRequest.kt    ← gpxDurationSec
    JoinSessionRequest.kt      ← ActualStats, CompleteSessionRequest
    SessionResponse.kt         ← ActualStatsResponse, gpxDurationSec
  data/local/db/
    CompletedActivityEntity.kt ← retry_count, last_retry_at_ms, remote_id
    CompletedActivityDao.kt    ← bumpRetry, markSynced(id, remoteId)
    TsmDatabase.kt             ← version 4
  viewmodel/
    RegistraViewModel.kt       ← sync immediato, dialog 50m
    ActivityListViewModel.kt   ← sync libere, filtro anno esatto
    ActivityDetailViewModel.kt ← delete remoto attività libere
    SessionPlanViewModel.kt    ← parser <time>
  ui/screens/home/
    ActivityListScreen.kt      ← bar chart fix, empty state, Risincronizza
    ActivityDetailScreen.kt    ← ElevationProfileChart riscritto
  ui/screens/registra/
    RegistraScreen.kt          ← dialog attività corta
  ui/screens/session/
    SessionDetailScreen.kt     ← label durata dinamica (REALE/GPX/CAI)
  TsmApplication.kt            ← SyncManager.start

docs/
  SECURITY.md                  ← nuovo: threat model, OWASP, ACM, deploy checklist
  TSM_PROJECT_STATE.md         ← ricreato e aggiornato

.env.example                   ← nuovo: template variabili con commenti
package.json                   ← 4 nuove dipendenze sicurezza
```

---

## Note

- **Room database v4**: la migrazione usa `fallbackToDestructiveMigration` (modalità dev). Prima del deploy produzione aggiungere migration esplicita.
- **SyncManager**: usa un coroutine loop invece di WorkManager. Funziona finché il processo è in vita; alla riapertura riprende dal backlog.
- **Rate limit**: in-memory, single-instance. Per multi-instance su Render Pro servirebbe uno store Redis condiviso.
