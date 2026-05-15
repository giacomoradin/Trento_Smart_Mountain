package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.theme.TsmPrimary

@Composable
fun RegistraRecFab(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FloatingActionButton(
    onClick = onClick,
    modifier = modifier.size(72.dp),
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
