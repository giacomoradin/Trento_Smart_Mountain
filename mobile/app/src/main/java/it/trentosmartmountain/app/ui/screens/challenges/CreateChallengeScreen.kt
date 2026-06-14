package it.trentosmartmountain.app.ui.screens.challenges

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.CreateChallengeRequest
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGradientButton
import it.trentosmartmountain.app.ui.components.TsmSnackbar
import it.trentosmartmountain.app.ui.screens.profilev2.NumberFieldDouble
import it.trentosmartmountain.app.ui.screens.profilev2.SectionHeader
import it.trentosmartmountain.app.ui.screens.profilev2.SegmentedChips
import it.trentosmartmountain.app.viewmodel.ChallengesViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val AccentCyan = Color(0xFF4DD0E1)
private val TextSecondary = Color(0xFF8E8E93)
private val FieldBorder = Color(0xFF3A3A3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeScreen(
    onBack: () -> Unit,
    onCreated: (challengeId: String) -> Unit,
    viewModel: ChallengesViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var metric by remember { mutableStateOf("distance") }
    var targetValue by remember { mutableStateOf<Double?>(null) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var participantIds by remember { mutableStateOf("") } // CSV per MVP

    LaunchedEffect(state.operationMessage, state.error) {
        val msg = state.operationMessage ?: state.error
        if (!msg.isNullOrBlank()) { snackbar.showSnackbar(msg); viewModel.clearMessages() }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) {
    TsmAuroraBackground(modifier = Modifier.fillMaxSize(), particleCount = 12)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) { TsmSnackbar(it) } },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Nuova sfida", color = Color.White, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionHeader("Titolo", "Breve descrittivo della sfida (max 80 caratteri).")
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                label = { Text("Titolo", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(),
                singleLine = true,
            )

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 280) description = it },
                label = { Text("Descrizione (opzionale)", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(),
            )

            SectionHeader("Metrica", "Cosa si misura per stabilire il vincitore.")
            SegmentedChips(
                options = listOf("distance", "elevation", "count", "points"),
                selected = metric,
                labelFromValue = { metricLabel(it) },
                onSelect = { metric = it },
            )

            SectionHeader(
                "Target (opzionale)",
                "Se lasci vuoto: vince chi accumula di più nel periodo.",
            )
            NumberFieldDouble(
                value = targetValue,
                onChange = { targetValue = it },
                label = "Valore obiettivo",
                suffix = metricSuffix(metric),
            )

            SectionHeader("Date (AAAA-MM-GG)", "Inizio e fine del periodo di gara.")
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it.filter { c -> c.isDigit() || c == '-' }.take(10) },
                label = { Text("Inizio", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(),
                singleLine = true,
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it.filter { c -> c.isDigit() || c == '-' }.take(10) },
                label = { Text("Fine", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(),
                singleLine = true,
            )

            SectionHeader(
                "Invita partecipanti (opzionale)",
                "Per ora: ID utenti separati da virgola. Picker by-username arriva con la sezione Social.",
            )
            OutlinedTextField(
                value = participantIds,
                onValueChange = { participantIds = it },
                label = { Text("ObjectId separati da virgola", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(),
            )

            Spacer(Modifier.height(8.dp))

            val canSubmit = title.length >= 3 &&
                startDate.length == 10 && endDate.length == 10
            TsmGradientButton(
                text = "CREA SFIDA",
                onClick = {
                    val ids = participantIds
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it.length == 24 }
                    viewModel.createChallenge(
                        CreateChallengeRequest(
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            metric = metric,
                            targetValue = targetValue,
                            startDate = "${startDate}T00:00:00.000Z",
                            endDate = "${endDate}T23:59:59.999Z",
                            participantUserIds = ids,
                        ),
                        onCreated = onCreated,
                    )
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
                fill = Brush.horizontalGradient(listOf(AccentCyan, Color(0xFF0097A7))),
                contentColor = DarkSurface,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = FieldBorder,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = AccentCyan,
)

private fun metricSuffix(metric: String): String = when (metric) {
    "distance" -> "km"
    "elevation" -> "m"
    "count" -> "esc."
    "points" -> "pt"
    else -> ""
}
