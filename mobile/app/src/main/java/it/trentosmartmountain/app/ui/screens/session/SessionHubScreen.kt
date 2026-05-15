package it.trentosmartmountain.app.ui.screens.session

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmBorder
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSos
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.viewmodel.SessionJoinViewModel
import it.trentosmartmountain.app.viewmodel.SessionPlanViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHubScreen(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (sessionId: String) -> Unit = {},
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize().background(TsmBackground)) {
        // Header
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
            Text(
                text = "Sessione",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Text(
                text = "Pianifica e unisciti alle escursioni",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PrimaryTabRow(
            selectedTabIndex = subTab,
            containerColor = TsmBackground,
            contentColor = Color.White,
            divider = { HorizontalDivider(color = Color(0xFF2A2A2A)) },
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        selectedTabIndex = subTab,
                        matchContentSize = true
                    ),
                    color = TsmAccent
                )
            },
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = {
                    Text(
                        stringResource(R.string.session_tab_plan),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (subTab == 0) TsmAccent else Color.Gray,
                    )
                },
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = {
                    Text(
                        stringResource(R.string.session_tab_join),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (subTab == 1) TsmAccent else Color.Gray,
                    )
                },
            )
        }

        when (subTab) {
            0 -> SessionPlanTab(onSessionCreated = { subTab = 1 })
            1 -> SessionJoinTab(onNavigateToDetail = onNavigateToDetail)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionPlanTab(
    onSessionCreated: () -> Unit = {},
    viewModel: SessionPlanViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDifficultyMenu by remember { mutableStateOf(false) }

    val gpxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "tracciato.gpx"
            viewModel.onGpxFileSelected(context.contentResolver, uri, fileName)
        }
    }

    if (uiState.sessionCreated && uiState.createdInviteCode != null) {
        SessionCreatedDialog(
            inviteCode = uiState.createdInviteCode!!,
            onDismiss = {
                viewModel.resetAfterCreation()
                onSessionCreated()
            },
        )
    }

    if (uiState.showQrPreview) {
        QrPreviewDialog(
            code = uiState.createdInviteCode ?: uiState.previewCode,
            onDismiss = { viewModel.onToggleQrPreview() },
        )
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN)
                        viewModel.onMeetingDateChange(fmt.format(Date(millis)))
                    }
                    showDatePicker = false
                }) { Text("OK", color = TsmAccent) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annulla", color = Color.Gray) } },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = 6, initialMinute = 30)
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(color = TsmSurface, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ora di ritrovo", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timeState)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Annulla", color = Color.Gray) }
                        TextButton(onClick = {
                            viewModel.onMeetingTimeChange(
                                "%02d:%02d".format(timeState.hour, timeState.minute),
                            )
                            showTimePicker = false
                        }) { Text("OK", color = TsmAccent) }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // TRACCIATO SECTION
        SectionCard {
            SectionLabel(stringResource(R.string.session_tracciato_title))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.session_gpx_import_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TsmBorder, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { gpxLauncher.launch("*/*") }
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.FileUpload, contentDescription = null, tint = TsmAccent, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.session_gpx_drop_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    Text(
                        stringResource(R.string.session_gpx_drop_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { gpxLauncher.launch("*/*") },
                        border = androidx.compose.foundation.BorderStroke(1.dp, TsmBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Text(
                            stringResource(R.string.session_gpx_sfoglia),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }

            uiState.gpxParseError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            uiState.gpxData?.let { gpx ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TsmBackground, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TsmPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(gpx.fileName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                        Text(
                            "%.1f km · +%d m · %d punti".format(gpx.distanceKm, gpx.elevationGainM, gpx.trackPoints),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                    IconButton(onClick = viewModel::onRemoveGpx) {
                        Icon(Icons.Outlined.Close, contentDescription = "Rimuovi", tint = Color.Gray)
                    }
                }
            }
        }

        // DETTAGLI SESSIONE
        SectionCard {
            SectionLabel(stringResource(R.string.session_details_section))

            SessionFieldLabel(stringResource(R.string.session_name_label))
            OutlinedTextField(
                value = uiState.sessionName,
                onValueChange = viewModel::onSessionNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.session_name_placeholder), color = Color.Gray) },
                singleLine = true,
                colors = sessionFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SessionFieldLabel(stringResource(R.string.session_date_label))
                    OutlinedTextField(
                        value = uiState.meetingDate,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        placeholder = { Text("20 Mag 2026", color = Color.Gray) },
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TsmAccent) },
                        colors = sessionFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SessionFieldLabel(stringResource(R.string.session_time_label))
                    OutlinedTextField(
                        value = uiState.meetingTime,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
                        placeholder = { Text("06:30", color = Color.Gray) },
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TsmAccent) },
                        colors = sessionFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SessionFieldLabel(stringResource(R.string.session_max_participants_label))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(
                    onClick = { viewModel.onMaxParticipantsChange(uiState.maxParticipants - 1) },
                    modifier = Modifier.size(36.dp).background(TsmSurfaceVariant, CircleShape),
                ) { Text("−", color = Color.White, style = MaterialTheme.typography.titleMedium) }
                Text(
                    "${uiState.maxParticipants} persone",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { viewModel.onMaxParticipantsChange(uiState.maxParticipants + 1) },
                    modifier = Modifier.size(36.dp).background(TsmSurfaceVariant, CircleShape),
                ) { Text("+", color = Color.White, style = MaterialTheme.typography.titleMedium) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SessionFieldLabel(stringResource(R.string.session_difficulty_label))
            Box {
                OutlinedTextField(
                    value = uiState.difficultyLevel,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().clickable { showDifficultyMenu = true },
                    readOnly = true,
                    enabled = false,
                    colors = sessionFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                )
                DropdownMenu(
                    expanded = showDifficultyMenu,
                    onDismissRequest = { showDifficultyMenu = false },
                    modifier = Modifier.background(TsmSurface),
                ) {
                    listOf("T" to "T – Turistico", "E" to "E – Escursionistico", "EE" to "EE – Escurs. Esperto", "EEA" to "EEA – Alpinistico").forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White) },
                            onClick = {
                                viewModel.onDifficultyChange(code)
                                showDifficultyMenu = false
                            },
                        )
                    }
                }
            }
        }

        // CONDIVISIONE
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(stringResource(R.string.session_sharing_section))
                OutlinedButton(
                    onClick = viewModel::onToggleQrPreview,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TsmAccent),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                ) {
                    Text(stringResource(R.string.session_sharing_preview), color = TsmAccent, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val displayCode = uiState.createdInviteCode ?: uiState.previewCode
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QrCodeImage(content = displayCode, modifier = Modifier.size(160.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.session_sharing_code_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = TsmAccent,
                    )
                    Text(
                        displayCode,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                        color = Color.White,
                    )
                    Text(
                        stringResource(R.string.session_sharing_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        uiState.generalError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = viewModel::onCreateSession,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !uiState.isLoading && !uiState.sessionCreated,
            colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
            shape = RoundedCornerShape(8.dp),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(
                    if (uiState.sessionCreated) "✓ SESSIONE CREATA" else stringResource(R.string.session_create_button),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SessionJoinTab(
    onNavigateToDetail: (sessionId: String) -> Unit = {},
    viewModel: SessionJoinViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayFormatted = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(Date())
    }

    if (uiState.leaveConfirmSessionId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLeaveConfirm,
            containerColor = TsmSurface,
            title = { Text(stringResource(R.string.session_leave_confirm_title), color = Color.White) },
            text = { Text(stringResource(R.string.session_leave_confirm_body), color = Color.Gray) },
            confirmButton = {
                Button(onClick = viewModel::confirmLeaveSession, colors = ButtonDefaults.buttonColors(containerColor = TsmSos)) {
                    Text("Abbandona", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissLeaveConfirm) { Text("Annulla", color = Color.Gray) } },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // CODE INPUT + JOIN
        SectionCard {
            SectionLabel(stringResource(R.string.session_join_code_label))
            Spacer(modifier = Modifier.height(8.dp))

            SessionCodeBoxInput(
                code = uiState.joinCode,
                onCodeChange = viewModel::onJoinCodeChange,
            )

            uiState.joinError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { /* TODO: ML Kit Barcode Scanning */ },
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TsmBorder),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = TsmAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.session_join_scan_qr),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Button(
                    onClick = viewModel::onJoinSession,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !uiState.isJoining && uiState.joinCode.length >= 4,
                ) {
                    if (uiState.isJoining) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(stringResource(R.string.session_join_button), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // SESSION LIST — tre stati: loading | error | empty | populated
        when {
            uiState.isLoadingSessions -> {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TsmAccent)
                }
            }
            uiState.generalError != null -> {
                // Errore visibile (include JsonSyntaxException da backend asimmetrico)
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = TsmSurface) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(uiState.generalError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        TextButton(onClick = viewModel::loadSessions) {
                            Text("Riprova", color = TsmAccent, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
            uiState.sessions.isEmpty() -> {
                // Empty state esplicito — non collassa silenziosamente
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Nessuna escursione in programma.\nInserisci un codice invito o scansiona un QR per unirti a una sessione.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.session_join_sessions_header), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = Color.Gray)
                    Text(uiState.sessions.size.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TsmAccent)
                }
                uiState.sessions.forEach { session ->
                    val isToday = session.meetingDate == todayFormatted
                    SessionCard(
                        session = session,
                        isToday = isToday,
                        onDetailClick = { onNavigateToDetail(session._id) },
                        onAvviaClick = { onNavigateToDetail(session._id) },
                        onAbbandonaClick = { viewModel.requestLeaveSession(session._id) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Moduli Sub-Gerarchici (Componenti Custom) ──

@Composable
private fun SessionCodeBoxInput(code: String, onCodeChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val display = code.uppercase().padEnd(8)

    Box(modifier = Modifier.fillMaxWidth().clickable { focusRequester.requestFocus() }) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (0..7).forEach { i ->
                val char = display.getOrElse(i) { ' ' }
                val isCursor = i == code.length && code.length < 8
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 52.dp)
                        .background(TsmSurfaceVariant, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isCursor) TsmAccent else Color(0xFF3A3A3A), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (char == ' ') "" else char.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (i == 3) Color.Gray else Color.White,
                    )
                }
            }
        }
        BasicTextField(
            value = code,
            onValueChange = { new ->
                val filtered = new.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(8)
                onCodeChange(filtered)
            },
            modifier = Modifier.focusRequester(focusRequester).alpha(0f).size(1.dp),
            singleLine = true,
        )
    }
}

@Composable
private fun SessionCard(
    session: it.trentosmartmountain.app.data.remote.dto.SessionResponse,
    isToday: Boolean,
    onDetailClick: () -> Unit,
    onAvviaClick: () -> Unit,
    onAbbandonaClick: () -> Unit,
) {
    val name = session.routeDetails?.name ?: "Sessione"
    val dateTime = listOfNotNull(session.meetingDate, session.meetingTime).joinToString(" · ")
    val host = session.creatorId?.username?.let { "host $it" } ?: ""
    val subtitle = listOf(dateTime, host).filter { it.isNotBlank() }.joinToString(" · ")
    val distKm = session.gpxStats?.distanceKm?.let { "%.1f km".format(it) }
    val participants = session.participants?.size?.let { "$it partecipanti" }
    val stats = listOfNotNull(distKm, participants).joinToString("  ·  ")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = TsmSurface,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onDetailClick() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = TsmSurfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        // Inserire l'icona TSMMountain o un placeholder corretto:
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TsmAccent)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        if (isToday) {
                            Surface(color = TsmPrimary, shape = RoundedCornerShape(4.dp)) {
                                Text(stringResource(R.string.session_card_oggi), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }
                    if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (stats.isNotBlank()) Text(stats, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp).alpha(0.5f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isToday && session.status == "PLANNED") {
                    Button(
                        onClick = onAvviaClick,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(stringResource(R.string.session_card_avvia), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
                OutlinedButton(
                    onClick = onAbbandonaClick,
                    modifier = Modifier.weight(if (isToday && session.status == "PLANNED") 1f else 2f).height(40.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TsmSos),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.session_card_abbandona), color = TsmSos, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// ── Utility Base Composables ──

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = TsmSurface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SessionFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun sessionFieldColors() = OutlinedTextFieldDefaults.colors(
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

@Composable
private fun QrCodeImage(content: String, modifier: Modifier = Modifier) {
    val bitmap = remember(content) {
        runCatching {
            val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 256, 256)
            val bmp = createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paintBlack = Paint().apply { color = AColor.BLACK }
            val paintWhite = Paint().apply { color = AColor.WHITE }
            canvas.drawRect(0f, 0f, 256f, 256f, paintWhite)
            val cell = 256f / matrix.width
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    if (matrix.get(x, y)) canvas.drawRect(x * cell, y * cell, (x + 1) * cell, (y + 1) * cell, paintBlack)
                }
            }
            bmp
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code", modifier = modifier.clip(RoundedCornerShape(8.dp)))
    } else {
        Box(modifier = modifier.background(Color.White, RoundedCornerShape(8.dp)))
    }
}

@Composable
private fun QrPreviewDialog(code: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = TsmSurface, shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("QR Code Sessione", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                QrCodeImage(content = code, modifier = Modifier.size(240.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    code,
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Mostra ai partecipanti per unirsi", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Chiudi", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
            }
        }
    }
}

@Composable
private fun SessionCreatedDialog(inviteCode: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TsmSurface,
        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TsmPrimary, modifier = Modifier.size(48.dp)) },
        title = { Text(stringResource(R.string.session_created_title), color = Color.White, textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Il codice sessione è:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    inviteCode,
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = TsmAccent,
                )
                Spacer(modifier = Modifier.height(8.dp))
                QrCodeImage(content = inviteCode, modifier = Modifier.size(160.dp))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary)) {
                Text("OK")
            }
        },
    )
}