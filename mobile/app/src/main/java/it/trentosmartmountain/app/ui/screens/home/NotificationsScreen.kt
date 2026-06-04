package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.NotificationItem
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.components.ListSkeleton
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.util.RelativeTime
import it.trentosmartmountain.app.viewmodel.NotificationsViewModel

private val DarkSurface = TsmColors.FeedBackground
private val CardBackground = TsmColors.CardElevated
private val AccentCyan = TsmColors.Cyan
private val AccentRed = TsmColors.Danger
private val AccentGreen = TsmColors.Online
private val AccentAmber = Color(0xFFFFC107)
private val TextSecondary = TsmColors.TextSecondary
/** Sfondo opaco per notifiche non lette (evita che il rosso dello swipe traspaia). */
private val UnreadCardBackground = Color(0xFF1A2D3A)

/**
 * Centro notifiche. Oltre alle social (follow/like/commento) mostra:
 *  - richieste di partecipazione, accettazioni e rimozioni dalle sessioni;
 *  - promemoria delle proprie escursioni entro ~24h;
 *  - allerte pubblicate dai rifugisti.
 *
 * Gesti: swipe verso sinistra → cestino (elimina), tasto "Elimina tutte" in fondo,
 * pull-to-refresh per aggiornare.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    onOpenActivity: (activityId: String, sessionId: String?) -> Unit,
    onOpenSession: (sessionId: String) -> Unit,
    viewModel: NotificationsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showClearAll by remember { mutableStateOf(false) }

    if (showClearAll) {
        AlertDialog(
            onDismissRequest = { showClearAll = false },
            containerColor = CardBackground,
            title = { Text("Eliminare tutte le notifiche?", color = Color.White) },
            text = { Text("L'operazione non è reversibile.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showClearAll = false
                }) { Text("Elimina tutte", color = AccentRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAll = false }) { Text("Annulla", color = TextSecondary) }
            },
        )
    }

    fun handleClick(n: NotificationItem) {
        when (n.type) {
            "follow" -> n.actor?._id?.let(onUserClick)
            "refuge_alert" -> { /* nessun deep-link: l'allerta è informativa */ }
            else -> when {
                n.targetKind == "session" && !n.targetId.isNullOrBlank() ->
                    onOpenSession(n.targetId)
                !n.targetId.isNullOrBlank() ->
                    onOpenActivity(n.targetId, null)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) {
    it.trentosmartmountain.app.ui.components.TsmAuroraBackground(
        modifier = Modifier.fillMaxSize(),
        particleCount = 12,
    )
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                // Surface "glass": rispetta la barra di navigazione del device (gesti/3-button)
                // così la riga "Elimina tutte" resta sempre sopra gli insets di sistema.
                Surface(color = DarkSurface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars),
                    ) {
                        TextButton(
                            onClick = { showClearAll = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Elimina tutte", color = AccentRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
    ) { padding ->
        val listState = rememberLazyListState()
        val shouldLoadMore by remember {
            derivedStateOf {
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = listState.layoutInfo.totalItemsCount
                total > 0 && last >= total - 3
            }
        }
        androidx.compose.runtime.LaunchedEffect(shouldLoadMore, state.hasMore) {
            if (shouldLoadMore && state.hasMore) viewModel.loadMore()
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading && state.items.isNotEmpty(),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding),
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> ListSkeleton()
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = AccentCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.notifications_empty),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, start = 12.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it._id }) { n ->
                        if (n.deletable) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.delete(n._id)
                                        true
                                    } else {
                                        false
                                    }
                                },
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AccentRed),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Elimina",
                                            tint = Color.White,
                                            modifier = Modifier.padding(end = 20.dp),
                                        )
                                    }
                                },
                            ) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
                                    NotificationRow(n = n, onClick = { handleClick(n) })
                                }
                            }
                        } else {
                            NotificationRow(n = n, onClick = { handleClick(n) })
                        }
                    }
                    if (state.isLoadingMore) {
                        item(key = "loading-more") {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun NotificationRow(n: NotificationItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (n.read) CardBackground else UnreadCardBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (icon, tint) = typeIcon(n.type)
            if (n.actor != null) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AvatarImage(avatarUrl = n.actor.avatarUrl, fallbackName = n.actor.username ?: "?", size = 44.dp)
                    Box(
                        modifier = Modifier.size(18.dp).clip(CircleShape).background(DarkSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
                    }
                }
            } else {
                // Notifiche di sistema (promemoria/allerte): solo icona tonda colorata.
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notifText(n),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (n.read) FontWeight.Normal else FontWeight.SemiBold,
                )
                Text(
                    RelativeTime.short(n.createdAt),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (!n.read) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentCyan))
            }
        }
    }
}

private fun typeIcon(type: String): Pair<ImageVector, Color> = when (type) {
    "follow" -> Icons.Filled.PersonAdd to AccentGreen
    "like" -> Icons.Filled.Favorite to AccentRed
    "comment" -> Icons.Filled.ChatBubble to AccentCyan
    "join_request" -> Icons.Filled.Group to AccentCyan
    "join_accepted" -> Icons.Filled.CheckCircle to AccentGreen
    "removed" -> Icons.Filled.Block to AccentRed
    "activity_reminder" -> Icons.Filled.Schedule to AccentAmber
    "refuge_alert" -> Icons.Filled.Warning to AccentAmber
    else -> Icons.Filled.Schedule to TextSecondary
}

@Composable
private fun notifText(n: NotificationItem): String {
    // I tipi non-social arrivano col testo già pronto dal server.
    if (!n.message.isNullOrBlank()) return n.message
    val name = n.actor?.username ?: "Qualcuno"
    val isSession = n.targetKind == "session"
    return when (n.type) {
        "follow" -> stringResource(R.string.notif_follow, name)
        "like" -> stringResource(
            if (isSession) R.string.notif_like_session else R.string.notif_like_activity, name,
        )
        "comment" -> stringResource(
            if (isSession) R.string.notif_comment_session else R.string.notif_comment_activity, name,
        )
        "join_request" -> "$name ha chiesto di unirsi alla tua escursione"
        "join_accepted" -> "$name ha accettato la tua richiesta di partecipazione"
        "removed" -> "Sei stato rimosso da un'escursione"
        else -> stringResource(R.string.notif_generic, name)
    }
}
