package it.trentosmartmountain.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
    border: Color = Color.White.copy(alpha = 0.06f),
    topColor: Color = TsmColors.CardElevated,
    bottomColor: Color = TsmColors.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .border(1.dp, border, shape),
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
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
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
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = TsmMotion.EaseInOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )
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
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = TsmMotion.EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
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
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (enabled) fill else Brush.horizontalGradient(listOf(TsmColors.CardElevated, TsmColors.Card)))
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.18f else 0.06f), shape)
            .clickable(enabled = enabled, onClick = onClick)
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
