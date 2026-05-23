package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.location.HikeTrackingEngine
import it.trentosmartmountain.app.data.location.LocationSnapshot
import it.trentosmartmountain.app.data.location.StationaryDetector
import it.trentosmartmountain.app.data.location.TrackingLocationBus
import it.trentosmartmountain.app.data.location.TrackingStatus
import it.trentosmartmountain.app.data.location.UserLocationTracker
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.ActualStats
import it.trentosmartmountain.app.data.remote.dto.CompleteSessionRequest
import it.trentosmartmountain.app.data.remote.dto.CreateActivityRequest
import it.trentosmartmountain.app.data.remote.dto.UpdateSessionStatusRequest
import it.trentosmartmountain.app.data.sync.SyncManager
import it.trentosmartmountain.app.data.session.SessionStartCoordinator
import it.trentosmartmountain.app.service.ForegroundTrackingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * Logica della tab **Registra**: permessi GPS, tracking escursione, metriche e traccia su mappa OSMdroid.
 *
 * Coordina [HikeTrackingEngine], servizio in foreground e [SessionStartCoordinator]
 * quando l'utente avvia una sessione dal dettaglio escursione.
 */
class RegistraViewModel(application: Application) : AndroidViewModel(application) {

  /** Stato osservato da [it.trentosmartmountain.app.ui.screens.registra.RegistraScreen]. */
  data class UiState(
    val hasLocationPermission: Boolean = false,
    val locationPermissionDenied: Boolean = false,
    val userLocation: LocationSnapshot? = null,
    val gpsSignalLevel: Int = 0,
    val gpsAccuracyLabel: String? = null,
    val centerOnUserTick: Int = 0,
    val trackingStatus: TrackingStatus = TrackingStatus.IDLE,
    val isAutoPaused: Boolean = false,
    val elapsedSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val elevationGainMeters: Int = 0,
    val currentAltitudeMeters: Int? = null,
    val trackGeoPoints: List<GeoPoint> = emptyList(),
    val showStopConfirm: Boolean = false,
    /** Sessione attiva collegata al tracking (null = sessione libera). */
    val activeSessionId: String? = null,
<<<<<<< HEAD
=======
    /** Epoch ms di inizio tracking (per salvare start time su Room). */
    val trackStartTimeMs: Long = 0L,
    /** Nome bozza dell'attività che l'utente può modificare nel dialog di salvataggio. */
    val activityNameDraft: String = "",
    /** true dopo che il salvataggio in Room è completato → mostra feedback all'utente. */
    val activitySaved: Boolean = false,
    /**
     * Mostrato quando l'utente termina un'attività libera (no sessionId) con
     * distanza < 50m: chiede conferma esplicita ("vuoi salvarla comunque?")
     * per evitare avvii accidentali silenziosi. Per le sessioni con sessionId
     * NON viene mostrato (il backend è già stato avvisato della partenza).
     */
    val shortActivityConfirm: Boolean = false,
>>>>>>> 7c170be742c0ca0f16c4c6df6f5c273d643d4a7a
  )

  private val app = getApplication<Application>()
  private val locationTracker = UserLocationTracker(app)
  private val trackingEngine = HikeTrackingEngine()
  private val stationaryDetector = StationaryDetector(app)

  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  private var timerJob: Job? = null
  private var stillSinceMs: Long? = null
  private var lastSnapshot: LocationSnapshot? = null

  init {
    viewModelScope.launch {
      locationTracker.location.collect { snapshot ->
        if (_uiState.value.trackingStatus == TrackingStatus.IDLE) {
          snapshot?.let { applyLocation(it) }
        }
      }
    }
    viewModelScope.launch {
      TrackingLocationBus.locations.collect { snapshot ->
        if (_uiState.value.trackingStatus != TrackingStatus.IDLE) {
          applyLocation(snapshot)
        }
      }
    }
    // Auto-start tracking quando SessionDetail.AVVIA emette un sessionId.
    // Lo consumiamo subito per evitare retrigger su recomposition/rotation.
    viewModelScope.launch {
      SessionStartCoordinator.pendingSessionStart.collect { sessionId ->
        if (sessionId != null && _uiState.value.trackingStatus == TrackingStatus.IDLE) {
          autoStartFromSession(sessionId)
          SessionStartCoordinator.consume()
        }
      }
    }
  }

  /**
   * Avvia il tracking collegato a una sessione esistente:
   *   1. Marca lo stato locale con l'activeSessionId
   *   2. PATCH /api/v1/sessions/{id}/status = ACTIVE sul backend
   *      (così la sessione esce da PLANNED e startTime viene popolato)
   *   3. Avvia il tracking GPS standard
   *
   * Se l'utente non ha ancora concesso il permesso GPS, lo richiediamo lazily
   * dal lato Compose; quando arriverà il grant, il tracking partirà.
   */
  private fun autoStartFromSession(sessionId: String) {
    _uiState.update { it.copy(activeSessionId = sessionId) }
    viewModelScope.launch {
      runCatching {
        TsmApiClient.service().updateSessionStatus(
          sessionId,
          UpdateSessionStatusRequest(status = "ACTIVE"),
        )
      }
    }
    if (_uiState.value.hasLocationPermission) {
      startTracking()
    }
  }

  fun onLocationPermissionResult(granted: Boolean) {
    _uiState.update {
      it.copy(
        hasLocationPermission = granted,
        locationPermissionDenied = !granted,
      )
    }
    if (granted && _uiState.value.trackingStatus == TrackingStatus.IDLE) {
      locationTracker.start()
    } else if (!granted) {
      locationTracker.stop()
    }
  }

  fun startTracking() {
    if (!_uiState.value.hasLocationPermission) return
    trackingEngine.start()
    stillSinceMs = null
    locationTracker.stop()
    ForegroundTrackingService.start(app)
    stationaryDetector.start()
    startTimer()
    _uiState.update {
      it.copy(
        trackingStatus = TrackingStatus.RECORDING,
        isAutoPaused = false,
        elapsedSeconds = 0,
        distanceMeters = 0.0,
        elevationGainMeters = 0,
        trackGeoPoints = emptyList(),
      )
    }
    lastSnapshot?.let { applyLocation(it) }
  }

  fun togglePause() {
    when (_uiState.value.trackingStatus) {
      TrackingStatus.RECORDING -> {
        trackingEngine.pause(manual = true)
        stillSinceMs = null
        _uiState.update {
          it.copy(trackingStatus = TrackingStatus.PAUSED, isAutoPaused = false)
        }
      }
      TrackingStatus.PAUSED -> resumeTracking()
      TrackingStatus.IDLE -> Unit
    }
  }

  fun requestStopTracking() {
    _uiState.update { it.copy(showStopConfirm = true) }
  }

  fun dismissStopConfirm() {
    _uiState.update { it.copy(showStopConfirm = false) }
  }

<<<<<<< HEAD
  fun confirmStopTracking() {
=======
  /**
   * Scarta il tracciato senza salvare e termina il tracking.
   * Usato dal bottone "Scarta" nel dialog di salvataggio.
   */
  fun discardTracking() {
    stopHardware()
    _uiState.update {
      it.copy(
        trackingStatus = TrackingStatus.IDLE,
        isAutoPaused = false,
        showStopConfirm = false,
        shortActivityConfirm = false,
        trackGeoPoints = emptyList(),
        elapsedSeconds = 0,
        distanceMeters = 0.0,
        elevationGainMeters = 0,
        activeSessionId = null,
        trackStartTimeMs = 0L,
        activityNameDraft = "",
      )
    }
  }

  private fun stopHardware() {
>>>>>>> 7c170be742c0ca0f16c4c6df6f5c273d643d4a7a
    trackingEngine.stop()
    stationaryDetector.stop()
    ForegroundTrackingService.stop(app)
    timerJob?.cancel()
    stillSinceMs = null
    if (_uiState.value.hasLocationPermission) {
      locationTracker.start()
    }
<<<<<<< HEAD
    // Se il tracking era collegato a una sessione, segnala COMPLETED sul backend.
    // (in futuro: salvataggio activity in Home → "LE MIE ATTIVITÀ" come da requisito utente)
    val sessionId = _uiState.value.activeSessionId
    if (sessionId != null) {
      viewModelScope.launch {
        runCatching {
          TsmApiClient.service().updateSessionStatus(
            sessionId,
            UpdateSessionStatusRequest(status = "COMPLETED"),
          )
        }
      }
    }
=======
  }

  fun confirmShortActivity() {
    _uiState.update { it.copy(shortActivityConfirm = false) }
    confirmStopTracking(force = true)
  }

  fun dismissShortActivity() {
    _uiState.update { it.copy(shortActivityConfirm = false) }
  }

  /**
   * Termina e salva il tracking. Per attività libere < 50m mostra prima il
   * dialog di conferma; le sessioni di gruppo non hanno soglia minima.
   */
  fun confirmStopTracking(force: Boolean = false) {
    val current = _uiState.value
    val isFreeShort = current.activeSessionId == null && current.distanceMeters < 50.0
    if (isFreeShort && !force) {
      // Mostra dialog "Attività troppo corta". Hardware ancora attivo, può cancellare.
      _uiState.update { it.copy(shortActivityConfirm = true, showStopConfirm = false) }
      return
    }
    stopHardware()
    val snapState = _uiState.value

    // 1. Resetta subito lo state UI (nasconde i controlli di tracking)
>>>>>>> 7c170be742c0ca0f16c4c6df6f5c273d643d4a7a
    _uiState.update {
      it.copy(
        trackingStatus = TrackingStatus.IDLE,
        isAutoPaused = false,
        showStopConfirm = false,
        trackGeoPoints = emptyList(),
        elapsedSeconds = 0,
        distanceMeters = 0.0,
        elevationGainMeters = 0,
        activeSessionId = null,
      )
    }
<<<<<<< HEAD
=======

    // Salva in Room e poi tenta il sync immediato. Se fallisce, SyncManager
    // riproverà automaticamente con backoff incrementale.
    viewModelScope.launch {
      val savedId = saveCompletedActivity(snapState)
      attemptImmediateSync(savedId, snapState)
    }
  }

  private suspend fun attemptImmediateSync(localId: String, state: UiState) {
    val dao = (app as TsmApplication).database.completedActivityDao()
    val distKm = state.distanceMeters / 1000.0
    val actualH = state.elapsedSeconds / 3600.0
    val finalPts = HikeEstimation.finalPoints(distKm, state.elevationGainMeters, actualH)
    val payload = ActualStats(
      movingSeconds = state.elapsedSeconds,
      totalSeconds = state.elapsedSeconds,
      distanceMeters = state.distanceMeters,
      elevationGainM = state.elevationGainMeters,
      finalPoints = finalPts,
      estimatedCalories = (70 * distKm * 0.85).toInt(),
      currentAltitudeM = state.currentAltitudeMeters,
    )

    val sessionId = state.activeSessionId
    runCatching {
      if (sessionId != null) {
        val resp = TsmApiClient.service().completeSession(
          sessionId,
          CompleteSessionRequest(actualStats = payload),
        )
        if (resp.isSuccessful) {
          dao.markSynced(localId, sessionId)
        } else {
          SyncManager.enqueueImmediate(app)
        }
      } else {
        val req = CreateActivityRequest(
          name = state.activityNameDraft.trim().ifBlank { "Escursione" },
          activityType = "hiking",
          startTimeMs = if (state.trackStartTimeMs > 0) state.trackStartTimeMs else System.currentTimeMillis() - state.elapsedSeconds * 1000L,
          endTimeMs = System.currentTimeMillis(),
          actualStats = payload,
        )
        val resp = TsmApiClient.service().createActivity(req)
        if (resp.isSuccessful) {
          val remoteId = resp.body()?._id
          dao.markSynced(localId, remoteId)
        } else {
          SyncManager.enqueueImmediate(app)
        }
      }
    }.onFailure {
      SyncManager.enqueueImmediate(app)
    }
  }

  fun dismissActivitySaved() {
    _uiState.update { it.copy(activitySaved = false) }
  }

  /**
   * Salva il tracciato GPS e le metriche in Room al termine del tracking.
   * I punti vengono campionati a max 200 per non superare i limiti di SQLite.
   */
  private suspend fun saveCompletedActivity(state: UiState): String {
    val dao = (app as TsmApplication).database.completedActivityDao()
    val gson = Gson()

    // Campionamento: max 200 punti
    val points = state.trackGeoPoints
    val sampled = if (points.size > 200) {
      val step = points.size / 200
      points.filterIndexed { i, _ -> i % step == 0 }
    } else points

    val trackJson = gson.toJson(sampled.map { listOf(it.latitude, it.longitude, it.altitude) })
    val distKm = state.distanceMeters / 1000.0
    val movingSec = state.elapsedSeconds
    val elevM = state.elevationGainMeters
    val actualH = movingSec / 3600.0
    val points2 = HikeEstimation.finalPoints(distKm, elevM, actualH)
    val cals = (70 * distKm * 0.85).toInt()
    val now = System.currentTimeMillis()
    val startMs = if (state.trackStartTimeMs > 0) state.trackStartTimeMs else now - movingSec * 1000L
    // Usa il nome che l'utente ha scelto nel dialog; fallback su default se vuoto
    val dateSuffix = SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(Date(now))
    val activityName = state.activityNameDraft.trim().ifBlank { "Escursione – $dateSuffix" }
    val newId = UUID.randomUUID().toString()

    dao.upsert(CompletedActivityEntity(
      id = newId,
      sessionId = state.activeSessionId,
      name = activityName,
      activityType = "hiking",
      startTimeMs = startMs,
      endTimeMs = now,
      movingSeconds = movingSec,
      totalSeconds = movingSec,
      distanceMeters = state.distanceMeters,
      elevationGainMeters = elevM,
      currentAltitudeMeters = state.currentAltitudeMeters,
      difficultyLevel = null,
      trackLatLng = trackJson,
      estimatedCalories = cals,
      points = points2,
      isSynced = false,
      completedAt = now,
    ))

    _uiState.update { it.copy(activitySaved = true) }
    return newId
>>>>>>> 7c170be742c0ca0f16c4c6df6f5c273d643d4a7a
  }

  fun centerOnUser() {
    _uiState.update { it.copy(centerOnUserTick = it.centerOnUserTick + 1) }
  }

  private fun resumeTracking() {
    trackingEngine.resume()
    stillSinceMs = null
    _uiState.update {
      it.copy(trackingStatus = TrackingStatus.RECORDING, isAutoPaused = false)
    }
  }

  private fun applyLocation(snapshot: LocationSnapshot) {
    val previous = lastSnapshot
    when (trackingEngine.status) {
      TrackingStatus.RECORDING -> evaluateAutoPause(snapshot, previous)
      TrackingStatus.PAUSED ->
        if (trackingEngine.isAutoPaused) evaluateAutoPause(snapshot, previous)
      TrackingStatus.IDLE -> Unit
    }
    lastSnapshot = snapshot
    val metrics = trackingEngine.onLocation(snapshot)
    _uiState.update { state ->
      state.copy(
        userLocation = snapshot,
        gpsSignalLevel = UserLocationTracker.gpsSignalLevel(snapshot.accuracyMeters),
        gpsAccuracyLabel = "±${snapshot.accuracyMeters.toInt()} m",
        trackingStatus = trackingEngine.status,
        isAutoPaused = trackingEngine.isAutoPaused,
        distanceMeters = metrics.distanceMeters,
        elevationGainMeters = metrics.elevationGainMeters,
        currentAltitudeMeters = snapshot.altitudeMeters?.toInt(),
        trackGeoPoints =
          metrics.trackPoints.map { GeoPoint(it.latitude, it.longitude) },
      )
    }
  }

  private fun evaluateAutoPause(snapshot: LocationSnapshot, previous: LocationSnapshot?) {
    if (trackingEngine.status == TrackingStatus.RECORDING) {
      val speed = effectiveSpeedMps(snapshot, previous)
      if (speed < STATIONARY_SPEED_MPS && stationaryDetector.isStationary) {
        val now = System.currentTimeMillis()
        if (stillSinceMs == null) stillSinceMs = now
        if (now - (stillSinceMs ?: now) >= AUTO_PAUSE_DELAY_MS) {
          trackingEngine.pause(manual = false)
          _uiState.update {
            it.copy(trackingStatus = TrackingStatus.PAUSED, isAutoPaused = true)
          }
        }
      } else {
        stillSinceMs = null
      }
      return
    }

    if (trackingEngine.status == TrackingStatus.PAUSED && trackingEngine.isAutoPaused) {
      val speed = effectiveSpeedMps(snapshot, previous)
      if (speed >= RESUME_SPEED_MPS || !stationaryDetector.isStationary) {
        resumeTracking()
      }
    }
  }

  private fun effectiveSpeedMps(snapshot: LocationSnapshot, previous: LocationSnapshot?): Float {
    snapshot.speedMps?.let { return it }
    val prev = previous ?: return 0f
    if (prev.timestampMs >= snapshot.timestampMs) return 0f
    val dtSec = (snapshot.timestampMs - prev.timestampMs) / 1000.0
    if (dtSec <= 0) return 0f
    val distM = haversineM(prev, snapshot)
    return (distM / dtSec).toFloat()
  }

  private fun haversineM(a: LocationSnapshot, b: LocationSnapshot): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h =
      kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
        kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(h), kotlin.math.sqrt(1 - h))
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerJob =
      viewModelScope.launch {
        while (isActive) {
          delay(1_000)
          if (_uiState.value.trackingStatus == TrackingStatus.RECORDING) {
            _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
          }
        }
      }
  }

  override fun onCleared() {
    timerJob?.cancel()
    stationaryDetector.stop()
    locationTracker.stop()
    if (_uiState.value.trackingStatus != TrackingStatus.IDLE) {
      ForegroundTrackingService.stop(app)
    }
    super.onCleared()
  }

  companion object {
    private const val STATIONARY_SPEED_MPS = 0.5f
    private const val RESUME_SPEED_MPS = 1.0f
    private const val AUTO_PAUSE_DELAY_MS = 45_000L
  }
}
