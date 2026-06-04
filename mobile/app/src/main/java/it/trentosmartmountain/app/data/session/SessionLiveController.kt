package it.trentosmartmountain.app.data.session

import android.content.Context
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import kotlinx.coroutines.CoroutineScope

/**
 * Orchestrazione azioni live sessione: stato locale + coordinator navigazione.
 */
class SessionLiveController(context: Context) {

    private val appContext = context.applicationContext

    fun localState(sessionId: String): UserSessionLiveState =
        SessionLiveStateStore.getState(appContext, sessionId)

    fun snapshot(): Map<String, UserSessionLiveState> =
        SessionLiveStateStore.snapshot(appContext)

    fun reconcile(sessions: List<SessionResponse>) {
        SessionLiveStateStore.reconcileWithSessions(appContext, sessions)
    }

    fun leaderStart(sessionId: String) {
        SessionLiveStateStore.setState(appContext, sessionId, UserSessionLiveState.IN_GROUP_LIVE)
        SessionStartCoordinator.requestStart(sessionId)
    }

    fun leaderStop(scope: CoroutineScope, sessionId: String) {
        SessionLiveStateStore.setState(appContext, sessionId, UserSessionLiveState.NOT_IN_LIVE)
        // NB: NON riportiamo più la sessione a PLANNED qui. Prima `markSessionPlanned`
        // (ACTIVE→PLANNED) correva contro `completeSession` (→COMPLETED) innescato dallo
        // stop del tracking, lasciando la sessione in uno stato incoerente — la causa
        // del "non riesco a terminare la sessione in solitaria". Ora lo stop passa per
        // un'unica strada: requestStop → confirmStopTracking → completeSession, che con
        // il modello Ibrido chiude pulito (solo o ultimo membro → COMPLETED).
        SessionStopCoordinator.requestStop(sessionId)
    }

    fun joinLive(sessionId: String) {
        SessionLiveStateStore.setState(appContext, sessionId, UserSessionLiveState.IN_GROUP_LIVE)
        SessionStartCoordinator.requestStart(sessionId)
    }

    fun leaveLive(sessionId: String) {
        SessionLiveStateStore.setState(appContext, sessionId, UserSessionLiveState.LEFT_LIVE)
        SessionStopCoordinator.requestStop(sessionId)
    }

    fun startSoloPractice(sessionId: String) {
        SessionLiveStateStore.setState(appContext, sessionId, UserSessionLiveState.SOLO_PRACTICE)
        SessionStartCoordinator.requestStart(sessionId)
    }

    fun endSoloPractice(sessionId: String) {
        SessionLiveStateStore.setState(appContext, sessionId, UserSessionLiveState.NOT_IN_LIVE)
        SessionStopCoordinator.requestStop(sessionId)
    }

    fun clearOnSessionRemoved(sessionId: String) {
        SessionLiveStateStore.remove(appContext, sessionId)
        SessionStopCoordinator.requestStop(sessionId)
    }
}
