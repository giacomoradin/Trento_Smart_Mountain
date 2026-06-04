package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import it.trentosmartmountain.app.data.remote.dto.StoryEditorDecor
import it.trentosmartmountain.app.data.remote.dto.StoryStickerTransformDto
import it.trentosmartmountain.app.ui.screens.home.RouteTracePreview

@Composable
fun StoryViewerDecorations(
    decor: StoryEditorDecor?,
    routePoints: List<it.trentosmartmountain.app.data.remote.dto.RoutePoint>,
    modifier: Modifier = Modifier,
) {
    val d = decor ?: return
    val pts = routePoints.takeIf { it.size >= 2 } ?: return
    val color = hexToColor(d.routeColor, Color(0xFF4DD0E1))

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .zIndex(1f),
        ) {
            when (d.routeOverlayKind) {
                "map_scene" -> {
                    val t = d.routeTransform?.toLocal() ?: StoryStickerTransform()
                    StoryStaticSticker(transform = t, modifier = Modifier.fillMaxSize()) {
                        StoryRouteMapScene(
                            points = pts,
                            modifier =
                                Modifier.size(
                                    this@BoxWithConstraints.maxWidth * StoryMapSceneOverscale,
                                    this@BoxWithConstraints.maxHeight * StoryMapSceneOverscale,
                                ),
                            lineColorArgb = color.toArgb(),
                        )
                    }
                }
                "map_widget" -> {
                    val t = d.mapWidgetTransform?.toLocal() ?: StoryStickerTransform(scale = 0.85f)
                    StoryStaticSticker(transform = t, modifier = Modifier.align(Alignment.Center)) {
                        StoryRouteMapWidget(
                            points = pts,
                            modifier = Modifier.width(260.dp).height(200.dp),
                            lineColorArgb = color.toArgb(),
                        )
                    }
                }
                "trace" -> {
                    d.routeTransform?.let { t ->
                        StoryStaticSticker(transform = t.toLocal(), modifier = Modifier.align(Alignment.Center)) {
                            RouteTracePreview(
                                points = pts,
                                modifier = Modifier.width(220.dp).height(170.dp),
                                lineColor = color,
                            )
                        }
                    }
                }
            }
        }

        val text = d.floatingText?.trim().orEmpty()
        if (text.isNotBlank()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .zIndex(2f),
            ) {
                val textT = d.textTransform?.toLocal() ?: StoryStickerTransform()
                StoryStaticSticker(transform = textT, modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        text = text,
                        color = hexToColor(d.textColor, Color.White),
                        fontWeight = FontWeight.Bold,
                        fontFamily = StoryFont.fromKey(d.textFont).composeFamily,
                        fontSize = 22.sp,
                        modifier =
                            Modifier
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

private fun StoryStickerTransformDto.toLocal() =
    StoryStickerTransform(
        offsetX = offsetX,
        offsetY = offsetY,
        scale = scale,
        rotationDeg = rotationDeg,
    )
