package it.trentosmartmountain.app.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.CreateSessionRequest
import it.trentosmartmountain.app.data.remote.dto.GeoPoint
import it.trentosmartmountain.app.data.remote.dto.GpxStats
import it.trentosmartmountain.app.data.remote.dto.SessionRouteDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SessionPlanViewModel : ViewModel() {

    data class GpxParseResult(
        val fileName: String,
        val distanceKm: Double,
        val elevationGainM: Int,
        val trackPoints: Int,
        val firstPoint: Pair<Double, Double>?,
        val lastPoint: Pair<Double, Double>?,
    )

    data class UiState(
        val sessionName: String = "",
        val meetingDate: String = "",
        val meetingTime: String = "",
        val maxParticipants: Int = 8,
        val difficultyLevel: String = "E",
        val gpxData: GpxParseResult? = null,
        val gpxParseError: String? = null,
        val previewCode: String = generatePreviewCode(),
        val isLoading: Boolean = false,
        val generalError: String? = null,
        val sessionCreated: Boolean = false,
        val createdInviteCode: String? = null,
        val showQrPreview: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onSessionNameChange(v: String) = _uiState.update { it.copy(sessionName = v) }
    fun onMeetingDateChange(v: String) = _uiState.update { it.copy(meetingDate = v) }
    fun onMeetingTimeChange(v: String) = _uiState.update { it.copy(meetingTime = v) }
    fun onMaxParticipantsChange(v: Int) = _uiState.update { it.copy(maxParticipants = v.coerceIn(1, 50)) }
    fun onDifficultyChange(v: String) = _uiState.update { it.copy(difficultyLevel = v) }
    fun onRemoveGpx() = _uiState.update { it.copy(gpxData = null, gpxParseError = null) }
    fun onToggleQrPreview() = _uiState.update { it.copy(showQrPreview = !it.showQrPreview) }
    fun resetAfterCreation() = _uiState.update { UiState(previewCode = generatePreviewCode()) }

    fun onGpxFileSelected(contentResolver: ContentResolver, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(gpxParseError = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val stream = contentResolver.openInputStream(uri)
                        ?: throw IOException("Impossibile aprire il file.")
                    stream.use { parseGpx(it, fileName) }
                }
                _uiState.update { state ->
                    state.copy(
                        gpxData = result,
                        sessionName = if (state.sessionName.isBlank())
                            fileName.removeSuffix(".gpx").replace("-", " ").replace("_", " ")
                        else state.sessionName,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(gpxParseError = "Errore parsing GPX: ${e.message}") }
            }
        }
    }

    fun onCreateSession() {
        val state = _uiState.value
        if (state.sessionName.isBlank()) {
            _uiState.update { it.copy(generalError = "Inserisci un nome per l'escursione.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            try {
                val gpx = state.gpxData
                val request = CreateSessionRequest(
                    routeDetails = SessionRouteDetails(
                        name = state.sessionName,
                        difficultyLevel = state.difficultyLevel,
                        elevationGain = gpx?.elevationGainM,
                        startPoint = gpx?.firstPoint?.let { GeoPoint(coordinates = listOf(it.second, it.first)) },
                        endPoint = gpx?.lastPoint?.let { GeoPoint(coordinates = listOf(it.second, it.first)) },
                    ),
                    meetingDate = state.meetingDate.ifBlank { null },
                    meetingTime = state.meetingTime.ifBlank { null },
                    maxParticipants = state.maxParticipants,
                    minExperienceLevel = state.difficultyLevel,
                    gpxFileName = gpx?.fileName,
                    gpxStats = gpx?.let {
                        GpxStats(
                            distanceKm = it.distanceKm,
                            elevationGainM = it.elevationGainM,
                            trackPoints = it.trackPoints,
                        )
                    },
                )
                val response = TsmApiClient.service().createSession(request)
                if (response.isSuccessful) {
                    val code = response.body()?.inviteCode ?: state.previewCode
                    _uiState.update { it.copy(isLoading = false, sessionCreated = true, createdInviteCode = code) }
                } else {
                    val error = when (response.code()) {
                        409 -> "Sei già in una sessione attiva."
                        else -> "Errore server (${response.code()})."
                    }
                    _uiState.update { it.copy(isLoading = false, generalError = error) }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, generalError = "Nessuna connessione al server.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, generalError = e.message) }
            }
        }
    }

    // --- GPX Parser ---

    private fun parseGpx(stream: InputStream, fileName: String): GpxParseResult {
        data class TrackPoint(val lat: Double, val lon: Double, val ele: Double?)

        val points = mutableListOf<TrackPoint>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, null)

        var lat = 0.0
        var lon = 0.0
        var ele: Double? = null
        var inTrkpt = false
        var inEle = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "trkpt" -> {
                        lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                        lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                        ele = null
                        inTrkpt = true
                    }
                    "ele" -> if (inTrkpt) inEle = true
                }
                XmlPullParser.TEXT -> if (inEle) ele = parser.text.toDoubleOrNull()
                XmlPullParser.END_TAG -> when (parser.name) {
                    "trkpt" -> {
                        points.add(TrackPoint(lat, lon, ele))
                        inTrkpt = false
                    }
                    "ele" -> inEle = false
                }
            }
            eventType = parser.next()
        }

        if (points.isEmpty()) throw IllegalStateException("Nessun track point trovato nel file GPX.")

        var totalDistanceKm = 0.0
        var elevationGainM = 0
        for (i in 1 until points.size) {
            totalDistanceKm += haversineKm(
                points[i - 1].lat, points[i - 1].lon,
                points[i].lat, points[i].lon,
            )
            val prevEle = points[i - 1].ele
            val currEle = points[i].ele
            if (prevEle != null && currEle != null && currEle > prevEle) {
                elevationGainM += (currEle - prevEle).toInt()
            }
        }

        return GpxParseResult(
            fileName = fileName,
            distanceKm = (totalDistanceKm * 10).toLong() / 10.0,
            elevationGainM = elevationGainM,
            trackPoints = points.size,
            firstPoint = points.first().let { Pair(it.lat, it.lon) },
            lastPoint = points.last().let { Pair(it.lat, it.lon) },
        )
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        fun generatePreviewCode(): String {
            val chars = "0123456789ABCDEF"
            return "TSM-" + (1..4).map { chars.random() }.joinToString("")
        }
    }
}

/*
 * SISTEMA ANALISI TRACCIATO - DA IMPLEMENTARE
 *
 * Obiettivo: classificare automaticamente un percorso GPX e generare la "Hike Packet"
 * con dati offline (tile mappa, meteo fascia oraria, equipment consigliato).
 *
 * Algoritmo di scoring (RF1, RF2, RF3):
 *
 * 1. INPUT: punti GPS (lat/lon/ele) + metadata OpenData Trentino/OSM
 *
 * 2. CALCOLO METRICHE BASE (già implementato in parseGpx):
 *    - Distanza totale (Haversine)
 *    - Dislivello positivo cumulato
 *    - Numero track point
 *
 * 3. CLASSIFICAZIONE SENTIERO (da implementare):
 *    - Query OSM Overpass API: trkpt → nearest way → tags[sac_scale / trail_visibility]
 *    - Mapping sac_scale → livello CAI: hiking=T, mountain_hiking=E, demanding_mountain=EE, alpine=EEA
 *    - Se nessun tag OSM → stima da dislivello/km:
 *        < 300m/10km → T
 *        300-600m/10km → E
 *        600-900m/10km → EE
 *        > 900m/10km → EEA
 *
 * 4. SCORING PARTECIPANTI (RF2, da implementare in backend):
 *    - Score sentiero S = f(distanza, dislivello, livello_CAI, esposizione_max)
 *    - Confronto con minExperienceLevel scelto dal Capogruppo
 *    - Endpoint: POST /api/v1/sessions/{id}/compatibility-check { userId }
 *
 * 5. DOWNLOAD HIKE PACKET (offline-first, da implementare - §4 setup_mobile.md):
 *    - Map tiles OSM nel bounding box del percorso (padding 1km) → Room/FileSystem
 *    - Meteo fascia oraria da MeteoTrentino (GET /meteo?codice=...) → cache Room
 *    - Equipment list da backend in base a score + meteo → Room
 *    - WorkManager job: scarica packet prima di ATTIVA, aggiorna se rete disponibile
 */
