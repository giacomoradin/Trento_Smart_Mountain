package it.trentosmartmountain.app.data.remote.dto

/**
 * Body per `POST /api/v1/activities` (creazione attività libera).
 *
 * Le attività libere sono escursioni personali (no sessione di gruppo). Sul server
 * vivono nella collection [Activity] separata da HikeSession. Tutte le statistiche
 * sono SEMPRE reali — non c'è una fase "pianificata" come per le sessioni.
 */
data class CreateActivityRequest(
    val name: String,
    val activityType: String = "hiking",
    val startTimeMs: Long,
    val endTimeMs: Long,
    val actualStats: ActualStats,
    val difficultyLevel: String? = null,
    val elevationProfile: List<Double>? = null,
    /** Traccia GPS campionata (max ~80 punti) per la route signature del feed. */
    val routePolyline: List<RoutePoint>? = null,
)

/** Risposta dopo creazione/lettura di un'attività libera. */
data class ActivityResponse(
    val _id: String,
    val userId: String,
    val name: String,
    val activityType: String,
    val difficultyLevel: String?,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val completedAt: String?,
    val actualStats: ActualStatsResponse?,
    val elevationProfile: List<Double>?,
)
