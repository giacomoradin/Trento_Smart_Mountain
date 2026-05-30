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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val markers = remember(state.step, state.destinations, state.trailsForDestination, state.selectedDestination, state.selectedTrailDetail) {
        when (state.step) {
            Step.Destinations -> state.destinations.mapNotNull { d ->
                d.coordinate?.let {
                    SentieroMapMarker(d.nome, GeoPoint(it.lat, it.lon), d.nome, SentieroMarkerType.DESTINATION)
                }
            }
            Step.TrailsForDestination -> buildList {
                state.selectedDestination?.coordinate?.let {
                    add(SentieroMapMarker(state.selectedDestination!!.nome, GeoPoint(it.lat, it.lon), state.selectedDestination!!.nome, SentieroMarkerType.SELECTED_DESTINATION))
                }
                state.trailsForDestination.forEach { t ->
                    t.puntoInizio?.coordinate?.let {
                        add(SentieroMapMarker(t.codice, GeoPoint(it.lat, it.lon), "${t.codice} · ${t.puntoInizio?.nome ?: ""}", SentieroMarkerType.START))
                    }
                }
            }
            Step.TrailDetail -> buildList {
                val d = state.selectedTrailDetail
                d?.puntoInizio?.coordinate?.let {
                    add(SentieroMapMarker("start", GeoPoint(it.lat, it.lon), "Partenza", SentieroMarkerType.START))
                }
                d?.puntoFine?.coordinate?.let {
                    add(SentieroMapMarker("end", GeoPoint(it.lat, it.lon), "Arrivo", SentieroMarkerType.SELECTED_DESTINATION))
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
    if (state.destinations.isEmpty() && !state.isLoading) {
        Text("Nessuna destinazione disponibile.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(state.destinations, key = { it.nome }) { dest ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.onDestinationClick(dest) },
                shape = RoundedCornerShape(8.dp),
                color = TsmSurfaceVariant,
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dest.nome, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                        val quota = dest.quota?.let { "$it m" }
                        val info = listOfNotNull(quota, "${dest.numeroSentieri} sentieri").joinToString(" · ")
                        Text(info, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
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
