package it.trentosmartmountain.app.data.location

/** Singolo punto della traccia GPS registrata (coordinate WGS84 + quota opzionale). */
data class TrackPoint(
  val latitude: Double,
  val longitude: Double,
  val altitudeMeters: Double?,
  val timestampMs: Long,
)
