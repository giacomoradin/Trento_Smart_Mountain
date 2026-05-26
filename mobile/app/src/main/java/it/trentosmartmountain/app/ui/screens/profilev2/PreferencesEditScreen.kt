package it.trentosmartmountain.app.ui.screens.profilev2

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.NotificationPreferences
import it.trentosmartmountain.app.data.remote.dto.Preferences
import it.trentosmartmountain.app.data.remote.dto.PrivacyPreferences
import it.trentosmartmountain.app.viewmodel.ProfileV2ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesEditScreen(
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

    var units by remember(state.preferences) { mutableStateOf(state.preferences?.units ?: "metric") }
    var pushEnabled by remember(state.preferences) { mutableStateOf(state.preferences?.notifications?.pushEnabled ?: true) }
    var emailDigest by remember(state.preferences) { mutableStateOf(state.preferences?.notifications?.emailDigest ?: false) }
    var visibility by remember(state.preferences) { mutableStateOf(state.preferences?.privacy?.profileVisibility ?: "friends") }

    LaunchedEffect(state.sectionSuccess, state.sectionError) {
        val msg = state.sectionSuccess ?: state.sectionError
        if (!msg.isNullOrBlank()) { snackbar.showSnackbar(msg); viewModel.clearSectionMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Preferenze", color = Color.White, fontWeight = FontWeight.Bold) },
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
            SectionHeader(
                title = "Unità di misura",
                subtitle = "Si applica a distanze, dislivelli e peso in tutta l'app.",
            )
            SegmentedChips(
                options = ProfileV2Labels.unitsValues,
                selected = units,
                labelFromValue = { ProfileV2Labels.unitsLabel(it) },
                onSelect = { units = it },
            )

            SectionHeader(
                title = "Notifiche",
                subtitle = "Push per sessioni e totem, digest email settimanale opzionale.",
            )
            ToggleRow(label = "Notifiche push", checked = pushEnabled, onChange = { pushEnabled = it })
            ToggleRow(label = "Digest email settimanale", checked = emailDigest, onChange = { emailDigest = it })

            SectionHeader(
                title = "Visibilità profilo",
                subtitle = "Chi può vedere le tue attività e i tuoi crediti nella futura sezione Social.",
            )
            SegmentedChips(
                options = ProfileV2Labels.privacyValues,
                selected = visibility,
                labelFromValue = { ProfileV2Labels.privacyLabel(it) },
                onSelect = { visibility = it },
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.savePreferences(
                        Preferences(
                            units = units,
                            notifications = NotificationPreferences(
                                pushEnabled = pushEnabled,
                                emailDigest = emailDigest,
                            ),
                            privacy = PrivacyPreferences(profileVisibility = visibility),
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
