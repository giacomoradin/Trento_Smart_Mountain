package it.trentosmartmountain.app.ui.screens.home

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.estimation.HikeEstimation
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.ui.theme.TsmSurfaceVariant
import it.trentosmartmountain.app.viewmodel.ActivityListViewModel
import it.trentosmartmountain.app.viewmodel.ActivitySort
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val MONTH_LABELS = listOf("G", "F", "M", "A", "M", "G", "L", "A", "S", "O", "N", "D")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityListScreen(
    onActivityClick: (activityId: String, sessionId: String?) -> Unit = { _, _ -> },
    viewModel: ActivityListViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear downTo currentYear - 4).toList()
    val pagerState = rememberPagerState(initialPage = 0) { years.size }

    // Refresh esplicito ogni volta che la schermata entra in composizione
    // (es. utente torna da RegistraScreen dopo aver salvato un'attività).
    LaunchedEffect(Unit) { viewModel.onTabEntered() }

    // Quando swipe card, aggiorna anno selezionato e carica stats
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onYearChanged(years[page])
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(TsmBackground),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── Header ──
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    "LE MIE ATTIVITÀ",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = Color.Gray,
                )
                Text(
                    "${uiState.filteredActivities.size} escursioni",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }

        // ── Yearly Stats Cards (swipeable) ──
        item {
            val stats = uiState.yearlyStats[uiState.selectedYear]

            Column {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 12.dp,
                ) { page ->
                    val year = years[page]
                    val s = uiState.yearlyStats[year]
                    YearlyStatsCard(
                        year = year,
                        totalActivities = s?.totalActivities ?: 0,
                        totalDistanceKm = s?.totalDistanceKm ?: 0.0,
                        totalElevationM = s?.totalElevationGainM ?: 0,
                        totalPoints = s?.totalPoints ?: 0,
                        isLoading = uiState.statsLoading && s == null,
                    )
                }

                // Page indicator dots
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    years.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (pagerState.currentPage == i) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == i) TsmAccent else Color(0xFF3A3A3A)),
                        )
                    }
                }
            }
        }

        // ── Monthly Bar Chart ──
        item {
            val stats = uiState.yearlyStats[uiState.selectedYear]
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = TsmSurface,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ATTIVITÀ PER MESE · ${uiState.selectedYear}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = Color.Gray,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (stats != null) {
                        MonthlyBarChart(
                            counts = stats.monthlyActivityCount,
                            avgDifficulties = stats.monthlyAvgDifficulty,
                            selectedMonth = uiState.selectedMonth,
                            onMonthClick = { m -> viewModel.onMonthFilter(if (uiState.selectedMonth == m) null else m) },
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            if (uiState.statsLoading) CircularProgressIndicator(color = TsmAccent, modifier = Modifier.size(24.dp))
                            else Text("Nessun dato per questo anno", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // ── Sort filter chips ──
        item {
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val options = listOf(
                    ActivitySort.MOST_RECENT to "Più recente",
                    ActivitySort.OLDEST to "Più vecchia",
                    ActivitySort.ALPHABETICAL to "A-Z",
                    ActivitySort.DISTANCE to "Distanza",
                    ActivitySort.DIFFICULTY to "Difficoltà",
                    ActivitySort.DURATION to "Durata",
                )
                items(options) { (sort, label) ->
                    FilterChip(
                        selected = uiState.sort == sort,
                        onClick = { viewModel.onSortChanged(sort) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TsmPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = TsmSurfaceVariant,
                            labelColor = Color.Gray,
                        ),
                    )
                }
            }
        }

        // ── Activity List ──
        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TsmAccent)
                }
            }
        } else if (uiState.filteredActivities.isEmpty()) {
            item { EmptyActivitiesState() }
        } else {
            items(uiState.filteredActivities, key = { it.id }) { activity ->
                ActivityListItem(
                    activity = activity,
                    onClick = { onActivityClick(activity.id, activity.sessionId) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                )
            }
        }
    }
}

// ── Yearly Stats Card ──

@Composable
private fun YearlyStatsCard(
    year: Int,
    totalActivities: Int,
    totalDistanceKm: Double,
    totalElevationM: Int,
    totalPoints: Int,
    isLoading: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TsmSurface,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TsmAccent,
                )
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TsmAccent)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCell("Uscite", totalActivities.toString(), Color.White)
                StatCell("Km totali", "%.0f".format(totalDistanceKm), TsmAccent)
                StatCell("Dislivello", "${totalElevationM}m", Color(0xFFFF9800))
                StatCell("Punti", "$totalPoints pt", TsmPrimary)
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = valueColor)
    }
}

// ── Monthly Bar Chart (Canvas) ──

@Composable
private fun MonthlyBarChart(
    counts: List<Int>,
    avgDifficulties: List<Double>,
    selectedMonth: Int?,
    onMonthClick: (Int) -> Unit,
) {
    val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val chartHeight = 90.dp
    val labelHeight = 20.dp
    val barColor1 = TsmPrimary
    val barColor2 = Color(0xFFFF5722) // rosso per difficoltà alta

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .clickable { },
        ) {
            val barWidth = (size.width - 4.dp.toPx() * 11) / 12
            val spacing = (size.width - barWidth * 12) / 11

            counts.forEachIndexed { i, count ->
                val x = i * (barWidth + spacing)
                val ratio = count.toFloat() / maxCount.toFloat()
                val barH = ratio * size.height
                val diff = avgDifficulties.getOrElse(i) { 0.5 }
                val barCol = lerp(barColor1, barColor2, diff.toFloat().coerceIn(0f, 1f))
                val isSelected = selectedMonth == i
                val barAlpha = if (selectedMonth == null || isSelected) 1f else 0.35f

                // Bar fill
                drawRect(
                    brush = Brush.verticalGradient(listOf(barCol.copy(alpha = barAlpha), barCol.copy(alpha = barAlpha * 0.6f))),
                    topLeft = Offset(x, size.height - barH),
                    size = Size(barWidth, barH),
                )
                // Count label
                if (count > 0) {
                    val canvas = drawContext.canvas.nativeCanvas
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 11.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        alpha = (barAlpha * 255).toInt()
                    }
                    canvas.drawText(count.toString(), x + barWidth / 2, size.height - barH - 4.dp.toPx(), paint)
                }
            }
        }

        // Month labels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MONTH_LABELS.forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(labelHeight)
                        .clickable { onMonthClick(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedMonth == i) TsmAccent else Color.Gray,
                    )
                }
            }
        }
    }
}

// ── Activity List Item ──

@Composable
private fun ActivityListItem(
    activity: ActivityListViewModel.ActivityItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.ITALIAN)
    val timeFmt = SimpleDateFormat("HH:mm", Locale.ITALIAN)
    val dateStr = dateFmt.format(Date(activity.dateMs))
    val timeStr = timeFmt.format(Date(activity.dateMs))
    val movingH = HikeEstimation.formatHours(activity.movingSeconds / 3600.0)

    Surface(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = TsmSurface,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Activity icon
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = TsmSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Outlined.DirectionsWalk,
                        contentDescription = null,
                        tint = TsmAccent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$dateStr · $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "%.1f km".format(activity.distanceKm),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TsmAccent,
                    )
                    Text("·", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        movingH,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                    if (activity.elevationGainM > 0) {
                        Text("·", color = Color.Gray, fontSize = 10.sp)
                        Text(
                            "+${activity.elevationGainM}m",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFF9800),
                        )
                    }
                    // Difficulty dots
                    DifficultyIndicator(activity.difficultyLevel)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Sync status
                if (activity.isSynced) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Sincronizzato", tint = TsmPrimary, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Outlined.Sync, contentDescription = "Non sincronizzato", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                // Share button
                IconButton(onClick = { /* TODO: condivisione social */ }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = "Condividi", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DifficultyIndicator(level: String?) {
    val count = when (level) { "T" -> 1; "E" -> 2; "EE" -> 3; "EEA" -> 4; else -> 0 }
    if (count == 0) return
    val color = when (level) { "T" -> TsmPrimary; "E" -> TsmAccent; "EE" -> Color(0xFFFF9800); "EEA" -> Color(0xFFFF5722); else -> Color.Gray }
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color)) }
        repeat(4 - count) { Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF3A3A3A))) }
    }
}

@Composable
private fun EmptyActivitiesState() {
    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.DirectionsWalk, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Nessuna attività ancora registrata.",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
            Text(
                "Inizia una nuova avventura dalla tab Registra!",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }
    }
}
