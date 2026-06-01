package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.FeedItem
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.components.TsmRouteMapPreview
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.theme.difficultyColor
import it.trentosmartmountain.app.ui.util.RelativeTime
import it.trentosmartmountain.app.viewmodel.PostDetailViewModel
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dettaglio "social" di un post (Activity/HikeSession condivisa).
 *
 * Riusa i dati già presenti nel [FeedItem] del feed — niente fetch — e li
 * rilegge in chiave più ricca/sociale: autore in evidenza, route signature
 * grande, profilo altimetrico, griglia metriche completa, partecipanti, e le
 * azioni like/commento inline. Aperto tappando una card del feed o della bacheca.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    item: FeedItem,
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit = {},
    viewModel: PostDetailViewModel = viewModel(),
) {
    LaunchedEffect(item.id) { viewModel.init(item) }
    val current by viewModel.item.collectAsStateWithLifecycle()
    val post = current ?: item
    var commentsTarget by remember { mutableStateOf<CommentsTarget?>(null) }

    val haptic = LocalHapticFeedback.current
    val likeScale = remember { Animatable(1f) }
    var likedPrev by remember { mutableStateOf(post.likedByMe) }
    LaunchedEffect(post.likedByMe) {
        if (post.likedByMe && !likedPrev) {
            likeScale.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            likeScale.animateTo(1f)
        }
        likedPrev = post.likedByMe
    }

    Scaffold(
        containerColor = TsmColors.FeedBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        post.user?.username ?: "Dettaglio",
                        color = TsmColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = TsmColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TsmColors.FeedBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
        ) {
            // ── Autore ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(enabled = post.user?._id != null) { post.user?._id?.let(onUserClick) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarImage(avatarUrl = post.user?.avatarUrl, fallbackName = post.user?.username, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.user?.username ?: "Utente sconosciuto",
                        color = TsmColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(metaLine(post), color = TsmColors.TextTertiary, style = MaterialTheme.typography.labelMedium)
                }
                KindChip(post.kind)
            }

            // ── Hero: route signature o profilo altimetrico ──
            val route = post.routePolyline
            val hasRoute = route != null && route.size >= 2
            val profile = post.elevationProfile
            val hasProfile = profile != null && profile.size >= 2
            when {
                hasRoute -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Brush.verticalGradient(listOf(TsmColors.HeroTop, TsmColors.HeroBottom))),
                ) {
                    TsmRouteMapPreview(points = route!!, modifier = Modifier.fillMaxSize())
                    post.difficultyLevel?.let {
                        DifficultyChip(it, Modifier.align(Alignment.TopStart).padding(12.dp))
                    }
                }
                hasProfile -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Brush.verticalGradient(listOf(TsmColors.HeroTop, TsmColors.HeroBottom))),
                ) {
                    ElevationSparkline(profile = profile!!, modifier = Modifier.fillMaxSize().padding(12.dp), lineColor = TsmColors.Cyan)
                }
            }

            // ── Titolo + caption ──
            Spacer(Modifier.height(14.dp))
            Text(
                post.title ?: "Escursione",
                color = TsmColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (!post.caption.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    post.caption,
                    color = TsmColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // ── Griglia metriche ──
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                color = TsmColors.Card,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCell("DISTANZA", formatDistance(post.distanceMeters), TsmColors.Cyan, Modifier.weight(1f))
                        StatCell("DISLIVELLO", formatElevation(post.elevationGainM), TsmColors.TextPrimary, Modifier.weight(1f))
                        StatCell("TEMPO", formatDuration(post.movingSeconds), TsmColors.TextPrimary, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCell("PASSO", formatPace(post.distanceMeters, post.movingSeconds), TsmColors.TextPrimary, Modifier.weight(1f))
                        StatCell("VEL. MEDIA", formatSpeed(post.distanceMeters, post.movingSeconds), Color(0xFFFF9800), Modifier.weight(1f))
                        StatCell("PUNTI", post.finalPoints?.let { "$it" } ?: "—", TsmColors.Gold, Modifier.weight(1f))
                    }
                }
            }

            // ── Profilo altimetrico (se la route era l'hero) ──
            if (hasRoute && hasProfile) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = TsmColors.Card,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PROFILO ALTIMETRICO", color = TsmColors.TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                            Text("+${post.elevationGainM ?: 0} m D+", color = TsmColors.Cyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(10.dp))
                        ElevationSparkline(profile = profile!!, modifier = Modifier.fillMaxWidth().height(90.dp), lineColor = TsmColors.Cyan)
                    }
                }
            }

            // ── Badge di performance (novità social) ──
            val badges = buildBadges(post)
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = TsmColors.Card,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("BADGE OTTENUTI", color = TsmColors.TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            badges.forEach { badge ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = CircleShape,
                                        color = badge.bgColor,
                                    ) {
                                        Box(contentAlignment = Alignment.Center) { Text(badge.emoji, fontSize = 22.sp) }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(badge.title, color = badge.textColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }

            // ── Timeline degli Split (ogni 5km) ──
            val timeline = buildTimeline(post)
            if (timeline.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = TsmColors.Card,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TIMELINE DI MARCIA", color = TsmColors.TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                        Spacer(Modifier.height(12.dp))
                        timeline.forEach { event ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(event.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(event.icon, null, tint = event.color, modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(event.label, color = TsmColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    event.subtitle?.let { Text(it, color = TsmColors.TextTertiary, style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                            if (event != timeline.last()) {
                                Box(modifier = Modifier.padding(start = 14.dp).size(1.dp, 12.dp).background(TsmColors.Divider))
                            }
                        }
                    }
                }
            }

            // ── Partecipanti (sessioni) ──
            val participants = post.participants
            if (post.kind == "session" && !participants.isNullOrEmpty()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = TsmColors.Card,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PARTECIPANTI · ${participants.size}", color = TsmColors.TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                        Spacer(Modifier.height(10.dp))
                        participants.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = p._id.isNotBlank()) { onUserClick(p._id) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AvatarImage(avatarUrl = p.avatarUrl, fallbackName = p.username, size = 32.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(p.username ?: "Utente", color = TsmColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // ── Azioni ──
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    if (!post.likedByMe) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleLike()
                }) {
                    Icon(
                        imageVector = if (post.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (post.likedByMe) "Rimuovi like" else "Metti like",
                        tint = if (post.likedByMe) TsmColors.Danger else TsmColors.TextSecondary,
                        modifier = Modifier.scale(likeScale.value),
                    )
                }
                Text("${post.likesCount}", color = if (post.likedByMe) TsmColors.Danger else TsmColors.TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { commentsTarget = CommentsTarget(post.id, post.kind) }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Commenta", tint = TsmColors.TextSecondary)
                }
                Text("${post.commentsCount}", color = TsmColors.TextSecondary)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    CommentsBottomSheet(
        target = commentsTarget,
        onDismiss = { commentsTarget = null },
        onCountChanged = { _, _, count -> viewModel.setCommentCount(count) },
    )
}

@Composable
private fun StatCell(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TsmColors.TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun KindChip(kind: String) {
    val (label, color) = when (kind) {
        "session" -> "GRUPPO" to TsmColors.Cyan
        else -> "LIBERA" to TsmColors.Online
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun DifficultyChip(level: String, modifier: Modifier = Modifier) {
    val color = difficultyColor(level)
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.9f), modifier = modifier) {
        Text(level, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

private fun metaLine(item: FeedItem): String {
    val rel = RelativeTime.long(item.sharedAt)
    val type = if (item.kind == "session") "Escursione di gruppo" else when (item.activityType?.lowercase(Locale.ROOT)) {
        "trail" -> "Trail running"
        "skitouring" -> "Scialpinismo"
        "trekking" -> "Trekking"
        else -> "Escursione"
    }
    return listOf(rel, type).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun formatDistance(meters: Double?): String {
    if (meters == null || meters <= 0) return "—"
    val km = meters / 1000.0
    return if (km >= 10) "${km.roundToInt()} km" else "%.1f km".format(km)
}

private fun formatElevation(elev: Int?): String =
    if (elev == null || elev <= 0) "—" else "$elev m"

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return "—"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatPace(meters: Double?, seconds: Long?): String {
    if (meters == null || meters <= 0 || seconds == null || seconds <= 0) return "—"
    val secPerKm = (seconds / (meters / 1000.0)).roundToInt()
    return "%d:%02d".format(secPerKm / 60, secPerKm % 60)
}

private fun formatSpeed(meters: Double?, seconds: Long?): String {
    if (meters == null || meters <= 0 || seconds == null || seconds <= 0) return "—"
    val kmh = (meters / 1000.0) / (seconds / 3600.0)
    return "%.1f km/h".format(kmh)
}

// ── Modelli e builder per il dettaglio social (Sprint 3) ──

private data class PostBadgeInfo(
    val emoji: String,
    val title: String,
    val textColor: Color,
    val bgColor: Color
)

private data class TimelineItem(
    val label: String,
    val subtitle: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

/** Calcola i badge di performance basati sulle statistiche del post (effetto social). */
private fun buildBadges(post: FeedItem): List<PostBadgeInfo> {
    val badges = mutableListOf<PostBadgeInfo>()
    val distKm = (post.distanceMeters ?: 0.0) / 1000.0
    val elevM = post.elevationGainM ?: 0
    val speedKmh = if ((post.movingSeconds ?: 0L) > 0) distKm / (post.movingSeconds!! / 3600.0) else 0.0

    // Badge Maratoneta / Esploratore per la distanza
    if (distKm >= 15) badges.add(PostBadgeInfo("🏔", "Maratoneta", Color(0xFF673AB7), Color(0xFFEDE7F6)))
    else if (distKm >= 8) badges.add(PostBadgeInfo("👟", "Esploratore", Color(0xFF4CAF50), Color(0xFFE8F5E9)))

    // Badge Scalatore / Vettista per il dislivello
    if (elevM >= 1000) badges.add(PostBadgeInfo("🧗", "Scalatore", Color(0xFFF44336), Color(0xFFFFEBEE)))
    else if (elevM >= 400) badges.add(PostBadgeInfo("⛰", "Vettista", Color(0xFF009688), Color(0xFFE0F2F1)))

    // Badge Velocista per la velocità media
    if (speedKmh > 5.5) badges.add(PostBadgeInfo("⚡", "Velocista", Color(0xFFFF9800), Color(0xFFFFF3E0)))

    return badges
}

/** Costruisce la timeline degli split km (ogni 5km) per un tocco tecnico Strava-like. */
private fun buildTimeline(post: FeedItem): List<TimelineItem> {
    val items = mutableListOf<TimelineItem>()
    val distKm = (post.distanceMeters ?: 0.0) / 1000.0
    val movingSec = post.movingSeconds ?: 0L

    items.add(TimelineItem("Partenza", "Inizio attività", Icons.Filled.Flag, TsmColors.Cyan))

    if (distKm > 5) {
        var km = 5.0
        while (km < distKm) {
            // Calcola il ritmo medio per simulare lo split
            val avgPacePerKm = if (distKm > 0) (movingSec / 60.0) / distKm else 0.0
            val mm = avgPacePerKm.toInt()
            val ss = ((avgPacePerKm - mm) * 60).toInt()
            val paceStr = "%d:%02d".format(mm, ss)
            
            items.add(TimelineItem("Split ${km.toInt()} km", "ritmo medio $paceStr/km", Icons.Filled.Timer, TsmColors.Gold))
            km += 5.0
        }
    }

    items.add(TimelineItem("Traguardo", "%.1f km totali completati".format(distKm), Icons.Filled.CheckCircle, TsmColors.Success))

    return items
}
