package it.trentosmartmountain.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Token di motion condivisi (design system — Fase 0).
 *
 * Durate ed easing standard così transizioni, press-state ed enter-animation
 * hanno lo stesso "feel" in tutta l'app. Usare questi al posto di valori sparsi.
 */
object TsmMotion {
    const val FAST = 120      // micro-feedback (press, tap)
    const val MEDIUM = 250    // transizioni standard (enter/exit, crossfade)
    const val SLOW = 400      // movimenti ampi (header collassabili, reveal)
    const val AMBIENT = 6000  // loop ambientali (glow pulsante, ring storie)

    /** Easing "premium" in uscita: parte deciso, rallenta dolce. */
    val EaseOutCubic: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    /** Easing simmetrico per crossfade. */
    val EaseInOut: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    fun <T> tweenFast(easing: Easing = EaseOutCubic): FiniteAnimationSpec<T> =
        tween(durationMillis = FAST, easing = easing)

    fun <T> tweenMedium(easing: Easing = EaseOutCubic): FiniteAnimationSpec<T> =
        tween(durationMillis = MEDIUM, easing = easing)

    fun <T> tweenSlow(easing: Easing = EaseInOut): FiniteAnimationSpec<T> =
        tween(durationMillis = SLOW, easing = easing)

    /** Spring "bouncy" per pop di icone/like. */
    fun <T> springBouncy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

    /** Spring morbido senza overshoot per offset/scale di layout. */
    fun <T> springSmooth(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}
