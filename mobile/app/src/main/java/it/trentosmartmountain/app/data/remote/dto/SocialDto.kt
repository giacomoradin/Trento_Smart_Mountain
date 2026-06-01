package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO della schermata Social (Sprint 2).
 *
 * Mappano 1:1 i payload del backend (`socialService.toFeedItem` per il feed,
 * `shareActivity` / `likeActivity` per le risposte azioni). Tutti i campi
 * con valori opzionali sono `nullable` lato Kotlin per non crashare sul
 * deserialize quando il server li omette (post privato, attività senza GPS, ecc.).
 *
 * NB: `FeedUser` non riusa `SessionUserInfo` perché la shape, pur identica,
 * appartiene a contesti semanticamente diversi -- evita confusione cross-screen.
 * `FeedUserPersonalInfo` è ridotto al solo `avatarUrl` (privacy gate).
 */

// ── Feed ──────────────────────────────────────────────────────────────────

data class FeedResponse(
    @SerializedName("items") val items: List<FeedItem> = emptyList(),
    @SerializedName("hasMore") val hasMore: Boolean = false,
)

data class FeedItem(
    @SerializedName("kind") val kind: String,            // "activity" | "session"
    @SerializedName("id") val id: String,
    @SerializedName("user") val user: FeedUser?,
    @SerializedName("sharedAt") val sharedAt: String?,    // ISO 8601
    @SerializedName("caption") val caption: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("activityType") val activityType: String? = null,    // hiking|trail|skitouring|trekking
    @SerializedName("difficultyLevel") val difficultyLevel: String? = null, // T|E|EE|EEA
    @SerializedName("distanceMeters") val distanceMeters: Double?,
    @SerializedName("movingSeconds") val movingSeconds: Long?,
    @SerializedName("elevationGainM") val elevationGainM: Int?,
    @SerializedName("finalPoints") val finalPoints: Int?,
    @SerializedName("elevationProfile") val elevationProfile: List<Double>? = null,
    /** Traccia GPS campionata (route signature). null se l'attività/sessione non ha geometria. */
    @SerializedName("routePolyline") val routePolyline: List<RoutePoint>? = null,
    @SerializedName("participants") val participants: List<FeedUser>? = null,
    @SerializedName("likesCount") val likesCount: Int = 0,
    @SerializedName("commentsCount") val commentsCount: Int = 0,
    @SerializedName("likedByMe") val likedByMe: Boolean = false,
)

/** Punto della route signature. Formato `{lat, lon}` coerente col backend. */
data class RoutePoint(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
)

data class FeedUser(
    @SerializedName("_id") val _id: String,
    @SerializedName("username") val username: String?,
    @SerializedName("personalInfo") val personalInfo: FeedUserPersonalInfo? = null,
) {
    /** Accesso conveniente all'avatar (i campi privati di personalInfo sono
     *  rimossi dal privacy gate per gli "other viewer"). */
    val avatarUrl: String? get() = personalInfo?.avatarUrl
}

data class FeedUserPersonalInfo(
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
)

// ── Share / Unshare ────────────────────────────────────────────────────────

/** Body opzionale per share — caption max 200 caratteri, lato server. */
data class ShareRequest(
    @SerializedName("caption") val caption: String? = null,
)

data class ShareResponse(
    @SerializedName("sharedAt") val sharedAt: String?,
    @SerializedName("caption") val caption: String?,
)

// ── Like ──────────────────────────────────────────────────────────────────

data class LikeResponse(
    @SerializedName("likesCount") val likesCount: Int,
    @SerializedName("likedByMe") val likedByMe: Boolean,
)

// ── Follow ────────────────────────────────────────────────────────────────

data class FollowStatsResponse(
    @SerializedName("followers") val followers: Int,
    @SerializedName("following") val following: Int,
    @SerializedName("isFollowedByMe") val isFollowedByMe: Boolean,
    /** True se l'utente target segue il viewer → badge "Ti segue" sul profilo. */
    @SerializedName("followsViewer") val followsViewer: Boolean = false,
)

/**
 * Totali escursionistici ALL-TIME per il "biglietto da visita" del profilo
 * (GET /api/v1/users/:id/hiking-stats). Aggrega sessioni COMPLETED + attività libere.
 */
data class HikingStatsResponse(
    @SerializedName("totalActivities") val totalActivities: Int = 0,
    @SerializedName("totalDistanceKm") val totalDistanceKm: Double = 0.0,
    @SerializedName("totalElevationGainM") val totalElevationGainM: Int = 0,
    @SerializedName("totalPoints") val totalPoints: Int = 0,
)

data class FollowListResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("items") val items: List<FollowListEntry> = emptyList(),
)

data class FollowListEntry(
    @SerializedName("user") val user: FeedUser?,
    @SerializedName("since") val since: String?,
)

// ── Ricerca / scoperta utenti (GET /api/v1/users/search) ────────────────────

/** Risposta di GET /users/search: lista di utenti che matchano lo username. */
data class UserSearchResponse(
    @SerializedName("items") val items: List<UserSearchItem> = emptyList(),
)

/**
 * Singolo risultato di ricerca: utente pubblico (username + avatar) +
 * `isFollowedByMe` per mostrare subito "Segui"/"Seguito" senza altra query.
 */
data class UserSearchItem(
    @SerializedName("user") val user: FeedUser?,
    @SerializedName("isFollowedByMe") val isFollowedByMe: Boolean = false,
)

// ── Classifica settimanale (GET /api/v1/users/me/weekly-leaderboard) ────────

data class WeeklyLeaderboardResponse(
    @SerializedName("since") val since: String? = null,
    @SerializedName("items") val items: List<LeaderboardEntry> = emptyList(),
)

/** Riga della classifica: utente + totali settimanali (km/dislivello/punti/uscite). */
data class LeaderboardEntry(
    @SerializedName("user") val user: FeedUser?,
    @SerializedName("km") val km: Double = 0.0,
    @SerializedName("elevM") val elevM: Int = 0,
    @SerializedName("points") val points: Int = 0,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("isMe") val isMe: Boolean = false,
)

// ── Notifiche (/api/v1/users/me/notifications) ──────────────────────────────

data class NotificationsResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("hasMore") val hasMore: Boolean = false,
    @SerializedName("items") val items: List<NotificationItem> = emptyList(),
)

/**
 * Notifica social. `type` ∈ {follow, like, comment}; per like/comment
 * `targetKind`/`targetId` puntano all'Activity/HikeSession coinvolta (per il deep-link).
 */
data class NotificationItem(
    @SerializedName("_id") val _id: String,
    @SerializedName("type") val type: String,
    @SerializedName("actor") val actor: FeedUser?,
    @SerializedName("targetKind") val targetKind: String? = null,
    @SerializedName("targetId") val targetId: String? = null,
    @SerializedName("read") val read: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null,
)

data class UnreadCountResponse(
    @SerializedName("unreadCount") val unreadCount: Int = 0,
)

data class MarkReadResponse(
    @SerializedName("updated") val updated: Int = 0,
)

// ── Commenti ───────────────────────────────────────────────────────────────

/** Body POST commento — server valida 1..500 char + trim. */
data class CreateCommentRequest(
    @SerializedName("text") val text: String,
)

/** Risposta a POST commento: il server ritorna il commento populated (user). */
data class CreateCommentResponse(
    @SerializedName("comment") val comment: CommentItem,
)

/** Lista paginata di commenti restituita da GET comments. */
data class CommentListResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("items") val items: List<CommentItem> = emptyList(),
)

/**
 * Singolo commento. `userId` arriva populated dal backend con username +
 * `personalInfo.avatarUrl` (privacy gate selettivo). Gson lo deserializza
 * come oggetto `FeedUser` riusato.
 */
data class CommentItem(
    @SerializedName("_id") val _id: String,
    @SerializedName("activityRefId") val activityRefId: String?,
    @SerializedName("kind") val kind: String?,           // "activity" | "session"
    @SerializedName("userId") val userId: FeedUser?,
    @SerializedName("text") val text: String,
    @SerializedName("createdAt") val createdAt: String?, // ISO 8601
)

/**
 * Profilo utente "pubblico" come ritornato da `GET /hikers/:id` o `/users/:id`
 * con privacy gate applicato (avatar pubblico, resto privato per other-view).
 *
 * Per la `UserProfileScreen` carichiamo:
 *  - il profilo via `getHikerById(id)` → username, avatar, isVerified
 *  - le stats via `getFollowStats(id)` → followers/following/isFollowedByMe
 *  - i post via `getUserPosts(id)` → `FeedResponse` riusata
 */
data class PublicUserProfile(
    @SerializedName("_id") val _id: String,
    @SerializedName("username") val username: String?,
    @SerializedName("email") val email: String? = null,
    @SerializedName("isVerified") val isVerified: Boolean? = null,
    /** True se il profilo è limitato per visibilità (private / friends-non-seguito). */
    @SerializedName("restricted") val restricted: Boolean? = null,
    /** "public" | "friends" | "private" — presente quando restricted. */
    @SerializedName("visibility") val visibility: String? = null,
    @SerializedName("socialCredits") val socialCredits: Int? = null,
    @SerializedName("personalInfo") val personalInfo: FeedUserPersonalInfo? = null,
) {
    val avatarUrl: String? get() = personalInfo?.avatarUrl
}

// ── Social Row (avatar row in cima al feed) ────────────────────────────────

/** Risposta di `GET /api/v1/users/me/social-row`. */
data class SocialRowResponse(
    @SerializedName("items") val items: List<SocialRowItem> = emptyList(),
)

/**
 * Singolo elemento della Avatar Row.
 *
 * Status (vedi sprint2_social.md §1):
 *  - "live"     → utente con HikeSession ACTIVE (anello giallo animato)
 *  - "story"    → utente con shared in last 24h (anello azzurro pieno);
 *                 filtrato lato client contro `viewed_stories` per "viste"
 *  - "goal"     → progresso settimanale (anello verde arco proporzionale)
 *  - "neutral"  → nessuno stato (anello grigio)
 *
 * Campi opzionali popolati solo per il loro status:
 *  - [liveSessionId] solo per "live" → deep link a SessionDetail
 *  - [storyActivityRef] solo per "story" → apre StoryViewerScreen
 *  - [weeklyProgressPct] solo per "goal" ∈ [0,1]
 */
data class SocialRowItem(
    @SerializedName("user") val user: FeedUser,
    @SerializedName("status") val status: String,
    @SerializedName("liveSessionId") val liveSessionId: String? = null,
    @SerializedName("storyActivityRef") val storyActivityRef: StoryActivityRef? = null,
    @SerializedName("weeklyProgressPct") val weeklyProgressPct: Float? = null,
)

data class StoryActivityRef(
    @SerializedName("id") val id: String,
    @SerializedName("kind") val kind: String,         // "activity" | "session"
    @SerializedName("sharedAt") val sharedAt: String?,
)
