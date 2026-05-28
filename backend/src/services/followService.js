import mongoose from "mongoose";
import Follow from "../models/follow.js";
import User from "../models/user.js";

/**
 * Servizio per la gestione delle relazioni di follow asimmetrico.
 *
 * Convenzione errori (mappati a HTTP dal global error mapper):
 *   - SELF_FOLLOW       → 400  (non puoi seguire te stesso)
 *   - USER_NOT_FOUND    → 404  (l'utente target non esiste)
 *   - ALREADY_FOLLOWING → 409  (lo segui già — idempotenza)
 *   - NOT_FOLLOWING     → 404  (non lo segui, niente da rimuovere)
 *
 * Race condition: la creazione di un duplicate viene catturata dal vincolo
 * `unique compound` dell'indice (`E11000` lato Mongoose) e tradotta in
 * `ALREADY_FOLLOWING`. Idempotente by-design.
 */

/**
 * Crea una relazione di follow (followerId → followingId).
 * Idempotente: se esiste già, ritorna lo stesso documento senza errore.
 *
 * Anti-self-follow: errore 400 se followerId === followingId.
 * Validazione esistenza target: lookup User.findById prima dell'insert.
 */
export async function followUser(followerId, followingId) {
  if (String(followerId) === String(followingId)) {
    throw new Error("SELF_FOLLOW");
  }
  // Lookup esplicito così possiamo distinguere 404 (utente inesistente) da
  // 500 (DB error). Il ref non valida l'esistenza automaticamente.
  const target = await User.findById(followingId).select("_id").lean();
  if (!target) throw new Error("USER_NOT_FOUND");

  // Check esplicito prima del create: idempotenza a livello service. Il
  // vincolo `unique` composto su (followerId, followingId) è il safety net
  // per le race condition tra POST concorrenti, ma negli ambienti dove gli
  // indici sono creati lazy (es. mongodb-memory-server in test al primo run)
  // il vincolo può non essere ancora attivo al primo insert. Senza questo
  // pre-check il test "second POST does not create duplicate" risulta flaky.
  const existing = await Follow.findOne({ followerId, followingId }).lean();
  if (existing) return existing;

  try {
    const follow = await Follow.create({ followerId, followingId });
    return follow.toObject();
  } catch (err) {
    if (err.code === 11000) {
      // Duplicate key (race tra check e create): idempotente, ritorniamo l'esistente.
      return Follow.findOne({ followerId, followingId }).lean();
    }
    throw err;
  }
}

/**
 * Rimuove una relazione di follow. Errore 404 NOT_FOLLOWING se non esiste:
 * scelta UX deliberata invece di idempotenza silente — l'utente vuole
 * sapere se l'azione ha effetto (per aggiornare lo stato del bottone).
 */
export async function unfollowUser(followerId, followingId) {
  const result = await Follow.findOneAndDelete({ followerId, followingId });
  if (!result) throw new Error("NOT_FOLLOWING");
  return { followerId, followingId };
}

/**
 * Lista paginata degli utenti che `userId` segue.
 *
 * Populate restituisce `{user, since}` per ogni riga, dove `user` ha
 * `_id`, `username` e — per coerenza con la sezione Social — anche
 * `personalInfo.avatarUrl` (vedi userPrivacy.js: avatar pubblico per
 * gli "other viewer").
 */
export async function getFollowing(userId, { page = 1, limit = 20 } = {}) {
  const skip = (page - 1) * limit;
  const [items, count] = await Promise.all([
    Follow.find({ followerId: userId })
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit)
      .populate("followingId", "username personalInfo.avatarUrl")
      .lean(),
    Follow.countDocuments({ followerId: userId }),
  ]);
  return {
    count,
    items: items.map((f) => ({ user: f.followingId, since: f.createdAt })),
  };
}

/** Simmetrico di getFollowing: lista degli utenti che seguono `userId`. */
export async function getFollowers(userId, { page = 1, limit = 20 } = {}) {
  const skip = (page - 1) * limit;
  const [items, count] = await Promise.all([
    Follow.find({ followingId: userId })
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit)
      .populate("followerId", "username personalInfo.avatarUrl")
      .lean(),
    Follow.countDocuments({ followingId: userId }),
  ]);
  return {
    count,
    items: items.map((f) => ({ user: f.followerId, since: f.createdAt })),
  };
}

/**
 * Statistiche di follow su un utente target dal punto di vista del viewer.
 * Restituisce contatori + `isFollowedByMe` (utile per renderizzare il bottone
 * Segui/Smetti senza una seconda query lato client).
 */
export async function getFollowStats(targetUserId, viewerId) {
  const [followers, following, mine] = await Promise.all([
    Follow.countDocuments({ followingId: targetUserId }),
    Follow.countDocuments({ followerId: targetUserId }),
    viewerId
      ? Follow.exists({ followerId: viewerId, followingId: targetUserId })
      : null,
  ]);
  return {
    followers,
    following,
    isFollowedByMe: Boolean(mine),
  };
}

/**
 * Helper interno usato dal feed: ritorna gli ObjectId degli utenti che
 * `userId` segue. Distinct per evitare duplicati (non ne genera mai per via
 * dell'indice unique, ma defensive).
 */
export async function getFollowingIds(userId) {
  return Follow.find({ followerId: userId }).distinct("followingId");
}
