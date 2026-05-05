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
- `data/local/`: Database Room per il caching offline.
- `data/remote/`: Client Retrofit per le API Node.js.
- `service/`: Contiene il **Foreground Service** per il tracking GPS continuo (User Story #42).

## Permessi Critici
Durante lo sviluppo, testare sempre il comportamento dei permessi a runtime:
- `ACCESS_FINE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `BLUETOOTH_SCAN` / `BLUETOOTH_ADVERTISE`

---
*Documento creato per il team di sviluppo Trento Smart Mountain.*
