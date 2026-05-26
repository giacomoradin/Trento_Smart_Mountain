package it.trentosmartmountain.app.viewmodel

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.location.HikeTrackingEngine
import it.trentosmartmountain.app.data.location.LocationSnapshot
import it.trentosmartmountain.app.data.location.StationaryDetector
import it.trentosmartmountain.app.data.location.TrackingLocationBus
import it.trentosmartmountain.app.data.location.TrackingStatus
import it.trentosmartmountain.app.data.location.UserLocationTracker
import it.trentosmartmountain.app.data.session.SessionStartCoordinator
import it.trentosmartmountain.app.repository.SessionCommandRepository
import it.trentosmartmountain.app.repository.TrackingPersistenceRepository
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
 *
 * Refactor audit 2026-05: la persistenza Room (WAL + snapshot finale) è stata
 * estratta in [TrackingPersistenceRepository], e le chiamate API
 * (update session status, complete session, create activity) in
 * [SessionCommandRepository]. La VM resta orchestrator dello UI state +
 * lifecycle hardware (GPS, foreground service, timer).
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
    /**
     * true quando un tentativo di avviare il tracking è stato bloccato perché
     * il GPS hardware del dispositivo è spento. La UI mostra un dialog con
     * link a Settings.ACTION_LOCATION_SOURCE_SETTINGS. Viene resettato a false
     * appena il tracking parte o l'utente chiude il dialog.
     */
    val gpsDisabledWarning: Boolean = false,
  )

  private val app = getApplication<Application>()
  private val locationTracker = UserLocationTracker(app)
  private val trackingEngine = HikeTrackingEngine()
  private val stationaryDetector = StationaryDetector(app)
  private val persistence = TrackingPersistenceRepository(app)
  private val sessionCommands = SessionCommandRepository(app)

  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  private var timerJob: Job? = null
  private var stillSinceMs: Long? = null
  private var lastSnapshot: LocationSnapshot? = null
  // Identifica il tracciato corrente nella WAL Room (crash-safety).
  // Non-null sse trackingStatus != IDLE. Generato da persistence.startTrack().
  private var currentTrackId: String? = null

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
    // Auto-start tracking quando SessionDetail / SessionHub.AVVIA emette un sessionId.
    // Consumiamo SEMPRE il segnale dopo aver gestito: il replay cache viene resettato
    // così non ritriggriamo a recomposition. SharedFlow distribuisce ogni emit anche
    // a HikerMainScreen (che a sua volta switcha la tab) in modo indipendente.
    viewModelScope.launch {
      SessionStartCoordinator.pendingSessionStart.collect { sessionId ->
        if (_uiState.value.trackingStatus == TrackingStatus.IDLE) {
          autoStartFromSession(sessionId)
        } else {
          // Tracking già in corso → memorizziamo l'ID ma non interrompiamo
          _uiState.update { it.copy(activeSessionId = sessionId) }
        }
        SessionStartCoordinator.consume()
      }
    }
  }

  /** True solo se il GPS hardware è attivo nelle impostazioni del dispositivo. */
  private fun isGpsHardwareEnabled(): Boolean {
    val lm = app.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
  }

  /** Chiamato dalla UI quando l'utente chiude il dialog "GPS spento". */
  fun dismissGpsWarning() {
    _uiState.update { it.copy(gpsDisabledWarning = false) }
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
      sessionCommands.markSessionActive(sessionId)
    }
    // Avvia solo se permessi GPS + GPS hardware acceso; altrimenti
    // startTracking() stesso setterà i flag di warning corretti e la UI
    // chiederà permessi o di accendere il GPS.
    if (_uiState.value.hasLocationPermission) {
      startTracking()
    }
  }

  /** Aggiorna il nome bozza dell'attività che l'utente sta editando nel dialog di salvataggio. */
  fun updateActivityNameDraft(name: String) {
    _uiState.update { it.copy(activityNameDraft = name) }
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
    // GPS hardware acceso? Se no, non avviamo: la UI mostra il dialog di warning.
    if (!isGpsHardwareEnabled()) {
      _uiState.update { it.copy(gpsDisabledWarning = true) }
      return
    }
    trackingEngine.start()
    stillSinceMs = null
    locationTracker.stop()
    ForegroundTrackingService.start(app)
    stationaryDetector.start()
    startTimer()
    val now = System.currentTimeMillis()
    // Crea trackId WAL: ogni snapshot GPS successivo sarà persistito.
    currentTrackId = persistence.startTrack()
    _uiState.update {
      it.copy(
        trackingStatus = TrackingStatus.RECORDING,
        isAutoPaused = false,
        elapsedSeconds = 0,
        distanceMeters = 0.0,
        elevationGainMeters = 0,
        trackGeoPoints = emptyList(),
        trackStartTimeMs = now,
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
    val default = persistence.defaultActivityName()
    _uiState.update {
      it.copy(
        showStopConfirm = true,
        activityNameDraft = it.activityNameDraft.ifBlank { default },
      )
    }
  }

  fun dismissStopConfirm() {
    _uiState.update { it.copy(showStopConfirm = false) }
  }

  /**
   * Scarta il tracciato senza salvare e termina il tracking.
   * Usato dal bottone "Scarta" nel dialog di salvataggio.
   */
  fun discardTracking() {
    stopHardware()
    val orphanTrackId = currentTrackId
    currentTrackId = null
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
    // Cleanup WAL — i punti raccolti sono ora spazzatura. Fire-and-forget:
    // anche se fallisce, la prossima `startTrack()` genera un trackId nuovo.
    if (orphanTrackId != null) {
      viewModelScope.launch { persistence.discardTrack(orphanTrackId) }
    }
  }

  private fun stopHardware() {
    trackingEngine.stop()
    stationaryDetector.stop()
    ForegroundTrackingService.stop(app)
    timerJob?.cancel()
    stillSinceMs = null
    if (_uiState.value.hasLocationPermission) {
      locationTracker.start()
    }
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
    val trackId = currentTrackId
    currentTrackId = null

    // 1. Resetta subito lo state UI (nasconde i controlli di tracking)
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

    // Salva in Room (via WAL → CompletedActivity) e poi tenta il sync immediato.
    // Se fallisce, SyncManager riproverà automaticamente con backoff incrementale.
    if (trackId == null) {
      // Stato anomalo: nessun trackId attivo. Skip salvataggio.
      return
    }
    viewModelScope.launch {
      val finalize = TrackingPersistenceRepository.FinalizeSnapshot(
        trackId = trackId,
        activeSessionId = snapState.activeSessionId,
        activityName = snapState.activityNameDraft.trim().ifBlank {
          persistence.defaultActivityName()
        },
        startTimeMs = snapState.trackStartTimeMs,
        movingSeconds = snapState.elapsedSeconds,
        distanceMeters = snapState.distanceMeters,
        elevationGainMeters = snapState.elevationGainMeters,
        currentAltitudeMeters = snapState.currentAltitudeMeters,
      )
      val localId = persistence.finalize(finalize)
      _uiState.update { it.copy(activitySaved = true) }

      val result = sessionCommands.completeOrUpload(
        sessionId = snapState.activeSessionId,
        activityName = finalize.activityName,
        startTimeMs = finalize.startTimeMs,
        movingSeconds = finalize.movingSeconds,
        distanceMeters = finalize.distanceMeters,
        elevationGainMeters = finalize.elevationGainMeters,
        currentAltitudeMeters = finalize.currentAltitudeMeters,
      )
      if (result is SessionCommandRepository.SyncResult.Synced) {
        // Marca sincronizzato con il remoteId emesso dal backend (per attività
        // libere) o con il sessionId stesso (per sessioni di gruppo).
        (app as it.trentosmartmountain.app.TsmApplication)
          .database
          .completedActivityDao()
          .markSynced(localId, result.remoteId ?: snapState.activeSessionId)
      }
    }
  }

  fun dismissActivitySaved() {
    _uiState.update { it.copy(activitySaved = false) }
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

    // WAL append: solo se stiamo registrando attivamente (non in pause/idle).
    // Crash-safety: anche se l'app muore dopo questo insert, il punto è
    // recuperabile dalla tabella tracking_wal.
    val trackId = currentTrackId
    if (trackId != null && trackingEngine.status == TrackingStatus.RECORDING) {
      viewModelScope.launch {
        persistence.appendPoint(
          trackId = trackId,
          latitude = snapshot.latitude,
          longitude = snapshot.longitude,
          altitudeMeters = snapshot.altitudeMeters,
          timestampMs = snapshot.timestampMs,
        )
      }
    }

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
