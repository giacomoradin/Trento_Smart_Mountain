package it.trentosmartmountain.app.ui.screens.profile

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.viewmodel.ProfileViewModel

/** Profilo utente: in questa fase mostra solo lo username. */
@Composable
fun ProfileScreen(
  onLoggedOut: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ProfileViewModel =
    viewModel(
      factory =
        ViewModelProvider.AndroidViewModelFactory.getInstance(
          LocalContext.current.applicationContext as Application,
        ),
    ),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
    Text(
      text = stringResource(R.string.profile_title),
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(modifier = Modifier.height(16.dp))

    // In questa fase l’unico dato reale è lo username; il resto è placeholder.
    when {
      uiState.isLoading -> {
        CircularProgressIndicator()
      }
      uiState.username != null -> {
        Text(
          text = stringResource(R.string.profile_username_label),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = uiState.username.orEmpty(),
          style = MaterialTheme.typography.titleLarge,
        )
      }
      else -> {
        Text(
          text = uiState.errorMessage ?: stringResource(R.string.profile_load_error),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.profile_placeholder_body),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(
      onClick = {
        viewModel.logout()
        onLoggedOut()
      },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(R.string.profile_logout_button))
    }
  }
}
