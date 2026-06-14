# Trento Smart Mountain — Stato del progetto

## 🩹 Sprint 3 — 12 giugno 2026 (round bug fix dal field testing: zoom, autopause, storie live, sync partecipanti, navigazione, area rifugista)

> Backend **330/330 test verdi** (22 suite, +5 nuovi); mobile **`compileDebugKotlin` BUILD SUCCESSFUL**. Branch `UI`, lavoro locale (no push). Fingerprint build visibile in welcome: **`v2.5 · Sync`**.

**Regressione "attività non sincronizzata" + "Errore imprevisto sui percorsi suggeriti" — causa radice: R8/minify senza keep-rule (build release):** durante il pass prestazioni era stato attivato `isMinifyEnabled = true` + `isShrinkResources = true` in `app/build.gradle.kts`, ma `proguard-rules.pro` teneva solo OkHttp. R8 **offusca i nomi dei campi** dei DTO Gson **privi di `@SerializedName`** → in release:
> - `CreateActivityRequest`/`ActualStats`/`RoutePoint` serializzati con nomi `a/b/c…` → backend `422` → **attività mai sincronizzata** (né auto né con "Sincronizza", perché ogni retry rifà la stessa POST corrotta);
> - `SentieroListItemDto`/wrapper sentieri non parsati → null → **"Errore imprevisto"** nel picker percorsi.
> Login/feed restavano OK perché i loro DTO **hanno** `@SerializedName`. **Fix**: `isMinifyEnabled`/`isShrinkResources` riportati a `false` (stato noto-buono; l'utente non aveva richiesto la minificazione) **e** `proguard-rules.pro` ora contiene keep-rule complete (package `data.remote.dto.**`, Retrofit, Gson, `@SerializedName`) così R8 è riattivabile in sicurezza. Diagnostica aggiunta: `SyncManager` logga ora l'`errorBody()` sul fallimento upload; il ViewModel del picker logga la classe d'eccezione (tag `RoutePicker`).

**Picker "Percorsi suggeriti" — redesign LIST-FIRST (era "grezzo", mappa con tutti i pin):**
> Riscritto `SessionRoutePickerDialog` con la **lista come protagonista**. La mappa **NON** compare più negli step di navigazione (era il "muro di pin" confuso): appare **solo sul dettaglio** del sentiero scelto, dove mostra la traccia reale (polyline + start/end). La lista non è mai limitata (tutte le destinazioni/sentieri, scrollabili).
> - **Step 1 destinazioni**: ricerca ("rifugio, malga, cima…") + filtri collassabili (difficoltà/dislivello/distanza/tempo) + LazyColumn di **glass card** (`TsmGlassCard`, press-scale) con icona-luogo accent, quota e conteggio sentieri.
> - **Step 2 sentieri**: glass card con **badge difficoltà colorato** (CAI T/E/EE/EEA via `difficultyColor`), distanza/tempo/partenza.
> - **Step 3 dettaglio**: mappa traccia (240 dp) + griglia `InfoChip` (difficoltà/distanza/dislivello/tempi/quota) + CTA premium `TsmGradientButton` "Usa questo tracciato".
> - Header con sottotitolo-conteggio; stati `LoadingBox`/`EmptyBox`/`ErrorRow` chiari; log con tag `RoutePicker`.

**Pass grafico schermata-per-schermata (`v2.6 · Glass`):** unificazione progressiva delle schermate secondarie ai materiali del design system (eliminate le card flat `Color(0xFF2C2C2E)` "grezze"):
> - **Profilo**: tutte le voci ora su `TsmGlassCard` (gradiente+hairline+press-scale) con **icon-chip** accent coerenti al posto degli emoji-in-box; logout come glass card "danger".
> - **Formazione**: category card glass con chip-icona nella tinta della categoria + badge "✓ Completato".
> - **Quiz**: domanda su glass, opzioni con stato selezionato evidente (bordo/sfondo/lettera accent), CTA `TsmGradientButton` verde.
> - **Esito quiz**: glow dietro il ring punteggio, stat card glass.
> - **Bacheca (Badge)**: aurora di sfondo, stat/badge/certificati su glass; emoji incorniciato in disco tinto del tier; certificato con bordo oro + shimmer.
> - **Sfide**: aurora + card glass con chip-stato tinto; **ChallengeDetail** (header/classifica/invito su glass) e **CreateChallenge** (aurora + CTA gradiente) elevati.
> - **Account**: aurora, voci profilo/sicurezza su glass con icon-chip, "Elimina account" glass-danger, CTA `TsmGradientButton`; **Cambia password** e **Elimina account** con CTA gradiente; warning-card glass-danger.
> - **Onboarding** (3 step via `OnboardingStepScaffold`): aurora di sfondo + CTA "Salva e continua" a gradiente.
> - **Esito NFC**: glow dietro l'icona-esito, card crediti glass, CTA gradiente nel colore dell'esito.
> - **Attività personali** (tab Personale): yearly stats card, grafico mensile e righe attività su `TsmGlassCard`; icona attività in chip accent.
> - **Auth/registrazione**: Register, RegisterRifugio, ForgotPassword (glow su "inviato"), EmailVerificationPending (hero mail + glow), NfcScan (aurora, glow "campo NFC" dietro l'anello, card stato glass) — tutti con sfondo aurora e CTA `TsmGradientButton`.
> Sessione (hub/detail) era già su materiali glass dalla fase C7. **Pass schermata-per-schermata completato** (`v2.7 · Glass`); restano solo rifiniture puntuali se richieste.

**Intensificazione centralizzata del design system (`v2.8 · Depth`):** un'unica modifica ai componenti-radice che si propaga a TUTTA l'app (nessun ritocco schermata-per-schermata):
> - **`TsmGlassCard`**: ora ha **ombra reale** (elevazione 10 dp a riposo → 2 dp al press, animata), **sheen** superiore (riflesso di luce sul bordo alto) e **bordo più luminoso** (alpha 0.06→0.10 a riposo, 0.22→0.32 al press). Le card "staccano" dallo sfondo invece di galleggiare piatte.
> - **`TsmGradientButton`**: ombra animata (galleggia, 10 dp→2 dp al press), **gloss** sulla metà superiore, bordo 0.18→0.28 → CTA più materiche.
> - **`TsmGlow`**: gradiente radiale con mid-stop → alone più **pieno** e presente (stessi call-site).
> - **`TsmAuroraBackground`**: alpha blob più decise (0.42/0.30/0.22 → 0.52/0.38/0.30) e particelle più vive (0.18→0.24) → più profondità dietro il vetro.
> - **Feed card** allineata (elevazione 10 dp + sheen) per coerenza con la massima superficie visibile.

**Effetti "WOW" / extreme premium (`v2.9 · WOW`)** — nuovi componenti riusabili in `ui/components/TsmEffects.kt`:
> - **`TsmRewardBurst`**: esplosione one-shot di coriandoli/scintille nei colori brand (ventaglio dal centro-alto, rotazione + gravità, dissolvenza, ~1,3 s, una sola `Animatable`). Cablata nei momenti-premio: **quiz superato** (`QuizResultScreen`) e **checkpoint NFC riuscito** (`NfcResultScreen`).
> - **`Modifier.tsmSweepBorder`**: banda di luce che **scorre lungo il bordo** (gradiente diagonale traslante, fase quantizzata ~12/s) per gli elementi-hero. Applicata alla **card SOCIAL CREDITS** del profilo (effetto "vetro vivo").
> Entrambi pensati per essere performanti (no full frame-rate sui loop, burst attivo solo durante la celebrazione) e riutilizzabili su altri traguardi futuri.

**Estensioni "wow" (`v3.0 · WOW+`):**
> - **`TsmRewardBurst`** ora anche a **fine attività salvata** (`RegistraScreen`, trigger su `activitySaved` del VM, consumato e resettato) e all'apertura della **Bacheca** (`BadgesScreen`, una sola volta a sessione se l'utente ha già badge/certificati).
> - **`Modifier.tsmSweepBorder`** anche sulla **CTA "Accedi"** della welcome (primo impatto) e sui **certificati** (bordo oro che scorre, in aggiunta allo shimmer).
> - **Count-up animato** (`TsmAnimatedCounter`) su **SOCIAL CREDITS** e KPI (uscite/km/dislivello) del profilo: i numeri salgono da 0 all'apertura.

**Area rifugio allineata al sistema (`v3.2 · Refuge`):** la **Dashboard IoT** (`RefugeMainScreen`) passa dalle flat `Surface(DashboardCard)` a **`TsmGlassCard`** (sensori, edge-node, passaggi, entry Rifiuti) → eredita ombra+sheen+bordo luminoso e il fix particelle dell'aurora; count-up sui **crediti del giorno** + icon-chip accent sull'entry Rifiuti. `RefugeProfileScreen` e `WasteSimulatorScreen` usavano già `TsmGlassCard`/`TsmGradientButton` → ereditano automaticamente l'intensificazione (profondità, CTA più materiche). Area rifugio ora coerente al 100% con l'area escursionista.
> **Wow rifugio:** bordo "luce viaggiante" (`tsmSweepBorder`) sulla card-CTA **Rifiuti & Logistica**; **`TsmRewardBurst`** quando i **crediti del giorno salgono** (un escursionista passa al checkpoint → scintille), con guardia anti-trigger al primo caricamento.

**Tier A — rifinitura verso la release (`v3.3 · Polish`):**
> - **Skeleton loaders unificati**: `ListSkeleton` (shimmer) sostituisce lo spinner nudo su Badge/Sfide/Formazione; le liste social (notifiche/ricerca/follow) e il feed già lo usavano → caricamenti coerenti in tutta l'app.
> - **Splash Android 12+**: nuovo `values-v31/themes.xml` con `windowSplashScreenBackground = tsm_icon_bg` → il cold-start su device moderni mostra l'icona adattiva TSM sullo sfondo brand (prima il sistema ignorava `windowBackground`).
> - **R8/minify**: lasciato OFF per la demo (scelta di sicurezza dopo la regressione Gson); le keep-rule in `proguard-rules.pro` sono complete e pronte → riattivabile post-test on-device.
> - Restano opzionali (manutenibilità, bassa visibilità): audit colori hardcoded→`TsmColors`, i18n delle stringhe nuove, versioning store reale.

**Tier C/E/F — motion & accessibilità (`v3.4 · Motion`):**
> - **Reduce-motion centralizzato** (E): nuovo `rememberReduceMotion()` (legge `ANIMATOR_DURATION_SCALE==0`); tutti gli effetti decorativi — aurora (blob+particelle), `tsmSweepBorder`, `TsmRewardBurst`, `TsmPulseGlow`, `tsmShimmer` — diventano statici quando l'utente ha "Rimuovi animazioni" attivo. Una sola sorgente, propaga ovunque.
> - **Reveal d'ingresso liste** (C): `Modifier.tsmEnterReveal()` (fade + slide-up al primo ingresso in composizione, reduce-motion aware) applicato alle card del feed → la lista "prende vita" allo scroll/apertura.
> - **Splash v31** + **icona monochrome** (F) già a posto; R8 OFF per sicurezza (keep-rule pronte).
> Restano per i prossimi giri: `contentDescription` per TalkBack.

**Hero header coerente su tutte le tab (`v4.0 · Hero everywhere`):** nuovo componente riusabile `TsmHeroHeader` (overline accent + titolo grande + slot azioni) + `TsmHeroActionChip` (chip glass circolare con badge) in `ui/components/TsmHeroHeader.kt`. Applicato alle schermate top-level:
> - **Feed** (header scrollabile, già fatto); **Profilo** ("TRENTO SMART MOUNTAIN" / "Profilo" + chip Vedi-profilo/Impostazioni); **Sessioni** (hero in cima alla colonna scrollabile); **Dashboard rifugio** (overline nome-rifugio ora in accent, già con titolo grande); **Personale** (overline "LE MIE ATTIVITÀ" ora in accent).
> Tutti gli hero stanno in cima a contenuto scrollabile → **scorrono via** scrollando giù (niente header fissi). Le schermate di **dettaglio** (Formazione, Badge, Sfide, Account, NFC…) mantengono la `TopAppBar` con back — è la UX corretta per la sotto-navigazione, non un'incoerenza.

**Ottimizzazione finale pre-release — build a zero warning (`v4.2 · Optimize`):**
> Pulizia mirata guidata dai warning di un clean-compile (non alla cieca). Backend **330/330 test verdi (22 suite)**; mobile `compileDebugKotlin` **e** `assembleRelease` **BUILD SUCCESSFUL, 0 warning**.
> - **Icone RTL-correct** (`AutoMirrored`): `DirectionsWalk` (ActivityList), `DirectionsRun` (Registra), `Send` (StoryComposer) migrate da `Icons.Outlined/Filled.*` deprecate a `Icons.AutoMirrored.*` → si specchiano correttamente in lingue right-to-left.
> - **`LocalLifecycleOwner`**: import spostato da `androidx.compose.ui.platform` (deprecato) a `androidx.lifecycle.compose` (SessionDetail, SessionHub).
> - **`@OptIn(ExperimentalCoroutinesApi)`** esplicito dove serviva (`resetReplayCache` nei 3 bus singleton NfcTagBus/SessionStart/SessionStop; `flatMapLatest` in ProfileViewModel) → niente più warning opt-in.
> - **`menuAnchor()`** (SosDialogs): overload deprecato → `menuAnchor(MenuAnchorType.PrimaryNotEditable)`.
> - **`getParcelableExtra`** (NfcUtils): ora con branch API-33 (`getParcelableExtra(name, Tag::class.java)` su Tiramisu+, vecchio overload `@Suppress`-ato sotto) → robusto e senza deprecazione.
> - **Condizioni ridondanti rimosse** ("always true"): `routePoints != null` già implicato da `showMap` (TsmRouteElevationPager); `decor == null` ridondante nel ramo `when` (StoryViewer). Più un **import morto** rimosso (Registra).
> - **`androidx.security:security-crypto`** (TokenStorage): libreria deprecata da Google **senza sostituto drop-in** → `@file:Suppress("DEPRECATION")` con commento esplicativo; la migrazione (Tink/DataStore) resta **debito tecnico post-release** (funzionalità intatta, token cifrati a riposo).
> - **R8/minify**: **RIATTIVATO** (`isMinifyEnabled`/`isShrinkResources = true`) per il test on-device pre-release — APK release scende a ~6.3 MB, `:app:minifyReleaseWithR8` senza "Missing class". Le keep-rule coprono l'intero `data.remote.dto.**` (incl. EmergencyDto + adapter Gson). Release firmata con **debug keystore** (`signingConfig = signingConfigs.debug`) per installabilità ⚠️ da sostituire con upload-key reale prima della pubblicazione. **Da validare on-device** (sync attività + picker percorsi + scan SOS): se un flusso fallisce in release ma non in debug → keep-rule mancante, rimettere OFF o aggiungere la regola.
> - **Security review** (`/security-review`, backend + mobile, focus feature SOS/Emergency): **nessuna vulnerabilità ad alta confidenza**. Verificati senza falle: IDOR/authorization su `emergencyService` (un partecipante non-leader/non-mittente vede un SOS solo se `SHARED_WITH_GROUP`; il `profileSnapshot` medico è ricostruito server-side, non fidato dal client), NoSQL injection (mongoSanitize globale + Joi tipizzato), parsing BLE non fidato (length-guard, Kotlin memory-safe), decode base64 avatar (nessun path costruito dal data-URI), componenti Android esportati (`SosBeaconService`/FileProvider `exported=false`).
> - **Hardening backend** (unico intervento concreto emerso): nuova `emergencyCoordinatesSchema` dedicata in `validationMiddleware.js` — per il SOS le coordinate sono `type:"Point"` + `[lon, lat]` obbligatorie e nei range reali (prima il `geoPointSchema` condiviso/lasco lasciava passare `coordinates: {}` → potenziale `ValidationError` 500 dal model; ora 422 pulito alla validazione). Sessione `startPoint`/`endPoint` invariati.

**Test backend enterprise-grade + bug trovato (`351/351`, 22 suite):** `emergency.test.js` era l'outlier (1 sola assert di error-path vs 5-17 delle suite pari): aggiunti **+21 test** di authN (401), authZ per ruolo, **IDOR** (un partecipante non-mittente/non-leader non legge un SOS `ACTIVE` non condiviso; lo vede solo dopo `share_with_group`), macchina a stati (già-chiuso→409, transizioni invalide→409, `ack`→`hasUnacked` false) e validazione (coordinate malformate/out-of-range→422, lock-in dell'hardening sopra). Il giro ha **scovato un bug reale**: `emergencyService.getEmergencyById` chiamava `assertCanViewEmergency` con `senderUserId` **già popolato**, quindi `senderUserId.toString()` (documento User) non eguagliava mai l'hex dell'utente → **il mittente riceveva 403 leggendo il proprio SOS** (incoerente con la lista, che invece glielo mostra). Fix: normalizzazione `(senderUserId?._id || senderUserId).toString()` come nel resto del codice. Inoltre, sostituite in `weather.test.js` 2 assert deboli che accettavano `5xx` come successo (mascheravano regressioni) con assert onesti sulla barriera 401/403; rimosso un `console.log` di debug residuo in `hikeSession.test.js`.

**Coverage + CI (verso il 100%, iterativo):**
> - **Coverage era ROTTA (riportava 0%)**: con ESM nativo (`transform: {}`) il provider di default `babel` non instrumenta nulla. Fix in `jest.config.js`: `coverageProvider: 'v8'` → ora misura davvero. Baseline reale emersa: **84% stmts / 64% branch**.
> - **Gate di copertura** in `jest.config.js` (`coverageThreshold`): `npm test` (e quindi la CI) fallisce se la copertura scende. **Ratchet progressivo** man mano che si aggiungono test — attuale **88/67/84/88** (stmts/branch/funcs/lines), poco sotto i valori reali.
> - **CI già esistente e solida** (`.github/workflows/ci.yml`: job backend `npm ci`→`npm test`→`npm audit`, job mobile `testDebugUnitTest`+lint). Aggiunto l'**upload del report di coverage** come artefatto.
> - **Moduli portati a 100%** (statement+branch+funcs+lines): `KMLparser.js` (0→100, parser puro), `refugeService.js` (18→100, handler con mock email + spyOn per i rami 500), `authService.js` (46→100, verify/login/refresh/logout/forgot/reset con mock email+refreshTokenService), `emailService.js` (44→100, path Brevo reale via mock di `global.fetch` + override `NODE_ENV`).
> - **Stato suite**: **431 test / 26 suite verdi**, coverage globale **~89% stmts / 67.6% branch / 84.6% funcs**.
> - **Nota tecnica ESM**: per usare `jest.fn/spyOn/unstable_mockModule` nei test serve `import { jest } from "@jest/globals"` (in ESM non è globale); per i moduli con cache a livello-modulo (es. `weatherService._venuesCache`) serve `jest.resetModules()` + re-import dinamico per testare cache-miss/hit in modo isolato.
> - **Rimanente per il 100% letterale** (multi-sessione): `weatherService` (cache+fetch+DB), `quizService`/`checklistService`/`notificationService`/`socialService`/`storyService` e i rami `catch→500` di vari handler; route `creditsRoutes`/`nfcRoutes`/`badgeRoutes`/`quizRoutes`; rami di `hikeSessionService`/`hikeSessionRoutes`; `middleware` (rateLimit/security/error), `app.js`, modelli `hikeSession`/`sentiero`.

**Giro di polish + dedup (`v4.1 · Polish`):**
> - **Reveal d'ingresso esteso per coerenza**: `Modifier.tsmEnterReveal()` (fade + slide-up, reduce-motion aware) ora anche sulle voci di **Attività personali** (`ActivityListScreen`), **Badge + Certificati** (`BadgesScreen`) e **Sfide** (`ChallengesScreen`) — prima era solo il feed. Tutte le liste principali "prendono vita" allo scroll/apertura con lo stesso linguaggio di movimento.
> - **Dedup componente**: il chip d'azione privato `HeaderActionChip` del feed è stato rimosso; `FeedHeader` usa ora il `TsmHeroActionChip` condiviso (`ui/components/TsmHeroHeader.kt`) → un'unica sorgente per i chip glass circolari, niente duplicazione.
> - Build verde: `compileDebugKotlin` **e** `assembleRelease` **BUILD SUCCESSFUL**.

**Redesign hero Home/Feed (`v3.8 · Hero feed`):** la riga piatta in cima al feed è ora un vero **hero** — overline brand "TRENTO SMART MOUNTAIN" + titolo grande "Community", azioni classifica/notifiche come **chip glass circolari** accent (badge non lette), e barra di ricerca su `TsmGlassCard` (prima un `Surface` grigio hardcoded). È la prima cosa visibile aprendo l'app → cambiamento inequivocabile (a differenza dei pass di micro-polish precedenti, percepiti poco dall'utente).

**Tier D — mappa scura: TENTATA e RIPRISTINATA (`v3.7`).** Il filtro "notte soft" sui tile OpenTopoMap è stato giudicato brutto dall'utente e **rimosso**: la mappa è tornata al basemap topografico originale (chiaro) su tutte le 4 superfici. Lezione: scurire un topografico chiaro non funziona esteticamente; una mappa "premium scura" vera richiederebbe un tile-source dark dedicato (Carto/Stadia, con API key/policy) — fuori scope per la release.

**Tier B — stati & feedback (`v3.5 · Feedback`):** nuovi componenti riusabili in `ui/components/TsmDialogs.kt`:
> - **`TsmAlertDialog`** glass (card + icon-chip + CTA gradiente, `destructive`=rosso): migrati i confirm puliti — FeedCard "Rimuovi dal feed", PostDetail, Profilo "rimuovi avatar", Elimina account, Elimina attività. Restano custom (per scelta) i dialog ricchi (salva-attività con KPI+nome, ShareActivityDialog con caption) e i dialog SOS (UI di sicurezza).
> - **`TsmSnackbar`** brandizzata (glass) applicata via slot `snackbar` ai `SnackbarHost` esistenti (AccountEdit, CambiaPassword, ChallengeDetail, CreateChallenge). I `Toast` di sistema (share/sync) restano funzionali — migrazione a snackbar opzionale, più invasiva.

**Tab Registra — micro-interazioni sui comandi (pausa/stop/partecipanti/SOS):** `GlassFab` (condiviso dai 4 controlli) ora ha **press-scale 0.92** + bordo più definito al tocco. Effetto **contenuto**: scala ≤ 1 e nessun glow/alone esterno → la silhouette del pulsante **non si allarga mai** (vincolo esplicito utente). Il FAB REC (`RegistraGlowFab`) mantiene il suo glow pulsante intenzionale.

**Zoom mappa — causa radice DEFINITIVA (terza segnalazione):** `isTilesScaledToDpi = false` su tutte le MapView: su display ~450 dpi le tile 256 px erano renderizzate 1:1, quindi **anche al massimo zoom la mappa appariva "lontana" e illeggibile**. Ora `true` ovunque (`TsmMapView`, `TsmSentieriMapView`, `TsmRouteMapPreview`, `StoryMapSnapshotter`): resa ~2,8× più vicina a parità di livello, oltre ai fix precedenti su auto-fit (`routeKey`/`contentKey`).

**Autopause in timeline — anello mancante trovato:** `SessionCommandRepository.completeOrUpload` inviava `totalSeconds = movingSeconds` (hardcoded) → il server salvava totale==movimento e al re-import dal sync l'evento "Pause" non poteva mai comparire. Ora il chiamante passa il **wall-clock reale** start→stop (stesso calcolo di `finalize`).

**Storie "non dinamiche" — causa radice:** per le storie FOTO il composer azzerava `editorDecor` e cuoceva mappa/traccia/testo in un JPEG statico (`editorDecor = if (isVideo) decor else null`). Ora `editorDecor` + `routePolyline` sono inviati SEMPRE e l'exporter non cuoce più gli overlay (`bakeOverlays=false`): il viewer renderizza mappa (frecce animate ~30 Hz), traccia e testo **live** sopra il media; il JPEG resta come sfondo/fallback. `StoryViewerDecorations` renderizza ora il testo anche senza traccia.

**Sessioni — il partecipante non poteva sincronizzare/condividere (evidente dopo il force-close del leader):**

- Root cause della share: il `remoteId` del partecipante era il **sessionId** → `POST /activities/:sessionId/share` → `404 ACTIVITY_NOT_FOUND`. Nessuna Activity personale esisteva server-side per i partecipanti.
- Fix architetturale (ADR-001): al termine del tracking di una sessione, il client crea anche la **copia personale** (`POST /activities` con `sourceSessionId`) — idempotente per `(userId, sourceSessionId)`, esclusa dalle stats aggregate (niente doppio conteggio), senza ri-accredito crediti. `markSynced` usa l'id della copia → la share del partecipante funziona, anche a sessione già chiusa dal capogruppo (il backend accetta `complete` post-COMPLETED e la copia personale è indipendente dallo stato sessione). `SyncManager` allineato (retry idempotenti, traccia personale inclusa).

**Freeze "schermata blu col logo" (soprattutto eliminando un'attività):**

- Root cause: il dialog "Elimina" non si chiudeva al tap e la navigazione attendeva la rete (cold start Render 30–100 s) → tap ripetuti accodavano più `popBackStack()` → **back stack vuoto** → restava visibile solo il window background (blu + logo) = app percepita come bloccata sullo splash.
- Fix: dialog chiuso subito al tap, guardia di rientranza in `deleteActivity`, e **`safePop()`** in tutto il NavHost (38 call-site: mai rimuovere l'ultima destination).

**Bloccato sul profilo utente dopo follow:** double-tap sull'avatar impilava due copie identiche della schermata (il back "non funzionava"). Ora `openUserProfile()` con `launchSingleTop` su tutti i 9 entry-point + safePop.

**Dettaglio attività:** valore DISTANZA da Cyan→`TsmColors.Info` (era percepito grigio su grigio); sottotitoli e orari timeline da `Color.Gray`→`TextSecondary`.

**Area rifugista:**

- **Insets**: dashboard e simulatore rifiuti non sbordano più sotto status bar / tasti di navigazione (`tsmStatusBarPadding` + `tsmNavigationBarPadding`).
- **Foto della struttura**: nuovo `Refuge.avatarUrl` + `PATCH /api/v1/refuge/profile` (Joi `refugeProfileUpdateSchema`, stesso formato/limiti dell'avatar hiker, `""` per rimuovere; update via modello discriminator `Refuge`, mai `User.findByIdAndUpdate`). Mobile: picker + compressione (pipeline `AvatarUtils`), avatar nella scheda profilo e nell'header dashboard; esposto in `GET /refuge/dashboard`.
- **Impostazioni**: sezione con "Cambia password" (endpoint `change-password` role-agnostic) accanto a bacheca e logout.
- Il refactor profondo di design dell'area rifugista (S3-14) resta pianificato come task dedicato.

**Login dopo switch account — loop "credenziali non valide" / "serve cancellare la cache":**
- In `cacheDir` non c'è nulla che influenzi il login (niente HTTP cache; token in EncryptedSharedPreferences): il "fix" del cancella-cache era il TEMPO trascorso + la ridigitazione manuale. Cause reali, tutte corrette:
  1. **`/auth/refresh` condivideva l'ISTANZA di `loginLimiter`** → contatore per-IP unico: refresh falliti (token stantio dopo switch account, retry dell'Authenticator) esaurivano i 10 tentativi e bloccavano anche login CORRETTI per 15 min. Ora `refreshLimiter` dedicato (max 60 failed/15 min).
  2. **Autofill stantio**: dopo il login rifugio il campo password (mascherato) poteva conservare la password dell'account precedente → 401 reali in loop. Ora su 401 il client **azzera la password** e chiede il re-inserimento manuale.
  3. **429 illeggibile**: il body del limiter (`{error, retryAfter}`) non veniva parsato → "Accesso non riuscito (429)". Ora `ApiMessageBody` parse `error`+`retryAfter` e il client mostra "Troppi tentativi: attendi ~X min".
  4. **Logout senza revoca server-side**: `ProfileViewModel.logout` non chiamava `POST /auth/logout` → il refresh token restava valido 30 giorni. Ora revoca best-effort prima del clear locale.

**Login — chiusura diagnosi (probe sul backend deployato `xz7u`):** il server risponde 200 al login demo anche con casing diverso (collation fix DEPLOYATO) e l'account personale ESISTE sul DB (`GiacomoRadin`, verificato via `GET /users/search` con token demo) → i 401 osservati nel logcat sono **password mismatch reali**. Causa abilitante trovata nel client: i campi password di **Login/Registrazione hiker/Registrazione rifugio** non avevano `KeyboardType.Password` → autocorrect/suggerimenti attivi su campo mascherato potevano alterare la password digitata (anche alla REGISTRAZIONE → password salvata già "corrotta", accessi successivi solo via autofill). Fix: `KeyboardOptions(Password, autoCorrectEnabled=false)` su tutti i campi password + `KeyboardType.Email/capitalization None` sull'identificativo login. Recovery utente: reset password via "Password dimenticata".

**Pass grafico premium (S3-14 + WOW):** applicato il design system glass (TsmGlassCard/TsmGradientButton/TsmAccentRule/TsmGlow/shimmer) dove mancava:
- **Login**: form in glass card + CTA a gradiente con stato loading integrato.
- **Welcome (AuthEntry)**: CTA "Accedi" a gradiente con `TsmPulseGlow` dietro.
- **Simulatore Rifiuti**: restyle completo — sezioni con header iconati + accent rule (PARAMETRI/PRODUZIONE/COSTI), tastiera numerica sui campi, switch tinto, KPI colorati (massa/volume/riduzione), compliance come chip verdi/ambra, **vettore consigliato evidenziato in oro con stella + shimmer**.
- **Profilo rifugista**: aurora di sfondo, card identità e righe impostazioni in glass, glow ciano dietro la foto della struttura.

**Pass "oltre" — upgrade del SISTEMA grafico (non delle singole schermate):**
1. **Identità tipografica** (`Type.kt`, nuova): titoli athletic (ExtraBold, tracking negativo) + label "telemetria" (tracking +0.8sp), collegata a `TsmTheme` → ogni `MaterialTheme.typography.*` dell'app la eredita. Più `TsmType.Numeric`: **cifre monospace** per tutti i KPI (cronometro/metriche live Registra, contatori animati, stat feed/post/dettaglio, KPI waste) — larghezza fissa, i numeri non "ballano" in tempo reale, taglio strumento-di-misura.
2. **Transizioni di navigazione condivise** (`TsmNavHost`): push = slide+fade dal bordo destro, pop speculare, schermata sotto in leggera scala (parallasse di profondità), easing `TsmMotion`. Un solo punto di definizione → tutte le destination animano coerenti (prima: crossfade secco).
3. **Micro-interazioni sul materiale** (`TsmGlass.kt`): le glass card cliccabili e le CTA a gradiente si comprimono al tocco (spring 0.96-0.97) col bordo che si accende — ogni superficie interattiva dell'app risponde fisicamente al dito.
4. **Bottom bar**: hairline luminosa a gradiente sopra la barra (separazione "il contenuto scorre sotto").

**Test nuovi:** copia personale di sessione (idempotenza, share 200, esclusione stats), foto rifugio (set/clear/422 data URI non-image/403 hiker). Suite completa: 330/330.

---

## 🔋 Sprint 3 kick-off — 10 giugno 2026 (bug fix, consumi, sicurezza, modulo rifiuti)

> Backend **261/261 test verdi** (19 suite, +8 modulo rifiuti); mobile **`compileDebugKotlin` BUILD SUCCESSFUL**. Branch `UI`, nessun push (lavoro locale).

**Bug fix (B-01/02/03 della retrospettiva M4):**

- **B-01 — Zoom mappa "bloccato troppo distante"**: root cause = liste punti ricreate a ogni ricomposizione (no `remember`) che ri-triggeravano `zoomToBoundingBox` resettando lo zoom dell'utente al livello fit. Fix: `remember` in `TsmRouteMapDialog` + fingerprint **per-valore** del contenuto in `TsmSentieriMapView` (`contentKey`) e `TsmRouteMapPreview` (`routeKey`) al posto delle chiavi per-identità.
- **B-02 — Badge NFC sempre "ATTIVO"**: il badge in `ProfileScreen` era condizionato a `nfcAvailable` (presenza chip) invece dello stato reattivo `nfcEnabled` (già aggiornato da BroadcastReceiver + ON_RESUME). Ora badge a 3 stati: ATTIVO (verde) / DISATTIVATO (ambra) / NON DISPONIBILE (rosso).
- **B-03 — "Termina" incoerente Unisciti vs dettaglio**: `SessionJoinViewModel.leaderStop` non eseguiva il **force-close ADR-001** (solo stop locale → la sessione restava ACTIVE per gli altri); inoltre l'Hub bypassava `leaderStop` quando live. Ora entrambi i flussi convergono: stop coordinato + `forceCloseSession` + navigazione al dialog "Salva attività" se live.

**Audit consumi (focus batteria):**

- **Frecce direzionali mappe**: la fase animata cambiava a ogni frame → `MapView.invalidate()` a ~60 Hz _per ogni mappa visibile_. Quantizzata a 18 step/ciclo → ~7 Hz (−87/89% di redraw) in `TsmRouteMapPreview` e `TsmSentieriMapView`, visivamente identico.
- **GPS idle (tab Registra, non registrando)**: `UserLocationTracker` da HIGH_ACCURACY @2 s/1 s → @5 s/2,5 s (−60% duty cycle); durante la registrazione resta il ForegroundTrackingService a 2 s.
- **SyncManager**: idle backoff del poll loop 60 s → 2 min → 5 min (cap) a coda vuota; `enqueueImmediate` e nuovo lavoro resettano a 60 s.
- Verificati e lasciati invariati (giustificati): live polling sessione 5 s (sicurezza gruppo), SOS retry 15 s (solo con SOS pendente), BLE `SCAN_MODE_LOW_LATENCY` (solo durante SOS attivo), animazioni decorative (composition-scoped).

**Sicurezza:**

- `requireRoles("rifugio", "admin")` su `/api/v1/refuge/*` (prima bastava un JWT qualsiasi).
- Verificato: `authenticate` + `authenticatedLimiter` su tutte le route Sprint 2 (stories, board, follow, emergency, badge, credits); Joi su tutti i write.
- `npm audit --omit=dev`: **0 vulnerabilità** (erano 6 moderate a inizio Sprint 2).

**Nuovo — Modulo Rifiuti & Logistica (ADR-002, MVP read-only):**

- Backend: `services/wasteService.js` (config 6 categorie + grigliato, 4 vettori, formule del Simulatore web/elaborato OGA), route `/api/v1/refuge/waste/{config,simulate}` con auth+limiter+ruoli+Joi (`wasteSimulationSchema`), 8 test in `__tests__/routes/waste.test.js` con valori di riferimento (elicottero 2 viaggi → 2,00 €/kg, coerente col c_kg dell'elaborato).
- Mobile: `WasteSimulatorScreen` + `WasteSimulatorViewModel` + DTO; entry card "Rifiuti & Logistica" nella dashboard rifugio; route `REFUGE_WASTE`.
- Prossimi step (non MVP): persistenza `WasteRecord` (storico stagionale), riduzioni per categoria da UI, benchmark anonimo tra rifugi.

---

## 🚚 Consegna — giugno 2026 (ridisegno sessioni ADR-001 + auth + polish finale)

> Backend **253/253 test verdi** (18 suite); mobile **`compileDebugKotlin` BUILD SUCCESSFUL**.

**Sessioni — ridisegno ciclo di vita (ADR-001, vedi `docs/ADR-session-lifecycle.md`)**
Separati i due cicli di vita prima sovrapposti su un unico `status`:

- **Lifecycle sessione** (`PLANNED→ACTIVE→COMPLETED`) controllato dal **leader**.
- **Partecipazione del singolo** → `participants[].participationState`
  (`idle/live/finished/left`), **ortogonale** e non bloccante.
- **"Arresta" del capogruppo CHIUDE sempre per tutti** (`/close` con **auto-finalize**
  dei membri ancora live) → risolve il bug "non riesco a terminare la sessione" /
  sessioni **ghost** tenute aperte da un partecipante che non concludeva.
- `completeSession` = conclusione **individuale** (non forza lo stato globale);
  auto-complete solo quando **tutti** gli accettati sono `finished/left`.
- **Failover** esclude i membri `finished/left` (no elezione di un ghost); il creator
  conserva sempre il diritto di chiudere.
- Mobile: `SessionLiveStateStore.reconcileWithSessions` azzera lo stato live locale
  anche su `COMPLETED` → eliminata l'ultima desync della doppia macchina a stati.
- Rimosso `finishedParticipants[]` (assorbito da `participationState`).

**Auth — login con email O username**: `POST /auth/login` (campo `email` = email o
username, lookup `$or`); validazione client/server rilassata.

**Logout pulisce la cache**: `ProfileViewModel.logout` ora svuota `cacheDir` + gli
store `SharedPreferences` (`tsm_session_live_state`, `tsm_board_dismissed`) → niente
stato stantio dopo il re-login (causa di bug segnalati).

**Bug-D re-auth password**: `WRONG_PASSWORD`/`WRONG_OLD_PASSWORD` → **403** (distinto
dal 401 "token scaduto") così il mobile non confonde "password errata" con sessione
scaduta. Test allineati.

**Polish/ottimizzazione finale**: rimosso dead code (`finishedParticipants`, import
`Patterns` inutilizzato), commenti/doc allineati (`database_schema.md`,
`api_reference.md`, ADR).

> ⚠️ **Deploy**: tutte le modifiche backend (ADR-001 sessioni, login email/username)
> vivono sul branch `UI` e richiedono **deploy** + **rebuild APK** per essere attive.

---

## 🎨 Polish grafico + hardening stabilità — giugno 2026 (consegna)

> Mobile **build pulita SUCCESSFUL**; backend **248/248 test verdi** (17 suite).

**Design system (riusabile)**: `TsmGradients` (preset Brush + `altitudeTint`),
`TsmMotion` (durate/easing/spring), `TsmDimens` (spacing + corner radius),
`TsmGlass` esteso (`TsmPulseGlow`, `TsmGlassChip`, `TsmGradientButton`),
`TsmAnimatedCounter` (count-up).

**Splash brandizzato**: eliminato il bianco all'avvio — `windowBackground` a
gradiente montano + logo (`splash_background.xml`), tema dark; `TsmBootScreen`
con reveal animato (logo + orizzonte montano disegnato via Canvas).

**Schermate**: aurora/glass portati a parità su Formazione, Notifiche,
Follower/Seguiti, Ricerca utenti, Classifica (oltre a Feed/Profilo/Sessione già
fatti). Anello storie a **gradiente conico rotante**; **comet** luminosa lungo la
traccia GPX; medaglie classifica "materiche" (oro/argento/bronzo + glow) con
count-up; emoji placeholder → icone vettoriali; bottom nav con micro-bounce +
colori palette.

**Hardening stabilità**:

- Backend: handler globali `unhandledRejection` / `uncaughtException` (log + shutdown
  controllato → riavvio pulito del process manager).
- Mobile: crash logger globale `Thread.setDefaultUncaughtExceptionHandler` (tag
  `TSM-CRASH`) — rende diagnosticabili i crash che prima davano "schermata bianca"
  senza traccia in Logcat. Audit `.first()`/`!!` nei path UI: già guardati.
- Overlay mappa (frecce/comet) in `try/catch` + sanitize coordinate NaN/Inf.

> _Polish opzionale futuro (non bloccante)_: scrub+tooltip interattivo
> sull'altimetria, transizioni shared-element in navigazione, pull-to-refresh
> custom col logo, set completo di icone custom mountain-tech, particellari
> contestuali (neve/scintille) e sfondi reattivi al meteo. Fondamenta già pronte
> (`TsmGradients`/`TsmMotion`/`TsmGlass`) per applicarli rapidamente.

---

## ✅ Chiusura Sprint 2 — giugno 2026

> Build mobile **`compileDebugKotlin` BUILD SUCCESSFUL**; backend **248/248 test verdi** (17 suite).

Ultimo round di fix/feature + consolidamento prima della chiusura:

**Sessioni — ridisegno logica capogruppo/partecipanti**

- **Completamento "Ibrido"**: ogni membro conclude il proprio tracking (`finishedParticipants[]` + crediti per-utente); la sessione passa a `COMPLETED` quando **tutti gli accettati** hanno finito → **sessione in solitaria** finalmente terminabile (rimossa la race `markSessionPlanned`↔`completeSession`).
- **"Chiudi sessione per tutti"** (`POST /sessions/:id/close`): force-close del capogruppo.
- **Failover capogruppo**: heartbeat sul live-location; se il leader è inattivo >90s → **elezione automatica** del partecipante accettato più anziano ancora live; **reclaim** automatico al rientro del creator. `currentLeaderId` + `statoFailover`.
- **Elimina attività di sessione** propagato al server (`hiddenForUsers[]` via `DELETE /sessions/:id/from-activities`) → non riappaiono dopo logout/login.

**Feed / Attività**

- Profilo altimetrico **ricco** (gradiente per quota, marker max, assi + MIN/MAX) usato **anche nel feed** (prima ricadeva sul chart piatto). Share routing corretto (no più 404 su attività di sessione).
- **Frecce direzionali animate** (stile Komoot) sulla polyline GPX, adattive allo zoom.

**Storie**

- Editor: **drag/pinch/rotate** di traccia e sticker risolti (gesture canonica), rotazione 1:1, toolbar premium, **scelta font** (Classico/Elegante/Mono/Corsivo), mappa aggiungibile **anche su video**.
- Qualità immagini upload aumentata; cattura video low-res con size-limit.

**Robustezza / UI**

- Guard anti-overwrite tracking + dialog Salva/Scarta/Annulla; insets nav-bar; sfondo aurora su Sessione/Pianifica; overlay mappa hardening (try/catch + sanitize coordinate NaN).

> ⚠️ **Deploy**: le modifiche backend (completamento Ibrido, `/close`, `/from-activities`, failover, story `textFont`) vivono sul branch `UI` e richiedono deploy per essere attive.

---

## ⏱️ Aggiornamento Sprint 3 — giugno 2026

> Build mobile **`compileDebugKotlin` green**; backend **220/220 test verdi** (15 suite).

Funzionalità aggiunte dopo lo Sprint 2:

**Social (completo)**

- Ricerca/scoperta utenti, liste follower/seguiti navigabili.
- Metriche escursionistiche sul profilo (km/dislivello/uscite/punti) + classifica settimanale tra i seguiti.
- **Notifiche** in-app (follow/like/commento) con centro notifiche + badge non-letti + deep-link.
- Badge "Ti segue"; **gate privacy** profilo (`profileVisibility` applicato in `getHikerById` + feed + bacheca).

**Rifugio — Dashboard IoT (mock)**

- Modelli `EdgeNode`, `RefugeSensorReading`, `RefugePassage` + seed mock + `GET /api/v1/refuge/dashboard`.
- UI dashboard fedele al mockup (sensori, edge nodes BLE-mesh, passaggi/social-credit) + scheda profilo rifugista.
- ⚠️ Nessun ingest MQTT reale: dati generati lato server, schema definitivo.

**Bacheca rifugi**

- Modello `RefugeBoardPost` (info/avviso/pericolo, `validUntil`) + CRUD `/api/v1/board` Joi-validato.
- Composizione lato rifugista (crea/modifica/elimina) + consultazione utenti (icona in Home/Pianificazione/Registra).

**Altro**: fix bug attività (paginazione feed, cancel sfida, eliminazioni persistenti); pipeline CI; 0 vulnerabilità npm; copertura test backend 88 → 220.

> _Polish residuo (non bloccante): estrazione stringhe i18n delle schermate nuove, unificazione design-token su `TsmColors`, skeleton loaders._

### 🐛 Fix UI/UX mobile — giugno 2026 (sessione fix)

Correzioni mirate (incluse regressioni **non ancora notate dall'utente**):

- **Build mobile ripristinata**: `:app:compileDebugKotlin` non compilava più per regressioni pre-esistenti — import mancanti in `FeedCard.kt` (`fillMaxSize`/`clip`/`CircleShape`), riferimento errato `FeedUser.userId` (→ `_id`) e mismatch di tipo `List<Any>` vs `List<RoutePoint>` in `SessionHubScreen`. Risolte.
- **Traccia GPX + altimetria come due schede swipe** — nuovo componente unico `ui/components/TsmRouteElevationPager.kt`: 1ª pagina = traccia su **mappa** OpenTopoMap con **inizio = cerchio verde** e **fine = bandiera a scacchi** (via `TsmRouteMapPreview`), 2ª pagina (swipe a destra) = **profilo altimetrico** (`ElevationSparkline`). Adottato in **feed, dettaglio social, dettaglio attività e Unisciti (lista + dettaglio sessione)**: elimina l'incoerenza per cui alcune attività pianificate mostravano solo l'altimetria e altre solo la traccia, e rimuove le copie divergenti/bacate del pager.
- **Avatar utenti coerenti + tap → profilo social ovunque**: i partecipanti del **Dettaglio Attività** (prima iniziali statiche, non cliccabili) e del **Dettaglio Sessione**, e gli **autori dei commenti** (`CommentsBottomSheet`), ora usano `AvatarImage` (foto profilo) e al tap aprono il profilo social. `onUserClick` propagato fino a `SessionDetailScreen`, `ActivityDetailScreen`, `UserProfileScreen` e alle destinazioni in `TsmNavHost`. Le liste social (ricerca, follower/seguiti, classifica, notifiche, feed, hub sessioni) erano già conformi. _Fuori scope_: gli sheet di tracking live (`LiveParticipantSheet`/`GroupRosterMenu`) restano "schede" del partecipante sulla mappa, non navigano al profilo social.
- **Arresto sessione da "Unisciti" con salvataggio**: arrestando una sessione **mentre il tracking è attivo**, ora compare lo stesso dialog "Salva attività" del tasto **Termina** in Registra (Salva / Scarta / Annulla) e l'escursione viene **salvata**; prima l'attività veniva **persa** (`detachFromLiveTracking`). Implementazione: il `SessionStopCoordinator`, se c'è un tracking in corso, instrada verso `requestStopTracking()` (anziché lo stacco silenzioso); `HikerMainScreen` osserva `showStopConfirm` e porta l'utente sulla tab Registra dove vive il dialog; `SessionDetailScreen` torna alla shell allo stop così il dialog è visibile.

> Backend / API / DB **invariati** (nessuna modifica a endpoint o schema): `api_reference.md` e `database_schema.md` restano allineati.

### 🚀 Stories reali + Approvazione partecipanti + Sesso visibile — giugno 2026

> Build mobile **`compileDebugKotlin` green**; backend **238/238 test verdi (17 suite)**.

Epic a fasi (0→A→B→C→D):

**Fase 0 — Sesso partecipanti**: `getLiveLocations` invia `personalInfo.sex` a **tutti i membri** (non solo capogruppo); mostrato in `LiveParticipantSheet` e `GroupRosterMenu`.

**Fase A — Join sessione con approvazione/rimozione**

- `participants[]` esteso con `status` (`pending`/`accepted`, default accepted per retrocompat) + `approvedBy`; nuovo `removedUserIds[]` (ban locale alla sessione).
- `joinSession` ora crea una **richiesta pending** (+ check ban). Nuovi endpoint: `POST /sessions/:id/participants/:userId/approve` e `/reject` (capogruppo **o** un partecipante già accettato), `DELETE /sessions/:id/participants/:userId` (rimozione definitiva, **solo capogruppo**).
- Mobile: `ParticipantsCard` dinamica (accettati + sezione **In attesa** con accetta/rifiuta, "accettato da X", rimuovi per il capogruppo); join → feedback "richiesta inviata". +7 test backend.

**Fase B — Sistema Stories reale (sostituisce la derivazione dai post)**

- Nuovo model `Story` (TTL 24h via index): `type` (`planned_session`/`activity`), `sessionId`/`activityId`, `inviteCode` snapshot, `caption`, `media[]` (Base64 foto/video capped), `overlay` (titolo/distanza/dislivello/tempo/traccia), `viewers[]`.
- Endpoint `/api/v1/stories`: `POST`, `GET /user/:userId`, `POST /:id/view`, `DELETE /:id` (auth + rate limit + Joi con cap dimensione media). `getSocialRowForUser` ora deriva l'anello "story" dalle Story reali (+ `hasUnviewedStory`). +8 test backend.

**Fase C — Stories mobile**

- `StoryViewerScreen` riscritto: carica le storie reali dell'autore (`/stories/user/:id`), progress segmentata + auto-advance, **media reali** (foto da Base64, video breve da cache file + `VideoView`), overlay tracciamento, bottone **"Unisciti"** per le storie `planned_session`, `markViewed` per segmento.
- `StoryComposerScreen` + VM: picker foto/video (PhotoPicker, niente permessi), encoding Base64 (immagine compressa via `AvatarUtils`, video con cap), pubblicazione. Entry point **"Condividi come storia"** in `ActivityDetailScreen` (post-hike) e `SessionDetailScreen` (pre-hike).
- AvatarRow / Home / Nav ricablati: le storie si aprono **per autore** (`STORY_VIEWER = story_viewer/{userId}`); nuovo `STORY_COMPOSER` con holder `pendingStoryDraft`.

**Fase D — Media/permessi**: scelta gallery PhotoPicker → **nessun permesso** runtime aggiuntivo necessario; cap media coerenti client/server (immagine ≤ ~1.5MB, video ≤ ~3.5MB, sotto il body limit 5mb).

> Media decisi con l'utente: foto + **video breve capped** in Base64 (no object storage nello stack). Doc API/DB aggiornati in `api_reference.md` e `database_schema.md`.

---

> Snapshot dell'architettura, codebase e implementazione corrente. Aggiornato al **26 maggio 2026 (sessione serale)** — Sprint 2 con feature **Foto profilo utente** completata (privacy gate fix, componente Compose `AvatarImage` riusabile, EXIF rotation, foto visibile in ProfileScreen / ProfileViewScreen / partecipanti sessioni), oltre alle tre sessioni precedenti dello stesso giorno (notturna: discriminator persistence + anti-cheat; pomeridiana: refresh token rotation + WAL Room v5). Build mobile **`compileDebugKotlin` green**, backend **88/89 test verdi** (1 test fragile pre-esistente su `BREVO_API_KEY`).

---

## 1. Identità progetto

- **Corso**: Ingegneria del Software, UniTrento
- **Gruppo**: ID-6
  - Giacomo Radin (242907) — backend + mobile + lead
  - Federico Cattelan (242111)
  - Marco Christian Stoica (246443) — weather integration
- **Deliverable**: D1 (requisiti, 27/03/2026) ✅ — D2 (architettura, 26/04/2026) ✅ — D3 in progress
- **Stack**:
  - Backend: Node.js 18+, Express 4, Mongoose 8, MongoDB Atlas
  - Mobile: Kotlin 2.0.21, Compose BOM 2024.12.01, Room 2.6.1, Retrofit, OSMdroid
  - IoT: Mosquitto MQTT + Python gateway (placeholder)
- **Hosting**: Backend su Render Free tier (cold start ~30-60s); MongoDB Atlas free

---

## 2. Struttura monorepo

```
trento-smart-mountain/
├─ backend/src/
│  ├─ app.js                # bootstrap Express + security stack
│  ├─ server.js             # avvio + assertEnvironment fail-fast
│  ├─ middleware/
│  │  ├─ authMiddleware.js          # JWT verify
│  │  ├─ authorizationMiddleware.js # role check (Admin only)
│  │  ├─ errorMiddleware.js
│  │  ├─ rateLimitMiddleware.js     # 5 limiter differenziati
│  │  ├─ securityMiddleware.js      # helmet, mongo-sanitize, hpp, CORS
│  │  └─ validationMiddleware.js    # Joi schemas + factory
│  ├─ models/
│  │  ├─ user.js                    # discriminator base
│  │  ├─ hiker.js / refuge.js / admin.js
│  │  ├─ hikeSession.js             # sessioni di gruppo (gpxStats + actualStats)
│  │  ├─ activity.js                # attività libere personali (nuovo)
│  │  └─ location.js                # weather venues (towns/POI)
│  ├─ routes/                       # mapping URL → service
│  │  ├─ authRoutes.js              # rate limit + validate per ogni endpoint sensibile
│  │  ├─ hikerRoutes.js / refugeRoutes.js / adminRoutes.js
│  │  ├─ hikeSessionRoutes.js
│  │  ├─ activityRoutes.js          # nuovo: CRUD attività libere
│  │  └─ weatherRoutes.js
│  └─ services/                     # logica business
│
├─ mobile/app/src/main/java/it/trentosmartmountain/app/
│  ├─ TsmApplication.kt             # DI manuale: TokenStorage, Room, Retrofit, SyncManager
│  ├─ data/
│  │  ├─ estimation/HikeEstimation.kt   # CAI/Naismith formulas
│  │  ├─ local/
│  │  │  ├─ TokenStorage.kt              # EncryptedSharedPreferences (JWT)
│  │  │  └─ db/                          # Room v4 (con retry fields)
│  │  ├─ location/                       # GPS tracking engine
│  │  ├─ remote/
│  │  │  ├─ TsmApiClient.kt / TsmApiService.kt
│  │  │  └─ dto/                         # CreateActivityRequest, ActualStats, ...
│  │  ├─ session/SessionStartCoordinator.kt
│  │  └─ sync/SyncManager.kt             # poll loop 60s + backoff 1m→5m→30m→1h
│  ├─ service/ForegroundTrackingService.kt
│  ├─ ui/
│  │  ├─ navigation/
│  │  ├─ screens/{auth,home,login,main,profile,refuge,register,registra,session}
│  │  └─ theme/
│  └─ viewmodel/
│
├─ docs/
│  ├─ SECURITY.md           # nuovo: threat model, OWASP, ACM
│  ├─ TSM_PROJECT_STATE.md  # questo file
│  ├─ D1_*.pdf / D2_*.pdf
│  ├─ api_reference.md / architecture.md / database_schema.md
│  ├─ mobile_app_base.md / setup_backend.md / setup_mobile.md
│  ├─ android_server_communication.md
│  ├─ sprint2_plan.md
│  └─ test_cases_sprint1.md
│
├─ iot/                              # placeholder Python gateway
├─ scripts/                          # adb-reverse, ecc.
├─ docker-compose.yml                # MongoDB + Mosquitto locali
├─ swagger.js / swagger-output.json
├─ .env.example                      # nuovo: template variabili
└─ .gitignore                        # .env già escluso
```

---

## 3. Cosa è implementato

### Backend

#### Auth

- ✅ POST `/auth/register/hiker` — schema Joi, rate limit 5/h, bcrypt
- ✅ POST `/auth/register/refuge` — flat schema con rifugioName/address/altitudeMeters
- ✅ POST `/auth/login` — rate limit 10/15min (skip success)
- ✅ GET `/auth/verify/:token` — deep link `tsm://` con auto-login
- ✅ POST `/auth/forgot-password` — email link via Brevo, rate limit 5/h
- ✅ GET / POST `/auth/reset-password/:token` — form HTML + JSON, token monouso 1h

#### Sessions (gruppo)

- ✅ CRUD sessione + invite code TSM-XXXX univoco
- ✅ Join via codice invito (vincolo: una sola ACTIVE per utente)
- ✅ PATCH `/status` per lifecycle PLANNED→ACTIVE→COMPLETED
- ✅ PATCH `/complete` con actualStats (movingSec, distMeters, finalPoints, ecc.)
- ✅ GET `/stats?year=N` aggregate annuali (unifica HikeSession.COMPLETED + Activity libere)
- ✅ Modello gpxStats con gpxDurationSec (durata effettiva dal `<time>` GPX)
- ✅ Modello actualStats per dati registrati live

#### Activities (libere)

- ✅ POST `/api/v1/activities` — creazione attività personale
- ✅ GET `/api/v1/activities` — lista per utente
- ✅ GET `/api/v1/activities/:id` — dettaglio con check owner
- ✅ DELETE `/api/v1/activities/:id` — solo proprietario
- ✅ Indici `(userId, completedAt)` + 2dsphere sparse su startPoint

#### Weather

- ✅ GET `/weather/locations/nearby?lon=&lat=&maxDistance=&type=` (2dsphere, auth)
- ✅ GET `/weather/locations/search?q=&type=&limit=` (regex case-insensitive, auth)
- ✅ GET `/weather/forecast/:externalId?forceRefresh=` (cache 1h, auth)
- ✅ POST `/weather/seed` — admin only (auth + requireRoles)
- ✅ POST `/weather/forecast/:id/refresh` — admin only (auth + requireRoles)
- ✅ Seed automatico da meteo.report + gitlab tinia-euregio

#### Security

- ✅ helmet (CSP custom per Swagger UI, HSTS in prod)
- ✅ CORS allow-list via `ALLOWED_ORIGINS` env
- ✅ express-mongo-sanitize (NoSQL injection)
- ✅ hpp (HTTP Parameter Pollution)
- ✅ Body size 100 KB
- ✅ Rate limit a 5 livelli (global, login, register, password reset, authenticated, write)
- ✅ Joi validation su tutti gli endpoint POST/PATCH/DELETE
- ✅ fail-fast su env vars mancanti / JWT_SECRET debole
- ✅ `trust proxy = 1` per IP corretto dietro Render
- ✅ JWT expiry esteso a 7d (default) per supportare requisito offline 3 giorni
- ✅ Tutte le route weather admin protette (`/weather/seed`, `/weather/forecast/:id/refresh`)

### Mobile

#### Auth

- ✅ AuthEntry → Login / Register / RegisterRifugio / ForgotPassword
- ✅ JWT in EncryptedSharedPreferences (TokenStorage)
- ✅ Deep link auto-login post email verify

#### Sessions

- ✅ SessionHubScreen: tab PIANIFICA (GPX import + form + QR) / UNISCITI (code box + lista)
- ✅ Tab UNISCITI mostra solo sessioni PLANNED/ACTIVE (COMPLETED filtrate → "Le mie attività")
- ✅ SessionDetailScreen: elevation chart, meteo reale TINIA, checklist drag-and-drop, partecipanti, edit creator
- ✅ SessionPlanViewModel con parser GPX (haversine + smoothing + valley-peak + `<time>` per durata effettiva)
- ✅ SessionStartCoordinator: SharedFlow(replay=1) per consegna affidabile a HikerMainScreen + RegistraVM

#### Registra (tracking GPS)

- ✅ TsmMapView (OSMdroid + OpenTopoMap tiles)
- ✅ HikeTrackingEngine + StationaryDetector (auto-pause)
- ✅ ForegroundTrackingService (persistenza GPS in background)
- ✅ Check GPS hardware abilitato dentro `RegistraViewModel.startTracking()` (copre tutte le route: REC button, autoStart da Detail/Hub)
- ✅ Dialog "GPS spento" con link a `Settings.ACTION_LOCATION_SOURCE_SETTINGS`
- ✅ Dialog "Salva Attività" con KPI strip (Distanza/Durata/Dislivello/Punti) + nome editabile pre-popolato "Escursione – [data]"
- ✅ Dialog "Attività troppo corta" per libere < 50m con 3 opzioni distinte: Salva comunque / Continua / Cancella
- ✅ Upload immediato post-stop (PATCH /complete o POST /activities)
- ✅ Fallback SyncManager se la rete fallisce

#### Home / Le Mie Attività

- ✅ HomeScreen sotto-tab "Personale" cabla `ActivityListScreen` (prima placeholder)
- ✅ Tap su card attività apre `ActivityDetailScreen` via `Routes.ACTIVITY_DETAIL` (navigazione cablata)
- ✅ ActivityListScreen con yearly stats card (HorizontalPager 5 anni)
- ✅ MonthlyBarChart cliccabile (filtro mese)
- ✅ Sort: recente / vecchia / A-Z / distanza / difficoltà / durata
- ✅ Bottone "Risincronizza ($n)" bypassa il backoff (`SyncManager.enqueueImmediate(ignoreBackoff=true)`)
- ✅ Empty state contestuale (no attività vs no attività per periodo)
- ✅ ElevationProfileChart con assi disegnati nel canvas (no più sovrapposizione)
- ✅ ActivityDetailScreen: metric grid, mappa preview (TsmRouteMapPreview con OSMdroid), partecipanti, profilo altimetrico, timeline split km, performance badges (Alpinista, Maratoneta), export GPX
- ✅ PostDetailScreen: dettaglio social avanzato con timeline split ogni 5km e performance badges per un'esperienza "Strava-like".
- ✅ Nuova rotta `POST_DETAIL` in `TsmNavHost.kt` e `Routes.kt` per la navigazione nel feed social.
- ✅ TsmRouteMapPreview: nuovo componente basato su OSMdroid che renderizza il tracciato GPX sopra una mappa topografica reale, sostituendo la vecchia RouteTracePreview statica. Integrato in FeedCard, ActivityDetailScreen e PostDetailScreen.
- ✅ Delete con cleanup remoto per attività libere (DELETE /activities/:id)

#### Sync engine

- ✅ SyncManager: coroutine loop 60s + backoff incrementale per record (1m → 5m → 30m → 1h cap)
- ✅ Fix bug GPX: aggiunto `parseRoutePolyline` in `SyncManager.kt` per garantire l'invio della `routePolyline` durante il sync delle attività libere (risolve tracce mancanti nel feed social).
- ✅ `enqueueImmediate()` per pull-to-refresh manuale
- ✅ Room v4 con campi `retry_count`, `last_retry_at_ms`, `remote_id`
- ✅ Marcatura `isSynced=1` post upload + tracking `remoteId` per delete cross-device

#### Foto profilo (avatar) — sessione serale 26/05

- ✅ Schema: `Hiker.personalInfo.avatarUrl` (data URI Base64, max 7 MB lato Joi su body cap 5 MB)
- ✅ Endpoint riusato: `PATCH /api/v1/users/me/personal-info` con `avatarUrl` opzionale
- ✅ Privacy gate (`userPrivacy.js`): per "other viewer" è pubblico SOLO `personalInfo.avatarUrl`; gli altri campi (sex, birthDate, heightCm, weightKg) restano privati
- ✅ Populate sessioni: `participants.userId` e `creatorId` includono `personalInfo.avatarUrl` (8 occorrenze in `hikeSessionService.js`)
- ✅ Joi validation: pattern stretto `^data:image/(jpeg|jpg|png|webp);base64,...$`, accetta `""` per il flow "rimuovi foto"
- ✅ Mobile: componente Compose riusabile `ui/components/AvatarImage.kt` (decode Base64 memoizzato via `remember(avatarUrl)`, fallback iniziali con colore deterministico, overlay loader)
- ✅ Mobile: utility `ui/util/AvatarUtils.kt` (load URI → EXIF rotation → downscale 500 px → JPEG q70 → Base64 NO_WRAP, tutto su `Dispatchers.IO`)
- ✅ UX: long-press sull'avatar in ProfileScreen → dialog "Rimuovi foto" + Toast su success/error; icona `CameraAlt` come hint di tap
- ✅ Foto visibile in: `ProfileScreen` (64 dp), `ProfileViewScreen` (88 dp header), `SessionDetailScreen` PartecipantsCard (40 dp con bordo accent per creator)
- ✅ Dipendenza aggiunta: `androidx.exifinterface:1.3.7`

### Convenzioni codice

- Backend: routes → services → models (3 layer); errori business come `throw new Error("CODE")` mappati in HTTP
- Mobile: MVVM (Compose UI + StateFlow + Repository). DI manuale (no Hilt)
- JWT payload: `{ userId, role }`
- ID Mongo serializzati come `_id` (string ObjectId)

---

## 4. Gap analysis (cosa manca)

### Non ancora implementato

- ❌ SOS via BLE Mesh — UC4 da D1 (Sprint 3)
- 📋 NFC check-in vetta — UC5 da D1 — **piano scritto** in [sprint2_profilo_formazione.md](sprint2_profilo_formazione.md) (fase C+G)
- 📋 Social Credits + livelli + feed — **piano scritto** in [sprint2_social.md](sprint2_social.md) e [sprint2_profilo_formazione.md](sprint2_profilo_formazione.md)
- 📋 Educational mode + Quiz — **piano scritto** in [sprint2_profilo_formazione.md](sprint2_profilo_formazione.md) (fase B+F)
- ❌ MQTT mobile client + IoT gateway Python — placeholder (Sprint 3)
- ❌ OAuth Google login — solo email/password (Sprint 3)
- ❌ Socket.io live tracking partecipanti — dipendenza installata ma non usata (Sprint 3)
- ❌ Admin dashboard web — solo API
- ❌ Refuge dashboard mobile — placeholder (post-Sprint 3)

> Legend: ❌ = non pianificato per Sprint 2; 📋 = piano operativo scritto, codice da produrre

### Tech debt / TODO security

- ⚠ Logging strutturato + Sentry (oggi solo `console.log/error`)
- ⚠ Audit trail per azioni admin (chi ha eliminato chi, change-role events)
- ⚠ Rotazione automatica JWT secret (oggi manuale, vedi SECURITY.md sez. 6)
- ⚠ Tombstone table per delete offline-first (oggi best-effort via `remoteId`)
- ⚠ Rate limit con store Redis (oggi in-memory, ok per Render single-instance)
- ⚠ CI gate su `npm audit` (oggi 6 moderate severity da risolvere)
- ⚠ Coverage Jest da estendere ai service layer (oggi: route auth/hiker/sessions/activities/weather/account/refreshToken coperte; service layer indiretto via route)
- ⚠ Test fragile `POST /auth/register/hiker` dipende da `BREVO_API_KEY` env in test → mockare `emailService.sendVerificationEmail` per renderlo hermetico (1 test su 89 in failure per questo)
- ⚠ Recovery dialog post-crash dalla WAL (`tracking_wal`): l'infra c'è (Room v5, `TrackingPersistenceRepository.finalize`), manca solo il prompt UX alla riapertura per recuperare l'attività interrotta
- ⚠ Avatar: serializzato come Base64 inline nei `participants.userId.personalInfo.avatarUrl` → payload pesante (~30-100 KB per partecipante). Tradeoff accettato per Sprint 2; in Sprint 3 considerare endpoint dedicato `/users/:id/avatar` con cache headers

### Limiti noti sync mobile

- ⚠ Senza WorkManager: il sync funziona solo se il process è vivo (foreground o cached background). Se l'OS killa l'app, niente retry finché non si riapre. Workaround: alla riapertura il loop riparte e processa il backlog.
- ⚠ Niente Redis distribuito: rate limit è per-instance (Render free tier single instance, accettabile).

---

## 5. Roadmap Sprint 2-3

### Sprint 2 (in corso, deadline ~giugno 2026)

#### Chiuso

- [x] Bug fix tempi GPX (durata effettiva da `<time>`)
- [x] Sync attività locali/cloud con actualStats
- [x] Profilo altimetrico corretto (no overlap)
- [x] Bar chart mese + cards anno funzionanti
- [x] Attività libere collection + endpoint
- [x] Security hardening completo (rate limit, validation, secrets)
- [x] Retry incrementale sync (1m → 5m → 30m → 1h)
- [x] UI re-sync manuale + dialog soglia 50m
- [x] Risolti merge conflicts user.js/hikeSessionRoutes.js/hikeSessionService.js (bloccavano deploy Render)
- [x] Render develop deploy live (auto-build su push branch UI)
- [x] Wiring ActivityListScreen in HomeScreen + nav verso ActivityDetailScreen
- [x] Dialog "Salva Attività" con KPI strip (Distanza/Durata/Dislivello/Punti) + nome pre-popolato
- [x] Dialog "Attività troppo corta" UX a 3 opzioni (Salva/Continua/Cancella)
- [x] Check GPS hardware spostato nel VM (copre tutte le route di start tracking)
- [x] Fix AVVIA tab switch via SharedFlow (StateFlow conflated saltava emit)
- [x] Fix bottone Risincronizza che ignorava il backoff retry
- [x] JWT expiry esteso a 7d per requisito offline 3 giorni
- [x] Auth admin su `POST /weather/seed` e `POST /weather/forecast/:id/refresh`
- [x] Filtro tab UNISCITI: solo sessioni PLANNED/ACTIVE
- [x] Endpoint mobile stats corretto (`/api/v1/sessions/stats` non `/activities/stats`)

#### Piani approvati, codice da scrivere

- [ ] Schermata SOCIAL (Home → tab Social) — piano in [sprint2_social.md](sprint2_social.md)
- [ ] Schermata Profilo rinnovata + Formazione + NFC — piano in [sprint2_profilo_formazione.md](sprint2_profilo_formazione.md)

#### Ancora aperto

- [x] Tests Jest su route principali (auth + hiker + sessions + activities + weather auth + account v2 + discriminator persistence) — **78/78 verde**
- [x] Profilo v2 completo (personalInfo/experience/preferences/goals) + onboarding 3-step skippable
- [x] ProfileViewScreen read-only con indicatori 🔒 sui campi anti-cheat
- [x] Anti-cheat enforcement server-side (birthDate, caiLevel) — fix critico 26/05
- [x] Discriminator persistence (Hiker fields via $set/$inc) — fix critico 26/05
- [x] Auto-seed quizzes al boot del server (idempotente)
- [x] Foto profilo utente (avatar) end-to-end — sessione serale 26/05: privacy gate fix, componente `AvatarImage` riusabile, EXIF rotation, foto visibile in ProfileScreen/ProfileViewScreen/PartecipantsCard, long-press per rimuovere
- [ ] D3 documentation (in scrittura)
- [ ] M4 (Milestone 4) — deadline 07/06/2026, scheletro `docs/M4_ID6_Ingegneria_del_Software.md` creato in worktree `.claude/`

### Sprint 3 (planned)

- [ ] BLE Mesh SOS prototype
- [ ] OAuth Google login
- [ ] Sentry integration + logging strutturato (Pino)
- [ ] CI con `npm audit` gate
- [ ] Recovery dialog post-crash WAL (UX prompt per riprendere il tracking interrotto)
- [ ] WorkManager per sync robusto anche quando OS killa l'app
- [ ] CMS web admin per quiz (oggi seed JSON in repo)
- [ ] Modalità gara quiz (timer per domanda + leaderboard)
- [ ] Endpoint dedicato `/users/:id/avatar` (estrazione blob dai populate sessione, cache headers ETag)
- [ ] Audit trail admin (chi ha eliminato chi, change-role events)
- [ ] Rate limit con store Redis (preparazione per multi-instance Render paid)

---

## 6. Setup rapido

### Backend

```bash
# Prima volta
npm install
cp .env.example .env
# Edita .env e popola JWT_SECRET, MONGO_URI, BREVO_API_KEY, BASE_URL

# Avvio dev
docker compose up -d mongodb        # MongoDB locale
npm run dev                          # nodemon su localhost:3000
```

### Mobile

```bash
cd mobile
# crea mobile/local.properties:
#   sdk.dir=/path/to/Android/Sdk
#   tsm.api.baseUrl=http://10.0.2.2:3000/   (emulatore Android)

./gradlew compileDebugKotlin         # check sintassi
./gradlew installDebug               # deploy su emulatore connesso
```

### Variabili env critiche (vedi `.env.example`)

- `JWT_SECRET` ≥ 32 char random
- `MONGO_URI` connection string MongoDB
- `BREVO_API_KEY` per email transazionali
- `BASE_URL` URL pubblico backend
- `ALLOWED_ORIGINS` CSV CORS (solo prod)

---

## 7. Riferimenti documenti correlati

- **Sicurezza dettagliata**: [docs/SECURITY.md](SECURITY.md) — threat model, OWASP, ACM, secret management
- **API endpoint**: [docs/api_reference.md](api_reference.md)
- **Architettura componenti**: [docs/architecture.md](architecture.md)
- **MongoDB schema**: [docs/database_schema.md](database_schema.md)
- **Setup mobile**: [docs/setup_mobile.md](setup_mobile.md)
- **Setup backend**: [docs/setup_backend.md](setup_backend.md)
- **Comunicazione client-server**: [docs/android_server_communication.md](android_server_communication.md)
- **Sprint plan generale**: [docs/sprint2_plan.md](sprint2_plan.md)
- **Sprint 2 — Social**: [docs/sprint2_social.md](sprint2_social.md) — feed Strava-like, follow, like, commenti
- **Sprint 2 — Profilo+Formazione**: [docs/sprint2_profilo_formazione.md](sprint2_profilo_formazione.md) — Social Credits con 10 livelli alpini, quiz, NFC totem

---

_Last update: 2026-05-26 — Giacomo Radin (ID-6)_

---

## 8. Cronologia bug fix critici (26 maggio 2026 — sessione autonoma)

Sessione di hardening notturna dopo che l'utente ha segnalato 6 bug post-deploy
Render. Durante l'audit sono stati scoperti **3 bug critici** prima nascosti
oltre ai 6 originali. Test passati da 60/61 a **78/78**, build mobile verde.

### 8.1 [CRITICO] Discriminator persistence (root cause del bug "menu data not persistent")

**Sintomo:** L'utente compilava i dati nel profilo (peso, altezza, livello CAI,
preferenze), tornava indietro e ritrovava i valori di default. Inoltre i crediti
NFC/quiz/sessioni non sembravano cumularsi correttamente.

**Causa:** Tutti i write usavano `User.findByIdAndUpdate(...)` (modello base).
I campi `personalInfo`, `experience`, `preferences`, `weeklyGoals`,
`profileCompletedAt`, `socialCredits`, `nfcStats.*` sono però **solo nel
sub-schema Hiker** (discriminator). Lo strict mode di Mongoose applicato al
modello base scartava silenziosamente l'`$set`/`$inc` — la response tornava
200 OK ma il DB non veniva toccato. Bug invisibile perché:

- Le response 200 facevano pensare a un save riuscito
- I successivi `findById` ritornavano comunque i dati esistenti (per i campi
  letti, la projection MongoDB funziona indipendentemente dal modello)
- Le ViewModel scoped-to-Activity mostravano gli ultimi valori in memoria

**Fix:** Tutti i write su campi discriminator usano ora `Hiker.findByIdAndUpdate`
(o il modello corretto via lookup `role`). File toccati:

- `services/accountService.js` — `updatePersonalInfo/Experience/Preferences/Goals`
  - `markProfileCompleted`
- `services/creditService.js` — `addCredits` ($inc socialCredits)
- `services/nfcService.js` — $inc nfcStats.scansCount/scansCredits
- `services/adminService.js` — `updateAnyUser` con lookup discriminator

**Coverage:** Test in `__tests__/services/discriminator.test.js` (4 test) +
`__tests__/routes/account.test.js` (13 test) che fissano il contratto.

### 8.2 [CRITICO] Anti-cheat enforcement server-side

**Sintomo:** Frontend mostrava lucchetto 🔒 su `birthDate` e `caiLevel` dopo
prima impostazione, ma chiunque poteva aggirare con curl/Postman e abbassare il
livello CAI per farmare crediti facili.

**Fix:** `updatePersonalInfo` / `updateExperience` ora rilanciano
`LockedFieldError` → HTTP 409 se il campo è già impostato. Mobile parsea il
campo `message` del body di errore per UX leggibile.

### 8.3 [QoL] Username con caratteri italiani

`updateAccountSchema.username` riusa ora il regex `usernameField` permissivo
(`/^[a-zA-ZÀ-ÿ0-9\s''.\-]+$/`) della registrazione. Prima era `.min(3)` raw
senza pattern → "Giacomo Radin" passava la registrazione ma falliva il PATCH.

### 8.4 [DX] Test affidabili: rate limit bypass in NODE_ENV=test

I rate limiter ora hanno `skip: () => process.env.NODE_ENV === "test"`.
`__tests__/setup.js` forza `NODE_ENV=test`. Risolve i 429 random nei test
con molte richieste consecutive.

### 8.5 [F13/F14] Schermata profilo read-only + auto-seed quiz

- Nuova `ProfileViewScreen.kt` con 4 sezioni (Dati personali, Esperienza,
  Preferenze, Obiettivi) e indicatori 🔒 sui campi anti-cheat. Route
  `PROFILE_VIEW` + icona AccountCircle nella `ProfileScreen` header.
- `server.js` esegue `autoSeedQuizzes()` al boot se la collection
  `quizcategories` è vuota — risolve il problema "Formazione blank" su Render.
- `FormazioneScreen` ha empty state grafico quando nessuna categoria disponibile.

### 8.6 ViewModel scoping (Activity-wide invece di NavBackStackEntry)

8 schermate (4 edit + 3 onboarding + ProfileScreen) ora usano
`viewModelStoreOwner = LocalContext.current as ComponentActivity` per
condividere un singolo `ProfileV2ViewModel` a livello Activity. Senza Hilt
era l'unica soluzione per evitare che ogni NavBackStackEntry creasse una
nuova istanza con stato perso al popBackStack.

### 8.7 Lock visuale nei campi profilo (lato UI)

`SegmentedChips` (caiLevel) e `BirthDateField` (data nascita) accettano ora
`locked`/`enabled` per disabilitare interazione + alpha(0.6) quando il valore
è già stato salvato. Coerente con il blocco server.

### Stato build & test

- **Backend test:** 78/78 verdi (5 suite + 2 nuove: account.test.js, discriminator.test.js)
- **Mobile build:** `BUILD SUCCESSFUL` debug APK, 39 task
- **Lint:** non eseguito (SSL handshake fallisce in ambiente offline)

---

## 9. Sessione pomeridiana 26/05/2026 — Tier 1-4 (Audit Gemini closure)

Continuazione esplicita dall'utente per implementare i 4 tier rimasti pending
nell'`Audit_Tecnico_Jack.md`. Tutto green a fine sessione: **89/89 test backend,
`compileDebugKotlin` green mobile**, zero breaking change per client esistente.

### 9.1 Tier 1A — Route cleanup ridondante (post Global Error Mapper)

`BUSINESS_ERROR_MAP` esteso da 18 a 24 codici (aggiunti contestuali:
`WRONG_OLD_PASSWORD`, `INVITE_CODE_INVALID`, `ONLY_CREATOR_CAN_UPDATE_SESSION`,
`ONLY_CREATOR_CAN_DELETE_SESSION`, `ONLY_CREATOR_CAN_COMPLETE_SESSION`,
`ONLY_CREATOR_CAN_CANCEL_CHALLENGE`, `TOTEM_TAG_DUPLICATE`).

Fix bug in `resolveBusinessError`: il check parametrico `FIELD_LOCKED:*` veniva
dopo il `if (!mapped) return null` che faceva exit immediato → 2 test
anti-cheat fallivano. Spostato il check `FIELD_LOCKED` PRIMA del lookup map.

Service aggiornati per emettere i nuovi codici: `accountService.js`,
`hikeSessionService.js`, `challengeService.js`.

Route ridotti: **35+ blocchi `if (err.message === "...")` rimossi** dai 6 file
(accountRoutes, activityRoutes, nfcRoutes, quizRoutes, challengeRoutes,
hikeSessionRoutes). Tutti sostituiti da `next(err)` puro. Aggiunti `next` ai
signature handler dove mancava. Mantenuti i 2 blocchi semanticamente unici:

- `err.code === 11000` (E11000 duplicate key) in nfcRoutes per il messaggio
  contestuale "tagId già esistente"
- `err.name === "CastError"` in hikeSessionRoutes per 400 su ObjectId
  malformato (semantica diversa dal generic 500)

### 9.2 Tier 1B — Room migration helper pattern

Nuovo file `data/local/db/TsmMigrations.kt` come single source of truth per
le migration esplicite. Pattern documentato per i futuri bump:

1. Aggiungere `val MIGRATION_N_M = object : Migration(N, M) { ... }`
2. Aggiungerla all'array `ALL`
3. `Room.databaseBuilder.addMigrations(*TsmMigrations.ALL)` la prende automaticamente

`TsmApplication.kt` aggiornato per chiamare `.addMigrations(*TsmMigrations.ALL)`
PRIMA di `.fallbackToDestructiveMigration()`, così Room preferisce sempre la
migration esplicita al wipe distruttivo.

### 9.3 Tier 2 — `HikeSession.meetingDate` String → Date (con backward compat 100%)

Cambio schema: `meetingDate: { type: Date, set: parseMeetingDate }` con setter
che accetta sia "YYYY-MM-DD" (formato legacy del mobile) sia ISO 8601 / Date.
Aggiunto transform `toJSON`/`toObject` che converte Date → "YYYY-MM-DD" in
output, così il client mobile riceve esattamente la stessa stringa di prima.

Indice composto `{ status: 1, meetingDate: 1 }` per la query frequente
"sessioni dell'utente ordinate per data".

Joi validation: `meetingDateField` con `Joi.alternatives` accetta entrambi i
formati. Applicato a `createSessionSchema` e `updateSessionSchema`.

Migration script `backend/migrations/2026-05-26-meetingDate-string-to-date.js`:

- Connessione MongoDB via `MONGODB_URI` (o `MONGO_URI` fallback)
- Cursor su `hikesessions` con `meetingDate: { $type: 2 }` (BSON String)
- Parse "YYYY-MM-DD" → UTC midnight, update in place
- Idempotente (skip doc già Date, $type: 9)
- Report finale con counter migrated/skipped/errors + sample errori
- Lo script va eseguito MANUALMENTE prima di deployare il backend in prod
  (non runna in CI/server boot per evitare effetti collaterali)

3 nuovi test in `session.test.js`: formato output identico, BSON Date in DB,
sort cronologico funziona via `$sort: { meetingDate: 1 }`.

### 9.4 Tier 3 — Refresh token rotation con replay detection

**Backend:**

- Nuovo model `models/refreshToken.js`: hash SHA-256 (mai raw in DB),
  `family` UUID per rotation chain, `replacedBy` link per detection replay,
  TTL index 30 giorni (auto-cleanup MongoDB).
- Nuovo service `services/refreshTokenService.js`:
  - `generateAccessToken(user)` — JWT con `type: "access"` claim
  - `issueRefreshToken(userId, {family, userAgent})` — random 96 hex
  - `rotateRefreshToken(raw, {userAgent})` — valida, revoca, emette nuova
    coppia. **Detection replay**: se il token già revocato (replacedBy != null)
    viene riusato, revoca tutta la family → user deve fare re-login
    (assumiamo esfiltrazione).
  - `revokeRefreshToken(raw)` — logout single device (idempotente)
  - `revokeAllForUser(userId)` — logout su tutti i device
- `authService.js`:
  - `loginUser` ora emette `{ token, accessToken, refreshToken, refreshExpiresAt }`.
    Il campo `token` è alias backward-compat di `accessToken`.
  - Nuove `refreshTokens(req, res)` e `logout(req, res)`.
- `authRoutes.js`: `POST /auth/refresh` (con `loginLimiter` rate limit anti-brute)
  e `POST /auth/logout`.
- ACCESS_TTL configurabile via `JWT_ACCESS_TTL` env (default "15m"). Fallback
  su `JWT_EXPIRES_IN` per backward compat con env Render attuale.
- 8 nuovi test in `__tests__/routes/refreshToken.test.js`: login emit, family
  diverse per login multipli, rotation valida, refresh inventato → 401,
  refresh mancante → 400, replay attack → 401 + revoca family, logout idempotente.

**Mobile:**

- `TokenStorage.kt`: nuova `saveTokens(access, refresh, expiresAtIso)`,
  `getRefreshToken()`, `getRefreshExpiresAtIso()`. Backward compat con
  `saveToken(token)` legacy.
- `LoginResponse.kt`: campi nullable `accessToken`, `refreshToken`,
  `refreshExpiresAt`. Aggiunti `RefreshRequest` e `LogoutRequest`.
- `TsmApiService.kt`: `refresh()` e `logout()` endpoints.
- Nuovo `TsmAuthenticator.kt` (OkHttp `Authenticator`): intercetta 401, fa
  refresh sincrono via `OkHttpClient` interno (NO Authenticator chain →
  evita loop), salva la nuova coppia, ritenta la request originale con
  nuovo Bearer. **Trasparente per i ViewModel.** Mutex su refresh per
  evitare N refresh paralleli se più request scadono insieme.
- `TsmApiClient.kt`: `.authenticator(TsmAuthenticator(tokenStorage))`.
- `AuthRepositoryImpl.kt`: usa `saveTokens` invece di `saveToken`.

### 9.5 Tier 4 — `RegistraViewModel` refactor + WAL Room v5

**Bump Room v4 → v5** con migration esplicita `TsmMigrations.MIGRATION_4_5`
che CREATE TABLE `tracking_wal` + INDEX su `track_id`. Preserva tutti i dati
esistenti (zero perdita di `completed_activities` con `isSynced=0`).

Nuovo `data/local/db/TrackingWalEntity.kt` + `TrackingWalDao.kt`: WAL per i
punti GPS durante un tracking attivo. **Risolve crash-safety**: prima i punti
GPS vivevano SOLO in memoria nel `HikeTrackingEngine` — un crash perdeva
TUTTO. Ora ogni snapshot è un INSERT immediato.

**Estratti 2 repository:**

- `repository/TrackingPersistenceRepository.kt`: `startTrack()` (UUID),
  `appendPoint()` (insert WAL), `finalize(snapshot)` (legge WAL, sample 200pt,
  insert in `completed_activities`, cleanup WAL), `discardTrack()`.
- `repository/SessionCommandRepository.kt`: `markSessionActive(id)` (fire-and-
  forget PATCH status ACTIVE), `completeOrUpload(...)` (PATCH /complete o
  POST /activities a seconda di `sessionId`, con fallback a SyncManager se
  upload fallisce). Ritorna `SyncResult.Synced(remoteId?)` o `Pending`.

**`RegistraViewModel` refactor:**

- Da 547 → 501 righe + responsabilità chiare (orchestrator UI/lifecycle only).
- Nuovo field `currentTrackId: String?` — non-null sse `trackingStatus != IDLE`.
- `startTracking()` → `persistence.startTrack()`.
- `applyLocation()` → `persistence.appendPoint()` se RECORDING.
- `discardTracking()` → `persistence.discardTrack(orphanId)`.
- `confirmStopTracking()` → `persistence.finalize(snapshot)` →
  `sessionCommands.completeOrUpload(...)` → `dao.markSynced()` se OK.
- Rimossi import non più usati: `Gson`, `CompletedActivityEntity`,
  `CreateActivityRequest`, `CompleteSessionRequest`, `UpdateSessionStatusRequest`,
  `ActualStats`, `HikeEstimation`, `SimpleDateFormat`, `Locale`, `Date`,
  `UUID`, `TsmApiClient`, `SyncManager`.

`LocationRepository` non estratto: la VM non aveva logica di search/nearby
(vive in `WeatherViewModel` e altri). Recovery dialog post-crash dalla WAL
rimane TODO Sprint 3.

### Stato finale build & test

- **Backend test:** 89/89 verdi (+11 vs sessione notturna: 3 meetingDate + 8 refresh)
- **Mobile build:** `compileDebugKotlin` BUILD SUCCESSFUL, solo deprecation warnings
- **Audit Gemini:** 9/9 azioni richieste implementate (8 fatte tra notte+pomeriggio,
  1 parziale già documentata)

---

## 10. Sessione serale 26/05/2026 — Feature foto profilo end-to-end

Sessione richiesta esplicitamente dall'utente ("sto cercando di implementare la
foto profilo con scarso successo, aiutami a sistemarla"). Il lavoro pregresso
sul branch `UI` (5 commit avatar) aveva già messo le fondamenta ma con problemi
strutturali che impedivano alla foto di apparire correttamente, soprattutto
nelle altre schermate. Tutti i fix completati in-session.

### 10.1 Sintomo originale e root cause

**Sintomo riportato:** "Upload OK ma foto non appare."

**Root cause (3 cause concorrenti, tutte fixate):**

1. **`BitmapFactory.decodeByteArray` ritornava `null` → Box vuoto** — il codice
   inline in `ProfileScreen.kt` aveva `if (bitmap != null) Image(...)` ma
   nessun ramo `else` con fallback. Una qualsiasi decode fallita (Base64
   corrotto, char extra) lasciava un cerchio vuoto.
2. **Decodifica Base64 ad ogni ricomposizione su main thread** — non c'era
   `remember(avatarUrl)` → ogni cambio di stato ridecodificava ~100 KB di
   bytes su UI thread (jank + spreco batteria).
3. **`personalInfo` response merge debole** — se il body del `PATCH` arrivava
   troncato o senza il sub-document completo, lo state restava col vecchio
   `personalInfo` (senza il nuovo avatarUrl) e l'UI mostrava le iniziali.

### 10.2 Fix critici backend

#### `User.avatarUrl` morto rimosso

`backend/src/models/user.js`: il campo `avatarUrl` era nello schema base ma
**nessuno scriveva/leggeva** lì (tutti gli write andavano su
`Hiker.personalInfo.avatarUrl`). Source of truth ora univoca →
`Hiker.personalInfo.avatarUrl`.

#### Privacy gate (`utils/userPrivacy.js`)

`stripPrivateFields` cancellava l'**intero** `personalInfo` per gli "other
viewer", impedendo di mostrare l'avatar nei partecipanti delle sessioni.

**Fix:** introduzione di `PERSONAL_INFO_PUBLIC_FIELDS = ["avatarUrl"]`. Per
viewer "other" ora:

- `personalInfo` mantiene solo `avatarUrl` (gli altri campi sex/birthDate/
  heightCm/weightKg restano privati)
- Se nessun campo pubblico è valorizzato → la chiave viene rimossa per non
  sporcare la response con un oggetto vuoto

#### Populate sessioni

`backend/src/services/hikeSessionService.js`: 8 occorrenze di
`populate(..., "username email")` → `"username email personalInfo.avatarUrl"`
(creatorId + participants.userId in createSession, getSessionById,
getSessionsByUser, updateSessionDetails).

#### Validazione Joi stretta

`backend/src/middleware/validationMiddleware.js`: nuovo
`avatarDataUriField` con pattern stretto
`^data:image/(jpeg|jpg|png|webp);base64,[A-Za-z0-9+/=]+$` + messaggi custom.
Accetta `""` per il flow "rimuovi foto" (`.allow(null, "")` bypassa il
pattern, by design Joi).

#### Body limit a 5 MB

`backend/src/middleware/securityMiddleware.js`: `requestSizeLimit` portato
da "2mb" a "5mb" (commit del lavoro pregresso uncommitted in branch UI).
Lascia margine per foto 500 px JPEG q70 anche a quality più alta in futuro.

### 10.3 Componente mobile riusabile

#### `ui/components/AvatarImage.kt` (nuovo)

Composable circolare riusabile in tutta l'app:

- **Decode Base64 memoizzato** con `remember(avatarUrl)` → un solo decode per
  ogni valore distinto di URL (risolve causa #2).
- **Fallback iniziali** se decode fallisce o URL è null/blank (risolve causa #1).
- **Colore di sfondo deterministico** dal hash dello username — stesso utente
  sempre stesso colore (palette 8 tonalità outdoor).
- **Overlay loader** (parametro `isLoading: Boolean`) → CircularProgressIndicator
  bianco su sfondo semi-trasparente sopra l'avatar quando il VM sta uploadando
  o rimuovendo.
- **Helpers visibili al test**: `initialsFrom(name)`, `deterministicAvatarColor(seed)`.

#### `ui/util/AvatarUtils.kt` (nuovo)

Utility per gestire la foto end-to-end:

- `loadOrientedBitmapFromUri(resolver, uri)`: legge bytes, parsea EXIF tag
  `TAG_ORIENTATION`, applica `Matrix.postRotate` (gestisce ROTATE_90/180/270 +
  FLIP_HORIZONTAL/VERTICAL + TRANSPOSE/TRANSVERSE). Risolve foto camera in
  portrait che apparivano ruotate.
- `downscaleToBox(bitmap, maxSide=500)`: usa il lato **maggiore** (non solo
  width come il vecchio codice) così foto verticali non restano enormi sull'altezza.
- `encodeToDataUri(bitmap, q=70)`: JPEG + `Base64.NO_WRAP` (no newline → safe
  per JSON + regex Joi).
- `decodeDataUri(dataUri)`: robusto sui prefissi (taglia fino a `base64,`),
  ritorna `null` invece di crashare.
- `prepareAvatarForUpload(resolver, uri)`: pipeline completa che il
  `ProfileScreen` invoca su `Dispatchers.IO`.

### 10.4 Wiring nelle schermate

#### `ui/screens/profile/ProfileScreen.kt`

- Photo picker (`ActivityResultContracts.GetContent`) ora delega ad
  `AvatarUtils.prepareAvatarForUpload` in `withContext(Dispatchers.IO)`.
- Avatar 64 dp interattivo: `combinedClickable` con tap = picker, long-press =
  dialog "Rimuovi foto" (visibile solo se `hasAvatar`).
- Icona badge cambiata da `Settings` a `CameraAlt` per chiarezza UX.
- `LaunchedEffect` su `sectionSuccess/sectionError` → Toast + clear messages.

#### `ui/screens/profile/ProfileViewScreen.kt`

- Aggiunto header row con `AvatarImage` 88 dp + username + email sopra le
  sezioni dati. Prima era solo testo.

#### `ui/screens/session/SessionDetailScreen.kt` (PartecipantsCard)

- Sostituito il Box con iniziali colorate (`avatarColorFor` rimosso) con
  `AvatarImage` 40 dp. Wrapper esterno mantiene il bordo accent per il
  creator senza interferire con il clip circolare.
- Foto reale dei partecipanti ora visibile grazie al populate aggiornato
  lato backend (sezione 10.2).

### 10.5 ViewModel changes

`ProfileV2ViewModel.kt`:

- **`uploadAvatar(dataUri)`** ora merge-safe: se `resp.body()?.personalInfo`
  è null (caso patologico response gzip troncata), fa fallback al
  `_state.personalInfo?.copy(avatarUrl = dataUri)` invece di perdere il
  nuovo URL (risolve causa #3 della root cause).
- **`removeAvatar()` (nuovo)**: optimistic update (UI mostra subito le
  iniziali) + rollback automatico se il server risponde errore. Invia
  `avatarUrl=""` (accettato da Joi via `.allow("")`).

### 10.6 Build & dipendenze

- Nuova dep: `androidx.exifinterface:1.3.7` in `libs.versions.toml` +
  `app/build.gradle.kts`.
- DTO update: `SessionUserInfo` ora ha `personalInfo: SessionUserPersonalInfo?`
  con helper `.avatarUrl` (proxy del campo nested).

### Stato finale build & test (sessione serale)

- **Backend test:** 88/89 verdi (1 test `POST /auth/register/hiker` fallisce
  per `BREVO_API_KEY` mancante in env di test — **pre-esistente**, nessuna
  delle 5 modifiche backend tocca quel path; mockare `emailService` lo
  renderebbe hermetico, è in tech debt).
- **Mobile build:** `compileDebugKotlin` BUILD SUCCESSFUL in 1m 7s, solo
  deprecation warnings pre-esistenti (TokenStorage EncryptedSharedPreferences,
  Room `fallbackToDestructiveMigration`, alcune Icons.Outlined → AutoMirrored).
- **File toccati totali:** 5 backend (user.js, userPrivacy.js,
  hikeSessionService.js, validationMiddleware.js, securityMiddleware.js)
  - 6 mobile (SessionResponse.kt, ProfileV2ViewModel.kt, ProfileScreen.kt,
    ProfileViewScreen.kt, SessionDetailScreen.kt, libs.versions.toml +
    app/build.gradle.kts) + 2 nuovi mobile (AvatarImage.kt, AvatarUtils.kt).

### Lezione di processo (consolidata da Sprint 1 + Sprint 2)

Anche stavolta vale il pattern visto nelle sessioni notturna e pomeridiana
del 26/05: **un sintomo riportato in UI (qui "foto non appare") aveva 3
cause concorrenti** (no fallback decode, no memoization, weak state merge),
solo una delle quali era ovvia dal codice. Senza l'audit a 2 passi prima
del commit, due delle tre sarebbero rimaste in produzione e il "bug" sarebbe
ricomparso a colpi singoli su utenti diversi.
