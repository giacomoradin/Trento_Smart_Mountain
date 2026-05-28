package it.trentosmartmountain.app.repository

import android.content.Context
import com.google.gson.Gson
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.estimation.HikeEstimation
import it.trentosmartmountain.app.data.local.db.CompletedActivityEntity
import it.trentosmartmountain.app.data.local.db.TrackingWalEntity
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Persistenza locale del tracciato GPS — WAL durante il tracking attivo,
 * snapshot finale in [CompletedActivityEntity] al termine.
 *
 * Estratto da [it.trentosmartmountain.app.viewmodel.RegistraViewModel] nel
 * refactor audit 2026-05 per:
 *  - testabilità (questo modulo non dipende da Application context oltre al DB).
 *  - separation of concerns (la VM resta solo orchestrator UI).
 *  - **crash-safety**: ogni punto GPS è scritto nella WAL appena ricevuto,
 *    invece che vivere solo in memoria fino al stop.
 *
 * Ciclo di vita di un tracking:
 *  1. `startTrack()` → genera trackId UUID, lo restituisce alla VM.
 *  2. `appendPoint(trackId, ...)` → INSERT in `tracking_wal` per ogni snapshot.
 *  3. `finalize(trackId, snapshot)` → legge WAL, costruisce CompletedActivityEntity,
 *     INSERT in `completed_activities`, DELETE WAL. Ritorna l'id locale.
 *  4. (opzionale) `discardTrack(trackId)` → DELETE WAL senza salvataggio.
 */
class TrackingPersistenceRepository(context: Context) {

  private val appContext = context.applicationContext
  private val db get() = (appContext as TsmApplication).database
  private val walDao get() = db.trackingWalDao()
  private val activityDao get() = db.completedActivityDao()
  private val gson = Gson()

  /**
   * Snapshot delle metriche finali fornite dalla VM al termine del tracking.
   * Contiene tutto ciò che serve per costruire [CompletedActivityEntity];
   * il tracciato GPS viene letto dalla WAL via [walDao].
   */
  data class FinalizeSnapshot(
    val trackId: String,
    val activeSessionId: String?,
    val activityName: String,
    val startTimeMs: Long,
    val movingSeconds: Long,
    val distanceMeters: Double,
    val elevationGainMeters: Int,
    val currentAltitudeMeters: Int?,
  )

  /** Genera un nuovo trackId e lo ritorna. Nessun side-effect di DB. */
  fun startTrack(): String = UUID.randomUUID().toString()

  /**
   * Aggiunge un singolo punto GPS alla WAL. Chiamato per ogni snapshot.
   * Non bloccante per la UI: la VM lo invoca dentro viewModelScope.
   */
  suspend fun appendPoint(
    trackId: String,
    latitude: Double,
    longitude: Double,
    altitudeMeters: Double?,
    timestampMs: Long,
  ) {
    walDao.insert(
      TrackingWalEntity(
        trackId = trackId,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        timestampMs = timestampMs,
      ),
    )
  }

  /** Conta i punti WAL per il trackId — utile per debug e per il recovery. */
  suspend fun countPoints(trackId: String): Int = walDao.countForTrack(trackId)

  /**
   * Finalizza il tracking: legge la WAL, sample dei punti (max 200) per la
   * persistenza compatta in [CompletedActivityEntity.trackLatLng], crea il
   * documento, e cancella la WAL per liberare spazio.
   *
   * @return id locale del [CompletedActivityEntity] appena creato.
   */
  suspend fun finalize(snapshot: FinalizeSnapshot): String {
    val walPoints = walDao.getAllForTrack(snapshot.trackId)
    val geoPoints = walPoints.map { GeoPoint(it.latitude, it.longitude, it.altitudeMeters ?: 0.0) }

    // Campionamento per il blob persistito (max 200 punti) — la WAL può avere
    // anche migliaia di punti su un'escursione lunga.
    val sampled = if (geoPoints.size > 200) {
      val step = geoPoints.size / 200
      geoPoints.filterIndexed { i, _ -> i % step == 0 }
    } else {
      geoPoints
    }
    val trackJson = gson.toJson(
      sampled.map { listOf(it.latitude, it.longitude, it.altitude) },
    )

    val distKm = snapshot.distanceMeters / 1000.0
    val actualH = snapshot.movingSeconds / 3600.0
    val points2 = HikeEstimation.finalPoints(distKm, snapshot.elevationGainMeters, actualH)
    val calories = (70 * distKm * 0.85).toInt()
    val now = System.currentTimeMillis()
    val startMs = if (snapshot.startTimeMs > 0) {
      snapshot.startTimeMs
    } else {
      now - snapshot.movingSeconds * 1000L
    }

    val newId = UUID.randomUUID().toString()
    activityDao.upsert(
      CompletedActivityEntity(
        id = newId,
        sessionId = snapshot.activeSessionId,
        name = snapshot.activityName,
        activityType = "hiking",
        startTimeMs = startMs,
        endTimeMs = now,
        movingSeconds = snapshot.movingSeconds,
        totalSeconds = snapshot.movingSeconds,
        distanceMeters = snapshot.distanceMeters,
        elevationGainMeters = snapshot.elevationGainMeters,
        currentAltitudeMeters = snapshot.currentAltitudeMeters,
        difficultyLevel = null,
        trackLatLng = trackJson,
        estimatedCalories = calories,
        points = points2,
        isSynced = false,
        completedAt = now,
      ),
    )

    // Cleanup WAL — i punti sono ora persistiti nella tabella final.
    walDao.clearForTrack(snapshot.trackId)
    return newId
  }

  /** Scarta il tracking senza salvarlo: cancella solo la WAL. */
  suspend fun discardTrack(trackId: String) {
    walDao.clearForTrack(trackId)
  }

  /**
   * Helper per generare il nome di default ("Escursione – 26 mag 2026")
   * quando l'utente non lo personalizza.
   */
  fun defaultActivityName(now: Date = Date()): String {
    val suffix = SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(now)
    return "Escursione – $suffix"
  }
}
