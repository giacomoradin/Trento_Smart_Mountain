package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.ui.graphics.Color

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

/**
 * Rotazione pinch: rapporto tra rotazione delle dita e rotazione dello sticker.
 * 1.0 = 1:1 naturale (Instagram-like): ruoti le dita di 30° → lo sticker ruota di
 * 30°. Prima era 0.22 (super smorzato) → la rotazione sembrava "lenta e
 * complicata" perché serviva un'enorme torsione per poco effetto.
 */
const val StoryRotationGestureFactor = 1.0f

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
