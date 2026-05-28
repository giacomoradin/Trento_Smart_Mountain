package it.trentosmartmountain.app.data.remote.dto

/** Corpo della richiesta POST /api/v1/sessions/:id/live-location */
data class PostLiveLocationRequest(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float? = null,
    val altitudeM: Double? = null,
    val trackingStatus: String? = null,
    val timestampMs: Long? = null,
)

/** Un singolo utente nel feed live */
data class LiveUserDto(
    val id: String,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
    val role: String,
    /** Visibile solo al capogruppo (M, F, X, N). */
    val sex: String? = null,
)

/** La posizione di un utente nel feed live */
data class LiveLocationDto(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float? = null,
    val altitudeM: Double? = null,
    val trackingStatus: String? = null,
    val updatedAt: String? = null,
)

/** Un elemento del feed: utente + sua posizione */
data class LiveLocationItemDto(
    val user: LiveUserDto,
    val location: LiveLocationDto,
)

/** Risposta completa di GET /api/v1/sessions/:id/live-locations */
data class LiveLocationsResponse(
    val data: List<LiveLocationItemDto>,
    val message: String? = null,
)

/** Marker SOS da coordinate emergenza (se non presente nel feed live). */
data class SosMapMarkerDto(
    val userId: String,
    val lat: Double,
    val lon: Double,
    val displayName: String?,
    val avatarUrl: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
)
