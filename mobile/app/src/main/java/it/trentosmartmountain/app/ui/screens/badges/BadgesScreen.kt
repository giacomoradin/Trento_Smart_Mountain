package it.trentosmartmountain.app.ui.screens.badges

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.BadgeItem
import it.trentosmartmountain.app.data.remote.dto.CertificateItem
import it.trentosmartmountain.app.viewmodel.BadgesViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentGold = Color(0xFFFFD700)
private val TextSecondary = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    onBack: () -> Unit,
    viewModel: BadgesViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val earnedCount = state.badges.count { it.earned }
    val totalCount = state.badges.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bacheca", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
        },
        containerColor = DarkSurface,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Stats header
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatPair("BADGE", "$earnedCount / $totalCount", AccentCyan)
                        StatPair("CERTIFICATI", "${state.certificates.size}", AccentGold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Certificati ──────────────────────────────────────────────
            if (state.certificates.isNotEmpty()) {
                item {
                    Text(
                        "Certificati",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                items(state.certificates, key = { it.categorySlug }) { cert ->
                    CertificateCard(cert)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Badges ────────────────────────────────────────────────────
            item {
                Text(
                    "Badge",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            // Sorted: earned prima (per dare gratifica visiva), poi non-earned per tier.
            val sorted = state.badges.sortedWith(
                compareByDescending<BadgeItem> { it.earned }
                    .thenBy { tierOrder(it.tier) },
            )
            items(sorted, key = { it.code }) { badge ->
                BadgeCard(badge)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatPair(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    }
}

@Composable
private fun BadgeCard(badge: BadgeItem) {
    val tierColor = when (badge.tier) {
        "bronze" -> Color(0xFFCD7F32)
        "silver" -> Color(0xFFC0C0C0)
        "gold" -> AccentGold
        "platinum" -> Color(0xFFE5E4E2)
        else -> TextSecondary
    }
    // Non-earned: opacity ridotta + grayscale visivo (icona resta a colori ma il
    // testo principale viene attenuato per renderlo "locked").
    val alpha = if (badge.earned) 1f else 0.45f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = if (badge.earned) androidx.compose.foundation.BorderStroke(1.dp, tierColor) else null,
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(badge.emoji, fontSize = 32.sp, modifier = Modifier.alpha(alpha))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        badge.name,
                        color = if (badge.earned) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        badge.tier.uppercase(),
                        color = tierColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    badge.description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (badge.earned && badge.earnedAt != null) {
                    Text(
                        "Sbloccato il ${badge.earnedAt.take(10)}",
                        color = tierColor,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificateCard(cert: CertificateItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A3A)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📜", fontSize = 32.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Certificato — ${cert.categoryName}",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Hai superato tutti i ${cert.totalQuizzes} quiz della categoria",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Rilasciato il ${cert.issuedAt.take(10)}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun tierOrder(tier: String): Int = when (tier) {
    "bronze" -> 0
    "silver" -> 1
    "gold" -> 2
    "platinum" -> 3
    else -> 99
}
