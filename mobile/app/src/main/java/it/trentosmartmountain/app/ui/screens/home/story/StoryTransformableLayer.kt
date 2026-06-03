package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/** Layer trasformabile: stato locale durante il gesto; commit al rilascio / deselezione. */
@Composable
fun StoryTransformableLayer(
    selected: Boolean,
    transform: StoryStickerTransform,
    onSelect: () -> Unit,
    onTransformChange: (StoryStickerTransform) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val offsetX = remember { mutableFloatStateOf(transform.offsetX) }
    val offsetY = remember { mutableFloatStateOf(transform.offsetY) }
    val scale = remember { mutableFloatStateOf(transform.scale) }
    val rotationDeg = remember { mutableFloatStateOf(transform.rotationDeg) }

    val onCommit = rememberUpdatedState(onTransformChange)
    val gesturing = remember { AtomicBoolean(false) }
    val awaitingParentAck = remember { AtomicBoolean(false) }

    fun readTransform(): StoryStickerTransform =
        StoryStickerTransform(
            offsetX = offsetX.floatValue,
            offsetY = offsetY.floatValue,
            scale = scale.floatValue,
            rotationDeg = rotationDeg.floatValue,
        )

    fun writeTransform(t: StoryStickerTransform) {
        offsetX.floatValue = t.offsetX
        offsetY.floatValue = t.offsetY
        scale.floatValue = t.scale
        rotationDeg.floatValue = t.rotationDeg
    }

    var acknowledgedKey by remember { mutableStateOf(transform.toKey()) }

    // Sync dal ViewModel solo per reset esterni; ignora valori obsoleti finché il commit non è riflesso nel parent.
    SideEffect {
        if (gesturing.get()) return@SideEffect
        val inboundKey = transform.toKey()
        if (awaitingParentAck.get()) {
            if (inboundKey.near(acknowledgedKey)) {
                awaitingParentAck.set(false)
            }
            return@SideEffect
        }
        if (!inboundKey.contentEquals(acknowledgedKey)) {
            writeTransform(transform)
            acknowledgedKey = inboundKey
        }
    }

    var wasSelected by remember { mutableStateOf(selected) }
    SideEffect {
        if (wasSelected && !selected) {
            val local = readTransform()
            awaitingParentAck.set(true)
            acknowledgedKey = local.toKey()
            onCommit.value(local)
        }
        wasSelected = selected
    }

    Box(
        modifier =
            modifier
                .pointerInput(selected) {
                    detectTapGestures(onTap = { onSelect() })
                    if (!selected) return@pointerInput
                    detectTransformGesturesWithEnd(
                        onGestureStart = { gesturing.set(true) },
                        onGesture = { centroid, pan, zoom, rotation, pointerCount ->
                            val pivot =
                                Offset(
                                    x = centroid.x - size.width / 2f,
                                    y = centroid.y - size.height / 2f,
                                )
                            val rotationRad = if (pointerCount >= 2) rotation else 0f
                            writeTransform(
                                readTransform().applyGesture(
                                    pan = pan,
                                    zoom = zoom,
                                    rotationRad = rotationRad,
                                    pivotInCanvas = pivot,
                                ),
                            )
                        },
                        onGestureEnd = {
                            val local = readTransform()
                            acknowledgedKey = local.toKey()
                            awaitingParentAck.set(true)
                            gesturing.set(false)
                            onCommit.value(local)
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        clip = false
                        translationX = offsetX.floatValue
                        translationY = offsetY.floatValue
                        scaleX = scale.floatValue
                        scaleY = scale.floatValue
                        rotationZ = rotationDeg.floatValue
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

private fun StoryStickerTransform.toKey(): FloatArray =
    floatArrayOf(offsetX, offsetY, scale, rotationDeg)

private fun FloatArray.near(other: FloatArray, epsilon: Float = 0.75f): Boolean {
    if (size != other.size) return false
    for (i in indices) {
        if (abs(this[i] - other[i]) > epsilon) return false
    }
    return true
}

private suspend fun PointerInputScope.detectTransformGesturesWithEnd(
    onGestureStart: () -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float, pointerCount: Int) -> Unit,
    onGestureEnd: () -> Unit,
) {
    awaitEachGesture {
        var started = false
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var panAccum = Offset.Zero
        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            if (!event.changes.any { it.isConsumed }) {
                val panChange = event.calculatePan()
                val pointers = event.changes.count { it.pressed }
                if (!pastTouchSlop) {
                    panAccum += panChange
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    val pinchStarted =
                        pointers >= 2 &&
                            (kotlin.math.abs(zoomChange - 1f) > 0.01f || kotlin.math.abs(rotationChange) > 0.01f)
                    if (panAccum.getDistance() > touchSlop || pinchStarted) {
                        pastTouchSlop = true
                        if (!started) {
                            onGestureStart()
                            started = true
                        }
                    }
                } else {
                    if (!started) {
                        onGestureStart()
                        started = true
                    }
                    val centroid = event.calculateCentroid(useCurrent = false)
                    onGesture(
                        centroid,
                        panChange,
                        event.calculateZoom(),
                        event.calculateRotation(),
                        pointers,
                    )
                }
                if (started) {
                    event.changes.forEach { it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })
        if (started) {
            onGestureEnd()
        }
    }
}
