package it.trentosmartmountain.app.ui.screens.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R

/** Scheletro mappa di gruppo in tempo reale (contenuti futuri). */
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
    Text(
      text = stringResource(R.string.map_title),
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.map_placeholder_body),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
