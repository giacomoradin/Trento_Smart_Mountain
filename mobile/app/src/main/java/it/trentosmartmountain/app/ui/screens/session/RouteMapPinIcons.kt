package it.trentosmartmountain.app.ui.screens.session

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

enum class RouteEndpointKind {
    /** Partenza — cerchio verde. */
    START,
    /** Arrivo — bandiera a scacchi. */
    FINISH,
}

/** Icona partenza/arrivo per percorso pianificato (Registra, anteprima GPX). */
internal fun buildRouteEndpointIcon(density: Float, kind: RouteEndpointKind): Drawable =
    when (kind) {
        RouteEndpointKind.START -> buildGreenStartIcon(density)
        RouteEndpointKind.FINISH -> buildCheckeredFlagIcon(density)
    }

/** Compatibilità con [SentieroMarkerType] usato in Pianifica. */
internal fun buildRouteEndpointIcon(density: Float, type: SentieroMarkerType): Drawable =
    when (type) {
        SentieroMarkerType.START -> buildGreenStartIcon(density)
        SentieroMarkerType.SELECTED_DESTINATION -> buildCheckeredFlagIcon(density)
        SentieroMarkerType.DESTINATION -> buildRoutePinIcon(density, "#4FC3F7")
    }

private fun buildGreenStartIcon(density: Float): Drawable {
    val sizePx = (34 * density).toInt().coerceAtLeast(28)
    val bmp = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bmp)
    val r = sizePx / 2f
    val fill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#43A047")
        }
    val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }
    canvas.drawCircle(r, r, r - 2.5f * density, fill)
    canvas.drawCircle(r, r, r - 2.5f * density, stroke)
    return BitmapDrawable(android.content.res.Resources.getSystem(), bmp)
}

private fun buildCheckeredFlagIcon(density: Float): Drawable {
    val w = (34 * density).toInt().coerceAtLeast(28)
    val h = (30 * density).toInt().coerceAtLeast(24)
    val bmp = createBitmap(w, h)
    val canvas = Canvas(bmp)
    val polePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#616161")
            strokeWidth = (2.5f * density).coerceAtLeast(2f)
            strokeCap = Paint.Cap.ROUND
        }
    val poleX = 4f * density
    canvas.drawLine(poleX, 2f * density, poleX, h - 2f * density, polePaint)

    val flagLeft = poleX + 2f * density
    val flagTop = 3f * density
    val flagW = w - flagLeft - 2f * density
    val flagH = h * 0.55f
    val cols = 4
    val rows = 3
    val cellW = flagW / cols
    val cellH = flagH / rows
    val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val paint = if ((row + col) % 2 == 0) black else white
            canvas.drawRect(
                flagLeft + col * cellW,
                flagTop + row * cellH,
                flagLeft + (col + 1) * cellW,
                flagTop + (row + 1) * cellH,
                paint,
            )
        }
    }
    val border =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
    canvas.drawRect(flagLeft, flagTop, flagLeft + flagW, flagTop + flagH, border)
    return BitmapDrawable(android.content.res.Resources.getSystem(), bmp)
}

private fun buildRoutePinIcon(density: Float, colorHex: String): Drawable {
    val sizePx = (28 * density).toInt().coerceAtLeast(24)
    val bmp = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bmp)
    val r = sizePx / 2f
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor(colorHex) }
    val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
    canvas.drawCircle(r, r, r - 2f * density, fill)
    canvas.drawCircle(r, r, r - 2f * density, stroke)
    return BitmapDrawable(android.content.res.Resources.getSystem(), bmp)
}
