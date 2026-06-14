package it.trentosmartmountain.app.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGlow
import it.trentosmartmountain.app.ui.components.TsmGradientButton
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmPrimary

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
  Box(modifier = Modifier.fillMaxSize().background(TsmBackground)) {
  TsmAuroraBackground(modifier = Modifier.fillMaxSize(), particleCount = 14)
  Scaffold(
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.email_verification_title), color = Color.White) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.register_back),
              tint = Color.White,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
      // Hero: icona mail con glow brand.
      Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)) {
        TsmGlow(color = TsmPrimary, modifier = Modifier.size(140.dp), alpha = 0.4f)
        Icon(Icons.Outlined.MarkEmailUnread, contentDescription = null, tint = TsmPrimary, modifier = Modifier.size(72.dp))
      }
      Text(
        text = stringResource(R.string.email_verification_body, email),
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
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
      TsmGradientButton(
        text = stringResource(R.string.email_verification_continue_login),
        onClick = onContinueToLogin,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
  }
}
