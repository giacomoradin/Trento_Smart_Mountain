package it.trentosmartmountain.app.ui.screens.challenges

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import it.trentosmartmountain.app.ui.components.TsmSnackbar
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.dto.Challenge
import it.trentosmartmountain.app.data.remote.dto.ChallengeProgressItem
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.viewmodel.ChallengeDetailViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFE91E63)
private val TextSecondary = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    challengeId: String,
    onBack: () -> Unit,
    viewModel: ChallengeDetailViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val app = LocalContext.current.applicationContext as TsmApplication
    val myId = remember { app.tokenStorage.getToken()?.let { JwtDecoder.userIdFrom(it) } ?: "" }

    LaunchedEffect(challengeId) { viewModel.load(challengeId) }

    LaunchedEffect(state.operationMessage, state.error) {
        val msg = state.operationMessage ?: state.error
        if (!msg.isNullOrBlank()) { snackbar.showSnackbar(msg); viewModel.clearMessages() }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) {
    TsmAuroraBackground(modifier = Modifier.fillMaxSize(), particleCount = 12)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) { TsmSnackbar(it) } },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Sfida", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
            return@Scaffold
        }
        val detail = state.detail
        if (detail == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Caricamento fallito", color = AccentRed)
            }
            return@Scaffold
        }

        val ch = detail.challenge
        val isInvitedMe = ch.participants.find { it.user?.id == myId }?.status == "invited"

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderCard(ch)

            // Inviti pendenti per ME → bottoni accept/decline
            if (isInvitedMe && ch.status != "COMPLETED" && ch.status != "CANCELLED") {
                TsmGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    border = AccentCyan.copy(alpha = 0.5f),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Sei stato invitato a questa sfida", color = AccentCyan, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.respond(true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            ) { Text("ACCETTA", color = Color.White, fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = { viewModel.respond(false) },
                                modifier = Modifier.weight(1f),
                            ) { Text("RIFIUTA", color = Color.White) }
                        }
                    }
                }
            }

            Text("Classifica", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            // Ordinamento progress decrescente per visualizzare il leader
            val ranked = detail.progress.sortedByDescending { it.value }
            ranked.forEach { item ->
                val participant = ch.participants.find { it.user?.id == item.userId }
                ProgressRow(
                    username = participant?.user?.username ?: "Utente",
                    item = item,
                    target = ch.targetValue,
                    metric = ch.metric,
                    isMe = item.userId == myId,
                    isWinner = ch.winnerId == item.userId,
                )
            }
        }
    }
    }
}

@Composable
private fun HeaderCard(ch: Challenge) {
    TsmGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (!ch.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(ch.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${metricLabel(ch.metric)} · Stato: ${ch.status}",
                color = AccentCyan,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            ch.targetValue?.let {
                Text("Target: ${formatMetricValue(ch.metric, it)}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Dal ${ch.startDate.take(10)} al ${ch.endDate.take(10)}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ProgressRow(
    username: String,
    item: ChallengeProgressItem,
    target: Double?,
    metric: String,
    isMe: Boolean,
    isWinner: Boolean,
) {
    val color = when {
        isWinner -> Color(0xFFFFD700) // gold
        item.reachedTarget -> AccentGreen
        isMe -> AccentCyan
        else -> Color.White
    }
    val progress = if (target != null && target > 0) (item.value / target).toFloat().coerceIn(0f, 1f) else 0f
    TsmGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        border = if (isWinner || isMe) color.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = (if (isWinner) "🏆 " else "") + username + (if (isMe) " (tu)" else ""),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(formatMetricValue(metric, item.value), color = color, fontWeight = FontWeight.Bold)
            }
            if (target != null) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = Color(0xFF3A3A3C),
                )
            }
        }
    }
}
