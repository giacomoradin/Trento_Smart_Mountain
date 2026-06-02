package it.trentosmartmountain.app.ui.screens.home

import android.app.Application
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.components.FeedSkeleton
import it.trentosmartmountain.app.ui.components.TsmEmptyState
import it.trentosmartmountain.app.ui.components.TsmErrorState
import it.trentosmartmountain.app.ui.components.TsmLoadingState
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.SocialFeedViewModel

private val DarkSurface = TsmColors.FeedBackground
private val AccentCyan = TsmColors.Cyan
private val TextSecondary = TsmColors.TextSecondary

/**
 * Schermata principale del feed sociale (HomeScreen sotto-tab Social).
 *
 *  - LazyColumn paginata: quando l'utente arriva entro 3 item dal fondo,
 *    chiamiamo `loadMore()` (sentinel via `derivedStateOf` su last visible).
 *  - PullToRefreshBox (Compose Material3 BOM 2024.12) per refresh manuale.
 *  - Empty state quando il backend ritorna `items=[]`: messaggio + bottone
 *    "Aggiorna" + suggerimento "Segui qualcuno o pubblica un'attività".
 *  - Loading state iniziale: CircularProgressIndicator centrato.
 *  - Toast su `shareSuccess` / `shareError` (l'utente può condividere da
 *    ActivityDetailScreen e il ViewModel è scoped Activity-wide, quindi la
 *    success arriva anche su questa schermata).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSocialScreen(
    modifier: Modifier = Modifier,
    viewModel: SocialFeedViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            (LocalContext.current as ComponentActivity).application,
        ),
    ),
    onUserClick: (userId: String) -> Unit = {},
    onCommentClick: (itemId: String, kind: String) -> Unit = { _, _ -> },
    /** Tap sul corpo della card: apre il dettaglio social del post. */
    onOpenDetail: (item: it.trentosmartmountain.app.data.remote.dto.FeedItem) -> Unit = {},
    /** Tap su anello LIVE: apre la SessionDetail della sessione in corso. */
    onLiveClick: (sessionId: String) -> Unit = {},
    /** Tap su anello STORY: apre lo StoryViewerScreen full-screen. */
    onStoryClick: (refId: String, kind: String) -> Unit = { _, _ -> },
    /** Tap sulla barra "Trova persone": apre la ricerca utenti ("aggiungi amici"). */
    onSearchClick: () -> Unit = {},
    /** Tap sull'icona trofeo: apre la classifica settimanale. */
    onLeaderboardClick: () -> Unit = {},
    /** Tap sulla campanella: apre il centro notifiche. */
    onNotificationsClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Target attuale della BottomSheet commenti (null = chiusa).
    var commentsTarget by remember { mutableStateOf<CommentsTarget?>(null) }

    // Toast su esiti di share: il VM è Activity-scoped quindi può ricevere
    // success da un dialog aperto su altra schermata (ActivityDetail).
    LaunchedEffect(state.shareSuccess) {
        state.shareSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearShareMessages()
        }
    }
    LaunchedEffect(state.shareError) {
        state.shareError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearShareMessages()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = DarkSurface) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.weight(1f)) { SearchEntryBar(onClick = onSearchClick) }
          IconButton(onClick = onLeaderboardClick) {
            Icon(
              Icons.Filled.EmojiEvents,
              contentDescription = stringResource(R.string.cd_leaderboard),
              tint = AccentCyan,
            )
          }
          IconButton(
            onClick = {
              // Aprire = leggere: azzeriamo subito il badge locale (server le marca lette).
              viewModel.clearNotificationBadge()
              onNotificationsClick()
            },
            modifier = Modifier.padding(end = 8.dp),
          ) {
            BadgedBox(badge = {
              if (state.unreadNotifications > 0) {
                Badge {
                  Text(if (state.unreadNotifications > 99) "99+" else "${state.unreadNotifications}")
                }
              }
            }) {
              Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.cd_notifications), tint = AccentCyan)
            }
          }
        }
        PullToRefreshBox(
            // Lo spinner pull-to-refresh va mostrato solo durante un refresh CON
            // contenuti già a schermo; il primo caricamento usa TsmLoadingState.
            isRefreshing = state.isLoading && state.items.isNotEmpty(),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f),
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> FeedSkeleton()
                // Errore di rete con lista vuota: stato dedicato (prima appariva
                // come "feed vuoto", facendo credere all'utente di non seguire
                // nessuno invece che a un problema di connessione).
                state.error != null && state.items.isEmpty() -> TsmErrorState(
                    message = "Non riesco a caricare il feed. Controlla la connessione e riprova.",
                    onRetry = { viewModel.refresh() },
                )
                state.items.isEmpty() -> TsmEmptyState(
                    emoji = "👥",
                    title = "Il tuo feed è vuoto",
                    message = "Segui qualcuno o pubblica una tua attività dalla sezione \"Personale\" → tap su un'attività → Condividi.",
                    actionLabel = "Aggiorna",
                    onAction = { viewModel.refresh() },
                )
                else -> FeedList(
                    items = state.items,
                    hasMore = state.hasMore,
                    isLoadingMore = state.isLoadingMore,
                    onLoadMore = viewModel::loadMore,
                    onLikeToggle = viewModel::toggleLike,
                    onUserClick = onUserClick,
                    onOpenDetail = onOpenDetail,
                    onCommentClick = { id, kind ->
                        // Apre la BottomSheet commenti per il post tappato.
                        // L'on demand: il VM carica la lista la prima volta che
                        // la sheet si apre (vedi CommentsViewModel.openFor).
                        commentsTarget = CommentsTarget(id = id, kind = kind)
                        // Notifica anche il chiamante esterno (no-op default)
                        // così l'app può tracciare metriche o aprire bottom-sheet
                        // a livello globale se serve in futuro.
                        onCommentClick(id, kind)
                    },
                    socialRow = state.socialRow,
                    viewedStoryIds = state.viewedStoryIds,
                    onUserAvatarClick = onUserClick,
                    onLiveClick = onLiveClick,
                    onStoryClick = onStoryClick,
                )
            }
        }
      }
    }

    // BottomSheet condivisa: si apre solo quando `commentsTarget != null`.
    CommentsBottomSheet(
        target = commentsTarget,
        onDismiss = { commentsTarget = null },
        // Aggiorna SOLO il contatore dell'item interessato alla chiusura: niente
        // più refresh totale del feed (che ricaricava tutto e perdeva lo scroll).
        onCountChanged = viewModel::setCommentCount,
    )
}

/**
 * Barra "Trova persone da seguire" in cima al feed: non è un campo editabile,
 * è un bottone che apre la schermata di ricerca utenti dedicata (flusso
 * "aggiungi amici"). Stile coerente con una search bar per affordance chiara.
 */
@Composable
private fun SearchEntryBar(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2C2C2E),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.social_search_bar),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FeedList(
    items: List<it.trentosmartmountain.app.data.remote.dto.FeedItem>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onLikeToggle: (it.trentosmartmountain.app.data.remote.dto.FeedItem) -> Unit,
    onUserClick: (String) -> Unit,
    onOpenDetail: (it.trentosmartmountain.app.data.remote.dto.FeedItem) -> Unit = {},
    onCommentClick: (String, String) -> Unit,
    socialRow: List<it.trentosmartmountain.app.data.remote.dto.SocialRowItem> = emptyList(),
    viewedStoryIds: Set<String> = emptySet(),
    onUserAvatarClick: (String) -> Unit = {},
    onLiveClick: (String) -> Unit = {},
    onStoryClick: (String, String) -> Unit = { _, _ -> },
) {
    val listState = rememberLazyListState()

    // Sentinel paginazione: trigger loadMore quando il last visible è negli ultimi 3
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore, hasMore) {
        if (shouldLoadMore && hasMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Avatar Row in cima (header sticky-feel): mostra live/story/goal
        // per ogni utente seguito. Rimane visibile mentre l'utente scrolla
        // — non è "sticky" nel senso Material ma resta in cima al feed.
        if (socialRow.isNotEmpty()) {
            item(key = "avatar-row") {
                AvatarRow(
                    items = socialRow,
                    viewedStoryIds = viewedStoryIds,
                    onUserClick = onUserAvatarClick,
                    onLiveClick = onLiveClick,
                    onStoryClick = onStoryClick,
                )
            }
        }
        items(
            items = items,
            key = { "${it.kind}-${it.id}" },
        ) { feedItem ->
            FeedCard(
                item = feedItem,
                onLikeToggle = { onLikeToggle(feedItem) },
                onCommentClick = { onCommentClick(feedItem.id, feedItem.kind) },
                onUserClick = onUserClick,
                onOpenDetail = { onOpenDetail(feedItem) },
            )
        }
        if (isLoadingMore) {
            item(key = "loading-more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        } else if (!hasMore && items.isNotEmpty()) {
            item(key = "end-marker") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Fine del feed.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
