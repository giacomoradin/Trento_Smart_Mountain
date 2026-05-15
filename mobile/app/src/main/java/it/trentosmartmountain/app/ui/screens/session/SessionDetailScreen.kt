package it.trentosmartmountain.app.ui.screens.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import it.trentosmartmountain.app.data.session.SessionStartCoordinator
import it.trentosmartmountain.app.ui.screens.auth.TsmMountainLogo
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmBorder
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSos
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.viewmodel.SessionDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    onAvviaConfirmed: (sessionId: String) -> Unit = {},
    currentUserId: String = "",
    viewModel: SessionDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayFormatted = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(Date())
    }

    LaunchedEffect(sessionId) { viewModel.loadSession(sessionId) }

    // AVVIA confirmation dialog (when not same day)
    if (uiState.showAvviaConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAvviaConfirm,
            containerColor = TsmSurface,
            title = { Text("Avviare in anticipo?", color = Color.White) },
            text = {
                Text(
                    "La sessione è pianificata per ${uiState.session?.meetingDate ?: "un altro giorno"}. Vuoi avviarla ugualmente?",
                    color = Color.Gray,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissAvviaConfirm()
                        // Segnaliamo via Coordinator: HikerMainScreen switcha alla tab Registra,
                        // RegistraViewModel auto-avvia il tracking.
                        SessionStartCoordinator.requestStart(sessionId)
                        onAvviaConfirmed(sessionId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
                ) { Text("Avvia") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAvviaConfirm) { Text("Annulla", color = Color.Gray) }
            },
        )
    }

    // Edit mode date/time pickers
    var showEditDatePicker by remember { mutableStateOf(false) }
    var showEditTimePicker by remember { mutableStateOf(false) }
    var showDifficultyMenu by remember { mutableStateOf(false) }

    if (showEditDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEditDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        viewModel.onEditDateChange(SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(Date(millis)))
                    }
                    showEditDatePicker = false
                }) { Text("OK", color = TsmAccent) }
            },
            dismissButton = { TextButton(onClick = { showEditDatePicker = false }) { Text("Annulla", color = Color.Gray) } },
        ) { DatePicker(state = dateState) }
    }

    if (showEditTimePicker) {
        val timeState = rememberTimePickerState(initialHour = 6, initialMinute = 30)
        Dialog(onDismissRequest = { showEditTimePicker = false }) {
            Surface(color = TsmSurface, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ora di ritrovo", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timeState)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showEditTimePicker = false }) { Text("Annulla", color = Color.Gray) }
                        TextButton(onClick = {
                            viewModel.onEditTimeChange("%02d:%02d".format(timeState.hour, timeState.minute))
                            showEditTimePicker = false
                        }) { Text("OK", color = TsmAccent) }
                    }
                }
            }
        }
    }

    val session = uiState.session
    val isCreator = session?.creatorId?._id == currentUserId || currentUserId.isBlank()
    val isToday = session?.meetingDate == todayFormatted

    Scaffold(
        containerColor = TsmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                session?.routeDetails?.name ?: "Dettagli sessione",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            if (isToday) {
                                Surface(color = TsmPrimary, shape = RoundedCornerShape(4.dp)) {
                                    Text("OGGI", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }
                        }
                        val subtitle = buildString {
                            session?.meetingDate?.let { append(it) }
                            session?.meetingTime?.let { append(" · $it") }
                            session?.creatorId?.username?.let { append(" · host $it") }
                        }
                        if (subtitle.isNotBlank()) {
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                actions = {
                    if (isCreator) {
                        IconButton(onClick = {
                            if (uiState.editMode) viewModel.saveEdit() else viewModel.enterEditMode()
                        }) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TsmAccent)
                            } else {
                                Icon(
                                    if (uiState.editMode) Icons.Filled.CheckCircle else Icons.Outlined.Edit,
                                    contentDescription = if (uiState.editMode) "Salva" else "Modifica",
                                    tint = if (uiState.editMode) TsmPrimary else Color.White,
                                )
                            }
                        }
                        if (uiState.editMode) {
                            IconButton(onClick = viewModel::exitEditMode) {
                                Icon(Icons.Outlined.Close, contentDescription = "Annulla modifica", tint = Color.Gray)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TsmBackground),
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TsmAccent)
            }
            return@Scaffold
        }

        if (session == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Sessione non trovata", color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Edit mode fields
            if (uiState.editMode) {
                EditModeCard(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDateClick = { showEditDatePicker = true },
                    onTimeClick = { showEditTimePicker = true },
                    showDifficultyMenu = showDifficultyMenu,
                    onDifficultyMenuToggle = { showDifficultyMenu = !showDifficultyMenu },
                    onDifficultySelect = { d -> viewModel.onEditDifficultyChange(d); showDifficultyMenu = false },
                )
            }

            // ── CONDIVISIONE (codice invito sempre visibile) ──
            InviteCodeCard(inviteCode = session.inviteCode)

            // ── ELEVATION PROFILE + STATS ──
            DetailCard {
                ElevationProfileChart(
                    distanceKm = session.gpxStats?.distanceKm ?: 0.0,
                    elevationGainM = session.gpxStats?.elevationGainM ?: 0,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    val dist = session.gpxStats?.distanceKm
                    val elev = session.gpxStats?.elevationGainM
                    val estHours = if (dist != null && elev != null)
                        "~%.0fh".format(dist / 2.5 + elev / 400.0) else "—"
                    StatCell("DISTANZA", dist?.let { "%.1f km".format(it) } ?: "—", TsmBorder, Modifier.weight(1f))
                    StatCell("DISLIVELLO", elev?.let { "+$it m" } ?: "—", TsmAccent, Modifier.weight(1f))
                    StatCell("DURATA", estHours, TsmPrimary, Modifier.weight(1f))
                }
            }

            // ── METEO (dati reali da MeteoTrentino via backend di Marco) ──
            MeteoCard(
                meetingDate = session.meetingDate ?: "—",
                uiState = uiState,
                onRefresh = viewModel::refreshMeteo,
            )

            // ── CHECKLIST ──
            ChecklistCard(uiState = uiState, viewModel = viewModel)

            // ── PARTECIPANTI ──
            /*
             * PARTECIPANTI — Profili completi in sviluppo (HOME SOCIAL)
             * I profili completi (avatar foto, social credits, badge) saranno disponibili
             * quando HomeScreen Social sarà implementata. Per ora: avatar con iniziali + colore.
             */
            ParticipantsCard(session = session)

            // ── AVVIA ──
            Button(
                onClick = {
                    viewModel.onAvviaClick(todayFormatted) {
                        SessionStartCoordinator.requestStart(sessionId)
                        onAvviaConfirmed(sessionId)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("▶ AVVIA ESCURSIONE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Edit mode card ──

@Composable
private fun EditModeCard(
    uiState: SessionDetailViewModel.UiState,
    viewModel: SessionDetailViewModel,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    showDifficultyMenu: Boolean,
    onDifficultyMenuToggle: () -> Unit,
    onDifficultySelect: (String) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = TsmSurface) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Modifica sessione", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = TsmAccent)
            OutlinedTextField(
                value = uiState.editName, onValueChange = viewModel::onEditNameChange,
                label = { Text("NOME", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(),
                colors = detailFieldColors(), shape = RoundedCornerShape(8.dp), singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.editDate, onValueChange = {},
                    label = { Text("DATA", color = Color.Gray) },
                    modifier = Modifier.weight(1f).clickable { onDateClick() }, readOnly = true, enabled = false,
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = TsmAccent) },
                    colors = detailFieldColors(), shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = uiState.editTime, onValueChange = {},
                    label = { Text("ORA", color = Color.Gray) },
                    modifier = Modifier.weight(1f).clickable { onTimeClick() }, readOnly = true, enabled = false,
                    leadingIcon = { Icon(Icons.Outlined.Schedule, null, tint = TsmAccent) },
                    colors = detailFieldColors(), shape = RoundedCornerShape(8.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MAX: ", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { viewModel.onEditMaxParticipantsChange(uiState.editMaxParticipants - 1) }, modifier = Modifier.size(32.dp).background(TsmSurfaceVariant, CircleShape)) {
                    Text("−", color = Color.White)
                }
                Text("${uiState.editMaxParticipants}", color = Color.White, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                IconButton(onClick = { viewModel.onEditMaxParticipantsChange(uiState.editMaxParticipants + 1) }, modifier = Modifier.size(32.dp).background(TsmSurfaceVariant, CircleShape)) {
                    Text("+", color = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    OutlinedTextField(
                        value = uiState.editDifficulty, onValueChange = {},
                        label = { Text("DIFF.", color = Color.Gray) },
                        modifier = Modifier.width(100.dp).clickable { onDifficultyMenuToggle() }, readOnly = true, enabled = false,
                        colors = detailFieldColors(), shape = RoundedCornerShape(8.dp),
                    )
                    DropdownMenu(expanded = showDifficultyMenu, onDismissRequest = { onDifficultyMenuToggle() }, modifier = Modifier.background(TsmSurface)) {
                        listOf("T", "E", "EE", "EEA").forEach { d ->
                            DropdownMenuItem(text = { Text(d, color = Color.White) }, onClick = { onDifficultySelect(d) })
                        }
                    }
                }
            }
        }
    }
}

// ── Elevation profile chart ──

@Composable
private fun ElevationProfileChart(distanceKm: Double, elevationGainM: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Faint terrain background layers
        for (i in 0..2) {
            val terrainPath = Path().apply {
                val base = h * (0.6f + i * 0.12f)
                moveTo(0f, h)
                cubicTo(w * 0.2f, base - 15f, w * 0.5f, base + 25f, w * 0.75f, base - 10f)
                cubicTo(w * 0.85f, base - 5f, w * 0.92f, base + 15f, w, base)
                lineTo(w, h)
                close()
            }
            drawPath(terrainPath, color = Color(0xFF2A2A2A).copy(alpha = 0.6f + i * 0.1f))
        }

        // Elevation profile dashed line
        val profilePath = Path().apply {
            moveTo(w * 0.05f, h * 0.72f)
            cubicTo(
                w * 0.25f, h * 0.68f,
                w * 0.55f, h * 0.18f,
                w * 0.95f, h * 0.30f,
            )
        }
        drawPath(
            profilePath,
            color = TsmAccent,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 9f), 0f),
            ),
        )

        // Start marker (green circle)
        val startCenter = Offset(w * 0.05f, h * 0.72f)
        drawCircle(color = Color(0xFF1B5E20), radius = 10.dp.toPx(), center = startCenter)
        drawCircle(color = TsmPrimary, radius = 7.dp.toPx(), center = startCenter)

        // End marker (red/SOS circle)
        val endCenter = Offset(w * 0.95f, h * 0.30f)
        drawCircle(color = Color(0xFF880E4F).copy(alpha = 0.4f), radius = 13.dp.toPx(), center = endCenter)
        drawCircle(color = TsmSos, radius = 9.dp.toPx(), center = endCenter)
    }
}

// ── Stats row cell ──

@Composable
private fun StatCell(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = valueColor, textAlign = TextAlign.Center)
    }
}

// ── Meteo card (dati reali da MeteoTrentino via backend di Marco) ──

@Composable
private fun MeteoCard(
    meetingDate: String,
    uiState: SessionDetailViewModel.UiState,
    onRefresh: () -> Unit,
) {
    val station = uiState.meteoStation
    val latestTemp = station?.air_temperature?.lastOrNull()
    val stationName = station?.stationInfo?.name ?: station?.stationCode ?: "—"
    val elevation = station?.stationInfo?.elevation?.let { "${it} m" } ?: ""
    val updatedAgo = uiState.meteoLastUpdate?.let { ts ->
        val diffMs = System.currentTimeMillis() - ts
        when {
            diffMs < 60_000L -> "ora"
            diffMs < 3_600_000L -> "${diffMs / 60_000L} min fa"
            else -> "${diffMs / 3_600_000L} h fa"
        }
    } ?: "—"

    DetailCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "METEO · ${meetingDate.uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = Color.Gray,
            )
            Surface(
                color = TsmSurfaceVariant,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable(enabled = !uiState.meteoLoading) { onRefresh() },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (uiState.meteoLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = TsmAccent)
                    } else {
                        Icon(Icons.Outlined.Schedule, null, tint = TsmAccent, modifier = Modifier.size(12.dp))
                    }
                    Text(
                        if (uiState.meteoLoading) "Aggiornamento..." else "Aggiornato $updatedAgo",
                        style = MaterialTheme.typography.labelSmall,
                        color = TsmAccent,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.meteoError != null -> {
                Text(
                    text = uiState.meteoError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onRefresh) {
                    Text("Riprova", color = TsmAccent, style = MaterialTheme.typography.labelMedium)
                }
            }
            latestTemp == null -> {
                Text(
                    "Nessun dato meteo disponibile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Icona dinamica in base alla temperatura (placeholder finché Marco non aggiunge condition icons)
                    val icon = when {
                        (latestTemp.value ?: 0.0) < 0 -> "❄️"
                        (latestTemp.value ?: 0.0) < 10 -> "🌤"
                        (latestTemp.value ?: 0.0) < 20 -> "☀️"
                        else -> "🔥"
                    }
                    Text(icon, fontSize = 36.sp)
                    Column {
                        Text(
                            text = "${latestTemp.value?.let { "%.1f".format(it) } ?: "—"}${latestTemp.UM ?: "°C"}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = "Stazione: $stationName${if (elevation.isNotBlank()) " · $elevation" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        latestTemp.date?.let {
                            Text(
                                text = "Rilevazione: $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                            )
                        }
                    }
                }
            }
        }

        // NOTA: hourly forecast + previsioni multi-giorno saranno disponibili
        // quando Marco aggiungerà l'endpoint forecast.
        // Per ora il backend espone solo l'ultima temperatura per stazione.
    }
}

// ── Checklist card (drag-and-drop con sh.calvin.reorderable) ──

@Composable
private fun ChecklistCard(
    uiState: SessionDetailViewModel.UiState,
    viewModel: SessionDetailViewModel,
) {
    val checkedCount = uiState.checklist.count { it.checked }
    val total = uiState.checklist.size

    DetailCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CHECKLIST", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$checkedCount / $total", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TsmAccent)
                IconButton(
                    onClick = { if (uiState.newItemText.isNotBlank()) viewModel.onAddItem() },
                    modifier = Modifier.size(28.dp).background(TsmPrimary, RoundedCornerShape(6.dp)),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Aggiungi", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ReorderableColumn: long-press sul drag handle per spostare gli item
        sh.calvin.reorderable.ReorderableColumn(
            list = uiState.checklist,
            onSettle = { from, to -> viewModel.onChecklistMove(from, to) },
        ) { _, item, _ ->
            // Key per item: necessario per Compose recomposition stabile durante il drag.
            key(item.id) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Drag handle: long-press qui attiva il drag-and-drop dell'item
                    IconButton(
                        modifier = Modifier
                            .size(28.dp)
                            .draggableHandle(),
                        onClick = {},
                    ) {
                        Icon(
                            Icons.Outlined.DragHandle,
                            contentDescription = "Trascina per riordinare",
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { viewModel.onToggleCheck(item.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TsmPrimary,
                            uncheckedColor = Color(0xFF555555),
                            checkmarkColor = Color.White,
                        ),
                    )
                    Text(
                        item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.checked) Color.Gray else Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.onRemoveItem(item.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Rimuovi", tint = Color(0xFF888888), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Add new item input
        OutlinedTextField(
            value = uiState.newItemText,
            onValueChange = viewModel::onNewItemTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Aggiungi elemento personalizzato…", color = Color(0xFF555555), style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TsmAccent,
                unfocusedBorderColor = Color(0xFF3A3A3A),
                focusedContainerColor = TsmSurfaceVariant,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = TsmAccent,
            ),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                if (uiState.newItemText.isNotBlank()) {
                    IconButton(onClick = viewModel::onAddItem) {
                        Icon(Icons.Outlined.Add, contentDescription = "Aggiungi", tint = TsmAccent)
                    }
                }
            },
        )
    }
}

// ── Participants card ──

@Composable
private fun ParticipantsCard(session: SessionResponse) {
    val participants = session.participants ?: emptyList()
    val max = session.maxParticipants ?: participants.size

    DetailCard {
        Text(
            "PARTECIPANTI · ${participants.size}/$max",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Color.Gray,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            participants.forEach { p ->
                val username = p.userId?.username ?: "?"
                val initials = username.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("")
                val avatarColor = avatarColorFor(username)
                val isCreator = p.role == "groupLeader"

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarColor)
                        .then(if (isCreator) Modifier.border(2.dp, TsmAccent, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials.take(2), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
            // Empty slots
            val emptySlots = (max - participants.size).coerceIn(0, 4)
            repeat(emptySlots) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(TsmSurfaceVariant).border(1.dp, Color(0xFF3A3A3A), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", style = MaterialTheme.typography.labelMedium, color = Color(0xFF555555))
                }
            }
        }
    }
}

private fun avatarColorFor(username: String): Color {
    val palette = listOf(
        Color(0xFF1B5E20), Color(0xFF01579B), Color(0xFF37474F),
        Color(0xFF4A148C), Color(0xFF006064), Color(0xFF3E2723),
    )
    return palette[abs(username.hashCode()) % palette.size]
}

// ── Invite Code Card (sempre visibile, copyable in clipboard) ──

@Composable
private fun InviteCodeCard(inviteCode: String) {
    val clipboard = LocalClipboardManager.current
    DetailCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "CODICE INVITO",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = Color.Gray,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    inviteCode,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                    color = TsmAccent,
                )
                Text(
                    "Condividi con i partecipanti per unirsi",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            TextButton(onClick = { clipboard.setText(AnnotatedString(inviteCode)) }) {
                Text("COPIA", color = TsmAccent, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ── Utility ──

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = TsmSurface) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun detailFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TsmAccent,
    unfocusedBorderColor = Color(0xFF3A3A3A),
    disabledBorderColor = Color(0xFF3A3A3A),
    focusedContainerColor = TsmSurfaceVariant,
    unfocusedContainerColor = TsmSurfaceVariant,
    disabledContainerColor = TsmSurfaceVariant,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = Color.White,
    cursorColor = TsmAccent,
)
