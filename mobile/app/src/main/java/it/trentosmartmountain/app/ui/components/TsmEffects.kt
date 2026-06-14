package it.trentosmartmountain.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import it.trentosmartmountain.app.ui.theme.TsmMotion
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.ui.theme.TsmColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * True se l'utente ha attivato **"Rimuovi animazioni"** nelle impostazioni di
 * sistema (Animator duration scale = 0). Gli effetti decorativi (aurora, sweep
 * border, burst, glow pulsanti) lo rispettano → accessibilità + segnale di
 * cura. Letto una volta in composizione: un cambio di setting richiede comunque
 * un riavvio della schermata, accettabile per effetti puramente estetici.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/* ───────────────────────────── Reward burst (confetti/spark) ───────────────────────────── */

private data class BurstParticle(
    val angle: Float,   // direzione di lancio (rad)
    val speed: Float,   // 0..1 frazione del raggio
    val color: Color,
    val size: Float,    // dp
    val spin: Float,    // giri totali della scheggia
    val elongated: Boolean,
)

/**
 * **Esplosione di particelle celebrativa** (effetto "wow" premium). Da sovrapporre
 * a una schermata nei momenti-premio (quiz superato, checkpoint NFC, traguardo):
 * coriandoli/scintille nei colori brand partono dal centro-alto, si aprono a
 * ventaglio, ruotano e cadono con gravità, sfumando. **One-shot**: si gioca quando
 * [play] passa a true e, a fine animazione, invoca [onFinished] (per riazzerare lo stato).
 *
 * Performante: una sola [Animatable] guida ~50 particelle per ~1,3 s; nessun loop
 * infinito (vive solo durante la celebrazione).
 */
@Composable
fun TsmRewardBurst(
    play: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 56,
    colors: List<Color> = listOf(TsmColors.Primary, TsmColors.Cyan, Color(0xFFFFD700), Color.White),
    onFinished: () -> Unit = {},
) {
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(0f) }
    val particles = remember(particleCount) {
        List(particleCount) {
            // Ventaglio prevalentemente verso l'alto (-90°) ma a 360° per "scoppio".
            val a = (Random.nextFloat() * 2f * PI.toFloat())
            BurstParticle(
                angle = a,
                speed = 0.35f + Random.nextFloat() * 0.65f,
                color = colors[Random.nextInt(colors.size)],
                size = 5f + Random.nextFloat() * 7f,
                spin = (Random.nextFloat() - 0.5f) * 4f,
                elongated = Random.nextBoolean(),
            )
        }
    }

    LaunchedEffect(play) {
        if (play) {
            if (!reduceMotion) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(durationMillis = 1300, easing = LinearEasing))
                progress.snapTo(0f)
            }
            // Con "riduci animazioni": nessun coriandolo, ma l'evento si conclude
            // comunque (l'esito resta comunicato da testo/glow statici).
            onFinished()
        }
    }

    val p = progress.value
    if (p <= 0f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.40f
        val maxR = size.minDimension * 0.62f
        // Ease-out sull'espansione + gravità quadratica sulla caduta.
        val eased = 1f - (1f - p) * (1f - p)
        val gravity = size.height * 0.55f
        val alpha = (1f - p).coerceIn(0f, 1f)

        particles.forEach { part ->
            val dist = part.speed * maxR * eased
            val x = cx + cos(part.angle) * dist
            val y = cy + sin(part.angle) * dist + gravity * p * p
            val s = part.size.dp.toPx()
            rotate(degrees = part.spin * p * 360f, pivot = Offset(x, y)) {
                if (part.elongated) {
                    drawRect(
                        color = part.color.copy(alpha = alpha),
                        topLeft = Offset(x - s * 0.35f, y - s * 0.9f),
                        size = Size(s * 0.7f, s * 1.8f),
                    )
                } else {
                    drawRect(
                        color = part.color.copy(alpha = alpha),
                        topLeft = Offset(x - s / 2f, y - s / 2f),
                        size = Size(s, s),
                    )
                }
            }
        }
    }
}

/* ───────────────────────────── Reveal d'ingresso liste ───────────────────────────── */

/**
 * Micro-animazione d'ingresso per gli elementi di lista: l'item **sfuma salendo**
 * (alpha 0→1 + traslazione verso l'alto) la prima volta che entra in composizione
 * — dà vita all'apertura del feed/liste senza appesantire (una sola [Animatable]
 * per item). Rispetta "riduci animazioni" (in quel caso l'item appare statico).
 */
@Composable
fun Modifier.tsmEnterReveal(): Modifier {
    if (rememberReduceMotion()) return this
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(durationMillis = 360, easing = TsmMotion.EaseOutCubic))
    }
    return this.graphicsLayer {
        alpha = anim.value
        translationY = (1f - anim.value) * 22.dp.toPx()
    }
}

/* ───────────────────────────── Bordo "luce viaggiante" (hero) ───────────────────────────── */

/**
 * Bordo premium con una **banda di luce che scorre** lungo il perimetro (gradiente
 * diagonale che trasla). Da riservare agli elementi-hero (es. card crediti, premi)
 * per dare l'effetto "vetro vivo / extreme premium" senza appesantire: la fase è
 * quantizzata (~ aggiornamenti ridotti) e disegna solo uno stroke.
 */
fun Modifier.tsmSweepBorder(
    cornerRadius: Dp,
    width: Dp = 2.5.dp,
    colors: List<Color> = listOf(
        Color.Transparent,
        TsmColors.Cyan,
        Color.White,
        Color.White,
        TsmColors.Primary,
        Color.Transparent,
    ),
    durationMillis: Int = 3200,
): Modifier = composed {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "sweep-border")
    val raw by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep-border-phase",
    )
    // PERF: ~130 step sul ciclo (≈40/s) — fluido (uno stroke è economico), niente scatti.
    // Con "riduci animazioni" il bordo resta fermo a metà corsa (luce statica).
    val t by remember { derivedStateOf { if (reduceMotion) 0.5f else (raw * 130f).toInt() / 130f } }

    drawWithContent {
        drawContent()
        val strokePx = width.toPx()
        val cr = CornerRadius(cornerRadius.toPx())
        // La banda diagonale parte fuori a sinistra e scorre oltre il bordo destro.
        val span = size.width * 2f
        val startX = -size.width + span * t
        val brush = Brush.linearGradient(
            colors = colors,
            start = Offset(startX, 0f),
            end = Offset(startX + size.width, size.height),
        )
        drawRoundRect(
            brush = brush,
            topLeft = Offset(strokePx / 2f, strokePx / 2f),
            size = Size(size.width - strokePx, size.height - strokePx),
            cornerRadius = cr,
            style = Stroke(width = strokePx),
        )
    }
}
