package it.trentosmartmountain.app.ui.screens.session

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.SentieroDettaglioDto
import it.trentosmartmountain.app.data.remote.dto.SentieroListItemDto
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.ui.components.TsmGradientButton
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.ui.theme.difficultyColor
import it.trentosmartmountain.app.viewmodel.SessionRoutePickerViewModel
import it.trentosmartmountain.app.viewmodel.SessionRoutePickerViewModel.Step
import org.osmdroid.util.GeoPoint

/**
 * Popup **"Scegli tra i percorsi suggeriti"** (modalità DB sentieri).
 *
 * Design **list-first** (rework 2026-06): la lista dei percorsi è la protagonista.
 * Niente mappa con "muro di pin" negli step di navigazione — la mappa compare
 * SOLO sul dettaglio del sentiero scelto, dove mostra la traccia reale.
 *
 * Flusso: destinazioni → sentieri della destinazione → dettaglio + conferma.
 * Alla conferma restituisce il [SentieroDettaglioDto] selezionato via [onConfirm].
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = TsmBackground,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxSize(0.92f),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                // ── Header: back + titolo/sottotitolo-conteggio + chiudi ──
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (state.step != Step.Destinations) {
                        IconButton(onClick = viewModel::onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = if (state.step == Step.Destinations) 4.dp else 0.dp)) {
                        Text(
                            text = when (state.step) {
                                Step.Destinations -> "Percorsi suggeriti"
                                Step.TrailsForDestination -> state.selectedDestination?.nome ?: "Sentieri"
                                Step.TrailDetail -> state.selectedTrailDetail?.let { it.denominazione?.takeIf { d -> d.isNotBlank() } ?: it.codice } ?: "Dettaglio"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                        )
                        val subtitle = when (state.step) {
                            Step.Destinations -> state.visibleDestinations.size.takeIf { it > 0 }?.let { "$it destinazioni" }
                            Step.TrailsForDestination -> state.trailsForDestination.size.takeIf { it > 0 }?.let { "$it sentieri" }
                            Step.TrailDetail -> state.selectedTrailDetail?.codice
                        }
                        if (subtitle != null && !state.isLoading) {
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TsmAccent, maxLines = 1)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Chiudi", tint = TsmColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Corpo per step ──
                when (state.step) {
                    Step.Destinations -> DestinationsStep(state, viewModel, modifier = Modifier.weight(1f))
                    Step.TrailsForDestination -> TrailsStep(state, viewModel, modifier = Modifier.weight(1f))
                    Step.TrailDetail -> TrailDetailStep(state, onConfirm, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/* ───────────────────────────── Step 1: destinazioni ───────────────────────────── */

@Composable
private fun DestinationsStep(
    state: SessionRoutePickerViewModel.UiState,
    viewModel: SessionRoutePickerViewModel,
    modifier: Modifier = Modifier,
) {
    var filtersExpanded by remember { mutableStateOf(false) }
    val visible = state.visibleDestinations

    Column(modifier = modifier.fillMaxWidth()) {
        // Barra di ricerca (filtro client-side sul nome).
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Cerca una meta (rifugio, malga, cima…)", color = TsmColors.TextTertiary) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TsmColors.TextSecondary) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancella", tint = TsmColors.TextSecondary)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = TsmAccent,
                unfocusedBorderColor = TsmSurfaceVariant,
                cursorColor = TsmAccent,
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                    Text("Azzera", color = TsmColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (filtersExpanded) {
            FilterSection(filter = state.filter, onChange = viewModel::applyFilter)
            Spacer(modifier = Modifier.height(8.dp))
        }

        state.error?.let {
            ErrorRow(it, onRetry = if (state.destinations.isEmpty()) viewModel::loadDestinations else null)
        }

        when {
            state.isLoading && state.destinations.isEmpty() -> LoadingBox()
            visible.isEmpty() -> {
                val msg = when {
                    state.searchQuery.isNotBlank() -> "Nessuna meta per \"${state.searchQuery}\"."
                    state.filter.isActive -> "Nessuna meta soddisfa i filtri."
                    else -> "Nessun percorso disponibile."
                }
                EmptyBox(msg)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            ) {
                items(visible, key = { it.nome }) { dest ->
                    DestinationCard(
                        nome = dest.nome,
                        quota = dest.quota,
                        sentieriLabel = if (state.filter.isActive) "${dest.numeroSentieri} compatibili" else "${dest.numeroSentieri} sentieri",
                        onClick = { viewModel.onDestinationClick(dest) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    nome: String,
    quota: Int?,
    sentieriLabel: String,
    onClick: () -> Unit,
) {
    TsmGlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(TsmAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = TsmAccent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nome, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Color.White, maxLines = 2)
                val info = listOfNotNull(quota?.let { "$it m" }, sentieriLabel).joinToString(" · ")
                Text(info, style = MaterialTheme.typography.bodySmall, color = TsmColors.TextSecondary)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = TsmColors.TextTertiary, modifier = Modifier.size(22.dp))
        }
    }
}

/* ───────────────────────────── Step 2: sentieri della meta ───────────────────────────── */

@Composable
private fun TrailsStep(
    state: SessionRoutePickerViewModel.UiState,
    viewModel: SessionRoutePickerViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        state.error?.let { ErrorRow(it, onRetry = null) }
        when {
            state.isLoading && state.trailsForDestination.isEmpty() -> LoadingBox()
            state.trailsForDestination.isEmpty() -> EmptyBox("Nessun sentiero per questa meta.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            ) {
                items(state.trailsForDestination, key = { it.codice }) { trail ->
                    TrailCard(trail) { viewModel.onTrailClick(trail.codice) }
                }
            }
        }
    }
}

@Composable
private fun TrailCard(trail: SentieroListItemDto, onClick: () -> Unit) {
    TsmGlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            DifficultyBadge(trail.difficolta)
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${trail.codice}${trail.denominazione?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 2,
                )
                val dist = trail.lunghezzaPlanimetrica?.let { "%.1f km".format(it / 1000.0) }
                val tempo = trail.tempoAndata?.let { "salita $it" }
                val from = trail.puntoInizio?.nome?.let { "da $it" }
                Text(
                    listOfNotNull(dist, tempo, from).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TsmColors.TextSecondary,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = TsmColors.TextTertiary, modifier = Modifier.size(22.dp))
        }
    }
}

/** Badge difficoltà CAI colorato (T/E/EE/EEA). */
@Composable
private fun DifficultyBadge(level: String?) {
    val text = level?.takeIf { it.isNotBlank() } ?: "?"
    val color = difficultyColor(text)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

/* ───────────────────────────── Step 3: dettaglio + mappa traccia ───────────────────────────── */

@Composable
private fun TrailDetailStep(
    state: SessionRoutePickerViewModel.UiState,
    onConfirm: (SentieroDettaglioDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.selectedTrailDetail
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Mappa con la traccia reale del sentiero scelto (start/end + polyline).
        val markers = remember(detail) {
            buildList {
                detail?.puntoInizio?.let { p ->
                    p.coordinate?.let { c ->
                        add(SentieroMapMarker("start", GeoPoint(c.lat, c.lon), pointLabel("Partenza", p.nome, p.quota), SentieroMarkerType.START))
                    }
                }
                detail?.puntoFine?.let { p ->
                    p.coordinate?.let { c ->
                        add(SentieroMapMarker("end", GeoPoint(c.lat, c.lon), pointLabel("Arrivo", p.nome, p.quota), SentieroMarkerType.SELECTED_DESTINATION))
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(14.dp), color = TsmColors.Card, modifier = Modifier.fillMaxWidth().height(240.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                TsmSentieriMapView(
                    modifier = Modifier.fillMaxSize(),
                    markers = markers,
                    polyline = state.selectedTrailPolyline,
                )
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TsmAccent)
                    }
                }
            }
        }

        if (detail == null) return@Column

        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip("Difficoltà", detail.difficolta ?: "—")
            detail.lunghezzaPlanimetrica?.let { InfoChip("Distanza", "%.1f km".format(it / 1000.0)) }
            if (detail.quotaMassima != null && detail.quotaMinima != null) {
                InfoChip("Dislivello", "+%d m".format((detail.quotaMassima - detail.quotaMinima).coerceAtLeast(0)))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            detail.tempoAndata?.let { InfoChip("Salita", it) }
            detail.tempoRitorno?.let { InfoChip("Discesa", it) }
            detail.quotaMassima?.let { InfoChip("Quota max", "$it m") }
        }

        val partenza = detail.puntoInizio?.nome
        val arrivo = detail.puntoFine?.nome
        if (!partenza.isNullOrBlank() || !arrivo.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            partenza?.takeIf { it.isNotBlank() }?.let {
                Text("Partenza: $it", style = MaterialTheme.typography.bodySmall, color = TsmColors.TextSecondary)
            }
            arrivo?.takeIf { it.isNotBlank() }?.let {
                Text("Arrivo: $it", style = MaterialTheme.typography.bodySmall, color = TsmColors.TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        TsmGradientButton(
            text = "Usa questo tracciato",
            onClick = { onConfirm(detail) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/* ───────────────────────────── Filtri (riusati) ───────────────────────────── */

/** Difficoltà (chip multi-select) + soglie massime (slider) per dislivello, distanza, tempo. */
@Composable
private fun FilterSection(
    filter: SessionRoutePickerViewModel.RouteFilter,
    onChange: (SessionRoutePickerViewModel.RouteFilter) -> Unit,
) {
    Surface(shape = RoundedCornerShape(12.dp), color = TsmColors.CardElevated, modifier = Modifier.fillMaxWidth()) {
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
                            labelColor = TsmColors.TextSecondary,
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
            color = if (rounded >= noLimitAt) TsmColors.TextSecondary else TsmAccent,
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

/* ───────────────────────────── Stati comuni ───────────────────────────── */

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TsmAccent)
    }
}

@Composable
private fun EmptyBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = TsmColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorRow(message: String, onRetry: (() -> Unit)?) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text("Riprova", color = TsmAccent, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

/* ───────────────────────────── Helper ───────────────────────────── */

/** Etichetta marker: "Ruolo: Nome · quota m" (parti mancanti omesse). */
private fun pointLabel(role: String, nome: String?, quota: Int?): String {
    val tail = listOfNotNull(
        nome?.takeIf { it.isNotBlank() },
        quota?.let { "$it m" },
    ).joinToString(" · ")
    return if (tail.isEmpty()) role else "$role · $tail"
}

/** Minuti → "Xh YYmin" / "YYmin". */
private fun formatMinutes(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m}min"
    }
}
