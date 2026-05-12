package it.trentosmartmountain.app.ui.screens.session

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

/** Scheletro sessione escursione: avvio o riepilogo sessione attiva (contenuti futuri). */
@Composable
fun SessionScreen(modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
    Text(
      text = stringResource(R.string.session_title),
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.session_placeholder_body),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
