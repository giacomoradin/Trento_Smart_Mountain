package it.trentosmartmountain.app.ui.screens.register

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import it.trentosmartmountain.app.ui.theme.TsmBorder
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.viewmodel.RegisterRifugioViewModel

/**
 * Registrazione account rifugio (dati struttura + credenziali).
 *
 * @param onBack torna alla schermata di ingresso auth
 * @param onRegistrationPendingVerification come per [RegisterScreen], verso verifica email
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterRifugioScreen(
    onBack: () -> Unit,
    onRegistrationPendingVerification: (email: String, serverMessage: String?) -> Unit = { _, _ -> },
    viewModel: RegisterRifugioViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    var passwordVisible by remember { mutableStateOf(false) }

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
                        stringResource(R.string.register_rifugio_title),
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

            // Info box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = TsmSurfaceVariant,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Home, contentDescription = null, tint = TsmAccent, modifier = Modifier.size(24.dp))
                    Text(
                        text = stringResource(R.string.register_rifugio_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            RifugioFieldLabel(stringResource(R.string.register_rifugio_name_label))
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.rifugioName,
                onValueChange = viewModel::onRifugioNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.register_rifugio_name_placeholder), color = Color.Gray) },
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.rifugioNameError != null,
                supportingText = uiState.rifugioNameError?.let { err -> { Text(err) } },
                colors = rifugioFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            RifugioFieldLabel(stringResource(R.string.register_rifugio_cai_label))
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.caiCode,
                onValueChange = viewModel::onCaiCodeChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.register_rifugio_cai_placeholder), color = Color.Gray) },
                singleLine = true,
                enabled = !uiState.isLoading,
                colors = rifugioFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    RifugioFieldLabel(stringResource(R.string.register_rifugio_quota_label))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.quota,
                        onValueChange = viewModel::onQuotaChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("2243", color = Color.Gray) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        colors = rifugioFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    RifugioFieldLabel(stringResource(R.string.register_rifugio_posti_label))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.posti,
                        onValueChange = viewModel::onPostiChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("80", color = Color.Gray) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        colors = rifugioFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            RifugioFieldLabel(stringResource(R.string.register_rifugio_coords_label))
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.coordinates,
                onValueChange = viewModel::onCoordinatesChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.register_rifugio_coords_placeholder), color = Color.Gray) },
                singleLine = true,
                enabled = !uiState.isLoading,
                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TsmAccent) },
                colors = rifugioFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            RifugioFieldLabel(stringResource(R.string.register_rifugio_email_label))
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("gestore@rifugio.it", color = Color.Gray) },
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { err -> { Text(err) } },
                colors = rifugioFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            RifugioFieldLabel(stringResource(R.string.register_rifugio_password_label))
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
                colors = rifugioFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            uiState.generalError?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.register_rifugio_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))

            TsmGradientButton(
                text = if (uiState.isLoading) "ATTENDI…" else stringResource(R.string.register_rifugio_submit),
                onClick = {
                    keyboard?.hide()
                    viewModel.onSubmitClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }
}

@Composable
private fun RifugioFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun rifugioFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TsmAccent,
    unfocusedBorderColor = Color(0xFF3A3A3A),
    focusedContainerColor = TsmSurfaceVariant,
    unfocusedContainerColor = TsmSurfaceVariant,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = TsmAccent,
)
