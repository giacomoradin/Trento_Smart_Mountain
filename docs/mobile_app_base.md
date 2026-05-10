# Guida rapida: cos’è stata creata per l’app Android (base da zero)

Questo documento riassume, in modo semplice, **cosa c’è nella cartella `mobile/`** e perché serve. È pensato per chi **sa programmare** ma ha **poca o nulla esperienza con le app Android**.

---

## 1. Cos’è “un’app Android” in questo progetto

- Il codice dell’app sta nella cartella **`mobile/`**.
- L’app è scritta in **Kotlin** e usa **Jetpack Compose** per l’interfaccia (UI dichiarata nel codice invece che solo con file XML layout tradizionali).
- Il pattern adottato è **MVVM** (Model–View–ViewModel): la schermata mostra dati, il **ViewModel** contiene la logica di presentazione (validazione, stati di caricamento), i **repository** (da completare) collegheranno rete e database locale.

---

## 2. Gradle: perché esiste e cosa abbiamo messo

**Gradle** è lo strumento che **compila** l’app, scarica le librerie (dipendenze) e genera il file installabile (APK).

File importanti nella radice di `mobile/`:

| File / cartella | A cosa serve |
|-----------------|--------------|
| **`settings.gradle.kts`** | Dice a Gradle che esiste il modulo **`app`** e come si chiama il progetto. |
| **`build.gradle.kts`** (root) | Dichiara quali plugin Android/Kotlin sono disponibili per i moduli figli. |
| **`gradle.properties`** | Opzioni globali (memoria, AndroidX, ecc.). |
| **`gradle/libs.versions.toml`** | Elenco centralizzato delle **versioni** delle librerie (Compose, Retrofit, ecc.). |
| **`gradlew` / `gradlew.bat`** | Script per lanciare Gradle **senza** installarlo a mano sul PC. |
| **`gradle/wrapper/gradle-wrapper.properties`** | Indica **quale versione di Gradle** usare (nel nostro caso **9.0.0**, come da accordi del team). |

Compilazione tipica da terminale (dentro `mobile/`):

```bash
.\gradlew.bat :app:assembleDebug
```

---

## 3. Il modulo `app/`: cuore dell’applicazione

Dentro **`mobile/app/`**:

| Percorso | Contenuto |
|----------|-----------|
| **`build.gradle.kts`** | Configura il modulo: `applicationId`, SDK minimo/target, dipendenze (Compose, Navigation, Retrofit, ecc.), generazione di **`BuildConfig.BASE_URL`** per l’URL del backend. |
| **`src/main/AndroidManifest.xml`** | “Scheda” dell’app per Android: nome, icona, Activity principale, permesso **INTERNET**, configurazione di rete per sviluppo. |
| **`src/main/java/it/trentosmartmountain/app/`** | Codice Kotlin dell’app (package **`it.trentosmartmountain.app`**). |
| **`src/main/res/`** | Risorse: stringhe, colori, drawable (icona), XML per la sicurezza di rete in sviluppo. |

### Package `it.trentosmartmountain.app`

È l’**identificativo univoco** dell’app (come un “dominio invertito”). Tutte le classi Kotlin vivono sotto questo package (o sotto-package come `.ui`, `.viewmodel`, ecc.).

---

## 4. Struttura delle cartelle Kotlin (coerente con MVVM)

Cartelle principali sotto `java/it/trentosmartmountain/app/`:

| Cartella | Ruolo |
|----------|--------|
| **`MainActivity.kt`** | Punto d’ingresso UI: avvia Compose e il tema. |
| **`TsmApplication.kt`** | Classe `Application` globale (per ora minimale; qui si potranno mettere init future). |
| **`ui/theme/`** | Colori e tema Material 3 (`TsmTheme`). |
| **`ui/navigation/`** | Destinazioni (`Routes`) e grafo di navigazione (`TsmNavHost`): login → home. |
| **`ui/screens/login/`** | Schermata **login** (email e password). |
| **`ui/screens/`** | Altre schermate (es. **`HomePlaceholderScreen`**, segnaposto dopo login). |
| **`viewmodel/`** | **`LoginViewModel`**: stato del form, validazione locale, (in futuro) chiamate API. |
| **`data/remote/`** | **`TsmApiClient`** (Retrofit + OkHttp) e **`TsmApiService`** (contratto delle API REST; da estendere con `@POST` login, ecc.). |
| **`data/local/`** | Segnaposto per dati sul telefono (Room / DataStore) quando implementerete la parte offline. |
| **`repository/`** | Interfaccia **`AppRepository`**: livello che la UI userà per ottenere dati senza sapere se vengono da rete o cache. |
| **`service/`** | Segnaposto per il **Foreground Service** (GPS/BLE, user story futura). |

---

## 5. Cosa fa oggi la schermata di login

- Due campi: **email** e **password**, con messaggi di errore se i dati non sono validi (email vuota/formato, password troppo corta).
- Pulsante **Entra** e stato di **caricamento** durante l’operazione.
- **Importante:** al momento, dopo la validazione, c’è ancora un **ritardo simulato** e poi navigazione alla **Home** (placeholder). La **vera chiamata HTTP** al backend (`POST /auth/login`) va implementata nel ViewModel/repository quando il server è stabile.

---

## 6. Rete e backend (concetti utili)

- **`BuildConfig.BASE_URL`**: URL base del server Node.js. In emulatore Android, **`10.0.2.2`** punta al **localhost del PC** dove gira il backend (porta tipica **3000**).
- **`network_security_config.xml`**: in sviluppo permette HTTP verso host locali (solo per test; in produzione si userà HTTPS).
- **`TsmApiClient`**: crea un’istanza Retrofit con Gson per JSON; da qui si ottiene `TsmApiService` per definire le funzioni API.

---

## 7. Cosa non è ancora stato fatto (prossimi passi tipici)

- Definire in **`TsmApiService`** il login (`POST auth/login` rispetto alla `BASE_URL`).
- Aggiungere un **repository** (es. `AuthRepository`) che chiama l’API e traduce errori HTTP in messaggi per l’utente.
- Salvare il **JWT** in modo sicuro (es. EncryptedSharedPreferences / DataStore).
- Sostituire il delay nel **`LoginViewModel`** con la chiamata reale.

---

## 8. Dove approfondire senza essere esperti Android

- Documentazione ufficiale **Jetpack Compose**: [developer.android.com/develop/ui/compose](https://developer.android.com/develop/ui/compose)
- **MVVM** in Android: ViewModel + osservazione dello stato (nel nostro caso `StateFlow` + `collectAsStateWithLifecycle`).
- **Retrofit**: client REST che si interfaccia con le route Express del backend Node.js.

---

## Riepilogo in una frase

È stato creato uno **scheletro di app Android moderna** (Gradle 9, Compose, navigazione, tema, login con validazione, client HTTP pronto), organizzato per **MVVM** e per collegarsi al backend Node quando implementerete le API di autenticazione.
