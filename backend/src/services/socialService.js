import mongoose from "mongoose";
import Activity from "../models/activity.js";
import HikeSession from "../models/hikeSession.js";
import Hiker from "../models/hiker.js";
import Story from "../models/story.js";
import { getFollowingIds, isFollowing } from "./followService.js";
import { createNotification } from "./notificationService.js";
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
    // Notifica l'autore (best-effort, no-op se mette like a sé stesso).
    await createNotification({
      recipientId: activity.userId,
      actorId: userId,
      type: "like",
      targetKind: "activity",
      targetId: activity._id,
    });
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
    await createNotification({
      recipientId: session.creatorId,
      actorId: userId,
      type: "like",
      targetKind: "session",
      targetId: session._id,
    });
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

// ── VISIBILITÀ PROFILO ──────────────────────────────────────────────────────

/**
 * Modello di visibilità a livello di account (`preferences.privacy.profileVisibility`):
 *
 *   - "public"  → i post condivisi sono visibili a chiunque
 *   - "friends" → visibili solo ai propri follower (modello "amici = follower")
 *   - "private" → visibili solo a se stessi
 *
 * Restituisce una Map<string, "public"|"friends"|"private"> per gli autori dati.
 * Default "friends" (coerente con lo schema Hiker) per autori senza preferenza.
 */
async function getVisibilityForAuthors(authorIds) {
  const map = new Map();
  if (!authorIds || authorIds.length === 0) return map;
  const docs = await Hiker.find({ _id: { $in: authorIds } })
    .select("preferences.privacy.profileVisibility")
    .lean();
  for (const d of docs) {
    map.set(
      String(d._id),
      d.preferences?.privacy?.profileVisibility ?? "friends",
    );
  }
  return map;
}

// ── DISCOVERY / RICERCA UTENTI ──────────────────────────────────────────────

/**
 * Ricerca escursionisti per username (case-insensitive, match parziale).
 *
 * Usata dalla schermata "Cerca persone da seguire" del mobile: alimenta il
 * flusso "aggiungi amici" del social (follow asimmetrico).
 *
 *  - Esclude il viewer stesso dai risultati.
 *  - Restituisce solo campi pubblici (`username`, `personalInfo.avatarUrl`),
 *    coerente con userPrivacy.js.
 *  - `isFollowedByMe` per ogni risultato → la UI mostra subito "Segui"/"Seguito"
 *    senza una seconda query per riga.
 *  - Ordina i non-ancora-seguiti per primi (più utili da scoprire), poi A→Z.
 *
 * Sicurezza: i metacaratteri regex nel termine vengono escapati per prevenire
 * regex injection / ReDoS. Termine < 2 caratteri → nessun risultato (evita
 * di restituire l'intero DB su query vuote).
 *
 * @param {string|ObjectId} viewerId  utente che cerca (per escludere sé + isFollowedByMe)
 * @param {string} q                  termine di ricerca (username, parziale)
 * @param {{limit?:number}} opts      cap risultati (default 20, max 50)
 */
export async function searchUsers(viewerId, q, { limit = 20 } = {}) {
  const term = (q ?? "").trim();
  if (term.length < 2) return { items: [] };

  // Escape dei metacaratteri regex: il termine arriva dall'utente, non deve
  // poter alterare il pattern (ReDoS / match imprevisti).
  const escaped = term.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const regex = new RegExp(escaped, "i");
  const safeLimit = Math.min(Math.max(1, limit), 50);

  const docs = await Hiker.find({
    username: regex,
    _id: { $ne: viewerId },
  })
    .select("username personalInfo.avatarUrl")
    .limit(safeLimit)
    .lean();

  // Una sola query per sapere quali risultati il viewer segue già.
  const followingIds = new Set(
    (await getFollowingIds(viewerId)).map((id) => String(id)),
  );

  const items = docs.map((d) => ({
    user: {
      _id: String(d._id),
      username: d.username,
      personalInfo: d.personalInfo?.avatarUrl
        ? { avatarUrl: d.personalInfo.avatarUrl }
        : null,
    },
    isFollowedByMe: followingIds.has(String(d._id)),
  }));

  // Non-seguiti prima (scoperta), poi ordine alfabetico per stabilità.
  items.sort((a, b) => {
    if (a.isFollowedByMe !== b.isFollowedByMe) return a.isFollowedByMe ? 1 : -1;
    return (a.user.username || "").localeCompare(b.user.username || "");
  });

  return { items };
}

// ── METRICHE PROFILO (riepilogo escursionistico pubblico) ───────────────────

/**
 * Totali escursionistici ALL-TIME di un utente, per il "biglietto da visita"
 * sul profilo (km, dislivello, uscite, punti).
 *
 * Aggrega due sorgenti, identico criterio di [hikeSessionService.getActivityStats]
 * ma senza filtro anno:
 *   - HikeSession COMPLETED dove l'utente è creator o partecipante
 *     (preferisce `actualStats`, fallback `gpxStats`);
 *   - Activity libere dell'utente (`actualStats`).
 *
 * Le due collection sono disgiunte (una sessione completata NON crea anche
 * un'Activity), quindi la somma non doppia-conta — coerente con le card
 * "Le Mie Attività".
 *
 * @param {string|ObjectId} userId
 * @returns {Promise<{totalActivities:number,totalDistanceKm:number,totalElevationGainM:number,totalPoints:number}>}
 */
export async function getPublicHikingStats(userId) {
  const [sessions, activities] = await Promise.all([
    HikeSession.find({
      $or: [{ creatorId: userId }, { "participants.userId": userId }],
      status: "COMPLETED",
    })
      .select("actualStats gpxStats")
      .lean(),
    Activity.find({ userId }).select("actualStats").lean(),
  ]);

  let totalDistanceKm = 0;
  let totalElevationGainM = 0;
  let totalPoints = 0;

  for (const s of sessions) {
    totalDistanceKm +=
      s.actualStats?.distanceMeters != null
        ? s.actualStats.distanceMeters / 1000.0
        : s.gpxStats?.distanceKm || 0;
    totalElevationGainM +=
      s.actualStats?.elevationGainM ?? s.gpxStats?.elevationGainM ?? 0;
    totalPoints += s.actualStats?.finalPoints ?? s.gpxStats?.estimatedPoints ?? 0;
  }
  for (const a of activities) {
    totalDistanceKm += (a.actualStats?.distanceMeters || 0) / 1000.0;
    totalElevationGainM += a.actualStats?.elevationGainM || 0;
    totalPoints += a.actualStats?.finalPoints || 0;
  }

  return {
    totalActivities: sessions.length + activities.length,
    totalDistanceKm: Math.round(totalDistanceKm * 10) / 10,
    totalElevationGainM,
    totalPoints,
  };
}

// ── CLASSIFICA SETTIMANALE ──────────────────────────────────────────────────

const WEEK_MS = 7 * 24 * 3600 * 1000;

/**
 * Classifica settimanale (rolling 7 giorni) tra il viewer e gli utenti che
 * segue: per ciascuno aggrega km / dislivello / punti / uscite delle Activity
 * libere e delle HikeSession COMPLETED (creator o partecipante).
 *
 * Restituisce TUTTE le metriche per ogni utente attivo, così il client può
 * cambiare il criterio di ordinamento (km/dislivello/punti) senza ri-chiamare
 * il server. Default ordinato per km desc. Include solo chi ha ≥1 uscita nella
 * finestra; `isMe` evidenzia la riga del viewer.
 *
 * @param {string|ObjectId} viewerId
 * @returns {Promise<{since:string, items:Array}>}
 */
export async function getWeeklyLeaderboard(viewerId) {
  const followingIds = await getFollowingIds(viewerId);
  const uniqueIds = [
    ...new Set([...followingIds.map(String), String(viewerId)]),
  ];
  const since = new Date(Date.now() - WEEK_MS);

  const totals = new Map(); // uid -> { km, elevM, points, count }
  const bump = (uid, km, elev, pts) => {
    if (!uniqueIds.includes(uid)) return; // solo viewer + seguiti
    const cur = totals.get(uid) ?? { km: 0, elevM: 0, points: 0, count: 0 };
    cur.km += km || 0;
    cur.elevM += elev || 0;
    cur.points += pts || 0;
    cur.count += 1;
    totals.set(uid, cur);
  };

  const [activities, sessions] = await Promise.all([
    Activity.find({
      userId: { $in: uniqueIds },
      completedAt: { $gte: since },
    })
      .select("userId actualStats")
      .lean(),
    HikeSession.find({
      status: "COMPLETED",
      $or: [
        { creatorId: { $in: uniqueIds } },
        { "participants.userId": { $in: uniqueIds } },
      ],
    })
      .select("creatorId participants actualStats gpxStats endTime createdAt")
      .lean(),
  ]);

  for (const a of activities) {
    bump(
      String(a.userId),
      (a.actualStats?.distanceMeters || 0) / 1000.0,
      a.actualStats?.elevationGainM || 0,
      a.actualStats?.finalPoints || 0,
    );
  }
  for (const s of sessions) {
    const ref = s.endTime || s.createdAt;
    if (!ref || new Date(ref) < since) continue;
    const km =
      s.actualStats?.distanceMeters != null
        ? s.actualStats.distanceMeters / 1000.0
        : s.gpxStats?.distanceKm || 0;
    const elev = s.actualStats?.elevationGainM ?? s.gpxStats?.elevationGainM ?? 0;
    const pts = s.actualStats?.finalPoints ?? s.gpxStats?.estimatedPoints ?? 0;
    // Chi "ha fatto" la sessione: creator + partecipanti (tutti hanno camminato).
    const members = new Set([
      String(s.creatorId),
      ...(s.participants || []).map((p) => String(p.userId)),
    ]);
    for (const uid of members) bump(uid, km, elev, pts);
  }

  const rankedIds = [...totals.keys()];
  const users = await Hiker.find({ _id: { $in: rankedIds } })
    .select("username personalInfo.avatarUrl")
    .lean();
  const userMap = new Map(users.map((u) => [String(u._id), u]));

  const items = rankedIds
    .map((uid) => {
      const t = totals.get(uid);
      const u = userMap.get(uid);
      return {
        user: {
          _id: uid,
          username: u?.username ?? null,
          personalInfo: u?.personalInfo?.avatarUrl
            ? { avatarUrl: u.personalInfo.avatarUrl }
            : null,
        },
        km: Math.round(t.km * 10) / 10,
        elevM: t.elevM,
        points: t.points,
        count: t.count,
        isMe: uid === String(viewerId),
      };
    })
    .sort((a, b) => b.km - a.km || b.elevM - a.elevM);

  return { since: since.toISOString(), items };
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
  // Gate di visibilità: il viewer segue ciascun autore (quindi ne è follower),
  // perciò gli autori "public" e "friends" sono sempre ammessi nel feed. Vanno
  // esclusi solo gli autori che hanno impostato il profilo su "private".
  const visMap = await getVisibilityForAuthors(followingIds);
  const allowedFollowingIds = followingIds.filter(
    (id) => visMap.get(String(id)) !== "private",
  );
  // Include "me" così l'utente vede i propri post nel feed. ObjectId esplicito
  // per evitare Cast string->ObjectId implicito che a volte non match nei $in.
  const visibleAuthorIds = [
    ...allowedFollowingIds,
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
  // ── "La tua storia" (stile Instagram) ──────────────────────────────────────
  // Il viewer NON segue se stesso, quindi le PROPRIE storie non passerebbero mai
  // dalle query sui `followingIds`: senza questo blocco, dopo aver pubblicato una
  // storia l'utente "non la vede da nessuna parte". La recuperiamo a parte e
  // mettiamo l'eventuale entry self IN TESTA alla row.
  const ownStories = await Story.find({
    authorId: viewerId,
    expiresAt: { $gt: new Date() },
  })
    .select("_id")
    .lean();
  let selfItem = null;
  if (ownStories.length > 0) {
    const self = await Hiker.findById(viewerId)
      .select("username personalInfo.avatarUrl")
      .lean();
    if (self) {
      selfItem = {
        user: {
          _id: String(self._id),
          username: self.username,
          personalInfo: self.personalInfo
            ? { avatarUrl: self.personalInfo.avatarUrl ?? null }
            : null,
        },
        status: "story",
        isSelf: true,
        // Anello pieno: feedback chiaro che la TUA storia è online (24h).
        hasUnviewedStory: true,
      };
    }
  }

  const rawFollowingIds = await getFollowingIds(viewerId);
  if (rawFollowingIds.length === 0) return { items: selfItem ? [selfItem] : [] };
  // Gate di visibilità: gli autori "private" non compaiono nella row (né story
  // né live). "public"/"friends" restano visibili (il viewer ne è follower).
  const visMap = await getVisibilityForAuthors(rawFollowingIds);
  const followingIds = rawFollowingIds.filter(
    (id) => visMap.get(String(id)) !== "private",
  );
  if (followingIds.length === 0) return { items: selfItem ? [selfItem] : [] };

  const since7d = new Date(Date.now() - 7 * STORY_WINDOW_MS);

  // 5 query parallele per minimizzare la latenza totale.
  const [
    hikers,            // anagrafica + weeklyGoals
    liveSessions,      // ACTIVE sessions
    stories,           // storie reali non scadute (TTL 24h)
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
    Story.find({
      authorId: { $in: followingIds },
      expiresAt: { $gt: new Date() },
    })
      .select("authorId viewers createdAt")
      .sort({ createdAt: -1 })
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
  // Un autore ha "story" se possiede ≥1 Story non scaduta. `hasUnviewed` è true
  // se almeno una delle sue storie non è stata vista dal viewer → anello pieno.
  const storyByUser = new Map();
  for (const st of stories) {
    const uid = String(st.authorId);
    const viewed = (st.viewers || []).some(
      (v) => String(v.userId) === String(viewerId),
    );
    const cur = storyByUser.get(uid);
    if (!cur) {
      storyByUser.set(uid, { hasUnviewed: !viewed });
    } else if (!viewed) {
      cur.hasUnviewed = true;
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
        hasUnviewedStory: storyByUser.get(uid).hasUnviewed,
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

  // "La tua storia" sempre in testa alla row (come Instagram).
  return { items: selfItem ? [selfItem, ...items] : items };
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

  // Gate di visibilità a livello account (solo per viewer != autore):
  //   - "private" → nessun post visibile
  //   - "friends" → visibile solo se il viewer è follower dell'autore
  //   - "public"  → post condivisi visibili a chiunque
  if (!isSelf) {
    const visMap = await getVisibilityForAuthors([authorId]);
    const visibility = visMap.get(String(authorId)) ?? "friends";
    if (visibility === "private") return { items: [], hasMore: false };
    if (visibility === "friends" && !(await isFollowing(viewerId, authorId))) {
      return { items: [], hasMore: false };
    }
  }

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
