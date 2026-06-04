package it.trentosmartmountain.app.ui.screens.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs
import it.trentosmartmountain.app.ui.screens.home.story.StoryEditorCanvas
import it.trentosmartmountain.app.ui.screens.home.story.StoryEditorControlsBar
import it.trentosmartmountain.app.ui.screens.home.story.StoryStickerKind
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.StoryComposerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryComposerScreen(
    args: StoryComposerArgs,
    onClose: () -> Unit,
    onPublished: () -> Unit,
    viewModel: StoryComposerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hostActivity = context as ComponentActivity

    LaunchedEffect(args) { viewModel.initFromArgs(args) }

    LaunchedEffect(state.published) {
        if (state.published) {
            Toast.makeText(context, "Storia pubblicata!", Toast.LENGTH_SHORT).show()
            onPublished()
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.onMediaPicked(context.contentResolver, it) } }

    var pendingCaptureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingCaptureUri?.let { viewModel.onMediaPicked(context.contentResolver, it) }
    }
    // Cattura video a BASSA risoluzione con limite di durata e di DIMENSIONE
    // hardware: la fotocamera interrompe la registrazione al raggiungimento del
    // size-limit, così il file resta sotto il cap del backend (evita l'errore
    // "media troppo grande" anche su clip di pochi secondi ad alto bitrate).
    val captureVideo = rememberLauncherForActivityResult(remember { LowResCaptureVideo() }) { ok ->
        if (ok) pendingCaptureUri?.let { viewModel.onMediaPicked(context.contentResolver, it) }
    }
    fun launchCapture(isVideo: Boolean) {
        val ext = if (isVideo) "mp4" else "jpg"
        val file = java.io.File(context.cacheDir, "story_capture_${System.currentTimeMillis()}.$ext")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        pendingCaptureUri = uri
        if (isVideo) captureVideo.launch(uri) else takePhoto.launch(uri)
    }

    val hasRoute = state.routePoints.size >= 2
    var editorCanvasWidthPx by remember { mutableStateOf(0f) }
    var editorCanvasHeightPx by remember { mutableStateOf(0f) }

    Scaffold(
        containerColor = TsmColors.FeedBackground,
        topBar = {
            // Header con badge sottile "STORIA · 24H" per dare contesto editoriale.
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = TsmColors.Cyan,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "STORIA · 24H",
                                color = TsmColors.Cyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            "Crea la tua storia",
                            color = TsmColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = TsmColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TsmColors.FeedBackground),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
        ) {
            StoryEditorCanvas(
                routePoints = state.routePoints,
                hasCustomBackground = state.hasCustomBackground,
                hasMediaBackground = state.hasMediaBackground,
                mediaKind = state.mediaKind,
                mediaDataUri = state.mediaDataUri,
                isEncoding = state.isEncoding,
                routeOverlayMode = state.routeOverlayMode,
                mapSceneTransform = state.mapSceneTransform,
                routeTransform = state.routeTransform,
                mapWidgetTransform = state.mapWidgetTransform,
                routeColor = state.routeColor,
                selectedSticker = state.selectedSticker,
                onSelectSticker = viewModel::selectSticker,
                onMapSceneTransformChange = viewModel::onMapSceneTransformChange,
                onRouteTransformChange = viewModel::onRouteTransformChange,
                onMapWidgetTransformChange = viewModel::onMapWidgetTransformChange,
                showTextSticker = state.showTextSticker,
                textEditMode = state.textEditMode,
                floatingText = state.floatingText,
                textTransform = state.textTransform,
                textColor = state.textColor,
                textFont = state.textFont,
                onTextTransformChange = viewModel::onTextTransformChange,
                onEditorCanvasSize = { w, h ->
                    editorCanvasWidthPx = w
                    editorCanvasHeightPx = h
                },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.height(8.dp))

            StoryEditorControlsBar(
                // Per le opzioni mappa/traccia conta che ci sia un media di sfondo
                // (foto O video): così anche sul video si può aggiungere la mappa (#5).
                hasCustomBackground = state.hasMediaBackground,
                hasRoute = hasRoute,
                routeOverlayMode = state.routeOverlayMode,
                selectedSticker = state.selectedSticker,
                routeColor = state.routeColor,
                textColor = state.textColor,
                showTextSticker = state.showTextSticker,
                textEditMode = state.textEditMode,
                textFont = state.textFont,
                onCycleFont = viewModel::cycleTextFont,
                onImportGallery = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onImportPhoto = { launchCapture(isVideo = false) },
                onImportVideo = { launchCapture(isVideo = true) },
                onRemoveBackground = viewModel::clearMedia,
                onAddTraceOverlay = viewModel::addTraceOverlay,
                onAddMapWidget = viewModel::addMapWidgetOverlay,
                onRemoveRouteOverlay = viewModel::removeRouteOverlay,
                onAddText = viewModel::addTextSticker,
                onConfirmText = viewModel::confirmTextSticker,
                onRemoveText = viewModel::removeTextSticker,
                onRouteColor = viewModel::onRouteColorChange,
                onTextColor = viewModel::onTextColorChange,
            )

            if (state.textEditMode || state.selectedSticker == StoryStickerKind.TEXT) {
                OutlinedTextField(
                    value = state.floatingText,
                    onValueChange = viewModel::onFloatingTextChange,
                    label = { Text("Testo sticker") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                )
            }

            OutlinedTextField(
                value = state.caption,
                onValueChange = viewModel::onCaptionChange,
                label = { Text("Didascalia") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
            )

            state.error?.let {
                Text(it, color = TsmColors.Danger, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(10.dp))
            // CTA Pubblica: pill premium con icona Send + label + counter caption.
            // Quando encoding/publishing in corso, mostra spinner + descrizione.
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TsmColors.Cyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shadowElevation = 6.dp,
                onClick = {
                    val w = editorCanvasWidthPx.takeIf { it > 0f } ?: 1080f
                    val h = editorCanvasHeightPx.takeIf { it > 0f } ?: 1920f
                    if (!state.isPublishing && !state.isEncoding) {
                        viewModel.publish(args, hostActivity, w, h)
                    }
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (state.isPublishing || state.isEncoding) {
                        CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (state.isEncoding) "Preparo media…" else "Pubblicazione…",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    } else {
                        Icon(Icons.Filled.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "PUBBLICA STORIA",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * Contratto di cattura video a bassa risoluzione con limiti di durata (10s) e di
 * DIMENSIONE file (~2.4MB): MediaStore.EXTRA_SIZE_LIMIT fa interrompere la
 * registrazione alla fotocamera quando il file raggiunge il limite, così il clip
 * resta sotto il cap del backend (storie ≤ ~3.8MB base64). I flag sono best-effort
 * (alcune camera app li ignorano), ma sulle camera stock evitano l'errore
 * "media troppo grande" sui video brevi ad alto bitrate.
 */
private class LowResCaptureVideo : ActivityResultContracts.CaptureVideo() {
    override fun createIntent(context: android.content.Context, input: android.net.Uri): android.content.Intent {
        return super.createIntent(context, input).apply {
            putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 0) // low quality
            putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 10) // secondi
            putExtra(android.provider.MediaStore.EXTRA_SIZE_LIMIT, 2_400_000L) // ~2.4MB
        }
    }
}
