# Test Cases — Sprint 1 (Design)

> Documento di **design dei test cases** per lo Sprint 1 di Trento Smart Mountain.
> Per Sprint 1 il docente accetta il solo *design* dei test (non l'esecuzione automatica). Questo file documenta ogni TC con precondizioni, passi e risultato atteso/attuale.
>
> **Ultima revisione**: 17/05/2026 — Fine Sprint 1.
> **Riferimenti**: `TSM_PROJECT_STATE.md` §11.3, `docs/T6_D3_Ingegneria_Del_Software.md` §Test Cases.

---

## 1. Convenzioni

### 1.1 Tipologie di test

| Tipo | Descrizione | Esecutore |
|------|-------------|-----------|
| **E2E manuale** | Flusso utente completo, app + backend + DB | Tester umano su device |
| **UI manuale** | Solo schermata mobile, no rete | Tester umano su emulator |
| **API manuale** | Chiamata REST diretta, no UI | Postman / cURL |
| **Device fisico** | Richiede device reale (GPS, accelerometro) | Tester con device |

### 1.2 Esiti

| Codice | Significato |
|--------|-------------|
| ✅ **PASS** | Output atteso = output attuale |
| 🔴 **FAIL** | Bug confermato; FAIL noto è documentato in `TSM_PROJECT_STATE.md` §10 |
| 🟠 **PARTIAL** | Comportamento funzionante ma con caveat documentati |
| ⏳ **N/A Sprint 1** | Test rinviato a Sprint 2 (richiede feature non ancora implementate) |

### 1.3 Severità del bug

| Severità | Definizione |
|----------|-------------|
| 🔴 Critical | Blocca un flusso utente principale o introduce vulnerabilità sicurezza |
| 🟠 High | Degrada significativamente l'UX o causa data inconsistency |
| 🟡 Medium | Limitazione funzionale non bloccante |
| ⚪ Low | Cosmetico, UX improvement |

---

## 2. Matrice di copertura User Story → Test Case

| US | Titolo | TC primari | TC bug specifici |
|----|--------|-----------|-------------------|
| US-01 | Registrazione + verifica email | TC-01 | — |
| US-02 | Login JWT offline | TC-14 | — |
| US-03 | Registrazione rifugio | TC-13 | — |
| US-04 | Reset password | TC-02 | — |
| US-05 | Pianifica escursione GPX | TC-03 | — |
| US-06 | Join sessione con codice | TC-04 | — |
| US-07 | Dettaglio sessione | TC-05 | — |
| US-08 | Edit mode sessione | TC-12 | — |
| US-09 | AVVIA escursione | TC-06 | TC-07 (C1) |
| US-10 | Tracking GPS background | TC-08 (C3) | TC-15 |
| US-11 | Mappa OSMdroid live | TC-16 | — |
| US-12 | SOS button (UI-only) | TC-17 | — |
| US-13 | Lista attività completate | TC-09 | — |
| US-14 | Dettaglio attività | TC-18 | — |
| US-15 | Meteo TINIA reale | TC-10 | — |
| C1 | Security: weather auth | — | TC-11 (C2) |
| C3 | Permission background | — | TC-08 (C3) |

---

## 3. Test cases — Autenticazione & Account

### TC-01 — Registrazione utente + verifica email

| Campo | Valore |
|-------|--------|
| **User Story** | US-01 |
| **Tipo** | E2E manuale |
| **Severità potenziale** | 🔴 Critical |
| **Precondizione** | App pulita installata, backend up, MongoDB pronto, SMTP Gmail attivo |

**Passi:**

1. Aprire l'app → schermata `AuthEntryScreen`.
2. Tap su `Registrati`.
3. Inserire email valida non già usata (es. `nuovo@example.com`).
4. Inserire password ≥ 8 caratteri.
5. Spuntare checkbox ToS GPS.
6. Tap `Conferma`.
7. Attendere email da `noreply@gmail.com` (controllare anche spam).
8. Tap sul link `tsm://auth/verify/<token>` nell'email.
9. App si apre automaticamente su `LoginScreen` con email precompilata.
10. Inserire password creata e tap `Accedi`.

**Output atteso:**

- Step 6: API `POST /users` → `201 Created`, body con `userId`.
- Step 7: email ricevuta entro 30 secondi (con retry 3x esponenziale: 2s/4s/8s).
- Step 8: deep link gestito da `MainActivity` con `intent-filter` scheme `tsm`.
- Step 10: API `POST /auth/login` → `200 OK` + JWT.
- JWT cifrato e persistito in `EncryptedSharedPreferences` via `TokenStorage`.
- Navigazione verso `MAIN_HIKER` (bottom nav 4 tab).

**Output attuale:** ✅ **PASS**

---

### TC-02 — Reset password via email

| Campo | Valore |
|-------|--------|
| **User Story** | US-04 |
| **Tipo** | E2E manuale |
| **Severità potenziale** | 🟠 High |
| **Precondizione** | Account esistente verificato (TC-01 completato) |

**Passi:**

1. App pulita → `LoginScreen` → tap su `Password dimenticata?`.
2. Inserire email account esistente.
3. Tap `Invia link`.
4. Attendere email reset (entro 30s).
5. Tap sul link nell'email → si apre browser con form HTML.
6. Inserire nuova password (≥ 8 caratteri) due volte.
7. Tap `Salva nuova password`.
8. Tornare all'app, fare login con la nuova password.

**Output atteso:**

- Step 3: API `POST /auth/forgot-password` → `200 OK`.
- Step 5: link contiene token monouso (`passwordResetToken` su User, scadenza 1h).
- Step 6-7: form HTML responsive servita da `GET /auth/reset-password/:token`; submit a `POST /auth/reset-password/:token`.
- Step 7: response: HTML success page; token invalidato lato server.
- Step 8: login successful con nuova password; vecchia password rifiutata.

**Output attuale:** ✅ **PASS**

---

### TC-13 — Registrazione rifugio

| Campo | Valore |
|-------|--------|
| **User Story** | US-03 |
| **Tipo** | E2E manuale |
| **Severità potenziale** | 🟠 High |
| **Precondizione** | App pulita installata, backend up |

**Passi:**

1. `AuthEntryScreen` → tap `Registra Rifugio`.
2. Compilare form:
   - Nome rifugio (es. "Rifugio Bolzano al Bicchiere")
   - Codice CAI (es. "B046")
   - Quota in metri (es. 2541)
   - Posti disponibili (es. 30)
   - Coordinate (es. "46.6231 11.4583")
   - Email + password
3. Tap `Conferma`.
4. Verificare email + tap link `tsm://`.
5. Login.

**Output atteso:**

- Step 3: API `POST /users` con `role: "rifugio"` e `rifugioDetails` populated → `201 Created`.
- User document con `role: "rifugio"`, `rifugioDetails: { rifugioName, caiCode, quota, posti, coordinates }`.
- Step 5: dopo login, navigazione verso `MAIN_RIFUGIO` (non `MAIN_HIKER`).

**Output attuale:** ✅ **PASS**

---

### TC-14 — Login offline con JWT cached

| Campo | Valore |
|-------|--------|
| **User Story** | US-02 |
| **Tipo** | E2E manuale |
| **Severità potenziale** | 🟠 High |
| **Precondizione** | Login già fatto in passato; JWT salvato in EncryptedSharedPreferences |

**Passi:**

1. Login online completato (TC-01 step 10).
2. Chiudere completamente l'app (force stop o swipe via).
3. Disattivare Wi-Fi e dati mobili sul device.
4. Riaprire l'app.

**Output atteso:**

- App parte direttamente da `MAIN_HIKER` (non `AuthEntryScreen`).
- `AuthSession.init()` legge il JWT cifrato e ne valuta la validità (scadenza claim).
- Bottom nav mostra le 4 tab.
- Tap su `Profilo` → mostra `username` da cache Room (non dalla rete).
- Eventuale chip "Offline" visibile.

**Output attuale:** ✅ **PASS**

---

## 4. Test cases — Sessioni Escursione

### TC-03 — Creazione sessione con import GPX

| Campo | Valore |
|-------|--------|
| **User Story** | US-05 |
| **Tipo** | E2E manuale |
| **Severità potenziale** | 🔴 Critical |
| **Precondizione** | Login completato; file `.gpx` valido sul device (es. `Catinaccio.gpx`) |

**Passi:**

1. Bottom nav → tab `Sessione`.
2. Tab interno `PIANIFICA`.
3. Tap `Carica GPX` → selezionare file dal file picker Android.
4. Verificare che le statistiche siano calcolate (distanza, dislivello, durata stimata).
5. Compilare form:
   - Nome (es. "Catinaccio – Domenica")
   - Data (DatePicker)
   - Ora (TimePicker, es. 06:30)
   - Difficoltà (Dropdown: T/E/EE/EEA)
   - Max partecipanti (es. 8)
6. Tap `Crea sessione`.

**Output atteso:**

- Step 4: parser GPX (XmlPullParser + smoothing MA(5) + valley-peak threshold 10m + campionamento 50 punti).
- Stats GPX visibili: `distanceKm`, `elevationGainM`, `trackPoints`, `elevationProfile` (max 50 punti).
- `estimatedPoints` calcolato da `HikeEstimation.estimatedPoints(D, H, K=10)`.
- Step 6: API `POST /api/v1/sessions` → `201 Created`.
- Dialog mostra `inviteCode TSM-XXXX` + QR code (ZXing) + bottone Copia.
- Sessione visibile nel tab `UNISCITI`.

**Output attuale:** ✅ **PASS**

---

### TC-04 — Join sessione con codice invito

| Campo | Valore |
|-------|--------|
| **User Story** | US-06 |
| **Tipo** | E2E manuale (2 account, 2 device/emulator) |
| **Severità potenziale** | 🔴 Critical |
| **Precondizione** | Account A ha creato una sessione (TC-03); account B esiste e ha fatto login |

**Passi:**

1. Account B: login → bottom nav `Sessione`.
2. Tab interno `UNISCITI`.
3. Inserire `TSM-XXXX` nelle OTP boxes.
4. Tap `Unisciti`.
5. Verificare comparsa della sessione nella lista UNISCITI.
6. Tap sulla sessione → `SessionDetailScreen`.

**Output atteso:**

- Step 4: API `POST /api/v1/sessions/join` → `200 OK`.
- Backend aggiunge `userId` a `participants[]` con `role: "hiker"`.
- Step 5: sessione visibile con info corrette (nome, data, host).
- Step 6: dettaglio mostra creator e tutti i partecipanti (populate symmetric).

**Output attuale:** ✅ **PASS**

---

### TC-05 — Visualizzazione dettaglio sessione

| Campo | Valore |
|-------|--------|
| **User Story** | US-07 |
| **Tipo** | UI manuale |
| **Severità potenziale** | 🟠 High |
| **Precondizione** | Sessione con GPX caricato esistente (TC-03), utente unito (TC-04) |

**Passi:**

1. Aprire la sessione dal tab `UNISCITI`.
2. Scrollare lungo `SessionDetailScreen`.

**Output atteso:**

- **InviteCodeCard**: codice TSM-XXXX visibile, bottone COPIA funzionante.
- **DetailCard con profilo altimetrico**: Canvas normalizzato min/max dal GPX reale, area fill gradient TsmAccent.
- **Metriche**: Distanza, Dislivello +Xm, Durata CAI (formato `Xh Ym`).
- **Punti stimati**: `μ = 1.0`, calcolato CAI.
- **MeteoCard**: emoji condizione cielo, temp min/max, vento, prob. pioggia (dati TINIA reali).
- **ChecklistCard**: 5-8 item di default (in base a difficoltà), checkbox + handle drag.
- **ParticipantsCard**: avatar circolari con iniziali, badge groupLeader (border TsmAccent).
- Solo creator: edit icon visibile nell'app bar.
- Solo creator + stato PLANNED: pulsante `▶ AVVIA ESCURSIONE`.
- Non-creator + PLANNED: chip "⏳ In attesa che il Capogruppo avvii la sessione".

**Output attuale:** ✅ **PASS**

---

### TC-12 — Edit mode sessione (creator only)

| Campo | Valore |
|-------|--------|
| **User Story** | US-08 |
| **Tipo** | UI manuale |
| **Severità potenziale** | 🟠 High |
| **Precondizione** | Login come creator di una sessione PLANNED |

**Passi:**

1. Aprire `SessionDetailScreen`.
2. Tap icona Edit (lapis) in app bar.
3. EditModeCard si espande con animazione.
4. Modificare nome / data / ora / max participants / difficoltà.
5. Tap icona Save (check verde).
6. Attendere chiusura automatica del pannello edit.

**Output atteso:**

- Step 3: AnimatedVisibility expandVertically(tween(250)).
- Step 5: API `PATCH /api/v1/sessions/:id` → `200 OK` con `ApiMessageBody`.
- `silentReloadSession()` aggiorna i dati senza scatenare `isLoading`.
- Step 6: `editMode = false` automaticamente (state machine controllata).
- Verifica MongoDB: campi aggiornati persistiti.

**Output attuale:** ✅ **PASS**

---

## 5. Test cases — AVVIA & GPS Tracking

### TC-06 — AVVIA escursione (creator)

| Campo | Valore |
|-------|--------|
| **User Story** | US-09 |
| **Tipo** | E2E manuale |
| **Severità potenziale** | 🔴 Critical |
| **Precondizione** | Login come creator, sessione PLANNED esistente con GPX, permessi GPS dati |

**Passi:**

1. Aprire `SessionDetailScreen` come creator.
2. Tap `▶ AVVIA ESCURSIONE`.
3. (Se data ≠ oggi): dialog "Avviare in anticipo?" → tap `Avvia`.

**Output atteso:**

- API `PATCH /api/v1/sessions/:id/status` con `status: "ACTIVE"` → `200 OK`.
- Backend: `session.status = "ACTIVE"`, `startTime = new Date()`.
- `SessionStartCoordinator.requestStart(sessionId)` → emit in `pendingSessionStart`.
- `RegistraViewModel.autoStartFromSession()` consuma l'evento.
- `HikerMainScreen` riceve l'evento, switcha automaticamente a tab `Registra`.
- `ForegroundTrackingService` parte (notifica persistente con icona montagna).
- Mappa OSMdroid centrata su posizione GPS attuale.

**Output attuale:** ✅ **PASS**

---

### TC-07 — AVVIA da partecipante (Bug C1)

| Campo | Valore |
|-------|--------|
| **User Story** | US-09 |
| **Tipo** | E2E manuale |
| **Severità potenziale** | 🔴 Critical |
| **Precondizione** | Login come partecipante non-creator, sessione PLANNED esistente |

**Passi:**

1. Aprire `SessionDetailScreen` come partecipante.
2. Verificare visibilità del pulsante AVVIA.

**Output atteso:**

- Pulsante `▶ AVVIA ESCURSIONE` NON visibile (gated da `isCreator` check).
- In sua sostituzione: chip informativo "⏳ In attesa che il Capogruppo avvii la sessione".
- Quando lo status diventa `ACTIVE` (creator ha avviato): pulsante "▶ UNISCITI AL TRACKING" appare.

**Output attuale:** ✅ **PASS** (fix applicato 17/05 — gating in `SessionDetailScreen.kt`)

**Storico:** Prima del fix C1, il pulsante AVVIA era visibile a tutti → partecipanti chiamavano `PATCH /status` → `403 FORBIDDEN` (solo creator autorizzato) → caricamento perpetuo. Audit interno 17/05 ha rilevato il bug; fix applicato gating UI al creator.

---

### TC-08 — Tracking GPS a schermo spento (Bug C3)

| Campo | Valore |
|-------|--------|
| **User Story** | US-10 |
| **Tipo** | Device fisico (NON funziona su emulator) |
| **Severità potenziale** | 🔴 Critical |
| **Precondizione** | Sessione ACTIVE, tracking GPS in corso, device con GPS hardware reale |

**Passi:**

1. Avviare tracking (TC-06).
2. Camminare per 5 minuti tenendo l'app in foreground → verificare metriche live.
3. Bloccare schermo (tasto power).
4. Continuare a camminare 5 minuti con schermo spento.
5. Sbloccare schermo e tornare all'app.
6. Verificare metriche: distanza, dislivello, traccia sulla mappa.

**Output atteso:**

- Step 2: `distanceMeters` aumenta linearmente; `trackGeoPoints` cresce.
- Step 4: tracciamento continua via `ForegroundTrackingService` + `WAKE_LOCK`.
- Step 5: nessun gap evidente nella traccia GPS.
- Step 6: distanza totale ≈ ground truth ± 5%.

**Output attuale:** 🟠 **PARTIAL** (fix C3 parziale applicato 17/05)

**Storico/Status:**
- Prima del 17/05: `ACCESS_BACKGROUND_LOCATION` mancante dal manifest → tracking interrotto dopo 1-2 minuti di schermo spento.
- Fix 17/05: aggiunto `ACCESS_BACKGROUND_LOCATION` e `WAKE_LOCK` al manifest.
- **Resta da fare (Sprint 2 — US-18):** implementare il **flusso runtime** di richiesta permesso. Su Android 11+ l'utente deve:
  1. Concedere `ACCESS_FINE_LOCATION` (foreground)
  2. Concedere separatamente `ACCESS_BACKGROUND_LOCATION` (settings system)
  Il test cliente NON può quindi essere considerato 100% PASS finché il runtime permission flow non è esposto in UI.

---

### TC-15 — Auto-pause GPS da fermo

| Campo | Valore |
|-------|--------|
| **User Story** | US-10 |
| **Tipo** | Device fisico |
| **Severità potenziale** | 🟡 Medium |
| **Precondizione** | Tracking GPS attivo, accelerometro disponibile |

**Passi:**

1. Avviare tracking.
2. Rimanere fermi (seduto/in piedi) per 45+ secondi.
3. Verificare comparsa indicatore "AUTO-PAUSA" in UI.
4. Riprendere a camminare (≥ 1.0 m/s).
5. Verificare ripristino automatico del tracking.

**Output atteso:**

- Step 3: `StationaryDetector` rileva immobilità (speed < 0.5 m/s + accelerometro), dopo `AUTO_PAUSE_DELAY_MS = 45_000ms` → `trackingEngine.pause(manual = false)`.
- UI: `isAutoPaused = true`, badge giallo "AUTO-PAUSA".
- Step 5: speed ≥ `RESUME_SPEED_MPS = 1.0` → `resumeTracking()`.

**Output attuale:** ✅ **PASS**

---

### TC-16 — Mappa OSMdroid posizione live

| Campo | Valore |
|-------|--------|
| **User Story** | US-11 |
| **Tipo** | UI manuale + device fisico |
| **Severità potenziale** | 🟠 High |
| **Precondizione** | Permesso GPS concesso, tab Registra aperta |

**Passi:**

1. Bottom nav → tab `Registra`.
2. Concedere permessi GPS se richiesti.
3. Attendere fix GPS (badge segnale).
4. Tap bottone "center on user" (icona crosshair).
5. Avviare tracking (`▶`).
6. Camminare per qualche metro.

**Output atteso:**

- Step 3: `UserLocationTracker` ottiene fix; badge segnale GPS mostra livello (0-5).
- Step 4: `centerOnUserTick` incrementato → mappa si centra sulla posizione.
- Step 6: marker utente si muove sulla mappa; traccia disegnata (polyline TsmPrimary).

**Output attuale:** ✅ **PASS**

---

## 6. Test cases — Sicurezza & Permessi

### TC-11 — Endpoint Weather protetti (fix Bug C2)

| Campo | Valore |
|-------|--------|
| **Bug** | C2 |
| **Tipo** | API manuale (Postman/cURL) |
| **Severità potenziale** | 🔴 Critical |
| **Precondizione** | Backend up; nessun JWT |

**Passi:**

1. Chiamare `POST http://localhost:3000/weather/seed` senza header `Authorization`.
2. Chiamare `POST http://localhost:3000/weather/forecast/<id>/refresh` senza header.
3. Chiamare `GET http://localhost:3000/weather/locations/nearby?lon=11.35&lat=46.50` senza header.
4. Chiamare `POST /weather/seed` con JWT di un utente normale (role: `groupLeader`).
5. Chiamare `POST /weather/seed` con JWT di un utente admin (`role: "admin"`).

**Output atteso:**

- Step 1, 2: `401 Unauthorized` (middleware `authenticate` mancante = JWT richiesto).
- Step 3: `200 OK` (i `GET /weather/*` sono pubblici per design — dati meteo non sensibili).
- Step 4: `403 Forbidden` (middleware `requireRoles("admin")` blocca utenti non-admin).
- Step 5: `200 OK` con risposta del servizio TINIA.

**Output attuale:** ✅ **PASS** (fix applicato 17/05 — `weatherRoutes.js` ora protetto)

**Storico:** Prima del 17/05 tutti gli endpoint `/weather/*` erano pubblici. Audit interno ha identificato il rischio (DoS economico verso API TINIA + blacklist IP). Fix: aggiunto `authenticate + requireRoles("admin")` a `POST /seed` e `POST /forecast/:id/refresh`.

---

### TC-19 — Sicurezza JWT tampering

| Campo | Valore |
|-------|--------|
| **Tipo** | API manuale |
| **Severità potenziale** | 🔴 Critical |
| **Precondizione** | JWT valido ottenuto da login |

**Passi:**

1. Login → ottenere JWT valido.
2. Decodificare il payload del JWT (es. jwt.io).
3. Modificare `userId` o `role` nel payload.
4. Re-encodare senza firmare con il secret.
5. Chiamare `GET /api/v1/sessions/my` con il JWT manipolato.

**Output atteso:**

- Step 5: `401 Unauthorized` (`authenticate` middleware verifica firma HS256 vs `JWT_SECRET`).

**Output attuale:** ⏳ **N/A Sprint 1** (test di sicurezza pianificato Sprint 2 con script automatizzato)

---

## 7. Test cases — Le Mie Attività & Home

### TC-09 — Lista attività completate

| Campo | Valore |
|-------|--------|
| **User Story** | US-13 |
| **Tipo** | UI manuale |
| **Severità potenziale** | 🟠 High |
| **Precondizione** | Almeno 1 attività COMPLETED esistente in Room |

**Passi:**

1. Completare un tracking GPS (TC-06 → camminare → Stop → confermare salvataggio).
2. Verificare comparsa toast "Attività salvata".
3. Bottom nav → tab `Home`.
4. Tab interno `Le Mie Attività`.
5. Scrollare la lista.

**Output atteso:**

- Step 2: `CompletedActivityEntity` inserito in Room via `saveCompletedActivity`.
- Step 4: schermata mostra card statistiche annuali (totale km, dislivello, punti, attività).
- Lista attività ordinata per `completedAt` decrescente, con metriche per ogni card.
- Filtro per anno disponibile.
- Tap su una card → naviga a `ActivityDetailScreen`.

**Output attuale:** ✅ **PASS**

---

### TC-18 — Dettaglio attività completata

| Campo | Valore |
|-------|--------|
| **User Story** | US-14 |
| **Tipo** | UI manuale |
| **Severità potenziale** | 🟡 Medium |
| **Precondizione** | Attività completata visibile in lista (TC-09) |

**Passi:**

1. Tap su una card dalla lista attività.
2. Scrollare `ActivityDetailScreen`.

**Output atteso:**

- Nome attività editabile.
- Metriche: distanza, dislivello, durata, velocità media, punti CAI finali.
- `finalPoints = K × D_eq × clip(T_nom/T_reale, 0.8, 1.2)` con μ adattato.
- Mappa OSMdroid con polyline del tracciato (max 200 punti campionati da `trackLatLng` JSON).
- Calorie stimate (formula approx 70 kcal/km × 0.85).

**Output attuale:** ✅ **PASS**

---

## 8. Test cases — Meteo & Servizi Esterni

### TC-10 — Meteo TINIA per coordinate Trentino

| Campo | Valore |
|-------|--------|
| **User Story** | US-15 |
| **Tipo** | E2E manuale |
| **Severità potenziale** | 🟠 High |
| **Precondizione** | DB `locations` seedato (almeno una volta `POST /weather/seed`), sessione con GPX in Trentino |

**Passi:**

1. Creare/aprire sessione con startPoint in Trentino (es. coord `[11.35, 46.50]`).
2. Aprire `SessionDetailScreen`.
3. Attendere caricamento `MeteoCard`.
4. Tap pulsante refresh in alto a destra della card.

**Output atteso:**

- Step 3:
  - API `GET /weather/locations/nearby?lon=11.35&lat=46.50&type=town&limit=1` → town più vicina.
  - API `GET /weather/forecast/<externalId>` → forecast 3h (16 slot) + 24h (7 giorni).
  - UI mostra: emoji condizione, temp min/max, vento, prob. pioggia, lista 5 slot 3h orari.
- Step 4: `forceRefresh: true` query param → backend bypassa cache MongoDB (1h).
- `meteoLastUpdate` aggiornato.

**Output attuale:** ✅ **PASS**

---

### TC-20 — Meteo per coordinate fuori area coperta

| Campo | Valore |
|-------|--------|
| **User Story** | US-15 |
| **Tipo** | E2E manuale |
| **Severità potenziale** | 🟡 Medium |
| **Precondizione** | Sessione con startPoint fuori Trentino (es. Roma) |

**Passi:**

1. Creare sessione con startPoint a Roma `[12.49, 41.90]`.
2. Aprire `SessionDetailScreen`.

**Output atteso:**

- `MeteoCard` mostra messaggio: "Nessuna stazione meteo trovata nelle vicinanze (404)".
- Pulsante "Riprova" visibile, ma non risolve (la zona non è coperta).
- App NON crasha.

**Output attuale:** ✅ **PASS**

---

## 9. Test cases — SOS (UI-only Sprint 1)

### TC-17 — Pulsante SOS in tracking

| Campo | Valore |
|-------|--------|
| **User Story** | US-12 |
| **Tipo** | UI manuale |
| **Severità potenziale** | 🟡 Medium |
| **Precondizione** | Tracking attivo (TC-06) |

**Passi:**

1. Durante tracking, tap FAB SOS (rosso) in basso a destra.
2. Verificare comparsa dialog conferma.
3. Tap `Annulla`.
4. Tap di nuovo SOS → tap `Conferma SOS`.

**Output atteso:**

- Step 2: dialog "Sei sicuro di inviare un SOS?" con i tasti `Annulla` e `Conferma SOS`.
- Step 4 (attuale Sprint 1): nessuna API chiamata (backend `POST /api/v1/emergencies` non implementato).
- (Sprint 2 — US-19): API call con coordinate GPS + firma ECC + propagazione BLE Mesh.

**Output attuale:** 🟠 **PARTIAL** (solo UI, backend pianificato Sprint 2)

---

## 10. Riepilogo esiti Sprint 1

| TC | Stato | Note |
|----|-------|------|
| TC-01 | ✅ PASS | Registrazione + verifica email + login |
| TC-02 | ✅ PASS | Reset password via email |
| TC-03 | ✅ PASS | Pianifica sessione con GPX |
| TC-04 | ✅ PASS | Join sessione con codice |
| TC-05 | ✅ PASS | Dettaglio sessione completo |
| TC-06 | ✅ PASS | AVVIA da creator |
| TC-07 | ✅ PASS | (fix C1) — Non-creator non vede AVVIA |
| TC-08 | 🟠 PARTIAL | (fix C3 parziale) — runtime permission flow Sprint 2 |
| TC-09 | ✅ PASS | Lista attività completate |
| TC-10 | ✅ PASS | Meteo TINIA Trentino |
| TC-11 | ✅ PASS | (fix C2) — Weather endpoints protetti |
| TC-12 | ✅ PASS | Edit mode sessione |
| TC-13 | ✅ PASS | Registrazione rifugio |
| TC-14 | ✅ PASS | Login offline con JWT |
| TC-15 | ✅ PASS | Auto-pause GPS |
| TC-16 | ✅ PASS | Mappa OSMdroid live |
| TC-17 | 🟠 PARTIAL | SOS UI-only |
| TC-18 | ✅ PASS | Dettaglio attività |
| TC-19 | ⏳ N/A | JWT tampering — Sprint 2 |
| TC-20 | ✅ PASS | Meteo fuori area |

**Totale TC**: 20
- ✅ PASS: 16 (80%)
- 🟠 PARTIAL: 3 (15%)
- ⏳ N/A: 1 (5%)
- 🔴 FAIL: 0

---

## 11. Bug fixati durante Sprint 1 (cronologia)

| Bug | Severità | Scoperto | Fixato | TC |
|-----|----------|----------|--------|-----|
| Gson crash su `updateSession` (ObjectId raw) | 🔴 Critical | Mid-sprint | Mid-sprint (populate symmetric backend) | TC-12 |
| Code box cursore + history ghost | 🟡 Medium | Mid-sprint | Mid-sprint (TextFieldValue) | TC-04 |
| Tab UNISCITI non si aggiorna dopo join | 🟠 High | Mid-sprint | Mid-sprint (LaunchedEffect + DisposableEffect ON_RESUME) | TC-04 |
| Dislivello GPX sovrastimato 2-3× | 🟠 High | Mid-sprint | Mid-sprint (smoothing MA(5) + valley-peak) | TC-03 |
| Edit mode non si chiude dopo save | 🟡 Medium | Mid-sprint | Mid-sprint (loadSession + LaunchedEffect) | TC-12 |
| C1 — Partecipante AVVIA 403 silent | 🔴 Critical | Audit fine sprint | 17/05 (gating UI) | TC-07 |
| C2 — Weather endpoints non protetti | 🔴 Critical | Audit fine sprint | 17/05 (authenticate + requireRoles) | TC-11 |
| C3 — ACCESS_BACKGROUND_LOCATION mancante | 🔴 Critical | Audit fine sprint | 17/05 manifest + Sprint 2 runtime | TC-08 |

---

## 12. Test cases da progettare in Sprint 2

| TC futuro | Trigger | Note |
|-----------|---------|------|
| TC-21 | POST /emergencies (SOS backend) | US-19 |
| TC-22 | Runtime permission flow Android 11+ | US-18 |
| TC-23 | Repository pattern refactor | M5 debito tecnico |
| TC-24 | WorkManager telemetria batch | US-21 |
| TC-25 | Socket.io real-time positions | US-22 |
| TC-26 | meetingDate Date migration script | M2 debito tecnico |
| TC-27 | Unit test JUnit ViewModels | DoD nuovo Sprint 2 |
| TC-28 | Unit test Jest backend services | DoD nuovo Sprint 2 |

---

*Test cases design documento — Sprint 1 TSM, 17/05/2026. Allineato a `Backlog V1 - Sprint 1 Backlog.csv` e `T6_D3_Ingegneria_Del_Software.md`. Aggiornare quando il team finalizza il `Backlog V1 - Test cases.csv`.*
