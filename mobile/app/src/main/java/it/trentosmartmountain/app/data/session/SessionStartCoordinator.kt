package it.trentosmartmountain.app.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus singleton per coordinare il flusso "AVVIA da SessionDetail → Tab Registra → auto-start tracking".
 *
 * Flow:
 *   1. SessionDetailScreen.AVVIA → [requestStart(sessionId)]
 *   2. TsmNavHost popBackStack a MAIN_HIKER
 *   3. HikerMainScreen osserva [pendingSessionStart] → switcha selectedTab a Registra
 *   4. RegistraViewModel osserva [pendingSessionStart] → avvia tracking automatico
 *      (e patcha session.status = "ACTIVE" sul backend)
 *   5. RegistraViewModel chiama [consume] dopo aver gestito il segnale.
 *
 * Perché SharedFlow (replay=1) e non StateFlow:
 *   - StateFlow è conflated. Se VM consuma a null troppo presto, HikerMainScreen
 *     (collectAsStateWithLifecycle) può saltare il valore intermedio. Tab non switcha.
 *   - SharedFlow distribuisce ogni emit ad ogni subscriber indipendentemente. replay=1
 *     copre il caso "VM creato dopo l'emit" (es. prima volta che si apre la tab Registra).
 *   - consume() chiama resetReplayCache() per evitare retrigger su recomposition/rotation.
 *
 * Perché singleton: HikerMainScreen e RegistraViewModel sono in scope ViewModel diversi,
 * non condividono uno stesso ViewModelStoreOwner. Un Object/singleton è il pattern
 * più semplice per scambiare segnali brevi senza dependency injection esterna.
 */
object SessionStartCoordinator {
    private val _pendingSessionStart = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 1,
    )
    val pendingSessionStart: SharedFlow<String> = _pendingSessionStart.asSharedFlow()

    /** Chiamato da SessionDetailScreen / SessionHubScreen quando l'utente conferma AVVIA. */
    fun requestStart(sessionId: String) {
        _pendingSessionStart.tryEmit(sessionId)
    }

    /** Resetta il replay buffer dopo che il segnale è stato gestito (chiamato dal VM). */
    fun consume() {
        _pendingSessionStart.resetReplayCache()
    }
}
