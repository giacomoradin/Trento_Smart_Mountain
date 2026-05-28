package it.trentosmartmountain.app.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.Experience
import it.trentosmartmountain.app.ui.screens.profilev2.ProfileV2Labels
import it.trentosmartmountain.app.ui.screens.profilev2.SectionHeader
import it.trentosmartmountain.app.ui.screens.profilev2.SegmentedChips
import it.trentosmartmountain.app.viewmodel.ProfileV2ViewModel

private val AccentCyan = Color(0xFF4DD0E1)
private val SelectedBg = Color(0xFF1A2A3A)

@Composable
fun OnboardingExperienceScreen(
    onSkipStep: () -> Unit,
    onSkipAll: () -> Unit,
    onNext: () -> Unit,
    viewModel: ProfileV2ViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            (LocalContext.current as ComponentActivity).application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var caiLevel by remember(state.experience) { mutableStateOf(state.experience?.caiLevel) }
    var baselineFitness by remember(state.experience) { mutableStateOf(state.experience?.baselineFitness) }
    var trainingFreq by remember(state.experience) { mutableStateOf(state.experience?.weeklyTrainingFreq) }

    OnboardingStepScaffold(
        stepIndex = 2,
        title = "Esperienza outdoor",
        subtitle = "Influenza il moltiplicatore di crediti delle escursioni. Più sei principiante, più ogni hike vale.",
        isSaving = state.isSavingSection,
        onSkipStep = onSkipStep,
        onSkipAll = {
            viewModel.completeOnboarding()
            onSkipAll()
        },
        onSaveAndContinue = {
            val hasAnyValue = caiLevel != null || baselineFitness != null || trainingFreq != null
            if (hasAnyValue) {
                viewModel.saveExperience(
                    Experience(
                        caiLevel = caiLevel,
                        baselineFitness = baselineFitness,
                        weeklyTrainingFreq = trainingFreq,
                    ),
                )
            }
            onNext()
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SelectedBg),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    "Premiamo la sfida personale: un utente sedentario T che completa una hike EE prende un boost crediti maggiore di un atleta EEA sullo stesso percorso.",
                    modifier = Modifier.padding(12.dp),
                    color = AccentCyan,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            SectionHeader("Livello tecnico CAI", "Scala ufficiale dei sentieri italiani.")
            SegmentedChips(
                options = ProfileV2Labels.caiLevels,
                selected = caiLevel,
                labelFromValue = { ProfileV2Labels.caiLabel(it) },
                onSelect = { caiLevel = it },
            )

            SectionHeader("Forma fisica baseline", "Auto-valutazione del livello attuale.")
            SegmentedChips(
                options = ProfileV2Labels.baselineFitnessValues,
                selected = baselineFitness,
                labelFromValue = { ProfileV2Labels.fitnessLabel(it) },
                onSelect = { baselineFitness = it },
            )

            SectionHeader("Frequenza di allenamento", "Media settimanale di attività programmata.")
            SegmentedChips(
                options = ProfileV2Labels.trainingFreqValues,
                selected = trainingFreq,
                labelFromValue = { ProfileV2Labels.trainingFreqLabel(it) },
                onSelect = { trainingFreq = it },
            )
        }
    }
}
