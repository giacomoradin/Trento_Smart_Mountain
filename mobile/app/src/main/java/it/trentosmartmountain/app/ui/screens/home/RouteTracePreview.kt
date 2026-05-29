package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import kotlin.math.cos
import kotlin.math.max

/**
 * "Route signature" stile Strava: disegna la traccia GPS di un'escursione
 * normalizzata nei limiti del Canvas, **senza tile di mappa** (zero rete, zero
 * jank in una lista che scrolla).
 *
 * Scelte tecniche:
 *  - **Proiezione equirettangolare locale**: i gradi lon vengono scalati per
 *    `cos(latitudine media)` così l'aspect ratio del percorso è corretto (a
 *    45° di latitudine 1° di longitudine è ~0.7° di latitudine in distanza).
 *    Senza questa correzione le tracce apparirebbero "schiacciate" in orizzontale.
 *  - **Coordinate relative al primo punto** calcolate in `Double` prima del cast
 *    a `Float`: preserva la precisione (le differenze sono ~0.001°, vicine al
 *    limite di risoluzione del Float a 46° di latitudine assoluta).
 *  - **Aspect-fit + centratura**: il percorso riempie il box mantenendo le
 *    proporzioni, centrato, con padding di sicurezza per i marker.
 *  - Marker **start** (verde) ed **end** (rosso) con anello bianco per stacco
 *    sul fondo scuro, esattamente come Strava.
 *
 * Se [points] ha < 2 elementi non disegna nulla (il chiamante mostra un hero
 * alternativo, es. il profilo altimetrico).
 */
@Composable
fun RouteTracePreview(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF4DD0E1),
    startColor: Color = Color(0xFF4CAF50),
    endColor: Color = Color(0xFFFF6B6B),
) {
    // Proietta una sola volta per ogni lista di punti (evita ricomputo a ogni
    // ricomposizione mentre l'utente scrolla il feed).
    val projected = remember(points) { projectPoints(points) }

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

        // Latitudine cresce verso nord → in alto: invertiamo l'asse Y dello schermo.
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

        // Alone morbido sotto la linea principale (effetto "glow" Strava).
        drawPath(
            path = path,
            color = lineColor.copy(alpha = 0.22f),
            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val start = toScreen(projected.first().x, projected.first().y)
        val end = toScreen(projected.last().x, projected.last().y)
        drawCircle(Color.White, radius = 6.dp.toPx(), center = start)
        drawCircle(startColor, radius = 4.dp.toPx(), center = start)
        drawCircle(Color.White, radius = 6.dp.toPx(), center = end)
        drawCircle(endColor, radius = 4.dp.toPx(), center = end)
    }
}

/** Punto proiettato in coordinate planari relative (metri-equivalenti scalati). */
private data class ProjectedPoint(val x: Float, val y: Float)

/**
 * Proietta i punti lat/lon in coordinate planari relative al primo punto.
 * Sottrae il riferimento in `Double` (precisione piena) PRIMA del cast a Float.
 */
private fun projectPoints(points: List<RoutePoint>): List<ProjectedPoint> {
    if (points.isEmpty()) return emptyList()
    val meanLat = points.sumOf { it.lat } / points.size
    val k = cos(Math.toRadians(meanLat)) // compressione longitudine alla latitudine media
    val refLat = points[0].lat
    val refLon = points[0].lon
    return points.map { p ->
        ProjectedPoint(
            x = ((p.lon - refLon) * k).toFloat(),
            y = (p.lat - refLat).toFloat(),
        )
    }
}
