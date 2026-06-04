package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.screens.home.RouteTracePreview
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.util.AvatarUtils

private const val Z_BACKGROUND = 0f
private const val Z_ROUTE_STICKERS = 1f
private const val Z_TEXT = 3f

/**
 * Canvas 9:16: sfondo (0) → mappa/traccia/widget (1) → testo (2).
 */
@Composable
fun StoryEditorCanvas(
    routePoints: List<RoutePoint>,
    hasCustomBackground: Boolean,
    /** True per foto O video: gli overlay (traccia/mappa) si sovrappongono al media. */
    hasMediaBackground: Boolean = hasCustomBackground,
    mediaKind: String?,
    mediaDataUri: String?,
    isEncoding: Boolean,
    routeOverlayMode: RouteOverlayMode,
    mapSceneTransform: StoryStickerTransform,
    routeTransform: StoryStickerTransform,
    mapWidgetTransform: StoryStickerTransform,
    routeColor: Color,
    selectedSticker: StoryStickerKind?,
    onSelectSticker: (StoryStickerKind?) -> Unit,
    onMapSceneTransformChange: (StoryStickerTransform) -> Unit,
    onRouteTransformChange: (StoryStickerTransform) -> Unit,
    onMapWidgetTransformChange: (StoryStickerTransform) -> Unit,
    showTextSticker: Boolean,
    textEditMode: Boolean,
    floatingText: String,
    textTransform: StoryStickerTransform,
    textColor: Color,
    textFont: StoryFont = StoryFont.CLASSIC,
    onTextTransformChange: (StoryStickerTransform) -> Unit,
    onEditorCanvasSize: (widthPx: Float, heightPx: Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val frameShape = RoundedCornerShape(16.dp)
    val lineArgb = remember(routeColor) { routeColor.toArgb() }
    val hasRoute = routePoints.size >= 2
    val density = LocalDensity.current

    ColumnSurface(modifier = modifier) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(frameShape)
                    .background(Color.Black)
                    .pointerInput(textEditMode) {
                        if (!textEditMode) {
                            detectTapGestures(onTap = { onSelectSticker(null) })
                        }
                    },
        ) {
            SideEffect {
                onEditorCanvasSize(
                    with(density) { maxWidth.toPx() },
                    with(density) { maxHeight.toPx() },
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .zIndex(Z_BACKGROUND),
            ) {
                when {
                    isEncoding -> {
                        CircularProgressIndicator(
                            color = TsmColors.Cyan,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    hasCustomBackground && mediaKind == "image" && mediaDataUri != null -> {
                        val bmp = remember(mediaDataUri) { AvatarUtils.decodeDataUri(mediaDataUri) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    mediaKind == "video" -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1B2838), Color(0xFF0D1117)),
                                        ),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = TsmColors.Cyan, modifier = Modifier.size(56.dp))
                            Text("Video", color = TsmColors.TextSecondary)
                        }
                    }
                    !hasCustomBackground && hasRoute && mediaKind != "video" -> {
                        val overscale = StoryMapSceneOverscale
                        val sceneSize =
                            Modifier.size(
                                this@BoxWithConstraints.maxWidth * overscale,
                                this@BoxWithConstraints.maxHeight * overscale,
                            )
                        key(StoryStickerKind.MAP_SCENE) {
                        StoryTransformableLayer(
                            selected = selectedSticker == StoryStickerKind.MAP_SCENE,
                            transform = mapSceneTransform,
                            onSelect = { onSelectSticker(StoryStickerKind.MAP_SCENE) },
                            onTransformChange = onMapSceneTransformChange,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            StoryRouteMapScene(
                                points = routePoints,
                                modifier = sceneSize,
                                lineColorArgb = lineArgb,
                            )
                        }
                        }
                    }
                    else -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1B2838), Color(0xFF0D1117)),
                                        ),
                                    ),
                        )
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .zIndex(Z_ROUTE_STICKERS),
            ) {
                // hasMediaBackground (foto O video): gli overlay traccia/mappa-widget
                // si sovrappongono al media. Prima era gated a hasCustomBackground
                // (solo foto) → sul video la mappa non era aggiungibile (#5).
                if (hasMediaBackground && hasRoute) {
                    when (routeOverlayMode) {
                        RouteOverlayMode.TRACE -> {
                            key(StoryStickerKind.TRACE) {
                            StoryTransformableLayer(
                                selected = selectedSticker == StoryStickerKind.TRACE,
                                transform = routeTransform,
                                onSelect = { onSelectSticker(StoryStickerKind.TRACE) },
                                onTransformChange = onRouteTransformChange,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                RouteTracePreview(
                                    points = routePoints,
                                    modifier = Modifier.width(240.dp).height(180.dp),
                                    lineColor = routeColor,
                                )
                            }
                            }
                        }
                        RouteOverlayMode.MAP_WIDGET -> {
                            key(StoryStickerKind.MAP_WIDGET) {
                            StoryTransformableLayer(
                                selected = selectedSticker == StoryStickerKind.MAP_WIDGET,
                                transform = mapWidgetTransform,
                                onSelect = { onSelectSticker(StoryStickerKind.MAP_WIDGET) },
                                onTransformChange = onMapWidgetTransformChange,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                StoryRouteMapWidget(
                                    points = routePoints,
                                    modifier =
                                        Modifier
                                            .width(260.dp)
                                            .height(200.dp),
                                    lineColorArgb = lineArgb,
                                )
                            }
                            }
                        }
                        RouteOverlayMode.NONE -> Unit
                    }
                }
            }

            if (showTextSticker) {
                val displayText = floatingText.ifBlank { "La tua avventura" }
                val textActive = textEditMode || selectedSticker == StoryStickerKind.TEXT
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .zIndex(Z_TEXT),
                ) {
                    key(StoryStickerKind.TEXT) {
                    StoryTransformableLayer(
                        selected = textActive,
                        transform = textTransform,
                        onSelect = {
                            if (!textEditMode) {
                                onSelectSticker(StoryStickerKind.TEXT)
                            }
                        },
                        onTransformChange = onTextTransformChange,
                        modifier =
                            if (textActive) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier.align(Alignment.Center).wrapContentSize()
                            },
                    ) {
                        Text(
                        text = displayText,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = textFont.composeFamily,
                        fontSize = 22.sp,
                        modifier =
                            Modifier
                                .alpha(if (floatingText.isBlank()) 0.6f else 1f)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141418),
        border = BorderStroke(1.5.dp, TsmColors.Cyan.copy(alpha = 0.45f)),
    ) {
        Box(modifier = Modifier.padding(10.dp)) {
            content()
        }
    }
    Text(
        "Area di modifica · 9:16",
        color = TsmColors.TextTertiary,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    )
}
