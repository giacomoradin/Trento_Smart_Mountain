# Setup Sviluppo Mobile (Android Studio) 📱

Per garantire che il team lavori in modo consistente sull'app nativa **Trento Smart Mountain**, seguire queste linee guida.

## Prerequisiti
- **Android Studio Jellyfish** (o versione più recente).
- **JDK 17** (obbligatorio per le versioni recenti di Gradle/Kotlin).
- **Android SDK:** API Level 28 (Android 9.0 Pie) come target minimo.

## Configurazione Iniziale
1.  **Apertura Progetto:** All'interno di Android Studio, seleziona "Open" e punta alla cartella `mobile/` della repository.
2.  **Sincronizzazione Gradle:** Lascia che Android Studio scarichi le dipendenze e sincronizzi il progetto (richiede connessione internet).
3.  **Code Style:** Il file `.vscode/settings.json` definisce l'indentazione a 2 spazi. Assicurati che Android Studio sia configurato per rispettare questo standard (Settings -> Editor -> Code Style -> Kotlin).

## Architettura Codebase
L'app segue il pattern **MVVM (Model-View-ViewModel)**:
- `ui/`: Activity, Fragment e componenti Jetpack Compose (se utilizzati).
- `viewmodel/`: Logica di presentazione e gestione dello stato.
- `repository/`: Single Source of Truth per i dati (gestisce lo switch Online/Offline).
- `data/local/`: **`TokenStorage`** (JWT in **EncryptedSharedPreferences**, singleton da `TsmApplication`), **`AuthSession`** (ripristino sessione all’avvio), segnaposto **`LocalDataSource`**; obiettivo **Room** per cache escursione (vedi sotto).
- `data/remote/`: Client Retrofit per le API Node.js.
- `service/`: Contiene il **Foreground Service** per il tracking GPS continuo (User Story #42).

## Persistenza locale (piano di riferimento)

Allineato a README (**offline-first**, *store-and-forward*) e al backlog di progetto (`docs/Backlog V1 (1).xlsx`, non versionato in git):

| Fase | Obiettivo | User story / nota |
| --- | --- | --- |
| **0 — Stato attuale** | JWT cifrato + ripristino sessione all’avvio; nessun Room né coda sync | Fase auth (parziale **#2**) |
| **1 — Auth persistente** | Biometrico/PIN oltre al JWT; logout che pulisce tutta la cache auth sensibile | Resto **#2**, **#5** |
| **2 — Room + repository** | Database Room, DAO, migrazioni; repository che legge/scrivono locale prima della rete | **#10** (cache coordinate), fondazione SSOT |
| **3 — Limite storage** | Tetto **50 MB** su SQLite con eviction **FIFO** sui dati più vecchi | **#35** |
| **4 — Hike Packet** | Download traccia GeoJSON + map tiles OSM (padding 1 km) e metadati in DB; file binari su filesystem app | **#39**, **#12** |
| **5 — Store-and-forward** | Coda append-only (posizioni, eventi SOS/metadati); `WorkManager` per upload batch al ripristino rete | **#10**, **#37**, **#11** |
| **6 — Integrazione UI** | Sessione/mappa offline leggono dalla cache; indicatori online/offline | Tab Sessione / Mappa |

Dipendenze Gradle: **Room**, **DataStore**, **WorkManager** vanno aggiunte quando si apre la fase corrispondente (`gradle/libs.versions.toml`). **security-crypto** è già presente per il JWT.

## Permessi Critici
Durante lo sviluppo, testare sempre il comportamento dei permessi a runtime:
- `ACCESS_FINE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `BLUETOOTH_SCAN` / `BLUETOOTH_ADVERTISE`

---
*Documento creato per il team di sviluppo Trento Smart Mountain.*
