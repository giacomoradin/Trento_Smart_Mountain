import Story, { STORY_TTL_MS } from "../models/story.js";
import HikeSession from "../models/hikeSession.js";
import Activity from "../models/activity.js";
import User from "../models/user.js";
import { getFollowingIds } from "./followService.js";

// Cap dimensione TOTALE dei media per storia (oltre al cap per-media di Joi):
// restiamo sotto il body limit globale di 5mb (securityMiddleware) con margine.
const STORY_TOTAL_MEDIA_MAX_CHARS = 4_500_000;

const AUTHOR_POPULATE = {
  path: "authorId",
  select: "username personalInfo.avatarUrl",
};

/** L'autore è membro accettato (o creator) della sessione indicata? */
function isSessionMember(session, userId) {
  if (String(session.creatorId) === String(userId)) return true;
  return (session.participants || []).some(
    (p) =>
      String(p.userId?._id || p.userId) === String(userId) &&
      p.status !== "pending",
  );
}

/**
 * Verifica che l'autore possa creare una storia per il riferimento indicato e
 * ritorna l'entità referenziata (per lo snapshot inviteCode).
 */
async function resolveReference(authorId, { type, sessionId, activityId }) {
  if (type === "planned_session") {
    const session = await HikeSession.findById(sessionId).select(
      "participants creatorId inviteCode status",
    );
    if (!session || !isSessionMember(session, authorId)) {
      throw new Error("STORY_FORBIDDEN_REF");
    }
    return { session };
  }
  // type === "activity": può riferire una propria Activity oppure una sessione
  // di cui l'autore è membro.
  if (activityId) {
    const activity = await Activity.findById(activityId).select("userId");
    if (!activity || String(activity.userId) !== String(authorId)) {
      throw new Error("STORY_FORBIDDEN_REF");
    }
    return { activity };
  }
  if (sessionId) {
    const session = await HikeSession.findById(sessionId).select(
      "participants creatorId",
    );
    if (!session || !isSessionMember(session, authorId)) {
      throw new Error("STORY_FORBIDDEN_REF");
    }
    return { session };
  }
  throw new Error("STORY_FORBIDDEN_REF");
}

/** Crea una storia (foto/video + overlay tracciamento). TTL 24h. */
export async function createStory(authorId, payload) {
  const {
    type,
    sessionId = null,
    activityId = null,
    caption = null,
    media = [],
    overlay = null,
  } = payload;

  const totalChars = (media || []).reduce(
    (sum, m) => sum + (m?.dataUri?.length || 0),
    0,
  );
  if (totalChars > STORY_TOTAL_MEDIA_MAX_CHARS) {
    throw new Error("STORY_MEDIA_TOO_LARGE");
  }

  const ref = await resolveReference(authorId, { type, sessionId, activityId });

  const story = new Story({
    authorId,
    type,
    sessionId: sessionId || null,
    activityId: activityId || null,
    inviteCode:
      type === "planned_session" ? ref.session?.inviteCode || null : null,
    caption: typeof caption === "string" ? caption.trim() || null : null,
    media,
    overlay: overlay || null,
    createdAt: new Date(),
    expiresAt: new Date(Date.now() + STORY_TTL_MS),
  });
  await story.save();
  return story.populate(AUTHOR_POPULATE);
}

/**
 * Storie non scadute di un autore, in ordine cronologico (più vecchia prima)
 * per la riproduzione sequenziale nel viewer. Applica il gate di visibilità.
 */
export async function getStoriesByAuthor(viewerId, authorId) {
  if (String(authorId) !== String(viewerId)) {
    const author = await User.findById(authorId)
      .select("preferences.privacy.profileVisibility")
      .lean();
    const vis = author?.preferences?.privacy?.profileVisibility ?? "friends";
    if (vis === "private") return { items: [] };
    if (vis === "friends") {
      const following = await getFollowingIds(viewerId);
      if (!following.some((id) => String(id) === String(authorId))) {
        return { items: [] };
      }
    }
  }

  const stories = await Story.find({
    authorId,
    expiresAt: { $gt: new Date() },
  })
    .sort({ createdAt: 1 })
    .populate(AUTHOR_POPULATE)
    .lean();

  const items = stories.map((s) => {
    const viewedByMe = (s.viewers || []).some(
      (v) => String(v.userId) === String(viewerId),
    );
    const { viewers, ...rest } = s; // non esporre la lista completa dei viewer
    return { ...rest, viewedByMe };
  });
  return { items };
}

/** Marca la storia come vista dal viewer (idempotente). */
export async function markStoryViewed(storyId, viewerId) {
  const updated = await Story.findOneAndUpdate(
    { _id: storyId, "viewers.userId": { $ne: viewerId } },
    { $push: { viewers: { userId: viewerId, viewedAt: new Date() } } },
    { new: true },
  );
  if (!updated) {
    // null = già vista (ok) oppure inesistente: distinguiamo con un exists leggero.
    const exists = await Story.exists({ _id: storyId });
    if (!exists) throw new Error("STORY_NOT_FOUND");
  }
  return { ok: true };
}

/** Elimina una storia (solo autore). */
export async function deleteStory(storyId, authorId) {
  const story = await Story.findById(storyId).select("authorId");
  if (!story) throw new Error("STORY_NOT_FOUND");
  if (String(story.authorId) !== String(authorId)) {
    throw new Error("FORBIDDEN_NOT_OWNER");
  }
  await story.deleteOne();
  return { ok: true };
}

/** Singola storia per deep-link (es. da notifica). */
export async function getStoryById(viewerId, storyId) {
  const story = await Story.findById(storyId).populate(AUTHOR_POPULATE).lean();
  if (!story || (story.expiresAt && story.expiresAt <= new Date())) {
    throw new Error("STORY_NOT_FOUND");
  }
  const viewedByMe = (story.viewers || []).some(
    (v) => String(v.userId) === String(viewerId),
  );
  const { viewers, ...rest } = story;
  return { ...rest, viewedByMe };
}
