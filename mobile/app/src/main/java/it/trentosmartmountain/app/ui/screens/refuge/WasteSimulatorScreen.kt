package it.trentosmartmountain.app.ui.screens.refuge

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.WasteSimulationResponse
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.WasteSimulatorViewModel
import java.util.Locale

private val Bg = TsmColors.DashboardBackground
private val CardBg = TsmColors.DashboardCard
private val Cyan = TsmColors.Info
private val WarnAmber = Color(0xFFFFB454)
private val OkGreen = TsmColors.Online
private val DangerRed = TsmColors.Offline
private val TextSecondary = TsmColors.TextSecondary

/**
 * Simulatore **Rifiuti & Logistica** del rifugio (ADR-002, MVP read-only):
 * bilancio di massa stagionale, alert di compliance (art. 185-bis D.Lgs.
 * 152/2006) e confronto dei costi di evacuazione per vettore. Il calcolo gira
 * sul backend (`POST /api/v1/refuge/waste/simulate`).
 */
@Composable
fun WasteSimulatorScreen(
    onBack: () -> Unit = {},
    viewModel: WasteSimulatorViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                }
                Column {
                    Text(
                        "Rifiuti & Logistica",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Simulazione stagionale — costi di evacuazione a valle",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Form parametri ──
            Surface(color = CardBg, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WasteField("Giorni stagione", state.periodDays, Modifier.weight(1f)) { v ->
                            viewModel.update { copy(periodDays = v) }
                        }
                        WasteField("Posti letto", state.beds, Modifier.weight(1f)) { v ->
                            viewModel.update { copy(beds = v) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WasteField("Occupazione %", state.bedOccupancyPct, Modifier.weight(1f)) { v ->
                            viewModel.update { copy(bedOccupancyPct = v) }
                        }
                        WasteField("Escursionisti/g", state.dayVisitors, Modifier.weight(1f)) { v ->
                            viewModel.update { copy(dayVisitors = v) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WasteField("kg/ospite/g", state.wastePerGuestKg, Modifier.weight(1f)) { v ->
                            viewModel.update { copy(wastePerGuestKg = v) }
                        }
                        WasteField("kg/escurs./g", state.wastePerVisitorKg, Modifier.weight(1f)) { v ->
                            viewModel.update { copy(wastePerVisitorKg = v) }
                        }
                        WasteField("Grigliato kg/g", state.screeningPerGuestKg, Modifier.weight(1f)) { v ->
                            viewModel.update { copy(screeningPerGuestKg = v) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = state.compactorEnabled,
                            onCheckedChange = { c -> viewModel.update { copy(compactorEnabled = c) } },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Pre-trattamento / compattatore", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = viewModel::simulate,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("Simula stagione", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    state.error?.let { Text(it, color = DangerRed, style = MaterialTheme.typography.bodySmall) }
                }
            }

            Spacer(Modifier.height(16.dp))
            state.result?.let { WasteResults(it) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WasteField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Cyan,
            unfocusedBorderColor = TsmColors.DashboardBorder,
        ),
    )
}

@Composable
private fun WasteResults(result: WasteSimulationResponse) {
    val totals = result.totals ?: return
    Surface(color = CardBg, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bilancio stagionale", color = Color.White, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                KpiCell("Massa", String.format(Locale.ITALY, "%.0f kg", totals.postMassKg))
                KpiCell("Volume", String.format(Locale.ITALY, "%.2f m³", totals.postVolumeM3))
                KpiCell("Riduzione", String.format(Locale.ITALY, "−%.0f%%", totals.massReductionPct))
            }

            val alerts = result.compliance?.alerts.orEmpty()
            if (alerts.isEmpty()) {
                Text("✓ Nessun superamento dei limiti di stoccaggio (art. 185-bis)", color = OkGreen, style = MaterialTheme.typography.bodySmall)
            } else {
                alerts.forEach { a ->
                    Text("⚠ ${a.message}", color = WarnAmber, style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(color = TsmColors.DashboardBorder)
            Text("Costi di evacuazione per vettore", color = Color.White, fontWeight = FontWeight.Bold)
            result.vectors.forEach { v ->
                val highlight = v.name == result.cheapestVector
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (highlight) "★ ${v.name}" else v.name,
                        color = if (highlight) OkGreen else Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        String.format(Locale.ITALY, "%d viaggi · %.0f € · %.2f €/kg", v.trips, v.totalCostEur, v.costPerKgEur),
                        color = if (highlight) OkGreen else TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            result.cheapestVector?.let {
                Text("Vettore consigliato: $it", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun KpiCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Cyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}
