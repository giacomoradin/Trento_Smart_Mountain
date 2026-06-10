package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.UserSearchItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Stato della schermata "Cerca persone da seguire".
 *
 *  - [query]: testo corrente del campo di ricerca
 *  - [results]: risultati dell'ultima ricerca (con `isFollowedByMe`)
 *  - [isLoading]: ricerca in volo (dopo il debounce)
 *  - [hasSearched]: true dopo la prima ricerca valida → distingue lo stato
 *                   iniziale ("digita per cercare") dall'empty ("nessun risultato")
 *  - [followInFlight]: id utenti col toggle follow in volo (bottone disabilitato)
 *  - [error]: messaggio user-facing dell'ultima op fallita
 */
data class UserSearchState(
    val query: String = "",
    val results: List<UserSearchItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val followInFlight: Set<String> = emptySet(),
    val error: String? = null,
)

/**
 * ViewModel della ricerca utenti (flusso "aggiungi amici").
 *
 * Debounce 300ms: ogni keystroke cancella la ricerca precedente e ne pianifica
 * una nuova, così non martelliamo `GET /users/search` a ogni lettera. Termine
 * < 2 caratteri → nessuna chiamata (coerente col gate lato server) e risultati
 * azzerati.
 *
 * Follow ottimistico: il bottone passa subito a "Seguito"/"Segui" e fa rollback
 * se il server risponde errore. `followInFlight` evita doppi tap.
 */
class UserSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()
    private val _state = MutableStateFlow(UserSearchState())
    val state: StateFlow<UserSearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        val trimmed = q.trim()
        if (trimmed.length < 2) {
            _state.update {
                it.copy(results = emptyList(), isLoading = false, hasSearched = false, error = null)
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { api.searchUsers(trimmed) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                hasSearched = true,
                                results = resp.body()?.items.orEmpty(),
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(isLoading = false, hasSearched = true, error = "Ricerca non riuscita (${resp.code()}).")
                        }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, hasSearched = true, error = e.message ?: "Errore di rete.")
                    }
                }
        }
    }

    /** Toggle Segui/Smetti ottimistico su un risultato di ricerca. */
    fun toggleFollow(item: UserSearchItem) {
        val userId = item.user?._id ?: return
        if (userId in _state.value.followInFlight) return
        val willFollow = !item.isFollowedByMe
        _state.update { st ->
            st.copy(
                results = st.results.map {
                    if (it.user?._id == userId) it.copy(isFollowedByMe = willFollow) else it
                },
                followInFlight = st.followInFlight + userId,
            )
        }
        viewModelScope.launch {
            val ok = runCatching {
                if (willFollow) api.followUser(userId) else api.unfollowUser(userId)
            }.getOrNull()?.isSuccessful == true
            _state.update { st ->
                st.copy(
                    // Rollback se l'op è fallita.
                    results = if (ok) st.results else st.results.map {
                        if (it.user?._id == userId) it.copy(isFollowedByMe = !willFollow) else it
                    },
                    followInFlight = st.followInFlight - userId,
                    error = if (ok) st.error else "Operazione non riuscita.",
                )
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
