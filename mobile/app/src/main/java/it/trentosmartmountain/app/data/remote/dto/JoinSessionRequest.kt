package it.trentosmartmountain.app.data.remote.dto

/** Body per `POST /api/v1/sessions/join` (ingresso tramite codice invito). */
data class JoinSessionRequest(val inviteCode: String)

/** Body per `PATCH /api/v1/sessions/{id}/status` (es. PLANNED → ACTIVE). */
data class UpdateSessionStatusRequest(val status: String)

/** Body parziale per `PATCH /api/v1/sessions/{id}` (modifica metadati sessione). */
data class UpdateSessionRequest(
    val routeDetails: UpdateRouteDetails? = null,
    val meetingDate: String? = null,
    val meetingTime: String? = null,
    val meetingLocation: String? = null,
    val maxParticipants: Int? = null,
    val minExperienceLevel: String? = null,
)

/** Sottoinsieme modificabile dei dettagli percorso in PATCH sessione. */
data class UpdateRouteDetails(
    val name: String? = null,
    val difficultyLevel: String? = null,
)
