package it.trentosmartmountain.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bottone "Condividi come storia" — stile coerente con la creazione di una
 * storia: icona **+** dentro un **cerchio tratteggiato** e sfondo a **gradiente
 * verso il verde** (richiesta utente). Riusato da SessionDetail e ActivityDetail.
 */
@Composable
fun TsmShareStoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "CONDIVIDI COME STORIA",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    // Gradiente "athletic green": dal verde abete a un verde acceso.
                    listOf(Color(0xFF1B9E5A), Color(0xFF3FD27E)),
                ),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DashedAddCircle(size = 22.dp, color = Color.White)
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Cerchio a tratteggio con un "+" centrale (stile "aggiungi storia"). */
@Composable
private fun DashedAddCircle(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val ringStroke = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), 0f),
        )
        drawCircle(
            color = color,
            radius = this.size.minDimension / 2f - 1.dp.toPx(),
            style = ringStroke,
        )
        val c = center
        val arm = this.size.minDimension * 0.22f
        val pw = 2.2.dp.toPx()
        drawLine(color, Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), strokeWidth = pw, cap = StrokeCap.Round)
        drawLine(color, Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), strokeWidth = pw, cap = StrokeCap.Round)
    }
}
