package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.ui.theme.TsmColors

@Composable
fun StoryEditorControlsBar(
    hasCustomBackground: Boolean,
    hasRoute: Boolean,
    routeOverlayMode: RouteOverlayMode,
    selectedSticker: StoryStickerKind?,
    routeColor: Color,
    textColor: Color,
    showTextSticker: Boolean,
    textEditMode: Boolean,
    onImportGallery: () -> Unit,
    onImportPhoto: () -> Unit,
    onImportVideo: () -> Unit,
    onRemoveBackground: () -> Unit,
    onAddTraceOverlay: () -> Unit,
    onAddMapWidget: () -> Unit,
    onRemoveRouteOverlay: () -> Unit,
    onAddText: () -> Unit,
    onConfirmText: () -> Unit,
    onRemoveText: () -> Unit,
    onRouteColor: (Color) -> Unit,
    onTextColor: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var importOpen by remember { mutableStateOf(false) }
    var routeOpen by remember { mutableStateOf(false) }
    var colorOpen by remember { mutableStateOf(false) }
    var extrasOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            OutlinedButton(onClick = { importOpen = true }) {
                Icon(Icons.Filled.Image, null, modifier = Modifier.size(18.dp))
                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = importOpen, onDismissRequest = { importOpen = false }) {
                DropdownMenuItem(text = { Text("Galleria") }, onClick = { importOpen = false; onImportGallery() })
                DropdownMenuItem(text = { Text("Fotocamera") }, onClick = { importOpen = false; onImportPhoto() })
                DropdownMenuItem(text = { Text("Video") }, onClick = { importOpen = false; onImportVideo() })
                if (hasCustomBackground) {
                    DropdownMenuItem(
                        text = { Text("Rimuovi sfondo", color = TsmColors.Danger) },
                        onClick = { importOpen = false; onRemoveBackground() },
                    )
                }
            }
        }

        if (hasCustomBackground && hasRoute) {
            Box {
                OutlinedButton(onClick = { routeOpen = true }) {
                    Icon(Icons.Filled.Route, null, modifier = Modifier.size(18.dp))
                    Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = routeOpen, onDismissRequest = { routeOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Traccia GPX") },
                        onClick = { routeOpen = false; onAddTraceOverlay() },
                    )
                    DropdownMenuItem(
                        text = { Text("Widget mappa") },
                        onClick = { routeOpen = false; onAddMapWidget() },
                    )
                    if (routeOverlayMode != RouteOverlayMode.NONE) {
                        DropdownMenuItem(
                            text = { Text("Rimuovi percorso", color = TsmColors.Danger) },
                            onClick = { routeOpen = false; onRemoveRouteOverlay() },
                        )
                    }
                }
            }
        }

        val colorTarget =
            when (selectedSticker) {
                StoryStickerKind.TRACE, StoryStickerKind.MAP_SCENE, StoryStickerKind.MAP_WIDGET -> routeColor
                StoryStickerKind.TEXT -> textColor
                else -> null
            }
        if (colorTarget != null) {
            Box {
                OutlinedButton(onClick = { colorOpen = true }) {
                    Icon(Icons.Filled.Palette, null, modifier = Modifier.size(18.dp))
                    Box(
                        modifier =
                            Modifier
                                .size(14.dp)
                                .background(colorTarget, CircleShape),
                    )
                }
                DropdownMenu(expanded = colorOpen, onDismissRequest = { colorOpen = false }) {
                    StoryStickerColors.forEach { c ->
                        DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(18.dp).background(c, CircleShape))
                                    Text(colorName(c))
                                }
                            },
                            onClick = {
                                colorOpen = false
                                when (selectedSticker) {
                                    StoryStickerKind.TEXT -> onTextColor(c)
                                    else -> onRouteColor(c)
                                }
                            },
                        )
                    }
                }
            }
        }

        if (textEditMode || selectedSticker == StoryStickerKind.TEXT) {
            Button(
                onClick = onConfirmText,
                colors = ButtonDefaults.buttonColors(containerColor = TsmColors.Cyan),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Conferma testo", color = Color.Black)
            }
        }

        Box {
            OutlinedButton(onClick = { extrasOpen = true }) {
                Icon(Icons.Filled.TextFields, null, modifier = Modifier.size(18.dp))
                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = extrasOpen, onDismissRequest = { extrasOpen = false }) {
                if (!showTextSticker) {
                    DropdownMenuItem(text = { Text("Aggiungi testo") }, onClick = { extrasOpen = false; onAddText() })
                } else {
                    DropdownMenuItem(
                        text = { Text("Rimuovi testo", color = TsmColors.Danger) },
                        onClick = { extrasOpen = false; onRemoveText() },
                    )
                }
            }
        }
    }
}

private fun colorName(c: Color): String =
    when (c.toArgb()) {
        Color(0xFF4DD0E1).toArgb() -> "Ciano"
        Color.White.toArgb() -> "Bianco"
        Color(0xFFFF5252).toArgb() -> "Rosso"
        Color(0xFF4CAF50).toArgb() -> "Verde"
        Color(0xFFFFD54F).toArgb() -> "Giallo"
        else -> "Arancione"
    }
