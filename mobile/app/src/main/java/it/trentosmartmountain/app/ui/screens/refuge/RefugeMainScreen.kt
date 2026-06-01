package it.trentosmartmountain.app.ui.screens.refuge

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.EdgeNodeDto
import it.trentosmartmountain.app.data.remote.dto.PassageDto
import it.trentosmartmountain.app.data.remote.dto.RefugeSensorsDto
import it.trentosmartmountain.app.viewmodel.ProfileViewModel
import it.trentosmartmountain.app.viewmodel.RefugeDashboardViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

private val Bg = Color(0xFF0D0D0F)
private val CardBg = Color(0xFF161618)
private val CardBorder = Color(0xFF242428)
private val Cyan = Color(0xFF29B6F6)
private val Peach = Color(0xFFE0A87E)
private val WindGreen = Color(0xFF9CCC65)
private val OnlineGreen = Color(0xFF4CAF50)
private val OfflineRed = Color(0xFFE53935)
private val TextSecondary = Color(0xFF8E8E93)
private val TextDim = Color(0xFF6B6B70)

/**
 * Shell account rifugio: **Dashboard IoT** (mockup) — sensori ambientali,
 * edge nodes BLE-mesh, passaggi/social-credit della giornata. Dati dal backend
 * (`/api/v1/refuge/dashboard`, generati lato server in attesa dell'ingest MQTT).
 */
@Composable
fun RefugeMainScreen(
  onLoggedOut: () -> Unit,
  onNavigateToBoard: () -> Unit = {},
  modifier: Modifier = Modifier,
  dashboardViewModel: RefugeDashboardViewModel = viewModel(
    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
      LocalContext.current.applicationContext as Application,
    ),
  ),
  profileViewModel: ProfileViewModel = viewModel(
    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
      LocalContext.current.applicationContext as Application,
    ),
  ),
) {
  val state by dashboardViewModel.state.collectAsStateWithLifecycle()

  Surface(modifier = modifier.fillMaxSize(), color = Bg) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp),
    ) {
      Spacer(Modifier.height(24.dp))
      DashboardHeader(
        refugeName = state.data?.refuge?.name ?: "Rifugio",
        altitudeM = state.data?.refuge?.altitudeM,
        live = state.data?.live == true,
      )
      Spacer(Modifier.height(16.dp))

      when {
        state.isLoading && state.data == null -> {
          Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Cyan)
          }
        }
        state.data == null -> {
          Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Dashboard non disponibile.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { dashboardViewModel.refresh() }) { Text("Riprova", color = Cyan) }
          }
        }
        else -> {
          val d = state.data!!
          SensorsSection(d.sensors)
          Spacer(Modifier.height(20.dp))
          EdgeNodesSection(d.edgeNodes, d.edgeNodesOnline, d.edgeNodesTotal)
          Spacer(Modifier.height(20.dp))
          PassagesSection(d.passages.totalCreditsToday, d.passages.items)
        }
      }

      Spacer(Modifier.height(24.dp))
      Button(
        onClick = onNavigateToBoard,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Cyan),
      ) {
        Text("Gestisci bacheca", color = Color(0xFF0D0D0F), fontWeight = FontWeight.Bold)
      }
      Spacer(Modifier.height(12.dp))
      OutlinedButton(
        onClick = { profileViewModel.logout(onLoggedOut) },
        modifier = Modifier.fillMaxWidth(),
      ) { Text("Esci") }
      Spacer(Modifier.height(24.dp))
    }
  }
}

// ── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(refugeName: String, altitudeM: Int?, live: Boolean) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      val alt = altitudeM?.let { " · ${"%,d".format(it).replace(",", ".")} M" } ?: ""
      Text(
        "${refugeName.uppercase()}$alt",
        color = TextSecondary,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        "Dashboard IoT",
        color = Color.White,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      )
    }
    if (live) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = OnlineGreen.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, OnlineGreen.copy(alpha = 0.5f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(Modifier.size(8.dp).clip(CircleShape).background(OnlineGreen))
          Spacer(Modifier.width(6.dp))
          Text("LIVE", color = OnlineGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

// ── Sensori ────────────────────────────────────────────────────────────────

@Composable
private fun SensorsSection(sensors: RefugeSensorsDto?) {
  SectionLabel("SENSORI EDGE NODES · 4 ATTIVI")
  Spacer(Modifier.height(10.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    SensorCard(
      modifier = Modifier.weight(1f),
      label = "TEMPERATURA EST.",
      icon = Icons.Outlined.Thermostat,
      iconTint = Cyan,
      value = sensors?.temperature?.value?.let { "%.1f".format(it) } ?: "—",
      valueColor = Cyan,
      unit = "°C",
      sub = trendText(sensors?.temperature?.trend),
    )
    SensorCard(
      modifier = Modifier.weight(1f),
      label = "UMIDITÀ",
      icon = Icons.Outlined.WaterDrop,
      iconTint = Peach,
      value = sensors?.humidity?.value?.roundToInt()?.toString() ?: "—",
      valueColor = Peach,
      unit = "%",
      sub = trendText(sensors?.humidity?.trend),
    )
  }
  Spacer(Modifier.height(12.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    SensorCard(
      modifier = Modifier.weight(1f),
      label = "VENTO",
      icon = Icons.Outlined.Air,
      iconTint = WindGreen,
      value = sensors?.wind?.value?.roundToInt()?.toString() ?: "—",
      valueColor = WindGreen,
      unit = "km/h ${sensors?.wind?.dir ?: ""}".trim(),
      sub = sensors?.wind?.gust?.let { "raff. ${it.roundToInt()}" },
    )
    SensorCard(
      modifier = Modifier.weight(1f),
      label = "PRESSIONE",
      icon = Icons.Outlined.Speed,
      iconTint = Cyan,
      value = sensors?.pressure?.value?.roundToInt()?.toString() ?: "—",
      valueColor = Cyan,
      unit = "hPa",
      sub = trendText(sensors?.pressure?.trend),
    )
  }
}

@Composable
private fun SensorCard(
  modifier: Modifier,
  label: String,
  icon: ImageVector,
  iconTint: Color,
  value: String,
  valueColor: Color,
  unit: String,
  sub: String?,
) {
  Surface(
    modifier = modifier.height(150.dp),
    shape = RoundedCornerShape(14.dp),
    color = CardBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
          label,
          color = TextSecondary,
          style = MaterialTheme.typography.labelSmall,
          letterSpacing = 0.5.sp,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
      }
      Spacer(Modifier.weight(1f))
      Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 34.sp)
      Text(unit, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
      if (sub != null) {
        Spacer(Modifier.height(4.dp))
        Text(sub, color = TextDim, style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

// ── Edge nodes ───────────────────────────────────────────────────────────────

@Composable
private fun EdgeNodesSection(nodes: List<EdgeNodeDto>, online: Int, total: Int) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = CardBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
  ) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          "EDGE NODES BLE-MESH",
          color = TextSecondary,
          style = MaterialTheme.typography.labelMedium,
          letterSpacing = 1.sp,
          modifier = Modifier.weight(1f),
        )
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = OnlineGreen.copy(alpha = 0.12f),
          border = androidx.compose.foundation.BorderStroke(1.dp, OnlineGreen.copy(alpha = 0.4f)),
        ) {
          Text(
            "$online/$total ONLINE",
            color = OnlineGreen,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
      nodes.forEachIndexed { i, node ->
        if (i > 0) Divider()
        EdgeNodeRow(node)
      }
    }
  }
}

@Composable
private fun EdgeNodeRow(node: EdgeNodeDto) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(if (node.online) OnlineGreen else OfflineRed))
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(node.code, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
      Text(node.name, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
    Column(horizontalAlignment = Alignment.End) {
      if (node.online) {
        Text("${node.signalPct}%", color = Cyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text("segnale", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
      } else {
        Text("OFFLINE", color = TextSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Text(lastSeenText(node.lastSeenAt), color = TextDim, style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

// ── Passaggi oggi ────────────────────────────────────────────────────────────

@Composable
private fun PassagesSection(totalCredits: Int, items: List<PassageDto>) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = CardBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
  ) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          "PASSAGGI OGGI · SOCIAL CREDITS",
          color = TextSecondary,
          style = MaterialTheme.typography.labelMedium,
          letterSpacing = 1.sp,
          modifier = Modifier.weight(1f),
        )
        Text(
          "+${"%,d".format(totalCredits).replace(",", ".")}",
          color = Cyan,
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium,
        )
      }
      if (items.isEmpty()) {
        Text(
          "Nessun passaggio registrato oggi.",
          color = TextSecondary,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
      } else {
        items.forEachIndexed { i, p ->
          if (i > 0) Divider()
          PassageRow(p)
        }
      }
    }
  }
}

@Composable
private fun PassageRow(p: PassageDto) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.size(40.dp).clip(CircleShape).background(avatarColor(p.displayName)),
      contentAlignment = Alignment.Center,
    ) {
      Text(initials(p.displayName), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(p.displayName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
      Text(
        "passato alle ${formatTime(p.passedAt)} · via ${p.via ?: "mesh"}",
        color = TextSecondary,
        style = MaterialTheme.typography.bodySmall,
      )
    }
    Text("+${p.credits}", color = Cyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
  }
}

// ── Helpers UI ───────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
  Text(text, color = TextSecondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
}

@Composable
private fun Divider() {
  Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(CardBorder))
}

private fun trendText(trend: Double?): String? {
  if (trend == null || trend == 0.0) return null
  val arrow = if (trend > 0) "↑" else "↓"
  val a = abs(trend)
  val n = if (a == a.toLong().toDouble()) "${a.toLong()}" else "%.1f".format(a)
  return "$arrow $n"
}

private fun initials(name: String): String =
  name.trim().split(" ").filter { it.isNotBlank() }.take(2)
    .joinToString("") { it.first().uppercaseChar().toString() }

private val AVATAR_COLORS = listOf(
  Color(0xFF8D6E63), Color(0xFF43A047), Color(0xFF5C6BC0),
  Color(0xFFEF6C00), Color(0xFF00897B), Color(0xFFAD1457),
)

private fun avatarColor(name: String): Color =
  AVATAR_COLORS[(abs(name.hashCode())) % AVATAR_COLORS.size]

private fun formatTime(iso: String?): String {
  if (iso.isNullOrBlank()) return "—"
  return runCatching {
    val cleaned = iso.substringBefore(".").removeSuffix("Z").take(19)
    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
      .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
    val date = parser.parse(cleaned)!!
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.ITALIAN).format(date)
  }.getOrDefault("—")
}

private fun lastSeenText(iso: String?): String {
  if (iso.isNullOrBlank()) return "offline"
  val ms = runCatching {
    val cleaned = iso.substringBefore(".").removeSuffix("Z").take(19)
    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
      .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
      .parse(cleaned)!!.time
  }.getOrNull() ?: return "offline"
  val diffMin = ((System.currentTimeMillis() - ms) / 60000L).coerceAtLeast(0)
  return when {
    diffMin < 60 -> "ult: ${diffMin}m fa"
    diffMin < 1440 -> "ult: ${diffMin / 60}h fa"
    else -> "ult: ${diffMin / 1440}g fa"
  }
}
