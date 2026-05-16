package it.trentosmartmountain.app.data.location

/** Stato della macchina a stati del motore di registrazione traccia GPS. */
enum class TrackingStatus {
  IDLE,
  RECORDING,
  PAUSED,
}
