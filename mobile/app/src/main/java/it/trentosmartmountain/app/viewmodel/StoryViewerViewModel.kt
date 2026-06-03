package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.JoinSessionRequest
import it.trentosmartmountain.app.data.remote.dto.StoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * VM del visualizzatore storie (rework Fase C). Carica le storie reali di un
 * autore via `/stories/user/:id`, marca le viste e gestisce l'unione (join) da
 * una storia di sessione pianificata.
 */
class StoryViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()

    data class UiState(
        val isLoading: Boolean = true,
        val stories: List<StoryItem> = emptyList(),
        val error: String? = null,
        /** Feedback dopo il tap su "Unisciti" da una storia planned_session. */
        val joinInfo: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadedUserId: String? = null

    /** Idempotente sull'autore: evita ricariche a ogni recomposition. */
    fun load(userId: String) {
        if (loadedUserId == userId) return
        loadedUserId = userId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { api.getStoriesByUser(userId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.update {
                            it.copy(isLoading = false, stories = resp.body()?.items ?: emptyList())
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Storie non disponibili.") }
                    }
                }
                .onFailure { _state.update { it.copy(isLoading = false, error = "Errore di rete.") } }
        }
    }

    /** Marca la storia come vista (best-effort, idempotente lato server). */
    fun markViewed(storyId: String) {
        viewModelScope.launch { runCatching { api.markStoryViewed(storyId) } }
    }

    /** Unione alla sessione pianificata condivisa nella storia (→ richiesta pending). */
    fun joinSession(inviteCode: String) {
        viewModelScope.launch {
            val ok = runCatching { api.joinSession(JoinSessionRequest(inviteCode)) }
                .getOrNull()
                ?.isSuccessful == true
            _state.update {
                it.copy(
                    joinInfo = if (ok) {
                        "Richiesta inviata: in attesa di approvazione."
                    } else {
                        "Impossibile inviare la richiesta."
                    },
                )
            }
        }
    }

    fun clearJoinInfo() = _state.update { it.copy(joinInfo = null) }
}
