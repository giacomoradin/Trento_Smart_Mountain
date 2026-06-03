package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO delle Stories (Fase B/C). Le storie vivono 24h e contengono media
 * (foto/video brevi Base64) + un overlay di tracciamento snapshot.
 *
 * - `type = "planned_session"` → condivisione pre-hike con `inviteCode` per il
 *   bottone "Unisciti".
 * - `type = "activity"` → preview post-hike di un'attività completata.
 */

/** Risposta di GET /api/v1/stories/user/:userId — storie non scadute di un autore. */
data class StoriesResponse(
    @SerializedName("items") val items: List<StoryItem> = emptyList(),
)

data class StoryItem(
    @SerializedName("_id") val id: String,
    /** Autore populated dal backend ({_id, username, personalInfo.avatarUrl}). */
    @SerializedName("authorId") val author: FeedUser?,
    @SerializedName("type") val type: String,
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("activityId") val activityId: String? = null,
    /** Codice invito (solo planned_session) per il bottone "Unisciti". */
    @SerializedName("inviteCode") val inviteCode: String? = null,
    @SerializedName("caption") val caption: String? = null,
    @SerializedName("media") val media: List<StoryMedia> = emptyList(),
    @SerializedName("overlay") val overlay: StoryOverlay? = null,
    @SerializedName("viewedByMe") val viewedByMe: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null,
)

data class StoryMedia(
    @SerializedName("kind") val kind: String, // "image" | "video"
    @SerializedName("dataUri") val dataUri: String,
    @SerializedName("durationSec") val durationSec: Double? = null,
)

/** Snapshot dei dati di tracciamento da disegnare in overlay sul media. */
data class StoryOverlay(
    @SerializedName("title") val title: String? = null,
    @SerializedName("activityType") val activityType: String? = null,
    @SerializedName("difficultyLevel") val difficultyLevel: String? = null,
    @SerializedName("distanceMeters") val distanceMeters: Double? = null,
    @SerializedName("elevationGainM") val elevationGainM: Int? = null,
    @SerializedName("movingSeconds") val movingSeconds: Long? = null,
    @SerializedName("routePolyline") val routePolyline: List<RoutePoint>? = null,
)

/** Body di POST /api/v1/stories. */
data class CreateStoryRequest(
    @SerializedName("type") val type: String,
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("activityId") val activityId: String? = null,
    @SerializedName("caption") val caption: String? = null,
    @SerializedName("media") val media: List<StoryMedia> = emptyList(),
    @SerializedName("overlay") val overlay: StoryOverlay? = null,
)

/**
 * Dati sorgente passati al composer storie (via nav holder, serializzati con
 * Gson come per gli altri pending*). Contengono il riferimento + lo snapshot
 * overlay già pronto dall'origine (dettaglio attività / sessione).
 */
data class StoryComposerArgs(
    val type: String, // "planned_session" | "activity"
    val sessionId: String? = null,
    val activityId: String? = null,
    val overlay: StoryOverlay? = null,
)
