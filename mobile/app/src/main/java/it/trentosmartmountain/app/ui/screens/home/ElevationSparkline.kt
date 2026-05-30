package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Profilo altimetrico come area-chart con gradiente, stile Strava.
 *
 * I dati (`elevationProfile`, metri assoluti campionati) erano già esposti dal
 * backend ma non venivano mai disegnati: questo componente li valorizza come
 * elemento visivo della card del feed e fallback-hero quando manca la route.
 *
 * Normalizza l'altitudine sul range [min, max] del profilo (non da 0) così
 * anche escursioni con poco dislivello mostrano una silhouette leggibile.
 * Un inset verticale evita che la linea tocchi i bordi del Canvas.
 */
@Composable
fun ElevationSparkline(
    profile: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF4DD0E1),
) {
    Canvas(modifier = modifier) {
        if (profile.size < 2) return@Canvas

        val minV = profile.minOrNull() ?: return@Canvas
        val maxV = profile.maxOrNull() ?: return@Canvas
        val span = max(maxV - minV, 1.0)

        val inset = 4.dp.toPx()
        val usableH = size.height - inset * 2
        val stepX = size.width / (profile.size - 1)

        fun yAt(v: Double): Float =
            inset + (usableH * (1f - ((v - minV) / span).toFloat()))

        val line = Path().apply { moveTo(0f, yAt(profile[0])) }
        val fill = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, yAt(profile[0]))
        }
        for (i in 1 until profile.size) {
            val x = stepX * i
            val y = yAt(profile[i])
            line.lineTo(x, y)
            fill.lineTo(x, y)
        }
        fill.lineTo(size.width, size.height)
        fill.close()

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.38f), lineColor.copy(alpha = 0f)),
            ),
        )
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
