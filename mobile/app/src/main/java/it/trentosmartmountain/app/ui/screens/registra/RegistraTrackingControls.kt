package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.location.TrackingStatus
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmSos
import it.trentosmartmountain.app.ui.theme.TsmSurface

/** Pulsanti pausa/ripresa e stop durante la registrazione attiva. */
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
    SmallFloatingActionButton(
      onClick = onTogglePause,
      containerColor = TsmSurface,
      contentColor = TsmAccent,
    ) {
      Icon(
        imageVector =
          if (trackingStatus == TrackingStatus.RECORDING) {
            Icons.Filled.Pause
          } else {
            Icons.Filled.PlayArrow
          },
        contentDescription =
          if (trackingStatus == TrackingStatus.RECORDING) {
            stringResource(R.string.registra_pause_cd)
          } else {
            stringResource(R.string.registra_resume_cd)
          },
      )
    }
    FloatingActionButton(
      onClick = onStop,
      modifier = Modifier.size(56.dp),
      containerColor = TsmSos,
      contentColor = MaterialTheme.colorScheme.onError,
    ) {
      Icon(
        imageVector = Icons.Filled.Stop,
        contentDescription = stringResource(R.string.registra_stop_cd),
      )
    }
  }
}
