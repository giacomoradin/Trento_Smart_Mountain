package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.trentosmartmountain.app.data.remote.dto.FeedItem
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.theme.difficultyColor
import it.trentosmartmountain.app.ui.util.RelativeTime
import java.util.Locale
import kotlin.math.roundToInt

// Colori della card derivati dalla palette centrale (vedi ui/theme/TsmPalette.kt).
private val CardBackground = TsmColors.Card
private val HeroTop = TsmColors.HeroTop
private val HeroBottom = TsmColors.HeroBottom
private val TextPrimary = TsmColors.TextPrimary
private val TextSecondary = TsmColors.TextTertiary
private val AccentRed = TsmColors.Danger
private val AccentCyan = TsmColors.Cyan
private val Divider = TsmColors.Divider

/**
 * Card del feed sociale — redesign **Strava-style** (Sprint 3).
 *
 * Anatomia (dall'alto):
 *  1. **Header atleta**: avatar + nome + meta ("3 h fa · Trail") + chip kind.
 *  2. **Titolo** + caption opzionale.
 *  3. **Hero visivo**:
 *       - se l'item ha una `routePolyline` → *route signature* ([RouteTracePreview])
 *         con chip difficoltà in overlay;
 *       - altrimenti, se ha un `elevationProfile` → quello diventa l'hero;
 *       - altrimenti nessun hero (card compatta).
 *  4. **Stat strip**: Distanza · Dislivello · Tempo · Passo (4 celle).
 *  5. **Banda altimetrica** sottile (solo se la route era già l'hero e c'è un
 *     profilo: evita di duplicare l'altimetria quando è già l'hero).
 *  6. **Partecipanti** (solo sessioni di gruppo).
 *  7. **Action bar**: like (ottimistico) + commenti + badge punti.
 *
 * La firma resta invariata rispetto alla versione precedente → nessun call site
 * da toccare (HomeSocialScreen, UserProfileScreen, dettagli).
 */
@Composable
fun FeedCard(
    item: FeedItem,
    onLikeToggle: () -> Unit,
    onCommentClick: () -> Unit = {},
    onUserClick: (userId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val route = item.routePolyline
    val hasRoute = route != null && route.size >= 2
    val profile = item.elevationProfile
    val hasProfile = profile != null && profile.size >= 2
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // ── 1. Header atleta ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 10.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = item.user?._id != null) {
                            item.user?._id?.let { onUserClick(it) }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarImage(
                        avatarUrl = item.user?.avatarUrl,
                        fallbackName = item.user?.username,
                        size = 44.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.user?.username ?: "Utente sconosciuto",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = buildMetaLine(item),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                KindChip(kind = item.kind)
            }

            Spacer(Modifier.height(12.dp))

            // ── 2. Titolo + caption ───────────────────────────────────────────
            Text(
                text = item.title ?: "Escursione",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            if (!item.caption.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.caption,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── 3. Hero visivo ────────────────────────────────────────────────
            when {
                hasRoute -> RouteHero(item = item, route = route!!)
                hasProfile -> ProfileHero(profile = profile!!)
                else -> Unit
            }

            // ── 4. Stat strip ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCell("DISTANZA", formatDistance(item.distanceMeters), Modifier.weight(1f))
                StatCell("DISLIVELLO", formatElevation(item.elevationGainM), Modifier.weight(1f))
                StatCell("TEMPO", formatDuration(item.movingSeconds), Modifier.weight(1f))
                StatCell(
                    "PASSO",
                    formatPace(item.distanceMeters, item.movingSeconds),
                    Modifier.weight(1f),
                )
            }

            // ── 5. Banda altimetrica (solo se la route era l'hero) ────────────
            if (hasRoute && hasProfile) {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(
                        "ALTIMETRIA",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    ElevationSparkline(
                        profile = profile!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        lineColor = AccentCyan,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── 6. Partecipanti (solo sessioni) ───────────────────────────────
            val participants = item.participants
            if (item.kind == "session" && !participants.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "PARTECIPANTI",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        participants.take(4).forEach { p ->
                            AvatarImage(
                                avatarUrl = p.avatarUrl,
                                fallbackName = p.username,
                                size = 24.dp,
                            )
                        }
                        if (participants.size > 4) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "+${participants.size - 4}",
                                    color = AccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── 7. Action bar ─────────────────────────────────────────────────
            Surface(modifier = Modifier.fillMaxWidth().height(1.dp), color = Divider) {}
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        // Feedback aptico solo quando si AGGIUNGE il like (gesto positivo),
                        // come Strava/Instagram: rende il tap sul cuore soddisfacente.
                        if (!item.likedByMe) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onLikeToggle()
                    },
                ) {
                    Icon(
                        imageVector = if (item.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (item.likedByMe) "Rimuovi like" else "Metti like",
                        tint = if (item.likedByMe) AccentRed else TextSecondary,
                    )
                }
                Text(
                    text = "${item.likesCount}",
                    color = if (item.likedByMe) AccentRed else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onCommentClick) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Commenta",
                        tint = TextSecondary,
                    )
                }
                Text(
                    text = "${item.commentsCount}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.weight(1f))
                if (item.finalPoints != null && item.finalPoints > 0) {
                    PointsBadge(points = item.finalPoints)
                }
            }
        }
    }
}

/** Hero con la route signature + chip difficoltà in overlay. */
@Composable
private fun RouteHero(item: FeedItem, route: List<it.trentosmartmountain.app.data.remote.dto.RoutePoint>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .background(Brush.verticalGradient(listOf(HeroTop, HeroBottom))),
    ) {
        RouteTracePreview(
            points = route,
            modifier = Modifier.fillMaxWidth().height(176.dp),
            lineColor = AccentCyan,
        )
        item.difficultyLevel?.let { diff ->
            DifficultyChip(
                level = diff,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
            )
        }
    }
}

/** Hero alternativo quando manca la route: il profilo altimetrico a tutta larghezza. */
@Composable
private fun ProfileHero(profile: List<Double>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(Brush.verticalGradient(listOf(HeroTop, HeroBottom))),
    ) {
        Text(
            "ALTIMETRIA",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        ElevationSparkline(
            profile = profile,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .padding(top = 24.dp),
            lineColor = AccentCyan,
        )
    }
}

@Composable
private fun KindChip(kind: String) {
    val (label, color) = when (kind) {
        "session" -> "GRUPPO" to AccentCyan
        else -> "LIBERA" to Color(0xFF66BB6A)
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun DifficultyChip(level: String, modifier: Modifier = Modifier) {
    val color = difficultyColor(level)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.9f),
        modifier = modifier,
    ) {
        Text(
            text = level,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun PointsBadge(points: Int) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFC107).copy(alpha = 0.16f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$points",
                color = Color(0xFFFFC107),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "pt",
                color = Color(0xFFFFC107).copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            value,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.5.sp,
        )
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

/** Meta-riga "tempo relativo · tipo" sotto lo username. */
private fun buildMetaLine(item: FeedItem): String {
    val rel = RelativeTime.long(item.sharedAt)
    val type = activityTypeLabel(item)
    return listOf(rel, type).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun activityTypeLabel(item: FeedItem): String {
    if (item.kind == "session") return "Escursione di gruppo"
    return when (item.activityType?.lowercase(Locale.ROOT)) {
        "trail" -> "Trail running"
        "skitouring" -> "Scialpinismo"
        "trekking" -> "Trekking"
        "hiking" -> "Escursione"
        else -> "Escursione"
    }
}

// ── Formatters ────────────────────────────────────────────────────────────

private fun formatDistance(meters: Double?): String {
    if (meters == null || meters <= 0) return "—"
    val km = meters / 1000.0
    return if (km >= 10) "${km.roundToInt()} km" else "%.1f km".format(km)
}

private fun formatElevation(elev: Int?): String {
    if (elev == null || elev <= 0) return "—"
    return "$elev m"
}

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return "—"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/**
 * Passo medio in min/km (stile Strava). Es. 720s su 1.2km → "10:00 /km".
 * Ritorna "—" se distanza o tempo non sono validi.
 */
private fun formatPace(meters: Double?, seconds: Long?): String {
    if (meters == null || meters <= 0 || seconds == null || seconds <= 0) return "—"
    val km = meters / 1000.0
    val secPerKm = (seconds / km).roundToInt()
    val mm = secPerKm / 60
    val ss = secPerKm % 60
    return "%d:%02d".format(mm, ss)
}
