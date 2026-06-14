package it.trentosmartmountain.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.theme.TsmMotion

/**
 * Materiale "glass" premium riusabile (Fase grafica): card con gradiente verticale
 * sottile (CardElevated → Card), bordo hairline e angoli arrotondati. È la base
 * coerente per i moduli data-focused dell'app (feed, profilo, telemetria).
 */
@Composable
fun TsmGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    border: Color = Color.White.copy(alpha = 0.10f),
    topColor: Color = TsmColors.CardElevated,
    bottomColor: Color = TsmColors.Card,
    /** Elevazione a riposo: dà profondità reale (ombra) alle card su tutto l'app. */
    elevation: Dp = 10.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    // Micro-interazione di pressione (pass "oltre"): quando la card è cliccabile
    // si comprime appena (0.97), il bordo si accende e si "appoggia" (ombra ridotta).
    // Definita QUI una volta → ogni card interattiva dell'app risponde uguale.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val isPressed = pressed && onClick != null
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = TsmMotion.springSmooth(),
        label = "glass-press-scale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Color.White.copy(alpha = 0.32f) else border,
        animationSpec = TsmMotion.tweenFast(),
        label = "glass-press-border",
    )
    val elev by animateDpAsState(
        targetValue = if (isPressed) 2.dp else elevation,
        animationSpec = TsmMotion.tweenFast(),
        label = "glass-press-elev",
    )
    Column(
        modifier = modifier
            .scale(pressScale)
            // Ombra reale (clip=false) PRIMA del clip: stacca la card dallo sfondo.
            .shadow(elev, shape, clip = false)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            // Sheen: una luce che "cade" sul bordo superiore del vetro, come un
            // riflesso — dà spessore al materiale. Su tema scuro è il segnale di
            // profondità che si legge meglio (le ombre nere si vedono poco).
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.13f),
                    0.45f to Color.Transparent,
                ),
            )
            .border(1.dp, borderColor, shape),
        content = content,
    )
}

/**
 * Bagliore radiale morbido (glow) da posizionare DIETRO un elemento per dargli
 * profondità "athletic" (es. avatar, CTA, vetta). Niente blur hardware (compat
 * sotto API 31): è un gradiente radiale che sfuma a trasparente.
 */
@Composable
fun TsmGlow(
    color: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 0.5f,
) {
    Box(
        modifier = modifier
            .background(
                // Mid-stop: il glow resta intenso più a lungo prima di sfumare →
                // alone più "pieno" e presente (intensificazione sistema).
                Brush.radialGradient(
                    0f to color.copy(alpha = alpha),
                    0.5f to color.copy(alpha = alpha * 0.5f),
                    1f to Color.Transparent,
                ),
            ),
    )
}

/** Sottile linea-accento orizzontale a gradiente (separatore "telemetria"). */
@Composable
fun TsmAccentRule(
    modifier: Modifier = Modifier,
    color: Color = TsmColors.Primary,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, color.copy(alpha = 0.9f), Color.Transparent),
                ),
            ),
    )
}

/**
 * **Shimmer sweep**: una banda di luce diagonale che attraversa periodicamente
 * il contenuto (effetto "premio brillante"). Da applicare a badge/CTA dorati o a
 * elementi che meritano enfasi. Disegnato SOPRA il contenuto via [drawWithContent],
 * non altera il layout. Loop infinito morbido con pausa tra i passaggi.
 */
@Composable
fun Modifier.tsmShimmer(
    highlight: Color = Color.White,
    durationMillis: Int = 2600,
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer-sweep")
    val progressRaw = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = TsmMotion.EaseInOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )
    // PERF: ~30 update/s (78 step sul ciclo) invece del pieno frame-rate.
    val progress by remember {
        androidx.compose.runtime.derivedStateOf { (progressRaw.value * 78f).toInt() / 78f }
    }
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return this // niente sweep con "riduci animazioni"
    return this.drawWithContent {
        drawContent()
        val bandW = size.width * 0.4f
        // La banda parte fuori a sinistra e scorre oltre il bordo destro; sosta
        // implicita perché solo l'ultimo ~60% del ciclo è "in vista".
        val startX = -bandW + (size.width + bandW) * progress
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, highlight.copy(alpha = 0.45f), Color.Transparent),
                startX = startX,
                endX = startX + bandW,
            ),
        )
    }
}

/**
 * Glow radiale **pulsante** (anima alpha + scala) da mettere DIETRO una CTA o un
 * elemento focale per attirare l'attenzione (es. "Avvia", REC, "Pubblica").
 * Loop infinito morbido; nessun blur HW.
 */
@Composable
fun TsmPulseGlow(
    color: Color,
    modifier: Modifier = Modifier,
    minAlpha: Float = 0.18f,
    maxAlpha: Float = 0.45f,
) {
    val transition = rememberInfiniteTransition(label = "pulse-glow")
    val tRaw = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = TsmMotion.EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    // PERF: 32 livelli (~18 update/s sul ciclo da 1,8 s) — un glow morbido non
    // ha bisogno del pieno frame-rate. Con "riduci animazioni" resta fisso a metà.
    val reduceMotion = rememberReduceMotion()
    val t by remember(reduceMotion) {
        androidx.compose.runtime.derivedStateOf { if (reduceMotion) 0.5f else (tRaw.value * 32f).toInt() / 32f }
    }
    val alpha = minAlpha + (maxAlpha - minAlpha) * t
    Box(
        modifier = modifier
            .scale(0.9f + 0.15f * t)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), Color.Transparent),
                ),
            ),
    )
}

/**
 * Chip "glass" riusabile: riempimento gradiente sottile + bordo hairline. Quando
 * [selected] usa l'accent pieno. Per filtri, tag, stati.
 */
@Composable
fun TsmGlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = TsmColors.Cyan,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    val bg = if (selected) {
        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.14f)))
    } else {
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
        }
        androidx.compose.material3.Text(
            text = text,
            color = if (selected) accent else TsmColors.TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
        )
    }
}

/**
 * CTA "glass" premium: riempimento [fill] (gradiente), bordo luminoso 1px, glow
 * morbido sotto, scala al press. Niente Material Button (controllo totale su materiali).
 */
@Composable
fun TsmGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Brush = Brush.horizontalGradient(listOf(TsmColors.Primary, TsmColors.PrimaryDark)),
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    cornerRadius: Dp = 16.dp,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    // Press-state: la CTA si comprime al tocco e si "appoggia" (ombra ridotta),
    // stesso feel delle glass card.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val isPressed = pressed && enabled
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = TsmMotion.springSmooth(),
        label = "cta-press-scale",
    )
    val elev by animateDpAsState(
        targetValue = if (!enabled) 0.dp else if (isPressed) 2.dp else 10.dp,
        animationSpec = TsmMotion.tweenFast(),
        label = "cta-press-elev",
    )
    Row(
        modifier = modifier
            .scale(pressScale)
            // Ombra reale sotto la CTA: la fa "galleggiare" sopra la pagina.
            .shadow(elev, shape, clip = false)
            .height(height)
            .clip(shape)
            .background(if (enabled) fill else Brush.horizontalGradient(listOf(TsmColors.CardElevated, TsmColors.Card)))
            // Gloss: riflesso luminoso sulla metà superiore della CTA.
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (enabled) 0.18f else 0f),
                    0.5f to Color.Transparent,
                ),
            )
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.28f else 0.06f), shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        leading?.invoke(this)
        androidx.compose.material3.Text(
            text = text,
            color = if (enabled) contentColor else TsmColors.TextTertiary,
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
    }
}
