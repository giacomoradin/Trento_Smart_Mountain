package it.trentosmartmountain.app.ui.screens.account

import android.app.Application
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.ui.components.TsmGradientButton
import it.trentosmartmountain.app.ui.components.TsmSnackbar
import it.trentosmartmountain.app.viewmodel.AccountEditViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentRed = Color(0xFFE91E63)
private val TextSecondary = Color(0xFF8E8E93)
private val FieldBorder = Color(0xFF3A3A3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    onBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToPersonalInfo: () -> Unit = {},
    onNavigateToExperience: () -> Unit = {},
    onNavigateToPreferences: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onAccountDeleted: () -> Unit,
    viewModel: AccountEditViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Pre-populiamo i campi non appena il VM completa il GET /hikers/:id.
    // `key = currentUsername/Email` resetta i campi locali quando arrivano i valori dal server
    // (al primo render currentUsername è "" → poi diventa il valore reale e i remember si rigenerano).
    var username by remember(state.currentUsername) { mutableStateOf(state.currentUsername) }
    var email by remember(state.currentEmail) { mutableStateOf(state.currentEmail) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.success) {
        state.success?.let { snackbarHost.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHost.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(state.accountDeleted) {
        if (state.accountDeleted) onAccountDeleted()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) {
    TsmAuroraBackground(modifier = Modifier.fillMaxSize(), particleCount = 12)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) { TsmSnackbar(it) } },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Account e dati personali", color = Color.White, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Modifica profilo", color = TextSecondary, style = MaterialTheme.typography.labelMedium)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = FieldBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AccentCyan,
                ),
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = FieldBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AccentCyan,
                ),
            )
            if (state.requiresEmailVerification) {
                Text("Una nuova email di verifica verrà inviata.", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
            }

            val hasChanges = username.trim().isNotBlank() && (
                username.trim() != state.currentUsername || email.trim() != state.currentEmail
            ) || (email.trim().isNotBlank() && email.trim() != state.currentEmail)

            TsmGradientButton(
                text = if (state.isLoading || state.isLoadingProfile) "SALVATAGGIO…" else "SALVA MODIFICHE",
                onClick = { viewModel.updateAccount(username, email) },
                modifier = Modifier.fillMaxWidth(),
                fill = Brush.horizontalGradient(listOf(AccentCyan, Color(0xFF0097A7))),
                contentColor = DarkSurface,
                enabled = !state.isLoading && !state.isLoadingProfile && hasChanges,
            )

            Spacer(Modifier.height(8.dp))
            Text("Il mio profilo", color = TextSecondary, style = MaterialTheme.typography.labelMedium)

            // ── Profilo v2: 3 cards di edit per-sezione ──────────────────────
            // Ognuna apre una schermata dedicata (PersonalInfo/Experience/Preferences)
            // che condivide ProfileV2ViewModel per cache + state coerente.
            SectionNavCard(
                icon = Icons.Default.Person,
                title = "Dati personali",
                subtitle = "Genere, data di nascita, altezza, peso",
                onClick = onNavigateToPersonalInfo,
            )
            SectionNavCard(
                icon = Icons.Default.FitnessCenter,
                title = "Esperienza outdoor",
                subtitle = "Livello CAI, forma fisica, frequenza allenamento",
                onClick = onNavigateToExperience,
            )
            SectionNavCard(
                icon = Icons.Default.Settings,
                title = "Preferenze app",
                subtitle = "Unità di misura, notifiche, privacy",
                onClick = onNavigateToPreferences,
            )
            SectionNavCard(
                icon = Icons.Default.Flag,
                title = "Obiettivi settimanali",
                subtitle = "Target km, dislivello e numero escursioni",
                onClick = onNavigateToGoals,
            )

            Spacer(Modifier.height(8.dp))
            Text("Sicurezza", color = TextSecondary, style = MaterialTheme.typography.labelMedium)

            TsmGlassCard(
                onClick = onNavigateToChangePassword,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
            ) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AccountIconChip(Icons.Default.Lock, AccentCyan)
                    Spacer(Modifier.width(12.dp))
                    Text("Cambia password", color = Color.White, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Zona pericolo", color = AccentRed, style = MaterialTheme.typography.labelMedium)

            TsmGlassCard(
                onClick = onNavigateToDeleteAccount,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                topColor = Color(0xFF2A1620),
                bottomColor = Color(0xFF1E1014),
                border = AccentRed.copy(alpha = 0.4f),
            ) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AccountIconChip(Icons.Default.Warning, AccentRed)
                    Spacer(Modifier.width(12.dp))
                    Text("Elimina account", color = AccentRed, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentRed.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    }
}

/** Chip-icona quadrato tinto, coerente con il resto dell'app. */
@Composable
private fun AccountIconChip(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Card cliccabile con icona + titolo + sottotitolo + chevron, usata per le entry profilo v2. */
@Composable
private fun SectionNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    TsmGlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountIconChip(icon, AccentCyan)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
