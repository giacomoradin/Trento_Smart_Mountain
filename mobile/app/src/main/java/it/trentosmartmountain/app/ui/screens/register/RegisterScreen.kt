package it.trentosmartmountain.app.ui.screens.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import it.trentosmartmountain.app.viewmodel.RegisterViewModel

/** Schermata registrazione account (Material 3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
  /** Invocato dopo `POST /users` riuscito (di solito ritorno al login). */
  onRegistered: () -> Unit,
  /** Torna alla schermata di scelta auth. */
  onBack: () -> Unit,
  viewModel: RegisterViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val keyboard = LocalSoftwareKeyboardController.current
  var passwordVisible by remember { mutableStateOf(false) }
  var confirmPasswordVisible by remember { mutableStateOf(false) }

  // Un solo evento di navigazione verso il login dopo registrazione riuscita.
  LaunchedEffect(Unit) {
    viewModel.navigateLoginEventsFlow.collect {
      onRegistered()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.register_title)) },
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
          .padding(horizontal = 24.dp, vertical = 16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.Top,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.register_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(24.dp))

      OutlinedTextField(
        value = uiState.username,
        onValueChange = viewModel::onUsernameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.register_username_label)) },
        singleLine = true,
        enabled = !uiState.isLoading,
        isError = uiState.usernameError != null,
        supportingText =
          uiState.usernameError?.let { err ->
            { Text(err) }
          },
        keyboardOptions =
          KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
          ),
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = uiState.email,
        onValueChange = viewModel::onEmailChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.register_email_label)) },
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
        label = { Text(stringResource(R.string.register_password_label)) },
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
            imeAction = ImeAction.Next,
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
                  stringResource(R.string.register_hide_password)
                } else {
                  stringResource(R.string.register_show_password)
                },
            )
          }
        },
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = uiState.confirmPassword,
        onValueChange = viewModel::onConfirmPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.register_confirm_password_label)) },
        singleLine = true,
        enabled = !uiState.isLoading,
        isError = uiState.confirmPasswordError != null,
        supportingText =
          uiState.confirmPasswordError?.let { err ->
            { Text(err) }
          },
        visualTransformation =
          if (confirmPasswordVisible) {
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
              viewModel.onRegisterClick()
            },
          ),
        trailingIcon = {
          IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
            Icon(
              imageVector =
                if (confirmPasswordVisible) {
                  Icons.Filled.VisibilityOff
                } else {
                  Icons.Filled.Visibility
                },
              contentDescription =
                if (confirmPasswordVisible) {
                  stringResource(R.string.register_hide_password)
                } else {
                  stringResource(R.string.register_show_password)
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
          viewModel.onRegisterClick()
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
          Text(stringResource(R.string.register_button))
        }
      }
    }
  }
}
