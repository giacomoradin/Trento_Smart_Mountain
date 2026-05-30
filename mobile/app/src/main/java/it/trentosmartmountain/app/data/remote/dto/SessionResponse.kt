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
    /**
     * Statistiche reali registrate dal client al termine di una sessione COMPLETED.
     * Se presente, ha priorità sulle stime CAI/Naismith per tutte le metriche di durata,
     * distanza, dislivello e punteggio visualizzate sulle attività in cloud.
     */
    val actualStats: ActualStatsResponse? = null,
    val creatorId: SessionUserInfo?,
    val participants: List<SessionParticipant>?,
    val startTime: String?,
    val endTime: String?,
    val createdAt: String?,
    /**
     * Codice del sentiero SAT scelto in pianificazione (modalità DB). Null in modalità GPX.
     * Usato dalla SessionDetail per invocare la checklist dinamica con il codice corretto (US-7).
     */
    val sentieroCode: String? = null,
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
    /** Durata effettiva del GPX (differenza primo-ultimo timestamp dei trkpt). */
    val gpxDurationSec: Long? = null,
)

/** Statistiche reali registrate al termine di una sessione tracciata. */
data class ActualStatsResponse(
    val movingSeconds: Long? = null,
    val totalSeconds: Long? = null,
    val distanceMeters: Double? = null,
    val elevationGainM: Int? = null,
    val finalPoints: Int? = null,
    val estimatedCalories: Int? = null,
    val currentAltitudeM: Int? = null,
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
