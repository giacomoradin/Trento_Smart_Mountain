package it.trentosmartmountain.app.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.SentieroDettaglioDto
import it.trentosmartmountain.app.data.remote.dto.SentieroListItemDto
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.ui.theme.difficultyColor
import it.trentosmartmountain.app.viewmodel.SessionRoutePickerViewModel
import it.trentosmartmountain.app.viewmodel.SessionRoutePickerViewModel.Step
import org.osmdroid.util.GeoPoint

/**
 * Popup **"Scegli percorso sulla mappa"** (modalità DB sentieri).
 * Flusso: destinazioni → sentieri per destinazione → dettaglio → conferma tracciato.
 *
 * Nessuna navigazione di screen: il dialog vive sopra "Pianifica" e alla conferma
 * restituisce il [SentieroDettaglioDto] selezionato via [onConfirm].
 */
@Composable
fun SessionRoutePickerDialog(
    onConfirm: (SentieroDettaglioDto) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SessionRoutePickerViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onOpen() }
    // Alla chiusura del dialog riparti dallo step destinazioni (mantenendo la cache).
    DisposableEffect(Unit) { onDispose { viewModel.reset() } }

    // Marker e polyline derivati dallo step corrente.
    val markers = remember(state.step, state.destinations, state.searchQuery, state.trailsForDestination, state.selectedDestination, state.selectedTrailDetail) {
        when (state.step) {
            Step.Destinations -> state.visibleDestinations.mapNotNull { d ->
                d.coordinate?.let {
                    SentieroMapMarker(d.nome, GeoPoint(it.lat, it.lon), d.nome, SentieroMarkerType.DESTINATION)
                }
            }
            Step.TrailsForDestination -> buildList {
                state.selectedDestination?.coordinate?.let {
                    add(SentieroMapMarker(state.selectedDestination!!.nome, GeoPoint(it.lat, it.lon), state.selectedDestination!!.nome, SentieroMarkerType.SELECTED_DESTINATION))
                }
                state.trailsForDestination.forEach { t ->
                    t.puntoInizio?.let { p ->
                        p.coordinate?.let { c ->
                            add(SentieroMapMarker(t.codice, GeoPoint(c.lat, c.lon), pointLabel(t.codice, p.nome, p.quota), SentieroMarkerType.START))
                        }
                    }
                }
            }
            Step.TrailDetail -> buildList {
                val d = state.selectedTrailDetail
                d?.puntoInizio?.let { p ->
                    p.coordinate?.let { c ->
                        add(SentieroMapMarker("start", GeoPoint(c.lat, c.lon), pointLabel("Partenza", p.nome, p.quota), SentieroMarkerType.START))
                    }
                }
                d?.puntoFine?.let { p ->
                    p.coordinate?.let { c ->
                        add(SentieroMapMarker("end", GeoPoint(c.lat, c.lon), pointLabel("Arrivo", p.nome, p.quota), SentieroMarkerType.SELECTED_DESTINATION))
                    }
                }
            }
        }
    }
    val polyline = if (state.step == Step.TrailDetail) state.selectedTrailPolyline else emptyList()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = TsmSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxSize(0.92f),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Header con back + titolo + chiudi
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (state.step != Step.Destinations) {
                        IconButton(onClick = viewModel::onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                        }
                    }
                    Text(
                        text = when (state.step) {
                            Step.Destinations -> "Scegli una destinazione"
                            Step.TrailsForDestination -> state.selectedDestination?.nome ?: "Sentieri"
                            Step.TrailDetail -> state.selectedTrailDetail?.codice ?: "Dettaglio"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f).padding(start = if (state.step == Step.Destinations) 8.dp else 0.dp),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Chiudi", tint = Color.Gray)
                    }
                }

                // Mappa
                Surface(shape = RoundedCornerShape(12.dp), color = TsmBackground, modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TsmSentieriMapView(
                            modifier = Modifier.fillMaxSize(),
                            markers = markers,
                            polyline = polyline,
                            onMarkerClick = { id ->
                                when (state.step) {
                                    Step.Destinations ->
                                        state.destinations.firstOrNull { it.nome == id }?.let(viewModel::onDestinationClick)
                                    Step.TrailsForDestination -> viewModel.onTrailClick(id)
                                    Step.TrailDetail -> Unit
                                }
                            },
                        )
                        if (state.isLoading) {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = TsmAccent)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                    if (state.step == Step.Destinations && state.destinations.isEmpty()) {
                        TextButton(onClick = viewModel::loadDestinations) {
                            Text("Riprova", color = TsmAccent, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Pannello contestuale per step
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (state.step) {
                        Step.Destinations -> DestinationsPanel(state, viewModel)
                        Step.TrailsForDestination -> TrailsPanel(state, viewModel)
                        Step.TrailDetail -> TrailDetailPanel(state.selectedTrailDetail, onConfirm)
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationsPanel(
    state: SessionRoutePickerViewModel.UiState,
    viewModel: SessionRoutePickerViewModel,
) {
    var filtersExpanded by remember { mutableStateOf(false) }
    val visible = state.visibleDestinations

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra di ricerca destinazioni (filtro client-side sul nome).
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Cerca destinazione", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancella", tint = Color.Gray)
                    }
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = TsmAccent,
                unfocusedBorderColor = TsmSurfaceVariant,
                cursorColor = TsmAccent,
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Riga "Filtri" con badge conteggio + reset.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { filtersExpanded = !filtersExpanded }) {
                Icon(Icons.Outlined.FilterList, contentDescription = null, tint = TsmAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                val label = if (state.filter.isActive) "Filtri (${state.filter.activeCount})" else "Filtri"
                Text(label, color = if (state.filter.isActive) TsmAccent else Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.weight(1f))
            if (state.filter.isActive) {
                TextButton(onClick = viewModel::clearFilter) {
                    Text("Azzera", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (filtersExpanded) {
            FilterSection(filter = state.filter, onChange = viewModel::applyFilter)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (visible.isEmpty() && !state.isLoading) {
            val msg = when {
                state.searchQuery.isNotBlank() -> "Nessuna destinazione per \"${state.searchQuery}\"."
                state.filter.isActive -> "Nessuna destinazione soddisfa i filtri."
                else -> "Nessuna destinazione disponibile."
            }
            Text(msg, color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(visible, key = { it.nome }) { dest ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.onDestinationClick(dest) },
                        shape = RoundedCornerShape(8.dp),
                        color = TsmSurfaceVariant,
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dest.nome, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                val quota = dest.quota?.let { "$it m" }
                                val sentieriLabel = if (state.filter.isActive) "${dest.numeroSentieri} sentieri compatibili" else "${dest.numeroSentieri} sentieri"
                                val info = listOfNotNull(quota, sentieriLabel).joinToString(" · ")
                                Text(info, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Difficoltà (chip multi-select) + soglie massime (slider) per dislivello, distanza, tempo. */
@Composable
private fun FilterSection(
    filter: SessionRoutePickerViewModel.RouteFilter,
    onChange: (SessionRoutePickerViewModel.RouteFilter) -> Unit,
) {
    Surface(shape = RoundedCornerShape(10.dp), color = TsmSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Difficoltà", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("T", "E", "EE", "EEA").forEach { level ->
                    val selected = level in filter.difficolta
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = if (selected) filter.difficolta - level else filter.difficolta + level
                            onChange(filter.copy(difficolta = next))
                        },
                        label = { Text(level, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = difficultyColor(level).copy(alpha = 0.25f),
                            selectedLabelColor = difficultyColor(level),
                            labelColor = Color.Gray,
                            containerColor = TsmSurface,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            FilterSlider(
                label = "Dislivello max",
                value = filter.dislivelloMax,
                range = 0f..2500f,
                step = 50,
                noLimitAt = 2500,
                formatValue = { "$it m" },
                onCommit = { onChange(filter.copy(dislivelloMax = it)) },
            )

            Spacer(modifier = Modifier.height(12.dp))
            FilterSlider(
                label = "Distanza max",
                value = filter.distanzaMaxKm,
                range = 0f..30f,
                step = 1,
                noLimitAt = 30,
                formatValue = { "$it km" },
                onCommit = { onChange(filter.copy(distanzaMaxKm = it)) },
            )

            Spacer(modifier = Modifier.height(12.dp))
            FilterSlider(
                label = "Tempo max (andata)",
                value = filter.tempoMaxMin,
                range = 0f..600f,
                step = 15,
                noLimitAt = 600,
                formatValue = { formatMinutes(it) },
                onCommit = { onChange(filter.copy(tempoMaxMin = it)) },
            )
        }
    }
}

/**
 * Slider per una soglia massima. Il valore [noLimitAt] (estremo destro) equivale a
 * "nessun limite" → emette `null` su [onCommit]. Mostra il valore live durante il drag.
 */
@Composable
private fun FilterSlider(
    label: String,
    value: Int?,
    range: ClosedFloatingPointRange<Float>,
    step: Int,
    noLimitAt: Int,
    formatValue: (Int) -> String,
    onCommit: (Int?) -> Unit,
) {
    var sliderValue by remember(value) { mutableStateOf((value ?: noLimitAt).toFloat()) }
    val rounded = (Math.round(sliderValue / step) * step).coerceIn(range.start.toInt(), range.endInclusive.toInt())

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            if (rounded >= noLimitAt) "Nessun limite" else formatValue(rounded),
            color = if (rounded >= noLimitAt) Color.Gray else TsmAccent,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
    Slider(
        value = sliderValue,
        onValueChange = { sliderValue = it },
        valueRange = range,
        onValueChangeFinished = {
            val v = (Math.round(sliderValue / step) * step).coerceIn(range.start.toInt(), range.endInclusive.toInt())
            onCommit(if (v >= noLimitAt) null else v)
        },
        colors = SliderDefaults.colors(
            thumbColor = TsmAccent,
            activeTrackColor = TsmAccent,
            inactiveTrackColor = TsmSurfaceVariant,
        ),
    )
}

/** Etichetta marker: "Ruolo: Nome · quota m" (parti mancanti omesse). */
private fun pointLabel(role: String, nome: String?, quota: Int?): String {
    val tail = listOfNotNull(
        nome?.takeIf { it.isNotBlank() },
        quota?.let { "$it m" },
    ).joinToString(" · ")
    return if (tail.isEmpty()) role else "$role · $tail"
}

/** Minuti → "Xh YY" / "YY min". */
private fun formatMinutes(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m}min"
    }
}

@Composable
private fun TrailsPanel(
    state: SessionRoutePickerViewModel.UiState,
    viewModel: SessionRoutePickerViewModel,
) {
    if (state.trailsForDestination.isEmpty() && !state.isLoading) {
        Text("Nessun sentiero per questa destinazione.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(state.trailsForDestination, key = { it.codice }) { trail ->
            TrailRow(trail) { viewModel.onTrailClick(trail.codice) }
        }
    }
}

@Composable
private fun TrailRow(trail: SentieroListItemDto, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = TsmSurfaceVariant,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(6.dp), color = TsmAccent.copy(alpha = 0.15f)) {
                Text(
                    trail.difficolta ?: "?",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TsmAccent,
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${trail.codice}${trail.denominazione?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                val dist = trail.lunghezzaPlanimetrica?.let { "%.1f km".format(it / 1000.0) }
                val tempo = trail.tempoAndata?.let { "salita $it" }
                val from = trail.puntoInizio?.nome?.let { "da $it" }
                Text(
                    listOfNotNull(dist, tempo, from).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun TrailDetailPanel(
    detail: SentieroDettaglioDto?,
    onConfirm: (SentieroDettaglioDto) -> Unit,
) {
    if (detail == null) return
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            detail.denominazione?.takeIf { it.isNotBlank() } ?: detail.codice,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoChip("Codice", detail.codice)
            InfoChip("Difficoltà", detail.difficolta ?: "—")
            detail.lunghezzaPlanimetrica?.let { InfoChip("Distanza", "%.1f km".format(it / 1000.0)) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            detail.tempoAndata?.let { InfoChip("Salita", it) }
            detail.tempoRitorno?.let { InfoChip("Discesa", it) }
            if (detail.quotaMassima != null && detail.quotaMinima != null) {
                InfoChip("Dislivello", "+%d m".format((detail.quotaMassima - detail.quotaMinima).coerceAtLeast(0)))
            }
        }
        detail.puntoInizio?.nome?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Partenza: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        detail.puntoFine?.nome?.let {
            Text("Arrivo: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onConfirm(detail) },
            colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Conferma tracciato", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
