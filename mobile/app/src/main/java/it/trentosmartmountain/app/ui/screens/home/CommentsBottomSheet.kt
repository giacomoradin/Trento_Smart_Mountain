package it.trentosmartmountain.app.ui.screens.home

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.CommentItem
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.viewmodel.CommentsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val SheetSurface = Color(0xFF1C1C1E)
private val InputBackground = Color(0xFF2C2C2E)
private val TextSecondary = Color(0xFF8E8E93)
private val AccentCyan = Color(0xFF4DD0E1)

/**
 * BottomSheet modale dei commenti per un singolo target (Activity o Session).
 *
 * Layout (dal basso verso l'alto, ModalBottomSheet skirta dal basso):
 *   - Input row in fondo (sempre visibile, IME-friendly)
 *   - Lista commenti scrollabile (LazyColumn) sopra
 *   - Header titolo + count
 *
 * Apertura controllata via parametro `target`: quando `target` cambia
 * (non null), la sheet si apre e il VM esegue load. `onDismiss` la chiude.
 *
 * Il VM è Activity-scoped: riusato across diverse aperture. La logica
 * `openFor(targetId, kind)` resetta lo state per il nuovo target.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    target: CommentsTarget?,
    onDismiss: () -> Unit,
    viewModel: CommentsViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            (LocalContext.current as ComponentActivity).application,
        ),
    ),
) {
    if (target == null) return

    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    LaunchedEffect(target.id, target.kind) {
        viewModel.openFor(target.id, target.kind)
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Commenti",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(${state.count})",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Lista commenti
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading && state.items.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentCyan)
                        }
                    }
                    state.items.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Nessun commento ancora. Sii il primo!",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    else -> CommentList(
                        items = state.items,
                        currentUserId = state.currentUserId,
                        onDelete = viewModel::deleteComment,
                    )
                }
            }

            // Input row in fondo
            CommentInputRow(
                draft = state.draft,
                isSending = state.isSending,
                onDraftChange = viewModel::updateDraft,
                onSend = viewModel::submitComment,
            )
        }
    }
}

/** Modello "puntatore" su cosa stiamo commentando — usato come parametro
 *  della BottomSheet per evitare di passare due String separate. */
data class CommentsTarget(val id: String, val kind: String)

@Composable
private fun CommentList(
    items: List<CommentItem>,
    currentUserId: String?,
    onDelete: (commentId: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = items, key = { it._id }) { c ->
            CommentRow(
                comment = c,
                isOwn = currentUserId != null && c.userId?._id == currentUserId,
                onDelete = { onDelete(c._id) },
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    isOwn: Boolean,
    onDelete: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        AvatarImage(
            avatarUrl = comment.userId?.avatarUrl,
            fallbackName = comment.userId?.username,
            size = 32.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userId?.username ?: "Utente",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatRelative(comment.createdAt),
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.text,
                color = Color(0xFFD4D4D4),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (isOwn) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Elimina",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CommentInputRow(
    draft: String,
    isSending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val canSend = draft.trim().isNotEmpty() && draft.length <= 500 && !isSending
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Scrivi un commento…", color = TextSecondary) },
            singleLine = false,
            maxLines = 4,
            enabled = !isSending,
            supportingText = {
                if (draft.length > 450) {
                    Text(
                        "${draft.length}/500",
                        color = if (draft.length > 500) MaterialTheme.colorScheme.error else TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = canSend,
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color = AccentCyan,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Invia",
                    tint = if (canSend) AccentCyan else TextSecondary,
                )
            }
        }
    }
}

/** Formato relativo ("ora", "5 min fa", "2 h fa", "3 g fa", altrimenti dd/MM). */
private fun formatRelative(iso: String?): String {
    if (iso.isNullOrBlank()) return "ora"
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val sdfMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = runCatching {
        val trimmed = iso.removeSuffix("Z").take(23)
        if (trimmed.length > 19) sdfMs.parse(trimmed) else sdf.parse(trimmed.take(19))
    }.getOrNull() ?: return ""
    val seconds = (System.currentTimeMillis() - date.time) / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "ora"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}g"
        else -> SimpleDateFormat("dd/MM", Locale.ITALIAN).format(date)
    }
}
