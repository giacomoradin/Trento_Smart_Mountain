package it.trentosmartmountain.app.data.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class TrackingMetrics(
  val distanceMeters: Double,
  val elevationGainMeters: Int,
  val trackPoints: List<TrackPoint>,
)

/**
 * Accumula punti traccia e metriche mentre lo stato è [TrackingStatus.RECORDING].
 */
class HikeTrackingEngine {

  var status: TrackingStatus = TrackingStatus.IDLE
    private set

  var isAutoPaused: Boolean = false
    private set

  private val points = mutableListOf<TrackPoint>()
  private var lastAcceptedPoint: TrackPoint? = null
  private var totalDistanceM = 0.0
  private var elevationGainM = 0

  val trackPoints: List<TrackPoint> get() = points.toList()

  fun start() {
    status = TrackingStatus.RECORDING
    isAutoPaused = false
    points.clear()
    lastAcceptedPoint = null
    totalDistanceM = 0.0
    elevationGainM = 0
  }

  fun pause(manual: Boolean) {
    status = TrackingStatus.PAUSED
    isAutoPaused = !manual
  }

  fun resume() {
    status = TrackingStatus.RECORDING
    isAutoPaused = false
  }

  fun stop(): TrackingMetrics {
    val result = snapshot()
    status = TrackingStatus.IDLE
    isAutoPaused = false
    return result
  }

  fun onLocation(snapshot: LocationSnapshot): TrackingMetrics {
    if (status == TrackingStatus.RECORDING) {
      maybeAcceptPoint(snapshot)
    }
    return snapshot()
  }

  private fun maybeAcceptPoint(snapshot: LocationSnapshot) {
    val candidate =
      TrackPoint(
        latitude = snapshot.latitude,
        longitude = snapshot.longitude,
        altitudeMeters = snapshot.altitudeMeters,
        timestampMs = snapshot.timestampMs,
      )
    val last = lastAcceptedPoint
    if (last == null) {
      accept(candidate)
      return
    }
    val distanceM = haversineM(last, candidate)
    val elapsedMs = candidate.timestampMs - last.timestampMs
    if (distanceM >= MIN_POINT_DISTANCE_M || elapsedMs >= MAX_POINT_INTERVAL_MS) {
      accept(candidate, segmentMeters = distanceM)
    }
  }

  private fun accept(point: TrackPoint, segmentMeters: Double = 0.0) {
    val prev = lastAcceptedPoint
    if (prev != null && segmentMeters > 0) {
      totalDistanceM += segmentMeters
      val prevEle = prev.altitudeMeters
      val currEle = point.altitudeMeters
      if (prevEle != null && currEle != null && currEle > prevEle) {
        elevationGainM += (currEle - prevEle).toInt()
      }
    }
    points.add(point)
    lastAcceptedPoint = point
  }

  private fun snapshot() =
    TrackingMetrics(
      distanceMeters = totalDistanceM,
      elevationGainMeters = elevationGainM,
      trackPoints = points.toList(),
    )

  private fun haversineM(a: TrackPoint, b: TrackPoint): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h =
      sin(dLat / 2).pow(2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(h), sqrt(1 - h))
  }

  companion object {
    private const val MIN_POINT_DISTANCE_M = 5.0
    private const val MAX_POINT_INTERVAL_MS = 8_000L
  }
}
