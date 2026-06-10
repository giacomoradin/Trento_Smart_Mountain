package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Profilo altimetrico — area chart Strava-style.
 *
 * Aggiornamento (C2/C3): il chart "ricco" (gradiente colorato per altitudine +
 * marker sul punto massimo) è ora usato SEMPRE — anche nelle card compatte del
 * feed, che prima ricadevano sul vecchio canvas piatto. Gli **assi** e le label
 * MIN/MAX restano gated da [showAxisLabels] + altezza disponibile: nel feed
 * (card bassa) mostriamo solo la curva colorata, nel dettaglio anche header,
 * assi distanza e footer quote.
 *
 *  - colore curva: lerp(verde→arancio) in base all'altitudine media (pianura → vetta)
 *  - marker cerchio TsmAccent sul punto massimo
 *  - assi X (0/D2/D km) e label MIN/MAX in metri assoluti (solo se showAxisLabels)
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
    if (profile.size < 2) {
        // Niente dati sufficienti: placeholder neutro invece di un canvas vuoto.
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Profilo altimetrico non disponibile",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
            )
        }
        return
    }

    // Min/max effettivi: usiamo quelli passati (metri assoluti) quando presenti,
    // altrimenti li deriviamo dal profilo (gestisce sia valori 0..1 che metri).
    val (effMin, effMax) = remember(profile, minAltM, maxAltM) {
        if (minAltM != null && maxAltM != null && maxAltM > minAltM) {
            minAltM to maxAltM
        } else {
            elevationRangeFromProfile(profile)
        }
    }
    val hasAxisData = showAxisLabels && distanceKm != null && distanceKm > 0

    if (hasAxisData) {
        // Layout completo (dettaglio / pager alto): header + chart + assi + footer.
        Column(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "PROFILO ALTIMETRICO",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = Color.White.copy(alpha = 0.65f),
                )
                Text(
                    "+${(effMax - effMin).roundToInt()} m D+",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TsmPrimary,
                )
            }
            Spacer(Modifier.height(6.dp))
            ElevationDetailCanvas(
                profile = profile,
                distanceKm = distanceKm,
                minAltM = effMin,
                maxAltM = effMax,
                drawAxes = true,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "MIN ${effMin.roundToInt()} m",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
                Text(
                    "MAX ${effMax.roundToInt()} m",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFD700),
                )
            }
        }
    } else {
        // Card compatta (feed): SOLO la curva ricca colorata, senza assi.
        ElevationDetailCanvas(
            profile = profile,
            distanceKm = distanceKm ?: 0.0,
            minAltM = effMin,
            maxAltM = effMax,
            drawAxes = false,
            modifier = modifier,
        )
    }
}

/**
 * Canvas profilo altimetrico ricco — versione condivisa tra il dettaglio
 * attività (`ActivityDetailScreen.ElevationProfileChart`) e il pager
 * (`TsmRouteElevationPager`). Mantiene la stessa estetica per coerenza visiva.
 *
 * Il `profile` accetta sia valori normalizzati 0..1 sia metri assoluti: viene
 * sempre rinormalizzato sul proprio min/max prima del render, così i due
 * call site non devono pre-normalizzare in modo coerente.
 */
@Composable
private fun ElevationDetailCanvas(
    profile: List<Double>,
    distanceKm: Double,
    minAltM: Double,
    maxAltM: Double,
    drawAxes: Boolean,
    modifier: Modifier = Modifier,
) {
    val green = TsmPrimary
    val red = Color(0xFFFF5722)
    val avgAltM = (minAltM + maxAltM) / 2.0
    val altIntensity = ((avgAltM - 500.0) / 2000.0).coerceIn(0.0, 1.0).toFloat()
    val lineColor = lerp(green, red, altIntensity)

    Canvas(modifier = modifier) {
        if (profile.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val n = profile.size
        val axisHeight = if (drawAxes) 16.dp.toPx() else 4.dp.toPx()
        val topPadding = 6.dp.toPx()
        val chartH = (h - axisHeight - topPadding).coerceAtLeast(1f)

        // Normalizza su min/max del profilo stesso (così l'amplitude
        // visibile è massima, indipendentemente da come arriva il dato).
        val pMin = profile.min()
        val pMax = profile.max()
        val pSpan = max(pMax - pMin, 1e-6)

        val path = Path()
        val fillPath = Path()
        profile.forEachIndexed { i, v ->
            val x = i / (n - 1).toFloat() * w
            val norm = ((v - pMin) / pSpan).toFloat().coerceIn(0f, 1f)
            val y = topPadding + (1f - norm) * chartH
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, topPadding + chartH)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, topPadding + chartH)
        fillPath.close()

        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.5f), lineColor.copy(alpha = 0.15f)),
                startY = topPadding,
                endY = topPadding + chartH,
            ),
        )
        drawPath(
            path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Marker punto massimo.
        val maxIdx = profile.indexOf(pMax)
        val maxX = maxIdx / (n - 1).toFloat() * w
        val maxY = topPadding + (1f - ((profile[maxIdx] - pMin) / pSpan).toFloat()) * chartH
        drawCircle(color = TsmAccent, radius = 4.dp.toPx(), center = Offset(maxX, maxY))

        // Assi X (solo nel layout esteso; nel feed lo spazio non basta).
        if (drawAxes && distanceKm > 0) {
            val labelY = topPadding + chartH + 12.dp.toPx()
            val basePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 9.dp.toPx()
                isAntiAlias = true
            }
            val leftPaint = android.graphics.Paint(basePaint).apply { textAlign = android.graphics.Paint.Align.LEFT }
            val centerPaint = android.graphics.Paint(basePaint).apply { textAlign = android.graphics.Paint.Align.CENTER }
            val rightPaint = android.graphics.Paint(basePaint).apply { textAlign = android.graphics.Paint.Align.RIGHT }
            drawContext.canvas.nativeCanvas.apply {
                drawText("0 km", 0f, labelY, leftPaint)
                drawText("%.1f km".format(distanceKm / 2), w / 2, labelY, centerPaint)
                drawText("%.1f km".format(distanceKm), w, labelY, rightPaint)
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
