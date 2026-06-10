package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.components.TsmRouteMapPreview

/** Widget mappa + traccia GPX (per overlay su foto o scena mobile). */
@Composable
fun StoryRouteMapWidget(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
    lineColorArgb: Int = android.graphics.Color.parseColor("#4FC3F7"),
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D1117)),
    ) {
        if (points.size >= 2) {
            TsmRouteMapPreview(
                points = points,
                modifier = Modifier.fillMaxSize(),
                showTrack = true,
                interactive = false,
                lineColor = lineColorArgb,
            )
        }
    }
}
