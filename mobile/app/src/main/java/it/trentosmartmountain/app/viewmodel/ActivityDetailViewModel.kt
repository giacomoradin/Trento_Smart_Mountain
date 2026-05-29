package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.estimation.HikeEstimation
import it.trentosmartmountain.app.data.local.db.CompletedActivityEntity
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class TimelineEventType { DEPARTURE, SPLIT, AUTOPAUSE, ARRIVAL }

data class TimelineEvent(
    val type: TimelineEventType,
    val label: String,
    val timeLabel: String?,
    val subtitle: String?,
    val distanceKm: Double?,
)

class ActivityDetailViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val activityId: String = "",
        val local: CompletedActivityEntity? = null,
        val session: SessionResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        /** Punti track [[lat, lon, alt], ...] per il rendering mappa OSMdroid. */
        val trackPoints: List<Triple<Double, Double, Double>> = emptyList(),
        /** Profilo altimetrico normalizzato (0.0–1.0) per il grafico Canvas. */
        val elevationProfile: List<Double> = emptyList(),
        /** Profilo altimetrico in metri assoluti per asse Y. */
        val elevationAbsolute: List<Double> = emptyList(),
        val elevationMinM: Double = 0.0,
        val elevationMaxM: Double = 0.0,
        val timeline: List<TimelineEvent> = emptyList(),
        // Metriche calcolate
        val distanceKm: Double = 0.0,
        val movingSeconds: Long = 0L,
        val totalSeconds: Long = 0L,
        val elevationGainM: Int = 0,
        val points: Int? = null,
        val estimatedCalories: Int? = null,
        val avgSpeedKmh: Double = 0.0,
        val difficultyLevel: String? = null,
        val name: String = "",
        val activityType: String = "hiking",
        val startTimeMs: Long = 0L,
        val endTimeMs: Long = 0L,
        val caiTimeHours: Double = 0.0,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val dao get() = (getApplication() as TsmApplication).database.completedActivityDao()
    private val gson = Gson()

    /** Carica un'attività per [id] (id Room locale) o per [sessionId] (ID backend). */
    fun load(id: String, sessionId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, activityId = id) }
            try {
                // 1. Cerca in Room
                val local = dao.getById(id) ?: sessionId?.let { dao.getBySessionId(it) }

                // 2. Carica sessione backend se disponibile
                val session = if (sessionId != null || local?.sessionId != null) {
                    val sid = sessionId ?: local!!.sessionId!!
                    runCatching {
                        TsmApiClient.service().getSessionById(sid)
                            .takeIf { it.isSuccessful }?.body()
                    }.getOrNull()
                } else null

                // 3. Decodifica track points dal JSON locale
                val trackPoints = local?.let { parseTrack(it.trackLatLng) } ?: emptyList()

                // 4. Costruisce profilo altimetrico
                val altitudes = if (trackPoints.isNotEmpty()) {
                    trackPoints.map { it.third }
                } else {
                    session?.gpxStats?.elevationProfile ?: emptyList()
                }
                val minAlt = altitudes.minOrNull() ?: 0.0
                val maxAlt = altitudes.maxOrNull() ?: 0.0
                val range = (maxAlt - minAlt).coerceAtLeast(1.0)
                val normalized = altitudes.map { (it - minAlt) / range }

                // 5. Metriche
                val distKm = local?.let { it.distanceMeters / 1000.0 }
                    ?: session?.gpxStats?.distanceKm ?: 0.0
                val elevM = local?.elevationGainMeters ?: session?.gpxStats?.elevationGainM ?: 0
                val movingSec = local?.movingSeconds ?: HikeEstimation.caiTimeHours(distKm, elevM).let { (it * 3600).toLong() }
                val totalSec = local?.totalSeconds ?: movingSec
                val caiH = HikeEstimation.caiTimeHours(distKm, elevM)
                val actualH = movingSec / 3600.0
                val pts = local?.points ?: HikeEstimation.finalPoints(distKm, elevM, actualH)
                val cals = local?.estimatedCalories ?: (70 * distKm * 0.85).roundToInt()
                val avgSpd = if (movingSec > 0) distKm / (movingSec / 3600.0) else 0.0

                // 6. Timeline
                val timeline = buildTimeline(
                    distanceKm = distKm,
                    movingSeconds = movingSec,
                    startMs = local?.startTimeMs ?: 0L,
                    elevationGainM = elevM,
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        local = local,
                        session = session,
                        trackPoints = trackPoints,
                        elevationProfile = normalized,
                        elevationAbsolute = altitudes,
                        elevationMinM = minAlt,
                        elevationMaxM = maxAlt,
                        timeline = timeline,
                        distanceKm = distKm,
                        movingSeconds = movingSec,
                        totalSeconds = totalSec,
                        elevationGainM = elevM,
                        points = pts,
                        estimatedCalories = cals,
                        avgSpeedKmh = avgSpd,
                        difficultyLevel = local?.difficultyLevel ?: session?.routeDetails?.difficultyLevel,
                        name = local?.name ?: session?.routeDetails?.name ?: "Escursione",
                        activityType = local?.activityType ?: "hiking",
                        startTimeMs = local?.startTimeMs ?: 0L,
                        endTimeMs = local?.endTimeMs ?: System.currentTimeMillis(),
                        caiTimeHours = caiH,
                    )
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, error = "Nessuna connessione.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Elimina l'attività dalla lista "Le mie attività".
     *
     * - Attività libera sincronizzata (`sessionId == null`, `remoteId != null`):
     *   viene cancellata anche dal backend (`DELETE /activities/:id`).
     * - Sessione di gruppo COMPLETED: NON è cancellabile sul backend (appartiene
     *   anche agli altri partecipanti), quindi la nascondiamo localmente.
     *
     * In entrambi i casi marchiamo la riga come `hidden` (tombstone) invece di
     * rimuoverla fisicamente: così `syncCompletedSessionsToRoom` /
     * `syncFreeActivitiesToRoom` la vedono ancora come esistente e NON la
     * re-importano al riavvio dell'app.
     */
    fun deleteActivity(onDeleted: () -> Unit) {
        val id = _uiState.value.activityId
        viewModelScope.launch {
            val entity = dao.getById(id)
            if (entity?.sessionId == null && entity?.remoteId != null) {
                runCatching { TsmApiClient.service().deleteActivity(entity.remoteId) }
            }
            // Tombstone: nasconde la riga senza eliminarla (evita re-import dal sync).
            dao.markHidden(id)
            entity?.sessionId?.let { dao.markHiddenBySessionId(it) }
            onDeleted()
        }
    }

    // ── Parsing e costruzione ──

    private fun parseTrack(json: String): List<Triple<Double, Double, Double>> {
        return try {
            val type = object : TypeToken<List<List<Double>>>() {}.type
            val raw: List<List<Double>> = gson.fromJson(json, type)
            raw.mapNotNull { pts ->
                if (pts.size >= 2) Triple(pts[0], pts[1], pts.getOrElse(2) { 0.0 }) else null
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun buildTimeline(
        distanceKm: Double,
        movingSeconds: Long,
        startMs: Long,
        elevationGainM: Int,
    ): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        val avgSpeedKmh = if (movingSeconds > 0) distanceKm / (movingSeconds / 3600.0) else 4.0
        val fmt = SimpleDateFormat("HH:mm", Locale.ITALIAN)
        val start = if (startMs > 0) startMs else System.currentTimeMillis() - movingSeconds * 1000

        // Partenza
        events.add(TimelineEvent(
            type = TimelineEventType.DEPARTURE,
            label = "Partenza",
            timeLabel = fmt.format(Date(start)),
            subtitle = _uiState.value.session?.meetingLocation ?: "—",
            distanceKm = 0.0,
        ))

        // Split ogni 5km
        var km = 5.0
        while (km < distanceKm) {
            val elapsedSec = (km / avgSpeedKmh * 3600).toLong()
            val ts = start + elapsedSec * 1000
            val pace = if (avgSpeedKmh > 0) 60.0 / avgSpeedKmh else 0.0
            events.add(TimelineEvent(
                type = TimelineEventType.SPLIT,
                label = "Split · ${km.toInt()} km",
                timeLabel = fmt.format(Date(ts)),
                subtitle = "passo ${formatPace(pace)} · ${String.format("%.1f", avgSpeedKmh)} km/h",
                distanceKm = km,
            ))
            km += 5.0
        }

        // Arrivo
        val endMs = start + movingSeconds * 1000
        events.add(TimelineEvent(
            type = TimelineEventType.ARRIVAL,
            label = "Arrivo",
            timeLabel = fmt.format(Date(endMs)),
            subtitle = "Totale ${HikeEstimation.formatHours(movingSeconds / 3600.0)} · ${String.format("%.1f", distanceKm)} km",
            distanceKm = distanceKm,
        ))

        return events.sortedBy { it.distanceKm ?: 0.0 }
    }

    private fun formatPace(minutesPerKm: Double): String {
        if (minutesPerKm <= 0) return "—"
        val m = minutesPerKm.toInt()
        val s = ((minutesPerKm - m) * 60).roundToInt()
        return "$m:${s.toString().padStart(2, '0')}/km"
    }
}
