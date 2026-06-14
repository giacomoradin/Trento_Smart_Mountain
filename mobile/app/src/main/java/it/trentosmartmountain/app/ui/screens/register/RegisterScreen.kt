package it.trentosmartmountain.app.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGradientButton
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.viewmodel.RegisterViewModel

/**
 * Registrazione account escursionista (username, email, password).
 *
 * @param onRegistrationPendingVerification navigazione verifica email con messaggio opzionale dal server
 * @param onBack torna alla schermata precedente nello stack
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistrationPendingVerification: (email: String, serverMessage: String?) -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var termsError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateVerificationEventsFlow.collect { result ->
            onRegistrationPendingVerification(result.email, result.serverMessage)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(TsmBackground)) {
    TsmAuroraBackground(modifier = Modifier.fillMaxSize(), particleCount = 16)
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.register_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.register_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(modifier = Modifier.weight(1f).height(3.dp).background(TsmPrimary, RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.weight(1f).height(3.dp).background(Color(0xFF3A3A3A), RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.weight(1f).height(3.dp).background(Color(0xFF3A3A3A), RoundedCornerShape(2.dp)))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.register_step_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))

            FieldLabel(stringResource(R.string.register_full_name_label))
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Mario Rossi", color = Color.Gray) },
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.usernameError != null,
                supportingText = uiState.usernameError?.let { err -> { Text(err) } },
                colors = tsmFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            FieldLabel("EMAIL")
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("mario.rossi@email.com", color = Color.Gray) },
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { err -> { Text(err) } },
                colors = tsmFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            FieldLabel("PASSWORD")
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Minimo 8 caratteri", color = Color.Gray) },
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { err -> { Text(err) } },
                // KeyboardType.Password: niente auto-correct/suggerimenti — la tastiera
                // poteva alterare la password digitata (campo mascherato, invisibile).
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                    autoCorrectEnabled = false,
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = Color.Gray,
                        )
                    }
                },
                colors = tsmFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            FieldLabel("CONFERMA PASSWORD")
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ripeti password", color = Color.Gray) },
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.confirmPasswordError != null,
                supportingText = uiState.confirmPasswordError?.let { err -> { Text(err) } },
                // KeyboardType.Password: niente auto-correct/suggerimenti — la tastiera
                // poteva alterare la password digitata (campo mascherato, invisibile).
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                    autoCorrectEnabled = false,
                ),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = Color.Gray,
                        )
                    }
                },
                colors = tsmFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (termsError) MaterialTheme.colorScheme.error else Color(0xFF3A3A3A),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = {
                        termsAccepted = it
                        termsError = false
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = TsmAccent,
                        uncheckedColor = TsmAccent,
                    ),
                )
                Text(
                    text = stringResource(R.string.register_terms),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, end = 4.dp),
                )
            }
            if (termsError) {
                Text(
                    text = stringResource(R.string.register_terms_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }

            uiState.generalError?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TsmGradientButton(
                text = if (uiState.isLoading) "ATTENDI…" else "AVANTI",
                onClick = {
                    if (!termsAccepted) {
                        termsError = true
                    } else {
                        keyboard?.hide()
                        viewModel.onRegisterClick()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun tsmFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TsmAccent,
    unfocusedBorderColor = Color(0xFF3A3A3A),
    focusedContainerColor = TsmSurfaceVariant,
    unfocusedContainerColor = TsmSurfaceVariant,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = TsmAccent,
)
