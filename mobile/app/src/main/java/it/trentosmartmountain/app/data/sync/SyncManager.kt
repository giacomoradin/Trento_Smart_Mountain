package it.trentosmartmountain.app.data.sync

import android.content.Context
import android.util.Log
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.estimation.HikeEstimation
import it.trentosmartmountain.app.data.local.db.CompletedActivityDao
import it.trentosmartmountain.app.data.local.db.CompletedActivityEntity
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.ActualStats
import it.trentosmartmountain.app.data.remote.dto.CompleteSessionRequest
import it.trentosmartmountain.app.data.remote.dto.CreateActivityRequest
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.util.ELEVATION_PROFILE_MAX_POINTS
import it.trentosmartmountain.app.util.ROUTE_SIGNATURE_MAX_POINTS
import it.trentosmartmountain.app.util.downsampleByIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Gestisce il sync delle attività locali non ancora inviate al backend (isSynced = 0).
 *
 * Retry con backoff incrementale per record:
 *   retryCount=0 → 1 min (primo tentativo rapido dopo il fallimento)
 *   retryCount=1 → 5 min
 *   retryCount=2 → 30 min
 *   retryCount=3+ → 60 min
 *
 * Poll ogni 60s via coroutine globale avviata in [TsmApplication.onCreate].
 * Il loop rimane attivo finché il processo è in vita; alla riapertura dell'app
 * riparte e processa il backlog. [enqueueImmediate] forza un giro istantaneo
 * (es. pull-to-refresh, bottone "Risincronizza").
 */
object SyncManager {

    private const val TAG = "SyncManager"
    private const val POLL_INTERVAL_MS = 60_000L

    // Idle backoff del loop: a coda vuota il polling scala 60s → 2min → 5min (cap).
    // Evita query Room + wakeup periodici quando non c'è nulla da sincronizzare;
    // un nuovo elemento in coda o enqueueImmediate riportano il ritmo a 60s.
    private const val POLL_IDLE_INTERVAL_MS = 120_000L
    private const val POLL_IDLE_MAX_INTERVAL_MS = 300_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pollMutex = Mutex()
    private var pollJob: Job? = null

    @Volatile
    private var idleStreak = 0

    // Avvia il poll loop. Idempotente: se è già attivo non fa nulla.
    fun start(context: Context) {
        if (pollJob?.isActive == true) return
        val app = context.applicationContext as TsmApplication
        pollJob = scope.launch {
            while (isActive) {
                runOnce(app)
                val nextDelay = when {
                    idleStreak == 0 -> POLL_INTERVAL_MS
                    idleStreak == 1 -> POLL_IDLE_INTERVAL_MS
                    else -> POLL_IDLE_MAX_INTERVAL_MS
                }
                delay(nextDelay)
            }
        }
    }

    /**
     * Forza un giro di sync ignorando il backoff. Usato dal bottone "Risincronizza"
     * (e da pull-to-refresh). A differenza del poll loop normale, retry IMMEDIATO
     * su ogni entità non sincronizzata anche se il cooldown del backoff non è ancora
     * scaduto. Senza questo, le entità con retryCount alto (backoff fino a 1h) erano
     * saltate silenziosamente, dando l'impressione che il bottone non facesse nulla.
     */
    fun enqueueImmediate(context: Context) {
        val app = context.applicationContext as TsmApplication
        idleStreak = 0
        scope.launch { runOnce(app, ignoreBackoff = true) }
    }

    private suspend fun runOnce(
        app: TsmApplication,
        ignoreBackoff: Boolean = false,
    ) = pollMutex.withLock {
        val dao = app.database.completedActivityDao()
        val unsynced = dao.getUnsynced()
        if (unsynced.isEmpty()) {
            idleStreak = (idleStreak + 1).coerceAtMost(2)
            return@withLock
        }
        idleStreak = 0

        val now = System.currentTimeMillis()
        Log.d(TAG, "Polling: ${unsynced.size} attività da sincronizzare (ignoreBackoff=$ignoreBackoff)")

        for (entity in unsynced) {
            if (!ignoreBackoff && entity.lastRetryAtMs > 0) {
                val elapsed = now - entity.lastRetryAtMs
                val needed = computeBackoffMs(entity.retryCount)
                if (elapsed < needed) continue
            }
            syncOne(entity, dao)
        }
    }

    private suspend fun syncOne(
        entity: CompletedActivityEntity,
        dao: CompletedActivityDao,
    ) {
        val now = System.currentTimeMillis()
        try {
            val sessionId = entity.sessionId
            if (sessionId != null) {
                // 1) Conclusione individuale ADR-001 — best-effort: il server la
                //    accetta anche a sessione già chiusa dal capogruppo; un 4xx
                //    (es. partecipante rimosso) non blocca la copia personale.
                runCatching {
                    TsmApiClient.service()
                        .completeSession(sessionId, CompleteSessionRequest(buildActualStats(entity)))
                }.onSuccess { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "[${entity.id}] completeSession HTTP ${resp.code()} (proseguo con copia personale)")
                    }
                }

                // 2) Copia PERSONALE dell'uscita (idempotente lato server per
                //    utente+sessione): è il documento condivisibile sul feed.
                val req = CreateActivityRequest(
                    name = entity.name,
                    activityType = entity.activityType,
                    startTimeMs = entity.startTimeMs,
                    endTimeMs = entity.endTimeMs,
                    actualStats = buildActualStats(entity),
                    difficultyLevel = entity.difficultyLevel,
                    elevationProfile = parseElevationProfile(entity.trackLatLng),
                    routePolyline = parseRoutePolyline(entity.trackLatLng),
                    sourceSessionId = sessionId,
                )
                val resp = TsmApiClient.service().createActivity(req)
                if (resp.isSuccessful) {
                    dao.markSynced(entity.id, resp.body()?._id ?: sessionId)
                    Log.i(TAG, "[${entity.id}] session sync OK (personal copy ${resp.body()?._id})")
                } else {
                    Log.w(TAG, "[${entity.id}] session personal copy HTTP ${resp.code()}")
                    dao.bumpRetry(entity.id, now)
                }
            } else {
                val req = CreateActivityRequest(
                    name = entity.name,
                    activityType = entity.activityType,
                    startTimeMs = entity.startTimeMs,
                    endTimeMs = entity.endTimeMs,
                    actualStats = buildActualStats(entity),
                    difficultyLevel = entity.difficultyLevel,
                    elevationProfile = parseElevationProfile(entity.trackLatLng),
                    routePolyline = parseRoutePolyline(entity.trackLatLng),
                )
                val resp = TsmApiClient.service().createActivity(req)
                if (resp.isSuccessful) {
                    val remoteId = resp.body()?._id
                    dao.markSynced(entity.id, remoteId)
                    Log.i(TAG, "[${entity.id}] free activity sync OK remoteId=$remoteId")
                } else {
                    Log.w(TAG, "[${entity.id}] free activity sync HTTP ${resp.code()}")
                    dao.bumpRetry(entity.id, now)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[${entity.id}] sync exception: ${e.message}")
            dao.bumpRetry(entity.id, now)
        }
    }

    private fun buildActualStats(entity: CompletedActivityEntity): ActualStats {
        val distKm = entity.distanceMeters / 1000.0
        val movingH = entity.movingSeconds / 3600.0
        val pts = entity.points ?: HikeEstimation.finalPoints(distKm, entity.elevationGainMeters, movingH)
        val cals = entity.estimatedCalories ?: (70 * distKm * 0.85).toInt()
        return ActualStats(
            movingSeconds = entity.movingSeconds,
            totalSeconds = entity.totalSeconds,
            distanceMeters = entity.distanceMeters,
            elevationGainM = entity.elevationGainMeters,
            finalPoints = pts,
            estimatedCalories = cals,
            currentAltitudeM = entity.currentAltitudeMeters,
        )
    }

    // Estrae il profilo altimetrico dal blob `trackLatLng` ([[lat,lon,alt], ...]).
    // Campiona a ~50 punti come il path immediato (SessionCommandRepository):
    // così un'attività mostra la stessa banda altimetrica nel feed sia che venga
    // inviata online sia che venga risincronizzata offline (parità store-and-forward).
    private fun parseElevationProfile(trackJson: String): List<Double>? {
        if (trackJson.isBlank() || trackJson == "[]") return null
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<List<Double>>>() {}.type
            val raw: List<List<Double>> = com.google.gson.Gson().fromJson(trackJson, type)
            val elevations = raw.mapNotNull { pts -> pts.getOrNull(2) }
            downsampleByIndex(elevations, ELEVATION_PROFILE_MAX_POINTS)
        } catch (_: Exception) { null }
    }

    private fun parseRoutePolyline(trackJson: String): List<RoutePoint>? {
        if (trackJson.isBlank() || trackJson == "[]") return null
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<List<Double>>>() {}.type
            val raw: List<List<Double>> = com.google.gson.Gson().fromJson(trackJson, type)
            val points = raw.map { pts -> RoutePoint(pts[0], pts[1]) }
            downsampleByIndex(points, ROUTE_SIGNATURE_MAX_POINTS)
        } catch (_: Exception) { null }
    }

    fun computeBackoffMs(retryCount: Int): Long = when (retryCount) {
        0 -> 60_000L
        1 -> 5 * 60_000L
        2 -> 30 * 60_000L
        else -> 60 * 60_000L
    }
}
