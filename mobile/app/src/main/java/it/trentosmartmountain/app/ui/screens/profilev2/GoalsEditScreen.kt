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
import it.trentosmartmountain.app.viewmodel.ProfileV2ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsEditScreen(
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

    var km by remember(state.weeklyGoals) { mutableStateOf(state.weeklyGoals?.km) }
    var elevM by remember(state.weeklyGoals) { mutableStateOf(state.weeklyGoals?.elevM) }
    var count by remember(state.weeklyGoals) { mutableStateOf(state.weeklyGoals?.count) }

    LaunchedEffect(state.sectionSuccess, state.sectionError) {
        val msg = state.sectionSuccess ?: state.sectionError
        if (!msg.isNullOrBlank()) { snackbar.showSnackbar(msg); viewModel.clearSectionMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Obiettivi settimanali", color = Color.White, fontWeight = FontWeight.Bold) },
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
            // Banner motivazionale: spiega che gli obiettivi sono privati e self-tracking
            Card(
                colors = CardDefaults.cardColors(containerColor = SelectedBg),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    "Imposta target settimanali per distanza, dislivello e numero di escursioni. " +
                        "Sono privati — servono solo a te per monitorare i progressi.",
                    modifier = Modifier.padding(12.dp),
                    color = AccentCyan,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            SectionHeader("Distanza", "Quanti km vuoi percorrere a settimana? (0 disattiva l'obiettivo)")
            NumberFieldInt(value = km, onChange = { km = it }, label = "Km/settimana", suffix = "km")

            SectionHeader("Dislivello", "Quanti metri di dislivello positivo cumulato a settimana?")
            NumberFieldInt(value = elevM, onChange = { elevM = it }, label = "Dislivello/settimana", suffix = "m")

            SectionHeader("Frequenza", "Quante escursioni a settimana?")
            NumberFieldInt(value = count, onChange = { count = it }, label = "Escursioni/settimana", suffix = "esc.")

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveGoals(km, elevM, count) },
                enabled = !state.isSavingSection,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            ) {
                if (state.isSavingSection) CircularProgressIndicator(color = Color.White, modifier = Modifier.height(20.dp))
                else Text("SALVA OBIETTIVI", fontWeight = FontWeight.Bold, color = DarkSurface)
            }
        }
    }
}
