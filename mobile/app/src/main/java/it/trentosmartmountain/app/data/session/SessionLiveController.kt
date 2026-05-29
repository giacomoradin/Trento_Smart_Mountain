package it.trentosmartmountain.app.data.session

import android.content.Context
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import it.trentosmartmountain.app.repository.SessionCommandRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Orchestrazione azioni live sessione: stato locale + coordinator navigazione + PATCH server (solo capogruppo).
 */
class SessionLiveController(context: Context) {

    private val appContext = context.applicationContext
    private val commands = SessionCommandRepository(appContext)

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
        SessionStopCoordinator.requestStop(sessionId)
        scope.launch { commands.markSessionPlanned(sessionId) }
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
