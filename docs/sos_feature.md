# SOS — note di implementazione (branch `SOS`)

Documento di riferimento per il flusso emergenze in sessione ACTIVE. Aggiornare insieme a `api_reference.md` e Swagger.

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

## Scanner beacon BLE (ricezione)

Dal popup **Dettaglio SOS**, capogruppo e partecipanti (dopo **Condividi con il gruppo**) possono aprire **Cerca segnale beacon**:

- Scansione beacon TSM (manufacturer `0x5453`, UUID + major/minor da `beaconInstanceId`)
- RSSI smussato, fascia segnale (Vicino / Medio / Debole / Assente), distanza **stimata** (indicativa)
- Permessi: `BLUETOOTH_SCAN` + `CONNECT` (API 31+); su API precedenti anche posizione per BLE
- Mittente: se Bluetooth spento, dialog per attivarlo o inviare SOS senza beacon
- Capogruppo: **Revoca accesso al gruppo** (`unshare_with_group`) riporta l'emergenza a visibilità solo capo

| File | Ruolo |
|------|--------|
| `SosBeaconService.kt` | Trasmissione beacon (mittente) |
| `SosBeaconScanner.kt` / `SosBeaconParser.kt` | Scansione e parsing |
| `SosBeaconScannerDialog.kt` | UI dialog |
| `RegistraIncomingSosUi.kt` | Pulsante nel dettaglio |

---

## Coda offline emergenze

Se `POST /emergencies` fallisce per assenza rete, la richiesta resta in Room (`pending_emergencies`). Upload:

- Retry periodico da `RegistraViewModel` quando online
- **WorkManager** `EmergencyUploadWorker` (rete connessa) schedulato da `EmergencyUploadScheduler` alla messa in coda

---

## Riferimenti codice

| Area | File |
|------|------|
| Modello `coordinates` (snapshot) | `backend/src/models/emergency.js` |
| UI dettaglio (solo testo coordinate) | `mobile/.../registra/SosIncomingSosUi.kt` → `SosIncomingDetailDialog` |
| Scanner BLE | `mobile/.../data/ble/SosBeaconScanner.kt`, `SosBeaconScannerDialog.kt` |
| Coda upload | `EmergencyRepository.kt`, `EmergencyUploadWorker.kt` |
| Posizioni live (futuro) | US-22, `GET /api/v1/sessions/:id/positions`, Socket.io |

---

## Notifiche al capogruppo

Ricezione SOS su **Registra** tramite **poll ogni 8 secondi** sulla lista emergenze della sessione attiva, con notifica locale Android quando il poll rileva un nuovo SOS.

---

## Prossime fasi (non in questo branch)

| Fase | Contenuto |
|------|-----------|
| US-22 | Mappa sessione con pin partecipanti (posizione live) |
| Epic 2 | SOS per utenti soli / vicini (anti-spam) |
| Ed25519 | Campo `signature` opzionale sul payload |

---

*Ultimo aggiornamento: scanner BLE + WorkManager coda emergenze; notifiche via poll 8s.*
