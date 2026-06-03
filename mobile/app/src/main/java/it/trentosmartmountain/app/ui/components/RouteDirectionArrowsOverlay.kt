package it.trentosmartmountain.app.ui.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Overlay che disegna **chevron direzionali** (>>>) lungo la polyline del GPX,
 * stile Komoot/Strava. Le frecce indicano il verso di percorrenza (start → end).
 *
 * Caratteristiche:
 *  - **Adattivo allo zoom**: la spaziatura tra frecce è espressa in pixel
 *    schermo, quindi resta costante visivamente a qualsiasi livello di zoom.
 *  - **Animato**: lo "scorrimento" delle frecce avanza nel tempo per dare la
 *    sensazione di moto verso la fine del percorso. L'animazione è guidata
 *    dall'epoca passata in `phaseProvider()` (in `[0,1)` cyclic): chi ospita
 *    l'overlay (es. uno `withFrameNanos`) calcola la fase, qui solo si disegna.
 *  - **Dual color**: bordo nero per leggibilità su sfondo chiaro/scuro,
 *    riempimento bianco semi-trasparente per non oscurare la traccia.
 *
 * Performance: il path della freccia è ricalcolato a ogni draw (non cacheabile
 * perché lo zoom cambia la conversione coordinate → pixel). Con N≈80 punti
 * ricampionati e ~6 chevrons a vista, il costo è trascurabile.
 */
class RouteDirectionArrowsOverlay(
    private val pointsProvider: () -> List<GeoPoint>,
    private val phaseProvider: () -> Float = { 0f },
    /** Spaziatura nominale tra chevron in pixel (a phase=0). */
    private val gapPx: Float = 110f,
    /** Lunghezza di ciascun chevron in pixel. */
    private val chevronSizePx: Float = 14f,
) : Overlay() {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(220, 10, 20, 35)
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(240, 255, 255, 255)
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val pts = pointsProvider()
        if (pts.size < 2) return
        val proj = mapView.projection
        // Converte tutti i punti in pixel-screen una volta sola, poi misuriamo
        // la lunghezza cumulata per spaziare i chevron in modo isotropo.
        val sp = pts.map { p ->
            val out = android.graphics.Point()
            proj.toPixels(p as IGeoPoint, out)
            out
        }
        val cum = FloatArray(sp.size)
        for (i in 1 until sp.size) {
            cum[i] = cum[i - 1] + hypot(
                (sp[i].x - sp[i - 1].x).toFloat(),
                (sp[i].y - sp[i - 1].y).toFloat(),
            )
        }
        val total = cum.last()
        if (total <= 0f) return

        val gap = max(60f, gapPx)
        val phase = (phaseProvider() % 1f + 1f) % 1f
        // Inizio dell'animazione: trasla l'origine di phase*gap → effetto "scorrimento".
        var d = phase * gap
        // Manteniamo un margine di sicurezza ai due capi per evitare di disegnare
        // un chevron sopra ai marker start/end.
        val safeStart = chevronSizePx * 1.5f
        val safeEnd = total - chevronSizePx * 1.5f
        if (safeEnd <= safeStart) return
        d = d.coerceAtLeast(safeStart)

        var segIdx = 1
        while (d <= safeEnd) {
            while (segIdx < cum.size && cum[segIdx] < d) segIdx++
            if (segIdx >= cum.size) break
            val segStart = sp[segIdx - 1]
            val segEnd = sp[segIdx]
            val segLen = (cum[segIdx] - cum[segIdx - 1]).coerceAtLeast(1f)
            val t = ((d - cum[segIdx - 1]) / segLen).coerceIn(0f, 1f)
            val cx = segStart.x + (segEnd.x - segStart.x) * t
            val cy = segStart.y + (segEnd.y - segStart.y) * t
            val ang = atan2(
                (segEnd.y - segStart.y).toDouble(),
                (segEnd.x - segStart.x).toDouble(),
            ).toFloat()
            drawChevron(canvas, cx, cy, ang)
            d += gap
        }
    }

    private fun drawChevron(canvas: Canvas, cx: Float, cy: Float, angleRad: Float) {
        // Chevron a 90°: due segmenti che dal vertice formano una "V" rovesciata
        // puntata verso angleRad. La "punta" è in cx + s*cos, cy + s*sin.
        val s = chevronSizePx
        val tipX = cx + s * 0.5f * cos(angleRad)
        val tipY = cy + s * 0.5f * sin(angleRad)
        // Punto sinistro e destro a 135° dalla punta.
        val backAng = angleRad + Math.PI.toFloat()
        val leftAng = backAng + 0.5f
        val rightAng = backAng - 0.5f
        val lx = tipX + s * cos(leftAng)
        val ly = tipY + s * sin(leftAng)
        val rx = tipX + s * cos(rightAng)
        val ry = tipY + s * sin(rightAng)
        val path = Path().apply {
            moveTo(lx, ly)
            lineTo(tipX, tipY)
            lineTo(rx, ry)
        }
        canvas.drawPath(path, strokePaint)
        canvas.drawPath(path, fillPaint)
    }
}
