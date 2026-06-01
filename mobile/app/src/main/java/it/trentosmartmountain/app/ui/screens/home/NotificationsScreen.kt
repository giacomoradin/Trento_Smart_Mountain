package it.trentosmartmountain.app.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.NotificationItem
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.util.RelativeTime
import it.trentosmartmountain.app.viewmodel.NotificationsViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentRed = Color(0xFFFF5252)
private val AccentGreen = Color(0xFF4CAF50)
private val TextSecondary = Color(0xFF8E8E93)

/**
 * Centro notifiche: lista delle interazioni ricevute (follow/like/commento).
 * Deep-link al tap: follow → profilo dell'attore; like/commento → il post
 * coinvolto. Le notifiche non lette hanno un leggero tint + pallino accent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    onOpenActivity: (activityId: String, sessionId: String?) -> Unit,
    viewModel: NotificationsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = { Text("Notifiche", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
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

        when {
            state.isLoading && state.items.isEmpty() -> Centered(padding) { CircularProgressIndicator(color = AccentCyan) }
            state.items.isEmpty() -> Centered(padding) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Nessuna notifica per ora.\nQuando qualcuno ti segue o interagisce coi tuoi post, lo vedrai qui.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    start = 12.dp, end = 12.dp, bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it._id }) { n ->
                    NotificationRow(
                        n = n,
                        onClick = {
                            when (n.type) {
                                "follow" -> n.actor?._id?.let(onUserClick)
                                else -> n.targetId?.let { tid ->
                                    onOpenActivity(tid, if (n.targetKind == "session") tid else null)
                                }
                            }
                        },
                    )
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

@Composable
private fun NotificationRow(n: NotificationItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (n.read) CardBackground else AccentCyan.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AvatarImage(avatarUrl = n.actor?.avatarUrl, fallbackName = n.actor?.username ?: "?", size = 44.dp)
                // Badge tipo notifica sull'angolo dell'avatar.
                val (icon, tint) = typeIcon(n.type)
                Box(
                    modifier = Modifier.size(18.dp).clip(CircleShape).background(DarkSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
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
    else -> Icons.Filled.PersonAdd to TextSecondary
}

private fun notifText(n: NotificationItem): String {
    val name = n.actor?.username ?: "Qualcuno"
    val target = if (n.targetKind == "session") "uscita di gruppo" else "attività"
    return when (n.type) {
        "follow" -> "$name ha iniziato a seguirti"
        "like" -> "$name ha messo \"Mi piace\" alla tua $target"
        "comment" -> "$name ha commentato la tua $target"
        else -> "$name ha interagito con te"
    }
}

@Composable
private fun Centered(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { content() }
}
