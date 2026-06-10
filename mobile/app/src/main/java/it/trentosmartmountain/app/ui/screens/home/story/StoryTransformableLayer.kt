package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Layer trasformabile per gli sticker della storia (traccia, mappa, testo).
 *
 * Implementazione canonica "Instagram-sticker", riscritta per risolvere i bug:
 *  - **Tap per selezionare** e **drag/pinch/rotate** vivono in due `pointerInput`
 *    separati. Prima erano nello stesso blocco dopo `detectTapGestures`, che è una
 *    `suspend` che non ritorna mai → le transform gesture non partivano.
 *  - Il modello di trasformazione è **incrementale e stabile**: ad ogni frame
 *    `offset += pan`, `scale *= zoom`, `rotation += rot`. Niente più rotazione del
 *    centro attorno al pivot, che faceva "volare via" lo sticker fuori dal riquadro
 *    al rilascio.
 *  - Nessun local-state + ack machinery: leggiamo/scriviamo direttamente la
 *    `transform` del ViewModel (source of truth), così non c'è mismatch che
 *    facesse "saltare" lo sticker quando si mollava il dito.
 *
 * Lo stato è committato live ad ogni frame del gesto via [onTransformChange].
 */
@Composable
fun StoryTransformableLayer(
    selected: Boolean,
    transform: StoryStickerTransform,
    onSelect: () -> Unit,
    onTransformChange: (StoryStickerTransform) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // rememberUpdatedState: i lambda dei pointerInput catturano sempre i valori
    // più recenti senza ri-creare il gesture-detector ad ogni ricomposizione.
    val currentTransform = rememberUpdatedState(transform)
    val onChange = rememberUpdatedState(onTransformChange)
    val onSelected = rememberUpdatedState(onSelect)

    Box(
        modifier =
            modifier
                // 1) Tap → seleziona (sempre attivo).
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onSelected.value() })
                }
                // 2) Drag / pinch / rotate → solo quando selezionato.
                .pointerInput(selected) {
                    if (!selected) return@pointerInput
                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, rotation ->
                        val t = currentTransform.value
                        onChange.value(
                            t.copy(
                                offsetX = t.offsetX + pan.x,
                                offsetY = t.offsetY + pan.y,
                                scale = (t.scale * zoom).coerceIn(0.2f, 6f),
                                rotationDeg = t.rotationDeg + rotation * StoryRotationGestureFactor,
                            ),
                        )
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        clip = false
                        translationX = transform.offsetX
                        translationY = transform.offsetY
                        scaleX = transform.scale
                        scaleY = transform.scale
                        rotationZ = transform.rotationDeg
                    }
                    .then(
                        if (selected) {
                            Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
