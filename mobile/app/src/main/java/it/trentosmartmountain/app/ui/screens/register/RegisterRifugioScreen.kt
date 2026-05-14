package it.trentosmartmountain.app.ui.screens.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R

/** Registrazione account rifugio: struttura UI in attesa di API dedicate. */
@Composable
fun RegisterRifugioScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.register_rifugio_title),
      style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.register_rifugio_body),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(32.dp))
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.register_back))
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(
      onClick = {},
      enabled = false,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(R.string.register_rifugio_submit_placeholder))
    }
  }
}
