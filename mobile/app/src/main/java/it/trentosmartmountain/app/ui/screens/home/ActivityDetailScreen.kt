package it.trentosmartmountain.app.ui.screens.home

import android.app.Application
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas // Fix per Unresolved reference 'nativeCanvas' e 'drawText'
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.estimation.HikeEstimation
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs
import it.trentosmartmountain.app.data.remote.dto.StoryOverlay
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.components.TsmRouteElevationPager
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSos
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.viewmodel.ActivityDetailViewModel
import it.trentosmartmountain.app.viewmodel.TimelineEventType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: String,
    sessionId: String? = null,
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit = {},
    onShareStory: (it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs) -> Unit = {},
    viewModel: ActivityDetailViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
    // VM Activity-scoped per il social feed: il dialog "Condividi" usa questo
    // per chiamare share + ricaricare il feed; lo stesso VM è osservato dalla
    // HomeSocialScreen così l'utente vede il post in cima subito dopo Pubblica.
    socialFeedViewModel: it.trentosmartmountain.app.viewmodel.SocialFeedViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            (LocalContext.current as androidx.activity.ComponentActivity).application,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activityId) { viewModel.load(activityId, sessionId) }

    if (showShareDialog) {
        ShareActivityDialog(
            activityName = uiState.name,
            onDismiss = { showShareDialog = false },
            onShare = { caption ->
                showShareDialog = false
                // Distingue share di session (gruppo) da activity (libera):
                // se l'attività ha sessionId, è una sessione completata e
                // la condivisione passa per /sessions/:id/share. Authorization
                // server-side: solo creator può condividere la sessione.
                if (sessionId.isNullOrBlank()) {
                    // Lo share di un'attività libera richiede l'ID backend (ObjectId
                    // MongoDB), NON l'id Room locale (che per le attività registrate
                    // sul device è un UUID → 422 "ID non valido"). Usiamo il remoteId,
                    // disponibile solo dopo la sincronizzazione.
                    val remoteId = uiState.local?.remoteId
                    if (remoteId != null) {
                        socialFeedViewModel.shareActivity(remoteId, caption)
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Sincronizza l'attività prima di condividerla.",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                } else {
                    socialFeedViewModel.shareSession(sessionId, caption)
                }
            },
        )
    }

    // GPX export via "Create Document" — Android mostra il document picker
    val gpxExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val gpxContent = buildGpxString(uiState.name, uiState.trackPoints, uiState.startTimeMs)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(gpxContent.toByteArray(Charsets.UTF_8))
            }
            android.widget.Toast.makeText(context, "✓ GPX salvato!", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Errore esportazione GPX: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val timeFmt = SimpleDateFormat("HH:mm", Locale.ITALIAN)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = TsmSurface,
            title = { Text("Eliminare l'attività?", color = Color.White) },
            text = { Text("L'attività verrà rimossa dal dispositivo. Questa azione è irreversibile.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteActivity { onBack() } },
                    colors = ButtonDefaults.buttonColors(containerColor = TsmSos),
                ) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla", color = Color.Gray) } },
        )
    }

    Scaffold(
        containerColor = TsmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "LE MIE ATTIVITÀ",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = Color.Gray,
                        )
                        Text(
                            uiState.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Opzioni", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(TsmSurface),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Elimina", color = Color.White) },
                                onClick = { showMenu = false; showDeleteConfirm = true },
                            )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Sub-header: data + tipo ──
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (uiState.startTimeMs > 0) {
                    Surface(color = TsmSurfaceVariant, shape = RoundedCornerShape(20.dp)) {
                        Text(
                            uiState.activityType.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TsmAccent,
                        )
                    }
                    Text(
                        "${timeFmt.format(Date(uiState.startTimeMs))} · ${timeFmt.format(Date(uiState.endTimeMs))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }

            // ── Metrics Grid ──
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = TsmSurface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricCell(
                            label = "DISTANZA",
                            value = "%.1f km".format(uiState.distanceKm),
                            valueColor = TsmAccent,
                            subValue = null,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCell(
                            label = "DURATA",
                            value = HikeEstimation.formatHours(uiState.movingSeconds / 3600.0),
                            valueColor = TsmPrimary,
                            subValue = if (uiState.totalSeconds > uiState.movingSeconds) {
                                "${HikeEstimation.formatHours(uiState.totalSeconds / 3600.0)} tot."
                            } else null,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCell(
                            label = "VEL. MEDIA",
                            value = "%.1f km/h".format(uiState.avgSpeedKmh),
                            valueColor = Color(0xFFFF9800),
                            subValue = null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricCell(
                            label = "DISLIVELLO+",
                            value = "${uiState.elevationGainM} m",
                            valueColor = Color.White,
                            subValue = uiState.difficultyLevel?.let { "diff. $it" },
                            modifier = Modifier.weight(1f),
                        )
                        MetricCell(
                            label = "CALORIE",
                            value = "${uiState.estimatedCalories ?: "—"} kcal",
                            valueColor = Color(0xFFE91E63),
                            subValue = "stimate",
                            modifier = Modifier.weight(1f),
                        )
                        MetricCell(
                            label = "PUNTI",
                            value = "${uiState.points ?: "—"} pt",
                            valueColor = Color(0xFFFFD700),
                            subValue = null,
                            leadingIcon = {
                                Icon(Icons.Filled.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Mappa OSMdroid ──
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = TsmSurface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.session?.routeDetails?.name != null) {
                        Text(
                            "${uiState.session!!.routeDetails!!.name.uppercase()} · ${uiState.session!!.meetingLocation ?: ""}",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = TsmAccent,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    TsmRouteElevationPager(
                        routePoints = uiState.trackPoints.map { RoutePoint(it.first, it.second) },
                        elevationProfile = uiState.elevationProfile,
                        modifier = Modifier.fillMaxWidth(),
                        height = 200.dp,
                        cornerRadius = 8.dp,
                        backgroundBrush = androidx.compose.ui.graphics.SolidColor(TsmSurfaceVariant),
                        elevationLineColor = TsmAccent,
                        activeDotColor = TsmAccent,
                        emptyContent = {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(Icons.Outlined.RadioButtonChecked, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                Text("Nessun tracciato GPS disponibile", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    // Condividi sul feed sociale (apre dialog "Pubblica" con caption opzionale)
                    Button(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TsmAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CONDIVIDI SUL FEED", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Condividi come STORIA (foto/video breve + overlay tracciamento, 24h)
                    Button(
                        onClick = {
                            onShareStory(
                                StoryComposerArgs(
                                    type = "activity",
                                    sessionId = sessionId,
                                    activityId = if (sessionId == null) uiState.local?.remoteId else null,
                                    overlay = StoryOverlay(
                                        title = uiState.name,
                                        activityType = uiState.activityType,
                                        difficultyLevel = uiState.difficultyLevel,
                                        distanceMeters = uiState.distanceKm * 1000.0,
                                        elevationGainM = uiState.elevationGainM,
                                        movingSeconds = uiState.movingSeconds.toLong(),
                                        routePolyline = uiState.trackPoints
                                            .map { RoutePoint(it.first, it.second) }
                                            .takeIf { it.isNotEmpty() },
                                    ),
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CONDIVIDI COME STORIA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Download GPX
                    Button(
                        onClick = {
                            val safeName = uiState.name
                                .replace(" ", "_")
                                .replace("[^a-zA-Z0-9_\\-]".toRegex(), "")
                                .take(40)
                                .ifBlank { "escursione" }
                            gpxExportLauncher.launch("$safeName.gpx")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
                        shape = RoundedCornerShape(8.dp),
                        enabled = uiState.trackPoints.isNotEmpty(),
                    ) {
                        Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SCARICA .GPX", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // ── Partecipanti ──
            val participants = uiState.session?.participants
            if (!participants.isNullOrEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = TsmSurface) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "PARTECIPANTI · ${participants.size}",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = Color.Gray,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        participants.forEach { p ->
                            val name = p.userId?.username ?: "Utente"
                            val isOrganizer = p.role == "groupLeader"
                            val pid = p.userId?._id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !pid.isNullOrBlank()) { pid?.let(onUserClick) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AvatarImage(
                                    avatarUrl = p.userId?.avatarUrl,
                                    fallbackName = name,
                                    size = 36.dp,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isOrganizer) "Tu" else name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                    Text(if (isOrganizer) "Organizzatore" else "Partecipante", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Icon(Icons.Filled.CheckCircle, null, tint = TsmPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // ── Profilo Altimetrico ──
            if (uiState.elevationProfile.isNotEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = TsmSurface) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header: titolo a sinistra, "+dislivello positivo" a destra (singola label).
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "PROFILO ALTIMETRICO",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = Color.Gray,
                            )
                            Text(
                                "+${uiState.elevationGainM} m D+",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TsmPrimary,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Grafico — height contiene anche lo spazio per gli assi (riservato dentro la Canvas).
                        ElevationProfileChart(
                            profile = uiState.elevationProfile,
                            distanceKm = uiState.distanceKm,
                            minAltM = uiState.elevationMinM,
                            maxAltM = uiState.elevationMaxM,
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Footer: min/max altitudine reali (separati dal canvas, niente overlap).
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "MIN ${uiState.elevationMinM.roundToInt()} m",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                            )
                            Text(
                                "MAX ${uiState.elevationMaxM.roundToInt()} m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFD700),
                            )
                        }
                    }
                }
            }

            // ── Timeline ──
            if (uiState.timeline.isNotEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = TsmSurface) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "TIMELINE · ${uiState.timeline.size} EVENTI",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = Color.Gray,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        uiState.timeline.forEach { event ->
                            TimelineEventRow(event)
                            if (event != uiState.timeline.last()) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .size(width = 1.dp, height = 16.dp)
                                        .background(Color(0xFF3A3A3A)),
                                )
                            }
                        }
                    }
                }
            }

            // ── Badge ──
            val badges = buildBadges(uiState)
            if (badges.isNotEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = TsmSurface) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("BADGE OTTENUTI", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            badges.forEach { badge ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                    Surface(
                                        modifier = Modifier.size(52.dp),
                                        shape = CircleShape,
                                        color = badge.bgColor,
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(badge.emoji, fontSize = 24.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(badge.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = badge.textColor, textAlign = TextAlign.Center)
                                    Text(badge.subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Metric Cell ──

@Composable
private fun MetricCell(
    label: String,
    value: String,
    valueColor: Color,
    subValue: String?,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp), color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        if (leadingIcon != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                leadingIcon()
                Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = valueColor)
            }
        } else {
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = valueColor)
        }
        subValue?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
    }
}

// ── Elevation Profile Chart ──

/**
 * Profilo altimetrico con asse X (distanza km) reso DENTRO al canvas e clipping safe.
 *
 * Layout interno:
 *   [paddingTop]  area chart (curva + fill + marker max)
 *   [paddingBot]  asse X con label "0", "D/2", "D km"
 *
 * I label X sono disegnati con textAlign LEFT / CENTER / RIGHT in modo coerente,
 * così non straripano oltre i bordi (era il bug visivo della versione precedente).
 *
 * [profile] valori normalizzati 0..1 dalla ViewModel. [minAltM]/[maxAltM] sono
 * le altitudini assolute in metri usate solo per il colore della curva (gradiente
 * proporzionato all'altitudine media reale, non al valore normalizzato).
 */
@Composable
private fun ElevationProfileChart(
    profile: List<Double>,
    distanceKm: Double,
    minAltM: Double,
    maxAltM: Double,
    modifier: Modifier = Modifier,
) {
    if (profile.isEmpty()) return
    val green = TsmPrimary
    val red = Color(0xFFFF5722)
    // Indice di "alpinismo" basato sull'altitudine media reale (0 = pianura, 1 = vetta alpina).
    val avgAltM = (minAltM + maxAltM) / 2.0
    val altIntensity = ((avgAltM - 500.0) / 2000.0).coerceIn(0.0, 1.0).toFloat()
    val lineColor = lerp(green, red, altIntensity)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val n = profile.size
        val axisHeight = 16.dp.toPx()     // riservato per i label X in fondo al canvas
        val topPadding = 6.dp.toPx()      // spazio per il marker max
        val chartH = h - axisHeight - topPadding

        val path = Path()
        val fillPath = Path()

        profile.forEachIndexed { i, v ->
            val x = i / (n - 1).toFloat() * w
            // Inverte v ∈ [0,1]: 0 = base chart, 1 = top chart.
            val y = topPadding + (1f - v.toFloat()) * chartH
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, topPadding + chartH)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, topPadding + chartH)
        fillPath.close()

        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.5f),
                    lineColor.copy(alpha = 0.15f),
                ),
                startY = topPadding,
                endY = topPadding + chartH,
            ),
        )

        drawPath(
            path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Marker del punto massimo del profilo
        val maxVal = profile.maxOrNull() ?: 0.0
        val maxIdx = profile.indexOf(maxVal)
        val maxX = maxIdx / (n - 1).toFloat() * w
        val maxY = topPadding + (1f - profile[maxIdx].toFloat()) * chartH
        drawCircle(color = TsmAccent, radius = 4.dp.toPx(), center = Offset(maxX, maxY))

        // Asse X dentro al canvas con allineamento coerente (no overflow).
        val labelY = topPadding + chartH + 12.dp.toPx()
        val basePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9.dp.toPx()
            isAntiAlias = true
        }
        val leftPaint = android.graphics.Paint(basePaint).apply { textAlign = android.graphics.Paint.Align.LEFT }
        val centerPaint = android.graphics.Paint(basePaint).apply { textAlign = android.graphics.Paint.Align.CENTER }
        val rightPaint = android.graphics.Paint(basePaint).apply { textAlign = android.graphics.Paint.Align.RIGHT }
        drawContext.canvas.nativeCanvas.apply {
            drawText("0 km", 0f, labelY, leftPaint)
            drawText("%.1f km".format(distanceKm / 2), w / 2, labelY, centerPaint)
            drawText("%.1f km".format(distanceKm), w, labelY, rightPaint)
        }
    }
}

// ── Timeline Event Row ──

@Composable
private fun TimelineEventRow(event: it.trentosmartmountain.app.viewmodel.TimelineEvent) {
    val (dotColor, icon) = when (event.type) {
        TimelineEventType.DEPARTURE -> Pair(TsmPrimary, Icons.Outlined.PlayArrow)
        TimelineEventType.SPLIT -> Pair(TsmAccent, Icons.Outlined.RadioButtonChecked)
        TimelineEventType.AUTOPAUSE -> Pair(Color(0xFFFF9800), Icons.Outlined.RadioButtonChecked)
        TimelineEventType.ARRIVAL -> Pair(TsmSos, Icons.Outlined.RadioButtonChecked)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(dotColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = dotColor, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            event.subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
        }
        event.timeLabel?.let {
            Text(it, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.Gray)
        }
    }
}

// ── GPX Export ──

/**
 * Genera una stringa GPX 1.1 standard dal tracciato GPS registrato.
 *
 * Formato:
 *   <trkpt lat="46.07" lon="11.12"><ele>220.0</ele><time>ISO8601</time></trkpt>
 *
 * I punti sono distribuiti equamente nel tempo tra [startTimeMs] e il timestamp finale
 * stimato (startTimeMs + movingSeconds * 1000), dato che non salviamo timestamp per-punto.
 */
private fun buildGpxString(
    name: String,
    trackPoints: List<Triple<Double, Double, Double>>,
    startTimeMs: Long,
): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
    val n = trackPoints.size
    val sb = StringBuilder()
    sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
    sb.appendLine("""<gpx version="1.1" creator="Trento Smart Mountain" xmlns="http://www.topografix.com/GPX/1/1">""")
    sb.appendLine("  <trk>")
    sb.appendLine("    <name>${name.replace("&", "&amp;").replace("<", "&lt;")}</name>")
    sb.appendLine("    <trkseg>")
    trackPoints.forEachIndexed { i, (lat, lon, alt) ->
        val ts = if (n > 1 && startTimeMs > 0) {
            startTimeMs + (i.toLong() * 1000L)
        } else startTimeMs
        sb.appendLine("""      <trkpt lat="%.7f" lon="%.7f">""".format(lat, lon))
        if (alt != 0.0) sb.appendLine("""        <ele>%.1f</ele>""".format(alt))
        if (ts > 0) sb.appendLine("        <time>${sdf.format(java.util.Date(ts))}</time>")
        sb.appendLine("      </trkpt>")
    }
    sb.appendLine("    </trkseg>")
    sb.appendLine("  </trk>")
    sb.appendLine("</gpx>")
    return sb.toString()
}

// ── Badges significativi ──

private data class ActivityBadgeInfo(val emoji: String, val title: String, val subtitle: String, val bgColor: Color, val textColor: Color)

/**
 * Badge basati su soglie reali dell'escursionismo CAI.
 * Ogni badge ha una soglia significativa + un subtitolo con il valore effettivo.
 * L'efficienza (μ) dal modello TSM guida i badge di performance.
 */
private fun buildBadges(uiState: ActivityDetailViewModel.UiState): List<ActivityBadgeInfo> {
    val badges = mutableListOf<ActivityBadgeInfo>()
    val distKm = uiState.distanceKm
    val elevM = uiState.elevationGainM
    val movingH = uiState.movingSeconds / 3600.0
    val tNom = it.trentosmartmountain.app.data.estimation.HikeEstimation.naismithTimeHours(distKm, elevM)
    val mu = if (movingH > 0 && tNom > 0) (tNom / movingH).coerceIn(0.0, 2.0) else 0.0

    // 🏔 Alpinista: dislivello ≥ 1000m (soglia escursionismo EE/EEA)
    if (elevM >= 1000) badges.add(ActivityBadgeInfo(
        "🏔", "Alpinista",
        "+${elevM}m · soglia EE superata",
        Color(0xFF4A148C).copy(alpha = 0.25f), Color(0xFFCE93D8),
    ))
    // ⛰ Quotista: dislivello 500-999m
    else if (elevM >= 500) badges.add(ActivityBadgeInfo(
        "⛰", "Quotista",
        "+${elevM}m D+",
        Color(0xFF1A237E).copy(alpha = 0.25f), Color(0xFF90CAF9),
    ))

    // 🏃 In Forma: μ > 1.1 = più veloce del 10% rispetto al ritmo CAI
    if (mu > 1.1) badges.add(ActivityBadgeInfo(
        "🏃", "In Forma",
        "Ritmo +${((mu - 1) * 100).roundToInt()}% CAI",
        Color(0xFF1B5E20).copy(alpha = 0.25f), TsmPrimary,
    ))
    // 🐢 Passo Costante: 0.9 ≤ μ ≤ 1.1 = nella media CAI (ottimo per safety)
    else if (mu in 0.9..1.1) badges.add(ActivityBadgeInfo(
        "🎯", "Passo Costante",
        "Ritmo in linea CAI",
        Color(0xFF004D40).copy(alpha = 0.25f), TsmAccent,
    ))

    // 🗺 Lungo cammino: ≥ 20km
    if (distKm >= 20) badges.add(ActivityBadgeInfo(
        "🗺", "Lungo Cammino",
        "%.1f km percorsi".format(distKm),
        Color(0xFFE65100).copy(alpha = 0.25f), Color(0xFFFF9800),
    ))

    // ⭐ Punti bonus: se μ_clip = 1.2 (massima efficienza)
    if (mu >= 1.15) badges.add(ActivityBadgeInfo(
        "⭐", "Efficienza Max",
        "${uiState.points ?: 0} pt bonus",
        Color(0xFFF57F17).copy(alpha = 0.25f), Color(0xFFFFD700),
    ))

    // ✅ Completato: sempre presente (almeno un badge)
    if (badges.isEmpty()) badges.add(ActivityBadgeInfo(
        "✅", "Completato",
        "%.1f km · +${elevM}m".format(distKm),
        Color(0xFF212121).copy(alpha = 0.5f), Color(0xFF888888),
    ))

    return badges
}
