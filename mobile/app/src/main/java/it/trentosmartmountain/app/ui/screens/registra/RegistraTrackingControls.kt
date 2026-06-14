package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.location.TrackingStatus
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmMotion
import it.trentosmartmountain.app.ui.theme.TsmSos
import it.trentosmartmountain.app.ui.theme.TsmSurface

/**
 * Pulsanti pausa/ripresa e stop durante la registrazione.
 *
 * Pass di coerenza (B9): tutti i FAB ora usano lo stesso "stile glass":
 *  - bordo cerchiato sottile per definire la silhouette,
 *  - gradiente interno radiale per dare profondità,
 *  - ombra coerente, stessa elevazione su play/pausa e stop.
 *
 * Lo stop resta leggermente più piccolo (RegistraLayout.secondaryFabSize) per
 * gerarchia visiva — la pausa è l'azione "calma", lo stop quella "definitiva".
 */
@Composable
fun RegistraTrackingControls(
  trackingStatus: TrackingStatus,
  onTogglePause: () -> Unit,
  onStop: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (trackingStatus == TrackingStatus.IDLE) return

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val isRec = trackingStatus == TrackingStatus.RECORDING
    GlassFab(
      onClick = onTogglePause,
      icon = if (isRec) Icons.Filled.Pause else Icons.Filled.PlayArrow,
      contentDescription = stringResource(
        if (isRec) R.string.registra_pause_cd else R.string.registra_resume_cd,
      ),
      size = RegistraLayout.primaryFabSize,
      iconSize = RegistraLayout.primaryFabIconSize,
      containerColor = TsmSurface,
      iconTint = TsmAccent,
    )
    GlassFab(
      onClick = onStop,
      icon = Icons.Filled.Stop,
      contentDescription = stringResource(R.string.registra_stop_cd),
      size = RegistraLayout.secondaryFabSize,
      iconSize = RegistraLayout.secondaryFabIconSize,
      containerColor = TsmSos,
      iconTint = Color.White,
      borderTint = TsmSos.copy(alpha = 0.65f),
    )
  }
}

/**
 * FAB "glass" condiviso usato in Registra per coerenza visiva tra
 * play/pausa, stop, partecipanti, SOS. Bordo sottile + gradiente sottile.
 *
 * Micro-interazione al tocco (richiesta utente): il pulsante si **comprime**
 * (0.92) e il bordo diventa più definito. NB: effetto CONTENUTO — la scala
 * resta ≤ 1 e non c'è alcun glow/alone esterno, così la silhouette non si
 * allarga mai (vincolo esplicito: "i particellari aumentano il bordo").
 */
@Composable
internal fun GlassFab(
  onClick: () -> Unit,
  icon: ImageVector,
  contentDescription: String,
  size: Dp,
  iconSize: Dp,
  containerColor: Color,
  iconTint: Color,
  borderTint: Color = Color.White.copy(alpha = 0.10f),
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.92f else 1f,
    animationSpec = TsmMotion.springSmooth(),
    label = "glassfab-press-scale",
  )
  // Bordo più opaco al tocco (stesso colore, più presenza): nessun aumento di
  // dimensione, solo definizione. Su press alziamo l'alpha senza cambiare hue.
  val animBorder by animateColorAsState(
    targetValue = if (pressed) borderTint.copy(alpha = (borderTint.alpha + 0.30f).coerceAtMost(1f)) else borderTint,
    animationSpec = TsmMotion.tweenFast(),
    label = "glassfab-press-border",
  )
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = containerColor,
    shadowElevation = 6.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, animBorder),
    interactionSource = interaction,
    modifier = Modifier.size(size).scale(pressScale),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color.White.copy(alpha = 0.10f),
              Color.Transparent,
            ),
            radius = size.value * 2.0f,
          ),
          CircleShape,
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = iconTint,
        modifier = Modifier.size(iconSize),
      )
    }
  }
}
