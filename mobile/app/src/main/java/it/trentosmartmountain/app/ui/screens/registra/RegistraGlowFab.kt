package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import it.trentosmartmountain.app.ui.components.TsmGlow
import it.trentosmartmountain.app.ui.theme.TsmPrimary

/**
 * FAB con bagliore pulsante (stile pulsante REC) per azioni primarie in basso.
 */
@Composable
fun RegistraGlowFab(
  onClick: () -> Unit,
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  fabSize: Dp = RegistraLayout.primaryFabSize,
  iconSize: Dp = RegistraLayout.primaryFabIconSize,
  containerColor: Color = TsmPrimary,
  contentColor: Color = Color.White,
  glowColor: Color = TsmPrimary,
  glowSize: Dp = RegistraLayout.primaryGlowSize,
  showGlow: Boolean = true,
) {
  val transition = rememberInfiniteTransition(label = "glow-fab")
  val glowAlphaRaw = transition.animateFloat(
    initialValue = 0.40f,
    targetValue = 0.85f,
    animationSpec =
      infiniteRepeatable(
        animation = tween(durationMillis = 1100, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "glow-fab-alpha",
  )
  // PERF: alpha quantizzata a 24 livelli — il glow (gradiente radiale sopra la
  // MapView del tracking!) ridisegna ~20 volte/s invece che a ogni frame.
  val glowAlpha by androidx.compose.runtime.remember {
    androidx.compose.runtime.derivedStateOf { (glowAlphaRaw.value * 24f).toInt() / 24f }
  }

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    if (showGlow) {
      TsmGlow(
        color = glowColor,
        modifier = Modifier.size(glowSize),
        alpha = glowAlpha,
      )
    }
    FloatingActionButton(
      onClick = onClick,
      modifier = Modifier.size(fabSize),
      shape = CircleShape,
      containerColor = containerColor,
      contentColor = contentColor,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(iconSize),
      )
    }
  }
}

