package it.trentosmartmountain.app.ui.screens.profile

import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.screens.profilev2.ProfileV2Labels
import it.trentosmartmountain.app.viewmodel.ProfileV2ViewModel
import it.trentosmartmountain.app.viewmodel.ProfileViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBg = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentGreen = Color(0xFF4CAF50)
private val TextSecondary = Color(0xFF8E8E93)
private val LockColor = Color(0xFF636366)

/**
 * Schermata di sola lettura con tutti i dati del profilo compilato.
 *
 * Campi modificabili: genere, altezza, peso, forma fisica, frequenza allenamento,
 *   preferenze app (unità, notifiche, privacy).
 * Campi bloccati (anti-cheat): username, email, data di nascita, livello CAI.
 *
 * Il FAB "Modifica" porta alla sezione AccountEditScreen con le sub-sezioni
 * di edit. I campi bloccati mostrano solo lettura senza azione.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileViewScreen(
    onBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: ProfileV2ViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            (LocalContext.current as ComponentActivity).application,
        ),
    ),
    // Serve solo per leggere lo username e generare le iniziali del fallback
    // quando l'utente non ha ancora caricato una foto profilo.
    profileViewModel: ProfileViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profileUi by profileViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Il mio profilo", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToEdit,
                containerColor = AccentCyan,
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Modifica profilo", tint = DarkSurface)
            }
        },
        containerColor = DarkSurface,
    ) { padding ->
        if (state.isLoadingProfile) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = AccentCyan)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Header con avatar grande + username ──────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarImage(
                    avatarUrl = state.personalInfo?.avatarUrl,
                    fallbackName = profileUi.username,
                    size = 88.dp,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = profileUi.username ?: "—",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    val emailLine = profileUi.email
                    if (!emailLine.isNullOrBlank()) {
                        Text(
                            text = emailLine,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // ── Sezione: Dati personali ───────────────────────────
            ProfileSection(title = "Dati personali") {
                val p = state.personalInfo
                ProfileRow(label = "Genere", value = ProfileV2Labels.sexLabel(p?.sex), locked = false)
                ProfileRow(label = "Altezza", value = p?.heightCm?.let { "${it} cm" }, locked = false)
                ProfileRow(label = "Peso", value = p?.weightKg?.let { "${it} kg" }, locked = false)
                ProfileRow(
                    label = "Data di nascita",
                    value = p?.birthDate?.take(10)?.let { formatBirthDateDisplay(it) },
                    locked = true,
                    lockNote = "non modificabile",
                )
            }

            // ── Sezione: Esperienza outdoor ───────────────────────
            ProfileSection(title = "Esperienza outdoor") {
                val e = state.experience
                ProfileRow(
                    label = "Livello CAI",
                    value = ProfileV2Labels.caiLabel(e?.caiLevel),
                    locked = true,
                    lockNote = "non modificabile",
                )
                ProfileRow(label = "Forma fisica", value = ProfileV2Labels.fitnessLabel(e?.baselineFitness), locked = false)
                ProfileRow(label = "Frequenza allenamento", value = ProfileV2Labels.trainingFreqLabel(e?.weeklyTrainingFreq), locked = false)
            }

            // ── Sezione: Preferenze app ───────────────────────────
            ProfileSection(title = "Preferenze app") {
                val pr = state.preferences
                ProfileRow(label = "Unità di misura", value = ProfileV2Labels.unitsLabel(pr?.units), locked = false)
                ProfileRow(label = "Notifiche push", value = if (pr?.notifications?.pushEnabled == true) "Attive" else "Disattivate", locked = false)
                ProfileRow(label = "Digest email", value = if (pr?.notifications?.emailDigest == true) "Attivo" else "Disattivato", locked = false)
                ProfileRow(label = "Visibilità profilo", value = ProfileV2Labels.privacyLabel(pr?.privacy?.profileVisibility), locked = false)
            }

            // ── Sezione: Obiettivi settimanali ────────────────────
            ProfileSection(title = "Obiettivi settimanali") {
                val g = state.weeklyGoals
                ProfileRow(label = "Distanza", value = g?.km?.takeIf { it > 0 }?.let { "${it} km" } ?: "Non impostato", locked = false)
                ProfileRow(label = "Dislivello", value = g?.elevM?.takeIf { it > 0 }?.let { "${it} m" } ?: "Non impostato", locked = false)
                ProfileRow(label = "Escursioni", value = g?.count?.takeIf { it > 0 }?.let { "$it / settimana" } ?: "Non impostato", locked = false)
            }

            Spacer(Modifier.height(72.dp)) // spazio per FAB
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title.uppercase(),
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
            )
            content()
        }
    }
}

@Composable
private fun ProfileRow(
    label: String,
    value: String?,
    locked: Boolean,
    lockNote: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            if (locked) {
                Text("🔒", fontSize = 10.sp)
                if (!lockNote.isNullOrBlank()) {
                    Text("($lockNote)", color = LockColor, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text(
            text = value?.takeIf { it.isNotBlank() && it != "—" } ?: "—",
            color = if (!value.isNullOrBlank() && value != "—") Color.White else TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Mostra yyyy-MM-dd nel formato "21 marzo 1995" senza costruire un SimpleDateFormat
 * extra (riuso della stessa logica di BirthDateField).
 */
private fun formatBirthDateDisplay(iso: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val display = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.ITALIAN).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
        val date = sdf.parse(iso) ?: return iso
        display.format(date)
    } catch (_: Exception) { iso }
}
