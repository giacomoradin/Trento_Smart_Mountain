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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.screens.home.ElevationSparkline
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.theme.difficultyColor
import kotlinx.coroutines.launch

/**
 * Hero unico e riusabile per visualizzare il tracciato di un'escursione come
 * due schede scorrevoli orizzontalmente (Mappa GPX / Profilo Altimetrico).
 *
 * Implementa la nuova palette ad alte prestazioni unendo il Tech Navy e il profondo Alpine Pine.
 */
@Composable
fun TsmRouteElevationPager(
    routePoints: List<RoutePoint>?,
    elevationProfile: List<Double>?,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp, // Incrementato leggermente per ospitare la telemetria densa senza compressione degli assi
    cornerRadius: Dp = 12.dp, // Arrotondamento standardizzato per le card del modulo Social/Feed
    backgroundBrush: Brush? = null,
    elevationLineColor: Color = TsmColors.Cyan,
    activeDotColor: Color = TsmColors.Primary, // Allineato al brand High-Vis Athletic Orange
    difficultyLevel: String? = null,
    emptyContent: (@Composable () -> Unit)? = null,
) {
    val hasRoute = routePoints != null && routePoints.size >= 2
    val hasProfile = elevationProfile != null && elevationProfile.size >= 2
    val pageCount = listOf(hasRoute, hasProfile).count { it }
    val pagerState = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()

    val shape = RoundedCornerShape(cornerRadius)
    
    // Gradiente di default se non viene passato un brush personalizzato dal modulo chiamante
    val defaultHeroBrush = Brush.verticalGradient(
        colors = listOf(
            TsmColors.HeroTop,       // Tech Navy (#003748)
            TsmColors.AlpinePineDark // Transizione verso il profondo abete (#004225)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(backgroundBrush ?: defaultHeroBrush)
    ) {
        if (pageCount == 0) {
            emptyContent?.invoke()
        } else {
            HorizontalPager(
                state = pagerState, 
                modifier = Modifier.fillMaxSize()
            ) { pageIdx ->
                // Mappatura sequenziale delle pagine basata sulla presenza dei dati (Mappa prioritari)
                val showMap = hasRoute && pageIdx == 0

                if (showMap && routePoints != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TsmRouteMapPreview(
                            points = routePoints,
                            modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ElevationSparkline(
                            profile = elevationProfile,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 20.dp), // Padding calibrato per isolare le label sui canvas
                            lineColor = elevationLineColor,
                        )
                    }
                }
            }

            // Overlay di gesto: pilota lo swipe mappa↔altimetria in modo deterministico.
            // La MapView (AndroidView) può intercettare i drag a livello View; questo
            // overlay Compose, sovrapposto, consuma SOLO i drag orizzontali e cambia
            // pagina, lasciando passare i drag verticali al contenitore (es. LazyColumn).
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

            // Indicatore dei punti di scorrimento (Pager Indicator)
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
                        val dotWidth = if (isSelected) 14.dp else 6.dp // Feedback visivo elastico sull'indice attivo
                        
                        Box(
                            modifier = Modifier
                                .size(width = dotWidth, height = 6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

/** * Chip difficoltà CAI (T/E/EE/EEA) ottimizzato.
 * Sfrutta il sistema di calcolo deterministico `difficultyColor` aggiornato.
 */
@Composable
private fun DifficultyChip(level: String, modifier: Modifier = Modifier) {
    val chipColor = difficultyColor(level)
    Surface(
        shape = RoundedCornerShape(4.dp), // Angoli rigidi per interfacce di tipo "data-focused math"
        color = chipColor.copy(alpha = 0.95f),
        modifier = modifier
    ) {
        Text(
            text = level,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black // Massima leggibilità su mappa OpenTopoMap
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}