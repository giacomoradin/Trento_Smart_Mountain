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
)

data class SessionCreatedResponse(
    val _id: String,
    val inviteCode: String,
    val status: String,
    val createdAt: String?,
)
