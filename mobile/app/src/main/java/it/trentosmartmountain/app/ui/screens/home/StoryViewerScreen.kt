package it.trentosmartmountain.app.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.StoryItem
import it.trentosmartmountain.app.data.remote.dto.StoryMedia
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.util.AvatarUtils
import it.trentosmartmountain.app.viewmodel.StoryViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private const val IMAGE_SEGMENT_MS = 5000L

/**
 * Visualizzatore storie full-screen (rework Fase C).
 *
 * Carica le storie REALI dell'autore (`/stories/user/:id`) e le riproduce in
 * sequenza, stile Instagram:
 *  - barra di progresso segmentata (una per storia) con auto-advance;
 *  - media: foto (Base64) o video breve (Base64 → cache file → VideoView);
 *  - overlay tracciamento (titolo, traccia, distanza/dislivello/tempo);
 *  - per le storie planned_session: bottone "UNISCITI" (→ richiesta pending);
 *  - tap a sinistra = precedente, a destra = successiva, X = chiudi.
 */
@Composable
fun StoryViewerScreen(
    userId: String,
    onClose: () -> Unit,
    onOpenSession: (sessionId: String) -> Unit = {},
    viewModel: StoryViewerViewModel = viewModel(),
) {
    LaunchedEffect(userId) { viewModel.load(userId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.joinInfo) {
        state.joinInfo?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearJoinInfo()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            state.stories.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        state.error ?: "Nessuna storia disponibile",
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onClose) { Text("Chiudi") }
                }
            }
            else -> StoryPager(
                stories = state.stories,
                onClose = onClose,
                onMarkViewed = viewModel::markViewed,
                onJoin = viewModel::joinSession,
                onOpenSession = onOpenSession,
            )
        }
    }
}

@Composable
private fun StoryPager(
    stories: List<StoryItem>,
    onClose: () -> Unit,
    onMarkViewed: (String) -> Unit,
    onJoin: (inviteCode: String) -> Unit,
    onOpenSession: (sessionId: String) -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    val current = stories.getOrNull(index) ?: return
    val media = current.media.firstOrNull()

    // Durata del segmento: foto = 5s, video = durata (clamp 3-10s).
    val segmentMs = remember(index) {
        if (media?.kind == "video") {
            ((media.durationSec ?: 8.0) * 1000).toLong().coerceIn(3000L, 10000L)
        } else IMAGE_SEGMENT_MS
    }

    var elapsed by remember(index) { mutableStateOf(0L) }
    LaunchedEffect(index, stories.size) {
        onMarkViewed(current.id)
        elapsed = 0L
        while (elapsed < segmentMs) {
            delay(50L)
            elapsed += 50L
        }
        if (index < stories.lastIndex) index++ else onClose()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Media ──
        if (media != null) {
            when (media.kind) {
                "video" -> StoryVideo(media = media, modifier = Modifier.fillMaxSize())
                else -> StoryImage(media = media, modifier = Modifier.fillMaxSize())
            }
        } else {
            // Storia senza media: sfondo gradiente con la sola traccia/overlay.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1B1B1F), Color(0xFF101012)))),
            )
        }

        // Traccia GPS in overlay (se disponibile) — "firma" del percorso.
        current.overlay?.routePolyline?.takeIf { it.size >= 2 }?.let { pts ->
            RouteTracePreview(
                points = pts,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(24.dp),
                lineColor = Color.White,
            )
        }

        // Scrim inferiore per leggibilità testo.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                    ),
                ),
        )

        // ── Zone di tap: sx = precedente, dx = successiva ──
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (index > 0) index-- else { /* resta */ } },
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (index < stories.lastIndex) index++ else onClose() },
            )
        }

        // ── Progress segmentata + header ──
        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                stories.forEachIndexed { i, _ ->
                    val frac = when {
                        i < index -> 1f
                        i > index -> 0f
                        else -> (elapsed.toFloat() / segmentMs).coerceIn(0f, 1f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(frac)
                                .background(Color.White),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarImage(
                    avatarUrl = current.author?.avatarUrl,
                    fallbackName = current.author?.username,
                    size = 34.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    current.author?.username ?: "Utente",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Chiudi", tint = Color.White)
                }
            }
        }

        // ── Overlay informativo in basso ──
        StoryOverlayInfo(
            story = current,
            onJoin = onJoin,
            onOpenSession = onOpenSession,
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun StoryOverlayInfo(
    story: StoryItem,
    onJoin: (inviteCode: String) -> Unit,
    onOpenSession: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val o = story.overlay
    Column(modifier = modifier) {
        // Chip tipo storia
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = (if (story.type == "planned_session") Color(0xFF4DD0E1) else Color(0xFF66BB6A)).copy(alpha = 0.2f),
        ) {
            Text(
                if (story.type == "planned_session") "ESCURSIONE PIANIFICATA" else "ATTIVITÀ",
                color = if (story.type == "planned_session") Color(0xFF4DD0E1) else Color(0xFF66BB6A),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        o?.title?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
        if (!story.caption.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(story.caption, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
        }

        // Stat chips
        if (o != null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                o.distanceMeters?.takeIf { it > 0 }?.let { StatChip("DISTANZA", formatKm(it)) }
                o.elevationGainM?.takeIf { it > 0 }?.let { StatChip("DISLIVELLO", "+$it m") }
                o.movingSeconds?.takeIf { it > 0 }?.let { StatChip("TEMPO", formatDur(it)) }
            }
        }

        // Azione: planned_session → Unisciti; activity → vedi sessione (se presente)
        Spacer(Modifier.height(14.dp))
        if (story.type == "planned_session" && !story.inviteCode.isNullOrBlank()) {
            Button(
                onClick = { onJoin(story.inviteCode) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1)),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Filled.Group, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("UNISCITI", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else if (!story.sessionId.isNullOrBlank()) {
            Button(
                onClick = { onOpenSession(story.sessionId) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("VEDI ESCURSIONE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp)
    }
}

/** Immagine da data URI Base64 (riusa il decoder degli avatar). */
@Composable
private fun StoryImage(media: StoryMedia, modifier: Modifier = Modifier) {
    val bitmap = remember(media.dataUri) { AvatarUtils.decodeDataUri(media.dataUri) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF101012)))
    }
}

/**
 * Video breve da data URI Base64: scriviamo i byte in un file di cache e li
 * riproduciamo con un VideoView in loop (niente dipendenze extra tipo ExoPlayer).
 */
@Composable
private fun StoryVideo(media: StoryMedia, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var file by remember(media.dataUri) { mutableStateOf<File?>(null) }
    LaunchedEffect(media.dataUri) {
        file = withContext(Dispatchers.IO) {
            runCatching {
                val base64 = media.dataUri.substringAfter("base64,", "")
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val f = File(context.cacheDir, "story_${media.dataUri.hashCode()}.mp4")
                f.writeBytes(bytes)
                f
            }.getOrNull()
        }
    }
    val f = file
    if (f != null) {
        AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    setVideoPath(f.absolutePath)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                    }
                }
            },
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF101012)))
    }
}

private fun formatKm(meters: Double): String {
    val km = meters / 1000.0
    return if (km >= 10) "${km.roundToInt()} km" else "%.1f km".format(km)
}

private fun formatDur(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
