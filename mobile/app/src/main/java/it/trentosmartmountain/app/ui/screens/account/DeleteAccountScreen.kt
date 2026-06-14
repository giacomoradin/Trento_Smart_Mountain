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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.ui.components.TsmGradientButton
import it.trentosmartmountain.app.viewmodel.AccountEditViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val AccentRed = Color(0xFFE91E63)
private val TextSecondary = Color(0xFF8E8E93)
private val FieldBorder = Color(0xFF3A3A3C)
private val AccentCyan = Color(0xFF4DD0E1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: AccountEditViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.accountDeleted) {
        if (state.accountDeleted) onAccountDeleted()
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHost.showSnackbar(it); viewModel.clearMessages() }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) {
    TsmAuroraBackground(modifier = Modifier.fillMaxSize(), particleCount = 10)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Elimina account", color = AccentRed, fontWeight = FontWeight.Bold) },
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
            TsmGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                topColor = Color(0xFF2A1620),
                bottomColor = Color(0xFF1E1014),
                border = AccentRed.copy(alpha = 0.5f),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed)
                    Spacer(Modifier.height(8.dp))
                    Text("Questa azione è irreversibile.", color = AccentRed, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Il tuo account, tutte le attività e i quiz completati verranno eliminati definitivamente. I dati aggregati (statistiche) vengono anonimizzati.",
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Text("Conferma con la tua password", color = TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentRed, unfocusedBorderColor = FieldBorder,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = AccentRed,
                ),
            )

            Spacer(Modifier.height(8.dp))

            // Apre il dialog di conferma — la delete viene chiamata SOLO dopo "ELIMINA"
            // così un tap accidentale sul pulsante rosso non rende l'azione irreversibile.
            TsmGradientButton(
                text = if (state.isLoading) "ELIMINAZIONE…" else "ELIMINA DEFINITIVAMENTE",
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                fill = Brush.horizontalGradient(listOf(AccentRed, Color(0xFFB0003A))),
                enabled = !state.isLoading && password.isNotBlank(),
            )
        }

        if (showConfirmDialog) {
            it.trentosmartmountain.app.ui.components.TsmAlertDialog(
                onDismiss = { showConfirmDialog = false },
                title = "Sei sicuro?",
                text = "Eliminerai il tuo account, tutte le attività personali e i quiz completati. " +
                    "Le sessioni di gruppo di cui sei Capogruppo verranno trasferite a un altro " +
                    "partecipante (o annullate se sei l'unico iscritto). Questa operazione non è annullabile.",
                confirmLabel = "ELIMINA",
                destructive = true,
                icon = Icons.Default.Warning,
                onConfirm = {
                    showConfirmDialog = false
                    viewModel.deleteAccount(password)
                },
            )
        }
    }
    }
}
