package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.FollowListEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Quale lista mostrare: i follower di un utente o gli utenti che esso segue. */
enum class FollowListType { FOLLOWERS, FOLLOWING }

/**
 * Stato della [it.trentosmartmountain.app.ui.screens.home.FollowListScreen].
 *
 * Funziona per QUALSIASI utente (incluso se stessi): gli endpoint
 * `/users/:id/followers` e `/users/:id/following` accettano un id arbitrario.
 */
data class FollowListState(
    val userId: String = "",
    val type: FollowListType = FollowListType.FOLLOWERS,
    val entries: List<FollowListEntry> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val page: Int = 1,
    val error: String? = null,
)

/**
 * ViewModel per la lista follower/seguiti, riusabile via [load].
 *
 * Paginazione: `hasMore` derivato confrontando gli elementi accumulati col
 * `count` totale ritornato dal backend. Tollerante agli errori di rete:
 * mostra un messaggio ma conserva gli elementi già caricati.
 */
class FollowListViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()
    private val _state = MutableStateFlow(FollowListState())
    val state: StateFlow<FollowListState> = _state.asStateFlow()

    /** (Ri)carica dalla pagina 1 per il dato utente + tipo. Idempotente. */
    fun load(userId: String, type: FollowListType) {
        if (userId.isBlank()) return
        _state.value = FollowListState(userId = userId, type = type, isLoading = true)
        viewModelScope.launch { fetch(page = 1, append = false) }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || !s.hasMore || s.userId.isBlank()) return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch { fetch(page = s.page + 1, append = true) }
    }

    private suspend fun fetch(page: Int, append: Boolean) {
        val s = _state.value
        val result = runCatching {
            when (s.type) {
                FollowListType.FOLLOWERS -> api.getUserFollowers(s.userId, page, PAGE_SIZE)
                FollowListType.FOLLOWING -> api.getUserFollowing(s.userId, page, PAGE_SIZE)
            }
        }.getOrNull()

        if (result == null || !result.isSuccessful) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (it.entries.isEmpty()) "Impossibile caricare la lista." else it.error,
                )
            }
            return
        }

        val body = result.body()
        val newEntries = body?.items.orEmpty()
        val total = body?.count ?: 0
        _state.update {
            val merged = if (append) it.entries + newEntries else newEntries
            it.copy(
                entries = merged,
                total = total,
                isLoading = false,
                isLoadingMore = false,
                page = page,
                // hasMore: abbiamo accumulato meno del totale E l'ultima pagina
                // non era vuota (difensivo contro count incoerenti).
                hasMore = merged.size < total && newEntries.isNotEmpty(),
                error = null,
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
