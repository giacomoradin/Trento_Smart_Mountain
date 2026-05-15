package it.trentosmartmountain.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.JoinSessionRequest
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class SessionJoinViewModel : ViewModel() {

    data class UiState(
        val joinCode: String = "",
        val sessions: List<SessionResponse> = emptyList(),
        val isLoadingSessions: Boolean = false,
        val isJoining: Boolean = false,
        val joinError: String? = null,
        val generalError: String? = null,
        val leaveConfirmSessionId: String? = null,
        val isLeaving: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { loadSessions() }

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
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoadingSessions = false, generalError = "Nessuna connessione al server.") }
            } catch (e: Exception) {
                // JsonSyntaxException o altri errori di deserializzazione: esponiamo il messaggio
                // invece di inghiottirlo silenziosamente (error-swallowing bug).
                _uiState.update {
                    it.copy(
                        isLoadingSessions = false,
                        generalError = "Errore nel parsing dei dati: ${e.javaClass.simpleName}",
                    )
                }
            }
        }
    }

    fun onJoinCodeChange(value: String) {
        _uiState.update { it.copy(joinCode = value.uppercase(), joinError = null) }
    }

    fun onJoinSession() {
        val code = _uiState.value.joinCode.trim()
        if (code.length < 4) {
            _uiState.update { it.copy(joinError = "Codice non valido.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, joinError = null) }
            try {
                val response = TsmApiClient.service().joinSession(JoinSessionRequest(code))
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isJoining = false, joinCode = "") }
                    loadSessions()
                } else {
                    val error = when (response.code()) {
                        404 -> "Codice non trovato."
                        409 -> "Sei già in questa sessione o in un'altra attiva."
                        else -> "Errore server (${response.code()})."
                    }
                    _uiState.update { it.copy(isJoining = false, joinError = error) }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isJoining = false, joinError = "Nessuna connessione.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isJoining = false, joinError = "Errore: ${e.javaClass.simpleName}") }
            }
        }
    }

    fun requestLeaveSession(sessionId: String) {
        _uiState.update { it.copy(leaveConfirmSessionId = sessionId) }
    }

    fun dismissLeaveConfirm() {
        _uiState.update { it.copy(leaveConfirmSessionId = null) }
    }

    fun confirmLeaveSession() {
        val sessionId = _uiState.value.leaveConfirmSessionId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLeaving = true, leaveConfirmSessionId = null) }
            try {
                TsmApiClient.service().leaveSession(sessionId)
                _uiState.update { it.copy(isLeaving = false) }
                loadSessions()
            } catch (e: IOException) {
                _uiState.update { it.copy(isLeaving = false, generalError = "Nessuna connessione.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLeaving = false, generalError = "Errore: ${e.javaClass.simpleName}") }
            }
        }
    }
}
