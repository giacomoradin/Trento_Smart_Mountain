package it.trentosmartmountain.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Preset di gradienti riusabili (design system — Fase 0).
 *
 * Centralizza i [Brush] ricorrenti così ogni schermata usa lo stesso linguaggio
 * cromatico "montagna + tech" senza duplicare liste di colori. Tutti basati su
 * [TsmColors]. I gradienti lineari sono size-independent; per i radiali/mesh
 * usiamo `Offset.Infinite`/`radius` ampi così coprono il contenitore.
 */
object TsmGradients {

    /** Aurora verticale: Tech Navy → background assoluto. Sfondo schermate. */
    val auroraVertical: Brush
        get() = Brush.verticalGradient(listOf(TsmColors.HeroTop, TsmColors.FeedBackground))

    /** Crepuscolo montano: Navy → tocco arancio → nero. Hero/header premium. */
    val mountainDusk: Brush
        get() = Brush.verticalGradient(
            0.0f to TsmColors.HeroTop,
            0.55f to Color(0xFF0A2230),
            0.78f to Color(0xFF1A1410), // alone arancio bruciato molto tenue
            1.0f to TsmColors.FeedBackground,
        )

    /** Materiale card glass: CardElevated → Card. */
    val glassCard: Brush
        get() = Brush.verticalGradient(listOf(TsmColors.CardElevated, TsmColors.Card))

    /** Riempimento CTA primaria (Athletic Orange). */
    val primaryFill: Brush
        get() = Brush.horizontalGradient(listOf(TsmColors.Primary, TsmColors.PrimaryDark))

    /** Riempimento CTA "data" (Cyan → teal scuro). */
    val cyanFill: Brush
        get() = Brush.horizontalGradient(listOf(TsmColors.Cyan, Color(0xFF1C8A8E)))

    /** Accento verde "summit/share". */
    val summitGreen: Brush
        get() = Brush.horizontalGradient(listOf(Color(0xFF3FD27E), Color(0xFF1B9E5A)))

    /** Alone dorato per traguardi/badge. */
    val summitGold: Brush
        get() = Brush.horizontalGradient(listOf(TsmColors.Gold, Color(0xFFE0A800)))

    /** Allerta (bacheca pericolo / azioni distruttive). */
    val dangerFill: Brush
        get() = Brush.horizontalGradient(listOf(TsmColors.Danger, TsmColors.Offline))

    /**
     * Bagliore radiale morbido di [color] (per glow dietro elementi). [alpha] di
     * partenza al centro; sfuma a trasparente.
     */
    fun radialGlow(color: Color, alpha: Float = 0.5f, radius: Float = 600f): Brush =
        Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = Offset.Unspecified,
            radius = radius,
        )

    /**
     * Tinta che vira con la **quota media** (0 = valle → 1 = vetta): verde →
     * cyan → viola. Usata per sfondi reattivi al contesto altimetrico.
     */
    fun altitudeTint(intensity01: Float): Brush {
        val t = intensity01.coerceIn(0f, 1f)
        val top = lerp3(
            Color(0xFF0B3B2E), // valle (verde profondo)
            Color(0xFF0A2F45), // mezza quota (blu)
            Color(0xFF241A3A), // vetta (viola notte)
            t,
        )
        return Brush.verticalGradient(listOf(top, TsmColors.FeedBackground))
    }

    private fun lerp3(a: Color, b: Color, c: Color, t: Float): Color =
        if (t < 0.5f) androidx.compose.ui.graphics.lerp(a, b, t * 2f)
        else androidx.compose.ui.graphics.lerp(b, c, (t - 0.5f) * 2f)
}
