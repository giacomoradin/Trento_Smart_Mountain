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
| **`ui/navigation/`**       | Destinazioni (`Routes`) e grafo di navigazione (`TsmNavHost`): scelta accesso/registrazione → login, registrazione, verifica email → area principale con tab inferiori. |
| **`ui/screens/auth/`**     | Schermata iniziale con pulsanti **Accedi**, **Registrati** e demo **Google** (non integrata).                                                                                              |
| **`ui/screens/login/`**    | Schermata **login** (email e password).                                                                                                                   |
| **`ui/screens/register/`** | **Registrazione** e schermata **Verifica email** dopo `POST /users`.                                                                                      |
| **`ui/screens/main/`**     | **`MainScreen`**: barra di navigazione inferiore e contenuto della tab selezionata.                                                                        |
| **`ui/screens/session/`**  | Scheletro **Sessione** (nuova sessione / sessione attiva).                                                                                                |
| **`ui/screens/map/`**      | Scheletro **Mappa** (posizioni di gruppo in tempo reale, futura).                                                                                         |
| **`ui/screens/profile/`**  | **Profilo** (in questa fase: username da API protetta).                                                                                                   |
| **`viewmodel/`**           | **`LoginViewModel`**, **`RegisterViewModel`**, **`ProfileViewModel`**.                                                                                    |
| **`data/remote/`**         | **`TsmApiClient`**, **`TsmApiService`**, **`AuthInterceptor`**, **`JwtDecoder`**, DTO JSON.                                                               |
| **`data/local/`**          | **`TokenStorage`**: JWT in `SharedPreferences` dopo login; **`LocalDataSource`**: segnaposto per Room / cache offline-first.                                                                 |
| **`repository/`**          | Auth, registrazione, **`ProfileRepository`** (`GET /users/{id}`).                                                                                         |
| **`service/`**             | Segnaposto per il **Foreground Service** (GPS/BLE, user story futura).                                                                                                                    |

---

## 5. Cosa fa oggi il flusso di accesso

### Schermata iniziale (scelta)

- All’avvio l’app mostra **Trento Smart Mountain** con **Accedi**, **Registrati** e un pulsante **Google** solo dimostrativo (messaggio “non ancora disponibile”).
- È la **destinazione iniziale** del grafo di navigazione (`Routes.AUTH_ENTRY`).

### Login

- Campi **email** e **password**, con messaggi di errore se i dati non sono validi (email vuota/formato, password troppo corta).
- Pulsante **Entra**, stato di **caricamento** durante la richiesta, messaggio di errore se il server non risponde o rifiuta le credenziali.
- Chiamata reale a **`POST /auth/login`** tramite **`AuthRepositoryImpl`**; in caso di successo il **JWT** viene salvato in **`TokenStorage`** e si naviga all’**area principale** (`MainScreen`), senza poter tornare alle schermate di autenticazione con il tasto indietro di sistema.
- Se l’account non ha ancora completato la **verifica email**, il backend risponde **403**: l’app mostra un avviso dedicato (non un errore generico di credenziali).
- Arrivando dal flusso post-registrazione, il login può ricevere l’**email precompilata** e un promemoria di verifica (`Routes.loginRoute(pendingEmail)`).

### Area principale (dopo login)

- **`MainScreen`** mostra una **barra inferiore** con tre tab, da sinistra a destra: **Sessione**, **Mappa**, **Profilo**.
- **Sessione** e **Mappa** sono scheletri con testo descrittivo; i contenuti (sessione attiva, mappa live) arriveranno in step successivi.
- **Profilo** carica lo **username** dell’utente loggato tramite JWT + **`GET /users/{id}`** (header `Authorization: Bearer` aggiunto da **`AuthInterceptor`**). Statistiche e attività restano fuori scope in questa fase; è disponibile il **logout** locale (cancellazione JWT e ritorno al flusso auth).

### Registrazione e verifica email

- Campi **username**, **email**, **password** e **conferma password** (la conferma è solo validazione lato app).
- Chiamata a **`POST /users`** tramite **`RegistrationRepositoryImpl`** (password in chiaro nel body; hash sul server). Il backend crea l’account con `isVerified: false` e tenta l’invio dell’email di verifica (SMTP in **`backend/.env`**).
- In caso di successo l’app apre **`EmailVerificationPendingScreen`** con l’indirizzo usato e i passi per aprire il link ricevuto; da lì si passa al login. La conferma avviene tramite **`GET /auth/verify/:token`** (di solito dal browser, link nell’email).
- Conflitto (email/username già usati) o errore di rete: messaggio in UI sulla schermata di registrazione.

---

## 6. Rete, backend e configurazione di sviluppo

### URL del server nell’app

- **`BuildConfig.BASE_URL`**: URL base del server Node.js, generato in fase di build da Gradle.
- **Default emulatore:** `http://10.0.2.2:3000/` (`10.0.2.2` è il localhost del PC visto dall’emulatore Android).
- **Telefono fisico (USB debug):** in **`mobile/local.properties`** (file locale, non versionato) si può impostare ad esempio `tsm.api.baseUrl=http://127.0.0.1:3000/` e, sul PC con il device collegato, eseguire `adb reverse tcp:3000 tcp:3000` così la porta 3000 del telefono punta al backend sul computer. Dopo ogni modifica a `local.properties` serve **ricompilare/reinstallare** l’app.
- **Alternativa senza `adb reverse`:** usare l’**IP LAN del PC** nella stessa proprietà e consentire quell’host in **`network_security_config.xml`** per HTTP in sviluppo.

### Sicurezza di rete e client HTTP

- **`network_security_config.xml`**: in sviluppo permette HTTP verso host locali (solo per test; in produzione si userà HTTPS).
- **`TsmApiClient`**: Retrofit + OkHttp; **`AuthInterceptor`** allega il JWT alle richieste protette. Inizializzato in **`TsmApplication`**.

### Variabili d’ambiente del backend (per testare login e registrazione)

L’app **non** contiene segreti del server. Per far funzionare **login e verifica email** sul backend servono almeno **`JWT_SECRET`** e la configurazione **SMTP** nel file **`backend/.env`** (copia locale da incollare dal **Google Docs** condiviso dal team). La **registrazione** (`POST /users`) può creare l’utente anche se l’invio email fallisce (errore loggato lato server); senza verifica completata il login resta bloccato con **403**. Senza `JWT_SECRET` il login fallisce anche dopo la verifica. Il file **`.env` non va committato** (è in `.gitignore`); dopo ogni modifica al contenuto riavviare `npm run dev`.

---

## 7. Cosa non è ancora stato fatto (prossimi passi tipici)

- Persistenza locale **offline-first** (Room, code di sync, Hike Packet): vedi [`setup_mobile.md`](setup_mobile.md) e il piano in §9.
- Rafforzare la conservazione del token (es. **EncryptedSharedPreferences** / DataStore) e **sessione all’avvio** (JWT valido → salto del flusso auth).
- Contenuti reali per **Sessione** (API `/api/v1/sessions` già sul backend) e **Mappa** (posizione in tempo reale).
- Ampliare **Profilo** (progressi, attività); logout sicuro lato server oltre alla cancellazione locale del JWT.
- OAuth Google/Facebook; deep link o riinvio email dalla app.
- Test automatici (unitari / UI) su registrazione, verifica email, login e tab principali.

---

## 8. Dove approfondire senza essere esperti Android

- Documentazione ufficiale **Jetpack Compose**: [developer.android.com/develop/ui/compose](https://developer.android.com/develop/ui/compose)
- **MVVM** in Android: ViewModel + osservazione dello stato (nel nostro caso `StateFlow` + `collectAsStateWithLifecycle`).
- **Retrofit**: client REST che si interfaccia con le route Express del backend Node.js.

---

## 9. Persistenza locale (stato attuale e obiettivi di progetto)

L’architettura di prodotto è **offline-first** (*store-and-forward*, vedi README del monorepo). Sul dispositivo oggi c’è solo il JWT in **`TokenStorage`** (`SharedPreferences`); **`LocalDataSource`** è un segnaposto. Le user story di riferimento nel backlog di progetto includono tra le altre: **#2** accesso offline con JWT in secure storage, **#10** cache coordinate GPS in Room/SQLite, **#35** tetto **50 MB** con eviction **FIFO**, **#37** coda eventi offline, **#39** Hike Packet (GeoJSON + map tiles). Il piano operativo per il team è descritto in [`setup_mobile.md`](setup_mobile.md) (sezione persistenza).

---

## Riepilogo in una frase

È stata creata una **base di app Android moderna** (Gradle 9, Compose, navigazione auth con verifica email, area principale a tab, login e registrazione collegate al backend, profilo con username e logout), organizzata per **MVVM** e pronta per persistenza offline-first, sessione escursione e mappa live.
