package it.trentosmartmountain.app.ui.screens.profilev2

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.Experience
import it.trentosmartmountain.app.viewmodel.ProfileV2ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileV2ViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            (LocalContext.current as ComponentActivity).application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // caiLevel è BLOCCATO dopo la prima compilazione (anti-cheat: determina il
    // moltiplicatore baseline dei crediti e non deve essere modificabile).
    val caiLevelLocked = state.experience?.caiLevel != null
    var caiLevel by remember(state.experience) { mutableStateOf(state.experience?.caiLevel) }
    var baselineFitness by remember(state.experience) { mutableStateOf(state.experience?.baselineFitness) }
    var trainingFreq by remember(state.experience) { mutableStateOf(state.experience?.weeklyTrainingFreq) }

    LaunchedEffect(state.sectionSuccess, state.sectionError) {
        val msg = state.sectionSuccess ?: state.sectionError
        if (!msg.isNullOrBlank()) { snackbar.showSnackbar(msg); viewModel.clearSectionMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Esperienza outdoor", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
        },
        containerColor = DarkSurface,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Banner informativo: spiega perché questi dati contano per il punteggio.
            Card(
                colors = CardDefaults.cardColors(containerColor = SelectedBg),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    "I tuoi dati di esperienza alimentano il moltiplicatore di crediti delle sessioni. " +
                        "Più sei principiante, più ogni escursione vale crediti — premiamo la sfida personale.",
                    modifier = Modifier.padding(14.dp),
                    color = AccentCyan,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            SectionHeader(
                title = "Livello tecnico CAI",
                subtitle = if (caiLevelLocked)
                    "🔒 Campo bloccato — il livello CAI non è modificabile dopo la prima compilazione (anti-cheat)."
                else
                    "Scala ufficiale dei sentieri italiani. Si traduce nel filtro \"esperienza minima\" delle sessioni.",
            )
            SegmentedChips(
                options = ProfileV2Labels.caiLevels,
                selected = caiLevel,
                labelFromValue = { ProfileV2Labels.caiLabel(it) },
                onSelect = { if (!caiLevelLocked) caiLevel = it },
                locked = caiLevelLocked,
            )

            SectionHeader(
                title = "Livello di forma fisica",
                subtitle = "Auto-valutazione baseline. Influenza il coefficiente di sfida nel calcolo crediti.",
            )
            SegmentedChips(
                options = ProfileV2Labels.baselineFitnessValues,
                selected = baselineFitness,
                labelFromValue = { ProfileV2Labels.fitnessLabel(it) },
                onSelect = { baselineFitness = it },
            )

            SectionHeader(
                title = "Frequenza di allenamento",
                subtitle = "Approssimazione media settimanale di attività fisica programmata.",
            )
            SegmentedChips(
                options = ProfileV2Labels.trainingFreqValues,
                selected = trainingFreq,
                labelFromValue = { ProfileV2Labels.trainingFreqLabel(it) },
                onSelect = { trainingFreq = it },
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.saveExperience(
                        Experience(
                            caiLevel = caiLevel,
                            baselineFitness = baselineFitness,
                            weeklyTrainingFreq = trainingFreq,
                        ),
                    )
                },
                enabled = !state.isSavingSection,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            ) {
                if (state.isSavingSection) CircularProgressIndicator(color = Color.White, modifier = Modifier.height(20.dp))
                else Text("SALVA", fontWeight = FontWeight.Bold, color = DarkSurface)
            }
        }
    }
}
