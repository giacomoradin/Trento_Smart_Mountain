package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Sticker solo visualizzazione (viewer storie video). */
@Composable
fun StoryStaticSticker(
    transform: StoryStickerTransform,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier.graphicsLayer {
                translationX = transform.offsetX
                translationY = transform.offsetY
                scaleX = transform.scale
                scaleY = transform.scale
                rotationZ = transform.rotationDeg
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
