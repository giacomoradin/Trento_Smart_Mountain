package it.trentosmartmountain.app.ui.screens.board

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.remote.dto.BoardPost
import it.trentosmartmountain.app.ui.components.ListSkeleton
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.util.RelativeTime
import it.trentosmartmountain.app.viewmodel.BoardViewModel

private val DarkSurface = TsmColors.FeedBackground
private val CardBg = TsmColors.CardElevated
private val Cyan = TsmColors.Cyan
private val TextSecondary = TsmColors.TextSecondary

private val DangerRed = TsmColors.Offline
private val WarnOrange = TsmColors.Warning
private val InfoBlue = TsmColors.Info

/**
 * Bacheca rifugi.
 *  - [manage] = false → consultazione (tutti i post, sola lettura) per gli utenti;
 *  - [manage] = true  → gestione lato rifugista (i propri post + crea/elimina).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    manage: Boolean,
    onBack: () -> Unit,
    viewModel: BoardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCompose by remember { mutableStateOf(false) }
    var editPost by remember { mutableStateOf<BoardPost?>(null) }

    LaunchedEffect(manage) { viewModel.load(manage) }
    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (manage) R.string.board_title_manage else R.string.board_title_user),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
        },
        floatingActionButton = {
            if (manage) {
                FloatingActionButton(onClick = { showCompose = true }, containerColor = Cyan) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_new_post), tint = DarkSurface)
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading && state.items.isEmpty() -> ListSkeleton(modifier = Modifier.padding(padding))
            state.items.isEmpty() -> Centered(padding) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(if (manage) R.string.board_empty_manage else R.string.board_empty_user),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    start = 12.dp, end = 12.dp, bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it._id }) { post ->
                    BoardPostCard(
                        post = post,
                        canManage = manage,
                        onEdit = { editPost = post },
                        onDelete = { viewModel.delete(post._id) },
                    )
                }
            }
        }
    }

    if (showCompose || editPost != null) {
        ComposeDialog(
            initial = editPost,
            isSubmitting = state.isSubmitting,
            onDismiss = { showCompose = false; editPost = null },
            onPublish = { type, title, body, validUntil ->
                val target = editPost
                if (target != null) {
                    viewModel.update(target._id, type, title, body, validUntil) {
                        showCompose = false; editPost = null
                    }
                } else {
                    viewModel.create(type, title, body, validUntil) { showCompose = false }
                }
            },
        )
    }
}

@Composable
private fun BoardPostCard(post: BoardPost, canManage: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val (color, icon) = typeStyle(post.type)
    Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.16f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(boardBadgeRes(post.type)), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (canManage) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit), tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.cd_delete), tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(post.title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(post.body, color = Color(0xFFD0D0D5), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.board_footer, post.refugeName, RelativeTime.short(post.createdAt)),
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeDialog(
    initial: BoardPost?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onPublish: (type: String, title: String, body: String, validUntil: String?) -> Unit,
) {
    var type by remember { mutableStateOf(initial?.type ?: "info") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var validUntilMs by remember { mutableStateOf(isoToMillis(initial?.validUntil)) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        containerColor = CardBg,
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(if (initial != null) R.string.board_edit_post else R.string.board_new_post),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(stringResource(R.string.board_category), color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip(stringResource(R.string.board_type_info), "info", type) { type = it }
                    TypeChip(stringResource(R.string.board_type_avviso), "avviso", type) { type = it }
                    TypeChip(stringResource(R.string.board_type_pericolo), "pericolo", type) { type = it }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    placeholder = { Text(stringResource(R.string.board_title_hint), color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 2000) body = it },
                    placeholder = { Text(stringResource(R.string.board_body_hint), color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.board_deadline), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(validUntilMs?.let { formatDate(it) } ?: stringResource(R.string.board_deadline_none), color = Cyan)
                    }
                    if (validUntilMs != null) {
                        IconButton(onClick = { validUntilMs = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove_deadline), tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(type, title, body, validUntilMs?.let { millisToIso(it) }) },
                enabled = !isSubmitting && title.isNotBlank() && body.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = DarkSurface, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text(stringResource(if (initial != null) R.string.board_save else R.string.board_publish), color = DarkSurface, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = TextSecondary) } },
    )

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = validUntilMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { validUntilMs = dpState.selectedDateMillis; showDatePicker = false }) {
                    Text(stringResource(R.string.action_ok), color = Cyan)
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel), color = TextSecondary) } },
        ) { DatePicker(state = dpState) }
    }
}

// ── Helpers data ─────────────────────────────────────────────────────────────

private fun isoToMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        val cleaned = iso.substringBefore(".").removeSuffix("Z").take(19)
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .parse(cleaned)!!.time
    }.getOrNull()
}

private fun millisToIso(ms: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        .format(java.util.Date(ms))

private fun formatDate(ms: Long): String =
    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ITALIAN).format(java.util.Date(ms))

@Composable
private fun TypeChip(label: String, value: String, selected: String, onSelect: (String) -> Unit) {
    val (color, _) = typeStyle(value)
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.25f),
            selectedLabelColor = color,
            containerColor = DarkSurface,
            labelColor = TextSecondary,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Cyan,
    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
    cursorColor = Cyan,
)

private fun typeStyle(type: String): Pair<Color, ImageVector> = when (type) {
    "pericolo" -> DangerRed to Icons.Filled.Warning
    "avviso" -> WarnOrange to Icons.Filled.Campaign
    else -> InfoBlue to Icons.Filled.Info
}

/** Etichetta (string resource) del badge categoria. */
private fun boardBadgeRes(type: String): Int = when (type) {
    "pericolo" -> R.string.board_badge_pericolo
    "avviso" -> R.string.board_badge_avviso
    else -> R.string.board_badge_info
}

@Composable
private fun Centered(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { content() }
}
