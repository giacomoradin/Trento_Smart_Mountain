package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.JoinSessionRequest
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import it.trentosmartmountain.app.data.session.SessionLiveController
import it.trentosmartmountain.app.data.session.SessionParticipationResolver
import it.trentosmartmountain.app.data.session.SessionParticipationUi
import it.trentosmartmountain.app.data.session.UserSessionLiveState
import it.trentosmartmountain.app.ui.util.SessionDateFormats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Tab **Unisciti** in [SessionHubScreen]: join per codice invito e gestione lista sessioni dell'utente.
 */
class SessionJoinViewModel(application: Application) : AndroidViewModel(application) {

    enum class RemovalMode { LEAVE, DELETE }

    data class UiState(
        val joinCode: String = "TSM-",
        val sessions: List<SessionResponse> = emptyList(),
        val liveStates: Map<String, UserSessionLiveState> = emptyMap(),
        val isLoadingSessions: Boolean = false,
        val isJoining: Boolean = false,
        val joinError: String? = null,
        /** Messaggio informativo dopo un join riuscito (richiesta in attesa di approvazione). */
        val joinInfo: String? = null,
        val generalError: String? = null,
        val removeConfirm: RemovalRequest? = null,
        val isRemoving: Boolean = false,
        val currentUserId: String = "",
        val avviaConfirmSessionId: String? = null,
    )

    data class RemovalRequest(val sessionId: String, val mode: RemovalMode)

    private val liveController = SessionLiveController(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val token = (getApplication() as TsmApplication).tokenStorage.getToken()
        val userId = token?.let { JwtDecoder.userIdFrom(it) } ?: ""
        _uiState.update { it.copy(currentUserId = userId) }
        loadSessions()
    }

    fun participationUi(session: SessionResponse): SessionParticipationUi {
        val isCreator = session.creatorId?._id == _uiState.value.currentUserId
        val local = _uiState.value.liveStates[session._id] ?: UserSessionLiveState.NOT_IN_LIVE
        return SessionParticipationResolver.resolve(session, isCreator, local)
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSessions = true, generalError = null) }
            try {
                val response = TsmApiClient.service().getMySessions()
                if (response.isSuccessful) {
                    val sorted = (response.body() ?: emptyList())
                        .filter { it.status == "PLANNED" || it.status == "ACTIVE" }
                        .sortedBy { it.meetingDate ?: "" }
                    liveController.reconcile(sorted)
                    _uiState.update {
                        it.copy(
                            isLoadingSessions = false,
                            sessions = sorted,
                            liveStates = liveController.snapshot(),
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingSessions = false,
                            generalError = "Errore server (${response.code()}). Riprova.",
                        )
                    }
                }
            } catch (_: IOException) {
                _uiState.update { it.copy(isLoadingSessions = false, generalError = "Nessuna connessione al server.") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingSessions = false,
                        generalError = "Errore nel parsing dei dati: ${e.javaClass.simpleName}",
                    )
                }
            }
        }
    }

    fun onJoinCodeChange(rawInput: String) {
        val cleaned = rawInput.uppercase().filter { it.isLetterOrDigit() }
        val trailing = when {
            cleaned.startsWith("TSM") -> cleaned.substring(3).take(4)
            else -> cleaned.take(4)
        }
        _uiState.update { it.copy(joinCode = "TSM-$trailing", joinError = null, joinInfo = null) }
    }

    fun onJoinSession() {
        val code = _uiState.value.joinCode.trim()
        if (code.length != 8) {
            _uiState.update { it.copy(joinError = "Codice incompleto (TSM-XXXX).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, joinError = null) }
            try {
                val response = TsmApiClient.service().joinSession(JoinSessionRequest(code))
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            joinCode = "TSM-",
                            joinInfo = "Richiesta inviata: in attesa di approvazione del capogruppo.",
                        )
                    }
                    loadSessions()
                } else {
                    val error = when (response.code()) {
                        404 -> "Codice non trovato."
                        409 -> "Sei già in questa sessione o in un'altra attiva."
                        else -> "Errore server (${response.code()})."
                    }
                    _uiState.update { it.copy(isJoining = false, joinError = error) }
                }
            } catch (_: IOException) {
                _uiState.update { it.copy(isJoining = false, joinError = "Nessuna connessione.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isJoining = false, joinError = "Errore: ${e.javaClass.simpleName}") }
            }
        }
    }

    fun requestRemoveSession(session: SessionResponse) {
        val mode = if (session.creatorId?._id == _uiState.value.currentUserId) {
            RemovalMode.DELETE
        } else {
            RemovalMode.LEAVE
        }
        _uiState.update { it.copy(removeConfirm = RemovalRequest(session._id, mode)) }
    }

    fun dismissRemoveConfirm() {
        _uiState.update { it.copy(removeConfirm = null) }
    }

    fun confirmRemoveSession() {
        val request = _uiState.value.removeConfirm ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRemoving = true, removeConfirm = null) }
            try {
                val response = when (request.mode) {
                    RemovalMode.LEAVE -> TsmApiClient.service().leaveSession(request.sessionId)
                    RemovalMode.DELETE -> TsmApiClient.service().deleteSession(request.sessionId)
                }
                if (!response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isRemoving = false,
                            generalError = "Errore (${response.code()}) durante la rimozione.",
                        )
                    }
                } else {
                    liveController.clearOnSessionRemoved(request.sessionId)
                    _uiState.update { it.copy(isRemoving = false, liveStates = liveController.snapshot()) }
                }
                loadSessions()
            } catch (_: IOException) {
                _uiState.update { it.copy(isRemoving = false, generalError = "Nessuna connessione.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRemoving = false, generalError = "Errore: ${e.javaClass.simpleName}") }
            }
        }
    }

    fun requestLeaderStart(session: SessionResponse) {
        if (SessionDateFormats.isTodayApi(session.meetingDate)) {
            liveController.leaderStart(session._id)
            refreshLiveStates()
        } else {
            _uiState.update { it.copy(avviaConfirmSessionId = session._id) }
        }
    }

    fun confirmLeaderStartEarly() {
        val id = _uiState.value.avviaConfirmSessionId ?: return
        liveController.leaderStart(id)
        _uiState.update { it.copy(avviaConfirmSessionId = null) }
        refreshLiveStates()
    }

    fun dismissAvviaConfirm() {
        _uiState.update { it.copy(avviaConfirmSessionId = null) }
    }

    fun leaderStop(sessionId: String) {
        liveController.leaderStop(viewModelScope, sessionId)
        refreshLiveStates()
        loadSessions()
    }

    fun joinLive(sessionId: String) {
        liveController.joinLive(sessionId)
        refreshLiveStates()
    }

    fun leaveLive(sessionId: String) {
        val local = _uiState.value.liveStates[sessionId]
        if (local == UserSessionLiveState.SOLO_PRACTICE) {
            liveController.endSoloPractice(sessionId)
        } else {
            liveController.leaveLive(sessionId)
        }
        refreshLiveStates()
    }

    fun startSoloPractice(sessionId: String) {
        liveController.startSoloPractice(sessionId)
        refreshLiveStates()
    }

    private fun refreshLiveStates() {
        _uiState.update { it.copy(liveStates = liveController.snapshot()) }
    }
}
