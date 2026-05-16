package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Si solleva quando il tracking è attivo per non coprire i controlli.
 */
@Composable
fun RegistraMapActionFabs(
  isTrackingActive: Boolean,
  canCenterOnUser: Boolean,
  onCenterOnUser: () -> Unit,
  onSosClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bottomPadding by animateDpAsState(
    targetValue =
      if (isTrackingActive) RegistraLayout.fabBottomRaised else RegistraLayout.fabBottomRest,
    animationSpec = tween(durationMillis = 280),
    label = "map_fab_bottom_padding",
  )

  Column(
    modifier =
      modifier.padding(end = 20.dp, bottom = bottomPadding),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.End,
  ) {
    FloatingActionButton(
      onClick = { if (canCenterOnUser) onCenterOnUser() },
      modifier = Modifier.alpha(if (canCenterOnUser) 1f else 0.45f),
      containerColor = TsmSurface,
      contentColor = TsmAccent,
    ) {
      Icon(
        imageVector = Icons.Filled.MyLocation,
        contentDescription = stringResource(R.string.registra_center_location_cd),
      )
    }
    FloatingActionButton(
      onClick = onSosClick,
      containerColor = TsmSos,
      contentColor = MaterialTheme.colorScheme.onError,
    ) {
      Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.registra_sos_cd))
    }
  }
}
