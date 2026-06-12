package it.trentosmartmountain.app.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.dto.StoryItem
import it.trentosmartmountain.app.data.remote.dto.StoryMedia
import it.trentosmartmountain.app.data.remote.dto.StoryViewerLaunchContext
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.components.tsmNavigationBarPadding
import it.trentosmartmountain.app.ui.components.tsmStatusBarPadding
import it.trentosmartmountain.app.ui.util.AvatarUtils
import it.trentosmartmountain.app.ui.screens.home.story.StoryViewerDecorations
import it.trentosmartmountain.app.viewmodel.StoryViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private const val IMAGE_SEGMENT_MS = 5000L

@Composable
fun StoryViewerScreen(
    userId: String,
    launchContext: StoryViewerLaunchContext? = null,
    onClose: () -> Unit,
    onOpenSession: (sessionId: String) -> Unit = {},
    viewModel: StoryViewerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as TsmApplication
    val myUserId = remember {
        JwtDecoder.userIdFrom(app.tokenStorage.getToken().orEmpty())
    }

    val queue = remember(userId, launchContext) {
        val fromCtx = launchContext?.userIds?.filter { it.isNotBlank() }.orEmpty()
        when {
            fromCtx.isEmpty() -> listOf(userId)
            fromCtx.contains(userId) -> fromCtx
            else -> listOf(userId) + fromCtx
        }
    }
    val initialUserIndex = remember(userId, launchContext) {
        launchContext?.startIndex?.takeIf { queue.isNotEmpty() }
            ?: queue.indexOf(userId).coerceAtLeast(0)
    }

    var userIndex by remember(userId, queue) { mutableIntStateOf(initialUserIndex.coerceIn(queue.indices)) }
    var openAtLastStory by remember { mutableStateOf(false) }
    val currentUserId = queue.getOrNull(userIndex) ?: userId

    LaunchedEffect(currentUserId) { viewModel.load(currentUserId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.joinInfo) {
        state.joinInfo?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearJoinInfo()
        }
    }
    LaunchedEffect(state.deleteInfo) {
        state.deleteInfo?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearDeleteInfo()
        }
    }

    fun goToNextUser() {
        if (userIndex < queue.lastIndex) {
            userIndex++
        } else {
            onClose()
        }
    }

    fun goToPrevUser() {
        if (userIndex > 0) {
            openAtLastStory = true
            userIndex--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(queue, userIndex) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragCancel = { drag = 0f },
                    onDragEnd = {
                        val threshold = 72.dp.toPx()
                        when {
                            drag <= -threshold -> goToNextUser()
                            drag >= threshold -> goToPrevUser()
                        }
                        drag = 0f
                    },
                ) { _, amount -> drag += amount }
            },
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            state.stories.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        state.error ?: "Nessuna storia disponibile",
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (userIndex > 0) {
                            TextButton(onClick = { goToPrevUser() }) {
                                Text("Precedente", color = Color.White)
                            }
                        }
                        if (userIndex < queue.lastIndex) {
                            TextButton(onClick = { goToNextUser() }) {
                                Text("Successivo", color = Color.White)
                            }
                        }
                        Button(onClick = onClose) { Text("Chiudi") }
                    }
                }
            }
            else -> StoryPager(
                stories = state.stories,
                myUserId = myUserId,
                isDeleting = state.isDeleting,
                openAtLastStory = openAtLastStory,
                onConsumedOpenAtLast = { openAtLastStory = false },
                onClose = onClose,
                onMarkViewed = viewModel::markViewed,
                onJoin = viewModel::joinSession,
                onOpenSession = onOpenSession,
                onDeleteStory = viewModel::deleteStory,
                onFinishedLastStory = { goToNextUser() },
                onPrevAtFirstStory = { goToPrevUser() },
            )
        }
    }
}

@Composable
private fun StoryPager(
    stories: List<StoryItem>,
    myUserId: String?,
    isDeleting: Boolean,
    openAtLastStory: Boolean,
    onConsumedOpenAtLast: () -> Unit,
    onClose: () -> Unit,
    onMarkViewed: (String) -> Unit,
    onJoin: (inviteCode: String) -> Unit,
    onOpenSession: (sessionId: String) -> Unit,
    onDeleteStory: (String, () -> Unit) -> Unit,
    onFinishedLastStory: () -> Unit,
    onPrevAtFirstStory: () -> Unit,
) {
    var index by remember(stories) { mutableIntStateOf(0) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(stories, openAtLastStory) {
        index = if (openAtLastStory && stories.isNotEmpty()) {
            onConsumedOpenAtLast()
            stories.lastIndex
        } else {
            0
        }
    }

    // Se la lista si accorcia (es. elimina storia), evita IndexOutOfBounds.
    LaunchedEffect(stories.size) {
        if (stories.isNotEmpty() && index > stories.lastIndex) {
            index = stories.lastIndex
        }
    }

    val safeIndex = index.coerceIn(0, (stories.size - 1).coerceAtLeast(0))
    if (safeIndex != index) index = safeIndex
    val current = stories.getOrNull(safeIndex) ?: return
    val media = current.media.firstOrNull()
    val isOwnStory = !myUserId.isNullOrBlank() && current.author?._id == myUserId

    val segmentMs = remember(index, media) {
        if (media?.kind == "video") {
            ((media.durationSec ?: 8.0) * 1000).toLong().coerceIn(3000L, 10000L)
        } else IMAGE_SEGMENT_MS
    }

    var elapsed by remember(current.id) { mutableStateOf(0L) }
    LaunchedEffect(current.id, segmentMs) {
        onMarkViewed(current.id)
        elapsed = 0L
        while (elapsed < segmentMs) {
            delay(50L)
            elapsed += 50L
        }
        val pos = stories.indexOfFirst { it.id == current.id }
        if (pos < 0) return@LaunchedEffect
        if (pos < stories.lastIndex) {
            index = pos + 1
        } else {
            onFinishedLastStory()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminare la storia?", color = Color.White) },
            text = { Text("La storia verrà rimossa per tutti i follower.", color = Color.Gray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteStory(current.id) {
                            if (stories.size <= 1) onClose()
                            else if (index >= stories.lastIndex) index = (index - 1).coerceAtLeast(0)
                        }
                    },
                    enabled = !isDeleting,
                ) { Text("Elimina", color = Color(0xFFFF5252)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annulla", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2C2C2E),
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (media != null) {
            when (media.kind) {
                "video" -> StoryVideo(media = media, modifier = Modifier.fillMaxSize())
                else -> StoryImage(media = media, modifier = Modifier.fillMaxSize())
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1B1B1F), Color(0xFF101012)))),
            )
        }

        val decor = current.overlay?.editorDecor
        val routePts = current.overlay?.routePolyline.orEmpty()
        when {
            // Decor presente → overlay LIVE (mappa con frecce animate, traccia,
            // testo) sopra il media; anche per le storie solo-testo.
            decor != null -> {
                StoryViewerDecorations(
                    decor = decor,
                    routePoints = routePts,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            decor == null && routePts.size >= 2 && media?.kind != "image" -> {
                RouteTracePreview(
                    points = routePts,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(24.dp),
                    lineColor = Color.White,
                )
            }
        }

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

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (index > 0) index-- else onPrevAtFirstStory()
                    },
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (index < stories.lastIndex) index++
                        else onFinishedLastStory()
                    },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tsmStatusBarPadding()
                .padding(top = 12.dp),
        ) {
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
                if (isOwnStory) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        enabled = !isDeleting,
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Elimina storia", tint = Color.White)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Chiudi", tint = Color.White)
                }
            }
        }

        StoryOverlayInfo(
            story = current,
            onJoin = onJoin,
            onOpenSession = onOpenSession,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .tsmNavigationBarPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
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

        if (o != null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                o.distanceMeters?.takeIf { it > 0 }?.let { StatChip("DISTANZA", formatKm(it)) }
                o.elevationGainM?.takeIf { it > 0 }?.let { StatChip("DISLIVELLO", "+$it m") }
                o.movingSeconds?.takeIf { it > 0 }?.let { StatChip("TEMPO", formatDur(it)) }
            }
        }

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

@Composable
private fun StoryImage(media: StoryMedia, modifier: Modifier = Modifier) {
    val decoded by produceState<android.graphics.Bitmap?>(initialValue = null, media.dataUri) {
        value = withContext(Dispatchers.Default) {
            AvatarUtils.decodeDataUri(media.dataUri)
        }
    }
    val bmp = decoded
    if (bmp != null) {
        androidx.compose.foundation.Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF101012)))
    }
}

@Composable
private fun StoryVideo(media: StoryMedia, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var file by remember(media.dataUri) { mutableStateOf<File?>(null) }
    var failed by remember(media.dataUri) { mutableStateOf(false) }
    LaunchedEffect(media.dataUri) {
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                val base64 = media.dataUri.substringAfter("base64,", "")
                if (base64.isBlank()) return@runCatching null
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                if (bytes.isEmpty()) return@runCatching null
                val f = File(context.cacheDir, "story_${media.dataUri.hashCode()}.mp4")
                f.writeBytes(bytes)
                f
            }.getOrNull()
        }
        file = decoded
        failed = decoded == null
    }
    val f = file
    when {
        failed -> Box(modifier = modifier.background(Color(0xFF101012)), contentAlignment = Alignment.Center) {
            Text("Video non disponibile", color = Color.White.copy(alpha = 0.6f))
        }
        f != null -> AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    setOnErrorListener { _, _, _ -> true }
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(1f, 1f)
                        start()
                    }
                    setVideoPath(f.absolutePath)
                }
            },
            modifier = modifier,
            onRelease = { it.stopPlayback() },
        )
        else -> Box(modifier = modifier.background(Color(0xFF101012)))
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
