package it.trentosmartmountain.app.ui.screens.refuge

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.viewmodel.ProfileViewModel

/** Shell principale account con ruolo `rifugio`: metriche IoT e social credit ospiti. */
@Composable
fun RefugeMainScreen(
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
  Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
    Text(
      text = stringResource(R.string.refuge_main_title),
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.refuge_iot_section_title),
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.refuge_iot_placeholder),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
      text = stringResource(R.string.refuge_credits_section_title),
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.refuge_credits_placeholder),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.weight(1f))
    OutlinedButton(
      onClick = { viewModel.logout(onLoggedOut) },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(R.string.profile_logout_button))
    }
  }
}
