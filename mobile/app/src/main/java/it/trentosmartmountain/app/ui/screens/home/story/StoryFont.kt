package it.trentosmartmountain.app.ui.screens.home.story

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

/**
 * Font selezionabili per il testo delle storie.
 *
 * Dependency-free: usa le `FontFamily` di sistema (Compose) per editor/viewer e i
 * corrispondenti `Typeface` Android per l'export su bitmap. La [key] viene
 * serializzata nel DTO (`StoryEditorDecor.textFont`), così la scelta del font
 * sopravvive alla pubblicazione ed è riprodotta identica nel viewer.
 */
enum class StoryFont(
    val key: String,
    val label: String,
    val composeFamily: FontFamily,
) {
    CLASSIC("classic", "Classico", FontFamily.SansSerif),
    ELEGANT("elegant", "Elegante", FontFamily.Serif),
    MONO("mono", "Mono", FontFamily.Monospace),
    HANDWRITTEN("handwritten", "Corsivo", FontFamily.Cursive),
    ;

    /** Typeface Android equivalente (per StoryBitmapExporter, in grassetto). */
    fun typeface(): Typeface = when (this) {
        CLASSIC -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        ELEGANT -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        // "cursive" è la famiglia corsiva di sistema su Android; fallback a default
        // grassetto se il device non la espone.
        HANDWRITTEN -> runCatching { Typeface.create("cursive", Typeface.BOLD) }
            .getOrDefault(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
    }

    /** Prossimo font nel ciclo (per il toggle rapido nei controlli). */
    fun next(): StoryFont = entries[(ordinal + 1) % entries.size]

    companion object {
        /** Risolve la [key] serializzata; default CLASSIC se sconosciuta/null. */
        fun fromKey(key: String?): StoryFont =
            entries.firstOrNull { it.key == key } ?: CLASSIC
    }
}
