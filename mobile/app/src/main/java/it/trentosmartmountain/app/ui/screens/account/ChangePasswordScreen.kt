package it.trentosmartmountain.app.ui.screens.account

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGradientButton
import it.trentosmartmountain.app.ui.components.TsmSnackbar
import it.trentosmartmountain.app.viewmodel.AccountEditViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val AccentCyan = Color(0xFF4DD0E1)
private val TextSecondary = Color(0xFF8E8E93)
private val FieldBorder = Color(0xFF3A3A3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: AccountEditViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var oldVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.success) {
        state.success?.let { snackbarHost.showSnackbar(it); viewModel.clearMessages(); onBack() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHost.showSnackbar(it); viewModel.clearMessages() }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) {
    TsmAuroraBackground(modifier = Modifier.fillMaxSize(), particleCount = 10)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) { TsmSnackbar(it) } },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Cambia password", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            @Composable
            fun passwordField(value: String, label: String, visible: Boolean, onToggle: () -> Unit, onValueChange: (String) -> Unit) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(label, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = onToggle) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null, tint = TextSecondary,
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan, unfocusedBorderColor = FieldBorder,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = AccentCyan,
                    ),
                )
            }

            passwordField(oldPassword, "Password attuale", oldVisible, { oldVisible = !oldVisible }) { oldPassword = it }
            passwordField(newPassword, "Nuova password", newVisible, { newVisible = !newVisible }) { newPassword = it }
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Conferma nuova password", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan, unfocusedBorderColor = FieldBorder,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = AccentCyan,
                ),
            )
            if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                Text("Le password non coincidono.", color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
            // Block accidentally setting the same password — il backend non lo rifiuta, ma è sempre un errore dell'utente.
            val sameAsOld = oldPassword.isNotEmpty() && newPassword.isNotEmpty() && oldPassword == newPassword
            if (sameAsOld) {
                Text("La nuova password deve essere diversa dall'attuale.", color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(4.dp))

            TsmGradientButton(
                text = if (state.isLoading) "AGGIORNAMENTO…" else "AGGIORNA PASSWORD",
                onClick = { viewModel.changePassword(oldPassword, newPassword) },
                modifier = Modifier.fillMaxWidth(),
                fill = Brush.horizontalGradient(listOf(AccentCyan, Color(0xFF0097A7))),
                contentColor = DarkSurface,
                enabled = !state.isLoading && oldPassword.isNotBlank() && newPassword.length >= 8 && newPassword == confirmPassword && !sameAsOld,
            )
        }
    }
    }
}
