package it.trentosmartmountain.app.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.viewmodel.LoginViewModel

/**
 * Schermata di accesso con email e password (Material 3).
 *
 * - Legge lo stato da [LoginViewModel] e riflette errori di validazione sui campi.
 * - [LaunchedEffect] resta in ascolto del flusso di navigazione: un solo evento dopo login riuscito evita navigazioni duplicate.
 */
@Composable
fun LoginScreen(
  onLoggedIn: () -> Unit,
  viewModel: LoginViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val keyboard = LocalSoftwareKeyboardController.current
  var passwordVisible by remember { mutableStateOf(false) }

  // Collect infinito è voluto: il Channel emette al massimo un evento logico per login; il recomposer non reinizializza grazie a Unit key.
  LaunchedEffect(Unit) {
    viewModel.navigateHomeEventsFlow.collect {
      onLoggedIn()
    }
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.login_title),
      style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.login_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
      value = uiState.email,
      onValueChange = viewModel::onEmailChange,
      modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(R.string.login_email_label)) },
      singleLine = true,
      enabled = !uiState.isLoading,
      isError = uiState.emailError != null,
      supportingText =
        uiState.emailError?.let { err ->
          { Text(err) }
        },
      keyboardOptions =
        KeyboardOptions(
          keyboardType = KeyboardType.Email,
          imeAction = ImeAction.Next,
        ),
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
      value = uiState.password,
      onValueChange = viewModel::onPasswordChange,
      modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(R.string.login_password_label)) },
      singleLine = true,
      enabled = !uiState.isLoading,
      isError = uiState.passwordError != null,
      supportingText =
        uiState.passwordError?.let { err ->
          { Text(err) }
        },
      visualTransformation =
        if (passwordVisible) {
          VisualTransformation.None
        } else {
          PasswordVisualTransformation()
        },
      keyboardOptions =
        KeyboardOptions(
          keyboardType = KeyboardType.Password,
          imeAction = ImeAction.Done,
        ),
      keyboardActions =
        KeyboardActions(
          onDone = {
            keyboard?.hide()
            viewModel.onLoginClick()
          },
        ),
      trailingIcon = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
          Icon(
            imageVector =
              if (passwordVisible) {
                Icons.Filled.VisibilityOff
              } else {
                Icons.Filled.Visibility
              },
            contentDescription =
              if (passwordVisible) {
                stringResource(R.string.login_hide_password)
              } else {
                stringResource(R.string.login_show_password)
              },
          )
        }
      },
    )

    uiState.generalError?.let { msg ->
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = msg,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
      )
    }

    Spacer(modifier = Modifier.height(28.dp))

    Button(
      onClick = {
        keyboard?.hide()
        viewModel.onLoginClick()
      },
      modifier = Modifier.fillMaxWidth(),
      enabled = !uiState.isLoading,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        if (uiState.isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp).padding(end = 10.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        }
        Text(stringResource(R.string.login_button))
      }
    }
  }
}
