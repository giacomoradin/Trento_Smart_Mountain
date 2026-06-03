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

/** Trasformazione sticker salvata per storie video (overlay dinamico nel viewer). */
data class StoryStickerTransformDto(
    @SerializedName("offsetX") val offsetX: Float = 0f,
    @SerializedName("offsetY") val offsetY: Float = 0f,
    @SerializedName("scale") val scale: Float = 1f,
    @SerializedName("rotationDeg") val rotationDeg: Float = 0f,
)

/** Decorazioni editor (traccia/testo) per playback video; le immagini sono composte lato client. */
data class StoryEditorDecor(
    /** trace | map_widget | map_scene */
    @SerializedName("routeOverlayKind") val routeOverlayKind: String? = null,
    @SerializedName("routeColor") val routeColor: String? = null,
    @SerializedName("routeTransform") val routeTransform: StoryStickerTransformDto? = null,
    @SerializedName("mapWidgetTransform") val mapWidgetTransform: StoryStickerTransformDto? = null,
    @SerializedName("floatingText") val floatingText: String? = null,
    @SerializedName("textColor") val textColor: String? = null,
    @SerializedName("textTransform") val textTransform: StoryStickerTransformDto? = null,
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
    @SerializedName("editorDecor") val editorDecor: StoryEditorDecor? = null,
)

/**
 * Contesto di apertura dello Story Viewer: coda di autori con storie (ordine
 * della social-row) e indice di partenza, per navigazione stile Instagram.
 */
data class StoryViewerLaunchContext(
    val userIds: List<String>,
    val startIndex: Int = 0,
) {
    val startUserId: String? get() = userIds.getOrNull(startIndex.coerceIn(userIds.indices))
}

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
