import RefugeBoardPost from "../models/refugeBoardPost.js";
import Refuge from "../models/refuge.js";

/**
 * Servizio Bacheca rifugi.
 *
 * Convenzione errori (mappati a HTTP dal global error mapper):
 *   - BOARD_FORBIDDEN_ROLE → 403 (solo i rifugi possono pubblicare)
 *   - POST_NOT_FOUND       → 404
 *   - FORBIDDEN_NOT_AUTHOR → 403 (delete/edit di post altrui da non-admin)
 *   - INVALID_TYPE         → 422
 */

const VALID_TYPES = ["info", "avviso", "pericolo"];

/**
 * Crea un post in bacheca. Solo gli account rifugio possono pubblicare.
 * `refugeName` viene denormalizzato dal profilo rifugio per il feed utenti.
 */
export async function createBoardPost(refugeId, role, { type, title, body, validUntil } = {}) {
  if (role !== "rifugio" && role !== "admin") {
    throw new Error("BOARD_FORBIDDEN_ROLE");
  }
  const t = (title || "").trim();
  const b = (body || "").trim();
  if (!t || !b) throw new Error("POST_EMPTY");
  const safeType = VALID_TYPES.includes(type) ? type : "info";

  const refuge = await Refuge.findById(refugeId).select("rifugioName").lean();
  const post = await RefugeBoardPost.create({
    refugeId,
    refugeName: refuge?.rifugioName || "Rifugio",
    type: safeType,
    title: t.slice(0, 120),
    body: b.slice(0, 2000),
    validUntil: validUntil ? new Date(validUntil) : null,
  });
  return post.toObject();
}

/**
 * Lista paginata dei post in bacheca (feed utenti).
 *  - `type` opzionale filtra per categoria.
 *  - `activeOnly` (default true) nasconde i post scaduti (validUntil < now).
 */
export async function listBoardPosts({ page = 1, limit = 20, type = null, activeOnly = true } = {}) {
  const skip = (Math.max(1, page) - 1) * limit;
  const filter = {};
  if (type && VALID_TYPES.includes(type)) filter.type = type;
  if (activeOnly) {
    filter.$or = [{ validUntil: null }, { validUntil: { $gte: new Date() } }];
  }
  const [items, count] = await Promise.all([
    RefugeBoardPost.find(filter).sort({ createdAt: -1 }).skip(skip).limit(limit).lean(),
    RefugeBoardPost.countDocuments(filter),
  ]);
  return {
    count,
    hasMore: skip + items.length < count,
    items: items.map(shape),
  };
}

/** Post pubblicati dal rifugio loggato (per la gestione lato rifugista). */
export async function getMyBoardPosts(refugeId, { page = 1, limit = 20 } = {}) {
  const skip = (Math.max(1, page) - 1) * limit;
  const [items, count] = await Promise.all([
    RefugeBoardPost.find({ refugeId }).sort({ createdAt: -1 }).skip(skip).limit(limit).lean(),
    RefugeBoardPost.countDocuments({ refugeId }),
  ]);
  return { count, hasMore: skip + items.length < count, items: items.map(shape) };
}

/** Elimina un post. Solo l'autore (rifugio) o un admin. */
export async function deleteBoardPost(postId, userId, { isAdmin = false } = {}) {
  const post = await RefugeBoardPost.findById(postId);
  if (!post) throw new Error("POST_NOT_FOUND");
  if (!isAdmin && String(post.refugeId) !== String(userId)) {
    throw new Error("FORBIDDEN_NOT_OWNER");
  }
  await post.deleteOne();
  return { _id: String(postId) };
}

/** Aggiorna un proprio post (autore o admin). Solo i campi forniti. */
export async function updateBoardPost(postId, userId, { isAdmin = false, type, title, body, validUntil } = {}) {
  const post = await RefugeBoardPost.findById(postId);
  if (!post) throw new Error("POST_NOT_FOUND");
  if (!isAdmin && String(post.refugeId) !== String(userId)) {
    throw new Error("FORBIDDEN_NOT_OWNER");
  }
  if (type !== undefined && VALID_TYPES.includes(type)) post.type = type;
  if (title !== undefined) post.title = String(title).trim().slice(0, 120);
  if (body !== undefined) post.body = String(body).trim().slice(0, 2000);
  if (validUntil !== undefined) post.validUntil = validUntil ? new Date(validUntil) : null;
  await post.save();
  return shape(post.toObject());
}

function shape(p) {
  return {
    _id: String(p._id),
    refugeId: String(p.refugeId),
    refugeName: p.refugeName,
    type: p.type,
    title: p.title,
    body: p.body,
    validUntil: p.validUntil,
    createdAt: p.createdAt,
  };
}
