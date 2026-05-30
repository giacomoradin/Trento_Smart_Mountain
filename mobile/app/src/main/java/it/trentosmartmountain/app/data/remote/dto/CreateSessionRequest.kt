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
    /**
     * Codice del sentiero SAT selezionato dal DB (modalità "Scegli percorso sulla mappa").
     * Null in modalità GPX. Necessario alla checklist dinamica (US-7) per risalire al Sentiero.
     */
    val sentieroCode: String? = null,
    /**
     * Tracciato pianificato (origine + polyline campionata) per il controllo distanza
     * dal percorso durante il tracking. source = "GPX" (file importato) | "SAT" (DB sentieri).
     */
    val plannedRoute: PlannedRoute? = null,
)

/** Tracciato pianificato inviato in creazione sessione. */
data class PlannedRoute(
    val source: String,
    val polylinePoints: List<PlannedRoutePoint> = emptyList(),
    /** Bounding box del tracciato (opzionale). */
    val bbox: PlannedRouteBbox? = null,
)

/** Punto della polyline pianificata (lat/lon). */
data class PlannedRoutePoint(
    val lat: Double,
    val lon: Double,
)

/** Bounding box del tracciato pianificato (formato oggetto atteso dal backend). */
data class PlannedRouteBbox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
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
    /**
     * Durata effettiva estratta dai tag <time> dei trkpt del GPX (in secondi).
     * Null se il GPX non contiene timestamp (file generati senza traccia temporale).
     * Se presente, sostituisce la stima CAI nella label "DURATA" delle attività.
     */
    val gpxDurationSec: Long? = null,
)

/** Risposta sintetica dopo creazione sessione (id, invite code, stato iniziale). */
data class SessionCreatedResponse(
    val _id: String,
    val inviteCode: String,
    val status: String,
    val createdAt: String?,
)
