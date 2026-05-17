package it.trentosmartmountain.app.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bus singleton per coordinare il flusso "AVVIA da SessionDetail → Tab Registra → auto-start tracking".
 *
 * Flow:
 *   1. SessionDetailScreen.AVVIA → [requestStart(sessionId)]
 *   2. TsmNavHost popBackStack a MAIN_HIKER
 *   3. HikerMainScreen osserva [pendingSessionStart] → switcha selectedTab a Registra
 *   4. RegistraViewModel osserva [pendingSessionStart] → avvia tracking automatico
 *      (e patcha session.status = "ACTIVE" sul backend)
 *   5. Consumer chiama [consume] dopo aver elaborato il segnale per evitare doppi trigger.
 *
 * Perché singleton: HikerMainScreen e RegistraViewModel sono in scope ViewModel diversi,
 * non condividono uno stesso ViewModelStoreOwner. Un Object/singleton è il pattern
 * più semplice per scambiare segnali brevi senza dependency injection esterna.
 */
object SessionStartCoordinator {
    private val _pendingSessionStart = MutableStateFlow<String?>(null)
    val pendingSessionStart: StateFlow<String?> = _pendingSessionStart.asStateFlow()

    /** Chiamato da SessionDetailScreen quando l'utente conferma AVVIA. */
    fun requestStart(sessionId: String) {
        _pendingSessionStart.value = sessionId
    }

    /** Consuma il segnale dopo averlo gestito (es. avviato il tracking). */
    fun consume() {
        _pendingSessionStart.value = null
    }
}
