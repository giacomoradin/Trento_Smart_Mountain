package it.trentosmartmountain.app.ui.screens.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.checklist.ChecklistMapper
import it.trentosmartmountain.app.data.remote.dto.SessionResponse
import it.trentosmartmountain.app.data.remote.dto.SessionParticipant
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs
import it.trentosmartmountain.app.ui.components.TsmShareStoryButton
import it.trentosmartmountain.app.util.downsampleByIndex
import it.trentosmartmountain.app.data.remote.dto.StoryOverlay
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.components.SessionParticipationActions
import it.trentosmartmountain.app.ui.components.TsmRouteElevationPager
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmBorder
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSos
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.viewmodel.SessionDetailViewModel
import it.trentosmartmountain.app.ui.util.SessionDateFormats
import it.trentosmartmountain.app.ui.components.AvatarImage
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Dettaglio escursione (navigazione full-screen sopra [HikerMainScreen]).
 *
 * Mostra tracciato, meteo, checklist, partecipanti e pulsante **AVVIA ESCURSIONE**.
 * Se la data non è oggi, un dialog chiede conferma prima di avviare.
 *
 * @param sessionId identificativo sessione da caricare
 * @param onBack pop dello stack di navigazione
 * @param onAvviaConfirmed chiamato dopo conferma avvio (tipicamente torna alla shell; il tracking parte in tab Registra)
 * @param currentUserId dal JWT, per abilitare modifica/eliminazione al capogruppo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    onAvviaConfirmed: (sessionId: String) -> Unit = {},
    currentUserId: String = "",
    onUserClick: (userId: String) -> Unit = {},
    onShareStory: (it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs) -> Unit = {},
    viewModel: SessionDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            if (errorMessage.isNotBlank() && uiState.session != null) {
                Toast.makeText(context, "DIAGNOSTICA SERVER: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(sessionId) { viewModel.loadSession(sessionId) }
    LaunchedEffect(currentUserId) { viewModel.bindCurrentUserId(currentUserId) }

    LaunchedEffect(sessionId, uiState.session?.status) {
        if (uiState.session?.status != "PLANNED") return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(SessionDetailViewModel.AUTO_REFRESH_MS)
            viewModel.refreshChecklistAuto()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sessionId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadSession(sessionId)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.showAvviaConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAvviaConfirm,
            containerColor = TsmSurface,
            title = { Text(stringResource(R.string.session_avvia_confirm_title), color = Color.White) },
            text = {
                Text(
                    stringResource(R.string.session_avvia_confirm_body),
                    color = Color.Gray,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmLeaderStartEarly { onAvviaConfirmed(sessionId) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
                ) { Text("Avvia") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAvviaConfirm) { Text("Annulla", color = Color.Gray) }
            },
        )
    }

    var showEditDatePicker by remember { mutableStateOf(false) }
    var showEditTimePicker by remember { mutableStateOf(false) }
    var showDifficultyMenu by remember { mutableStateOf(false) }
    // Conferma rimozione DEFINITIVA di un partecipante (solo capogruppo): (userId, nome).
    var removeConfirmUser by remember { mutableStateOf<Pair<String, String>?>(null) }

    removeConfirmUser?.let { (uid, name) ->
        AlertDialog(
            onDismissRequest = { removeConfirmUser = null },
            containerColor = TsmSurface,
            title = { Text("Rimuovere $name?", color = Color.White) },
            text = {
                Text(
                    "$name verrà rimosso definitivamente dalla sessione e non potrà più unirsi.",
                    color = Color.Gray,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeParticipant(uid)
                        removeConfirmUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TsmSos),
                ) { Text("Rimuovi") }
            },
            dismissButton = {
                TextButton(onClick = { removeConfirmUser = null }) { Text("Annulla", color = Color.Gray) }
            },
        )
    }

    LaunchedEffect(uiState.editMode) {
        if (!uiState.editMode) {
            showEditDatePicker = false
            showEditTimePicker = false
            showDifficultyMenu = false
        }
    }

    if (showEditDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEditDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        viewModel.onEditDateChange(SessionDateFormats.formatApiFromMillis(millis))
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
    val isToday = SessionDateFormats.isTodayApi(session?.meetingDate)

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
                            session?.meetingDate?.let {
                                append(SessionDateFormats.formatDisplayFromApi(it))
                            }
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

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.loadSession(sessionId, manualRefresh = true) },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            Spacer(modifier = Modifier.height(4.dp))

            AnimatedVisibility(
                visible = uiState.editMode && !uiState.isSaving,
                enter = expandVertically(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250))
            ) {
                EditModeCard(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDateClick = { showEditDatePicker = true },
                    onTimeClick = { showEditTimePicker = true },
                    showDifficultyMenu = showDifficultyMenu,
                    onDifficultyMenuToggle = { showDifficultyMenu = !showDifficultyMenu },
                    onDifficultySelect = { d ->
                        viewModel.onEditDifficultyChange(d)
                        showDifficultyMenu = false
                    },
                )
            }

            InviteCodeCard(inviteCode = session.inviteCode)

            // Condividi la pianificazione come STORIA (pre-hike) col link "Unisciti".
            if (session.status == "PLANNED") {
                TsmShareStoryButton(
                    onClick = {
                        onShareStory(
                            StoryComposerArgs(
                                type = "planned_session",
                                sessionId = session._id,
                                overlay = StoryOverlay(
                                    title = session.routeDetails?.name,
                                    difficultyLevel = session.routeDetails?.difficultyLevel,
                                    distanceMeters = session.gpxStats?.distanceKm?.let { it * 1000.0 },
                                    elevationGainM = session.gpxStats?.elevationGainM,
                                    // Cap a 300 punti: l'overlay backend accetta max 500.
                                    // Percorsi GPX lunghi davano 422 "Dati non validi".
                                    routePolyline = session.plannedRoute?.polylinePoints
                                        ?.map { RoutePoint(it.lat, it.lon) }
                                        ?.let { downsampleByIndex(it, 300) },
                                ),
                            ),
                        )
                    },
                )
            }

            DetailCard {
                val dist = session.gpxStats?.distanceKm
                val elev = session.gpxStats?.elevationGainM

                val routePoints = session.plannedRoute?.polylinePoints?.map { RoutePoint(it.lat, it.lon) }
                TsmRouteElevationPager(
                    routePoints = routePoints,
                    elevationProfile = session.gpxStats?.elevationProfile,
                    distanceKm = dist,
                    modifier = Modifier.fillMaxWidth(),
                    height = 180.dp,
                    cornerRadius = 8.dp,
                    backgroundBrush = androidx.compose.ui.graphics.SolidColor(TsmSurfaceVariant),
                    elevationLineColor = TsmAccent,
                    activeDotColor = TsmAccent,
                    difficultyLevel = session.routeDetails?.difficultyLevel,
                    expandable = true,
                    emptyContent = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Nessun tracciato disponibile",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Priorità durata: actualStats.movingSeconds (post-completamento) →
                    //                  gpxStats.gpxDurationSec (file con timestamp) →
                    //                  stima CAI sintetica
                    val actualSec = session.actualStats?.movingSeconds
                    val gpxSec = session.gpxStats?.gpxDurationSec
                    val (durationLabel, durationSource) = when {
                        actualSec != null -> Pair(
                            it.trentosmartmountain.app.data.estimation.HikeEstimation.formatHours(actualSec / 3600.0),
                            "REALE",
                        )
                        gpxSec != null -> Pair(
                            it.trentosmartmountain.app.data.estimation.HikeEstimation.formatHours(gpxSec / 3600.0),
                            "GPX",
                        )
                        dist != null && elev != null -> Pair(
                            it.trentosmartmountain.app.data.estimation.HikeEstimation.formatHours(
                                it.trentosmartmountain.app.data.estimation.HikeEstimation.caiTimeHours(dist, elev),
                            ),
                            "CAI",
                        )
                        else -> Pair("—", "CAI")
                    }
                    StatCell("DISTANZA", dist?.let { "%.1f km".format(it) } ?: "—", TsmBorder, Modifier.weight(1f))
                    StatCell("DISLIVELLO", elev?.let { "+$it m" } ?: "—", TsmAccent, Modifier.weight(1f))
                    StatCell("DURATA $durationSource", durationLabel, TsmPrimary, Modifier.weight(1f))
                }

                if (dist != null && elev != null && dist > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val estPoints = session.gpxStats.estimatedPoints
                        ?: it.trentosmartmountain.app.data.estimation.HikeEstimation
                            .estimatedPoints(dist, elev)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = TsmSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "PUNTI STIMATI",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    color = Color.Gray,
                                )
                                Text(
                                    "Valore medio · μ = 1.0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray.copy(alpha = 0.6f),
                                )
                            }
                            Text(
                                "$estPoints pt",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = TsmAccent,
                            )
                        }
                    }
                }
            }

            MeteoCard(
                meetingDate = SessionDateFormats.formatDisplayFromApi(session.meetingDate).ifBlank { "—" },
                uiState = uiState,
                onRefresh = viewModel::refreshMeteo,
            )

            ChecklistCard(
                uiState = uiState,
                viewModel = viewModel,
                onRefresh = viewModel::refreshChecklistManual,
            )

            ParticipantsCard(
                session = session,
                currentUserId = currentUserId,
                onUserClick = onUserClick,
                onApprove = { viewModel.approveParticipant(it) },
                onReject = { viewModel.rejectParticipant(it) },
                onRemove = { uid ->
                    // Mostra conferma prima della rimozione definitiva (ban).
                    val name = session.participants.orEmpty()
                        .firstOrNull { it.userId?._id == uid }
                        ?.userId?.username ?: "il partecipante"
                    removeConfirmUser = uid to name
                },
            )

            val participation = remember(uiState.session, uiState.liveUiEpoch, currentUserId) {
                viewModel.participationUi()
            }
            if (participation != null) {
                SessionParticipationActions(
                    ui = participation,
                    onLeaderStart = {
                        viewModel.requestLeaderStart { onAvviaConfirmed(sessionId) }
                    },
                    onLeaderStop = {
                        // Arresta la sessione: se è in corso un'attività, il
                        // SessionStopCoordinator fa comparire su Registra il dialog
                        // "Salva attività" (stessa logica di "Termina"). Torniamo alla
                        // shell così il dialog è visibile e l'attività non viene persa.
                        viewModel.leaderStop()
                        onBack()
                    },
                    onJoinLive = {
                        viewModel.joinLive { onAvviaConfirmed(sessionId) }
                    },
                    onSoloPractice = {
                        viewModel.startSoloPractice { onAvviaConfirmed(sessionId) }
                    },
                    onLeaveLive = {
                        viewModel.leaveLive()
                        onBack()
                    },
                    compact = false,
                    leaderStartLabel = stringResource(R.string.session_detail_avvia),
                    leaderStopLabel = stringResource(R.string.session_detail_arresta),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

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
                    value = SessionDateFormats.formatDisplayFromApi(uiState.editDate),
                    onValueChange = {},
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

@Composable
private fun ElevationProfileChart(
    elevationProfile: List<Double>?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padX = 8.dp.toPx()
        val padY = 12.dp.toPx()

        for (i in 0..2) {
            val terrainPath = Path().apply {
                val base = h * (0.65f + i * 0.10f)
                moveTo(0f, h)
                cubicTo(w * 0.2f, base - 15f, w * 0.5f, base + 25f, w * 0.75f, base - 10f)
                cubicTo(w * 0.85f, base - 5f, w * 0.92f, base + 15f, w, base)
                lineTo(w, h)
                close()
            }
            drawPath(terrainPath, color = Color(0xFF2A2A2A).copy(alpha = 0.5f + i * 0.08f))
        }

        if (elevationProfile != null && elevationProfile.size >= 2) {
            val minEle = elevationProfile.min()
            val maxEle = elevationProfile.max()
            val range = (maxEle - minEle).coerceAtLeast(1.0)
            val chartW = w - 2 * padX
            val chartH = h - 2 * padY

            fun xOf(i: Int): Float =
                padX + chartW * (i.toDouble() / (elevationProfile.size - 1)).toFloat()

            fun yOf(ele: Double): Float =
                (padY + chartH * (1.0 - (ele - minEle) / range)).toFloat()

            val profilePath = Path().apply {
                elevationProfile.forEachIndexed { i, e ->
                    val x = xOf(i)
                    val y = yOf(e)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                profilePath,
                color = TsmAccent,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )

            val fillPath = Path().apply {
                addPath(profilePath)
                lineTo(xOf(elevationProfile.size - 1), h - padY)
                lineTo(xOf(0), h - padY)
                close()
            }
            drawPath(fillPath, color = TsmAccent.copy(alpha = 0.15f))

            val startCenter = Offset(xOf(0), yOf(elevationProfile.first()))
            drawCircle(color = Color(0xFF1B5E20), radius = 10.dp.toPx(), center = startCenter)
            drawCircle(color = TsmPrimary, radius = 7.dp.toPx(), center = startCenter)

            val endCenter = Offset(xOf(elevationProfile.size - 1), yOf(elevationProfile.last()))
            drawCircle(color = Color(0xFF880E4F).copy(alpha = 0.4f), radius = 13.dp.toPx(), center = endCenter)
            drawCircle(color = TsmSos, radius = 9.dp.toPx(), center = endCenter)
        } else {
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
                color = TsmAccent.copy(alpha = 0.6f),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 9f), 0f),
                ),
            )
            val startCenter = Offset(w * 0.05f, h * 0.72f)
            drawCircle(color = Color(0xFF1B5E20), radius = 10.dp.toPx(), center = startCenter)
            drawCircle(color = TsmPrimary, radius = 7.dp.toPx(), center = startCenter)
            val endCenter = Offset(w * 0.95f, h * 0.30f)
            drawCircle(color = Color(0xFF880E4F).copy(alpha = 0.4f), radius = 13.dp.toPx(), center = endCenter)
            drawCircle(color = TsmSos, radius = 9.dp.toPx(), center = endCenter)
        }
    }
}

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

private fun skyConditionEmoji(code: String?): String = when (code?.trim()) {
    "1" -> "☀️"; "2" -> "🌤"; "3" -> "⛅"; "4" -> "🌥"; "5" -> "☁️"
    "6" -> "🌫"; "7" -> "🌦"; "8" -> "🌧"; "9" -> "⛈"; "10" -> "🌨"
    "11" -> "❄️"; "12" -> "⛈"; "13" -> "🌨"
    "A" -> "☀️"; "B" -> "🌤"; "C" -> "⛅"; "D" -> "☁️"; "E" -> "🌫"; "F" -> "🌧"; "G" -> "❄️"
    null -> "🌤"; else -> "🌤"
}

@Composable
private fun MeteoCard(
    meetingDate: String,
    uiState: SessionDetailViewModel.UiState,
    onRefresh: () -> Unit,
) {
    val forecast = uiState.weatherForecast
    val locationName = forecast?.location?.name ?: forecast?.referenceTown?.name ?: "—"
    val elevation = forecast?.location?.elevation?.let { " · ${it}m" } ?: ""

    val today24h = forecast?.forecast24h?.firstOrNull()
    val tempMin = today24h?.temperatureMin?.let { "%.0f°".format(it) } ?: "—"
    val tempMax = today24h?.temperatureMax?.let { "%.0f°".format(it) } ?: "—"
    val mainIcon = skyConditionEmoji(today24h?.skyCondition)

    val next3h = forecast?.forecast3h?.take(5) ?: emptyList()

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
                    text = uiState.meteoError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onRefresh) {
                    Text("Riprova", color = TsmAccent, style = MaterialTheme.typography.labelMedium)
                }
            }
            forecast == null && !uiState.meteoLoading -> {
                Text(
                    "Nessun dato meteo disponibile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            forecast != null -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(mainIcon, fontSize = 36.sp)
                    Column {
                        Text(
                            text = "$tempMin / $tempMax",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = "$locationName$elevation",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        today24h?.windSpeed?.let { ws ->
                            val dir = today24h.windDirection?.let { d ->
                                val dirs = listOf("N","NE","E","SE","S","SO","O","NO")
                                dirs[((d + 22.5) / 45).toInt() % 8]
                            } ?: ""
                            Text(
                                text = "Vento $dir ${ws.toInt()} km/h",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                        today24h?.rainProbability?.let { prob ->
                            if (prob > 20) {
                                Text(
                                    text = "Pioggia ${prob.toInt()}%${today24h.rainFall?.let { " · ${it}mm" } ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TsmAccent,
                                )
                            }
                        }
                    }
                }

                if (next3h.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        next3h.forEach { slot ->
                            val hourLabel = slot.validFrom
                                ?.substringAfter("T")
                                ?.substringBefore(":")
                                ?.let { "${it}h" }
                                ?: "—"
                            val tempLabel = slot.temperature?.let { "%.0f°".format(it) } ?: "—"
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(hourLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(skyConditionEmoji(slot.skyCondition), fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    tempLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                slot.rainProbability?.let { prob ->
                                    if (prob > 30) {
                                        Text(
                                            "${prob.toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TsmAccent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistCard(
    uiState: SessionDetailViewModel.UiState,
    viewModel: SessionDetailViewModel,
    onRefresh: () -> Unit,
) {
    val checkedCount = uiState.checklist.count { it.checked }
    val total = uiState.checklist.size

    val updatedAgo = uiState.checklistLastUpdate?.let { ts ->
        val diffMs = System.currentTimeMillis() - ts
        when {
            diffMs < 60_000L -> "ora"
            diffMs < 3_600_000L -> "${diffMs / 60_000L} min fa"
            else -> "${diffMs / 3_600_000L} h fa"
        }
    }

    val freezeLabel = when {
        uiState.checklistIsFrozen -> "Congelata"
        uiState.checklistFreezeAtMillis != null -> {
            val diffMs = uiState.checklistFreezeAtMillis - System.currentTimeMillis()
            if (diffMs <= 0) "Congelata"
            else {
                val hours = diffMs / 3_600_000L
                val mins = (diffMs % 3_600_000L) / 60_000L
                when {
                    hours > 24 -> "Freeze tra ${hours / 24} g"
                    hours > 0 -> "Freeze tra ${hours}h ${mins}m"
                    else -> "Freeze tra ${mins} min"
                }
            }
        }
        else -> null
    }

    DetailCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CHECKLIST",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (freezeLabel != null) {
                    Surface(
                        color = if (uiState.checklistIsFrozen) TsmSurfaceVariant else TsmPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (uiState.checklistIsFrozen) Color.Gray else TsmAccent,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                freezeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.checklistIsFrozen) Color.Gray else TsmAccent,
                            )
                        }
                    }
                }
                Text(
                    text = "$checkedCount / $total",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TsmAccent,
                    maxLines = 1,
                )
            }
        }

        if (uiState.checklistAcquaLitri != null || uiState.checklistCalorie != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    uiState.checklistAcquaLitri?.let { append("Acqua consigliata: ${"%.1f".format(it)} L") }
                    if (uiState.checklistAcquaLitri != null && uiState.checklistCalorie != null) append(" · ")
                    uiState.checklistCalorie?.let { append("~$it kcal") }
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
            )
        }

        if (uiState.checklistMeteoApplied && !uiState.checklistMeteoLocationName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Include previsioni meteo · ${uiState.checklistMeteoLocationName}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.85f),
            )
        } else if (uiState.checklistUnavailableReason.isNullOrBlank() &&
            uiState.checklist.isNotEmpty() &&
            !uiState.checklistMeteoApplied
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Meteo non ancora applicato: in attesa del forecast o aggiorna manualmente.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.7f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                color = TsmSurfaceVariant,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable(enabled = !uiState.checklistLoading) { onRefresh() },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (uiState.checklistLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = TsmAccent)
                    } else {
                        Icon(Icons.Outlined.Schedule, null, tint = TsmAccent, modifier = Modifier.size(12.dp))
                    }
                    Text(
                        when {
                            uiState.checklistLoading -> "Aggiornamento..."
                            updatedAgo != null -> "Aggiornato $updatedAgo"
                            else -> "Aggiorna"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TsmAccent,
                    )
                }
            }
        }

        when {
            uiState.checklistUnavailableReason != null -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    uiState.checklistUnavailableReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            uiState.checklistError != null -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    uiState.checklistError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onRefresh) {
                    Text("Riprova", color = TsmAccent, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (uiState.checklist.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val sections = ChecklistMapper.partition(uiState.checklist)

            if (sections.essenziali.isNotEmpty()) {
                ChecklistExpandableSection(
                    title = "Essenziali",
                    allItems = sections.essenziali,
                    viewModel = viewModel,
                )
            }

            if (sections.consigliati.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                ChecklistExpandableSection(
                    title = "Consigliati",
                    allItems = sections.consigliati,
                    viewModel = viewModel,
                )
            }

            if (sections.opzionali.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                ChecklistExpandableSection(
                    title = "Opzionali",
                    allItems = sections.opzionali,
                    viewModel = viewModel,
                )
            }

            if (sections.personali.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                ChecklistSectionHeader("Aggiunti da te")
                ChecklistItemList(
                    items = sections.personali,
                    viewModel = viewModel,
                    reorderable = true,
                    onMove = { from, to ->
                        viewModel.onChecklistMoveInSubset(
                            sections.personali.map { it.id },
                            from,
                            to,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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

@Composable
private fun ChecklistExpandableSection(
    title: String,
    allItems: List<SessionDetailViewModel.ChecklistItem>,
    viewModel: SessionDetailViewModel,
) {
    var expanded by remember(allItems.size) { mutableStateOf(false) }
    val hiddenCount = (allItems.size - ChecklistMapper.INITIAL_SECTION_VISIBLE).coerceAtLeast(0)
    val visibleItems = if (expanded || hiddenCount == 0) {
        allItems
    } else {
        allItems.take(ChecklistMapper.INITIAL_SECTION_VISIBLE)
    }

    ChecklistSectionHeader(title)
    ChecklistItemList(
        items = visibleItems,
        viewModel = viewModel,
        reorderable = expanded,
        onMove = { from, to ->
            viewModel.onChecklistMoveInSubset(allItems.map { it.id }, from, to)
        },
    )
    if (hiddenCount > 0) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = TsmAccent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (expanded) "Mostra meno" else "Visualizza altri $hiddenCount elementi",
                color = TsmAccent,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ChecklistSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
        color = TsmAccent.copy(alpha = 0.85f),
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun ChecklistItemRow(
    item: SessionDetailViewModel.ChecklistItem,
    viewModel: SessionDetailViewModel,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Checkbox(
            checked = item.checked,
            onCheckedChange = { viewModel.onToggleCheck(item.id) },
            colors = CheckboxDefaults.colors(
                checkedColor = TsmPrimary,
                uncheckedColor = Color(0xFF555555),
                checkmarkColor = Color.White,
            ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.checked) Color.Gray else Color.White,
            )
            if (!item.motivo.isNullOrBlank()) {
                Text(
                    item.motivo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.8f),
                )
            }
        }
        if (item.isPersonal) {
            IconButton(onClick = { viewModel.onRemoveItem(item.id) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Rimuovi", tint = Color(0xFF888888), modifier = Modifier.size(18.dp))
            }
        } else {
            Spacer(modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun ChecklistItemList(
    items: List<SessionDetailViewModel.ChecklistItem>,
    viewModel: SessionDetailViewModel,
    reorderable: Boolean,
    onMove: (Int, Int) -> Unit,
) {
    if (items.isEmpty()) return

    if (reorderable) {
        sh.calvin.reorderable.ReorderableColumn(
            list = items,
            onSettle = onMove,
        ) { _, item, _ ->
            key(item.id) {
                ChecklistItemRow(item, viewModel) {
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
                }
            }
        }
    } else {
        items.forEach { item ->
            key(item.id) {
                ChecklistItemRow(item, viewModel) {
                    Spacer(modifier = Modifier.width(28.dp))
                }
            }
        }
    }
}

@Composable
private fun ParticipantsCard(
    session: SessionResponse,
    currentUserId: String,
    onUserClick: (userId: String) -> Unit = {},
    onApprove: (userId: String) -> Unit = {},
    onReject: (userId: String) -> Unit = {},
    onRemove: (userId: String) -> Unit = {},
) {
    val participants = session.participants ?: emptyList()
    val accepted = participants.filter { !it.isPending }
    val pending = participants.filter { it.isPending }
    val max = session.maxParticipants ?: accepted.size

    val isViewerLeader = session.creatorId?._id == currentUserId
    // Un membro già accettato (incluso il capogruppo) può approvare/rifiutare i pending.
    val isViewerAcceptedMember = accepted.any { it.userId?._id == currentUserId }

    DetailCard {
        Text(
            "PARTECIPANTI · ${accepted.size}/$max",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Color.Gray,
        )
        Spacer(modifier = Modifier.height(8.dp))
        accepted.forEach { p ->
            val subtitle = when {
                p.role == "groupLeader" -> "Capogruppo"
                !p.approvedBy?.username.isNullOrBlank() -> "Accettato da ${p.approvedBy?.username}"
                else -> "Partecipante"
            }
            ParticipantRow(
                participant = p,
                subtitle = subtitle,
                onUserClick = onUserClick,
                trailing = {
                    // Solo il capogruppo può rimuovere definitivamente (no se stesso).
                    if (isViewerLeader && p.role != "groupLeader") {
                        IconButton(onClick = { p.userId?._id?.let(onRemove) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Rimuovi", tint = TsmSos)
                        }
                    }
                },
            )
        }

        if (pending.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "IN ATTESA · ${pending.size}",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = Color(0xFFFB8C00),
            )
            Spacer(modifier = Modifier.height(4.dp))
            pending.forEach { p ->
                ParticipantRow(
                    participant = p,
                    subtitle = "Richiesta in attesa di approvazione",
                    pendingBadge = true,
                    onUserClick = onUserClick,
                    trailing = {
                        if (isViewerAcceptedMember) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { p.userId?._id?.let(onApprove) }) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Accetta", tint = TsmPrimary)
                                }
                                IconButton(onClick = { p.userId?._id?.let(onReject) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Rifiuta", tint = TsmSos)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: SessionParticipant,
    subtitle: String,
    onUserClick: (userId: String) -> Unit,
    pendingBadge: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val pid = participant.userId?._id
    val name = participant.userId?.username ?: "?"
    val isCreator = participant.role == "groupLeader"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(enabled = !pid.isNullOrBlank()) { pid?.let(onUserClick) }
                .then(if (isCreator) Modifier.border(2.dp, TsmAccent, CircleShape) else Modifier),
        ) {
            AvatarImage(avatarUrl = participant.userId?.avatarUrl, fallbackName = name, size = 40.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                if (pendingBadge) {
                    Surface(color = Color(0xFFFB8C00).copy(alpha = 0.18f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "IN ATTESA",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFB8C00),
                        )
                    }
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        trailing()
    }
}

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