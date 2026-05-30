# Sprint 2 — Schermata Social (Home → tab SOCIAL)

> Piano operativo per implementare il feed sociale tipo Strava in Trento Smart Mountain.
> Riferimento mockup: `Home > tab SOCIAL` (feed verticale + row avatar con anello stato).
> Documento generato il 2026-05-24 come input alla D3 e al planning di Sprint 2.

---

## 1. Decisioni di prodotto (confermate)

| Decisione | Valore scelto |
|---|---|
| Visibilità attività | **Privato di default; pubblico solo se l'utente preme "Condividi"** |
| Modello relazioni | **Follow asimmetrico** (Strava-like). Utente A può seguire B senza che B segua A. |
| Engagement | **Like + commenti completi** (no reactions multiple) |
| Avatar row | **Merge live + story 24h + obiettivo settimanale**, anello con priorità |
| Bottone `+ Condividi` | **Condividi attività esistente sul feed** (set `sharedAt = now` su Activity/HikeSession) |
| Obiettivo settimanale | **Multi-metrica**: km + dislivello + numero uscite |
| Scope v1 | **Tutto incluso** — nessun taglio rispetto alla proposta |

### Stato "anello avatar" — logica di priorità

Per ogni utente seguito, l'anello sull'avatar nella row in alto è determinato dal primo stato true (priorità decrescente):

| # | Stato | Colore | Trigger |
|---|---|---|---|
| 1 | `liveActiveSession` | 🟡 giallo (animato) | esiste una `HikeSession.status == "ACTIVE"` con quel `userId` in `participants` |
| 2 | `storyUnseen` | 🔵 azzurro pieno | esiste un'attività (`Activity` o `HikeSession`) con `sharedAt > now - 24h` E non ancora vista localmente da chi guarda |
| 3 | `weeklyGoalProgress` | 🟢 verde gauge parziale | nessuno stato sopra → mostro arco proporzionale alla `weeklyProgressPct` |
| 4 | `neutral` | grigio | nessun stato (utente "Luca" del mockup) |

`weeklyProgressPct = average(min(km/goalKm, 1), min(elevM/goalElevM, 1), min(count/goalCount, 1))` sui 7 giorni rolling.

Tap sull'avatar:
- Se ha story attiva → apre `StoryViewerScreen` con dettaglio attività
- Altrimenti → apre `UserProfileScreen` con bottone Segui/Smetti di seguire

---

## 2. Modello dati

### 2.1 Backend (Mongoose schemas)

```js
// backend/src/models/follow.js (nuovo)
const followSchema = new Schema({
  followerId:  { type: ObjectId, ref: "User", required: true, index: true },
  followingId: { type: ObjectId, ref: "User", required: true, index: true },
  createdAt:   { type: Date, default: Date.now },
});
followSchema.index({ followerId: 1, followingId: 1 }, { unique: true });
// Anti-self-follow validation lato service.

// backend/src/models/comment.js (nuovo)
const commentSchema = new Schema({
  activityRefId: { type: ObjectId, required: true, index: true },
  kind:          { type: String, enum: ["activity", "session"], required: true },
  userId:        { type: ObjectId, ref: "User", required: true },
  text:          { type: String, required: true, maxlength: 500 },
  createdAt:     { type: Date, default: Date.now },
});
commentSchema.index({ activityRefId: 1, kind: 1, createdAt: -1 });

// backend/src/models/user.js (modifiche)
// Aggiungere campo:
weeklyGoals: {
  km:     { type: Number, default: 0, min: 0, max: 500 },
  elevM:  { type: Number, default: 0, min: 0, max: 20000 },
  count:  { type: Number, default: 0, min: 0, max: 50 },
},

// backend/src/models/activity.js (modifiche) — attività libere
sharedAt:  { type: Date, default: null, index: true },
likes:     [{ userId: { type: ObjectId, ref: "User" }, createdAt: { type: Date, default: Date.now } }],
// likes come sub-document per evitare extra-query nella card feed.
// commentsCount: { type: Number, default: 0 } — denormalizzato per evitare $lookup nel feed.

// backend/src/models/hikeSession.js (modifiche)
sharedAt:  { type: Date, default: null, index: true },
likes:     [{ userId: ..., createdAt: ... }],
// commentsCount idem.
```

### 2.2 Mobile (Room)

```kotlin
// data/local/db/ViewedStoryEntity.kt (nuovo)
// Locale-only: traccia quali storie l'utente corrente ha già visualizzato.
// Niente sync remoto: il backend non saprà mai cosa ho visto/non visto.
@Entity(tableName = "viewed_stories")
data class ViewedStoryEntity(
    @PrimaryKey val activityRefId: String,
    @ColumnInfo(name = "kind") val kind: String,            // "activity" | "session"
    @ColumnInfo(name = "viewed_at_ms") val viewedAtMs: Long,
)

@Dao
interface ViewedStoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markViewed(entity: ViewedStoryEntity)

    @Query("SELECT activity_ref_id FROM viewed_stories WHERE viewed_at_ms > :sinceMs")
    suspend fun getViewedSince(sinceMs: Long): List<String>
}
```

`TsmDatabase` migration v5 → aggiunge tabella `viewed_stories`.

---

## 3. Endpoint backend

Tutti i path con `/api/v1/` salvo dove indicato. Tutti richiedono `authenticate` middleware + `authenticatedLimiter`. Body validati con Joi.

### 3.1 Follow

| Method | Path | Body | Risposta | Errori |
|---|---|---|---|---|
| `POST` | `/users/:id/follow` | — | `201 { message }` | 400 SELF_FOLLOW, 404 USER_NOT_FOUND, 409 ALREADY_FOLLOWING |
| `DELETE` | `/users/:id/follow` | — | `200 { message }` | 404 NOT_FOLLOWING |
| `GET` | `/users/me/following` | query: `page`, `limit` | `200 { count, items: [{ user, since }] }` | — |
| `GET` | `/users/me/followers` | query: `page`, `limit` | `200 { count, items: [{ user, since }] }` | — |
| `GET` | `/users/:id/follow-stats` | — | `200 { followers, following, isFollowedByMe }` | 404 |

### 3.2 Goals

| Method | Path | Body | Risposta |
|---|---|---|---|
| `PATCH` | `/users/me/goals` | `{ km?, elevM?, count? }` | `200 { weeklyGoals }` |
| `GET` | `/users/me/goals` | — | `200 { weeklyGoals, currentProgress: { km, elevM, count } }` |

`currentProgress` calcolato server-side aggregando Activity+HikeSession completed nell'ultima settimana rolling.

### 3.3 Share

| Method | Path | Body | Risposta |
|---|---|---|---|
| `POST` | `/activities/:id/share` | `{ caption? }` (max 200 char, opzionale) | `200 { sharedAt }` |
| `POST` | `/sessions/:id/share` | `{ caption? }` | `200 { sharedAt }` |
| `DELETE` | `/activities/:id/share` | — | `200 { message }` (sharedAt → null) |
| `DELETE` | `/sessions/:id/share` | — | `200 { message }` |

Idempotente: share su attività già condivisa aggiorna `sharedAt` (rilancia in stories).

### 3.4 Like

| Method | Path | Body | Risposta |
|---|---|---|---|
| `POST` | `/activities/:id/like` | — | `200 { likesCount }` |
| `DELETE` | `/activities/:id/like` | — | `200 { likesCount }` |
| `POST` | `/sessions/:id/like` | — | `200 { likesCount }` |
| `DELETE` | `/sessions/:id/like` | — | `200 { likesCount }` |

Tutti idempotenti (already-liked → no-op). Authorization: solo se l'attività è già `sharedAt != null` o se l'utente è partecipante/owner.

### 3.5 Commenti

| Method | Path | Body | Risposta |
|---|---|---|---|
| `POST` | `/activities/:id/comments` | `{ text }` | `201 { comment }` |
| `GET` | `/activities/:id/comments?page=&limit=` | — | `200 { count, items }` |
| `DELETE` | `/activities/:id/comments/:cid` | — | `200 { message }` (solo author o admin) |
| `POST` | `/sessions/:id/comments` | `{ text }` | analogo |
| `GET` | `/sessions/:id/comments` | — | analogo |
| `DELETE` | `/sessions/:id/comments/:cid` | — | analogo |

### 3.6 Feed e Social Row

| Method | Path | Query | Risposta |
|---|---|---|---|
| `GET` | `/users/me/feed` | `page`, `limit` (max 20) | `200 { items: [FeedItem], hasMore }` |
| `GET` | `/users/me/social-row` | — | `200 { items: [SocialRowItem] }` |

```ts
// FeedItem
{
  kind: "activity" | "session",
  id: string,
  user: { _id, username, avatarColor? },
  sharedAt: ISO,
  caption?: string,
  title: string,                    // session.routeDetails.name OR activity.name
  location?: string,                // se disponibile (es. città vicina via Weather POI)
  distanceMeters: number,
  movingSeconds: number,
  elevationGainM: number,
  finalPoints?: number,
  elevationProfile?: number[],      // max 50 punti per la mini-card chart
  participants?: [{ _id, username }],  // solo per session
  likesCount: number,
  commentsCount: number,
  likedByMe: boolean,
}

// SocialRowItem
{
  user: { _id, username, avatarColor? },
  status: "live" | "story" | "goal" | "neutral",
  liveSessionId?: string,           // solo se status === "live"
  storyActivityRef?: { id, kind, sharedAt },  // solo se status === "story"
  weeklyProgressPct?: number,       // 0.0-1.0, solo se status === "goal"
}
```

Server-side query feed (pseudocodice):
```js
const following = await Follow.find({ followerId: me }).distinct("followingId");
const activities = await Activity.find({
  userId: { $in: following },
  sharedAt: { $ne: null },
}).sort({ sharedAt: -1 }).skip(page * limit).limit(limit);
const sessions = await HikeSession.find({
  "participants.userId": { $in: following },
  sharedAt: { $ne: null },
}).sort({ sharedAt: -1 }).skip(...).limit(...);
// Merge, ordina per sharedAt desc, populate user, calcola likedByMe via $in check
```

---

## 4. Mobile architecture

### 4.1 Nuovi file Kotlin

```
mobile/app/src/main/java/it/trentosmartmountain/app/
├─ data/
│  ├─ local/db/
│  │  ├─ ViewedStoryEntity.kt        # nuovo
│  │  └─ ViewedStoryDao.kt           # nuovo
│  └─ remote/
│     ├─ dto/
│     │  ├─ FollowRequest.kt         # nuovo
│     │  ├─ FeedResponse.kt          # nuovo: FeedItem + paginazione
│     │  ├─ SocialRowResponse.kt     # nuovo
│     │  ├─ ShareRequest.kt          # nuovo
│     │  ├─ LikeResponse.kt          # nuovo
│     │  ├─ CommentRequest.kt        # nuovo
│     │  ├─ CommentResponse.kt       # nuovo
│     │  └─ WeeklyGoalsRequest.kt    # nuovo
│     └─ TsmApiService.kt            # estesa con nuovi endpoint
├─ repository/
│  ├─ SocialRepository.kt            # nuovo
│  └─ SocialRepositoryImpl.kt        # nuovo
├─ viewmodel/
│  ├─ SocialFeedViewModel.kt         # nuovo
│  ├─ UserProfileViewModel.kt        # nuovo (per tap avatar)
│  └─ WeeklyGoalsViewModel.kt        # nuovo
└─ ui/screens/home/
   ├─ HomeSocialScreen.kt            # nuovo (cabla in HomeScreen)
   ├─ FeedCard.kt                    # nuovo componente
   ├─ AvatarRow.kt                   # nuovo componente con anello stato
   ├─ ShareActivityDialog.kt         # nuovo dialog "+Condividi"
   ├─ CommentsBottomSheet.kt         # nuovo
   ├─ StoryViewerScreen.kt           # nuovo (full-screen story tap)
   └─ UserProfileScreen.kt           # nuovo (tap avatar non-story)

mobile/app/src/main/java/it/trentosmartmountain/app/ui/screens/profile/
└─ EditWeeklyGoalsScreen.kt          # nuovo
```

### 4.2 Modifiche file esistenti

| File | Cosa cambia |
|---|---|
| `HomeScreen.kt` | Sotto-tab Social ora monta `HomeSocialScreen` (no placeholder) |
| `TsmDatabase.kt` | Bump version 4 → 5 + add ViewedStoryDao |
| `Routes.kt` | Aggiungere `USER_PROFILE`, `STORY_VIEWER`, `EDIT_GOALS`, `COMMENTS` |
| `TsmNavHost.kt` | Compose routes per gli screen sopra |
| `ActivityDetailScreen.kt` | Sezione like (toggle + counter) + lista commenti inline |
| `ProfileScreen.kt` | Card "Obiettivi settimanali" tappabile → EditWeeklyGoalsScreen |
| `TsmApiService.kt` | ~15 nuovi endpoint Retrofit |

---

## 5. Piano fasi (5 fasi, ~3.5 giornate persona)

Ogni fase è auto-contenuta e deployabile (test in isolation). Ordine raccomandato:

### Fase A — Backend foundation
- A1. Schema `Follow` + indice unique compound
- A2. `User.weeklyGoals` (km, elevM, count) con validazione min/max
- A3. Campo `sharedAt` su `Activity` e `HikeSession` + indice
- A4. Service `followService.js` (followUser, unfollowUser, getFollowing, getFollowers)
- A5. Route `/users/:id/follow`, `/users/me/following`, `/users/me/followers`, `/users/:id/follow-stats`
- A6. Route `/users/me/goals` (GET con currentProgress, PATCH)
- A7. Joi schemas per `followIdParam`, `weeklyGoalsSchema`

### Fase B — Backend social actions
- B1. Service `shareActivity`/`shareSession` (set sharedAt)
- B2. Route `/activities/:id/share`, `/sessions/:id/share` (POST/DELETE)
- B3. Likes sub-document + service `likeActivity`/`unlikeActivity` idempotenti
- B4. Route like POST/DELETE per Activity e Session
- B5. Schema `Comment` + service `addComment`, `getComments` (paginazione), `deleteComment`
- B6. Route commenti per Activity e Session
- B7. Joi schemas (`shareSchema`, `commentSchema` con maxlength 500)
- B8. Authorization: like/comment richiede activity.sharedAt OR partecipante

### Fase C — Backend feed aggregator
- C1. Service `getFeedForUser(userId, page, limit)` — merge Activity+Session di seguiti con sharedAt
- C2. Populate ottimizzato (user info, participants per session)
- C3. Calcolo `likedByMe` con $in check su likes array
- C4. Route `GET /users/me/feed`
- C5. Service `getSocialRowForUser` — priority anello (live > story > goal > neutral)
- C6. Route `GET /users/me/social-row`
- C7. Test integration con Jest (almeno feed + social-row con dataset mockato)

### Fase D — Mobile feed core
- D1. DTO FeedItem, SocialRowItem, FollowRequest, ShareRequest
- D2. TsmApiService nuovi endpoint
- D3. SocialRepository / Impl (con cache Room opzionale)
- D4. ViewedStoryDao + Room migration v5
- D5. `SocialFeedViewModel` con paginazione (LazyColumn + LaunchedEffect bottom-reached)
- D6. `HomeSocialScreen.kt` shell con LazyColumn + pull-to-refresh
- D7. `FeedCard.kt` componente fedele al mockup (avatar, titolo, mini-chart, KPI, partecipanti, like, commenti)
- D8. `AvatarRow.kt` con logica anello priorità (live > story > goal > neutral)
- D9. Cabla in `HomeScreen` (rimuove placeholder Social)

### Fase E — Mobile interactions
- E1. Like toggle ottimistico + counter live (POST/DELETE)
- E2. Animazione cuore al tap like (scale + alpha tween)
- E3. `CommentsBottomSheet` con lista paginata + input + cancella propri
- E4. `ShareActivityDialog` — selettore attività + caption opzionale + POST share
- E5. `EditWeeklyGoalsScreen` da Profilo (3 slider per km/elev/count)
- E6. Card "Obiettivi" in `ProfileScreen` con progresso corrente
- E7. `UserProfileScreen` con bottone Segui/Smetti di seguire + lista attività condivise
- E8. `StoryViewerScreen` con timer 5s + bottone visualizza-attività intera
- E9. Markl story come "viewed" in ViewedStoryDao all'apertura
- E10. Logica `AvatarRow` per chiamare `markViewed` e ri-calcolare anello

---

## 6. Sicurezza e privacy

- **Rate limit**: share/like/comment vanno sotto `writeLimiter` (max 30/min per utente)
- **Authorization granulare**:
  - Comment delete: solo autore o admin
  - Activity share: solo owner (`activity.userId == req.user.userId`)
  - Session share: solo creator (`session.creatorId == req.user.userId`)
  - Like: solo se activity/session è già condivisa (impedisce di "scoprire" attività private)
- **Sanitization**: comment.text passa via `express-mongo-sanitize` (già attivo) + escape lato client
- **GDPR**: l'utente può cancellare le proprie attività; cascade delete dei like/comment correlati (via service)
- **Anti-spam**: limit comment 500 char, max 50/utente/giorno per attività

---

## 7. Considerazioni di performance

- **Feed query**: `following` array può crescere → indici Mongo su `Activity.{userId, sharedAt}` e `HikeSession.{participants.userId, sharedAt}`. Limit 20 items/page.
- **Avatar row**: prefetch on `HomeScreen` mount, refresh ogni 30s mentre tab attiva (anello live deve essere "fresco")
- **Comments count denormalizzato**: ogni POST/DELETE comment fa `$inc: commentsCount`. Evita aggregation per ogni feed item.
- **Cache feed offline**: SocialFeedViewModel salva ultima pagina in `feed_cache` Room → app offline mostra ultimo snapshot

---

## 8. Apertura per Sprint 3

Cose **non incluse** in Sprint 2 (esplicitamente):

- Notifiche push (FCM) per like/comment ricevuti
- Reactions multiple (oltre il cuore)
- Condivisione esterna (Android Share Sheet verso WhatsApp/Instagram)
- Storie con foto/video allegate (per ora solo "highlight" di un'attività esistente)
- Blocco utenti / segnalazione abuso
- Hashtag e mention
- Refresh token (vedi sezione 9)

## 9. Note collegate fuori scope

- **JWT 7d → refresh token (Sprint 3)**: con feed attivo l'utente userà l'app più spesso → expiry 7d basta. Però se vuole "post & forget" su una uscita di una settimana fa, il token può essere scaduto. Sprint 3 valuta refresh token rotation (access 15min + refresh 30d).
- **WorkManager (Sprint 3)**: il sync attuale richiede processo vivo. Con il feed che richiede aggiornamenti frequenti, conviene WorkManager con PeriodicWorkRequest.

---

## 10. Checklist consegna Sprint 2 (incluse feature precedenti)

- [ ] Fase A backend foundation
- [ ] Fase B backend social actions
- [ ] Fase C backend feed aggregator
- [ ] Fase D mobile feed core
- [ ] Fase E mobile interactions
- [ ] Test Jest schemi Joi su nuove route
- [ ] Aggiornare `TSM_PROJECT_STATE.md` con stato attuale
- [ ] Aggiornare `SECURITY.md` con nuove route + ACM rows
- [ ] Aggiornare `api_reference.md` con nuovi endpoint
- [ ] Aggiungere screenshot finali a D3

---

_Documento aperto a revisione del gruppo prima dell'esecuzione. Vincoli e priorità da confermare con product owner._
