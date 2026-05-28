import mongoose from "mongoose";
import Activity from "../models/activity.js";
import HikeSession from "../models/hikeSession.js";
import { getFollowingIds } from "./followService.js";

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
 * Restituisce `{ items, hasMore }` con `hasMore` true se ci sono items
 * oltre la finestra page+limit attualmente nel set merged. NB: hasMore può
 * sbagliare per eccesso se le due sorgenti hanno entrambe raggiunto il cap
 * SERVER_MAX_PER_SOURCE e ci sono post più vecchi che non abbiamo caricato.
 * Pragmaticamente OK per la fase v1 — Sprint 3 può migrare a aggregation
 * `$unionWith + $sort + $skip + $limit` pipeline per cursor-based pagination.
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

  const [activities, sessions] = await Promise.all([
    Activity.find({
      userId: { $in: visibleAuthorIds },
      sharedAt: { $ne: null },
    })
      .sort({ sharedAt: -1 })
      .limit(SERVER_MAX_PER_SOURCE)
      .populate("userId", "username personalInfo.avatarUrl")
      .lean(),
    HikeSession.find({
      creatorId: { $in: visibleAuthorIds },
      sharedAt: { $ne: null },
    })
      .sort({ sharedAt: -1 })
      .limit(SERVER_MAX_PER_SOURCE)
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

  const total = merged.length;
  const skip = (Math.max(1, page) - 1) * limit;
  const items = merged.slice(skip, skip + limit);
  return { items, hasMore: skip + limit < total };
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

  const [activities, sessions] = await Promise.all([
    Activity.find({ userId: authorId, ...sharedFilter })
      .sort({ sharedAt: -1, completedAt: -1 })
      .limit(SERVER_MAX_PER_SOURCE)
      .populate("userId", "username personalInfo.avatarUrl")
      .lean(),
    HikeSession.find({ creatorId: authorId, ...sharedFilter })
      .sort({ sharedAt: -1, createdAt: -1 })
      .limit(SERVER_MAX_PER_SOURCE)
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

  const total = merged.length;
  const skip = (Math.max(1, page) - 1) * limit;
  const items = merged.slice(skip, skip + limit);
  return { items, hasMore: skip + limit < total };
}
