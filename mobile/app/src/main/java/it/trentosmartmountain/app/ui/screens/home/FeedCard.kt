package it.trentosmartmountain.app.ui.screens.home

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.trentosmartmountain.app.data.remote.dto.FeedItem
import it.trentosmartmountain.app.ui.components.AvatarImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

private val CardBackground = Color(0xFF2C2C2E)
private val TextSecondary = Color(0xFF8E8E93)
private val AccentRed = Color(0xFFFF6B6B)
private val AccentCyan = Color(0xFF4DD0E1)
private val Divider = Color(0xFF3A3A3C)

/**
 * Card del feed sociale. Layout fedele al mockup:
 *
 *  [avatar 40dp] [username + sharedAt relativo]      [chip kind]
 *  [titolo escursione (es. nome GPX o nome attività libera)]
 *  [caption opzionale]
 *  [KPI strip: km, durata, dislivello, punti]
 *  [partecipanti (solo per session): "+3" overlay se >3]
 *  ───────────
 *  [like icon] [count] [comment icon] [count]
 *
 * Il `kind` distingue Activity (libere) da HikeSession (gruppo). Per le sessioni
 * mostriamo anche la riga partecipanti; per le attività libere no.
 *
 * Tap sull'icona cuore → `onLikeToggle()` (ottimistico nel VM).
 * Tap su commenti → `onCommentClick()` (placeholder Sprint 3 fino a UI commenti).
 * Tap sull'avatar → `onUserClick(user._id)` (apre profilo utente, ora no-op).
 */
@Composable
fun FeedCard(
    item: FeedItem,
    onLikeToggle: () -> Unit,
    onCommentClick: () -> Unit = {},
    onUserClick: (userId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: avatar + username (clickabili) + chip kind a destra ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Area clickabile per il tap-su-autore: comprende avatar + nome
                // così il target di tocco è abbondante (avatar 40dp da solo
                // è ai limiti delle linee guida M3, l'username è una bella aggiunta).
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
                        size = 40.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.user?.username ?: "Utente sconosciuto",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formatSharedAt(item.sharedAt),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                KindChip(kind = item.kind)
            }

            Spacer(Modifier.height(12.dp))

            // ── Titolo attività ──
            Text(
                text = item.title ?: "Escursione",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
            )

            // ── Caption (opzionale) ──
            if (!item.caption.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.caption,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── KPI strip ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                KpiBlock(label = "DISTANZA", value = formatDistance(item.distanceMeters))
                KpiBlock(label = "DURATA", value = formatDuration(item.movingSeconds))
                KpiBlock(label = "D+", value = formatElevation(item.elevationGainM))
                if (item.finalPoints != null && item.finalPoints > 0) {
                    KpiBlock(label = "PT", value = "${item.finalPoints}")
                }
            }

            // ── Partecipanti (solo sessioni) ──
            val participants = item.participants
            if (item.kind == "session" && !participants.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(top = 2.dp),
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
            }

            Spacer(Modifier.height(12.dp))

            // ── Divider + actions ──
            Surface(
                modifier = Modifier.fillMaxWidth().height(1.dp),
                color = Divider,
            ) {}
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeToggle) {
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
                Spacer(Modifier.width(16.dp))
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
            }
        }
    }
}

@Composable
private fun KindChip(kind: String) {
    val (label, color) = when (kind) {
        "session" -> "GRUPPO" to AccentCyan
        else -> "LIBERA" to Color(0xFF4CAF50)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
    ) {
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
private fun KpiBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp)
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
    return "${elev} m"
}

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return "—"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/**
 * Formato "tempo trascorso" leggibile: "ora", "5 min fa", "2 h fa", "3 g fa",
 * altrimenti data assoluta dd/MM. Niente librerie esterne — bastano gli intervals.
 */
private fun formatSharedAt(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val sdfMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = runCatching {
        // Tronca millis e Z per parser robusto (accetta entrambi i formati).
        val trimmed = iso.removeSuffix("Z").take(23)
        if (trimmed.length > 19) sdfMs.parse(trimmed) else sdf.parse(trimmed.take(19))
    }.getOrNull() ?: return ""
    val deltaMs = System.currentTimeMillis() - date.time
    val seconds = deltaMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "ora"
        minutes < 60 -> "${minutes} min fa"
        hours < 24 -> "${hours} h fa"
        days < 7 -> "${days} g fa"
        else -> SimpleDateFormat("dd/MM", Locale.ITALIAN).format(date)
    }
}
