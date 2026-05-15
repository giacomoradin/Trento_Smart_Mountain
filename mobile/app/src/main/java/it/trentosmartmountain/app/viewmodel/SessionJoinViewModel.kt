package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.JoinSessionRequest
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class SessionJoinViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Modalità di rimozione da una sessione:
     * - LEAVE: partecipante non-creator → POST /:id/leave (rimane la sessione, esce solo lui)
     * - DELETE: creator → DELETE /:id (rimuove l'intera sessione anche per i partecipanti)
     */
    enum class RemovalMode { LEAVE, DELETE }

    data class UiState(
        val joinCode: String = "TSM-",
        val sessions: List<SessionResponse> = emptyList(),
        val isLoadingSessions: Boolean = false,
        val isJoining: Boolean = false,
        val joinError: String? = null,
        val generalError: String? = null,
        val removeConfirm: RemovalRequest? = null,
        val isRemoving: Boolean = false,
        val currentUserId: String = "",
    )

    data class RemovalRequest(val sessionId: String, val mode: RemovalMode)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Estrai userId dal JWT per il check creator/partecipante
        val token = (getApplication() as TsmApplication).tokenStorage.getToken()
        val userId = token?.let { JwtDecoder.userIdFrom(it) } ?: ""
        _uiState.update { it.copy(currentUserId = userId) }
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSessions = true, generalError = null) }
            try {
                val response = TsmApiClient.service().getMySessions()
                if (response.isSuccessful) {
                    val sorted = (response.body() ?: emptyList()).sortedBy { it.meetingDate ?: "" }
                    _uiState.update { it.copy(isLoadingSessions = false, sessions = sorted) }
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
        _uiState.update { it.copy(joinCode = "TSM-$trailing", joinError = null) }
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
                    _uiState.update { it.copy(isJoining = false, joinCode = "TSM-") }
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

    /**
     * Richiede conferma per rimuovere la sessione. La modalità (LEAVE vs DELETE)
     * è determinata dal ruolo dell'utente sulla sessione specifica.
     */
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
                    _uiState.update { it.copy(isRemoving = false) }
                }
                loadSessions()
            } catch (_: IOException) {
                _uiState.update { it.copy(isRemoving = false, generalError = "Nessuna connessione.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRemoving = false, generalError = "Errore: ${e.javaClass.simpleName}") }
            }
        }
    }
}
