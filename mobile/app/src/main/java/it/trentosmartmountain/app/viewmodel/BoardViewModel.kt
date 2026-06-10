package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.BoardPost
import it.trentosmartmountain.app.data.remote.dto.CreateBoardPostRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Stato della Bacheca rifugi.
 *
 *  - [manage] true = vista rifugista (i propri post, con crea/elimina);
 *             false = feed di consultazione (tutti i post, sola lettura).
 */
data class BoardState(
    val manage: Boolean = false,
    val items: List<BoardPost> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/** ViewModel della bacheca: usato sia lato utente (feed) sia lato rifugista (gestione). */
class BoardViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()
    private val _state = MutableStateFlow(BoardState())
    val state: StateFlow<BoardState> = _state.asStateFlow()

    private val appContext get() = getApplication<Application>().applicationContext

    fun load(manage: Boolean) {
        _state.update { it.copy(manage = manage, isLoading = true, error = null) }
        viewModelScope.launch {
            val resp = runCatching {
                if (manage) api.getMyBoardPosts(1, 50) else api.getBoardPosts(1, 50, null)
            }.getOrNull()
            if (resp != null && resp.isSuccessful) {
                val all = resp.body()?.items.orEmpty()
                // In consultazione (lato utente) filtriamo gli avvisi che l'utente
                // ha rimosso dalla propria vista (hide locale per-dispositivo).
                val visible =
                    if (manage) all
                    else {
                        val dismissed = it.trentosmartmountain.app.data.local
                            .DismissedBoardStore.dismissed(appContext)
                        all.filterNot { p -> p._id in dismissed }
                    }
                _state.update { it.copy(isLoading = false, items = visible) }
            } else {
                _state.update { it.copy(isLoading = false, error = "Impossibile caricare la bacheca.") }
            }
        }
    }

    /**
     * Rimuove un avviso dalla vista di consultazione dell'utente (hide locale).
     * Non cancella il post sul server (appartiene al rifugista).
     */
    fun dismissForUser(id: String) {
        it.trentosmartmountain.app.data.local.DismissedBoardStore.dismiss(appContext, id)
        _state.update { it.copy(items = it.items.filterNot { p -> p._id == id }) }
    }

    /** Rimuove TUTTI gli avvisi correnti dalla vista dell'utente (hide locale). */
    fun dismissAllForUser() {
        val ids = _state.value.items.map { it._id }
        it.trentosmartmountain.app.data.local.DismissedBoardStore.dismissAll(appContext, ids)
        _state.update { it.copy(items = emptyList()) }
    }

    /** Crea un post (lato rifugista). `onDone` chiude il dialog su successo. */
    fun create(type: String, title: String, body: String, validUntil: String?, onDone: () -> Unit) {
        if (title.isBlank() || body.isBlank()) {
            _state.update { it.copy(error = "Inserisci titolo e testo.") }
            return
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val resp = runCatching {
                api.createBoardPost(CreateBoardPostRequest(type, title.trim(), body.trim(), validUntil))
            }.getOrNull()
            if (resp != null && resp.isSuccessful) {
                _state.update { it.copy(isSubmitting = false, message = "Pubblicato in bacheca.") }
                onDone()
                load(true)
            } else {
                _state.update { it.copy(isSubmitting = false, error = "Pubblicazione non riuscita.") }
            }
        }
    }

    /** Modifica un proprio post (lato rifugista). `onDone` chiude il dialog. */
    fun update(id: String, type: String, title: String, body: String, validUntil: String?, onDone: () -> Unit) {
        if (title.isBlank() || body.isBlank()) {
            _state.update { it.copy(error = "Inserisci titolo e testo.") }
            return
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val resp = runCatching {
                api.updateBoardPost(id, CreateBoardPostRequest(type, title.trim(), body.trim(), validUntil))
            }.getOrNull()
            if (resp != null && resp.isSuccessful) {
                _state.update { it.copy(isSubmitting = false, message = "Post aggiornato.") }
                onDone()
                load(true)
            } else {
                _state.update { it.copy(isSubmitting = false, error = "Aggiornamento non riuscito.") }
            }
        }
    }

    /** Elimina un proprio post (ottimistico, rollback su errore). */
    fun delete(id: String) {
        val prev = _state.value.items
        _state.update { it.copy(items = it.items.filterNot { p -> p._id == id }) }
        viewModelScope.launch {
            val ok = runCatching { api.deleteBoardPost(id) }.getOrNull()?.isSuccessful == true
            if (!ok) {
                _state.update { it.copy(items = prev, error = "Eliminazione non riuscita.") }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, message = null) }
    }
}
