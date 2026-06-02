package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.FeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del dettaglio "social" di un post (Activity/HikeSession condivisa).
 *
 * Non fa fetch di rete: viene inizializzato con il [FeedItem] già presente nel
 * feed/bacheca (che porta con sé tutto il necessario — route, profilo, stat,
 * partecipanti). Gestisce solo like ottimistico e aggiornamento del contatore
 * commenti, riusando gli stessi endpoint del feed.
 */
class PostDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()
    private val _item = MutableStateFlow<FeedItem?>(null)
    val item: StateFlow<FeedItem?> = _item.asStateFlow()

    /** Inizializza con l'item passato dal chiamante (idempotente sull'id). */
    fun init(initial: FeedItem) {
        if (_item.value?.id != initial.id) _item.value = initial
    }

    /** Like ottimistico con rollback, allineato a SocialFeedViewModel.toggleLike. */
    fun toggleLike() {
        val current = _item.value ?: return
        val willLike = !current.likedByMe
        _item.value = current.copy(
            likedByMe = willLike,
            likesCount = (current.likesCount + if (willLike) 1 else -1).coerceAtLeast(0),
        )
        viewModelScope.launch {
            val resp = runCatching {
                if (current.kind == "activity") {
                    if (willLike) api.likeActivity(current.id) else api.unlikeActivity(current.id)
                } else {
                    if (willLike) api.likeSession(current.id) else api.unlikeSession(current.id)
                }
            }.getOrNull()
            if (resp != null && resp.isSuccessful && resp.body() != null) {
                val server = resp.body()!!
                _item.value = _item.value?.copy(likesCount = server.likesCount, likedByMe = server.likedByMe)
            } else {
                _item.value = current // rollback
            }
        }
    }

    fun setCommentCount(count: Int) {
        _item.value = _item.value?.let { if (it.commentsCount == count) it else it.copy(commentsCount = count) }
    }
}
