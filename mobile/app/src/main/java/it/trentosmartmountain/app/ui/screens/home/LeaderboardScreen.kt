package it.trentosmartmountain.app.ui.screens.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.LeaderboardEntry
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.viewmodel.LeaderboardMetric
import it.trentosmartmountain.app.viewmodel.LeaderboardViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val TextSecondary = Color(0xFF8E8E93)

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

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Classifica settimanale", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Ultimi 7 giorni · tu e chi segui",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Toggle metrica
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricChip("Km", state.metric == LeaderboardMetric.KM) { viewModel.setMetric(LeaderboardMetric.KM) }
                MetricChip("Dislivello", state.metric == LeaderboardMetric.ELEVATION) { viewModel.setMetric(LeaderboardMetric.ELEVATION) }
                MetricChip("Punti", state.metric == LeaderboardMetric.POINTS) { viewModel.setMetric(LeaderboardMetric.POINTS) }
            }

            when {
                state.isLoading && state.items.isEmpty() -> Centered { CircularProgressIndicator(color = AccentCyan) }
                state.error != null && state.items.isEmpty() -> Centered {
                    Text(state.error ?: "Errore", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                state.items.isEmpty() -> Centered {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏔️", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nessuna attività questa settimana.\nEsci a camminare o segui altri escursionisti!",
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
            // Rank: medaglia per i primi 3, numero per gli altri.
            Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
                if (medal != null) {
                    Text(medal, style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("$rank", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
            AvatarImage(avatarUrl = entry.user?.avatarUrl, fallbackName = entry.user?.username ?: "?", size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (entry.isMe) "${entry.user?.username ?: "Tu"} (tu)" else entry.user?.username ?: "—",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "${entry.count} ${if (entry.count == 1) "uscita" else "uscite"}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                metricValue(entry, metric),
                color = AccentCyan,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun metricValue(entry: LeaderboardEntry, metric: LeaderboardMetric): String = when (metric) {
    LeaderboardMetric.KM -> if (entry.km >= 100) "%.0f km".format(entry.km) else "%.1f km".format(entry.km)
    LeaderboardMetric.ELEVATION -> "${entry.elevM} m"
    LeaderboardMetric.POINTS -> "${entry.points} pt"
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { content() }
}
