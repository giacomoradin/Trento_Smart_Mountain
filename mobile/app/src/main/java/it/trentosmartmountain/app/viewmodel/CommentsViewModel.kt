package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.dto.CommentItem
import it.trentosmartmountain.app.data.remote.dto.CreateCommentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stato dei commenti per un singolo target (Activity o HikeSession).
 *
 *  - [targetId]          id del parent currentemente caricato (può essere null
 *                        finché la BottomSheet non viene aperta)
 *  - [kind]              "activity" oppure "session" — determina gli endpoint
 *  - [items]             lista commenti (più recente in cima)
 *  - [count]             totale dal server (può essere > items.size se paginato)
 *  - [isLoading]         load iniziale o pull-to-refresh
 *  - [isSending]         POST in corso (disabilita bottone send + input)
 *  - [draft]             testo che l'utente sta digitando
 *  - [error]             messaggio user-facing dell'ultimo errore
 *  - [currentUserId]     usato per decidere se mostrare il bottone "elimina"
 *                        accanto a un commento (lo confrontiamo con userId.author)
 */
data class CommentsState(
    val targetId: String? = null,
    val kind: String = "activity",
    val items: List<CommentItem> = emptyList(),
    val count: Int = 0,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val draft: String = "",
    val error: String? = null,
    val currentUserId: String? = null,
)

/**
 * ViewModel della BottomSheet commenti.
 *
 * Single-target by design: chi apre la sheet chiama `openFor(targetId, kind)`
 * → load commenti iniziali. Quando la sheet viene chiusa, il VM resta in
 * memoria ma il prossimo open con altro id rifa load.
 *
 * Post commento è **ottimistico**: aggiungiamo subito un placeholder con
 * `_id = "tmp-..."` in cima, e lo sostituiamo con la risposta server al
 * success. Su errore: rimuoviamo il placeholder e mostriamo l'errore.
 *
 * Delete è ottimistico simmetrico.
 */
class CommentsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TsmApplication
    private val tokenStorage = app.tokenStorage
    private val api = TsmApiClient.service()

    private val _state = MutableStateFlow(CommentsState())
    val state: StateFlow<CommentsState> = _state.asStateFlow()

    init {
        // Recupera l'userId del viewer dal JWT cached — serve per mostrare/
        // nascondere il bottone "elimina" accanto ai commenti propri.
        val uid = tokenStorage.getToken()?.let { JwtDecoder.userIdFrom(it) }
        _state.value = _state.value.copy(currentUserId = uid)
    }

    /** Apre la sheet per un target: setta i metadata + load iniziale. */
    fun openFor(targetId: String, kind: String) {
        if (_state.value.targetId == targetId && _state.value.kind == kind) {
            // Stesso target già caricato — refresh silenzioso per consistency.
            refresh()
            return
        }
        _state.value = _state.value.copy(
            targetId = targetId,
            kind = kind,
            items = emptyList(),
            count = 0,
            error = null,
        )
        refresh()
    }

    fun refresh() {
        val id = _state.value.targetId ?: return
        val kind = _state.value.kind
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching {
                if (kind == "activity") api.getActivityComments(id)
                else api.getSessionComments(id)
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        items = body?.items.orEmpty(),
                        count = body?.count ?: 0,
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Errore caricamento commenti (${resp.code()}).",
                    )
                }
            }.onFailure {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = it.message ?: "Errore di rete.",
                )
            }
        }
    }

    fun updateDraft(text: String) {
        _state.value = _state.value.copy(draft = text)
    }

    /**
     * Invia un commento. Optimistic: aggiunge subito un placeholder con
     * id temporaneo, poi lo sostituisce con la risposta server. Rollback
     * automatico su errore (rimozione del placeholder + restore draft).
     */
    fun submitComment() {
        val state = _state.value
        val text = state.draft.trim()
        if (text.isEmpty() || state.targetId == null) return
        if (text.length > 500) {
            _state.value = state.copy(error = "Massimo 500 caratteri.")
            return
        }

        val targetId = state.targetId
        val kind = state.kind
        val viewerId = state.currentUserId
        val tmpId = "tmp-${System.currentTimeMillis()}"
        // Placeholder che simula un commento appena postato (no avatar fetch).
        val placeholder = CommentItem(
            _id = tmpId,
            activityRefId = targetId,
            kind = kind,
            userId = null, // verrà sostituito dalla response populated
            text = text,
            createdAt = null,
        )
        _state.value = state.copy(
            isSending = true,
            draft = "",
            error = null,
            items = listOf(placeholder) + state.items,
            count = state.count + 1,
        )

        viewModelScope.launch {
            runCatching {
                val req = CreateCommentRequest(text = text)
                if (kind == "activity") api.addActivityComment(targetId, req)
                else api.addSessionComment(targetId, req)
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val saved = resp.body()?.comment
                    if (saved != null) {
                        _state.value = _state.value.copy(
                            isSending = false,
                            items = _state.value.items.map {
                                if (it._id == tmpId) saved else it
                            },
                        )
                    } else {
                        // Server OK ma body null: ricarica per coerenza.
                        _state.value = _state.value.copy(isSending = false)
                        refresh()
                    }
                } else {
                    rollbackOptimisticAdd(tmpId, text)
                    _state.value = _state.value.copy(
                        error = "Errore invio commento (${resp.code()}).",
                    )
                }
            }.onFailure {
                rollbackOptimisticAdd(tmpId, text)
                _state.value = _state.value.copy(
                    error = it.message ?: "Errore di rete.",
                )
            }
        }
    }

    /**
     * Elimina un commento dell'utente corrente. Ottimistico: rimuove subito
     * dalla lista, rollback se il server risponde errore.
     *
     * Lato server, FORBIDDEN_NOT_AUTHOR rifiuta delete di commenti altrui
     * (filtro UI ridondante ma evita roundtrip per casi ovvi).
     */
    fun deleteComment(commentId: String) {
        val state = _state.value
        val targetId = state.targetId ?: return
        val original = state.items.find { it._id == commentId } ?: return
        val originalIndex = state.items.indexOf(original)
        // Rimozione ottimistica
        _state.value = state.copy(
            items = state.items.filterNot { it._id == commentId },
            count = (state.count - 1).coerceAtLeast(0),
        )
        viewModelScope.launch {
            runCatching {
                if (state.kind == "activity") api.deleteActivityComment(targetId, commentId)
                else api.deleteSessionComment(targetId, commentId)
            }.onSuccess { resp ->
                if (!resp.isSuccessful) {
                    // Rollback inserendo nella posizione originale (best effort).
                    val newItems = _state.value.items.toMutableList()
                    val insertAt = originalIndex.coerceAtMost(newItems.size)
                    newItems.add(insertAt, original)
                    _state.value = _state.value.copy(
                        items = newItems,
                        count = _state.value.count + 1,
                        error = "Impossibile eliminare il commento (${resp.code()}).",
                    )
                }
            }.onFailure { err ->
                val newItems = _state.value.items.toMutableList()
                val insertAt = originalIndex.coerceAtMost(newItems.size)
                newItems.add(insertAt, original)
                _state.value = _state.value.copy(
                    items = newItems,
                    count = _state.value.count + 1,
                    error = err.message ?: "Errore di rete.",
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun rollbackOptimisticAdd(tmpId: String, originalDraft: String) {
        _state.value = _state.value.copy(
            isSending = false,
            items = _state.value.items.filterNot { it._id == tmpId },
            count = (_state.value.count - 1).coerceAtLeast(0),
            draft = originalDraft,
        )
    }
}
