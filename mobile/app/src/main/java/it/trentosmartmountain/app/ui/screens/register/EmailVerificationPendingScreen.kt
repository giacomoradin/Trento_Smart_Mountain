package it.trentosmartmountain.app.ui.screens.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R

/**
 * Schermata post-registrazione: istruzioni per completare la verifica email prima del login.
 *
 * @param email indirizzo registrato (mostrato all'utente)
 * @param serverMessage messaggio opzionale restituito da `POST /users`
 * @param onContinueToLogin apre il login con email precompilata
 * @param onBack torna indietro nello stack di navigazione
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationPendingScreen(
  email: String,
  serverMessage: String?,
  onContinueToLogin: () -> Unit,
  onBack: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.email_verification_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.register_back),
            )
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.Top,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.email_verification_body, email),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Start,
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = stringResource(R.string.email_verification_steps),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      serverMessage?.let { message ->
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(28.dp))
      Button(
        onClick = onContinueToLogin,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.email_verification_continue_login))
      }
    }
  }
}
