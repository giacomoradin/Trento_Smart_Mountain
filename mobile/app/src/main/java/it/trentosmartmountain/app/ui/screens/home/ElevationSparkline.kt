package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.ui.theme.TsmAccent
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Profilo altimetrico come area-chart con gradiente, stile Strava.
 *
 * Con [distanceKm] e [minAltM]/[maxAltM] disegna anche assi distanza e quote min/max.
 */
@Composable
fun ElevationSparkline(
    profile: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF4DD0E1),
    distanceKm: Double? = null,
    minAltM: Double? = null,
    maxAltM: Double? = null,
    showAxisLabels: Boolean = false,
) {
    val hasAxisData = showAxisLabels && distanceKm != null && distanceKm > 0 &&
        minAltM != null && maxAltM != null && profile.size >= 2

    if (hasAxisData) {
        // Box invece di Column+weight: evita crash Compose "weight only in bounded height".
        Box(modifier = modifier) {
            ElevationSparklineCanvas(
                profile = profile,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 22.dp),
                lineColor = lineColor,
                distanceKm = distanceKm,
                minAltM = minAltM,
                maxAltM = maxAltM,
                drawAxes = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text(
                    "MIN ${minAltM!!.roundToInt()} m",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
                Text(
                    "MAX ${maxAltM!!.roundToInt()} m",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFD700),
                )
            }
        }
    } else {
        ElevationSparklineCanvas(
            profile = profile,
            modifier = modifier,
            lineColor = lineColor,
            drawAxes = false,
        )
    }
}

@Composable
private fun ElevationSparklineCanvas(
    profile: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF4DD0E1),
    distanceKm: Double? = null,
    minAltM: Double? = null,
    maxAltM: Double? = null,
    drawAxes: Boolean = false,
) {
    Canvas(modifier = modifier) {
        if (profile.size < 2) return@Canvas

        val minV = minAltM ?: profile.minOrNull() ?: return@Canvas
        val maxV = maxAltM ?: profile.maxOrNull() ?: return@Canvas
        val span = max(maxV - minV, 1.0)

        val axisHeight = if (drawAxes) 14.dp.toPx() else 0f
        val leftAxisWidth = if (drawAxes) 0f else 0f
        val topPadding = if (drawAxes) 8.dp.toPx() else 4.dp.toPx()
        val chartH = (size.height - axisHeight - topPadding).coerceAtLeast(1f)
        val chartW = size.width - leftAxisWidth
        val stepX = chartW / (profile.size - 1)

        fun yAt(v: Double): Float =
            topPadding + (chartH * (1f - ((v - minV) / span).toFloat())).coerceIn(0f, chartH)

        val line = Path().apply { moveTo(leftAxisWidth, yAt(profile[0])) }
        val fill = Path().apply {
            moveTo(leftAxisWidth, topPadding + chartH)
            lineTo(leftAxisWidth, yAt(profile[0]))
        }
        for (i in 1 until profile.size) {
            val x = leftAxisWidth + stepX * i
            val y = yAt(profile[i])
            line.lineTo(x, y)
            fill.lineTo(x, y)
        }
        fill.lineTo(leftAxisWidth + chartW, topPadding + chartH)
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

        if (drawAxes && distanceKm != null) {
            val maxVal = profile.maxOrNull() ?: 0.0
            val maxIdx = profile.indexOf(maxVal)
            val maxX = leftAxisWidth + maxIdx / (profile.size - 1).toFloat() * chartW
            val maxY = yAt(profile[maxIdx])
            drawCircle(color = TsmAccent, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(maxX, maxY))

            val labelY = topPadding + chartH + 11.dp.toPx()
            val basePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(180, 255, 255, 255)
                textSize = 9.dp.toPx()
                isAntiAlias = true
            }
            val leftPaint = android.graphics.Paint(basePaint).apply {
                textAlign = android.graphics.Paint.Align.LEFT
            }
            val centerPaint = android.graphics.Paint(basePaint).apply {
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val rightPaint = android.graphics.Paint(basePaint).apply {
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            drawContext.canvas.nativeCanvas.apply {
                drawText("0 km", leftAxisWidth, labelY, leftPaint)
                drawText("%.1f km".format(distanceKm / 2), leftAxisWidth + chartW / 2, labelY, centerPaint)
                drawText("%.1f km".format(distanceKm), leftAxisWidth + chartW, labelY, rightPaint)
            }
        }
    }
}

/** Deriva min/max metri da un profilo (assoluto o normalizzato 0..1). */
fun elevationRangeFromProfile(profile: List<Double>): Pair<Double, Double> {
    if (profile.isEmpty()) return 0.0 to 0.0
    val rawMin = profile.minOrNull() ?: 0.0
    val rawMax = profile.maxOrNull() ?: 0.0
    return if (rawMax <= 1.5 && rawMin >= 0.0) {
        0.0 to 1.0
    } else {
        rawMin to rawMax
    }
}
