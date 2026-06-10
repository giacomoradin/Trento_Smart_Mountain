package it.trentosmartmountain.app.repository

import android.content.Context
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.estimation.HikeEstimation
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.ActualStats
import it.trentosmartmountain.app.data.remote.dto.CompleteSessionRequest
import it.trentosmartmountain.app.data.remote.dto.CreateActivityRequest
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.data.remote.dto.UpdateSessionStatusRequest
import it.trentosmartmountain.app.data.sync.SyncManager
import it.trentosmartmountain.app.util.ELEVATION_PROFILE_MAX_POINTS
import it.trentosmartmountain.app.util.downsampleByIndex

/**
 * Sottile wrapper attorno agli endpoint backend chiamati durante il flusso
 * di tracking: avvio sessione, completamento sessione, upload attività libera.
 *
 * Estratto da [it.trentosmartmountain.app.viewmodel.RegistraViewModel] nel
 * refactor audit 2026-05. Permette di testare la VM senza mockare l'intero
 * Retrofit, e centralizza il fallback "se l'upload fallisce, accodalo al
 * SyncManager".
 *
 * Non gestisce errori di rete in modo loud: ritorna [SyncResult.Pending]
 * quando l'upload fallisce (la VM mostra all'utente "salvata localmente,
 * sync in corso").
 */
class SessionCommandRepository(context: Context) {

  private val appContext = context.applicationContext

  sealed class SyncResult {
    /** Upload riuscito. [remoteId] è null per le complete-session (riusano sessionId). */
    data class Synced(val remoteId: String?) : SyncResult()
    /** Upload fallito o offline — è già stato accodato al SyncManager. */
    object Pending : SyncResult()
  }

  /**
   * Notifica al backend che la sessione passa da PLANNED → ACTIVE.
   * Fire-and-forget per design: se fallisce, il client può comunque tracciare
   * e il complete-session aggiornerà tutto al termine.
   */
  suspend fun markSessionActive(sessionId: String) {
    runCatching {
      TsmApiClient.service().updateSessionStatus(
        sessionId,
        UpdateSessionStatusRequest(status = "ACTIVE"),
      )
    }
  }

  /**
   * Chiusura forzata della sessione (modello Ibrido, "Chiudi sessione"): porta a
   * COMPLETED per tutti, anche con partecipanti non ancora conclusi. Solo capogruppo.
   * @return true se il server ha confermato la chiusura.
   */
  suspend fun forceCloseSession(sessionId: String): Boolean =
    runCatching {
      TsmApiClient.service().closeSession(sessionId).isSuccessful
    }.getOrDefault(false)

  /**
   * Completa una sessione di gruppo o crea un'attività libera, a seconda
   * della presenza di [sessionId]. In entrambi i casi, se l'upload fallisce
   * accoda al [SyncManager] per retry con backoff.
   */
  suspend fun completeOrUpload(
    sessionId: String?,
    activityName: String,
    startTimeMs: Long,
    movingSeconds: Long,
    distanceMeters: Double,
    elevationGainMeters: Int,
    currentAltitudeMeters: Int?,
    /**
     * Traccia GPS completa (lat/lon) registrata durante il tracking. Viene
     * campionata a [ROUTE_MAX_POINTS] e inviata SOLO per le attività libere
     * (le sessioni di gruppo hanno già `plannedRoute` lato server). Lista vuota
     * o < 2 punti → nessuna route signature (degrada a hero alternativo).
     */
    routePoints: List<RoutePoint> = emptyList(),
    /**
     * Quote assolute (m) della traccia, allineate ai [routePoints]. Inviate SOLO
     * per le attività libere e campionate a [ELEVATION_PROFILE_MAX_POINTS] punti
     * per la banda altimetrica del feed. Lista vuota o < 2 punti → nessun profilo
     * (la card degrada a hero senza altimetria).
     */
    elevations: List<Double> = emptyList(),
  ): SyncResult {
    val distKm = distanceMeters / 1000.0
    val actualH = movingSeconds / 3600.0
    val finalPts = HikeEstimation.finalPoints(distKm, elevationGainMeters, actualH)
    val payload = ActualStats(
      movingSeconds = movingSeconds,
      totalSeconds = movingSeconds,
      distanceMeters = distanceMeters,
      elevationGainM = elevationGainMeters,
      finalPoints = finalPts,
      estimatedCalories = (70 * distKm * 0.85).toInt(),
      currentAltitudeM = currentAltitudeMeters,
    )

    return runCatching {
      if (sessionId != null) {
        val resp = TsmApiClient.service().completeSession(
          sessionId,
          CompleteSessionRequest(actualStats = payload),
        )
        if (resp.isSuccessful) SyncResult.Synced(remoteId = sessionId)
        else {
          SyncManager.enqueueImmediate(appContext)
          SyncResult.Pending
        }
      } else {
        val req = CreateActivityRequest(
          name = activityName.trim().ifBlank { "Escursione" },
          activityType = "hiking",
          startTimeMs = if (startTimeMs > 0) startTimeMs else System.currentTimeMillis() - movingSeconds * 1000L,
          endTimeMs = System.currentTimeMillis(),
          actualStats = payload,
          elevationProfile = downsampleByIndex(elevations, ELEVATION_PROFILE_MAX_POINTS),
          routePolyline = downsampleByIndex(routePoints, ROUTE_MAX_POINTS),
        )
        val resp = TsmApiClient.service().createActivity(req)
        if (resp.isSuccessful) SyncResult.Synced(remoteId = resp.body()?._id)
        else {
          SyncManager.enqueueImmediate(appContext)
          SyncResult.Pending
        }
      }
    }.getOrElse {
      SyncManager.enqueueImmediate(appContext)
      SyncResult.Pending
    }
  }

  private companion object {
    /** Cap punti route signature inviati al backend (allineato a geoPolyline.js). */
    const val ROUTE_MAX_POINTS = 80
  }
}
