package it.trentosmartmountain.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.theme.TsmGradients

/**
 * Boot screen brandizzato (Fase 1 polish): si sovrappone al contenuto per ~1s
 * all'avvio dell'app e si raccorda **senza stacco** al window background
 * (`@drawable/splash_background`), eliminando la vecchia schermata bianca.
 *
 * Sequenza: aurora di sfondo → il logo appare con scale+fade (spring out) →
 * si "disegna" una linea-orizzonte montana sotto al logo → wordmark in fade →
 * l'intero overlay sfuma chiamando [onFinished].
 */
@Composable
fun TsmBootScreen(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.82f) }
    val logoAlpha = remember { Animatable(0f) }
    val ridgeProgress = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1) Reveal logo
        logoAlpha.animateTo(1f, tween(420))
        logoScale.animateTo(1f, tween(520))
        // 2) Disegna l'orizzonte montano
        ridgeProgress.animateTo(1f, tween(560))
        // 3) Wordmark
        wordAlpha.animateTo(1f, tween(360))
        // 4) Hold breve poi fade-out dell'overlay
        kotlinx.coroutines.delay(360)
        overlayAlpha.animateTo(0f, tween(420))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(overlayAlpha.value)
            .background(TsmGradients.mountainDusk),
        contentAlignment = Alignment.Center,
    ) {
        // Aurora morbida dietro (particellare leggero) per dare profondità.
        TsmAuroraBackground(modifier = Modifier.fillMaxSize(), particleCount = 18)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.tsm_logo_photo),
                contentDescription = "Trento Smart Mountain",
                modifier = Modifier
                    .size(132.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(18.dp))
            // Orizzonte montano che si "disegna" (path animato).
            Canvas(
                modifier = Modifier
                    .size(width = 180.dp, height = 26.dp),
            ) {
                val w = size.width
                val h = size.height
                val p = ridgeProgress.value
                // Profilo di crinale stilizzato (3 picchi) tracciato fino a frazione p.
                val pts = listOf(
                    Offset(0f, h * 0.9f),
                    Offset(w * 0.18f, h * 0.5f),
                    Offset(w * 0.33f, h * 0.72f),
                    Offset(w * 0.5f, h * 0.18f),
                    Offset(w * 0.67f, h * 0.62f),
                    Offset(w * 0.82f, h * 0.4f),
                    Offset(w, h * 0.85f),
                )
                val total = pts.size - 1
                for (i in 0 until total) {
                    val segStart = i.toFloat() / total
                    val segEnd = (i + 1).toFloat() / total
                    if (p <= segStart) break
                    val localT = ((p - segStart) / (segEnd - segStart)).coerceIn(0f, 1f)
                    val a = pts[i]
                    val b = pts[i + 1]
                    val end = Offset(a.x + (b.x - a.x) * localT, a.y + (b.y - a.y) * localT)
                    drawLine(
                        color = TsmColors.Cyan.copy(alpha = 0.9f),
                        start = a,
                        end = end,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.Text(
                text = "TRENTO SMART MOUNTAIN",
                color = TsmColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(wordAlpha.value),
            )
        }
    }
}
