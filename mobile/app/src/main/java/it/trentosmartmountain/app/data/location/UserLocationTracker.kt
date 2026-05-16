package it.trentosmartmountain.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fix GPS normalizzato per UI e motori di tracking (indipendente da [android.location.Location]). */
data class LocationSnapshot(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float,
  val altitudeMeters: Double?,
  val speedMps: Float? = null,
  val timestampMs: Long = System.currentTimeMillis(),
)

/**
 * Aggiornamenti GPS ad alta precisione per la mappa Registra (foreground).
 */
class UserLocationTracker(context: Context) {

  private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
  private val _location = MutableStateFlow<LocationSnapshot?>(null)
  val location: StateFlow<LocationSnapshot?> = _location.asStateFlow()

  private var callback: LocationCallback? = null

  @SuppressLint("MissingPermission")
  fun start() {
    if (callback != null) return
    val request =
      LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
        .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
        .build()
    val locationCallback =
      object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          result.lastLocation?.let { publishLocation(it) }
        }
      }
    callback = locationCallback
    fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    fusedClient.lastLocation.addOnSuccessListener { loc ->
      if (loc != null) publishLocation(loc)
    }
  }

  fun stop() {
    callback?.let { fusedClient.removeLocationUpdates(it) }
    callback = null
  }

  private fun publishLocation(location: Location) {
    _location.value = location.toSnapshot()
  }

  private fun Location.toSnapshot() =
    LocationSnapshot(
      latitude = latitude,
      longitude = longitude,
      accuracyMeters = accuracy.coerceAtLeast(0f),
      altitudeMeters = altitude.takeIf { hasAltitude() },
      speedMps = speed.takeIf { hasSpeed() }?.coerceAtLeast(0f),
      timestampMs = time,
    )

  companion object {
    private const val UPDATE_INTERVAL_MS = 2_000L
    private const val MIN_UPDATE_INTERVAL_MS = 1_000L

    /**
     * Livello qualità segnale GPS (0–4) derivato dall'accuracia orizzontale in metri.
     * Usato dalla UI Registra per l'indicatore a barre.
     */
    fun gpsSignalLevel(accuracyMeters: Float): Int =
      when {
        accuracyMeters <= 0f -> 0
        accuracyMeters < 10f -> 4
        accuracyMeters < 25f -> 3
        accuracyMeters < 50f -> 2
        accuracyMeters < 100f -> 1
        else -> 0
      }
  }
}
