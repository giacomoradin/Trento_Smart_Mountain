package it.trentosmartmountain.app.ui.screens.home.story

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    textFont: StoryFont = StoryFont.CLASSIC,
    onCycleFont: () -> Unit = {},
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

    // Toolbar editor in stile "creator pro": Surface glass con bordo morbido,
    // tile uniformi cliccabili, label sotto l'icona, scroll orizzontale per
    // adattarsi a schermi piccoli senza tagliare azioni.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = TsmColors.CardElevated.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.06f),
        ),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Media (foto / video / galleria) ─────────────────────────────
            item {
                Box {
                    EditorTile(
                        icon = Icons.Filled.Image,
                        label = if (hasCustomBackground) "Media" else "Media",
                        accent = TsmColors.Cyan,
                        highlighted = hasCustomBackground,
                        onClick = { importOpen = true },
                    )
                    DropdownMenu(
                        expanded = importOpen,
                        onDismissRequest = { importOpen = false },
                        modifier = Modifier.background(TsmColors.CardElevated),
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, null, tint = TsmColors.Cyan) },
                            text = { Text("Galleria", color = Color.White) },
                            onClick = { importOpen = false; onImportGallery() },
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.PhotoCamera, null, tint = TsmColors.Cyan) },
                            text = { Text("Foto da fotocamera", color = Color.White) },
                            onClick = { importOpen = false; onImportPhoto() },
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Videocam, null, tint = TsmColors.Cyan) },
                            text = { Text("Video (max 10s)", color = Color.White) },
                            onClick = { importOpen = false; onImportVideo() },
                        )
                        if (hasCustomBackground) {
                            DropdownMenuItem(
                                text = { Text("Rimuovi sfondo", color = TsmColors.Danger) },
                                onClick = { importOpen = false; onRemoveBackground() },
                            )
                        }
                    }
                }
            }

            // ── Percorso (mappa/traccia) ────────────────────────────────────
            // Disponibile SEMPRE che ci sia una traccia (con o senza foto/video):
            // l'utente può importare la polyline da sola o mappa+polyline, poi
            // spostarla/ingrandirla/cambiarne colore. `hasCustomBackground` qui
            // riceve hasMediaBackground (foto O video) → serve solo per la label.
            if (hasRoute) {
                item {
                    Box {
                        EditorTile(
                            icon = Icons.Filled.Route,
                            label = when (routeOverlayMode) {
                                RouteOverlayMode.NONE -> if (hasCustomBackground) "Percorso" else "Mappa"
                                RouteOverlayMode.TRACE -> "Traccia"
                                RouteOverlayMode.MAP_WIDGET -> "Mappa"
                            },
                            accent = TsmColors.Primary,
                            highlighted = !hasCustomBackground || routeOverlayMode != RouteOverlayMode.NONE,
                            onClick = { routeOpen = true },
                        )
                        DropdownMenu(
                            expanded = routeOpen,
                            onDismissRequest = { routeOpen = false },
                            modifier = Modifier.background(TsmColors.CardElevated),
                        ) {
                            // Mappa a tutto schermo: solo senza media di sfondo
                            // (la mappa fa da sfondo). Mode NONE.
                            if (!hasCustomBackground) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Outlined.Map, null, tint = TsmColors.Cyan) },
                                    text = { Text("Mappa a tutto schermo", color = Color.White) },
                                    onClick = { routeOpen = false; onRemoveRouteOverlay() },
                                )
                            }
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Timeline, null, tint = TsmColors.Cyan) },
                                text = { Text("Solo traccia (polyline)", color = Color.White) },
                                onClick = { routeOpen = false; onAddTraceOverlay() },
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Map, null, tint = TsmColors.Cyan) },
                                text = { Text("Mappa + traccia (widget)", color = Color.White) },
                                onClick = { routeOpen = false; onAddMapWidget() },
                            )
                            // "Rimuovi" ha senso solo con uno sfondo media (toglie
                            // l'overlay lasciando la foto/video). Senza media il
                            // percorso è sempre presente (è il contenuto della storia).
                            if (hasCustomBackground && routeOverlayMode != RouteOverlayMode.NONE) {
                                DropdownMenuItem(
                                    text = { Text("Rimuovi percorso", color = TsmColors.Danger) },
                                    onClick = { routeOpen = false; onRemoveRouteOverlay() },
                                )
                            }
                        }
                    }
                }
            }

            // ── Testo (toggle) ──────────────────────────────────────────────
            item {
                EditorTile(
                    icon = Icons.Filled.TextFields,
                    label = if (showTextSticker) "Modifica" else "Testo",
                    accent = TsmColors.Online,
                    highlighted = showTextSticker,
                    onClick = {
                        if (!showTextSticker) onAddText()
                    },
                    onLongClick = if (showTextSticker) onRemoveText else null,
                )
            }

            // ── Font (ciclo rapido) — solo quando c'è un testo ──────────────
            if (showTextSticker || selectedSticker == StoryStickerKind.TEXT) {
                item {
                    EditorTile(
                        icon = Icons.Filled.FontDownload,
                        label = textFont.label,
                        accent = TsmColors.Cyan,
                        highlighted = textFont != StoryFont.CLASSIC,
                        onClick = onCycleFont,
                    )
                }
            }

            // ── Colore (chip dinamico in base allo sticker selezionato) ────
            val colorTarget =
                when (selectedSticker) {
                    StoryStickerKind.TRACE, StoryStickerKind.MAP_SCENE, StoryStickerKind.MAP_WIDGET -> routeColor
                    StoryStickerKind.TEXT -> textColor
                    else -> null
                }
            if (colorTarget != null) {
                item {
                    Box {
                        EditorTile(
                            icon = Icons.Filled.Palette,
                            label = "Colore",
                            accent = colorTarget,
                            highlighted = true,
                            onClick = { colorOpen = true },
                        )
                        DropdownMenu(
                            expanded = colorOpen,
                            onDismissRequest = { colorOpen = false },
                            modifier = Modifier.background(TsmColors.CardElevated),
                        ) {
                            StoryStickerColors.forEach { c ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(20.dp)
                                                    .background(c, CircleShape)
                                                    .border(
                                                        2.dp,
                                                        if (c == colorTarget) Color.White else Color.White.copy(alpha = 0.18f),
                                                        CircleShape,
                                                    ),
                                            )
                                            Text(colorName(c), color = Color.White)
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
            }

            // ── Conferma testo (CTA) ────────────────────────────────────────
            if (textEditMode || selectedSticker == StoryStickerKind.TEXT) {
                item {
                    Button(
                        onClick = onConfirmText,
                        colors = ButtonDefaults.buttonColors(containerColor = TsmColors.Cyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(56.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Conferma",
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pulsante "tile" dell'editor storia: icona grande + label compatta sotto.
 * Quando `highlighted` mostra un alone leggero del colore accent — segnala che
 * l'elemento è ATTIVO sulla scena (es. il media è importato, il testo c'è, ecc.).
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun EditorTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val bg = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.10f)),
        )
    } else {
        Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
    }
    val border = if (highlighted) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)
    val tap = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable { onClick() }
    }
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 56.dp)
            .background(bg, RoundedCornerShape(14.dp))
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .then(tap),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) accent else Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(20.dp),
            )
            Text(
                label,
                color = if (highlighted) accent else Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                fontSize = 10.sp,
            )
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
