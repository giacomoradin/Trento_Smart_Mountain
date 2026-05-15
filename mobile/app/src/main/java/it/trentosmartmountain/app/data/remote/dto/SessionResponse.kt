package it.trentosmartmountain.app.data.remote.dto

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

data class SessionRouteDetailsResponse(
    val name: String,
    val difficultyLevel: String?,
    val elevationGain: Int?,
    val startPoint: GeoPoint? = null,
    val endPoint: GeoPoint? = null,
)

data class GpxStatsResponse(
    val distanceKm: Double?,
    val elevationGainM: Int?,
    val trackPoints: Int?,
    /** Profilo altimetrico campionato (max 50 punti) usato per il rendering del chart. */
    val elevationProfile: List<Double>? = null,
    /** Punti stimati col modello CAI. */
    val estimatedPoints: Int? = null,
)

data class SessionUserInfo(
    val _id: String,
    val username: String,
    val email: String?,
)

data class SessionParticipant(
    val userId: SessionUserInfo?,
    val role: String?,
    val joinedAt: String?,
)
