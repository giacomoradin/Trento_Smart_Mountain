package it.trentosmartmountain.app.ui.screens.onboarding

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.PersonalInfo
import it.trentosmartmountain.app.ui.screens.profilev2.BirthDateField
import it.trentosmartmountain.app.ui.screens.profilev2.NumberFieldDouble
import it.trentosmartmountain.app.ui.screens.profilev2.NumberFieldInt
import it.trentosmartmountain.app.ui.screens.profilev2.ProfileV2Labels
import it.trentosmartmountain.app.ui.screens.profilev2.SectionHeader
import it.trentosmartmountain.app.ui.screens.profilev2.SegmentedChips
import it.trentosmartmountain.app.viewmodel.ProfileV2ViewModel

@Composable
fun OnboardingPersonalInfoScreen(
    onSkipStep: () -> Unit,
    onSkipAll: () -> Unit,
    onNext: () -> Unit,
    viewModel: ProfileV2ViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sex by remember(state.personalInfo) { mutableStateOf(state.personalInfo?.sex) }
    var heightCm by remember(state.personalInfo) { mutableStateOf(state.personalInfo?.heightCm) }
    var weightKg by remember(state.personalInfo) { mutableStateOf(state.personalInfo?.weightKg) }
    var birthDate by remember(state.personalInfo) { mutableStateOf(state.personalInfo?.birthDate?.take(10).orEmpty()) }

    OnboardingStepScaffold(
        stepIndex = 1,
        title = "Dati personali",
        subtitle = "Servono per stime caloriche e demografiche. Puoi saltare e compilarli dopo.",
        isSaving = state.isSavingSection,
        onSkipStep = onSkipStep,
        // "Salta tutto" da uno step intermedio: marca subito il profilo come completato
        // (il banner in ProfileScreen sparisce) e torna alla shell tramite onSkipAll del NavHost.
        onSkipAll = {
            viewModel.completeOnboarding()
            onSkipAll()
        },
        onSaveAndContinue = {
            // Salva solo se almeno un campo è stato compilato — altrimenti è un skip implicito
            // (il backend rifiuterebbe un body vuoto col Joi `.min(1)`).
            val hasAnyValue = sex != null || heightCm != null || weightKg != null || birthDate.length == 10
            if (hasAnyValue) {
                viewModel.savePersonalInfo(
                    PersonalInfo(
                        sex = sex,
                        heightCm = heightCm,
                        weightKg = weightKg,
                        birthDate = birthDate.takeIf { it.length == 10 },
                    ),
                )
            }
            onNext()
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Genere", "Solo per stime kcal — non condiviso pubblicamente.")
            SegmentedChips(
                options = ProfileV2Labels.sexValues,
                selected = sex,
                labelFromValue = { ProfileV2Labels.sexLabel(it) },
                onSelect = { sex = it },
            )
            SectionHeader("Altezza e peso", "Migliorano le stime caloriche delle escursioni.")
            NumberFieldInt(value = heightCm, onChange = { heightCm = it }, label = "Altezza", suffix = "cm")
            NumberFieldDouble(value = weightKg, onChange = { weightKg = it }, label = "Peso", suffix = "kg")
            SectionHeader("Data di nascita", "Tappa per aprire il calendario.")
            BirthDateField(
                isoValue = birthDate.ifBlank { null },
                onIsoChange = { iso -> birthDate = iso.orEmpty() },
            )
        }
    }
}

