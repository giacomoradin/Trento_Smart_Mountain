# Sprint 2 — Plan & Backlog Refinement

> Piano operativo per lo **Sprint 2** di Trento Smart Mountain, basato sul Backlog Refinement emerso dalla Sprint Review e dall'audit di fine Sprint 1.
>
> **Durata pianificata**: 1 settimana (date da concordare in Sprint Planning meeting).
> **Riferimenti**: `TSM_PROJECT_STATE.md` §10 (audit), §11.3 (refinement), `T6_D3_Ingegneria_Del_Software.md` §Product Backlog Refinement.

---

## 1. Sprint Goal proposto

> _"Chiudere il debito tecnico di Sprint 1 (bug critici già fixati + runtime permission flow, schema migration, pattern Repository), implementare il backend SOS (RF9) e introdurre il sistema di sync offline-to-online (WorkManager store-and-forward) per la telemetria GPS — consolidando le fondamenta prima dell'apertura del fronte real-time/BLE in Sprint 3."_

### Capacità team prevista

| Membro                 | h/settimana stimate    |
| ---------------------- | ---------------------- |
| Federico Cattelan      | ~50h                   |
| Marco Christian Stoica | ~50h                   |
| Giacomo Radin          | ~50h                   |
| **Totale**             | **~150h** (≈ 30-40 SP) |

---

## 2. Sprint 2 Backlog (Product Backlog Refinement)

### Legenda

| Priorità   | Significato                                                     |
| ---------- | --------------------------------------------------------------- |
| **Must**   | Bloccante — deve essere consegnato per chiudere Sprint 2        |
| **Should** | Importante — fortemente desiderato, può scivolare al successivo |
| **Could**  | Nice-to-have — se c'è capacità residua                          |

---

### US-16 — Fix C1 runtime [Must — già parzialmente fixato]

**User Story:** Come **partecipante** voglio poter **avviare il mio tracking GPS quando il Capogruppo avvia la sessione** così da partecipare attivamente all'escursione.

**Stato 17/05:** ✅ Pulsante AVVIA gated al creator (UI fix). Partecipanti vedono chip informativo.

**Da fare Sprint 2:**

1. Quando il creator avvia la sessione → server notifica via polling (LongPoll temporaneo, Socket.io in US-22).
2. UI partecipanti: pulsante "▶ UNISCITI AL TRACKING" appare automaticamente quando `session.status = ACTIVE`.
3. Implementare polling lato client: `LaunchedEffect` con `delay(30_000)` ogni 30s su sessione PLANNED.
4. Refactor `RegistraViewModel.autoStartFromSession()`: separare PATCH creator dal pure-tracking di partecipante.

**Acceptance criteria:**

- TC-07 risulta ✅ PASS completo.
- Partecipante vede "UNISCITI AL TRACKING" entro 30s dall'AVVIA del creator.
- Local tracking parte senza tentare PATCH inutili.

**Volunteer:** Giacomo (UI) + Federico (polling logic)
**Stima:** 5 SP

---

### US-17 — Fix C2 verifica [Must — già fixato, serve test automatizzato]

**User Story:** Come **sistema** voglio che gli endpoint `/weather/*` di amministrazione siano protetti da autenticazione, così da prevenire attacchi DoS economici verso TINIA.

**Stato 17/05:** ✅ `authenticate + requireRoles("admin")` su `POST /seed` e `POST /forecast/:id/refresh`.

**Da fare Sprint 2:**

1. Test Postman automatizzato → collection `tsm_security.postman_collection.json` con asserzioni 401/403.
2. Documentare procedura seed admin in `docs/setup_backend.md`.
3. Creare utente admin di default via script `backend/scripts/createAdmin.js` per il seeding iniziale.

**Acceptance criteria:**

- TC-11 risulta ✅ PASS in CI/Postman runner.
- Esiste un admin user in MongoDB seedato automaticamente al primo `npm run dev`.

**Volunteer:** Marco
**Stima:** 3 SP

---

### US-18 — Fix C3 runtime permission flow [Must]

**User Story:** Come **escursionista** voglio che l'app mi chieda chiaramente il permesso di tracciare la mia posizione anche con schermo spento, così da poter usare il tracking GPS in sicurezza.

**Stato 17/05:** ✅ `ACCESS_BACKGROUND_LOCATION` + `WAKE_LOCK` nel manifest.

**Da fare Sprint 2:**

1. Implementare flusso runtime permission a 2 step per Android 10+:
   - Step A: richiedere `ACCESS_FINE_LOCATION` (foreground) — rationale dialog.
   - Step B: dopo grant Step A, richiedere `ACCESS_BACKGROUND_LOCATION` — su Android 11+ apre Settings system.
2. Nuova schermata `LocationPermissionScreen` con UI educativa (icone + spiegazione testo).
3. Verifica permessi in `RegistraViewModel.startTracking()` prima di partire.
4. Eventuale toast "Permesso background necessario" se l'utente skippa lo Step B.

**Acceptance criteria:**

- TC-08 ✅ PASS completo: 5 minuti di camminata a schermo spento = traccia GPS continua senza gap.
- Su Android 11+ l'app guida l'utente all'impostazione Settings (Intent `ACTION_APPLICATION_DETAILS_SETTINGS`).

**Volunteer:** Federico
**Stima:** 5 SP

---

### US-19 — Backend SOS endpoint con firma ECC [Should]

**User Story:** Come **escursionista in difficoltà** voglio inviare un segnale SOS con le mie coordinate, così da ricevere soccorso il prima possibile.

**Stato 17/05:** UI SOS dialog implementata in `RegistraScreen` (TC-17 PARTIAL). Backend non esiste.

**Da fare Sprint 2:**

#### Backend

1. Modello `Emergency`:
   ```javascript
   {
     (userId,
       sessionId,
       coordinates(GeoJSON),
       emergencyType,
       signature(Ed25519),
       timestamp,
       hopCount,
       status(PENDING / VALIDATED / FORWARDED / CANCELLED));
   }
   ```
2. Endpoint `POST /api/v1/emergencies`:
   - Verifica firma ECC Ed25519 con chiave pubblica utente.
   - Status iniziale: `PENDING`.
   - Idempotency: `idempotencyKey` UUID v4 nel body.
3. Endpoint `PATCH /api/v1/emergencies/:id` per validazione/cancellazione capogruppo.
4. Endpoint `GET /api/v1/sessions/:id/emergencies` per dashboard capogruppo.

#### Mobile

5. Generazione coppia chiavi ECC Ed25519 al primo login (via Android Keystore o Tink).
6. Salvataggio chiave pubblica su backend al login.
7. SOS dialog → conferma → API call con timer 5s "Annulla" (Falso Allarme).
8. Cache locale offline se rete mancante (Room: `pending_emergencies`).

**Acceptance criteria:**

- Nuovo TC-21: SOS con device offline → cache locale → invio automatico al ritorno della rete.
- Firma ECC verificata server-side.

**Volunteer:** Marco (backend) + Giacomo (mobile)
**Stima:** 8 SP
**Stato 26/05 (branch `SOS`):** MVP SOS sessione ACTIVE implementato (backend + mobile). Dettaglio: `docs/sos_feature.md`, API: `docs/api_reference.md` § Emergenze.

#### Implementato (branch `SOS`)

**Backend**

- Modello `Emergency`: snapshot GPS, `profileSnapshot`, `beaconInstanceId`, `beaconActive`, stati `ACTIVE` \| `SHARED_WITH_GROUP` \| `DISMISSED` \| `CANCELLED_BY_SENDER`
- `POST /api/v1/emergencies` (idempotente su `idempotencyKey` UUID v4)
- `GET /api/v1/emergencies/:id`, `PATCH` con azioni `cancel`, `dismiss`, `share_with_group`, `unshare_with_group`, `ack`
- `GET /api/v1/sessions/:id/emergencies` (lista + `isGroupLeader`, `hasUnacked`)
- Test: `backend/__tests__/routes/emergency.test.js`

**Mobile**

- Invio SOS con countdown, dialog conferma/annulla, beacon BLE TSM (`SosBeaconService`)
- Dialog BT mittente; invio senza beacon (`beaconActive: false`)
- Ricezione capogruppo/partecipanti: poll 8s, notifica locale, dettaglio, scanner RSSI
- Coda offline Room + WorkManager (`pending_emergencies`, `EmergencyUploadWorker`)
- Revoca condivisione gruppo (`unshare_with_group`)

#### Ancora da fare (US-19 completa / Sprint successivo)

1. Firma **Ed25519** sul payload + verifica server (`signature` opzionale oggi, non verificata).
2. Registrazione chiave pubblica utente (`POST /users/:id/publicKey`).
3. Inoltro verso soccorsi / CNSAS (US backlog).
4. Swagger annotato per emergenze.
5. Allineare coda offline con `beaconActive` se si invia SOS senza beacon da offline.

**Acceptance criteria (parziale):**

- TC-21: SOS offline → coda Room → upload a rete (WorkManager + retry ViewModel) — **coperto**.
- Firma ECC verificata server-side — **non ancora**.

**Volunteer:** Marco (backend) + Giacomo (mobile)
**Stima residua:** ~3 SP (ECC + publicKey + Swagger)

---

### US-20 — HomeScreen Social Feed [Could]

**User Story:** Come **utente** voglio vedere un feed delle attività dei membri del team / community, così da scoprire nuove escursioni interessanti.

**Stato 17/05:** `HomeSocialPlaceholder` solo testo placeholder.

**Da fare Sprint 2:**

1. Modello backend: `Activity` (estensione di `HikeSession` COMPLETED con `visibility: "public/friends/private"`).
2. Endpoint `GET /api/v1/feed/public?limit=20` paginato.
3. UI feed con card: avatar utente, route name, distanza, dislivello, punti, foto opzionale.
4. Like + commenti (futura US-24).

**Acceptance criteria:**

- Feed mostra le ultime 20 attività pubbliche.
- Tap su card → naviga a `ActivityDetailScreen` (anche per attività altrui).

**Volunteer:** Giacomo
**Stima:** 5 SP

---

### US-21 — WorkManager Store-and-Forward telemetria [Should]

**User Story:** Come **sistema** voglio sincronizzare la telemetria GPS accumulata offline in batch al ritorno della rete, così da non perdere dati di tracking in zone di no-spot.

**Stato 17/05:** `ForegroundTrackingService` traccia, ma upload non implementato.

**Da fare Sprint 2:**

1. Aggiungere dipendenza `androidx.work:work-runtime-ktx:2.10.0`.
2. Salvare batch GPS in Room (`TelemetryEntity`) durante il tracking, in `RegistraViewModel.applyLocation()`.
3. Endpoint backend `POST /api/v1/sessions/:id/telemetry` (batch upload, idempotente).
4. `TelemetryUploadWorker` schedulato con `Constraints.Builder().setRequiredNetworkType(CONNECTED)` + `setBackoffCriteria(EXPONENTIAL)`.
5. Cleanup batch già sincronizzati da Room.

**Acceptance criteria:**

- Nuovo TC-24: tracking 5 min offline → ritorno rete → upload batch entro 30s.
- Idempotenza verificata: chiamare 2 volte la stessa batch non duplica.

**Volunteer:** Federico (mobile) + Marco (backend)
**Stima:** 8 SP

---

### US-fix-M5 — Refactor pattern Repository [Should]

**User Story:** Come **developer del team** voglio che tutti i ViewModel passino per il Repository pattern, così da poter testare la business logic senza dipendenze Retrofit.

**Stato 17/05:** Solo `LoginViewModel` e `RegisterViewModel` usano Repository. 4 altri ViewModel chiamano `TsmApiClient.service()` direttamente.

**Da fare Sprint 2:**

1. Creare `SessionRepository`:
   ```kotlin
   interface SessionRepository {
     suspend fun getMySessions(): Result<List<SessionResponse>>
     suspend fun getSessionById(id: String): Result<SessionResponse>
     suspend fun joinSession(code: String): Result<SessionResponse>
     suspend fun leaveSession(id: String): Result<Unit>
     suspend fun deleteSession(id: String): Result<Unit>
     suspend fun updateStatus(id: String, status: String): Result<Unit>
   }
   ```
2. Creare `ActivityRepository`, `WeatherRepository`.
3. Refactor di `SessionJoinViewModel`, `SessionDetailViewModel`, `SessionPlanViewModel`, `ActivityListViewModel`.
4. DI manuale via `TsmApplication` (no Hilt ancora — overhead).

**Acceptance criteria:**

- Nessun ViewModel importa direttamente `TsmApiClient`.
- 1 unit test JUnit per `SessionRepository` con `MockWebServer`.

**Volunteer:** Federico
**Stima:** 5 SP

---

### US-fix-M2 — meetingDate migration [Should]

**User Story:** Come **sistema** voglio che `meetingDate` sia ordinabile cronologicamente, così che il sort `meetingDate: 1` produca un risultato consistent cross-client.

**Stato 17/05:** Campo `String` su Mongoose. Sort lessicografico.

**Da fare Sprint 2:**

1. Cambio schema: `meetingDate: { type: Date }`.
2. Script `backend/scripts/migrateMeetingDate.js`:
   - Trova tutti i documenti con `meetingDate` String.
   - Parse con `Date.parse(...)` (supporta più formati).
   - Update `meetingDate` come `Date`.
3. Update mobile DTO: `meetingDate` come ISO 8601.
4. Update DatePicker in `SessionPlanViewModel` e `SessionDetailViewModel` per emettere ISO.

**Acceptance criteria:**

- TC-26 (nuovo): creare 3 sessioni con date 16/05, 17/05, 02/06 → sort produce ordine cronologico.
- Backfill 100% dei documenti esistenti.

**Volunteer:** Marco
**Stima:** 3 SP

---

### US-22 — Socket.io real-time positions [Could / Sprint 3]

> **Nota SOS:** le coordinate nel popup Dettaglio SOS restano lo **snapshot** all’invio (`docs/sos_feature.md`). La posizione **live** del mittente (e del gruppo) sarà sulla mappa di questa US, dopo il completamento del flusso SOS.

**User Story:** Come **capogruppo** voglio vedere le posizioni live dei partecipanti sulla mia mappa, così da monitorare la coesione del gruppo in tempo reale.

**Stato 17/05:** `socket.io` installato come dipendenza ma non integrato.

**Da fare:**

1. Backend: namespace `/sessions` con room per sessionId.
2. Eventi: `position:update`, `participant:joined`, `participant:left`.
3. Mobile: `SocketManager` singleton con riconnessione automatica.
4. Dashboard capogruppo con mappa partecipanti.

**Stima:** 13 SP — **rinviato a Sprint 3** se Sprint 2 è già pieno.

---

### US-23 — BLE Mesh fallback SOS [Could / Sprint 3]

**User Story:** Come **escursionista in zona di no-spot** voglio che il mio SOS si propaghi via BLE Mesh ai dispositivi vicini, così da avere un canale di emergenza anche senza copertura mobile.

**Stato 17/05:** Architettura pianificata in D2 §3.2.2, nessuna implementazione.

**Da fare:**

1. Studio fattibilità: librerie BLE Mesh per Android (Mesh Provisioner SDK Nordic vs custom GATT).
2. PoC ricezione/trasmissione SOS firmato Ed25519.
3. Politiche hopCount (max 10), TTL, idempotency key.

**Stima:** 21 SP — **Sprint 3** sicuramente.

---

## 3. Pulizia + Debito tecnico Sprint 2

Action items dalla retrospective Sprint 1 (da pianificare entro le prime 2 giornate di Sprint 2):

| #   | Task                                                                    | Volunteer | Stima |
| --- | ----------------------------------------------------------------------- | --------- | ----- |
| 1   | Setup GitHub Actions CI: build APK + ESLint backend + Jest dummy test   | Marco     | 3 SP  |
| 2   | Aggiornare KDoc orfani in `TsmApplication.kt` + `TsmApiService.kt` (M7) | Giacomo   | 1 SP  |
| 3   | Aggiornare `setup_mobile.md` con i nuovi permessi runtime               | Federico  | 1 SP  |
| 4   | Aggiungere unit test JUnit minimi (almeno 1 per ViewModel)              | Federico  | 3 SP  |
| 5   | Aggiungere unit test Jest minimi backend (auth + sessions)              | Marco     | 3 SP  |

---

## 4. Pianificazione settimanale Sprint 2

Distribuzione proposta su 7 giorni (D1-D7) — totale ~40 SP.

| Giorno | Focus principale                                                     | SP  |
| ------ | -------------------------------------------------------------------- | --- |
| D1     | Kickoff + US-16 (polling partecipanti) + US-18 (runtime perm flow A) | 5   |
| D2     | US-18 completion + US-fix-M5 (Repository refactor sessione)          | 5   |
| D3     | US-19 backend (modello Emergency + endpoint)                         | 4   |
| D4     | US-19 mobile (firma ECC + UI)                                        | 4   |
| D5     | US-21 (WorkManager + telemetry endpoint)                             | 8   |
| D6     | US-fix-M2 (migration) + US-17 (Postman tests) + CI setup             | 6   |
| D7     | US-20 (Home Social Feed) + unit tests + buffer per regressioni       | 8   |

---

## 5. Definition of Done estesa per Sprint 2

In aggiunta ai criteri Sprint 1 (cfr. `T6_D3_Ingegneria_Del_Software.md` §Definizione di "Done"):

9. **Unit test minimo**: ogni nuovo ViewModel ha almeno 1 unit test JUnit; ogni nuovo service Node ha almeno 1 test Jest.
10. **Pattern Repository**: ogni nuovo ViewModel passa per un Repository (no chiamate dirette a `TsmApiClient.service()`).
11. **CI green**: la PR non viene mergiata se la GitHub Action CI fallisce su lint/build/test.

---

## 6. Sprint Review attesa (output demo)

Al termine di Sprint 2, la demo dovrebbe includere:

1. **TC-07 fix**: Partecipante che riceve aggiornamento ACTIVE automatico e si unisce al tracking.
2. **TC-08 fix**: Tracking GPS continuo a schermo spento per 5 minuti (device fisico).
3. **TC-21 (nuovo)**: Premere SOS → invio API con firma ECC + visualizzazione su dashboard capogruppo.
4. **TC-24 (nuovo)**: Tracciare GPS offline → ritorno rete → upload batch automatico.
5. **TC-09 + US-20**: Home Social Feed con attività della community visibili.

---

## 7. Rischi noti per Sprint 2

| Rischio                                    | Impatto | Mitigazione                                                              |
| ------------------------------------------ | ------- | ------------------------------------------------------------------------ |
| Android Keystore Ed25519 complessità       | Alto    | Iniziare con libreria Google Tink come fallback se Keystore problematico |
| WorkManager backoff su batch grandi (>1MB) | Medio   | Splittare batch in chunk di max 500 punti                                |
| Sync polling 30s troppo lento UX           | Medio   | Considerare Server-Sent Events come transition layer prima di Socket.io  |
| Refactor Repository introduce regressioni  | Alto    | Refactor incrementale 1 ViewModel/PR + smoke test ogni merge             |

---

_Sprint 2 plan generato il 17/05/2026 — Pre-kickoff Sprint 2. Documento da revisionare nel meeting di Sprint Planning del team._
