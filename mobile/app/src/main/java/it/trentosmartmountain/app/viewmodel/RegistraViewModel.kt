package it.trentosmartmountain.app.viewmodel

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.estimation.HikeEstimation
import it.trentosmartmountain.app.data.local.db.CompletedActivityEntity
import it.trentosmartmountain.app.data.location.HikeTrackingEngine
import it.trentosmartmountain.app.data.location.LocationSnapshot
import it.trentosmartmountain.app.data.location.StationaryDetector
import it.trentosmartmountain.app.data.location.TrackingLocationBus
import it.trentosmartmountain.app.data.location.TrackingStatus
import it.trentosmartmountain.app.data.location.UserLocationTracker
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.EmergencyResponse
import it.trentosmartmountain.app.util.SosNotificationHelper
import it.trentosmartmountain.app.data.remote.dto.ActualStats
import it.trentosmartmountain.app.data.remote.dto.CompleteSessionRequest
import it.trentosmartmountain.app.data.remote.dto.CreateActivityRequest
import it.trentosmartmountain.app.data.remote.dto.UpdateSessionStatusRequest
import it.trentosmartmountain.app.data.session.SessionStartCoordinator
import it.trentosmartmountain.app.data.sync.SyncManager
import it.trentosmartmountain.app.repository.EmergencyRepository
import it.trentosmartmountain.app.repository.OfflineEmergencyException
import it.trentosmartmountain.app.service.ForegroundTrackingService
import it.trentosmartmountain.app.service.SosBeaconService
import java.security.SecureRandom
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    /** SOS: fase UI (conferma, countdown, attivo, coda offline). */
    val sosPhase: SosPhase = SosPhase.IDLE,
    val sosCountdownSeconds: Int = 0,
    val sosSelectedType: String = "INJURY",
    val sosActiveEmergencyId: String? = null,
    val sosBeaconInstanceId: String? = null,
    val sosPendingOffline: Boolean = false,
    val sosStatusMessage: String? = null,
    val showSosConfirmDialog: Boolean = false,
    val showSosCancelDialog: Boolean = false,
    /** SOS in entrata (capogruppo o partecipante dopo share). */
    val incomingEmergencies: List<EmergencyResponse> = emptyList(),
    val isSessionGroupLeader: Boolean = false,
    val showIncomingEmergencyIcon: Boolean = false,
    val showSosAlertBorder: Boolean = false,
    val showSosListSheet: Boolean = false,
    val showSosDetailSheet: Boolean = false,
    val selectedIncomingEmergency: EmergencyResponse? = null,
    /** Debug ricezione SOS (es. errore API o sessione non collegata). */
    val incomingSosDebugMessage: String? = null,
  )

  enum class SosPhase {
    IDLE,
    COUNTDOWN,
    ACTIVE,
    QUEUED_OFFLINE,
    SENDING,
  }

  private val app = getApplication<Application>()
  private val emergencyRepo = EmergencyRepository(app)
  private val locationTracker = UserLocationTracker(app)
  private val trackingEngine = HikeTrackingEngine()
  private val stationaryDetector = StationaryDetector(app)

  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  private var timerJob: Job? = null
  private var sosCountdownJob: Job? = null
  private var stillSinceMs: Long? = null
  private var lastSnapshot: LocationSnapshot? = null
  private var activeSosIdempotencyKey: String? = null
  private var emergencyPollJob: Job? = null
  private var lastIncomingEmergencyCount = 0
  private var currentUserId: String? = null

  init {
    currentUserId =
      TokenStorage.getInstance(app).getToken()?.let { JwtDecoder.userIdFrom(it) }
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
    viewModelScope.launch {
      uiState
        .map { it.activeSessionId }
        .distinctUntilChanged()
        .collect { sessionId ->
          if (sessionId != null) {
            startEmergencyPolling(sessionId)
          } else {
            stopEmergencyPolling()
            _uiState.update {
              it.copy(
                incomingEmergencies = emptyList(),
                showIncomingEmergencyIcon = false,
                showSosAlertBorder = false,
                isSessionGroupLeader = false,
                incomingSosDebugMessage = null,
              )
            }
          }
        }
    }
  }

  /**
   * Collega la sessione ACTIVE dal server se l'utente è su Registra senza aver passato da AVVIA
   * (es. capogruppo già in escursione o tab cambiata).
   */
  fun syncActiveSessionFromServer() {
    viewModelScope.launch {
      if (_uiState.value.activeSessionId != null) {
        refreshIncomingEmergencies()
        return@launch
      }
      runCatching {
        val res = TsmApiClient.service().getMySessions()
        if (!res.isSuccessful) {
          _uiState.update {
            it.copy(incomingSosDebugMessage = "Sessioni: HTTP ${res.code()}")
          }
          return@launch
        }
        val active = res.body()?.firstOrNull { it.status == "ACTIVE" }
        if (active != null) {
          _uiState.update {
            it.copy(
              activeSessionId = active._id,
              incomingSosDebugMessage = null,
            )
          }
          refreshSessionRole(active._id)
          refreshIncomingEmergencies()
        } else {
          _uiState.update {
            it.copy(
              incomingSosDebugMessage =
                "Nessuna sessione ACTIVE sul server — usa AVVIA dal dettaglio escursione",
            )
          }
        }
      }.onFailure { e ->
        _uiState.update {
          it.copy(incomingSosDebugMessage = "Rete sessioni: ${e.message}")
        }
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
      runCatching {
        TsmApiClient.service().updateSessionStatus(
          sessionId,
          UpdateSessionStatusRequest(status = "ACTIVE"),
        )
      }
      refreshSessionRole(sessionId)
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
    val dateSuffix = SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(Date())
    val default = "Escursione – $dateSuffix"
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

  fun canTriggerSos(): Boolean {
    val state = _uiState.value
    val trackingOk =
      state.trackingStatus == TrackingStatus.RECORDING ||
        state.trackingStatus == TrackingStatus.PAUSED
    return trackingOk && state.activeSessionId != null && state.sosPhase == SosPhase.IDLE
  }

  fun onSosFabClicked() {
    if (!canTriggerSos()) return
    _uiState.update { it.copy(showSosConfirmDialog = true) }
  }

  fun dismissSosConfirmDialog() {
    _uiState.update { it.copy(showSosConfirmDialog = false) }
  }

  fun updateSosEmergencyType(type: String) {
    _uiState.update { it.copy(sosSelectedType = type) }
  }

  /** Dopo "Prosegui" nel dialog iniziale: avvia countdown 15s. */
  fun confirmSosProceed() {
    _uiState.update {
      it.copy(
        showSosConfirmDialog = false,
        sosPhase = SosPhase.COUNTDOWN,
        sosCountdownSeconds = SOS_COUNTDOWN_SEC,
        sosStatusMessage = null,
      )
    }
    sosCountdownJob?.cancel()
    sosCountdownJob =
      viewModelScope.launch {
        var remaining = SOS_COUNTDOWN_SEC
        while (remaining > 0) {
          delay(1_000)
          remaining--
          _uiState.update { it.copy(sosCountdownSeconds = remaining) }
        }
        launchSosAfterCountdown()
      }
  }

  fun cancelSosCountdown() {
    sosCountdownJob?.cancel()
    _uiState.update {
      it.copy(
        sosPhase = SosPhase.IDLE,
        sosCountdownSeconds = 0,
        sosStatusMessage = null,
      )
    }
  }

  private fun launchSosAfterCountdown() {
    val state = _uiState.value
    val sessionId = state.activeSessionId ?: return
    val location = state.userLocation ?: run {
      _uiState.update {
        it.copy(
          sosPhase = SosPhase.IDLE,
          sosStatusMessage = "Posizione GPS non disponibile",
        )
      }
      return
    }

    val beaconId = randomBeaconInstanceId()
    val idempotencyKey = UUID.randomUUID().toString()
    activeSosIdempotencyKey = idempotencyKey

    SosBeaconService.start(app, beaconId)

    _uiState.update {
      it.copy(
        sosPhase = SosPhase.SENDING,
        sosBeaconInstanceId = beaconId,
        sosStatusMessage = null,
      )
    }

    viewModelScope.launch {
      val result =
        emergencyRepo.createEmergency(
          sessionId = sessionId,
          emergencyType = state.sosSelectedType,
          longitude = location.longitude,
          latitude = location.latitude,
          beaconInstanceId = beaconId,
          idempotencyKey = idempotencyKey,
        )
      result.fold(
        onSuccess = { emergency ->
          _uiState.update {
            it.copy(
              sosPhase = SosPhase.ACTIVE,
              sosActiveEmergencyId = emergency.id,
              sosPendingOffline = false,
              sosStatusMessage = "SOS inviato al capogruppo",
            )
          }
          refreshIncomingEmergencies()
        },
        onFailure = { err ->
          if (err is OfflineEmergencyException) {
            _uiState.update {
              it.copy(
                sosPhase = SosPhase.QUEUED_OFFLINE,
                sosPendingOffline = true,
                sosStatusMessage = "Beacon attivo — invio dati in attesa di connessione",
              )
            }
            retryPendingSosWhenOnline()
          } else {
            _uiState.update {
              it.copy(
                sosPhase = SosPhase.ACTIVE,
                sosPendingOffline = true,
                sosStatusMessage = "Beacon attivo — errore invio: ${err.message}",
              )
            }
          }
        },
      )
    }
  }

  private fun retryPendingSosWhenOnline() {
    viewModelScope.launch {
      while (_uiState.value.sosPendingOffline) {
        delay(15_000)
        if (!emergencyRepo.isNetworkAvailable()) continue
        val uploaded = emergencyRepo.flushPendingQueue()
        if (uploaded > 0) {
          _uiState.update {
            it.copy(
              sosPendingOffline = false,
              sosPhase = SosPhase.ACTIVE,
              sosStatusMessage = "SOS inviato al capogruppo",
            )
          }
          break
        }
      }
    }
  }

  fun requestCancelActiveSos() {
    if (_uiState.value.sosPhase == SosPhase.ACTIVE ||
      _uiState.value.sosPhase == SosPhase.QUEUED_OFFLINE
    ) {
      _uiState.update { it.copy(showSosCancelDialog = true) }
    }
  }

  fun dismissSosCancelDialog() {
    _uiState.update { it.copy(showSosCancelDialog = false) }
  }

  fun confirmCancelActiveSos(reason: String) {
    _uiState.update { it.copy(showSosCancelDialog = false) }
    SosBeaconService.stop(app)
    val emergencyId = _uiState.value.sosActiveEmergencyId
    val idempotencyKey = activeSosIdempotencyKey
    _uiState.update {
      it.copy(
        sosPhase = SosPhase.IDLE,
        sosActiveEmergencyId = null,
        sosBeaconInstanceId = null,
        sosPendingOffline = false,
        sosStatusMessage = null,
      )
    }
    activeSosIdempotencyKey = null
    if (emergencyId != null) {
      viewModelScope.launch {
        emergencyRepo.cancelEmergency(emergencyId, reason)
      }
    } else if (idempotencyKey != null) {
      viewModelScope.launch {
        (app as TsmApplication).database.pendingEmergencyDao().deleteByKey(idempotencyKey)
      }
    }
  }

  private fun randomBeaconInstanceId(): String {
    val bytes = ByteArray(6)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun onIncomingEmergencyIconClick() {
    _uiState.update { it.copy(showSosListSheet = true, showSosAlertBorder = false) }
    viewModelScope.launch {
      val unacked = _uiState.value.incomingEmergencies.filter { it.leaderAckAt == null }
      for (emergency in unacked) {
        emergencyRepo.ackEmergency(emergency.id)
      }
      refreshIncomingEmergencies()
    }
  }

  fun closeSosListSheet() {
    _uiState.update { it.copy(showSosListSheet = false) }
  }

  fun openIncomingEmergencyDetail(emergency: EmergencyResponse) {
    _uiState.update {
      it.copy(
        selectedIncomingEmergency = emergency,
        showSosDetailSheet = true,
        showSosListSheet = false,
      )
    }
  }

  fun closeSosDetailSheet() {
    _uiState.update {
      it.copy(showSosDetailSheet = false, selectedIncomingEmergency = null, showSosListSheet = true)
    }
  }

  fun dismissSelectedIncomingEmergency() {
    val id = _uiState.value.selectedIncomingEmergency?.id ?: return
    viewModelScope.launch {
      emergencyRepo.dismissEmergency(id)
      _uiState.update { it.copy(showSosDetailSheet = false, selectedIncomingEmergency = null) }
      refreshIncomingEmergencies()
      _uiState.update { it.copy(showSosListSheet = it.incomingEmergencies.isNotEmpty()) }
    }
  }

  fun shareSelectedIncomingEmergency() {
    val id = _uiState.value.selectedIncomingEmergency?.id ?: return
    viewModelScope.launch {
      emergencyRepo.shareEmergencyWithGroup(id)
      refreshIncomingEmergencies()
      val updated = _uiState.value.incomingEmergencies.find { it.id == id }
      if (updated != null) {
        _uiState.update { it.copy(selectedIncomingEmergency = updated) }
      }
    }
  }

  private fun startEmergencyPolling(sessionId: String) {
    emergencyPollJob?.cancel()
    emergencyPollJob =
      viewModelScope.launch {
        refreshSessionRole(sessionId)
        refreshIncomingEmergencies()
        while (isActive) {
          delay(EMERGENCY_POLL_INTERVAL_MS)
          refreshIncomingEmergencies()
        }
      }
  }

  private fun stopEmergencyPolling() {
    emergencyPollJob?.cancel()
    emergencyPollJob = null
    lastIncomingEmergencyCount = 0
  }

  private suspend fun refreshSessionRole(sessionId: String) {
    val userId = currentUserId ?: return
    runCatching {
      val res = TsmApiClient.service().getSessionById(sessionId)
      if (res.isSuccessful && res.body() != null) {
        val session = res.body()!!
        val isLeader =
          session.participants?.any {
            it.userId?._id == userId && it.role == "groupLeader"
          } == true
        _uiState.update { it.copy(isSessionGroupLeader = isLeader) }
      }
    }
  }

  private suspend fun refreshIncomingEmergencies() {
    val sessionId = _uiState.value.activeSessionId ?: return
    val result = emergencyRepo.listSessionEmergencies(sessionId)
    result.fold(
      onSuccess = { payload ->
        val list = payload.emergencies
        val isLeader = payload.isGroupLeader
        val visible =
          list.isNotEmpty() &&
            (isLeader || list.any { it.status == "SHARED_WITH_GROUP" })
        val showBorder =
          isLeader && payload.hasUnacked && !_uiState.value.showSosListSheet

        if (isLeader && list.size > lastIncomingEmergencyCount && list.isNotEmpty()) {
          val newest = list.firstOrNull()
          val name = newest?.profileSnapshot?.displayName ?: newest?.senderUserId?.username ?: "?"
          SosNotificationHelper.showIncomingSos(app, name)
        }
        lastIncomingEmergencyCount = if (isLeader) list.size else lastIncomingEmergencyCount

        _uiState.update {
          it.copy(
            incomingEmergencies = list,
            isSessionGroupLeader = isLeader,
            showIncomingEmergencyIcon = visible,
            showSosAlertBorder = showBorder,
            incomingSosDebugMessage =
              if (isLeader && list.isEmpty()) {
                "In attesa SOS (sessione ${sessionId.takeLast(6)})…"
              } else {
                null
              },
          )
        }
      },
      onFailure = { err ->
        _uiState.update {
          it.copy(
            incomingSosDebugMessage = "Poll emergenze: ${err.message}",
            showIncomingEmergencyIcon = false,
            showSosAlertBorder = false,
          )
        }
      },
    )
  }

  override fun onCleared() {
    stopEmergencyPolling()
    sosCountdownJob?.cancel()
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
    private const val SOS_COUNTDOWN_SEC = 15
    private const val EMERGENCY_POLL_INTERVAL_MS = 8_000L
  }
}
