package it.trentosmartmountain.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
  val googleNotAvailableMessage = stringResource(R.string.auth_oauth_google_not_available)
  var googleOAuthMessage by rememberSaveable { mutableStateOf<String?>(null) }

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

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
      onClick = {
        googleOAuthMessage = googleNotAvailableMessage
      },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_google),
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(stringResource(R.string.auth_OAuth_Google_demo_button))
      }
    }

    googleOAuthMessage?.let { message ->
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = message,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}
