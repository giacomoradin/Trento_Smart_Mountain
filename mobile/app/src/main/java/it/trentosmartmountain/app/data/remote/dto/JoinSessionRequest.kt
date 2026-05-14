package it.trentosmartmountain.app.data.remote.dto

data class JoinSessionRequest(val inviteCode: String)

data class UpdateSessionRequest(
    val routeDetails: UpdateRouteDetails? = null,
    val meetingDate: String? = null,
    val meetingTime: String? = null,
    val meetingLocation: String? = null,
    val maxParticipants: Int? = null,
    val minExperienceLevel: String? = null,
)

data class UpdateRouteDetails(
    val name: String? = null,
    val difficultyLevel: String? = null,
)
