package it.trentosmartmountain.app.data.remote.dto

/** Body per `POST /api/v1/sessions` (creazione sessione da capogruppo). */
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

/** Percorso e metadati inviati in fase di pianificazione. */
data class SessionRouteDetails(
    val name: String,
    val difficultyLevel: String = "E",
    val elevationGain: Int? = null,
    val startPoint: GeoPoint? = null,
    val endPoint: GeoPoint? = null,
)

/** Punto GeoJSON (`type: Point`, `coordinates: [lon, lat]`). */
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

/** Risposta sintetica dopo creazione sessione (id, invite code, stato iniziale). */
data class SessionCreatedResponse(
    val _id: String,
    val inviteCode: String,
    val status: String,
    val createdAt: String?,
)
