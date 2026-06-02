package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.FollowListEntry
import androidx.compose.ui.res.stringResource
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.components.ListSkeleton
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.FollowListType
import it.trentosmartmountain.app.viewmodel.FollowListViewModel

private val DarkSurface = TsmColors.FeedBackground
private val AccentCyan = TsmColors.Cyan
private val TextSecondary = TsmColors.TextSecondary

/**
 * Lista follower / seguiti di un utente (navigazione del grafo sociale).
 *
 * Aperta tappando i contatori FOLLOWER / SEGUITI nella [UserProfileScreen].
 * Riusa lo stesso ViewModel per entrambi i tipi via [FollowListType].
 * Ogni riga è tappabile → profilo dell'utente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    userId: String,
    type: FollowListType,
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    viewModel: FollowListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(userId, type) { viewModel.load(userId, type) }

    val title = stringResource(
        if (type == FollowListType.FOLLOWERS) R.string.followers_title else R.string.following_title,
    )

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = {
                    val suffix = if (state.total > 0) " · ${state.total}" else ""
                    Text("$title$suffix", color = Color.White, fontWeight = FontWeight.Bold)
                },
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
        LaunchedEffect(shouldLoadMore, state.hasMore) {
            if (shouldLoadMore && state.hasMore) viewModel.loadMore()
        }

        when {
            state.isLoading && state.entries.isEmpty() -> ListSkeleton(modifier = Modifier.padding(padding))
            state.entries.isEmpty() -> Centered(padding) {
                Text(
                    stringResource(
                        if (type == FollowListType.FOLLOWERS) R.string.followers_empty else R.string.following_empty,
                    ),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.entries, key = { it.user?._id ?: it.hashCode().toString() }) { entry ->
                    FollowRow(entry = entry, onClick = { entry.user?._id?.let(onUserClick) })
                }
                if (state.isLoadingMore) {
                    item(key = "loading-more") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowRow(entry: FollowListEntry, onClick: () -> Unit) {
    val user = entry.user
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(avatarUrl = user?.avatarUrl, fallbackName = user?.username ?: "?", size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            user?.username ?: "—",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun Centered(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { content() }
}
