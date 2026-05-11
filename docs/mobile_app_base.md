# Guida rapida: cos’è stata creata per l’app Android (base da zero)

Questo documento riassume, in modo semplice, **cosa c’è nella cartella `mobile/`** e perché serve.

---

## 1. Cos’è “un’app Android” in questo progetto

- Il codice dell’app sta nella cartella **`mobile/`**.
- L’app è scritta in **Kotlin** e usa **Jetpack Compose** per l’interfaccia (UI dichiarata nel codice invece che solo con file XML layout tradizionali).
- Il pattern adottato è **MVVM** (Model–View–ViewModel): la schermata mostra dati, il **ViewModel** contiene la logica di presentazione (validazione, stati di caricamento), i **repository** chiamano le API del backend e gestiscono dati locali (es. token JWT).

---

## 2. Gradle: perché esiste e cosa abbiamo messo

**Gradle** è lo strumento che **compila** l’app, scarica le librerie (dipendenze) e genera il file installabile (APK).

File importanti nella radice di `mobile/`:

| File / cartella                                | A cosa serve                                                                               |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------ |
| **`settings.gradle.kts`**                      | Dice a Gradle che esiste il modulo **`app`** e come si chiama il progetto.                 |
| **`build.gradle.kts`** (root)                  | Dichiara quali plugin Android/Kotlin sono disponibili per i moduli figli.                  |
| **`gradle.properties`**                        | Opzioni globali (memoria, AndroidX, ecc.).                                                 |
| **`gradle/libs.versions.toml`**                | Elenco centralizzato delle **versioni** delle librerie (Compose, Retrofit, ecc.).          |
| **`gradlew` / `gradlew.bat`**                  | Script per lanciare Gradle **senza** installarlo a mano sul PC.                            |
| **`gradle/wrapper/gradle-wrapper.properties`** | Indica **quale versione di Gradle** usare (nel nostro caso **9.0.0**, come da specifiche). |

Compilazione tipica da terminale (dentro `mobile/`):

```bash
.\gradlew.bat :app:assembleDebug
```

---

## 3. Il modulo `app/`: cuore dell’applicazione

Dentro **`mobile/app/`**:

| Percorso                                        | Contenuto                                                                                                                                                                   |
| ----------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`build.gradle.kts`**                          | Configura il modulo: `applicationId`, SDK minimo/target, dipendenze (Compose, Navigation, Retrofit, ecc.), generazione di **`BuildConfig.BASE_URL`** per l’URL del backend. |
| **`src/main/AndroidManifest.xml`**              | “Scheda” dell’app per Android: nome, icona, Activity principale, permesso **INTERNET**, configurazione di rete per sviluppo.                                                |
| **`src/main/java/it/trentosmartmountain/app/`** | Codice Kotlin dell’app (package **`it.trentosmartmountain.app`**).                                                                                                          |
| **`src/main/res/`**                             | Risorse: stringhe, colori, drawable (icona), XML per la sicurezza di rete in sviluppo.                                                                                      |

### Package `it.trentosmartmountain.app`

È l’**identificativo univoco** dell’app (come un “dominio invertito”). Tutte le classi Kotlin vivono sotto questo package (o sotto-package come `.ui`, `.viewmodel`, ecc.).

---

## 4. Struttura delle cartelle Kotlin (coerente con MVVM)

Cartelle principali sotto `java/it/trentosmartmountain/app/`:

| Cartella                   | Ruolo                                                                                                                                                                                     |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`MainActivity.kt`**      | Punto d’ingresso UI: avvia Compose e il tema.                                                                                                                                             |
| **`TsmApplication.kt`**    | Classe `Application` globale (per ora minimale; qui si potranno mettere init future).                                                                                                     |
| **`ui/theme/`**            | Colori e tema Material 3 (`TsmTheme`).                                                                                                                                                    |
| **`ui/navigation/`**       | Destinazioni (`Routes`) e grafo di navigazione (`TsmNavHost`): scelta accesso/registrazione → login o registrazione → home.                                                               |
| **`ui/screens/auth/`**     | Schermata iniziale con pulsanti **Accedi** e **Registrati**.                                                                                                                              |
| **`ui/screens/login/`**    | Schermata **login** (email e password).                                                                                                                                                   |
| **`ui/screens/register/`** | Schermata **registrazione** (username, email, password, conferma password).                                                                                                               |
| **`ui/screens/`**          | Altre schermate (es. **`HomePlaceholderScreen`**, segnaposto dopo login).                                                                                                                 |
| **`viewmodel/`**           | **`LoginViewModel`** e **`RegisterViewModel`**: stato dei form, validazione locale, chiamate ai repository.                                                                               |
| **`data/remote/`**         | **`TsmApiClient`** (Retrofit + OkHttp), **`TsmApiService`** (login e registrazione), DTO JSON (`LoginRequest`, `LoginResponse`, `RegisterRequest`, `RegisterResponse`, `ApiMessageBody`). |
| **`data/local/`**          | **`TokenStorage`**: salvataggio del JWT in `SharedPreferences` private dell’app dopo login riuscito.                                                                                      |
| **`repository/`**          | **`AuthRepository`** / **`AuthRepositoryImpl`** (login), **`RegistrationRepository`** / **`RegistrationRepositoryImpl`** (registrazione), più **`AppRepository`** (segnaposto generico).  |
| **`service/`**             | Segnaposto per il **Foreground Service** (GPS/BLE, user story futura).                                                                                                                    |

---

## 5. Cosa fa oggi il flusso di accesso

### Schermata iniziale (scelta)

- All’avvio l’app mostra **Trento Smart Mountain** con due azioni: **Accedi** (apre il login) e **Registrati** (apre la registrazione).
- È la **destinazione iniziale** del grafo di navigazione (`Routes.AUTH_ENTRY`).

### Login

- Campi **email** e **password**, con messaggi di errore se i dati non sono validi (email vuota/formato, password troppo corta).
- Pulsante **Entra**, stato di **caricamento** durante la richiesta, messaggio di errore generico se il server non risponde o rifiuta le credenziali.
- Chiamata reale a **`POST /auth/login`** tramite **`AuthRepositoryImpl`**; in caso di successo il **JWT** viene salvato in **`TokenStorage`** e si naviga alla **Home** (placeholder), senza poter tornare alle schermate di autenticazione con il tasto indietro di sistema.

### Registrazione

- Campi **username**, **email**, **password** e **conferma password** (la conferma è solo validazione lato app).
- Chiamata a **`POST /users`** tramite **`RegistrationRepositoryImpl`** (password in chiaro nel body; hash sul server).
- In caso di successo si torna alla schermata di **login**; in caso di conflitto (email/username già usati) o errore di rete viene mostrato un messaggio all’utente.

---

## 6. Rete, backend e configurazione di sviluppo

### URL del server nell’app

- **`BuildConfig.BASE_URL`**: URL base del server Node.js, generato in fase di build da Gradle.
- **Default emulatore:** `http://10.0.2.2:3000/` (`10.0.2.2` è il localhost del PC visto dall’emulatore Android).
- **Telefono fisico (USB debug):** in **`mobile/local.properties`** (file locale, non versionato) si può impostare ad esempio `tsm.api.baseUrl=http://127.0.0.1:3000/` e, sul PC con il device collegato, eseguire `adb reverse tcp:3000 tcp:3000` così la porta 3000 del telefono punta al backend sul computer. Dopo ogni modifica a `local.properties` serve **ricompilare/reinstallare** l’app.
- **Alternativa senza `adb reverse`:** usare l’**IP LAN del PC** nella stessa proprietà e consentire quell’host in **`network_security_config.xml`** per HTTP in sviluppo.

### Sicurezza di rete e client HTTP

- **`network_security_config.xml`**: in sviluppo permette HTTP verso host locali (solo per test; in produzione si userà HTTPS).
- **`TsmApiClient`**: crea un’istanza Retrofit con Gson per JSON; da qui si ottiene `TsmApiService` per le funzioni API.

### Variabili d’ambiente del backend (per testare login e registrazione)

L’app **non** contiene segreti del server. Per far funzionare il **login** sul backend serve almeno **`JWT_SECRET`** nel file **`backend/.env`** (copia locale da incollare dal **Google Docs** condiviso dal team). La **registrazione** (`POST /users`) può andare a buon fine anche senza JWT; senza `JWT_SECRET` il login fallisce alla generazione del token. Il file **`.env` non va committato** (è in `.gitignore`); dopo ogni modifica al contenuto riavviare `npm run dev`.

---

## 7. Cosa non è ancora stato fatto (prossimi passi tipici)

- Inviare il JWT nelle chiamate API protette (interceptor OkHttp `Authorization: Bearer …`).
- Rafforzare la conservazione del token (es. **EncryptedSharedPreferences** / DataStore).
- Sostituire la **Home** placeholder con le schermate reali del prodotto.
- Gestione sessione all’avvio (utente già loggato → salto del flusso auth).
- Test automatici (unitari / UI) su login e registrazione.

---

## 8. Dove approfondire senza essere esperti Android

- Documentazione ufficiale **Jetpack Compose**: [developer.android.com/develop/ui/compose](https://developer.android.com/develop/ui/compose)
- **MVVM** in Android: ViewModel + osservazione dello stato (nel nostro caso `StateFlow` + `collectAsStateWithLifecycle`).
- **Retrofit**: client REST che si interfaccia con le route Express del backend Node.js.

---

## Riepilogo in una frase

È stata creata una **base di app Android moderna** (Gradle 9, Compose, navigazione auth, tema, login e registrazione collegati alle API del backend, salvataggio JWT locale), organizzata per **MVVM** e pronta per estendere le funzionalità dopo l’accesso.
