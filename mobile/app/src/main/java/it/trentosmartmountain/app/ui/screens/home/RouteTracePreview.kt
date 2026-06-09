package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import kotlin.math.max

/**
 * "Route signature" stile Strava: disegna la traccia GPS di un'escursione
 * normalizzata nei limiti del Canvas, **senza tile di mappa** (zero rete, zero
 * jank in una lista che scrolla).
 */
@Composable
fun RouteTracePreview(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF4DD0E1),
    startColor: Color = Color(0xFF4CAF50),
    endColor: Color = Color(0xFFFF6B6B),
) {
    val projected = remember(points) { projectRoutePoints(points) }

    // Animazione tratteggiata per far sembrare il tracciato "animato" (Bug 5)
    val transition = rememberInfiniteTransition(label = "route-dash")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        if (projected.size < 2) return@Canvas

        val padding = 18.dp.toPx()

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (p in projected) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }

        val spanX = max(maxX - minX, 1e-6f)
        val spanY = max(maxY - minY, 1e-6f)
        val availW = size.width - padding * 2
        val availH = size.height - padding * 2
        if (availW <= 0f || availH <= 0f) return@Canvas

        // Aspect-fit: scala uguale su entrambi gli assi per non distorcere.
        val scale = minOf(availW / spanX, availH / spanY)
        val drawW = spanX * scale
        val drawH = spanY * scale
        val offsetX = padding + (availW - drawW) / 2f
        val offsetY = padding + (availH - drawH) / 2f

        fun toScreen(px: Float, py: Float): Offset =
            Offset(
                x = offsetX + (px - minX) * scale,
                y = offsetY + (maxY - py) * scale,
            )

        val path = Path()
        val first = toScreen(projected[0].x, projected[0].y)
        path.moveTo(first.x, first.y)
        for (i in 1 until projected.size) {
            val s = toScreen(projected[i].x, projected[i].y)
            path.lineTo(s.x, s.y)
        }

        // Alone morbido sotto la linea principale
        drawPath(
            path = path,
            color = lineColor.copy(alpha = 0.22f),
            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Linea principale animata con tratteggio che scorre
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 30f), phase)
            ),
        )

        val start = toScreen(projected.first().x, projected.first().y)
        val end = toScreen(projected.last().x, projected.last().y)
        drawCircle(Color.White, radius = 6.dp.toPx(), center = start)
        drawCircle(startColor, radius = 4.dp.toPx(), center = start)
        drawCircle(Color.White, radius = 6.dp.toPx(), center = end)
        drawCircle(endColor, radius = 4.dp.toPx(), center = end)
    }
}

