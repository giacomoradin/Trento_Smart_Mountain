package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/** Trasformazione sticker: offset px dal centro del canvas di edit. */
data class StoryStickerTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
)

/** Overlay percorso sopra foto personalizzata. */
enum class RouteOverlayMode {
    /** Solo immagine di sfondo. */
    NONE,
    /** Traccia GPX come sticker sulla foto. */
    TRACE,
    /** Widget (mappa + traccia) mobile sulla foto. */
    MAP_WIDGET,
}

enum class StoryStickerKind {
    /** Mappa + traccia insieme (senza foto di sfondo). */
    MAP_SCENE,
    TRACE,
    MAP_WIDGET,
    TEXT,
}

/** Rotazione pinch: fattore < 1 riduce la sensibilità rispetto al gesto nativo. */
const val StoryRotationGestureFactor = 0.22f

/** Palette rapida per traccia e testo. */
val StoryStickerColors: List<Color> =
    listOf(
        Color(0xFF4DD0E1),
        Color.White,
        Color(0xFFFF5252),
        Color(0xFF4CAF50),
        Color(0xFFFFD54F),
        Color(0xFFFF9800),
    )

/**
 * Applica pan/zoom/rotazione attorno al punto focale del gesto (coordinate canvas, origine al centro).
 */
fun StoryStickerTransform.applyGesture(
    pan: Offset,
    zoom: Float,
    rotationRad: Float,
    pivotInCanvas: Offset,
): StoryStickerTransform {
    val rotation = rotationRad * StoryRotationGestureFactor
    val center = Offset(offsetX, offsetY)
    val fromPivot = center - pivotInCanvas
    val cosR = cos(rotation)
    val sinR = sin(rotation)
    val rotated =
        Offset(
            x = fromPivot.x * cosR - fromPivot.y * sinR,
            y = fromPivot.x * sinR + fromPivot.y * cosR,
        )
    val scaled = rotated * zoom
    val newCenter = pivotInCanvas + scaled + pan
    return copy(
        offsetX = newCenter.x,
        offsetY = newCenter.y,
        scale = (scale * zoom).coerceIn(0.15f, 5f),
        rotationDeg = rotationDeg + Math.toDegrees(rotation.toDouble()).toFloat(),
    )
}

fun Color.toHexRgb(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

/** Scala offset dal canvas di edit alle dimensioni export (stesso aspect 9:16). */
fun StoryStickerTransform.scaledForExport(
    editorWidthPx: Float,
    editorHeightPx: Float,
    exportWidth: Int,
    exportHeight: Int,
): StoryStickerTransform {
    if (editorWidthPx <= 0f || editorHeightPx <= 0f) return this
    val sx = exportWidth / editorWidthPx
    val sy = exportHeight / editorHeightPx
    return copy(
        offsetX = offsetX * sx,
        offsetY = offsetY * sy,
    )
}

fun hexToColor(hex: String?, fallback: Color = Color.White): Color {
    if (hex.isNullOrBlank() || !hex.startsWith("#") || hex.length != 7) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}
