# Sprint 2 — Profilo + Formazione + Totem NFC

> Piano operativo per implementare la schermata Profilo rinnovata, il sistema di gamification
> Social Credits con livelli, il modulo Formazione (quiz), e il checkpoint NFC.
> Riferimento mockup: 3 schermate (Profilo, Formazione, Quiz Q&A).
> Documento generato il 2026-05-24 come input alla D3 e al planning di Sprint 2.

---

## 1. Decisioni di prodotto (confermate)

| Decisione | Valore |
|---|---|
| Fonti di Social Credits | **Sessioni di gruppo + Attività libere + Quiz superati + Totem NFC scansionati** |
| Sistema livelli | **10+ livelli con nomi tematici alpini** (vedi tabella sez. 3) |
| Quiz lifecycle | **Crediti unici al primo superamento**. Ripetizione possibile per esercitarsi, ma niente crediti extra. |
| Contenuto quiz | **Seed JSON hardcoded** in `backend/seed/quizzes.json`. Versionato con il repo. UniTrento contribuisce contenuto. |
| Anti-frode NFC | **Tag UUID statico + verifica GPS proximity ≤50m** dalla location registrata del totem |
| Quiz UX | **Feedback immediato dopo ogni risposta** (Corretto!/Sbagliato! + spiegazione) |
| Quiz timer | **Nessuno** per Sprint 2. Modalità gara in Sprint 3. |
| Account edit scope | Username + email (con re-verify), cambio password, obiettivi settimanali, logout, cancella account (GDPR) |

---

## 2. Modello dati

### 2.1 Backend (Mongoose schemas)

```js
// backend/src/models/user.js (modifiche)
// Aggiungere questi campi al baseSchema:

socialCredits:        { type: Number, default: 0, min: 0, index: true },
// Denormalizzato: totale sempre aggiornato via $inc atomic.
// Storia transazioni in collection separata (vedi creditTransaction).

weeklyGoals: {
  km:     { type: Number, default: 0, min: 0, max: 500 },
  elevM:  { type: Number, default: 0, min: 0, max: 20000 },
  count:  { type: Number, default: 0, min: 0, max: 50 },
},

// Contatori NFC denormalizzati per evitare aggregation nel ProfileScreen
nfcStats: {
  scansCount:   { type: Number, default: 0 },
  scansCredits: { type: Number, default: 0 },
},
```

```js
// backend/src/models/creditTransaction.js (nuovo)
// Audit log immutabile di ogni accredito/storno crediti.
const creditTransactionSchema = new Schema({
  userId:    { type: ObjectId, ref: "User", required: true, index: true },
  amount:    { type: Number, required: true },          // può essere negativo (storno)
  source:    {
    type: String,
    enum: ["session", "free_activity", "quiz", "nfc", "admin_adjust"],
    required: true,
  },
  refId:     { type: ObjectId, required: false },       // id riferimento (sessionId/activityId/quizId/totemId)
  refKind:   { type: String, required: false },         // discriminator del refId per polymorphism
  note:      { type: String, maxlength: 200 },          // breve testo human-readable
  createdAt: { type: Date, default: Date.now, index: true },
});
creditTransactionSchema.index({ userId: 1, createdAt: -1 });
```

```js
// backend/src/models/quizCategory.js (nuovo)
const quizCategorySchema = new Schema({
  slug:        { type: String, required: true, unique: true },   // "sicurezza-alpina"
  name:        { type: String, required: true },                 // "Sicurezza Alpina"
  description: { type: String, maxlength: 500 },
  color:       { type: String, required: true },                 // hex "#FFB74D"
  sortOrder:   { type: Number, default: 0 },
  iconName:    { type: String, default: "school" },              // material icon
});
```

```js
// backend/src/models/quiz.js (nuovo)
const quizQuestionSchema = new Schema({
  text:          { type: String, required: true, maxlength: 500 },
  choices:       {
    type: [{ type: String, maxlength: 200 }],
    validate: { validator: (v) => v.length === 4, message: "Esattamente 4 opzioni" },
  },
  correctIndex:  { type: Number, required: true, min: 0, max: 3 },
  explanation:   { type: String, required: true, maxlength: 500 },
}, { _id: true });

const quizSchema = new Schema({
  categoryId:     { type: ObjectId, ref: "QuizCategory", required: true, index: true },
  title:          { type: String, required: true },           // "Sentieri attrezzati"
  description:    { type: String, maxlength: 500 },
  questions:      [quizQuestionSchema],                       // 8-15 domande
  passThreshold:  { type: Number, default: 0.7 },             // 70% per superare
  creditsReward:  { type: Number, default: 25, min: 0, max: 1000 },
  sortOrder:      { type: Number, default: 0 },
  createdAt:      { type: Date, default: Date.now },
});
```

```js
// backend/src/models/quizAttempt.js (nuovo)
const quizAttemptSchema = new Schema({
  userId:           { type: ObjectId, ref: "User", required: true, index: true },
  quizId:           { type: ObjectId, ref: "Quiz", required: true, index: true },
  answers:          [{ questionId: ObjectId, choiceIndex: Number }],
  correctCount:     { type: Number, required: true },
  totalQuestions:   { type: Number, required: true },
  passed:           { type: Boolean, required: true },
  creditsAwarded:   { type: Number, default: 0 },             // > 0 solo al primo passed
  createdAt:        { type: Date, default: Date.now },
});
// Unique index (userId, quizId, passed=true) — per fast lookup del primo passed
quizAttemptSchema.index({ userId: 1, quizId: 1, passed: 1 });
```

```js
// backend/src/models/nfcTotem.js (nuovo)
const nfcTotemSchema = new Schema({
  tagId:         { type: String, required: true, unique: true, index: true },   // UUID univoco
  name:          { type: String, required: true },                              // "Cima Tosa - Vetta"
  description:   { type: String, maxlength: 500 },
  location: {
    type: { type: String, enum: ["Point"], default: "Point" },
    coordinates: { type: [Number], required: true },          // [lon, lat]
  },
  altitude:      { type: Number },                            // metri s.l.m. opzionale
  radius:        { type: Number, default: 50 },               // metri di tolleranza GPS
  creditsReward: { type: Number, default: 25, min: 0, max: 500 },
  kind:          { type: String, enum: ["checkpoint", "summit", "refuge"], default: "checkpoint" },
  active:        { type: Boolean, default: true },
  createdAt:     { type: Date, default: Date.now },
});
nfcTotemSchema.index({ location: "2dsphere" });
```

```js
// backend/src/models/nfcScan.js (nuovo)
// Audit log degli scan: chi, dove, quando. Anti-frode.
const nfcScanSchema = new Schema({
  userId:         { type: ObjectId, ref: "User", required: true, index: true },
  totemId:        { type: ObjectId, ref: "NfcTotem", required: true, index: true },
  tagId:          { type: String, required: true },
  scannedAt:      { type: Date, default: Date.now },
  gpsLocation: {
    type: { type: String, enum: ["Point"], default: "Point" },
    coordinates: { type: [Number], required: true },
  },
  distanceFromTotem: { type: Number, required: true },         // metri al momento dello scan
  creditsAwarded:    { type: Number, default: 0 },             // 0 se rate-limited o fuori range
  rejectionReason:   { type: String },                         // "OUT_OF_RANGE" | "RATE_LIMIT" | null
});
// Unique compound: userId+totemId+(stesso giorno) per rate-limit naturale
// Implementato via service check, non index (per supportare scan futuri).
```

### 2.2 Mobile (Room)

```kotlin
// data/local/db/CachedQuizEntity.kt (nuovo)
// Cache locale per accedere ai quiz già scaricati (offline-friendly).
@Entity(tableName = "cached_quizzes")
data class CachedQuizEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "questions_json") val questionsJson: String,  // serializzato (no correctIndex se mostrato pre-submit)
    @ColumnInfo(name = "credits_reward") val creditsReward: Int,
    @ColumnInfo(name = "is_completed_locally") val isCompletedLocally: Boolean = false,
    @ColumnInfo(name = "fetched_at_ms") val fetchedAtMs: Long,
)

// data/local/db/PendingQuizSubmissionEntity.kt (nuovo)
// Submission offline da risincronizzare quando l'utente torna online.
@Entity(tableName = "pending_quiz_submissions")
data class PendingQuizSubmissionEntity(
    @PrimaryKey val id: String,                                  // UUID locale
    @ColumnInfo(name = "quiz_id") val quizId: String,
    @ColumnInfo(name = "answers_json") val answersJson: String,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
)
```

`TsmDatabase` v6 → aggiunge le due tabelle.

---

## 3. Sistema Livelli (10 livelli alpini)

Soglie cumulative in Social Credits. Calibrate sul mockup (Lv4=Alpinista a 1.272 crediti, +228 a Lv5=1.500):

| Lv | Nome | Min | Max | Note |
|----|---|---|---|---|
| 1 | Sentiero | 0 | 249 | start |
| 2 | Rifugio | 250 | 499 | prima soglia "non principiante" |
| 3 | Bivacco | 500 | 999 | utente attivo |
| 4 | **Alpinista** | 1.000 | 1.499 | livello del mockup |
| 5 | Cima | 1.500 | 2.499 | |
| 6 | Esploratore | 2.500 | 3.999 | |
| 7 | Veterano | 4.000 | 5.999 | |
| 8 | Guida Alpina | 6.000 | 8.999 | |
| 9 | Maestro | 9.000 | 12.999 | |
| 10 | Leggenda Alpina | 13.000 | ∞ | endgame |

Helper `LevelCalculator.computeLevel(credits): { current, name, min, max, next, progressPct, creditsToNext }` esposto sia in backend service che in mobile (kotlin replica).

Backend (`backend/src/services/levelService.js`):
```js
const LEVELS = [
  { lv: 1, name: "Sentiero",         min: 0,     max: 249 },
  { lv: 2, name: "Rifugio",          min: 250,   max: 499 },
  // ...
  { lv: 10, name: "Leggenda Alpina", min: 13000, max: Infinity },
];
export function computeLevel(credits) { /* ... */ }
```

Mobile (`mobile/.../data/gamification/LevelCalculator.kt`): replica identica.

---

## 4. Categorie quiz (seed iniziale)

Dal mockup, 5 categorie con colori:

| Slug | Nome | Colore | Sort |
|---|---|---|---|
| `sicurezza-alpina` | Sicurezza Alpina | `#FFB74D` (arancione) | 1 |
| `flora-fauna` | Flora & Fauna | `#81C784` (verde) | 2 |
| `regolamenti-cai-sat` | Regolamenti CAI/SAT | `#4FC3F7` (azzurro) | 3 |
| `meteorologia` | Meteorologia | `#9575CD` (viola) | 4 |
| `primo-soccorso` | Primo Soccorso | `#F06292` (rosa) | 5 |

Quiz per categoria: 8–15 nel mockup (Sicurezza 12, Flora 10, Regolamenti 8, Meteo X, Primo Soccorso 15 = ~55 totali). Numero esatto da definire col contenuto UniTrento.

Domande per quiz: ~12 (dal mockup "Domanda 4 di 12"). Configurabile per quiz.

Crediti reward per quiz: variabile (campo `Quiz.creditsReward`). Suggerito: 20–40 crediti per quiz semplice, 60–100 per quiz complesso. UniTrento decide il bilanciamento.

---

## 5. Endpoint API

Tutti sotto `/api/v1/`, richiedono `authenticate` + `authenticatedLimiter` salvo dove indicato.

### 5.1 Crediti

| Method | Path | Body/Query | Risposta |
|---|---|---|---|
| `GET` | `/users/me/credits` | — | `{ total, level: {lv, name, min, max, next: {lv, name, min}, progressPct, creditsToNext} }` |
| `GET` | `/users/me/credits/history` | `?page=&limit=&source=` | `{ items: [Transaction], hasMore }` |

Note: il `total` su `User.socialCredits` è denormalizzato e sempre allineato. Source enum filter opzionale.

### 5.2 Quiz

| Method | Path | Body/Query | Risposta |
|---|---|---|---|
| `GET` | `/quiz/categories` | — | `[{ category, totalQuizzes, passedByMe, totalCredits, earnedByMe, progressPct }]` |
| `GET` | `/quiz/categories/:slug/quizzes` | — | `[{ quiz: {id, title, totalQuestions, creditsReward}, passedByMe, completedAt }]` |
| `GET` | `/quiz/:id` | — | `{ id, title, questions: [{id, text, choices}] }` ⚠ NIENTE correctIndex |
| `POST` | `/quiz/:id/submit` | `{ answers: [{questionId, choiceIndex}] }` | `{ score, correctCount, totalQuestions, passed, creditsAwarded, breakdown: [{questionId, choiceIndex, isCorrect, correctIndex, explanation}], newTotalCredits }` |

**Security cruciale**: `GET /quiz/:id` non espone mai `correctIndex` o `explanation`. Le risposte corrette + spiegazioni si scoprono solo POST submit dopo aver risposto. Mobile non può "spiare" le risposte ispezionando network.

**Submit logic** (server-side in `quizService.submitQuiz`):
```js
1. Carica quiz da DB
2. Per ogni answer in body, confronta con correctIndex
3. Calcola score = correctCount / totalQuestions
4. passed = score >= quiz.passThreshold
5. Cerca QuizAttempt esistente con (userId, quizId, passed=true)
   - Se esiste E new attempt also passed: creditsAwarded = 0 (no double-reward)
   - Se non esiste E new attempt passed: creditsAwarded = quiz.creditsReward
6. Crea QuizAttempt con creditsAwarded
7. Se creditsAwarded > 0:
   - $inc User.socialCredits by creditsAwarded
   - Crea CreditTransaction { source: "quiz", refId: quiz._id, amount }
8. Ritorna breakdown completo (ora SI` include correctIndex + explanation)
```

### 5.3 NFC Totem

| Method | Path | Body/Query | Risposta |
|---|---|---|---|
| `GET` | `/nfc/totems` | `?lon=&lat=&maxDistance=` (opz) | `[{ tagId, name, location, kind, creditsReward }]` |
| `POST` | `/nfc/scan` | `{ tagId, gpsLon, gpsLat }` | `{ ok, creditsAwarded, totem, distance, alreadyScannedToday, newTotalCredits }` |
| `GET` | `/users/me/nfc-history` | `?page=` | `[{ totem, scannedAt, creditsAwarded }]` |

**Scan validation logic** (`nfcService.scanTotem`):
```js
1. Trova totem per tagId. 404 se non esiste o non active.
2. Calcola distanza GPS (haversine) tra (gpsLon, gpsLat) e totem.location.coordinates.
3. Se distance > totem.radius (default 50m):
   - Crea NfcScan con rejectionReason="OUT_OF_RANGE", creditsAwarded=0
   - Ritorna { ok: false, reason: "OUT_OF_RANGE", distance }
4. Cerca NfcScan esistente (userId, totemId, scannedAt > 24h ago):
   - Se trovato: creditsAwarded=0, alreadyScannedToday=true
5. Altrimenti creditsAwarded = totem.creditsReward
6. Crea NfcScan record
7. Se creditsAwarded > 0:
   - $inc User.socialCredits
   - $inc User.nfcStats.scansCount, nfcStats.scansCredits
   - Crea CreditTransaction { source: "nfc", refId: totemId }
8. Ritorna esito
```

### 5.4 Account

| Method | Path | Body | Risposta |
|---|---|---|---|
| `PATCH` | `/users/me` | `{ username?, email? }` | `200 { user, requiresEmailVerification }` |
| `POST` | `/auth/change-password` | `{ oldPassword, newPassword }` | `200 { message }` |
| `DELETE` | `/users/me` | `{ password }` (conferma) | `200 { message }` + cascade delete |
| `PATCH` | `/users/me/goals` | `{ km?, elevM?, count? }` | `200 { weeklyGoals }` |

Note:
- PATCH email: invia nuova SMTP verification, `isVerified = false` finché click. JWT corrente resta valido (l'auth si basa su userId, non email).
- Change-password: revoca tutti i JWT esistenti? Per ora no (richiede blacklist), ma è sicuro per Sprint 2 (next login emette nuovo JWT).
- Delete account (GDPR): cascade su HikeSession (lascia "Utente eliminato" come placeholder nei partecipanti), Activity (cancella), Comment/Like (cancella), CreditTransaction (anonymize), NfcScan (anonymize), Follow (cancella).

### 5.5 Cross-cutting: aggiornamento crediti su completamento sessione/attività

I crediti da `session` e `free_activity` non hanno endpoint dedicati: vengono accreditati automaticamente quando l'attività passa a COMPLETED.

Modifica a `hikeSessionService.completeSession` e `activityService.createActivity`:
```js
// Dopo aver salvato la session/activity con actualStats:
const credits = actualStats?.finalPoints ?? 0;
if (credits > 0) {
  await User.findByIdAndUpdate(userId, { $inc: { socialCredits: credits } });
  await CreditTransaction.create({
    userId, amount: credits, source: "session", refId: session._id,
  });
}
```

⚠ Idempotency: se l'utente chiama `completeSession` due volte (retry), non rivogliamo accreditare due volte. Usare il check `session.status === "COMPLETED"` PRIMA della modifica (già presente). Aggiungere comunque `session.creditsAwardedAt: Date?` per audit.

---

## 6. Mobile architecture

### 6.1 Nuovi file Kotlin

```
mobile/app/src/main/java/it/trentosmartmountain/app/
├─ data/
│  ├─ gamification/
│  │  └─ LevelCalculator.kt           # replica logica server-side
│  ├─ local/db/
│  │  ├─ CachedQuizEntity.kt          # nuovo
│  │  ├─ CachedQuizDao.kt             # nuovo
│  │  ├─ PendingQuizSubmissionEntity.kt
│  │  └─ PendingQuizSubmissionDao.kt
│  └─ remote/
│     ├─ dto/
│     │  ├─ CreditsResponse.kt        # GET /users/me/credits
│     │  ├─ CreditTransactionResponse.kt
│     │  ├─ QuizCategoryResponse.kt
│     │  ├─ QuizListItemResponse.kt
│     │  ├─ QuizDetailResponse.kt     # SENZA correctIndex
│     │  ├─ QuizSubmissionRequest.kt
│     │  ├─ QuizSubmissionResponse.kt # CON breakdown completo
│     │  ├─ NfcTotemResponse.kt
│     │  ├─ NfcScanRequest.kt
│     │  ├─ NfcScanResponse.kt
│     │  └─ AccountUpdateRequest.kt
│     └─ TsmApiService.kt             # estesa
├─ repository/
│  ├─ CreditsRepository.kt + Impl     # wallet + history + level
│  ├─ QuizRepository.kt + Impl        # categorie + dettagli + submit offline-first
│  ├─ NfcRepository.kt + Impl
│  └─ AccountRepository.kt + Impl     # patch username/email, password, delete
├─ viewmodel/
│  ├─ ProfileViewModel.kt             # rinnovato con credits+level+nfc stats
│  ├─ FormazioneViewModel.kt          # lista categorie
│  ├─ QuizViewModel.kt                # Q&A state machine
│  ├─ NfcScanViewModel.kt             # scansione + GPS + chiamata backend
│  ├─ AccountEditViewModel.kt
│  ├─ ChangePasswordViewModel.kt
│  └─ WeeklyGoalsViewModel.kt         # (già nel piano social)
└─ ui/screens/
   ├─ profile/
   │  ├─ ProfileScreen.kt              # rinnovato (oggi minimo)
   │  ├─ CreditsCard.kt                # componente card 1.272 + livello + barra
   │  ├─ KpiGrid.kt                    # 3 colonne escursioni/km/dislivello
   │  ├─ NfcTotemCard.kt               # con chip NFC ATTIVO + counters
   │  ├─ FormazioneCard.kt             # entry verso schermata Formazione
   │  ├─ AccountEntryRow.kt            # voce "Account e dati personali"
   │  ├─ AccountEditScreen.kt          # form username/email
   │  ├─ ChangePasswordScreen.kt
   │  └─ DeleteAccountScreen.kt        # zona pericolo + conferma password
   ├─ formazione/
   │  ├─ FormazioneScreen.kt           # lista categorie con bordo colorato
   │  ├─ CategoryCard.kt               # componente categoria
   │  └─ QuizListScreen.kt             # lista dei quiz dentro una categoria (opzionale, mockup non lo mostra)
   ├─ quiz/
   │  ├─ QuizScreen.kt                 # Q&A flow con feedback immediato
   │  ├─ QuizQuestionCard.kt
   │  ├─ QuizOptionRow.kt              # opzione con radio + stato (idle/correct/wrong)
   │  ├─ QuizFeedbackBox.kt            # box "Corretto!" verde / "Sbagliato" rosso
   │  └─ QuizResultScreen.kt           # riepilogo finale: score, crediti, riprova
   └─ nfc/
      ├─ NfcScanScreen.kt              # full-screen con animazione scanner
      └─ NfcResultScreen.kt            # successo / fuori range / già scansionato
```

### 6.2 File esistenti modificati

| File | Cosa cambia |
|---|---|
| `ProfileScreen.kt` | Rifatto con sezioni card (sostituisce versione corrente) |
| `TsmDatabase.kt` | Bump v5 → v6 + add CachedQuizDao, PendingQuizSubmissionDao |
| `TsmApplication.kt` | Init NFC adapter check, log se non disponibile |
| `Routes.kt` | Aggiungere `FORMAZIONE`, `QUIZ`, `QUIZ_RESULT`, `NFC_SCAN`, `ACCOUNT_EDIT`, `CHANGE_PASSWORD`, `DELETE_ACCOUNT` |
| `TsmNavHost.kt` | Compose routes per gli screen sopra |
| `TsmApiService.kt` | ~20 nuovi endpoint Retrofit |
| `AndroidManifest.xml` | Permission `NFC` + uses-feature `android.hardware.nfc` (not required) |
| `hikeSessionService.js` | Accredito crediti su completeSession |
| `activityService.js` | Accredito crediti su createActivity |

---

## 7. Piano fasi (8 fasi)

Ordine raccomandato: backend prima, mobile poi. Backend è propedeutico ma alcune fasi sono parallelizzabili (es. F può iniziare quando A è ready).

### Fase A — Backend foundation (Credits + Levels) ~½ giornata
- A1. `User.socialCredits` + `User.weeklyGoals` + `User.nfcStats` (campi)
- A2. Schema `CreditTransaction` + service `creditService.js` (addCredits, getHistory)
- A3. `levelService.js` con tabella 10 livelli + computeLevel(credits)
- A4. Endpoint `GET /users/me/credits` (with level)
- A5. Endpoint `GET /users/me/credits/history?page=`
- A6. Modificare `completeSession` e `createActivity` per accreditare crediti (idempotent guard)

### Fase B — Backend Quiz domain ~1 giornata
- B1. Schema `QuizCategory`, `Quiz`, `QuizAttempt`
- B2. Service `quizService.js` (listCategories con progress, listByCategory, getQuiz senza correctIndex, submitQuiz con logica idempotente)
- B3. Endpoint `GET /quiz/categories`, `GET /quiz/categories/:slug/quizzes`, `GET /quiz/:id`, `POST /quiz/:id/submit`
- B4. Joi schemas (`quizSubmissionSchema` con max 50 answers)
- B5. Seed iniziale: `backend/seed/quizzes.json` + script `node backend/seed/seedQuizzes.js`
- B6. Test Jest: submit con tutte corrette / metà / nessuna; idempotency (no double credits)

### Fase C — Backend NFC domain ~½ giornata
- C1. Schema `NfcTotem` + `NfcScan` (con 2dsphere indice)
- C2. Service `nfcService.js` (listTotems con geo filter, scanTotem con haversine check + rate limit 24h)
- C3. Endpoint `GET /nfc/totems`, `POST /nfc/scan`, `GET /users/me/nfc-history`
- C4. Joi schemas (`nfcScanSchema` con lat/lon range check)
- C5. Seed iniziale: 5-10 totem demo in `backend/seed/totems.json` (es. Rifugio Pedrotti, Cima Tosa, Bocchette Centrali)
- C6. Endpoint admin POST `/nfc/totems` per creare nuovi totem (auth admin)

### Fase D — Backend Account management ~½ giornata
- D1. Service `accountService.js` (updateUser, changePassword, deleteAccount con cascade)
- D2. Endpoint `PATCH /users/me`, `POST /auth/change-password`, `DELETE /users/me`
- D3. Endpoint `PATCH /users/me/goals` (per allineamento con piano social)
- D4. Joi `accountUpdateSchema`, `changePasswordSchema`, `goalsSchema`
- D5. Re-verify email logic se email cambia (usa `verificationToken` esistente)

### Fase E — Mobile Profilo ~1 giornata
- E1. DTO + repository CreditsRepository, AccountRepository
- E2. `ProfileViewModel` con load credits + level + nfc stats
- E3. `ProfileScreen.kt` rifatto: header, card crediti, KPI grid, NfcTotemCard (placeholder se nfc non disponibile), FormazioneCard preview, AccountEntryRow
- E4. Componenti riusabili: `CreditsCard`, `KpiGrid`, `NfcTotemCard`, `FormazioneCard`
- E5. `LevelCalculator.kt` mobile-side (per fallback offline)

### Fase F — Mobile Formazione + Quiz ~1.5 giornate
- F1. DTO + `QuizRepository` con cache Room (CachedQuizEntity)
- F2. `FormazioneViewModel` (lista categorie + progresso)
- F3. `FormazioneScreen.kt` con CategoryCard (bordo colorato, progresso, "Continua →")
- F4. `QuizViewModel` state machine (currentQuestionIdx, selectedAnswers, isAnswered, isCorrect, ...)
- F5. `QuizScreen.kt` con QuizQuestionCard + 4 QuizOptionRow + QuizFeedbackBox + "Prossima domanda"
- F6. Submit batched al backend a fine quiz (oppure per-question? — vedi Open Questions)
- F7. `QuizResultScreen.kt` con score + crediti guadagnati + "Torna a Formazione" / "Riprova"

### Fase G — Mobile NFC scan ~1 giornata
- G1. Permission NFC in manifest + check NfcAdapter at runtime
- G2. `NfcRepository` con scanTotem(tagId, gpsLon, gpsLat)
- G3. `NfcScanScreen.kt` foreground dispatch per intercept NDEF intent
- G4. Parser tag NDEF → estrazione tagId UUID
- G5. Geofence: ottieni current location via FusedLocationProvider, chiamata POST /nfc/scan
- G6. `NfcResultScreen.kt` con esiti: SUCCESS (animazione +N crediti), OUT_OF_RANGE (mappa con distanza), ALREADY_SCANNED (timer next allowed)
- G7. Entry point: tap su NfcTotemCard nel profilo → apre NfcScanScreen

### Fase H — Mobile Account edit ~½ giornata
- H1. `AccountEditScreen.kt` con form username + email (validazione client)
- H2. `ChangePasswordScreen.kt` con campo old + new + confirm
- H3. `DeleteAccountScreen.kt` zona pericolo + conferma password + countdown bottone
- H4. Integrazione esistente `WeeklyGoalsScreen` (dal piano social)
- H5. Navigation flow da AccountEntryRow del profilo

### Stima totale
**~6.5 giornate persona**. Su Sprint 2 (~3 settimane di stage residue) è fattibile se Federico e tu vi dividete:
- Backend (A+B+C+D): ~2.5gg
- Mobile profilo+formazione+quiz (E+F): ~2.5gg
- Mobile NFC+account (G+H): ~1.5gg

---

## 8. Sicurezza e privacy

- **Anti-cheating quiz**:
  - `correctIndex` mai esposto in GET /quiz/:id (solo POST submit response)
  - Submit valida tutte le answers server-side
  - QuizAttempt è append-only audit; nessun client può forgiare creditsAwarded
- **Anti-frode NFC**:
  - GPS proximity ≤50m mandatory
  - Rate limit 1 scan/utente/totem/24h
  - NfcScan immutable log per audit
- **Cascade delete account (GDPR)**:
  - Sessions: kept (collettive), participants[userId=deletedUser] → set null + label "Utente eliminato"
  - Activities: deleted
  - CreditTransactions: anonymized (userId → null, keep amounts for stats)
  - QuizAttempts: deleted (private)
  - NfcScans: anonymized (userId → null)
  - Follows: deleted in both directions
  - Comments + Likes: deleted (vedi social plan)
- **Change password**: hash bcrypt rounds=10 (come login). Niente revoke JWT esistenti per Sprint 2 (limite noto).
- **Email change**: nuova verificationToken inviata, `isVerified=false` finché click. JWT corrente continua a funzionare (auth basata su userId).

---

## 9. Open questions (da chiarire al gruppo)

1. **Quiz submit: batched o per-question?**
   Mockup mostra feedback immediato (per-question), ma per anti-cheating sarebbe meglio batched (mandi tutte le risposte a fine quiz). Compromesso: per-question UI ma il mobile tiene le risposte locali e fa UN solo POST a fine quiz, mostrando feedback simulato lato client basandosi sul correctIndex che il server svelerà solo al submit. → **Decisione: submit batched a fine quiz; il "feedback immediato" del mockup va rivisto come feedback alla fine.** Da confermare con il gruppo.

2. **Quiz lista vs accesso diretto?**
   Il mockup Formazione mostra "Continua →" sulla categoria. Dove va? Direttamente al prossimo quiz non superato (skip lista)? O passa per una `QuizListScreen` intermedia? → **Proposta: skip lista, vai al prossimo quiz non superato per quella categoria.**

3. **Totem NFC senza hardware NFC sul telefono?**
   Se l'utente ha un device senza NFC, mostriamo la card NfcTotem ma con stato "NFC NON DISPONIBILE" e link a "Come funziona?". Confermare.

4. **Quiz testo molto lungo: scroll?**
   Le domande possono superare le 500 char in casi rari. UI scroll inline o limitiamo a 500? → **Proposta: limit 500 lato schema + Joi.**

5. **Nfc totem geofence radius diverso per kind?**
   Summit (vetta esposta) può tollerare 100m, refuge (entrata edificio) 30m. Per ora default 50m globale, configurabile per totem.

---

## 10. Apertura per Sprint 3

Cose **non incluse** in Sprint 2:

- **Modalità gara quiz**: timer per domanda + leaderboard
- **Quiz feedback per-question vero**: con WebSocket o long-poll che valida lato server ogni risposta
- **CMS web per quiz**: dashboard browser-based per UniTrento (per ora seed JSON in repo)
- **HMAC NFC**: tag firmati con nonce rotation (anti-clone)
- **Achievement/badge**: medaglie speciali (es. "100 quiz superati", "Tutte le categorie completate")
- **Leaderboard crediti**: classifica globale settimanale/all-time
- **Notifiche level-up**: push quando si sale di livello
- **JWT blacklist su change-password**

---

## 11. Checklist consegna Sprint 2

- [ ] Fase A — Credits + Levels backend
- [ ] Fase B — Quiz backend + seed
- [ ] Fase C — NFC backend + seed
- [ ] Fase D — Account management backend
- [ ] Fase E — ProfileScreen mobile
- [ ] Fase F — Formazione + Quiz mobile
- [ ] Fase G — NFC scan mobile
- [ ] Fase H — Account edit mobile
- [ ] Test Jest su quiz submit + nfc scan (idempotency, anti-frode)
- [ ] Aggiornare `TSM_PROJECT_STATE.md`
- [ ] Aggiornare `SECURITY.md` (nuove route, threat model NFC)
- [ ] Aggiornare `api_reference.md` (~20 nuovi endpoint)
- [ ] Aggiornare `database_schema.md` (5+ nuove collection)
- [ ] Screenshot finali a D3
- [ ] Contenuto quiz reale fornito da UniTrento

---

_Documento aperto a revisione del gruppo prima dell'esecuzione. Apri questions in sez. 9 da risolvere con product owner._
