package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.remote.dto.LeaderboardEntry
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.components.ListSkeleton
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.LeaderboardMetric
import it.trentosmartmountain.app.viewmodel.LeaderboardViewModel

private val DarkSurface = TsmColors.FeedBackground
private val CardBackground = TsmColors.CardElevated
private val AccentCyan = TsmColors.Cyan
private val TextSecondary = TsmColors.TextSecondary

/**
 * Classifica settimanale (ultimi 7 giorni) tra l'utente e i suoi seguiti.
 * Toggle metrica (Km / Dislivello / Punti) con ri-ordinamento client-side.
 * La riga del viewer è evidenziata; tap su una riga apre il profilo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    viewModel: LeaderboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) {
        it.trentosmartmountain.app.ui.components.TsmAuroraBackground(
            modifier = Modifier.matchParentSize(),
            particleCount = 14,
        )
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.leaderboard_title), color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.leaderboard_subtitle),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Toggle metrica
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricChip(stringResource(R.string.leaderboard_metric_km), state.metric == LeaderboardMetric.KM) { viewModel.setMetric(LeaderboardMetric.KM) }
                MetricChip(stringResource(R.string.leaderboard_metric_elev), state.metric == LeaderboardMetric.ELEVATION) { viewModel.setMetric(LeaderboardMetric.ELEVATION) }
                MetricChip(stringResource(R.string.leaderboard_metric_points), state.metric == LeaderboardMetric.POINTS) { viewModel.setMetric(LeaderboardMetric.POINTS) }
            }

            when {
                state.isLoading && state.items.isEmpty() -> ListSkeleton()
                state.error != null && state.items.isEmpty() -> Centered {
                    Text(state.error ?: "Errore", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                state.items.isEmpty() -> Centered {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Terrain,
                            contentDescription = null,
                            tint = AccentCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.leaderboard_empty),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                else -> {
                    val ranked = state.ranked
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ranked, key = { it.user?._id ?: it.hashCode().toString() }) { entry ->
                            val rank = ranked.indexOf(entry) + 1
                            LeaderboardRow(
                                rank = rank,
                                entry = entry,
                                metric = state.metric,
                                onClick = { entry.user?._id?.let(onUserClick) },
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun MetricChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AccentCyan,
            selectedLabelColor = DarkSurface,
            containerColor = CardBackground,
            labelColor = TextSecondary,
        ),
    )
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: LeaderboardEntry,
    metric: LeaderboardMetric,
    onClick: () -> Unit,
) {
    val highlight = entry.isMe
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) AccentCyan.copy(alpha = 0.12f) else CardBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Rank: medaglia "materica" per i primi 3 (cerchio metallico + glow),
            // numero semplice per gli altri.
            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                val medalColor = when (rank) {
                    1 -> Color(0xFFFFC729) // oro
                    2 -> Color(0xFFC0C8D4) // argento
                    3 -> Color(0xFFCD7F32) // bronzo
                    else -> null
                }
                if (medalColor != null) {
                    Box(contentAlignment = Alignment.Center) {
                        // Glow soft dietro la medaglia.
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        listOf(medalColor.copy(alpha = 0.45f), Color.Transparent),
                                    ),
                                    androidx.compose.foundation.shape.CircleShape,
                                ),
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        listOf(medalColor, medalColor.copy(alpha = 0.65f)),
                                    ),
                                    androidx.compose.foundation.shape.CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$rank",
                                color = Color(0xFF0B0B0B),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                } else {
                    Text("$rank", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
            AvatarImage(avatarUrl = entry.user?.avatarUrl, fallbackName = entry.user?.username ?: "?", size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (entry.isMe) stringResource(R.string.leaderboard_you_suffix, entry.user?.username ?: "Tu")
                    else entry.user?.username ?: "—",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    pluralStringResource(R.plurals.leaderboard_outings, entry.count, entry.count),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            // Valore metrica con count-up animato.
            val (targetVal, fmt) = when (metric) {
                LeaderboardMetric.KM -> entry.km.toFloat() to { v: Float ->
                    if (v >= 100) "%.0f km".format(v) else "%.1f km".format(v)
                }
                LeaderboardMetric.ELEVATION -> entry.elevM.toFloat() to { v: Float -> "%.0f m".format(v) }
                LeaderboardMetric.POINTS -> entry.points.toFloat() to { v: Float -> "%.0f pt".format(v) }
            }
            it.trentosmartmountain.app.ui.components.TsmAnimatedCounter(
                target = targetVal,
                format = fmt,
                color = AccentCyan,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { content() }
}
