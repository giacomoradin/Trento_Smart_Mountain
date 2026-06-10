package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmSurface

/** Pulsante circolare in alto (bacheca rifugi, centra GPS) — stile condiviso. */
@Composable
fun RegistraTopCircleButton(
  onClick: () -> Unit,
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  iconTint: Color = TsmAccent,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = CircleShape,
    color = TsmSurface.copy(alpha = 0.92f),
    modifier = modifier.size(RegistraLayout.topActionButtonSize),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = iconTint,
      )
    }
  }
}
