package it.trentosmartmountain.app.ui.screens.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
    val captureVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
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
            TopAppBar(
                title = { Text("Nuova storia", color = TsmColors.TextPrimary, fontWeight = FontWeight.Bold) },
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
                onTextTransformChange = viewModel::onTextTransformChange,
                onEditorCanvasSize = { w, h ->
                    editorCanvasWidthPx = w
                    editorCanvasHeightPx = h
                },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.height(8.dp))

            StoryEditorControlsBar(
                hasCustomBackground = state.hasCustomBackground,
                hasRoute = hasRoute,
                routeOverlayMode = state.routeOverlayMode,
                selectedSticker = state.selectedSticker,
                routeColor = state.routeColor,
                textColor = state.textColor,
                showTextSticker = state.showTextSticker,
                textEditMode = state.textEditMode,
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

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val w = editorCanvasWidthPx.takeIf { it > 0f } ?: 1080f
                    val h = editorCanvasHeightPx.takeIf { it > 0f } ?: 1920f
                    viewModel.publish(args, hostActivity, w, h)
                },
                enabled = !state.isPublishing && !state.isEncoding,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TsmColors.Cyan),
                shape = RoundedCornerShape(10.dp),
            ) {
                if (state.isPublishing) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("PUBBLICA STORIA", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
