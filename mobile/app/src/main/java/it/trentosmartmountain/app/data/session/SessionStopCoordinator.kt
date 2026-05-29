package it.trentosmartmountain.app.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus per fermare il tracking live su tab Registra senza uscire dalla sessione di gruppo.
 * Usato da "Arresta per me" e da "Arresta" del capogruppo.
 */
object SessionStopCoordinator {
    private val _pendingSessionStop = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 1,
    )
    val pendingSessionStop: SharedFlow<String> = _pendingSessionStop.asSharedFlow()

    fun requestStop(sessionId: String) {
        _pendingSessionStop.tryEmit(sessionId)
    }

    fun consume() {
        _pendingSessionStop.resetReplayCache()
    }
}
