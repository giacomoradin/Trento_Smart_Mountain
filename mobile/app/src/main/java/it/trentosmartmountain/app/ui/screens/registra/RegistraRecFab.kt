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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.components.TsmGlow
import it.trentosmartmountain.app.ui.theme.TsmPrimary

/**
 * FAB centrale per avviare la registrazione GPS (stato IDLE), con **glow
 * arancione pulsante** dietro per un tocco "athletic / data-focused".
 */
@Composable
fun RegistraRecFab(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "rec-glow")
  val glowAlpha by transition.animateFloat(
    initialValue = 0.40f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "rec-glow-alpha",
  )

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    TsmGlow(
      color = TsmPrimary,
      modifier = Modifier.size(124.dp),
      alpha = glowAlpha,
    )
    FloatingActionButton(
      onClick = onClick,
      modifier = Modifier.size(60.dp),
      shape = CircleShape,
      containerColor = TsmPrimary,
      contentColor = Color.White,
    ) {
      Icon(
        imageVector = Icons.Filled.FiberManualRecord,
        contentDescription = stringResource(R.string.registra_start_tracking_cd),
        modifier = Modifier.size(32.dp),
      )
    }
  }
}
