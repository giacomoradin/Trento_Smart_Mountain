package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.FeedItem
import it.trentosmartmountain.app.data.remote.dto.FollowStatsResponse
import it.trentosmartmountain.app.data.remote.dto.PublicUserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stato della UserProfileScreen.
 *
 *  - [targetUserId]: id dell'utente di cui mostriamo il profilo
 *  - [isSelf]: true se sto guardando il mio stesso profilo (niente bottone
 *              Segui, vedo anche post privati)
 *  - [user]: dati pubblici (username + avatar)
 *  - [stats]: count follower/following + isFollowedByMe
 *  - [posts]: bacheca paginata (riusa FeedItem del feed)
 *  - [isLoading]: load iniziale del profilo + stats + posts (tutto in parallelo)
 *  - [isLoadingMore]: paginazione posts in volo
 *  - [hasMore]: c'è ancora una pagina di posts?
 *  - [error]: errore user-facing dell'ultima op
 *  - [isFollowActionInFlight]: bottone Segui disabilitato durante il toggle
 *                              (evita doppi click)
 */
data class UserProfileState(
    val targetUserId: String = "",
    val isSelf: Boolean = false,
    val user: PublicUserProfile? = null,
    val stats: FollowStatsResponse? = null,
    val posts: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val error: String? = null,
    val isFollowActionInFlight: Boolean = false,
)

/**
 * ViewModel per la schermata profilo di un altro utente.
 *
 * Carica in PARALLELO 3 risorse (profilo, follow stats, posts pagina 1) per
 * minimizzare il tempo "white screen" — anche se uno fallisce, gli altri
 * arrivano comunque. Errori parziali sono silenti: la UI mostra solo le
 * sezioni di cui ha dato. Tutto re-fetchable via `refresh()`.
 *
 * Toggle follow ottimistico: aggiorna subito `stats.isFollowedByMe` e il
 * counter followers, poi rollback se il server risponde errore. Bottone
 * disabilitato durante l'in-flight per evitare doppi click.
 */
class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TsmApplication
    private val tokenStorage = app.tokenStorage
    private val api = TsmApiClient.service()

    private val _state = MutableStateFlow(UserProfileState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    /**
     * Inizializza la schermata per un utente specifico. Chiamata dal
     * Composable in `LaunchedEffect(userId)`. Idempotente: se è lo stesso
     * userId già caricato, fa solo refresh leggero.
     */
    fun loadFor(userId: String) {
        val current = _state.value
        if (current.targetUserId == userId && current.user != null) {
            // Stesso target già caricato: solo aggiornamento stats (in caso di
            // follow azionato altrove) — niente reset completo.
            refreshStatsAndPosts()
            return
        }
        val myUserId = tokenStorage.getToken()?.let { JwtDecoder.userIdFrom(it) }
        _state.value = UserProfileState(
            targetUserId = userId,
            isSelf = myUserId == userId,
            isLoading = true,
        )
        viewModelScope.launch {
            coroutineScope {
                val userJob = async { runCatching { api.getPublicHiker(userId) } }
                val statsJob = async { runCatching { api.getFollowStats(userId) } }
                val postsJob = async { runCatching { api.getUserPosts(userId, 1, 20) } }
                val userResp = userJob.await().getOrNull()
                val statsResp = statsJob.await().getOrNull()
                val postsResp = postsJob.await().getOrNull()
                _state.value = _state.value.copy(
                    isLoading = false,
                    user = userResp?.body()?.takeIf { userResp.isSuccessful },
                    stats = statsResp?.body()?.takeIf { statsResp.isSuccessful },
                    posts = postsResp?.body()?.items.orEmpty().takeIf { postsResp?.isSuccessful == true } ?: emptyList(),
                    hasMore = postsResp?.body()?.hasMore == true,
                    currentPage = 1,
                    error = when {
                        userResp == null || !userResp.isSuccessful -> "Utente non disponibile."
                        else -> null
                    },
                )
            }
        }
    }

    /** Refresh non-distruttivo: ricarica stats + prima pagina di posts. */
    fun refreshStatsAndPosts() {
        val userId = _state.value.targetUserId.ifBlank { return }
        viewModelScope.launch {
            coroutineScope {
                val statsJob = async { runCatching { api.getFollowStats(userId) } }
                val postsJob = async { runCatching { api.getUserPosts(userId, 1, 20) } }
                val statsResp = statsJob.await().getOrNull()
                val postsResp = postsJob.await().getOrNull()
                _state.value = _state.value.copy(
                    stats = statsResp?.body()?.takeIf { statsResp.isSuccessful } ?: _state.value.stats,
                    posts = postsResp?.body()?.items.orEmpty().takeIf { postsResp?.isSuccessful == true }
                        ?: _state.value.posts,
                    hasMore = postsResp?.body()?.hasMore == true,
                    currentPage = 1,
                )
            }
        }
    }

    /** Pagina successiva di posts (infinite scroll). */
    fun loadMorePosts() {
        val state = _state.value
        if (state.isLoadingMore || !state.hasMore || state.targetUserId.isBlank()) return
        val nextPage = state.currentPage + 1
        viewModelScope.launch {
            _state.value = state.copy(isLoadingMore = true)
            runCatching { api.getUserPosts(state.targetUserId, nextPage, 20) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        _state.value = _state.value.copy(
                            isLoadingMore = false,
                            posts = _state.value.posts + body?.items.orEmpty(),
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
     * Toggle Segui/Smetti. Ottimistico:
     *  - Aggiorna subito isFollowedByMe + followers count (+/-1)
     *  - Rollback se errore
     *  - Disabilita il bottone durante l'in-flight per evitare doppi tap
     */
    fun toggleFollow() {
        val state = _state.value
        val targetUserId = state.targetUserId.ifBlank { return }
        if (state.isSelf || state.isFollowActionInFlight) return
        val current = state.stats ?: return
        val willFollow = !current.isFollowedByMe
        val optimistic = current.copy(
            isFollowedByMe = willFollow,
            followers = (current.followers + if (willFollow) 1 else -1).coerceAtLeast(0),
        )
        _state.value = state.copy(
            stats = optimistic,
            isFollowActionInFlight = true,
        )
        viewModelScope.launch {
            runCatching {
                if (willFollow) api.followUser(targetUserId)
                else api.unfollowUser(targetUserId)
            }.onSuccess { resp ->
                if (!resp.isSuccessful) {
                    _state.value = _state.value.copy(
                        stats = current, // rollback
                        isFollowActionInFlight = false,
                        error = "Operazione fallita (${resp.code()}).",
                    )
                } else {
                    _state.value = _state.value.copy(isFollowActionInFlight = false)
                }
            }.onFailure {
                _state.value = _state.value.copy(
                    stats = current,
                    isFollowActionInFlight = false,
                    error = it.message ?: "Errore di rete.",
                )
            }
        }
    }

    /** Aggiornamento ottimistico del like su un post nella bacheca. */
    fun toggleLikeOnPost(item: FeedItem) {
        val previous = item
        val willLike = !item.likedByMe
        val optimistic = item.copy(
            likedByMe = willLike,
            likesCount = (item.likesCount + if (willLike) 1 else -1).coerceAtLeast(0),
        )
        replacePost(optimistic)
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
                    replacePost(optimistic.copy(
                        likesCount = server.likesCount,
                        likedByMe = server.likedByMe,
                    ))
                } else {
                    replacePost(previous)
                }
            }.onFailure { replacePost(previous) }
        }
    }

    /**
     * Aggiorna il contatore commenti di un singolo post della bacheca in-place,
     * senza ricaricare profilo+stats+posts. Chiamato alla chiusura della sheet
     * commenti (vedi [SocialFeedViewModel.setCommentCount] per la stessa logica).
     */
    fun setCommentCount(id: String, kind: String, count: Int) {
        val item = _state.value.posts.firstOrNull { it.id == id && it.kind == kind } ?: return
        if (item.commentsCount == count) return
        replacePost(item.copy(commentsCount = count))
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun replacePost(updated: FeedItem) {
        _state.value = _state.value.copy(
            posts = _state.value.posts.map { p ->
                if (p.id == updated.id && p.kind == updated.kind) updated else p
            },
        )
    }
}
