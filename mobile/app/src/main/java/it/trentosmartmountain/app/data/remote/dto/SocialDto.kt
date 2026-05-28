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
    @SerializedName("distanceMeters") val distanceMeters: Double?,
    @SerializedName("movingSeconds") val movingSeconds: Long?,
    @SerializedName("elevationGainM") val elevationGainM: Int?,
    @SerializedName("finalPoints") val finalPoints: Int?,
    @SerializedName("elevationProfile") val elevationProfile: List<Double>? = null,
    @SerializedName("participants") val participants: List<FeedUser>? = null,
    @SerializedName("likesCount") val likesCount: Int = 0,
    @SerializedName("commentsCount") val commentsCount: Int = 0,
    @SerializedName("likedByMe") val likedByMe: Boolean = false,
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
)

data class FollowListResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("items") val items: List<FollowListEntry> = emptyList(),
)

data class FollowListEntry(
    @SerializedName("user") val user: FeedUser?,
    @SerializedName("since") val since: String?,
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
    @SerializedName("socialCredits") val socialCredits: Int? = null,
    @SerializedName("personalInfo") val personalInfo: FeedUserPersonalInfo? = null,
) {
    val avatarUrl: String? get() = personalInfo?.avatarUrl
}
