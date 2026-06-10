package it.trentosmartmountain.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.ui.theme.TsmColors

/**
 * Brush "shimmer" animato per i placeholder di caricamento (effetto scheletro).
 * Una banda chiara scorre orizzontalmente su una base scura, ripetuta all'infinito.
 */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    return Brush.linearGradient(
        colors = listOf(Color(0xFF26262B), Color(0xFF36363D), Color(0xFF26262B)),
        start = Offset(x - 400f, 0f),
        end = Offset(x, 0f),
    )
}

/** Blocco rettangolare con shimmer (mattoncino base degli scheletri). */
@Composable
fun SkeletonBox(modifier: Modifier, shape: Shape = RoundedCornerShape(6.dp)) {
    Box(modifier = modifier.clip(shape).background(shimmerBrush()))
}

/** Scheletro del feed sociale: 3 card placeholder (header + hero + stat strip). */
@Composable
fun FeedSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(3) { SkeletonFeedCard() }
    }
}

@Composable
private fun SkeletonFeedCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TsmColors.Card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(Modifier.size(44.dp), CircleShape)
                Spacer(Modifier.width(10.dp))
                Column {
                    SkeletonBox(Modifier.height(12.dp).width(140.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBox(Modifier.height(10.dp).width(90.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            SkeletonBox(Modifier.fillMaxWidth().height(160.dp), RoundedCornerShape(8.dp))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { SkeletonBox(Modifier.weight(1f).height(30.dp)) }
            }
        }
    }
}

/** Scheletro generico per liste (utenti/notifiche/follower): N righe avatar + testo. */
@Composable
fun ListSkeleton(modifier: Modifier = Modifier, rows: Int = 6) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(Modifier.size(44.dp), CircleShape)
                Spacer(Modifier.width(12.dp))
                Column {
                    SkeletonBox(Modifier.height(12.dp).width(160.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBox(Modifier.height(10.dp).width(100.dp))
                }
            }
        }
    }
}
