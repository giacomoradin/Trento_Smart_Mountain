package it.trentosmartmountain.app.data.remote.dto

/**
 * Sessione escursionistica restituita dalle API `GET/POST /api/v1/sessions`.
 *
 * DTO di deserializzazione Gson: i campi popolati dipendono dall'endpoint e dal livello
 * di populate del backend (es. `participants.userId` come oggetto utente embedded).
 */
data class SessionResponse(
    val _id: String,
    val inviteCode: String,
    val status: String,
    val routeDetails: SessionRouteDetailsResponse?,
    val meetingDate: String?,
    val meetingTime: String?,
    val meetingLocation: String?,
    val maxParticipants: Int?,
    val minExperienceLevel: String?,
    val gpxStats: GpxStatsResponse?,
    val gpxFileName: String?,
    val creatorId: SessionUserInfo?,
    val participants: List<SessionParticipant>?,
    val startTime: String?,
    val endTime: String?,
    val createdAt: String?,
)

/** Dettagli percorso inclusi nella sessione (nome, difficoltà, punti GeoJSON). */
data class SessionRouteDetailsResponse(
    val name: String,
    val difficultyLevel: String?,
    val elevationGain: Int?,
    val startPoint: GeoPoint? = null,
    val endPoint: GeoPoint? = null,
)

/** Statistiche GPX calcolate lato client o server e persistite sulla sessione. */
data class GpxStatsResponse(
    val distanceKm: Double?,
    val elevationGainM: Int?,
    val trackPoints: Int?,
    /** Profilo altimetrico campionato (max 50 punti) usato per il rendering del chart. */
    val elevationProfile: List<Double>? = null,
    /** Punti stimati col modello CAI. */
    val estimatedPoints: Int? = null,
)

/** Utente embedded (creatore o partecipante) nella risposta sessione. */
data class SessionUserInfo(
    val _id: String,
    val username: String,
    val email: String?,
)

/** Partecipante a una sessione con ruolo e timestamp di join. */
data class SessionParticipant(
    val userId: SessionUserInfo?,
    val role: String?,
    val joinedAt: String?,
)
