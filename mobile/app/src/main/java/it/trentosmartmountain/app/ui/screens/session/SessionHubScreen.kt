package it.trentosmartmountain.app.ui.screens.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R

/** Sessione: pianificazione (GPX, codice/QR) e adesione a sessioni pianificate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHubScreen(modifier: Modifier = Modifier) {
  var subTab by rememberSaveable { mutableIntStateOf(0) }

  Column(modifier = modifier.fillMaxSize()) {
    PrimaryTabRow(selectedTabIndex = subTab) {
      Tab(
        selected = subTab == 0,
        onClick = { subTab = 0 },
        text = { Text(stringResource(R.string.session_tab_plan)) },
      )
      Tab(
        selected = subTab == 1,
        onClick = { subTab = 1 },
        text = { Text(stringResource(R.string.session_tab_join)) },
      )
    }
    when (subTab) {
      0 -> SessionPlanPlaceholder(Modifier.padding(24.dp))
      1 -> SessionJoinPlaceholder(Modifier.padding(24.dp))
    }
  }
}

@Composable
private fun SessionPlanPlaceholder(modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Text(
      text = stringResource(R.string.session_plan_title),
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.session_plan_placeholder),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SessionJoinPlaceholder(modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Text(
      text = stringResource(R.string.session_join_title),
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.session_join_placeholder),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
