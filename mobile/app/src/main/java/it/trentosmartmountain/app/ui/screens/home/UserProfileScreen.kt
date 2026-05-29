package it.trentosmartmountain.app.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.viewmodel.UserProfileViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentGreen = Color(0xFF4CAF50)
private val TextSecondary = Color(0xFF8E8E93)
private val ChipBlue = Color(0xFF1A3A5C)

/**
 * Profilo pubblico di un altro utente.
 *
 * Layout:
 *  - TopAppBar con username + back
 *  - Header card: avatar 96dp, username grande, chip "Verificato", chip credits
 *  - Stat row: 3 box (POST, FOLLOWERS, FOLLOWING)
 *  - Bottone Segui/Smetti (solo se viewer != self)
 *  - LazyColumn della bacheca: FeedCard riusati
 *  - Empty state se l'utente non ha pubblicato niente
 *
 * Apertura: navigato da `FeedCard` (tap avatar) o da `HomeSocialScreen.onUserClick`.
 * I post mostrati: solo `sharedAt != null` se viewer != target; tutti se self.
 *
 * Tap su un FeedCard nella bacheca → comment sheet o like (riuso pattern feed
 * principale). Per ora il comment click chiude e apre la sheet a livello shell.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onCommentClick: (itemId: String, kind: String) -> Unit = { _, _ -> },
    viewModel: UserProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var commentsTarget by remember { mutableStateOf<CommentsTarget?>(null) }

    LaunchedEffect(userId) { viewModel.loadFor(userId) }
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.user?.username ?: "Profilo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
        },
        containerColor = DarkSurface,
    ) { padding ->
        when {
            state.isLoading && state.user == null -> CenteredLoader(padding)
            state.user == null -> CenteredText("Utente non disponibile.", padding)
            else -> ProfileContent(
                state = state,
                onToggleFollow = viewModel::toggleFollow,
                onLoadMore = viewModel::loadMorePosts,
                onLikeToggle = viewModel::toggleLikeOnPost,
                onCommentClick = { id, kind ->
                    commentsTarget = CommentsTarget(id, kind)
                    onCommentClick(id, kind)
                },
                contentPadding = padding,
            )
        }
    }

    CommentsBottomSheet(
        target = commentsTarget,
        onDismiss = { commentsTarget = null },
        // Aggiorna solo il contatore del post commentato: niente reload di
        // profilo+stats+bacheca (che perdeva lo scroll della bacheca).
        onCountChanged = viewModel::setCommentCount,
    )
}

@Composable
private fun ProfileContent(
    state: it.trentosmartmountain.app.viewmodel.UserProfileState,
    onToggleFollow: () -> Unit,
    onLoadMore: () -> Unit,
    onLikeToggle: (it.trentosmartmountain.app.data.remote.dto.FeedItem) -> Unit,
    onCommentClick: (String, String) -> Unit,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.hasMore) {
        if (shouldLoadMore && state.hasMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            start = 12.dp,
            end = 12.dp,
            bottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") { ProfileHeader(state = state, onToggleFollow = onToggleFollow) }
        item(key = "section-title") {
            Text(
                text = "POST · ${state.posts.size}",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
        if (state.posts.isEmpty() && !state.isLoading) {
            item(key = "empty") { EmptyPostsBlock(isSelf = state.isSelf) }
        } else {
            items(items = state.posts, key = { "${it.kind}-${it.id}" }) { item ->
                FeedCard(
                    item = item,
                    onLikeToggle = { onLikeToggle(item) },
                    onCommentClick = { onCommentClick(item.id, item.kind) },
                )
            }
        }
        if (state.isLoadingMore) {
            item(key = "loading-more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    state: it.trentosmartmountain.app.viewmodel.UserProfileState,
    onToggleFollow: () -> Unit,
) {
    val user = state.user ?: return
    val stats = state.stats
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardBackground,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarImage(
                avatarUrl = user.avatarUrl,
                fallbackName = user.username,
                size = 96.dp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                user.username ?: "—",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
            if (user.isVerified == true) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentGreen.copy(alpha = 0.15f),
                ) {
                    Text(
                        "✓ Verificato",
                        color = AccentGreen,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Stat row: post + followers + following
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatBlock(label = "POST", value = "${state.posts.size}")
                StatBlock(label = "FOLLOWER", value = "${stats?.followers ?: 0}")
                StatBlock(label = "SEGUITI", value = "${stats?.following ?: 0}")
                user.socialCredits?.let { credits ->
                    StatBlock(label = "CREDITI", value = "%,d".format(credits))
                }
            }

            // Bottone Segui/Smetti — solo se non sono io
            if (!state.isSelf) {
                Spacer(Modifier.height(16.dp))
                FollowButton(
                    isFollowing = stats?.isFollowedByMe == true,
                    isLoading = state.isFollowActionInFlight,
                    onClick = onToggleFollow,
                )
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun FollowButton(
    isFollowing: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    // Pattern: bottone "Segui" pieno (CTA), "Smetti" outline (azione meno
    // prominente per non incoraggiare l'unfollow accidentale).
    if (isFollowing) {
        OutlinedButton(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AccentCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("SEGUITO", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = DarkSurface,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(18.dp), tint = DarkSurface)
                Spacer(Modifier.width(6.dp))
                Text("SEGUI", fontWeight = FontWeight.Bold, color = DarkSurface)
            }
        }
    }
}

@Composable
private fun EmptyPostsBlock(isSelf: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏔️", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                if (isSelf) "Non hai ancora pubblicato nulla."
                else "Questo utente non ha ancora pubblicato nulla.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CenteredLoader(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = AccentCyan) }
}

@Composable
private fun CenteredText(text: String, padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}
