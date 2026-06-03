package it.trentosmartmountain.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.ui.theme.TsmColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val baseY: Float,
    val speed: Float,
    val radius: Float,
    val twinkle: Float,
)

/**
 * Sfondo "aurora" animato + campo particellare — materiale premium riusabile per
 * le schermate flagship (auth, hero, splash). Blob a gradiente radiale che
 * derivano lentamente (Athletic Orange / Electric Cyan / Tech Navy) + particelle
 * che salgono con un leggero twinkle. Pensato per essere **sottile**: bassa alpha,
 * movimento lento, niente distrazione dai dati.
 */
@Composable
fun TsmAuroraBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = TsmColors.FeedBackground,
    glowWarm: Color = TsmColors.Primary, // Athletic Orange
    glowCool: Color = TsmColors.Cyan,    // Electric Data Blue
    glowDeep: Color = TsmColors.HeroTop, // Tech Navy
    particleCount: Int = 26,
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                baseY = Random.nextFloat(),
                speed = 0.25f + Random.nextFloat() * 0.6f,
                radius = 0.6f + Random.nextFloat() * 1.8f,
                twinkle = Random.nextFloat(),
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize().background(baseColor)) {
        val w = size.width
        val h = size.height
        val t = phase * 2f * PI.toFloat()

        fun blob(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }

        // Tre blob aurora che derivano lentamente. Intensità "decisa ma curata":
        // alpha alzate per renderli percepibili anche su sfondi chiari di card glass.
        blob(glowDeep, w * (0.55f + 0.05f * sin(t * 0.6f)), h * (0.86f + 0.04f * cos(t)), w * 0.78f, 0.42f)
        blob(glowWarm, w * (0.24f + 0.08f * sin(t)), h * (0.18f + 0.05f * cos(t)), w * 0.62f, 0.30f)
        blob(glowCool, w * (0.82f + 0.06f * cos(t * 0.8f)), h * (0.30f + 0.06f * sin(t * 1.1f)), w * 0.55f, 0.22f)

        // Particelle che salgono con twinkle (più presenti ma sempre eteree).
        particles.forEach { p ->
            val y = (((p.baseY - phase * p.speed) % 1f) + 1f) % 1f
            val tw = 0.4f + 0.6f * (0.5f + 0.5f * sin((phase * p.speed * 6f + p.twinkle) * 2f * PI.toFloat()))
            drawCircle(
                color = Color.White.copy(alpha = 0.16f * tw),
                radius = p.radius.dp.toPx(),
                center = Offset(p.x * w, y * h),
            )
        }
    }
}
