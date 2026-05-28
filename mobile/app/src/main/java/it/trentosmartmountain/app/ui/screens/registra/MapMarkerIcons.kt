package it.trentosmartmountain.app.ui.screens.registra

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

/** Colori marker mappa live (hex). */
object MapMarkerColors {
  const val SELF = 0xFF4CAF50.toInt() // verde
  const val MEMBER = 0xFF29B6F6.toInt() // azzurro
  const val LEADER = 0xFFFFC107.toInt() // oro
  const val SOS = 0xFFE53935.toInt() // rosso
}

enum class LiveMarkerKind {
  SELF,
  MEMBER,
  LEADER,
  SOS,
}

/**
 * Genera icone circolari per i marker OSMdroid (dimensioni e badge SOS).
 */
object MapMarkerIcons {
  private const val STROKE_DP = 2.5f

  fun create(
    context: Context,
    kind: LiveMarkerKind,
    sizeDp: Float,
    withSosBadge: Boolean = kind == LiveMarkerKind.SOS,
  ): Drawable {
    val density = context.resources.displayMetrics.density
    val badgeExtraDp = if (withSosBadge) 10f else 0f
    val canvasDp = sizeDp + badgeExtraDp
    val sizePx = (canvasDp * density).roundToInt().coerceAtLeast(24)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val fillColor =
      when (kind) {
        LiveMarkerKind.SELF -> MapMarkerColors.SELF
        LiveMarkerKind.MEMBER -> MapMarkerColors.MEMBER
        LiveMarkerKind.LEADER -> MapMarkerColors.LEADER
        LiveMarkerKind.SOS -> MapMarkerColors.SOS
      }

    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val dotRadiusPx = (sizeDp * density / 2f) - (STROKE_DP * density)

    val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
      }
    val fillPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
      }

    canvas.drawCircle(centerX, centerY, dotRadiusPx + STROKE_DP * density, strokePaint)
    canvas.drawCircle(centerX, centerY, dotRadiusPx, fillPaint)

    if (withSosBadge) {
      drawSosBadge(canvas, density, sizePx, dotRadiusPx, centerX, centerY)
    }

    return BitmapDrawable(context.resources, bitmap)
  }

  private fun drawSosBadge(
    canvas: Canvas,
    density: Float,
    sizePx: Int,
    dotRadiusPx: Float,
    centerX: Float,
    centerY: Float,
  ) {
    val badgeRadius = 7f * density
    val badgeCx = centerX + dotRadiusPx * 0.55f
    val badgeCy = centerY - dotRadiusPx * 0.55f

    val badgeBg =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
      }
    val badgeRing =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = MapMarkerColors.SOS
      }
    canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgeBg)
    canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgeRing)

    val textPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MapMarkerColors.SOS
        textSize = 11f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val textY = badgeCy - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText("!", badgeCx, textY, textPaint)
  }

  fun markerSizeDp(kind: LiveMarkerKind, isSelf: Boolean): Float =
    when {
      kind == LiveMarkerKind.SOS -> 38f
      isSelf -> 36f
      kind == LiveMarkerKind.LEADER -> 34f
      else -> 28f
    }

  fun kindForUser(
    isSelf: Boolean,
    isLeader: Boolean,
    hasSos: Boolean,
  ): LiveMarkerKind =
    when {
      hasSos -> LiveMarkerKind.SOS
      isLeader -> LiveMarkerKind.LEADER
      isSelf -> LiveMarkerKind.SELF
      else -> LiveMarkerKind.MEMBER
    }
}
