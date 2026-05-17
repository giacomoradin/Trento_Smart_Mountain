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
| **`TsmApplication.kt`**    | `Application`: **`TokenStorage`** singleton, **`TsmDatabase`** Room (`tsm.db`), init **`TsmApiClient`**.                                                                                                     |
| **`ui/theme/`**            | Colori e tema Material 3 (`TsmTheme`).                                                                                                                                                    |
| **`ui/navigation/`**       | Destinazioni (`Routes`) e grafo di navigazione (`TsmNavHost`): scelta accesso/registrazione → login, registrazione, verifica email → area principale con tab inferiori. |
| **`ui/screens/auth/`**     | **`AuthEntryScreen`**: **Registrazione utente**, **Registrazione rifugio**, **Accedi**.                                                                                                                                    |
| **`ui/screens/login/`**    | Schermata **login** (email e password).                                                                                                                   |
| **`ui/screens/register/`** | **`RegisterScreen`** (utente), **`RegisterRifugioScreen`** (rifugio, placeholder), **Verifica email** dopo `POST /users`.                                  |
| **`ui/screens/main/`**     | **`HikerMainScreen`**: barra inferiore (Home, Sessione, Registra, Profilo) per utenti escursionisti.                                                        |
| **`ui/screens/home/`**     | **Home** con sottotab Social / Attività personali (placeholder).                                                                                          |
| **`ui/screens/registra/`** | Schermata **Registra** (mappa sessione + SOS, placeholder).                                                                                                 |
| **`ui/screens/refuge/`**   | **`RefugeMainScreen`**: dashboard rifugista (IoT, social credit) per JWT con ruolo `rifugio`.                                                               |
| **`ui/screens/session/`**  | **`SessionHubScreen`**: sottotab **Pianifica** / **Unisciti** (placeholder).                                                                              |
| **`ui/screens/profile/`**  | **Profilo**: username da **`ProfileRepositoryImpl`** (cache Room + `GET /users/{id}`), hint offline/refresh, **logout** (JWT + tabella profilo).                                                                                                   |
| **`viewmodel/`**           | **`LoginViewModel`**, **`RegisterViewModel`**, **`ProfileViewModel`**.                                                                                    |
| **`data/remote/`**         | **`TsmApiClient`**, **`TsmApiService`**, **`AuthInterceptor`**, **`JwtDecoder`**, DTO JSON.                                                               |
| **`data/local/`**          | **`TokenStorage`** (JWT cifrato), **`AuthSession`**, **`data/local/db/`** (`TsmDatabase`, `CachedUserProfileEntity`, `ProfileDao`); **`LocalDataSource`** segnaposto per altre cache. |
| **`repository/`**          | Auth, registrazione, **`ProfileRepository`** (`observeCurrentProfile()` → Room + rete).                                                                                         |
| **`service/`**             | Segnaposto per il **Foreground Service** (GPS/BLE, user story futura).                                                                                                                    |

---

## 5. Cosa fa oggi il flusso di accesso

### Schermata iniziale (scelta)

- All’avvio l’app mostra **Trento Smart Mountain** con **Registrazione utente**, **Registrazione rifugio** e **Accedi**.
- È la **destinazione iniziale** del grafo di navigazione (`Routes.AUTH_ENTRY`).

### Login

- Campi **email** e **password**, con messaggi di errore se i dati non sono validi (email vuota/formato, password troppo corta).
- Pulsante **Entra**, stato di **caricamento** durante la richiesta, messaggio di errore se il server non risponde o rifiuta le credenziali.
- Chiamata reale a **`POST /auth/login`** tramite **`AuthRepositoryImpl`**; in caso di successo il **JWT** viene salvato in **`TokenStorage`** (storage cifrato) e si naviga all’**area principale** (`HikerMainScreen` se ruolo escursionista, `RefugeMainScreen` se `rifugio` nel payload JWT), senza poter tornare alle schermate credenziali con il tasto indietro di sistema.
- All’**avvio successivo**, se il JWT salvato è ancora valido in locale (`userId` + `exp`), **`TsmNavHost`** apre direttamente la shell corretta senza ripassare dal login.
- Se l’account non ha ancora completato la **verifica email**, il backend risponde **403**: l’app mostra un avviso dedicato (non un errore generico di credenziali).
- Arrivando dal flusso post-registrazione, il login può ricevere l’**email precompilata** e un promemoria di verifica (`Routes.loginRoute(pendingEmail)`).

### Area principale (dopo login)

- **`HikerMainScreen`** mostra una **barra inferiore** con quattro tab: **Home**, **Sessione**, **Registra**, **Profilo**. **Home** e **Sessione** hanno sottotab (Social / Attività; Pianifica / Unisciti) con placeholder.
- **Registra** è lo scheletro per mappa sessione attiva, gruppo in tempo reale e SOS (contenuti funzionali in arrivo).
- **Profilo** legge prima la **cache Room** (`cached_user_profile`), poi aggiorna con **`GET /users/{id}`** (Bearer da **`AuthInterceptor`**). In assenza di rete o con errore HTTP, resta l’ultimo username salvato con messaggio esplicativo. Il **logout** rimuove JWT e svuota la tabella profilo. Statistiche e attività restano fuori scope.

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

- Persistenza **Room** estesa a **sessioni escursione** e altre entità; code di sync e Hike Packet: vedi [`setup_mobile.md`](setup_mobile.md) e §9.
- Biometrico / PIN per accesso offline (user story **#2**, oltre al JWT cifrato già in uso).
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

L’architettura di prodotto è **offline-first** (*store-and-forward*, vedi README del monorepo). Sul dispositivo: JWT in **`TokenStorage`** (cifrato); **`AuthSession`** per auth/main all’avvio; **`TsmDatabase`** Room con tabella **`cached_user_profile`** per lo snapshot profilo dopo fetch riuscito; **`LocalDataSource`** resta segnaposto per altre tabelle (sessioni, tile, ecc.).

---

## Riepilogo in una frase

È stata creata una **base di app Android moderna** (Gradle 9, Compose, navigazione auth con verifica email, area principale a tab, login e registrazione, profilo con **Room + rete** e logout che pulisce cache sensibile), organizzata per **MVVM** e pronta per sessioni escursione, mappa live e store-and-forward.
