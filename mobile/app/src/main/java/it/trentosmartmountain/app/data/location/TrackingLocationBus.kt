package it.trentosmartmountain.app.data.location

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Posizioni GPS emesse dal [ForegroundTrackingService] durante la registrazione. */
object TrackingLocationBus {
  private val _locations = MutableSharedFlow<LocationSnapshot>(extraBufferCapacity = 32)
  val locations: SharedFlow<LocationSnapshot> = _locations.asSharedFlow()

  fun emit(snapshot: LocationSnapshot) {
    _locations.tryEmit(snapshot)
  }
}
