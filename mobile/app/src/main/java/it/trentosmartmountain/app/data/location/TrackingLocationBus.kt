package it.trentosmartmountain.app.data.location

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Posizioni GPS emesse dal [ForegroundTrackingService] durante la registrazione. */
object TrackingLocationBus {
  private val _locations = MutableSharedFlow<LocationSnapshot>(extraBufferCapacity = 32)
  val locations: SharedFlow<LocationSnapshot> = _locations.asSharedFlow()

  /** Pubblica un fix GPS; `tryEmit` evita blocco se nessun collector è attivo. */
  fun emit(snapshot: LocationSnapshot) {
    _locations.tryEmit(snapshot)
  }
}
