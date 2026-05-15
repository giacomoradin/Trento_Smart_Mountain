package it.trentosmartmountain.app.data.location

data class TrackPoint(
  val latitude: Double,
  val longitude: Double,
  val altitudeMeters: Double?,
  val timestampMs: Long,
)
