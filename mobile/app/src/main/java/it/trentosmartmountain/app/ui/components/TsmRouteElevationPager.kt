package it.trentosmartmountain.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.screens.home.ElevationSparkline
import it.trentosmartmountain.app.ui.screens.home.elevationRangeFromProfile
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.theme.difficultyColor
import kotlinx.coroutines.launch

/**
 * Hero unico e riusabile per visualizzare il tracciato di un'escursione come
 * due schede scorrevoli orizzontalmente (Mappa GPX / Profilo Altimetrico).
 */
@Composable
fun TsmRouteElevationPager(
    routePoints: List<RoutePoint>?,
    elevationProfile: List<Double>?,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    cornerRadius: Dp = 12.dp,
    backgroundBrush: Brush? = null,
    elevationLineColor: Color = TsmColors.Cyan,
    activeDotColor: Color = TsmColors.Primary,
    difficultyLevel: String? = null,
    /** Se true, il pulsante ingrandisci apre mappa full-screen interattiva. */
    expandable: Boolean = false,
    /** Distanza totale (km) per l'asse X del profilo altimetrico nel pager. */
    distanceKm: Double? = null,
    /** Quote min/max in metri; se null derivate dal profilo quando possibile. */
    elevationMinM: Double? = null,
    elevationMaxM: Double? = null,
    emptyContent: (@Composable () -> Unit)? = null,
) {
    val hasRoute = routePoints != null && routePoints.size >= 2
    val hasProfile = elevationProfile != null && elevationProfile.size >= 2
    val pageCount = listOf(hasRoute, hasProfile).count { it }
    val pagerState = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()
    var showFullMap by remember { mutableStateOf(false) }

    val (derivedMin, derivedMax) = remember(elevationProfile, elevationMinM, elevationMaxM) {
        if (elevationMinM != null && elevationMaxM != null) {
            elevationMinM to elevationMaxM
        } else if (elevationProfile != null) {
            elevationRangeFromProfile(elevationProfile)
        } else {
            0.0 to 0.0
        }
    }

    val shape = RoundedCornerShape(cornerRadius)
    val defaultHeroBrush = Brush.verticalGradient(
        colors = listOf(TsmColors.HeroTop, TsmColors.AlpinePineDark),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(backgroundBrush ?: defaultHeroBrush),
    ) {
        if (pageCount == 0) {
            emptyContent?.invoke()
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { pageIdx ->
                val showMap = hasRoute && pageIdx == 0

                if (showMap && routePoints != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TsmRouteMapPreview(
                            points = routePoints,
                            modifier = Modifier.fillMaxSize(),
                            interactive = false,
                        )
                        if (!difficultyLevel.isNullOrBlank()) {
                            DifficultyChip(
                                level = difficultyLevel,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                            )
                        }
                    }
                } else if (elevationProfile != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ElevationSparkline(
                            profile = elevationProfile,
                            modifier = Modifier.fillMaxSize(),
                            lineColor = elevationLineColor,
                            distanceKm = distanceKm,
                            minAltM = derivedMin,
                            maxAltM = derivedMax,
                            // Mostra header + assi distanza + MIN/MAX quote ogni volta che
                            // abbiamo la distanza: anche nel feed (prima era gated a ≥200dp,
                            // così la card compatta non mostrava assi né unità).
                            showAxisLabels = distanceKm != null && distanceKm > 0 && height >= 150.dp,
                        )
                    }
                }
            }

            if (pageCount > 1) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(pageCount) {
                            var drag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { drag = 0f },
                                onDragCancel = { drag = 0f },
                                onDragEnd = {
                                    val threshold = 40.dp.toPx()
                                    val cur = pagerState.currentPage
                                    val target = when {
                                        drag <= -threshold -> (cur + 1).coerceAtMost(pageCount - 1)
                                        drag >= threshold -> (cur - 1).coerceAtLeast(0)
                                        else -> cur
                                    }
                                    scope.launch { pagerState.animateScrollToPage(target) }
                                },
                            ) { change, dragAmount ->
                                change.consume()
                                drag += dragAmount
                            }
                        },
                )
            }

            if (pageCount > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(pageCount) { i ->
                        val isSelected = pagerState.currentPage == i
                        val dotColor = if (isSelected) activeDotColor else TsmColors.TextSecondary.copy(alpha = 0.4f)
                        val dotWidth = if (isSelected) 14.dp else 6.dp
                        Box(
                            modifier = Modifier
                                .size(width = dotWidth, height = 6.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                    }
                }
            }

            if (expandable && hasRoute) {
                IconButton(
                    onClick = { showFullMap = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .zIndex(2f)
                        .padding(4.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Ingrandisci mappa",
                            tint = Color.White,
                            modifier = Modifier.padding(6.dp).size(20.dp),
                        )
                    }
                }
            }
        }

        if (showFullMap && routePoints != null) {
            TsmRouteMapDialog(
                routePoints = routePoints,
                onClose = { showFullMap = false },
            )
        }
    }
}

@Composable
private fun DifficultyChip(level: String, modifier: Modifier = Modifier) {
    val chipColor = difficultyColor(level)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = chipColor.copy(alpha = 0.95f),
        modifier = modifier,
    ) {
        Text(
            text = level,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
