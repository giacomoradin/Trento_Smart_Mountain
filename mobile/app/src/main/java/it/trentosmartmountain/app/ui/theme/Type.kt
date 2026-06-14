package it.trentosmartmountain.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import it.trentosmartmountain.app.R

/**
 * Identità tipografica TSM (pass "oltre" — giugno 2026).
 *
 * **Space Grotesk** (OFL, bundled in `res/font` — licenza in `assets/fonts/`)
 * come voce di brand: geometrico-tecnico, perfetto per l'identità
 * "telemetria/Electric Data Blue". Font VARIABILE: un solo TTF, i pesi sono
 * istanze via [FontVariation] (minSdk 28 ≥ API 26 richiesto, ok).
 *
 * Gerarchia a due voci:
 *  - **Space Grotesk** su display/headline/title/label → ogni titolo, sezione
 *    ed etichetta dell'app ha la faccia del brand;
 *  - **Roboto di sistema** su body → massima leggibilità dei paragrafi.
 *
 * Applicata via [TsmTheme]: ogni `MaterialTheme.typography.*` la eredita.
 */
@OptIn(ExperimentalTextApi::class)
val TsmDisplayFamily: FontFamily = FontFamily(
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

val TsmTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        displayMedium = displayMedium.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        displaySmall = displaySmall.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineLarge = headlineLarge.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineMedium = headlineMedium.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineSmall = headlineSmall.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleMedium = titleMedium.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
        labelMedium = labelMedium.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
        labelSmall = labelSmall.copy(fontFamily = TsmDisplayFamily, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
    )
}

/**
 * Stili fuori scala Material, per i DATI (il cuore dell'identità TSM).
 *
 * [Numeric]: Space Grotesk con cifre TABULARI (`tnum`): larghezza fissa dei
 * numeri — il cronometro e le metriche live non "ballano" al cambio cifra —
 * ma con la faccia del brand invece del generico monospace.
 */
@OptIn(ExperimentalTextApi::class)
object TsmType {
    val Numeric = TextStyle(
        fontFamily = TsmDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
        letterSpacing = (-0.5).sp,
    )

    /** Variante per numeri-eroe (HUD tracking, contatore crediti). */
    val NumericHero = Numeric.copy(letterSpacing = (-1).sp)
}
