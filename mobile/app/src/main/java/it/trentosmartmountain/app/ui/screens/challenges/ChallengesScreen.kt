package it.trentosmartmountain.app.ui.screens.challenges

import android.app.Application
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.Challenge
import it.trentosmartmountain.app.viewmodel.ChallengesViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentOrange = Color(0xFFFFB300)
private val AccentRed = Color(0xFFE91E63)
private val TextSecondary = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    onBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: ChallengesViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sfide", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = AccentCyan,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuova sfida", tint = DarkSurface)
            }
        },
        containerColor = DarkSurface,
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "", color = AccentRed)
            }
            state.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏔️", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Nessuna sfida attiva", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Tocca il + per crearne una", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(state.items, key = { it.id }) { ch ->
                    ChallengeListCard(ch, onClick = { onNavigateToDetail(ch.id) })
                }
                item { Spacer(Modifier.height(80.dp)) } // FAB clearance
            }
        }
    }
}

@Composable
private fun ChallengeListCard(challenge: Challenge, onClick: () -> Unit) {
    val statusColor = when (challenge.status) {
        "ACTIVE" -> AccentGreen
        "PENDING" -> AccentOrange
        "COMPLETED" -> AccentCyan
        else -> TextSecondary
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    challenge.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    challenge.status,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append(metricLabel(challenge.metric))
                    challenge.targetValue?.let { append(" · target ${formatMetricValue(challenge.metric, it)}") }
                    append(" · ${challenge.participants.size} partecipanti")
                },
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

internal fun metricLabel(metric: String): String = when (metric) {
    "distance" -> "Distanza"
    "elevation" -> "Dislivello"
    "count" -> "Numero escursioni"
    "points" -> "Punti"
    else -> metric
}

internal fun formatMetricValue(metric: String, value: Double): String = when (metric) {
    "distance" -> "%.1f km".format(value)
    "elevation" -> "%.0f m".format(value)
    "count" -> "%.0f".format(value)
    "points" -> "%.0f pt".format(value)
    else -> value.toString()
}
