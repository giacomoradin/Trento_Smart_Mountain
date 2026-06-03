package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.components.TsmRouteMapPreview

/** Mappa OSM a tutta scena (senza cornice), centrata sulla polyline. */
@Composable
fun StoryRouteMapScene(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
    lineColorArgb: Int = android.graphics.Color.parseColor("#4FC3F7"),
) {
    if (points.size >= 2) {
        TsmRouteMapPreview(
            points = points,
            modifier = modifier,
            showTrack = true,
            storySceneMode = true,
            lineColor = lineColorArgb,
        )
    }
}

/** Margine extra rispetto al frame 9:16 per pan/zoom/rotazione senza bordi neri. */
const val StoryMapSceneOverscale = 2.5f
