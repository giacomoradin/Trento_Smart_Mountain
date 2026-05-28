package it.trentosmartmountain.app.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.NotificationPreferences
import it.trentosmartmountain.app.data.remote.dto.Preferences
import it.trentosmartmountain.app.data.remote.dto.PrivacyPreferences
import it.trentosmartmountain.app.ui.screens.profilev2.ProfileV2Labels
import it.trentosmartmountain.app.ui.screens.profilev2.SectionHeader
import it.trentosmartmountain.app.ui.screens.profilev2.SegmentedChips
import it.trentosmartmountain.app.viewmodel.ProfileV2ViewModel

private val AccentCyan = Color(0xFF4DD0E1)
private val SelectedBg = Color(0xFF1A2A3A)
private val FieldBorder = Color(0xFF3A3A3C)
private val TextSecondary = Color(0xFF8E8E93)

@Composable
fun OnboardingPreferencesScreen(
    onSkipStep: () -> Unit,
    onSkipAll: () -> Unit,
    onFinish: () -> Unit,
    viewModel: ProfileV2ViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            (LocalContext.current as ComponentActivity).application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var units by remember(state.preferences) { mutableStateOf(state.preferences?.units ?: "metric") }
    var pushEnabled by remember(state.preferences) { mutableStateOf(state.preferences?.notifications?.pushEnabled ?: true) }
    var emailDigest by remember(state.preferences) { mutableStateOf(state.preferences?.notifications?.emailDigest ?: false) }
    var visibility by remember(state.preferences) { mutableStateOf(state.preferences?.privacy?.profileVisibility ?: "friends") }

    OnboardingStepScaffold(
        stepIndex = 3,
        title = "Preferenze app",
        subtitle = "Unità, notifiche, privacy. Tutto modificabile in seguito da Account.",
        isSaving = state.isSavingSection,
        onSkipStep = {
            // "Salta" sull'ultimo step = "Termina senza salvare le preferenze".
            // Marchiamo comunque il profilo come completato per evitare banner ricorrenti.
            viewModel.completeOnboarding()
            onSkipStep()
        },
        onSkipAll = {
            viewModel.completeOnboarding()
            onSkipAll()
        },
        onSaveAndContinue = {
            viewModel.savePreferences(
                Preferences(
                    units = units,
                    notifications = NotificationPreferences(pushEnabled = pushEnabled, emailDigest = emailDigest),
                    privacy = PrivacyPreferences(profileVisibility = visibility),
                ),
            )
            viewModel.completeOnboarding()
            onFinish()
        },
        saveLabel = "TERMINA",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Unità di misura", "Si applica a distanze, dislivelli, peso.")
            SegmentedChips(
                options = ProfileV2Labels.unitsValues,
                selected = units,
                labelFromValue = { ProfileV2Labels.unitsLabel(it) },
                onSelect = { units = it },
            )

            SectionHeader("Notifiche", "Push e digest email.")
            ToggleRow("Notifiche push", pushEnabled) { pushEnabled = it }
            ToggleRow("Digest email settimanale", emailDigest) { emailDigest = it }

            SectionHeader("Visibilità profilo", "Per la futura sezione Social.")
            SegmentedChips(
                options = ProfileV2Labels.privacyValues,
                selected = visibility,
                labelFromValue = { ProfileV2Labels.privacyLabel(it) },
                onSelect = { visibility = it },
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentCyan,
                checkedTrackColor = SelectedBg,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = FieldBorder,
            ),
        )
    }
}
