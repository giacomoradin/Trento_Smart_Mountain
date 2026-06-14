package it.trentosmartmountain.app.ui.screens.home

import android.app.Application
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.components.FeedSkeleton
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.ui.components.TsmHeroActionChip
import it.trentosmartmountain.app.ui.components.TsmEmptyState
import it.trentosmartmountain.app.ui.components.TsmErrorState
import it.trentosmartmountain.app.ui.components.TsmLoadingState
import it.trentosmartmountain.app.ui.components.tsmEnterReveal
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.dto.StoryViewerLaunchContext
import it.trentosmartmountain.app.viewmodel.SocialFeedViewModel

private val DarkSurface = TsmColors.FeedBackground
private val AccentCyan = TsmColors.Cyan
private val TextPrimary = TsmColors.TextPrimary
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
    /** Tap su anello STORY: apre lo StoryViewerScreen (coda autori per swipe tra utenti). */
    onStoryClick: (StoryViewerLaunchContext) -> Unit = {},
    /** Tap sulla barra "Trova persone": apre la ricerca utenti ("aggiungi amici"). */
    onSearchClick: () -> Unit = {},
    /** Tap sull'icona trofeo: apre la classifica settimanale. */
    onLeaderboardClick: () -> Unit = {},
    /** Tap sulla campanella: apre il centro notifiche. */
    onNotificationsClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val storyAuthorIds = remember(state.socialRow) {
        state.socialRow.filter { it.status == "story" }.map { it.user._id }
    }
    val currentUserId = remember {
        JwtDecoder.userIdFrom(
            (context.applicationContext as TsmApplication).tokenStorage.getToken().orEmpty(),
        )
    }
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
    LaunchedEffect(state.deleteSuccess) {
        state.deleteSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearDeleteMessages()
        }
    }
    LaunchedEffect(state.deleteError) {
        state.deleteError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearDeleteMessages()
        }
    }

    // Al ritorno sul feed (es. dopo aver pubblicato una storia dal composer)
    // aggiorniamo SOLO gli anelli live/story — così "La tua storia" appena creata
    // compare subito, senza pull-to-refresh e senza resettare lo scroll del feed.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshSocialRow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize()) {
     // Aurora di profondità dietro al feed: le card glass "galleggiano" sopra.
     TsmAuroraBackground(
        modifier = Modifier.fillMaxSize(),
        baseColor = DarkSurface,
        particleCount = 22,
     )
     Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
      Column(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            // Lo spinner pull-to-refresh va mostrato solo durante un refresh CON
            // contenuti già a schermo; il primo caricamento usa TsmLoadingState.
            isRefreshing = state.isLoading && state.items.isNotEmpty(),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f),
        ) {
            when {
                state.isLoading && state.items.isEmpty() && state.socialRow.isEmpty() -> FeedSkeleton()
                // Errore di rete con lista vuota: stato dedicato (prima appariva
                // come "feed vuoto", facendo credere all'utente di non seguire
                // nessuno invece che a un problema di connessione).
                state.error != null && state.items.isEmpty() && state.socialRow.isEmpty() -> TsmErrorState(
                    message = "Non riesco a caricare il feed. Controlla la connessione e riprova.",
                    onRetry = { viewModel.refresh() },
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
                    onUserAvatarClick = onUserClick,
                    onLiveClick = onLiveClick,
                    onStoryClick = { authorId ->
                        onStoryClick(
                            StoryViewerLaunchContext(
                                userIds = storyAuthorIds,
                                startIndex = storyAuthorIds.indexOf(authorId).coerceAtLeast(0),
                            ),
                        )
                    },
                    currentUserId = currentUserId,
                    onDeletePost = { item -> viewModel.removeFeedPost(item) },
                    onRefreshEmpty = { viewModel.refresh() },
                    header = {
                        FeedHeader(
                            unreadNotifications = state.unreadNotifications,
                            onSearchClick = onSearchClick,
                            onLeaderboardClick = onLeaderboardClick,
                            onNotificationsClick = {
                                // Aprire = leggere: azzeriamo subito il badge locale.
                                viewModel.clearNotificationBadge()
                                onNotificationsClick()
                            },
                        )
                    },
                )
            }
        }
      }
     }
    }

    // BottomSheet condivisa: si apre solo quando `commentsTarget != null`.
    CommentsBottomSheet(
        target = commentsTarget,
        onDismiss = { commentsTarget = null },
        onUserClick = onUserClick,
        // Aggiorna SOLO il contatore dell'item interessato alla chiusura: niente
        // più refresh totale del feed (che ricaricava tutto e perdeva lo scroll).
        onCountChanged = viewModel::setCommentCount,
    )
}

/**
 * **Hero header del feed** (redesign): overline brand + titolo grande "Community",
 * azioni (classifica/notifiche) come chip glass circolari, e barra di ricerca
 * glass a tutta larghezza. È la prima cosa che si vede aprendo l'app → identità
 * marcata e materiali coerenti col resto.
 */
@Composable
private fun FeedHeader(
    unreadNotifications: Int,
    onSearchClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    // NB: la LazyColumn ha già 12dp di contentPadding → qui solo un piccolo extra
    // orizzontale per allineare il titolo al contenuto delle card.
    Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "TRENTO SMART MOUNTAIN",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                Text(
                    "Community",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
            }
            TsmHeroActionChip(
                icon = Icons.Filled.EmojiEvents,
                contentDescription = stringResource(R.string.cd_leaderboard),
                onClick = onLeaderboardClick,
            )
            Spacer(Modifier.width(10.dp))
            TsmHeroActionChip(
                icon = Icons.Filled.Notifications,
                contentDescription = stringResource(R.string.cd_notifications),
                badgeCount = unreadNotifications,
                onClick = onNotificationsClick,
            )
        }
        Spacer(Modifier.height(14.dp))
        SearchEntryBar(onClick = onSearchClick)
    }
}

/**
 * Barra "Trova persone da seguire": bottone glass che apre la ricerca utenti.
 */
@Composable
private fun SearchEntryBar(onClick: () -> Unit) {
    TsmGlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = AccentCyan,
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
    onUserAvatarClick: (String) -> Unit = {},
    onLiveClick: (String) -> Unit = {},
    onStoryClick: (String) -> Unit = {},
    currentUserId: String? = null,
    onDeletePost: (it.trentosmartmountain.app.data.remote.dto.FeedItem) -> Unit = {},
    onRefreshEmpty: () -> Unit = {},
    /** Header (hero) scrollabile: reso come primo item → scompare scrollando giù. */
    header: @Composable () -> Unit = {},
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
        // Hero header come PRIMO item: scorre via col contenuto (non più fisso in
        // cima) → la barra "trova persone" sparisce scrollando giù, come richiesto.
        item(key = "feed-hero-header") { header() }

        // Avatar Row in cima (header sticky-feel): mostra live/story/goal
        // per ogni utente seguito. Rimane visibile mentre l'utente scrolla
        // — non è "sticky" nel senso Material ma resta in cima al feed.
        if (socialRow.isNotEmpty()) {
            item(key = "avatar-row") {
                AvatarRow(
                    items = socialRow,
                    onUserClick = onUserAvatarClick,
                    onLiveClick = onLiveClick,
                    onStoryClick = onStoryClick,
                )
            }
        }
        if (items.isEmpty()) {
            item(key = "feed-empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("👥", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Il tuo feed è vuoto",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Segui qualcuno o pubblica una tua attività dalla sezione \"Personale\" → tap su un'attività → Condividi.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onRefreshEmpty) {
                        Text("Aggiorna", color = AccentCyan, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        items(
            items = items,
            key = { "${it.kind}-${it.id}" },
        ) { feedItem ->
            FeedCard(
                item = feedItem,
                modifier = Modifier.tsmEnterReveal(),
                onLikeToggle = { onLikeToggle(feedItem) },
                onCommentClick = { onCommentClick(feedItem.id, feedItem.kind) },
                onUserClick = onUserClick,
                onOpenDetail = { onOpenDetail(feedItem) },
                currentUserId = currentUserId,
                onDeletePost = { onDeletePost(feedItem) },
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
