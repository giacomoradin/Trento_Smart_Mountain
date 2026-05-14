package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R

/**
 * Schermata “Registra” (stile Strava/Komoot): mappa sessione attiva, gruppo in tempo reale, SOS.
 * La mappa OSMdroid sarà integrata in quest’area in un passo successivo.
 */
@Composable
fun RegistraScreen(modifier: Modifier = Modifier) {
  var showSosDialog by rememberSaveable { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(24.dp),
    ) {
      Text(
        text = stringResource(R.string.registra_title),
        style = MaterialTheme.typography.headlineSmall,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.registra_placeholder_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    FloatingActionButton(
      onClick = { showSosDialog = true },
      modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
      containerColor = MaterialTheme.colorScheme.error,
      contentColor = MaterialTheme.colorScheme.onError,
    ) {
      Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.registra_sos_cd))
    }
  }

  if (showSosDialog) {
    AlertDialog(
      onDismissRequest = { showSosDialog = false },
      title = { Text(stringResource(R.string.registra_sos_dialog_title)) },
      text = { Text(stringResource(R.string.registra_sos_dialog_body)) },
      confirmButton = {
        Button(
          onClick = { showSosDialog = false },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
          Text(stringResource(R.string.registra_sos_dialog_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showSosDialog = false }) {
          Text(stringResource(R.string.registra_sos_dialog_dismiss))
        }
      },
    )
  }
}
