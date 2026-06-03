package it.trentosmartmountain.app.ui.screens.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.util.AvatarUtils
import it.trentosmartmountain.app.viewmodel.StoryComposerViewModel

/**
 * Composer storie (Fase C): pick di una foto/video breve dalla galleria,
 * didascalia opzionale, pubblicazione. L'overlay di tracciamento + i riferimenti
 * arrivano già pronti negli [args] dall'origine (dettaglio attività/sessione).
 */
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

    LaunchedEffect(state.published) {
        if (state.published) {
            Toast.makeText(context, "Storia pubblicata!", Toast.LENGTH_SHORT).show()
            onPublished()
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.onMediaPicked(context.contentResolver, it) } }

    // Cattura da fotocamera: scriviamo il media in un file di cache esposto via
    // FileProvider e lo passiamo all'app camera (TakePicture/CaptureVideo →
    // ACTION_IMAGE/VIDEO_CAPTURE, nessun permesso CAMERA dichiarato/richiesto).
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = if (args.type == "planned_session") {
                    "Condividi l'escursione pianificata: i tuoi follower potranno unirsi direttamente dalla storia."
                } else {
                    "Condividi la tua attività con una foto o un breve video."
                },
                color = TsmColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))

            // ── Area media (tap per scegliere) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TsmColors.CardElevated)
                    .clickable {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.isEncoding -> CircularProgressIndicator(color = TsmColors.Cyan)
                    state.mediaKind == "image" && state.mediaDataUri != null -> {
                        val bmp = remember(state.mediaDataUri) { AvatarUtils.decodeDataUri(state.mediaDataUri) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text("Anteprima non disponibile", color = TsmColors.TextSecondary)
                        }
                    }
                    state.mediaKind == "video" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = TsmColors.Cyan, modifier = Modifier.size(56.dp))
                        Text("Video pronto", color = TsmColors.TextSecondary)
                    }
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = TsmColors.Cyan, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tocca per aggiungere foto o video", color = TsmColors.TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    modifier = Modifier.weight(1f),
                ) { Text("Galleria") }
                OutlinedButton(
                    onClick = { launchCapture(isVideo = false) },
                    modifier = Modifier.weight(1f),
                ) { Text("Foto") }
                OutlinedButton(
                    onClick = { launchCapture(isVideo = true) },
                    modifier = Modifier.weight(1f),
                ) { Text("Video") }
            }
            if (state.mediaDataUri != null) {
                TextButton(onClick = { viewModel.clearMedia() }) {
                    Text("Rimuovi media", color = TsmColors.Danger)
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.caption,
                onValueChange = viewModel::onCaptionChange,
                label = { Text("Didascalia (opzionale)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = TsmColors.Danger, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { viewModel.publish(args) },
                enabled = !state.isPublishing && !state.isEncoding,
                modifier = Modifier.fillMaxWidth().height(52.dp),
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
            Text(
                "La storia sarà visibile ai tuoi follower per 24 ore.",
                color = TsmColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
