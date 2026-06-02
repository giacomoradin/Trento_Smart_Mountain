# SOS — note di implementazione (branch `SOS`)

Documento di riferimento per il flusso emergenze in sessione ACTIVE. Aggiornare insieme a `api_reference.md` (sezione Emergenze) e, quando disponibile, Swagger.

---

## Coordinate nel popup Dettaglio SOS

**Decisione:** le coordinate mostrate nel popup **Dettaglio SOS** sono **statiche**.

- Valore = `emergency.coordinates` salvato sul server al momento dell’**invio** del segnale (`POST /api/v1/emergencies`).
- Non vengono aggiornate mentre l’SOS resta attivo.
- Il poll periodico sulla lista emergenze **non** aggiorna il dettaglio già aperto con una nuova posizione (non esiste oggi un campo “ultima posizione” sull’emergenza).

**Motivazione:** evitare due fonti di verità (popup vs mappa) prima della user story dedicata.

---

## Posizione live — implementazione differita

**Previsto in user story separata:** mappa sessione con pin per **tutti i componenti** (backlog **US-22** — Socket.io / telemetria posizioni gruppo; vedi `docs/sprint2_plan.md`).

| Fase | Cosa mostra la posizione |
|------|---------------------------|
| **SOS (attuale)** | Snapshot GPS all’invio nel dettaglio testuale; avvicinamento fisico via **beacon BLE** (scanner RSSI nel popup dettaglio) |
| **Mappa sessione (dopo)** | Pin partecipanti aggiornati in tempo (quasi) reale; il mittente in SOS potrà essere evidenziato sulla stessa mappa |

**Non pianificato nel MVP SOS:**

- `PATCH /emergencies/:id` con coordinate aggiornate
- Aggiornamento live delle coordinate nel popup Dettaglio SOS

Quando US-22 sarà implementata, il capogruppo userà la **mappa Registra** come vista principale per la posizione corrente; il popup SOS resterà per metadati (profilo, tipo, azioni capo) e beacon.

---

## Campo `beaconActive`

Il client invia `beaconActive: boolean` su `POST /emergencies` (default `true`).

| Valore | Significato |
|--------|-------------|
| `true` | Il mittente ha attivato (o tentato) il beacon BLE; il ricevente può aprire **Cerca segnale beacon**. |
| `false` | SOS inviato **senza beacon** (Bluetooth spento e scelta “Invia senza beacon”, oppure BT non disponibile al momento dell’invio). |

**UI ricevente** (`SosIncomingDetailDialog`):

- Se `beaconActive === false`: messaggio esplicativo; pulsante scanner **non** mostrato.
- Se `beaconActive === true`: pulsante scanner visibile (capogruppo sempre; partecipanti solo dopo `share_with_group`).

Il `beaconInstanceId` è sempre presente (generato lato client per idempotenza/protocollo), anche quando il beacon non trasmette.

---

## Bluetooth — mittente e ricevente

| Ruolo | Comportamento |
|-------|----------------|
| **Mittente** | Prima dell’invio: se BT spento → `SosBluetoothEnableDialog` (attiva BT / invia senza beacon / annulla). Intent di sistema `ACTION_REQUEST_ENABLE`. |
| **Ricevente (scanner)** | In `SosBeaconScannerDialog`: se BT spento → messaggio + pulsante **Attiva Bluetooth** (stesso intent di sistema); la scan parte solo dopo BT on e permessi BLE. |

Notifica foreground del mittente (`SosBeaconService`): stati distinti (preparazione, in trasmissione, BT spento, errore) — non implica che il beacon stia trasmettendo se BT è off.

---

## Protocollo beacon BLE (TSM)

Beacon proprietario (non Apple `0x004C`), per compatibilità advertising su Android:

| Parametro | Valore |
|-----------|--------|
| Manufacturer ID | `0x5453` (“TS”) |
| UUID / major-minor | Da `beaconInstanceId` (12 hex) via `SosBeaconProtocol` |
| Advertising | `SosBeaconService` (foreground) |
| Scansione | `SosBeaconScanner` + `SosBeaconParser` |

File centrali: `SosBeaconProtocol.kt`, `BluetoothHelper.kt`.

---

## Scanner beacon BLE (ricezione)

Dal popup **Dettaglio SOS**, capogruppo e partecipanti (dopo **Condividi con il gruppo**) possono aprire **Cerca segnale beacon** (solo se `beaconActive`):

- RSSI smussato, fascia segnale (Vicino / Medio / Debole / Assente), distanza **stimata** (indicativa)
- Permessi: `BLUETOOTH_SCAN` + `CONNECT` (API 31+); su molti device serve anche `ACCESS_FINE_LOCATION` per risultati affidabili
- Dialog chiusura: icona **X** in alto a destra (liste e dettaglio SOS), non pulsante testuale “Chiudi”

**Azioni capogruppo** sul dettaglio:

- `share_with_group` — visibilità a tutti i partecipanti
- `unshare_with_group` — revoca accesso gruppo (torna `ACTIVE`, solo capo)
- `dismiss` — chiude l’emergenza

| File | Ruolo |
|------|--------|
| `SosBeaconService.kt` | Trasmissione beacon (mittente) |
| `SosBeaconScanner.kt` / `SosBeaconParser.kt` | Scansione e parsing |
| `SosBeaconScannerDialog.kt` / `SosBeaconScannerViewModel.kt` | UI scanner |
| `SosIncomingSosUi.kt` | Lista/dettaglio SOS in entrata |
| `SosDialogs.kt` | Dialog attivazione Bluetooth (mittente) |
| `RegistraViewModel.kt` | Flusso invio, poll, stato UI |

---

## Coda offline emergenze

Se `POST /emergencies` fallisce per assenza rete, la richiesta resta in Room (`pending_emergencies`). Upload:

- Retry periodico da `RegistraViewModel` quando online
- **WorkManager** `EmergencyUploadWorker` (rete connessa) schedulato da `EmergencyUploadScheduler` alla messa in coda

> La coda locale non persiste oggi `beaconActive`; al flush si usa il default server (`true`). Da allineare se serve tracciare SOS “senza beacon” anche offline.

---

## Stati emergenza (backend)

| `status` | Significato |
|----------|-------------|
| `ACTIVE` | Visibile al capogruppo (e al mittente) |
| `SHARED_WITH_GROUP` | Visibile a tutti i partecipanti della sessione |
| `DISMISSED` | Chiusa dal capogruppo |
| `CANCELLED_BY_SENDER` | Annullata dal mittente (`cancel`, opz. `reason`: `MISTAKE` \| `RESOLVED_SELF`) |

**Cleanup automatico (TTL):**  
Il database MongoDB esegue il cleanup automatico delle emergenze per evitare saturazione:
- **3 giorni**: Emergenze risolte (`DISMISSED` o `CANCELLED_BY_SENDER`).
- **7 giorni**: Emergenze attive o non gestite (basato su `createdAt`).

`PATCH` azioni: `cancel`, `dismiss`, `share_with_group`, `unshare_with_group`, `ack` (solo capogruppo, tranne `cancel` = mittente).

---

## Notifiche al capogruppo

Ricezione SOS su **Registra** tramite **poll ogni 8 secondi** su `GET /api/v1/sessions/:id/emergencies`, con notifica locale Android quando il poll rileva un nuovo SOS.

**Non usato in questo branch:** FCM / Firebase (rimosso); nessun push server-side.

---

## Riferimenti codice

| Area | File |
|------|------|
| Modello + `beaconActive` | `backend/src/models/emergency.js` |
| API service | `backend/src/services/emergencyService.js` |
| Route | `backend/src/routes/emergencyRoutes.js`, `hikeSessionRoutes.js` (lista per sessione) |
| Validazione Joi | `backend/src/middleware/validationMiddleware.js` |
| UI dettaglio | `mobile/.../registra/SosIncomingSosUi.kt` |
| Coda upload | `EmergencyRepository.kt`, `EmergencyUploadWorker.kt` |
| Posizioni live (futuro) | US-22, `GET /api/v1/sessions/:id/positions` |

---

## Prossime fasi (non in questo branch)

| Fase | Contenuto |
|------|-----------|
| US-22 | Mappa sessione con pin partecipanti (posizione live) |
| Epic 2 | SOS per utenti soli / vicini (anti-spam) |
| Ed25519 | Campo `signature` opzionale sul payload (verifica server) |
| Swagger | Annotazioni OpenAPI per `/emergencies` |

---

*Ultimo aggiornamento: 26/05/2026 — beaconActive, BT mittente/ricevente, unshare_with_group, protocollo TSM, UI X, poll 8s (no FCM).*
