package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmSos
import it.trentosmartmountain.app.ui.theme.TsmSurface

/**
 * FAB in basso a destra sulla mappa: centra su utente e **SOS** (apre dialog in [RegistraScreen]).
 */
@Composable
fun RegistraMapActionFabs(
  canCenterOnUser: Boolean,
  onCenterOnUser: () -> Unit,
  onSosClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier.padding(end = 20.dp, bottom = RegistraLayout.fabBottomRest),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.End,
  ) {
    FloatingActionButton(
      onClick = { if (canCenterOnUser) onCenterOnUser() },
      modifier =
        Modifier
          .size(RegistraLayout.primaryFabSize)
          .alpha(if (canCenterOnUser) 1f else 0.45f),
      containerColor = TsmSurface,
      contentColor = TsmAccent,
    ) {
      Icon(
        imageVector = Icons.Filled.MyLocation,
        contentDescription = stringResource(R.string.registra_center_location_cd),
        modifier = Modifier.size(RegistraLayout.primaryFabIconSize),
      )
    }
    FloatingActionButton(
      onClick = onSosClick,
      modifier = Modifier.size(RegistraLayout.primaryFabSize),
      containerColor = TsmSos,
      contentColor = MaterialTheme.colorScheme.onError,
    ) {
      Icon(
        imageVector = Icons.Filled.Warning,
        contentDescription = stringResource(R.string.registra_sos_cd),
        modifier = Modifier.size(RegistraLayout.primaryFabIconSize),
      )
    }
  }
}
