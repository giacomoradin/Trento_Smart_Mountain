package it.trentosmartmountain.app.ui.screens.home.story

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.screens.home.projectRoutePoints
import it.trentosmartmountain.app.ui.util.AvatarUtils
import kotlin.math.max
import kotlin.math.min

object StoryBitmapExporter {

    fun exportJpegDataUri(
        width: Int = 1080,
        height: Int = 1920,
        backgroundDataUri: String?,
        routePoints: List<RoutePoint>,
        hasCustomBackground: Boolean,
        routeOverlayMode: RouteOverlayMode,
        mapSceneTransform: StoryStickerTransform,
        routeTransform: StoryStickerTransform,
        mapWidgetTransform: StoryStickerTransform,
        routeColor: Color,
        floatingText: String?,
        textTransform: StoryStickerTransform?,
        textColor: Color,
        textFont: StoryFont = StoryFont.CLASSIC,
        mapSceneBitmap: Bitmap? = null,
        mapWidgetBitmap: Bitmap? = null,
        editorCanvasWidthPx: Float = width.toFloat(),
        editorCanvasHeightPx: Float = height.toFloat(),
        quality: Int = StoryComposerExport.QUALITY,
        /**
         * Se false, NON cuoce traccia/widget/testo nell'immagine: resta solo lo
         * sfondo (foto, gradiente o scena mappa). Gli overlay vengono inviati
         * come `editorDecor` e renderizzati LIVE (animati) dal viewer — è ciò
         * che rende mappa e polyline "dinamiche" nelle storie foto.
         */
        bakeOverlays: Boolean = true,
    ): String? {
        val mapT =
            mapSceneTransform.scaledForExport(editorCanvasWidthPx, editorCanvasHeightPx, width, height)
        val routeT =
            routeTransform.scaledForExport(editorCanvasWidthPx, editorCanvasHeightPx, width, height)
        val widgetT =
            mapWidgetTransform.scaledForExport(editorCanvasWidthPx, editorCanvasHeightPx, width, height)
        val textT =
            textTransform?.scaledForExport(editorCanvasWidthPx, editorCanvasHeightPx, width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Mappa a tutto schermo (scena) come SFONDO: solo senza media e quando
        // l'utente NON ha scelto un overlay esplicito (routeOverlayMode == NONE).
        val wantsMapScene =
            !hasCustomBackground &&
                routeOverlayMode == RouteOverlayMode.NONE &&
                routePoints.size >= 2
        val usableMapScene =
            wantsMapScene && StoryMapSnapshotter.isUsableSnapshot(mapSceneBitmap)

        when {
            usableMapScene -> {
                drawBackground(canvas, width, height, null)
                drawBitmapLayer(canvas, width, height, mapSceneBitmap!!, mapT)
            }
            wantsMapScene -> {
                drawMapSceneFallback(canvas, width, height, routePoints, mapT, routeColor)
            }
            else -> {
                // Sfondo: foto/video data-uri se presente, altrimenti gradiente scuro.
                drawBackground(canvas, width, height, backgroundDataUri)
                // Overlay traccia / widget-mappa — disponibili anche SENZA media di
                // sfondo (si sovrappongono allo sfondo scuro). Rispettano la scelta
                // esplicita dell'utente (routeOverlayMode).
                if (bakeOverlays && routePoints.size >= 2) {
                    when (routeOverlayMode) {
                        RouteOverlayMode.TRACE ->
                            drawRouteSticker(canvas, width, height, routePoints, routeT, routeColor)
                        RouteOverlayMode.MAP_WIDGET -> {
                            val widgetBmp =
                                mapWidgetBitmap?.takeIf { StoryMapSnapshotter.isUsableSnapshot(it) }
                            if (widgetBmp != null) {
                                drawBitmapLayer(canvas, width, height, widgetBmp, widgetT, roundCorners = true)
                            } else {
                                drawMapWidgetSticker(
                                    canvas, width, height, routePoints, widgetT, routeColor,
                                    widgetW = width * 0.72f,
                                    widgetH = height * 0.42f,
                                )
                            }
                        }
                        RouteOverlayMode.NONE -> Unit
                    }
                }
            }
        }

        val text = floatingText?.trim().orEmpty()
        if (bakeOverlays && text.isNotBlank() && textT != null) {
            drawTextSticker(canvas, width, height, text, textT, textColor, textFont)
        }

        val scaled = AvatarUtils.downscaleToBox(bitmap, max(width, height))
        if (scaled !== bitmap) bitmap.recycle()
        return AvatarUtils.encodeToDataUri(scaled, quality)
    }

    private fun drawBitmapLayer(
        canvas: Canvas,
        width: Int,
        height: Int,
        source: Bitmap,
        transform: StoryStickerTransform,
        roundCorners: Boolean = false,
    ) {
        val matrix = Matrix()
        val cx = width / 2f + transform.offsetX
        val cy = height / 2f + transform.offsetY
        matrix.postTranslate(-source.width / 2f, -source.height / 2f)
        matrix.postScale(transform.scale, transform.scale)
        matrix.postRotate(transform.rotationDeg)
        matrix.postTranslate(cx, cy)
        canvas.save()
        canvas.concat(matrix)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (roundCorners) {
            val rect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
            canvas.drawRoundRect(rect, 36f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt() })
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        canvas.restore()
    }

    /** Fallback se lo snapshot OSM non è disponibile: gradiente + traccia a tutta pagina. */
    private fun drawMapSceneFallback(
        canvas: Canvas,
        width: Int,
        height: Int,
        points: List<RoutePoint>,
        transform: StoryStickerTransform,
        lineColor: Color,
    ) {
        drawBackground(canvas, width, height, null)
        drawMapWidgetSticker(
            canvas, width, height, points, transform, lineColor,
            widgetW = width * 0.94f,
            widgetH = height * 0.94f,
        )
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int, backgroundDataUri: String?) {
        val bgBmp = backgroundDataUri?.let { AvatarUtils.decodeDataUri(it) }
        if (bgBmp != null) {
            val matrix = Matrix()
            val scale = max(width.toFloat() / bgBmp.width, height.toFloat() / bgBmp.height)
            val dx = (width - bgBmp.width * scale) / 2f
            val dy = (height - bgBmp.height * scale) / 2f
            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)
            canvas.drawBitmap(bgBmp, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
            return
        }
        val paint = Paint()
        paint.shader =
            LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(0xFF1B2838.toInt(), 0xFF0D1117.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawMapWidgetSticker(
        canvas: Canvas,
        width: Int,
        height: Int,
        points: List<RoutePoint>,
        transform: StoryStickerTransform,
        lineColor: Color,
        widgetW: Float,
        widgetH: Float,
    ) {
        val matrix = Matrix()
        val cx = width / 2f + transform.offsetX
        val cy = height / 2f + transform.offsetY
        matrix.postTranslate(-widgetW / 2f, -widgetH / 2f)
        matrix.postScale(transform.scale, transform.scale)
        matrix.postRotate(transform.rotationDeg)
        matrix.postTranslate(cx, cy)
        canvas.save()
        canvas.concat(matrix)
        val rect = RectF(0f, 0f, widgetW, widgetH)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt() }
        canvas.drawRoundRect(rect, 36f, 36f, bg)
        drawRoutePathInBox(canvas, points, lineColor, widgetW, widgetH, padding = 48f)
        canvas.restore()
    }

    private fun drawRouteSticker(
        canvas: Canvas,
        width: Int,
        height: Int,
        points: List<RoutePoint>,
        transform: StoryStickerTransform,
        lineColor: Color,
    ) {
        val stickerW = width * 0.55f
        val stickerH = height * 0.32f
        val matrix = Matrix()
        val cx = width / 2f + transform.offsetX
        val cy = height / 2f + transform.offsetY
        matrix.postTranslate(-stickerW / 2f, -stickerH / 2f)
        matrix.postScale(transform.scale, transform.scale)
        matrix.postRotate(transform.rotationDeg)
        matrix.postTranslate(cx, cy)
        canvas.save()
        canvas.concat(matrix)
        drawRoutePathInBox(canvas, points, lineColor, stickerW, stickerH, padding = 24f)
        canvas.restore()
    }

    private fun drawRoutePathInBox(
        canvas: Canvas,
        points: List<RoutePoint>,
        lineColor: Color,
        boxW: Float,
        boxH: Float,
        padding: Float,
    ) {
        val projected = projectRoutePoints(points)
        if (projected.size < 2) return
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (p in projected) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }
        val spanX = max(maxX - minX, 1e-6f)
        val spanY = max(maxY - minY, 1e-6f)
        val availW = boxW - padding * 2
        val availH = boxH - padding * 2
        val scale = min(availW / spanX, availH / spanY)
        val path = Path()
        fun toLocal(px: Float, py: Float): Pair<Float, Float> {
            val x = padding + (px - minX) * scale
            val y = padding + (maxY - py) * scale
            return x to y
        }
        val first = toLocal(projected[0].x, projected[0].y)
        path.moveTo(first.first, first.second)
        for (i in 1 until projected.size) {
            val s = toLocal(projected[i].x, projected[i].y)
            path.lineTo(s.first, s.second)
        }
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor.copy(alpha = 0.22f).toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, glow)
        canvas.drawPath(path, stroke)

        val start = toLocal(projected.first().x, projected.first().y)
        val end = toLocal(projected.last().x, projected.last().y)
        drawRouteEndpoint(canvas, start.first, start.second, isStart = true)
        drawRouteEndpoint(canvas, end.first, end.second, isStart = false)
    }

    private fun drawRouteEndpoint(canvas: Canvas, x: Float, y: Float, isStart: Boolean) {
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        val fill =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    if (isStart) {
                        android.graphics.Color.parseColor("#4CAF50")
                    } else {
                        android.graphics.Color.parseColor("#FF6B6B")
                    }
            }
        canvas.drawCircle(x, y, 11f, ring)
        canvas.drawCircle(x, y, 8f, fill)
    }

    private fun drawTextSticker(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        transform: StoryStickerTransform,
        textColor: Color,
        textFont: StoryFont,
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor.toArgb()
                textSize = 52f * transform.scale.coerceIn(0.5f, 2f)
                typeface = textFont.typeface()
            }
        val bounds = RectF()
        val textWidth = paint.measureText(text)
        bounds.set(0f, 0f, textWidth + 32f, paint.textSize + 24f)
        val matrix = Matrix()
        val cx = width / 2f + transform.offsetX
        val cy = height / 2f + transform.offsetY
        matrix.postTranslate(-bounds.width() / 2f, -bounds.height() / 2f)
        matrix.postScale(transform.scale, transform.scale)
        matrix.postRotate(transform.rotationDeg)
        matrix.postTranslate(cx, cy)
        canvas.save()
        canvas.concat(matrix)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x59000000 }
        canvas.drawRoundRect(bounds, 16f, 16f, bg)
        canvas.drawText(text, 16f, bounds.height() - 20f, paint)
        canvas.restore()
    }
}

object StoryComposerExport {
    // Bump 80 → 92: 80 produceva macroblock visibili sui dettagli mappa/foto;
    // 92 è il livello di referenza per la pubblicazione su feed social (≈ IG).
    const val QUALITY = 92
    const val WIDTH = 1080
    const val HEIGHT = 1920
}
