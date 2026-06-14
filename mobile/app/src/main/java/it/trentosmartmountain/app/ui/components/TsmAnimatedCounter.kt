package it.trentosmartmountain.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import it.trentosmartmountain.app.ui.theme.TsmMotion

/**
 * Testo numerico con **count-up animato** (Fase polish 4/6): all'apparire (o al
 * cambio di [target]) il valore sale da 0 → target con easing premium. Dà un tocco
 * "dashboard sportiva" alle metriche (km, dislivello, punti, uscite).
 *
 * [format] converte il valore corrente (Float) nella stringa mostrata, così si
 * gestiscono interi ("%.0f"), decimali ("%.1f km"), suffissi ("pt"), ecc.
 */
@Composable
fun TsmAnimatedCounter(
    target: Float,
    format: (Float) -> String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = FontWeight.Bold,
    durationMillis: Int = 900,
) {
    val anim = remember { Animatable(0f) }
    // Evita di ri-animare a ogni ricomposizione: anima solo quando il target cambia.
    var lastTarget by remember { mutableStateOf(Float.NaN) }
    LaunchedEffect(target) {
        if (lastTarget != target) {
            lastTarget = target
            anim.snapTo(0f)
            anim.animateTo(target, tween(durationMillis, easing = TsmMotion.EaseOutCubic))
        }
    }
    Text(
        text = format(anim.value),
        modifier = modifier,
        color = color,
        // Cifre monospace "telemetria" (TsmType.Numeric): larghezza fissa, il
        // numero non balla durante il count-up — identità dati di tutta l'app.
        style = style.merge(it.trentosmartmountain.app.ui.theme.TsmType.Numeric),
        fontWeight = fontWeight,
        maxLines = 1,
    )
}
