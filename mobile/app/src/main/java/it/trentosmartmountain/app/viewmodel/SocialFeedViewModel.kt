package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.local.db.ViewedStoryEntity
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.FeedItem
import it.trentosmartmountain.app.data.remote.dto.ShareRequest
import it.trentosmartmountain.app.data.remote.dto.SocialRowItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state della schermata Social.
 *
 *  - [items]            lista corrente del feed (accumulo via append on loadMore)
 *  - [isLoading]        true durante refresh iniziale o pull-to-refresh
 *  - [isLoadingMore]    true mentre paginazione "next page" è in volo
 *  - [hasMore]          dal server: se false, niente più infinite scroll
 *  - [currentPage]      1-based; loadMore incrementa post-fetch riuscito
 *  - [error]            messaggio user-facing l'ultima volta che qualcosa è andato male
 *  - [shareSuccess] / [shareError]  feedback transiente per il dialog "Pubblica"
 */
data class SocialFeedState(
    val items: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val error: String? = null,
    val shareSuccess: String? = null,
    val shareError: String? = null,
    /**
     * Avatar row mostrata in cima al feed. Filtrata client-side per le
     * "story" già viste localmente (vedi `socialRowDisplay`). Aggiornata
     * insieme a `refresh()`.
     */
    val socialRow: List<SocialRowItem> = emptyList(),
    /** Set degli `activityRefId` già marcati come "vista" dal viewer. */
    val viewedStoryIds: Set<String> = emptySet(),
    /** Notifiche non lette → badge sulla campanella in cima al feed. */
    val unreadNotifications: Int = 0,
)

/**
 * ViewModel per la schermata Social (HomeScreen sotto-tab Social).
 *
 * Pattern:
 *  - **Refresh totale** = reset paginazione + GET feed?page=1.
 *  - **loadMore** = chiamata in coda quando lo scroll arriva vicino al fondo
 *    della LazyColumn. Server cap a 50 items/page; default 20.
 *  - **toggleLike** è **ottimistico**: aggiorniamo lo state in-memory PRIMA
 *    della response per UX snappy, rollback se 4xx/5xx.
 *  - **shareActivity / shareSession** sono usati dal dialog "Pubblica" che vive
 *    in ActivityDetailScreen/SessionDetailScreen. Su success ricarichiamo il
 *    feed così l'utente vede subito il proprio post in cima.
 *
 * Scope: AndroidViewModel a livello Activity (vedi HikerMainScreen che lo
 * costruisce con `LocalContext.current as ComponentActivity`). Il ricaricamento
 * automatico avviene in `init` la prima volta che il VM viene istanziato.
 */
class SocialFeedViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TsmApplication
    private val api = TsmApiClient.service()
    private val viewedStoryDao = app.database.viewedStoryDao()

    private val _state = MutableStateFlow(SocialFeedState())
    val state: StateFlow<SocialFeedState> = _state.asStateFlow()

    init { refresh() }

    /** Reset paginazione + fetch pagina 1 (pull-to-refresh).
     *  Carica in parallelo feed + social-row + viewed-stories locali. */
    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
            )
            coroutineScope {
                val feedJob = async { runCatching { api.getFeed(page = 1, limit = 20) } }
                val rowJob = async { runCatching { api.getSocialRow() } }
                // 24h window: tutte le viste rilevanti per filtrare le story.
                val since24h = System.currentTimeMillis() - 24 * 3600 * 1000L
                val viewedJob = async {
                    runCatching { viewedStoryDao.getViewedSince(since24h) }
                }
                val notifJob = async { runCatching { api.getUnreadNotificationsCount() } }
                val feedResp = feedJob.await().getOrNull()
                val rowResp = rowJob.await().getOrNull()
                val viewedIds = viewedJob.await().getOrNull().orEmpty().toSet()
                val notifResp = notifJob.await().getOrNull()

                _state.value = _state.value.copy(
                    isLoading = false,
                    items = feedResp?.body()?.items.orEmpty().takeIf { feedResp?.isSuccessful == true }
                        ?: _state.value.items,
                    hasMore = feedResp?.body()?.hasMore == true,
                    currentPage = 1,
                    socialRow = rowResp?.body()?.items.orEmpty().takeIf { rowResp?.isSuccessful == true }
                        ?: _state.value.socialRow,
                    viewedStoryIds = viewedIds,
                    unreadNotifications = notifResp?.body()?.unreadCount?.takeIf { notifResp.isSuccessful }
                        ?: _state.value.unreadNotifications,
                    error = when {
                        feedResp == null || !feedResp.isSuccessful ->
                            "Errore caricamento feed (${feedResp?.code() ?: "rete"})."
                        else -> null
                    },
                )
            }
        }
    }

    /**
     * Aggiorna SOLO la social-row (anelli live/story), senza ricaricare il feed
     * né resettare lo scroll. Chiamata al ritorno sul feed (ON_RESUME) così la
     * storia appena pubblicata — inclusa "La tua storia" — compare subito senza
     * dover fare un pull-to-refresh manuale.
     */
    fun refreshSocialRow() {
        viewModelScope.launch {
            val rowResp = runCatching { api.getSocialRow() }.getOrNull()
            if (rowResp?.isSuccessful == true) {
                _state.value = _state.value.copy(
                    socialRow = rowResp.body()?.items.orEmpty(),
                )
            }
        }
    }

    /**
     * Azzera il badge notifiche in locale: chiamato quando l'utente apre il
     * centro notifiche (che le marca come lette lato server). Evita di mostrare
     * un badge stale al ritorno sul feed senza dover ri-fare il fetch.
     */
    fun clearNotificationBadge() {
        if (_state.value.unreadNotifications != 0) {
            _state.value = _state.value.copy(unreadNotifications = 0)
        }
    }

    /**
     * Marca una "story" come visualizzata localmente. Persiste su Room
     * (`viewed_stories`) + aggiorna lo state in-memory così l'anello story
     * sparisce subito senza dover ri-fetchare la social-row.
     */
    fun markStoryViewed(activityRefId: String, kind: String) {
        viewModelScope.launch {
            runCatching {
                viewedStoryDao.markViewed(
                    ViewedStoryEntity(
                        activityRefId = activityRefId,
                        kind = kind,
                        viewedAtMs = System.currentTimeMillis(),
                    ),
                )
            }
            _state.value = _state.value.copy(
                viewedStoryIds = _state.value.viewedStoryIds + activityRefId,
            )
        }
    }

    /** Carica la pagina successiva. No-op se già in fetch o se !hasMore. */
    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        viewModelScope.launch {
            val nextPage = current.currentPage + 1
            _state.value = current.copy(isLoadingMore = true)
            runCatching { api.getFeed(page = nextPage, limit = 20) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        _state.value = _state.value.copy(
                            isLoadingMore = false,
                            items = _state.value.items + body?.items.orEmpty(),
                            hasMore = body?.hasMore == true,
                            currentPage = nextPage,
                        )
                    } else {
                        _state.value = _state.value.copy(isLoadingMore = false)
                    }
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoadingMore = false)
                }
        }
    }

    /**
     * Toggle like ottimistico: aggiorna lo state PRIMA della chiamata server,
     * poi rollback se la risposta è errore. Il counter visivo cambia istantaneo
     * per UX percepita "snappy" (vedi piano `sprint2_social.md` fase E1).
     */
    fun toggleLike(item: FeedItem) {
        val previous = item
        val willLike = !item.likedByMe
        val optimistic = item.copy(
            likedByMe = willLike,
            likesCount = (item.likesCount + if (willLike) 1 else -1).coerceAtLeast(0),
        )
        replaceItem(optimistic)

        viewModelScope.launch {
            val result = runCatching {
                if (item.kind == "activity") {
                    if (willLike) api.likeActivity(item.id) else api.unlikeActivity(item.id)
                } else {
                    if (willLike) api.likeSession(item.id) else api.unlikeSession(item.id)
                }
            }
            result.onSuccess { resp ->
                if (resp.isSuccessful && resp.body() != null) {
                    val server = resp.body()!!
                    replaceItem(
                        optimistic.copy(
                            likesCount = server.likesCount,
                            likedByMe = server.likedByMe,
                        ),
                    )
                } else {
                    replaceItem(previous) // rollback
                }
            }.onFailure { replaceItem(previous) }
        }
    }

    /**
     * Pubblica un'attività libera (POST /activities/:id/share). Su success
     * ricarica il feed: l'utente vedrà il proprio post in cima entro 1-2 sec.
     * `caption` può essere null o vuoto — server normalizza.
     */
    fun shareActivity(activityId: String, caption: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(shareError = null, shareSuccess = null)
            runCatching {
                api.shareActivity(activityId, ShareRequest(caption = caption?.takeIf { it.isNotBlank() }))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(shareSuccess = "Pubblicato sul feed.")
                    refresh()
                } else {
                    _state.value = _state.value.copy(
                        shareError = "Impossibile pubblicare (${resp.code()}).",
                    )
                }
            }.onFailure {
                _state.value = _state.value.copy(shareError = it.message ?: "Errore di rete.")
            }
        }
    }

    /** Pubblica una sessione di gruppo (solo creator). */
    fun shareSession(sessionId: String, caption: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(shareError = null, shareSuccess = null)
            runCatching {
                api.shareSession(sessionId, ShareRequest(caption = caption?.takeIf { it.isNotBlank() }))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(shareSuccess = "Pubblicato sul feed.")
                    refresh()
                } else {
                    _state.value = _state.value.copy(
                        shareError = "Impossibile pubblicare (${resp.code()}).",
                    )
                }
            }.onFailure {
                _state.value = _state.value.copy(shareError = it.message ?: "Errore di rete.")
            }
        }
    }

    /** Rimuove la condivisione di un'attività dal feed (DELETE share). */
    fun unshareActivity(activityId: String) {
        viewModelScope.launch {
            runCatching { api.unshareActivity(activityId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) refresh()
                }
        }
    }

    /**
     * Aggiorna il `commentsCount` di un singolo item del feed **in-place**,
     * senza ricaricare l'intero feed. Chiamato alla chiusura della BottomSheet
     * commenti, che conosce il conteggio finale del target.
     *
     * Prima si faceva `refresh()` totale alla chiusura: una manciata di chiamate
     * di rete + perdita della posizione di scroll, solo per aggiornare un numero.
     * No-op se l'item non è (più) nel feed corrente.
     */
    fun setCommentCount(id: String, kind: String, count: Int) {
        val item = _state.value.items.firstOrNull { it.id == id && it.kind == kind } ?: return
        if (item.commentsCount == count) return
        replaceItem(item.copy(commentsCount = count))
    }

    /** Pulisce i messaggi transienti dopo che la UI li ha mostrati. */
    fun clearShareMessages() {
        _state.value = _state.value.copy(shareError = null, shareSuccess = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /** Sostituisce in-place un item nel feed (matching per id+kind). */
    private fun replaceItem(updated: FeedItem) {
        val newItems = _state.value.items.map { current ->
            if (current.id == updated.id && current.kind == updated.kind) updated else current
        }
        _state.value = _state.value.copy(items = newItems)
    }
}
