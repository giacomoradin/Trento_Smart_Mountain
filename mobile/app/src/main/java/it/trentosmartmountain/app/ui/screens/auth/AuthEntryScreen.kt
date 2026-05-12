package it.trentosmartmountain.app.ui.screens.auth

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

/** Schermata iniziale: scelta tra accesso e registrazione. */
@Composable
fun AuthEntryScreen(
  /** Navigazione verso [Routes.LOGIN]. */
  onLoginClick: () -> Unit,
  /** Navigazione verso [Routes.REGISTER]. */
  onRegisterClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.auth_entry_title),
      style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.auth_entry_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(40.dp))

    Button(
      onClick = onLoginClick,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(R.string.auth_entry_login_button))
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
      onClick = onRegisterClick,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(R.string.auth_entry_register_button))
    }
  }
}
