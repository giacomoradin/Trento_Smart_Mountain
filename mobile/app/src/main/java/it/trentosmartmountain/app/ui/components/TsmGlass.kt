package it.trentosmartmountain.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.ui.theme.TsmColors

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
