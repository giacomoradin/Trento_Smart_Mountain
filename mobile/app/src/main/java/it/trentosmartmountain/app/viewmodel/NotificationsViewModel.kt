package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Stato del centro notifiche (lista paginata). */
data class NotificationsState(
    val items: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val page: Int = 1,
    val error: String? = null,
)

/**
 * ViewModel del centro notifiche.
 *
 * All'apertura carica la pagina 1 e marca TUTTE le notifiche come lette lato
 * server (`markNotificationsRead`). Le righe mostrano comunque l'evidenziazione
 * "non letta" per QUESTA visualizzazione, in base ai flag `read` ricevuti: alla
 * prossima apertura saranno lette. Il badge sul feed viene azzerato dal
 * chiamante (vedi [SocialFeedViewModel.clearNotificationBadge]).
 */
class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.getNotifications(1, PAGE_SIZE) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        _state.update {
                            it.copy(
                                isLoading = false,
                                items = body?.items.orEmpty(),
                                hasMore = body?.hasMore == true,
                                page = 1,
                            )
                        }
                        // Marca lette (best-effort): non blocca la UI.
                        runCatching { api.markNotificationsRead() }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Impossibile caricare le notifiche.") }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Errore di rete.") }
                }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || !s.hasMore) return
        val next = s.page + 1
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            runCatching { api.getNotifications(next, PAGE_SIZE) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        _state.update {
                            it.copy(
                                isLoadingMore = false,
                                items = it.items + body?.items.orEmpty(),
                                hasMore = body?.hasMore == true,
                                page = next,
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }

    /** Pull-to-refresh / tasto aggiorna. */
    fun refresh() = load()

    /**
     * Elimina una notifica (ottimistico: la togliamo subito dalla lista). Le
     * notifiche dinamiche (id con prefisso reminder:/alert:) non esistono lato
     * server → rimozione solo locale.
     */
    fun delete(id: String) {
        val isSynthetic = id.contains(":")
        _state.update { it.copy(items = it.items.filterNot { n -> n._id == id }) }
        if (!isSynthetic) {
            viewModelScope.launch { runCatching { api.deleteNotification(id) } }
        }
    }

    /** Elimina TUTTE le notifiche (svuota la vista e cancella lato server). */
    fun deleteAll() {
        _state.update { it.copy(items = emptyList(), hasMore = false) }
        viewModelScope.launch { runCatching { api.deleteAllNotifications() } }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
