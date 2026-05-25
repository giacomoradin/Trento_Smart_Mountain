package it.trentosmartmountain.app.ui.screens.profilev2

import android.app.Application
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import it.trentosmartmountain.app.data.remote.dto.PersonalInfo
import it.trentosmartmountain.app.viewmodel.ProfileV2ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileV2ViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Form state locale, allineato all'ultimo profilo caricato dal server.
    // Quando arriva un nuovo profilo (loadProfile o save success) re-init dei field.
    var sex by remember(state.personalInfo) { mutableStateOf(state.personalInfo?.sex) }
    var heightCm by remember(state.personalInfo) { mutableStateOf(state.personalInfo?.heightCm) }
    var weightKg by remember(state.personalInfo) { mutableStateOf(state.personalInfo?.weightKg) }
    // BirthDate gestita come stringa ISO yyyy-MM-dd per ora — un date picker
    // dedicato la migliora ma è oltre lo scope di questa iterazione.
    var birthDate by remember(state.personalInfo) { mutableStateOf(state.personalInfo?.birthDate?.take(10).orEmpty()) }

    LaunchedEffect(state.sectionSuccess, state.sectionError) {
        val msg = state.sectionSuccess ?: state.sectionError
        if (!msg.isNullOrBlank()) { snackbar.showSnackbar(msg); viewModel.clearSectionMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Dati personali", color = Color.White, fontWeight = FontWeight.Bold) },
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
                title = "Genere",
                subtitle = "Usato solo per stime caloriche più accurate. Non condiviso pubblicamente.",
            )
            SegmentedChips(
                options = ProfileV2Labels.sexValues,
                selected = sex,
                labelFromValue = { ProfileV2Labels.sexLabel(it) },
                onSelect = { sex = it },
            )

            SectionHeader(
                title = "Misure corporee",
                subtitle = "Migliorano la stima delle calorie consumate sulle escursioni.",
            )
            NumberFieldInt(value = heightCm, onChange = { heightCm = it }, label = "Altezza", suffix = "cm")
            NumberFieldDouble(value = weightKg, onChange = { weightKg = it }, label = "Peso", suffix = "kg")

            SectionHeader(
                title = "Data di nascita",
                subtitle = "Usata lato server per calcolare l'età. Non condivisa pubblicamente.",
            )
            // Material3 DatePickerDialog wrapped — input ISO yyyy-MM-dd, display localizzato.
            BirthDateField(
                isoValue = birthDate.ifBlank { null },
                onIsoChange = { iso -> birthDate = iso.orEmpty() },
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.savePersonalInfo(
                        PersonalInfo(
                            sex = sex,
                            heightCm = heightCm,
                            weightKg = weightKg,
                            birthDate = birthDate.takeIf { it.length == 10 },
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
