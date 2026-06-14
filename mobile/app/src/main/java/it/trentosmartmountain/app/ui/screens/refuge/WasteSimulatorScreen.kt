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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.WasteSimulationResponse
import it.trentosmartmountain.app.ui.components.TsmAccentRule
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.ui.components.TsmGradientButton
import it.trentosmartmountain.app.ui.components.tsmNavigationBarPadding
import it.trentosmartmountain.app.ui.components.tsmShimmer
import it.trentosmartmountain.app.ui.components.tsmStatusBarPadding
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.WasteSimulatorViewModel
import java.util.Locale

private val Bg = TsmColors.DashboardBackground
private val Cyan = TsmColors.Info
private val WarnAmber = Color(0xFFFFB454)
private val OkGreen = TsmColors.Online
private val DangerRed = TsmColors.Offline
private val Gold = TsmColors.Gold
private val TextSecondary = TsmColors.TextSecondary

/**
 * Simulatore **Rifiuti & Logistica** del rifugio (ADR-002, MVP read-only):
 * bilancio di massa stagionale, alert di compliance (art. 185-bis D.Lgs.
 * 152/2006) e confronto dei costi di evacuazione per vettore. Il calcolo gira
 * sul backend (`POST /api/v1/refuge/waste/simulate`).
 *
 * Layout premium (S3-14): glass card del design system, sezioni con accent
 * rule, KPI colorati e vettore consigliato con highlight dorato + shimmer.
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
        // Stessa profondità "telemetria" della dashboard rifugio.
        it.trentosmartmountain.app.ui.components.TsmAuroraBackground(
            modifier = Modifier.fillMaxSize(),
            baseColor = Bg,
            particleCount = 12,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Edge-to-edge: insets per non finire sotto status/navigation bar.
                .tsmStatusBarPadding()
                .tsmNavigationBarPadding()
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
            TsmGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        icon = { Icon(Icons.Outlined.Groups, null, tint = Cyan, modifier = Modifier.size(18.dp)) },
                        title = "PARAMETRI STAGIONE",
                        accent = Cyan,
                    )
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

                    SectionHeader(
                        icon = { Icon(Icons.Outlined.Recycling, null, tint = OkGreen, modifier = Modifier.size(18.dp)) },
                        title = "PRODUZIONE RIFIUTI",
                        accent = OkGreen,
                    )
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
                        Icon(Icons.Outlined.Compress, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Pre-trattamento / compattatore",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.compactorEnabled,
                            onCheckedChange = { c -> viewModel.update { copy(compactorEnabled = c) } },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Cyan,
                                checkedThumbColor = Color.White,
                            ),
                        )
                    }
                    TsmGradientButton(
                        text = if (state.isLoading) "Calcolo in corso…" else "Simula stagione",
                        onClick = viewModel::simulate,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        fill = Brush.horizontalGradient(listOf(Cyan, TsmColors.Cyan)),
                        leading = {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                        },
                    )
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
private fun SectionHeader(
    icon: @Composable () -> Unit,
    title: String,
    accent: Color,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon()
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        TsmAccentRule(color = accent)
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
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Cyan,
            unfocusedBorderColor = TsmColors.DashboardBorder,
        ),
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
private fun WasteResults(result: WasteSimulationResponse) {
    val totals = result.totals ?: return
    TsmGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                icon = { Icon(Icons.Outlined.Recycling, null, tint = Cyan, modifier = Modifier.size(18.dp)) },
                title = "BILANCIO STAGIONALE",
                accent = Cyan,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                KpiCell("MASSA", String.format(Locale.ITALY, "%.0f kg", totals.postMassKg), Cyan)
                KpiCell("VOLUME", String.format(Locale.ITALY, "%.2f m³", totals.postVolumeM3), TsmColors.Peach)
                KpiCell("RIDUZIONE", String.format(Locale.ITALY, "−%.0f%%", totals.massReductionPct), OkGreen)
            }

            // ── Compliance (art. 185-bis) ──
            val alerts = result.compliance?.alerts.orEmpty()
            if (alerts.isEmpty()) {
                ComplianceRow(
                    icon = { Icon(Icons.Outlined.Verified, null, tint = OkGreen, modifier = Modifier.size(18.dp)) },
                    text = "Nessun superamento dei limiti di stoccaggio (art. 185-bis)",
                    color = OkGreen,
                )
            } else {
                alerts.forEach { a ->
                    ComplianceRow(
                        icon = { Icon(Icons.Outlined.WarningAmber, null, tint = WarnAmber, modifier = Modifier.size(18.dp)) },
                        text = a.message ?: "Limite di stoccaggio superato",
                        color = WarnAmber,
                    )
                }
            }

            SectionHeader(
                icon = { Icon(Icons.Filled.Star, null, tint = Gold, modifier = Modifier.size(18.dp)) },
                title = "COSTI EVACUAZIONE PER VETTORE",
                accent = Gold,
            )
            result.vectors.forEach { v ->
                val highlight = v.name == result.cheapestVector
                val rowModifier = if (highlight) {
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Gold.copy(alpha = 0.18f), Gold.copy(alpha = 0.05f)),
                            ),
                            RoundedCornerShape(10.dp),
                        )
                        .tsmShimmer(highlight = Gold)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                } else {
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
                }
                Row(
                    rowModifier,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (highlight) {
                            Icon(Icons.Filled.Star, null, tint = Gold, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            v.name ?: "—",
                            color = if (highlight) Gold else Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                    Text(
                        String.format(Locale.ITALY, "%d viaggi · %.0f € · %.2f €/kg", v.trips, v.totalCostEur, v.costPerKgEur),
                        color = if (highlight) Gold else TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            result.cheapestVector?.let {
                Text(
                    "Vettore consigliato: $it",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ComplianceRow(
    icon: @Composable () -> Unit,
    text: String,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon()
            Text(text, color = color, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun KpiCell(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = accent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
                .merge(it.trentosmartmountain.app.ui.theme.TsmType.Numeric),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.5.sp,
        )
    }
}
