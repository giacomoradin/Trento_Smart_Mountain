package it.trentosmartmountain.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette **estesa** del tema scuro, sorgente unica per i colori che finora
 * venivano ridefiniti `private val` in decine di schermate (DarkSurface,
 * AccentCyan, TextSecondary, CardBackground, …) con valori leggermente diversi
 * tra loro.
 *
 * Obiettivo: un solo posto da cui pescare → coerenza visiva garantita e
 * re-theming banale. I valori qui sono quelli **canonici** del cluster Social
 * (la parte più curata dell'app), così adottarli altrove allinea senza
 * introdurre regressioni dove erano già questi.
 *
 * NB: restano distinti da [TsmAccent] (0xFF4FC3F7) e [TsmSurface] (0xFF1E1E1E)
 * di Color.kt, che il Material theme usa per i componenti standard. Questa
 * palette è per le superfici "custom" (feed, card, bottom sheet) che hanno una
 * loro identità più scura/contrastata.
 */
object TsmColors {
    // ── Superfici ─────────────────────────────────────────────────────────
    /** Sfondo delle schermate scure a tutta pagina (feed, bottom sheet). */
    val FeedBackground = Color(0xFF1C1C1E)
    /** Sfondo card del feed. */
    val Card = Color(0xFF242427)
    /** Superficie più chiara: input, card secondarie, chip. */
    val CardElevated = Color(0xFF2C2C2E)
    /** Sfondo "hero" scuro (route signature / altimetria). */
    val HeroTop = Color(0xFF1B1B1F)
    val HeroBottom = Color(0xFF101012)
    /** Linea divisore sottile su superfici scure. */
    val Divider = Color(0xFF34343A)

    // ── Accenti ───────────────────────────────────────────────────────────
    /** Ciano del cluster Social (azioni, anelli story, link). */
    val Cyan = Color(0xFF4DD0E1)
    /** Verde "ok / attività libera / start". */
    val Success = Color(0xFF66BB6A)
    /** Rosso "like / end marker / azione affettiva" (≠ TsmSos critico). */
    val Danger = Color(0xFFFF6B6B)
    /** Oro punteggio/crediti. */
    val Gold = Color(0xFFFFC107)

    // ── Testo ─────────────────────────────────────────────────────────────
    /** Testo primario quasi-bianco (più morbido del bianco puro su nero). */
    val TextPrimary = Color(0xFFF2F2F4)
    /** Testo secondario / label / metadati. */
    val TextSecondary = Color(0xFF8E8E93)
    /** Grigio metadati leggermente più chiaro (meta-riga card, stati neutri). */
    val TextTertiary = Color(0xFF9A9AA0)
}

/**
 * Colore semantico della difficoltà escursionistica (scala CAI).
 * Centralizzato perché compariva replicato in FeedCard, dettaglio sessione,
 * pianificazione e profilo, a volte con tinte incoerenti.
 *
 *  - **T**   (Turistico)            → verde
 *  - **E**   (Escursionistico)      → azzurro
 *  - **EE**  (Esperti)              → arancione
 *  - **EEA** (Esperti Attrezzatura) → rosso
 */
fun difficultyColor(level: String?): Color = when (level?.uppercase()) {
    "T" -> Color(0xFF4CAF50)
    "E" -> Color(0xFF4FC3F7)
    "EE" -> Color(0xFFFF9800)
    "EEA" -> Color(0xFFE53935)
    else -> Color(0xFF9A9AA0)
}
