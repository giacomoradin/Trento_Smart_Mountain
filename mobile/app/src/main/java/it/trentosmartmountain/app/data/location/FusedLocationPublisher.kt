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

/**
 * Wrapper su Fused Location Provider per aggiornamenti GPS ad alta precisione.
 *
 * Usato dal [ForegroundTrackingService] durante la registrazione; espone callback
 * sincroni invece di Flow per ridurre overhead nel servizio foreground.
 */
internal class FusedLocationPublisher(context: Context) {

  private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
  private var callback: LocationCallback? = null

  @SuppressLint("MissingPermission")
  fun start(
    intervalMs: Long,
    minIntervalMs: Long,
    onLocation: (LocationSnapshot) -> Unit,
  ) {
    if (callback != null) return
    val request =
      LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
        .setMinUpdateIntervalMillis(minIntervalMs)
        .build()
    val locationCallback =
      object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          result.lastLocation?.let { onLocation(it.toSnapshot()) }
        }
      }
    callback = locationCallback
    fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    // Ultima posizione nota: fix immediato sulla mappa prima del primo callback periodico
    fusedClient.lastLocation.addOnSuccessListener { loc ->
      if (loc != null) onLocation(loc.toSnapshot())
    }
  }

  fun stop() {
    callback?.let { fusedClient.removeLocationUpdates(it) }
    callback = null
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
}
