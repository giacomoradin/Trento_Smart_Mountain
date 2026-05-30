package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.data.remote.dto.SocialRowItem
import it.trentosmartmountain.app.ui.components.AvatarImage

private val LiveYellow = Color(0xFFFFC107)
private val StoryCyan = Color(0xFF4DD0E1)
private val GoalGreen = Color(0xFF4CAF50)
private val NeutralGray = Color(0xFF3A3A3C)
private val GoalTrack = Color(0xFF2A4A2A)
private val TextSecondary = Color(0xFF8E8E93)

/**
 * Status risolto LATO CLIENT, post-filtering del set `viewedStoryIds`.
 *
 * Convertiamo lo `SocialRowItem.status` del server in uno di questi:
 *  - LIVE   → anello giallo animato (pulse alpha 0.4..1.0)
 *  - STORY  → anello azzurro pieno se NON in viewed; altrimenti degrada a
 *             GOAL (se `weeklyProgressPct` disponibile) o NEUTRAL
 *  - GOAL   → arco verde proporzionale (0..1)
 *  - NEUTRAL → cerchio grigio sottile
 */
private enum class ResolvedRingStatus { LIVE, STORY, GOAL, NEUTRAL }

private data class ResolvedRow(
    val item: SocialRowItem,
    val status: ResolvedRingStatus,
    /** Solo per status GOAL: percentuale 0..1. */
    val goalPct: Float = 0f,
)

private fun resolveStatus(item: SocialRowItem, viewedIds: Set<String>): ResolvedRow {
    return when (item.status) {
        "live" -> ResolvedRow(item, ResolvedRingStatus.LIVE)
        "story" -> {
            val refId = item.storyActivityRef?.id
            if (refId != null && refId in viewedIds) {
                // Story già vista: degrada a goal/neutral.
                val pct = item.weeklyProgressPct ?: 0f
                if (pct > 0f) ResolvedRow(item, ResolvedRingStatus.GOAL, pct)
                else ResolvedRow(item, ResolvedRingStatus.NEUTRAL)
            } else {
                ResolvedRow(item, ResolvedRingStatus.STORY)
            }
        }
        "goal" -> ResolvedRow(item, ResolvedRingStatus.GOAL, item.weeklyProgressPct ?: 0f)
        else -> ResolvedRow(item, ResolvedRingStatus.NEUTRAL)
    }
}

/**
 * Avatar Row in cima al feed Social: lista orizzontale scrollabile di
 * follower con anello stato.
 *
 * Tap sull'avatar:
 *  - LIVE  → apre la SessionDetail (deep link `liveSessionId`)
 *  - STORY → apre lo StoryViewerScreen + chiama `onMarkStoryViewed`
 *  - GOAL / NEUTRAL → apre il profilo utente
 *
 * @param items Lista raw dal server (post-priority assignment lato backend)
 * @param viewedStoryIds Set degli `activityRefId` già visti localmente
 * @param onUserClick Default action (profilo utente)
 * @param onLiveClick Apre SessionDetail per live session
 * @param onStoryClick Apre StoryViewerScreen + marca come vista
 */
@Composable
fun AvatarRow(
    items: List<SocialRowItem>,
    viewedStoryIds: Set<String>,
    onUserClick: (userId: String) -> Unit,
    onLiveClick: (sessionId: String) -> Unit,
    onStoryClick: (refId: String, kind: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return // nessun follower → niente row

    val resolved = items.map { resolveStatus(it, viewedStoryIds) }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = resolved, key = { it.item.user._id }) { entry ->
            AvatarRowItem(
                entry = entry,
                onClick = {
                    when (entry.status) {
                        ResolvedRingStatus.LIVE -> entry.item.liveSessionId?.let(onLiveClick)
                        ResolvedRingStatus.STORY -> entry.item.storyActivityRef?.let { ref ->
                            onStoryClick(ref.id, ref.kind)
                        }
                        else -> onUserClick(entry.item.user._id)
                    }
                },
            )
        }
    }
}

@Composable
private fun AvatarRowItem(
    entry: ResolvedRow,
    onClick: () -> Unit,
) {
    val user = entry.item.user
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Anello stato sotto l'avatar (Canvas customizzato per arco proporzionale GOAL)
            StatusRing(status = entry.status, goalPct = entry.goalPct)
            // Avatar al centro (ridotto di ~8dp così l'anello è visibile)
            AvatarImage(
                avatarUrl = user.avatarUrl,
                fallbackName = user.username,
                size = 52.dp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = user.username?.take(10) ?: "?",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        // Sub-label per LIVE (sempre visibile come hint)
        if (entry.status == ResolvedRingStatus.LIVE) {
            Text(
                text = "● LIVE",
                color = LiveYellow,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun StatusRing(
    status: ResolvedRingStatus,
    goalPct: Float,
) {
    // Animazione pulse solo per LIVE (alpha sinusoidale via tween infinito)
    val pulseAlpha = if (status == ResolvedRingStatus.LIVE) {
        val transition = rememberInfiniteTransition(label = "live-pulse")
        transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "alpha",
        ).value
    } else 1f

    Canvas(modifier = Modifier.size(64.dp)) {
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2f
        val drawSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)
        when (status) {
            ResolvedRingStatus.LIVE -> drawArc(
                color = LiveYellow.copy(alpha = pulseAlpha),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = drawSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            ResolvedRingStatus.STORY -> drawArc(
                color = StoryCyan,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = drawSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            ResolvedRingStatus.GOAL -> {
                // Track grigio + arco verde proporzionale
                drawArc(
                    color = GoalTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = drawSize,
                    style = Stroke(width = strokeWidth),
                )
                drawArc(
                    color = GoalGreen,
                    startAngle = -90f,
                    sweepAngle = 360f * goalPct.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = drawSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            ResolvedRingStatus.NEUTRAL -> drawArc(
                color = NeutralGray,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = drawSize,
                style = Stroke(width = (strokeWidth * 0.6f)),
            )
        }
    }
}
