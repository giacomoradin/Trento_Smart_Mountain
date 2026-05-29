import mongoose from "mongoose";
import Activity from "../models/activity.js";
import HikeSession from "../models/hikeSession.js";
import Hiker from "../models/hiker.js";
import { getFollowingIds } from "./followService.js";
import { downsamplePolyline } from "../utils/geoPolyline.js";

// Risoluzione della route signature nel feed: ~48 punti bastano a riconoscere
// la forma di un percorso in una thumbnail card senza appesantire il payload.
const FEED_ROUTE_POINTS = 48;

/**
 * Servizio "social" centralizzato per share + like su attività e sessioni.
 *
 * Perché un unico service per i due tipi di "post-able":
 *   - La logica è praticamente identica (set sharedAt, push/pull like)
 *   - Differiscono solo nell'authorization (Activity → owner, Session → creator)
 *   - Centralizzare evita drift e duplicazione di test
 *
 * Convenzione errori (gestiti dal global error mapper):
 *   - NOT_FOUND          → 404
 *   - FORBIDDEN_NOT_OWNER → 403  (Activity)
 *   - FORBIDDEN_NOT_CREATOR → 403  (HikeSession)
 *   - NOT_SHARED         → 403  (tentativo di like su attività non condivisa
 *                                e di cui il viewer non è proprietario/partecipante)
 */

// ── SHARE ───────────────────────────────────────────────────────────────────

/**
 * Condivide un'Activity sul feed: set `sharedAt = now` (o aggiorna se già
 * condivisa — rilancia in stories). Autorizzazione: solo owner.
 *
 * `caption` opzionale (max 200 char, già validato lato Joi).
 */
export async function shareActivity(activityId, userId, { caption = null } = {}) {
  const activity = await Activity.findById(activityId);
  if (!activity) throw new Error("ACTIVITY_NOT_FOUND");
  if (String(activity.userId) !== String(userId)) {
    throw new Error("FORBIDDEN_NOT_OWNER");
  }
  activity.sharedAt = new Date();
  if (caption !== undefined) activity.caption = caption ?? null;
  await activity.save();
  return { sharedAt: activity.sharedAt, caption: activity.caption };
}

/** Rimuove la condivisione (sharedAt → null). Caption resta archiviata. */
export async function unshareActivity(activityId, userId) {
  const activity = await Activity.findById(activityId);
  if (!activity) throw new Error("ACTIVITY_NOT_FOUND");
  if (String(activity.userId) !== String(userId)) {
    throw new Error("FORBIDDEN_NOT_OWNER");
  }
  activity.sharedAt = null;
  await activity.save();
  return { sharedAt: null };
}

/**
 * Condivide una HikeSession: solo il creator può farlo. I partecipanti non-
 * creator vogliono condividere la *propria* uscita (Activity) — la sessione
 * appartiene al groupLeader.
 */
export async function shareSession(sessionId, userId, { caption = null } = {}) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (String(session.creatorId) !== String(userId)) {
    throw new Error("FORBIDDEN_NOT_CREATOR");
  }
  session.sharedAt = new Date();
  if (caption !== undefined) session.caption = caption ?? null;
  await session.save();
  return { sharedAt: session.sharedAt, caption: session.caption };
}

export async function unshareSession(sessionId, userId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  if (String(session.creatorId) !== String(userId)) {
    throw new Error("FORBIDDEN_NOT_CREATOR");
  }
  session.sharedAt = null;
  await session.save();
  return { sharedAt: null };
}

// ── LIKE ────────────────────────────────────────────────────────────────────

/**
 * Authorization per il like su Activity:
 *   - Se sharedAt != null → chiunque (è un post pubblico nel feed)
 *   - Altrimenti → solo l'owner può "preferire" la propria attività non
 *     condivisa (caso d'uso: ho registrato un'attività privata, voglio
 *     poterla mettere tra le preferite anche senza condividerla)
 *
 * Restituiamo `NOT_SHARED` per non rivelare l'esistenza di attività private
 * a utenti che non hanno il diritto di vederle (anti-enumeration).
 */
function assertCanInteract(doc, userId, kind) {
  if (doc.sharedAt) return; // OK: post pubblico
  const isOwner = kind === "activity"
    ? String(doc.userId) === String(userId)
    : String(doc.creatorId) === String(userId)
      || doc.participants?.some((p) => String(p.userId) === String(userId));
  if (!isOwner) throw new Error("NOT_SHARED");
}

/**
 * Aggiunge un like idempotente. Se l'utente ha già messo like, no-op e
 * ritorna la count corrente. Pattern: $addToSet sul `likes.userId` non
 * funziona per sub-document con $addToSet semplice perché Mongoose lo
 * traduce in $addToSet sull'intero oggetto (mismatch su createdAt).
 * Usiamo quindi: check esistenza + $push condizionale in update separato.
 */
export async function likeActivity(activityId, userId) {
  const activity = await Activity.findById(activityId);
  if (!activity) throw new Error("ACTIVITY_NOT_FOUND");
  assertCanInteract(activity, userId, "activity");

  const already = activity.likes.some((l) => String(l.userId) === String(userId));
  if (!already) {
    activity.likes.push({ userId, createdAt: new Date() });
    await activity.save();
  }
  return { likesCount: activity.likes.length, likedByMe: true };
}

/** Rimuove il like idempotentemente (no-op se non c'era). */
export async function unlikeActivity(activityId, userId) {
  const activity = await Activity.findById(activityId);
  if (!activity) throw new Error("ACTIVITY_NOT_FOUND");
  assertCanInteract(activity, userId, "activity");

  const before = activity.likes.length;
  activity.likes = activity.likes.filter((l) => String(l.userId) !== String(userId));
  if (activity.likes.length !== before) {
    await activity.save();
  }
  return { likesCount: activity.likes.length, likedByMe: false };
}

export async function likeSession(sessionId, userId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  assertCanInteract(session, userId, "session");

  const already = session.likes.some((l) => String(l.userId) === String(userId));
  if (!already) {
    session.likes.push({ userId, createdAt: new Date() });
    await session.save();
  }
  return { likesCount: session.likes.length, likedByMe: true };
}

export async function unlikeSession(sessionId, userId) {
  const session = await HikeSession.findById(sessionId);
  if (!session) throw new Error("SESSION_NOT_FOUND");
  assertCanInteract(session, userId, "session");

  const before = session.likes.length;
  session.likes = session.likes.filter((l) => String(l.userId) !== String(userId));
  if (session.likes.length !== before) {
    await session.save();
  }
  return { likesCount: session.likes.length, likedByMe: false };
}

// ── FEED AGGREGATOR ─────────────────────────────────────────────────────────

/**
 * Cap massimo di items recuperati da ciascuna collection prima del merge JS.
 * Tradeoff: cap troppo basso fa "perdere" post vecchi dietro a tanti post
 * recenti di una sola sorgente; cap troppo alto carica documenti inutili.
 * 200 copre tipici 1-2 mesi di feed per un utente che segue ~20 persone.
 * Per scale superiori serve una vera aggregation pipeline con $unionWith.
 */
const SERVER_MAX_PER_SOURCE = 200;

/**
 * Costruisce un [FeedItem] uniforme da Activity o HikeSession.
 *
 * Calcola `likedByMe` localmente scorrendo l'array `likes` del documento:
 * O(n) ma `n` ≤ 50 in pratica (universitario), perfettamente accettabile.
 *
 * Per le sessioni preferisce sempre `actualStats` (dati reali registrati al
 * complete), fallback su `gpxStats` (stime in pianificazione). Per le
 * attività libere `actualStats` è obbligatorio (vedi createActivitySchema).
 */
function toFeedItem(doc, kind, viewerId) {
  const likes = doc.likes || [];
  const user = kind === "activity" ? doc.userId : doc.creatorId;
  const stats = doc.actualStats || {};
  const gpxStats = kind === "session" ? doc.gpxStats || {} : null;

  // Route signature: per le attività la traccia registrata (routePolyline),
  // per le sessioni la polyline del percorso pianificato (plannedRoute).
  // Entrambe ricampionate a FEED_ROUTE_POINTS per la thumbnail. null se assente
  // (attività vecchie / sessioni senza GPX) → la card userà un hero alternativo.
  const rawRoute =
    kind === "activity"
      ? doc.routePolyline
      : doc.plannedRoute?.polylinePoints;
  const routePolyline = downsamplePolyline(rawRoute, FEED_ROUTE_POINTS) ?? null;

  return {
    kind,
    id: doc._id.toString(),
    user: user
      ? {
        _id: user._id?.toString?.() ?? String(user._id),
        username: user.username,
        personalInfo: user.personalInfo
          ? { avatarUrl: user.personalInfo.avatarUrl ?? null }
          : null,
      }
      : null,
    sharedAt: doc.sharedAt,
    caption: doc.caption ?? null,
    title: kind === "activity" ? doc.name : doc.routeDetails?.name,
    activityType: kind === "activity" ? doc.activityType ?? null : null,
    difficultyLevel:
      kind === "activity"
        ? doc.difficultyLevel ?? null
        : doc.routeDetails?.difficultyLevel ?? null,
    distanceMeters:
      stats.distanceMeters ??
      (gpxStats?.distanceKm != null ? gpxStats.distanceKm * 1000 : null),
    movingSeconds: stats.movingSeconds ?? null,
    elevationGainM:
      stats.elevationGainM ?? gpxStats?.elevationGainM ?? null,
    finalPoints: stats.finalPoints ?? gpxStats?.estimatedPoints ?? null,
    elevationProfile:
      kind === "activity"
        ? doc.elevationProfile ?? null
        : gpxStats?.elevationProfile ?? null,
    routePolyline,
    participants:
      kind === "session"
        ? (doc.participants || [])
          .map((p) => p.userId)
          .filter(Boolean)
          .map((u) => ({
            _id: u._id?.toString?.() ?? String(u._id),
            username: u.username,
            personalInfo: u.personalInfo
              ? { avatarUrl: u.personalInfo.avatarUrl ?? null }
              : null,
          }))
        : null,
    likesCount: likes.length,
    commentsCount: doc.commentsCount || 0,
    likedByMe: likes.some((l) => String(l.userId) === String(viewerId)),
  };
}

/**
 * Genera il feed sociale per `userId`.
 *
 * Strategia:
 *   1. Recupera gli ObjectId di chi `userId` segue + se stesso (gli utenti
 *      vedono i propri post nel proprio feed → conferma di pubblicazione).
 *   2. Query parallele su Activity (autore in lista) e HikeSession (creator
 *      in lista) con `sharedAt != null`, ordinate per sharedAt desc, capped
 *      a SERVER_MAX_PER_SOURCE per evitare di caricare DB intero in memoria.
 *   3. Mappa entrambe a FeedItem (vedi `toFeedItem`).
 *   4. Merge + ordina per sharedAt desc + paginazione applicata in JS.
 *
 * Restituisce `{ items, hasMore }`. La paginazione usa un lookahead di 1
 * elemento per sorgente (vedi sotto): `hasMore` è esatto anche al raggiungimento
 * del cap di sicurezza. Per scale molto grandi (>200 post visibili) la strada
 * definitiva resta una pipeline `$unionWith + $sort + $skip + $limit` con
 * cursor-based pagination (Fase 2 del piano).
 *
 * @param {string|ObjectId} userId  Utente che richiede il feed (auth via JWT).
 * @param {{page:number, limit:number}} opts  Paginazione (page 1-based, max 50 limit).
 */
export async function getFeedForUser(userId, { page = 1, limit = 20 } = {}) {
  const followingIds = await getFollowingIds(userId);
  // Include "me" così l'utente vede i propri post nel feed. ObjectId esplicito
  // per evitare Cast string->ObjectId implicito che a volte non match nei $in.
  const visibleAuthorIds = [
    ...followingIds,
    new mongoose.Types.ObjectId(String(userId)),
  ];

  // Paginazione con "lookahead": invece di caricare un cap fisso (200) e
  // calcolare hasMore sul merge — sbagliato quando si RAGGIUNGE il cap —
  // recuperiamo da OGNI sorgente solo (skip + limit + 1) documenti. Poiché ogni
  // sorgente è già ordinata sharedAt desc, questo basta a costruire la finestra
  // globale corretta, e il +1 segnala se esiste almeno un altro elemento →
  // hasMore esatto. Pagina 1/limit 20 = 21 doc/sorgente invece di 200.
  // SERVER_MAX_PER_SOURCE resta come cap di sicurezza per paginazioni profonde.
  const skip = (Math.max(1, page) - 1) * limit;
  const fetchLimit = Math.min(skip + limit + 1, SERVER_MAX_PER_SOURCE);

  const [activities, sessions] = await Promise.all([
    Activity.find({
      userId: { $in: visibleAuthorIds },
      sharedAt: { $ne: null },
    })
      .sort({ sharedAt: -1 })
      .limit(fetchLimit)
      .populate("userId", "username personalInfo.avatarUrl")
      .lean(),
    HikeSession.find({
      creatorId: { $in: visibleAuthorIds },
      sharedAt: { $ne: null },
    })
      .sort({ sharedAt: -1 })
      .limit(fetchLimit)
      // Escludi gli array di tracking live: pesanti (una posizione per
      // partecipante × heartbeat) e mai usati da toFeedItem. Riduce il
      // documento trasferito da Mongo nel caso comune di sessioni condivise.
      .select("-liveLocations -liveTracking")
      .populate("creatorId", "username personalInfo.avatarUrl")
      .populate("participants.userId", "username personalInfo.avatarUrl")
      .lean(),
  ]);

  const merged = [
    ...activities.map((a) => toFeedItem(a, "activity", userId)),
    ...sessions.map((s) => toFeedItem(s, "session", userId)),
  ].sort((a, b) => {
    const ta = a.sharedAt ? new Date(a.sharedAt).getTime() : 0;
    const tb = b.sharedAt ? new Date(b.sharedAt).getTime() : 0;
    return tb - ta;
  });

  const items = merged.slice(skip, skip + limit);
  // hasMore: il lookahead (+1 doc per sorgente) ha portato almeno un elemento
  // oltre la finestra corrente? Allora esiste un'altra pagina. Esatto anche al
  // cap, a differenza del vecchio `skip + limit < total` sul merge troncato.
  return { items, hasMore: merged.length > skip + limit };
}

// ── SOCIAL ROW (avatar in cima al feed) ─────────────────────────────────────

const STORY_WINDOW_MS = 24 * 3600 * 1000; // 24 ore — vedi sprint2_social.md §1

/**
 * Calcola il `weeklyProgressPct` di un utente come media delle tre metriche
 * (km, dislivello, conteggio) normalizzate sui rispettivi goal. Skip metric
 * con goal === 0 (l'utente non l'ha impostata). Se nessun goal è impostato,
 * ritorna 0 (la UI mostrerà un anello vuoto neutro).
 *
 * NB: usiamo i dati grezzi delle Activity + HikeSession completed degli
 * ultimi 7 giorni rolling (week ISO è troppo stretta per il rolling window).
 */
function computeProgressPct(goals, totals) {
  if (!goals) return 0;
  const components = [];
  if (goals.km > 0 && totals.km != null) components.push(Math.min(totals.km / goals.km, 1));
  if (goals.elevM > 0 && totals.elevM != null) components.push(Math.min(totals.elevM / goals.elevM, 1));
  if (goals.count > 0 && totals.count != null) components.push(Math.min(totals.count / goals.count, 1));
  if (components.length === 0) return 0;
  return components.reduce((a, b) => a + b, 0) / components.length;
}

/**
 * Restituisce la "Avatar Row" mostrata in cima alla HomeSocialScreen.
 *
 * Per ogni utente seguito da `viewerId`, calcola uno **status** in priorità
 * decrescente (vedi docs/sprint2_social.md §1):
 *
 *   1. "live"    — esiste HikeSession con `status: "ACTIVE"` di cui l'utente
 *                  è creator o partecipante
 *   2. "story"   — esiste Activity o HikeSession con `sharedAt` negli ultimi
 *                  24h (il client filtra ulteriormente per "viewed" via Room
 *                  locale, ma il server espone l'esistenza)
 *   3. "goal"    — nessuno stato sopra → ritorna `weeklyProgressPct` ∈ [0,1]
 *                  derivato da `Hiker.weeklyGoals` + Activity/HikeSession
 *                  completed last 7 days
 *   4. "neutral" — nessun dato (utente senza goal e senza attività recenti)
 *
 * Risposta:
 *   { items: SocialRowItem[] }
 *
 * SocialRowItem = {
 *   user: { _id, username, avatarColor?, personalInfo?.avatarUrl },
 *   status: "live" | "story" | "goal" | "neutral",
 *   liveSessionId?: string,             // solo per status === "live"
 *   storyActivityRef?: { id, kind, sharedAt },  // solo per status === "story"
 *   weeklyProgressPct?: number,          // 0..1, solo per status === "goal"
 * }
 *
 * Implementazione: 5 query parallele Promise.all → merge per userId → priority assignment.
 */
export async function getSocialRowForUser(viewerId) {
  const followingIds = await getFollowingIds(viewerId);
  if (followingIds.length === 0) return { items: [] };

  const since24h = new Date(Date.now() - STORY_WINDOW_MS);
  const since7d = new Date(Date.now() - 7 * STORY_WINDOW_MS);

  // 5 query parallele per minimizzare la latenza totale.
  const [
    hikers,            // anagrafica + weeklyGoals
    liveSessions,      // ACTIVE sessions
    storyActivities,   // shared in last 24h
    storySessions,
    weekActivities,    // completed in last 7 days (per progress)
    weekSessions,
  ] = await Promise.all([
    Hiker.find({ _id: { $in: followingIds } })
      .select("username personalInfo.avatarUrl weeklyGoals")
      .lean(),
    HikeSession.find({
      status: "ACTIVE",
      $or: [
        { creatorId: { $in: followingIds } },
        { "participants.userId": { $in: followingIds } },
      ],
    })
      .select("_id creatorId participants")
      .lean(),
    Activity.find({
      userId: { $in: followingIds },
      sharedAt: { $gte: since24h },
    })
      .select("_id userId sharedAt")
      .sort({ sharedAt: -1 })
      .lean(),
    HikeSession.find({
      creatorId: { $in: followingIds },
      sharedAt: { $gte: since24h },
    })
      .select("_id creatorId sharedAt")
      .sort({ sharedAt: -1 })
      .lean(),
    Activity.find({
      userId: { $in: followingIds },
      completedAt: { $gte: since7d },
    })
      .select("userId actualStats")
      .lean(),
    HikeSession.find({
      status: "COMPLETED",
      $or: [
        { creatorId: { $in: followingIds } },
        { "participants.userId": { $in: followingIds } },
      ],
      endTime: { $gte: since7d },
    })
      .select("creatorId participants actualStats gpxStats")
      .lean(),
  ]);

  // ── 1. Mappa live sessions per userId (creator O partecipante) ──
  // Un utente può essere "live" come creator del proprio gruppo O come
  // partecipante di un altrui — entrambi contano. Memorizziamo sessionId
  // per il deep-link.
  const liveByUser = new Map();
  for (const s of liveSessions) {
    const ids = new Set([String(s.creatorId)]);
    for (const p of (s.participants || [])) ids.add(String(p.userId));
    for (const uid of ids) {
      if (followingIds.some((f) => String(f) === uid) && !liveByUser.has(uid)) {
        liveByUser.set(uid, String(s._id));
      }
    }
  }

  // ── 2. Mappa story refs per userId (più recente vince) ──
  // Iteriamo le activities + sessions già ordinate sharedAt desc così la
  // prima vista per ciascun userId è la più recente (anti N+1 per Date max).
  const storyByUser = new Map();
  for (const a of storyActivities) {
    const uid = String(a.userId);
    if (!storyByUser.has(uid)) {
      storyByUser.set(uid, {
        id: String(a._id),
        kind: "activity",
        sharedAt: a.sharedAt,
      });
    }
  }
  for (const s of storySessions) {
    const uid = String(s.creatorId);
    if (!storyByUser.has(uid)) {
      storyByUser.set(uid, {
        id: String(s._id),
        kind: "session",
        sharedAt: s.sharedAt,
      });
    }
  }

  // ── 3. Aggrega totals per progress weekly per ogni userId ──
  const totalsByUser = new Map();
  function bump(uid, km, elev, count) {
    const cur = totalsByUser.get(uid) ?? { km: 0, elevM: 0, count: 0 };
    cur.km += km || 0;
    cur.elevM += elev || 0;
    cur.count += count || 0;
    totalsByUser.set(uid, cur);
  }
  for (const a of weekActivities) {
    const km = (a.actualStats?.distanceMeters ?? 0) / 1000.0;
    const elev = a.actualStats?.elevationGainM ?? 0;
    bump(String(a.userId), km, elev, 1);
  }
  for (const s of weekSessions) {
    const km =
      (s.actualStats?.distanceMeters ?? 0) / 1000.0 ||
      (s.gpxStats?.distanceKm ?? 0);
    const elev = s.actualStats?.elevationGainM ?? s.gpxStats?.elevationGainM ?? 0;
    // Aggrega per il creator E per i partecipanti (chi partecipa "fa" l'escursione).
    const userIds = new Set([String(s.creatorId)]);
    for (const p of (s.participants || [])) userIds.add(String(p.userId));
    for (const uid of userIds) {
      if (followingIds.some((f) => String(f) === uid)) {
        bump(uid, km, elev, 1);
      }
    }
  }

  // ── 4. Costruisci items con priority assignment ──
  const items = hikers.map((h) => {
    const uid = String(h._id);
    const userPayload = {
      _id: uid,
      username: h.username,
      personalInfo: h.personalInfo
        ? { avatarUrl: h.personalInfo.avatarUrl ?? null }
        : null,
    };
    if (liveByUser.has(uid)) {
      return {
        user: userPayload,
        status: "live",
        liveSessionId: liveByUser.get(uid),
      };
    }
    if (storyByUser.has(uid)) {
      return {
        user: userPayload,
        status: "story",
        storyActivityRef: storyByUser.get(uid),
      };
    }
    const goals = h.weeklyGoals;
    const totals = totalsByUser.get(uid) ?? { km: 0, elevM: 0, count: 0 };
    const pct = computeProgressPct(goals, totals);
    if (pct > 0) {
      return {
        user: userPayload,
        status: "goal",
        weeklyProgressPct: pct,
      };
    }
    return { user: userPayload, status: "neutral" };
  });

  // Ordina: live first, story second, goal by pct desc, neutral last.
  // L'UI ha così l'ordine naturale "più interessante prima" nella row.
  const priority = { live: 0, story: 1, goal: 2, neutral: 3 };
  items.sort((a, b) => {
    const pa = priority[a.status];
    const pb = priority[b.status];
    if (pa !== pb) return pa - pb;
    if (a.status === "goal") return (b.weeklyProgressPct || 0) - (a.weeklyProgressPct || 0);
    return 0;
  });

  return { items };
}

/**
 * Restituisce i post (Activity + HikeSession condivise) di un singolo autore.
 * Usato dalla `UserProfileScreen` per mostrare la "bacheca" di un utente.
 *
 * Differenze vs `getFeedForUser`:
 *   - Filtro su un solo `authorId` invece che array di seguiti
 *   - Authorization: se il viewer è SE STESSO può vedere anche i post NON
 *     condivisi (privacy gate "diary": utente vede tutta la sua history);
 *     altrimenti solo `sharedAt != null` (anti-enumeration)
 *   - `likedByMe` calcolato dal POV del viewer (non dell'autore)
 *
 * @param {string|ObjectId} authorId  autore dei post
 * @param {string|ObjectId} viewerId  utente che richiede (per likedByMe + privacy)
 */
export async function getPostsByUser(authorId, viewerId, { page = 1, limit = 20 } = {}) {
  const isSelf = String(authorId) === String(viewerId);
  // Se viewer != autore, mostriamo solo i post condivisi (sharedAt != null).
  // Se viewer == autore, mostriamo anche i privati così l'utente può
  // "vedere la propria timeline completa" e decidere cosa pubblicare.
  const sharedFilter = isSelf ? {} : { sharedAt: { $ne: null } };

  // Stessa paginazione a lookahead di getFeedForUser: skip+limit+1 doc per
  // sorgente → hasMore esatto e payload ridotto rispetto al cap fisso.
  const skip = (Math.max(1, page) - 1) * limit;
  const fetchLimit = Math.min(skip + limit + 1, SERVER_MAX_PER_SOURCE);

  const [activities, sessions] = await Promise.all([
    Activity.find({ userId: authorId, ...sharedFilter })
      .sort({ sharedAt: -1, completedAt: -1 })
      .limit(fetchLimit)
      .populate("userId", "username personalInfo.avatarUrl")
      .lean(),
    HikeSession.find({ creatorId: authorId, ...sharedFilter })
      .sort({ sharedAt: -1, createdAt: -1 })
      .limit(fetchLimit)
      .select("-liveLocations -liveTracking")
      .populate("creatorId", "username personalInfo.avatarUrl")
      .populate("participants.userId", "username personalInfo.avatarUrl")
      .lean(),
  ]);

  const merged = [
    ...activities.map((a) => toFeedItem(a, "activity", viewerId)),
    ...sessions.map((s) => toFeedItem(s, "session", viewerId)),
  ].sort((a, b) => {
    // Ordina principalmente per sharedAt desc; in fallback (post NON
    // condivisi visibili solo a self) usa createdAt/completedAt impliciti
    // tramite l'ordine già dato dalla query Mongo.
    const ta = a.sharedAt ? new Date(a.sharedAt).getTime() : 0;
    const tb = b.sharedAt ? new Date(b.sharedAt).getTime() : 0;
    return tb - ta;
  });

  const items = merged.slice(skip, skip + limit);
  return { items, hasMore: merged.length > skip + limit };
}
