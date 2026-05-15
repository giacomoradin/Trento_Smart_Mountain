package it.trentosmartmountain.app.data.remote.dto

data class CreateSessionRequest(
    val routeDetails: SessionRouteDetails,
    val meetingDate: String? = null,
    val meetingTime: String? = null,
    val meetingLocation: String? = null,
    val maxParticipants: Int? = null,
    val minExperienceLevel: String? = null,
    val gpxFileName: String? = null,
    val gpxStats: GpxStats? = null,
)

data class SessionRouteDetails(
    val name: String,
    val difficultyLevel: String = "E",
    val elevationGain: Int? = null,
    val startPoint: GeoPoint? = null,
    val endPoint: GeoPoint? = null,
)

data class GeoPoint(
    val type: String = "Point",
    val coordinates: List<Double>,
)

data class GpxStats(
    val distanceKm: Double,
    val elevationGainM: Int,
    val trackPoints: Int,
    /** Profilo altimetrico campionato per il rendering del chart (max 50 punti, in metri). */
    val elevationProfile: List<Double>? = null,
    /** Punti stimati col modello CAI in fase di pianificazione (μ = 1.0). */
    val estimatedPoints: Int? = null,
)

data class SessionCreatedResponse(
    val _id: String,
    val inviteCode: String,
    val status: String,
    val createdAt: String?,
)
