package it.trentosmartmountain.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
    // Rispetta "riduci animazioni" di sistema: blob e particelle restano statici.
    val reduceMotion = rememberReduceMotion()

    // ── Blob: deriva lenta, COSTOSA (3 gradienti radiali quasi full-screen). ──
    // Quantizzata ~8/s: i blob si muovono pochissimo, lo step è impercettibile su
    // di loro e risparmia fill-rate. (Era questo lo step che rendeva "laggose" le
    // particelle quando condividevano la stessa fase: ora sono separate.)
    val blobTransition = rememberInfiniteTransition(label = "aurora-blobs")
    val blobAnim = blobTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aurora-blob-phase",
    )
    val phase by remember(reduceMotion) {
        androidx.compose.runtime.derivedStateOf { if (reduceMotion) 0f else (blobAnim.value * 160f).toInt() / 160f }
    }

    // ── Particelle: canvas SEPARATO e leggero (solo cerchi) → può girare fluido
    // a ~40 update/s senza ridisegnare i blob. Periodo più corto = salgono in modo
    // chiaramente percepibile, senza scatti. ──
    val particleTransition = rememberInfiniteTransition(label = "aurora-particles")
    val particleAnim = particleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aurora-particle-phase",
    )
    val pPhase by remember(reduceMotion) {
        androidx.compose.runtime.derivedStateOf { if (reduceMotion) 0f else (particleAnim.value * 440f).toInt() / 440f }
    }

    val particles = remember(particleCount) {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                baseY = Random.nextFloat(),
                speed = 0.4f + Random.nextFloat() * 0.8f,
                radius = 0.9f + Random.nextFloat() * 2.0f,
                twinkle = Random.nextFloat(),
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(baseColor)) {
        // Layer 1 — blob aurora (lento, quantizzato).
        Canvas(modifier = Modifier.fillMaxSize()) {
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

            blob(glowDeep, w * (0.55f + 0.05f * sin(t * 0.6f)), h * (0.86f + 0.04f * cos(t)), w * 0.82f, 0.52f)
            blob(glowWarm, w * (0.24f + 0.08f * sin(t)), h * (0.18f + 0.05f * cos(t)), w * 0.66f, 0.38f)
            blob(glowCool, w * (0.82f + 0.06f * cos(t * 0.8f)), h * (0.30f + 0.06f * sin(t * 1.1f)), w * 0.58f, 0.30f)
        }

        // Layer 2 — particelle fluide (canvas economico, ~40 update/s).
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            particles.forEachIndexed { i, p ->
                val y = (((p.baseY - pPhase * p.speed) % 1f) + 1f) % 1f
                val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin((pPhase * p.speed * 6f + p.twinkle) * 2f * PI.toFloat()))
                val tint = when (i % 6) {
                    0 -> glowCool   // cyan
                    3 -> glowWarm   // arancio caldo
                    else -> Color.White
                }
                drawCircle(
                    color = tint.copy(alpha = 0.30f * tw),
                    radius = p.radius.dp.toPx(),
                    center = Offset(p.x * w, y * h),
                )
            }
        }
    }
}
