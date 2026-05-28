package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.FeedItem
import it.trentosmartmountain.app.viewmodel.SocialFeedViewModel
import kotlinx.coroutines.delay

private val StoryBg = Color.Black
private val ProgressTrack = Color.White.copy(alpha = 0.3f)
private val ProgressFill = Color.White
private val OverlayDark = Color.Black.copy(alpha = 0.4f)
private const val STORY_DURATION_MS = 5000L

/**
 * Story viewer full-screen Instagram-like.
 *
 *  - Progress bar in alto (animata 5 sec linear)
 *  - Header: avatar + username + close icon
 *  - Centro: card FeedCard "compressed" del post + bottone "vedi attività intera"
 *  - Tap singolo → chiude (al timer scaduto chiude da solo)
 *  - Marca la story come vista all'apertura (via VM Activity-scoped)
 *
 * La story è derivata da un FeedItem dei seguiti — cerchiamo l'item nello
 * state del `SocialFeedViewModel` per evitare una nuova fetch. Se non lo
 * troviamo (es. paginato fuori), mostriamo solo l'avatar + invito a tornare al feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    refId: String,
    kind: String,
    onClose: () -> Unit,
    onOpenFullActivity: (refId: String, kind: String) -> Unit = { _, _ -> },
    viewModel: SocialFeedViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Cerca il FeedItem corrispondente nel feed cache; null se non in pagina 1.
    val item: FeedItem? = remember(refId, kind, state.items) {
        state.items.firstOrNull { it.id == refId && it.kind == kind }
    }

    // Marca come visualizzata all'apertura (idempotente, vedi Room).
    LaunchedEffect(refId, kind) {
        viewModel.markStoryViewed(refId, kind)
    }

    // Timer 5s: progress animato 0 → 1, alla fine onClose.
    var elapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(refId) {
        val step = 50L
        while (elapsedMs < STORY_DURATION_MS) {
            delay(step)
            elapsedMs += step
        }
        onClose()
    }
    val progress by animateFloatAsState(
        targetValue = (elapsedMs.toFloat() / STORY_DURATION_MS).coerceAtMost(1f),
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = "story-progress",
    )

    Surface(
        modifier = Modifier.fillMaxSize().clickable { onClose() },
        color = StoryBg,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Progress bar in alto
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(3.dp),
                color = ProgressTrack,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(ProgressFill),
                )
            }

            // Header con avatar autore + close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item?.user != null) {
                    it.trentosmartmountain.app.ui.components.AvatarImage(
                        avatarUrl = item.user.avatarUrl,
                        fallbackName = item.user.username,
                        size = 36.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.user.username ?: "Utente",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Chiudi", tint = Color.White)
                }
            }

            // Body: il post compresso (FeedCard riusata)
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (item != null) {
                    Column {
                        // Riusa la FeedCard con interazioni no-op (è una "preview" full-screen)
                        FeedCard(
                            item = item,
                            onLikeToggle = { viewModel.toggleLike(item) },
                            onCommentClick = { /* dentro StoryViewer non apriamo commenti */ },
                            onUserClick = { /* tap inutile qui */ },
                        )
                        Spacer(Modifier.height(16.dp))
                        // CTA: apri l'attività intera (lascia la story)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenFullActivity(refId, kind)
                                    onClose()
                                },
                            color = OverlayDark,
                        ) {
                            Text(
                                text = "Apri attività completa →",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    // FeedItem non in cache — fallback minimale
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Caricamento story…",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}
