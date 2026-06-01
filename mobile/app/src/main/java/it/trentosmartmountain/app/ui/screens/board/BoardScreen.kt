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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.BoardPost
import it.trentosmartmountain.app.ui.util.RelativeTime
import it.trentosmartmountain.app.viewmodel.BoardViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBg = Color(0xFF2C2C2E)
private val Cyan = Color(0xFF4DD0E1)
private val TextSecondary = Color(0xFF8E8E93)

private val DangerRed = Color(0xFFE53935)
private val WarnOrange = Color(0xFFFB8C00)
private val InfoBlue = Color(0xFF29B6F6)

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
                        if (manage) "La mia bacheca" else "Bacheca rifugi",
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
                    Icon(Icons.Filled.Add, contentDescription = "Nuovo post", tint = DarkSurface)
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading && state.items.isEmpty() -> Centered(padding) { CircularProgressIndicator(color = Cyan) }
            state.items.isEmpty() -> Centered(padding) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (manage) "Non hai ancora pubblicato nulla.\nTocca + per creare un avviso."
                        else "Nessuna comunicazione dai rifugi al momento.",
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
                        canDelete = manage,
                        onDelete = { viewModel.delete(post._id) },
                    )
                }
            }
        }
    }

    if (showCompose) {
        ComposeDialog(
            isSubmitting = state.isSubmitting,
            onDismiss = { showCompose = false },
            onPublish = { type, title, body ->
                viewModel.create(type, title, body) { showCompose = false }
            },
        )
    }
}

@Composable
private fun BoardPostCard(post: BoardPost, canDelete: Boolean, onDelete: () -> Unit) {
    val (color, icon, label) = typeStyle(post.type)
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
                        Text(label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Elimina", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(post.title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(post.body, color = Color(0xFFD0D0D5), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "${post.refugeName} · ${RelativeTime.short(post.createdAt)}",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onPublish: (type: String, title: String, body: String) -> Unit,
) {
    var type by remember { mutableStateOf("info") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = CardBg,
        onDismissRequest = onDismiss,
        title = { Text("Nuovo post", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("CATEGORIA", color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip("Info", "info", type) { type = it }
                    TypeChip("Avviso", "avviso", type) { type = it }
                    TypeChip("Pericolo", "pericolo", type) { type = it }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    placeholder = { Text("Titolo", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 2000) body = it },
                    placeholder = { Text("Testo della comunicazione", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = fieldColors(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(type, title, body) },
                enabled = !isSubmitting && title.isNotBlank() && body.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = DarkSurface, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text("Pubblica", color = DarkSurface, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = TextSecondary) } },
    )
}

@Composable
private fun TypeChip(label: String, value: String, selected: String, onSelect: (String) -> Unit) {
    val (color, _, _) = typeStyle(value)
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

private fun typeStyle(type: String): Triple<Color, ImageVector, String> = when (type) {
    "pericolo" -> Triple(DangerRed, Icons.Filled.Warning, "PERICOLO")
    "avviso" -> Triple(WarnOrange, Icons.Filled.Campaign, "AVVISO")
    else -> Triple(InfoBlue, Icons.Filled.Info, "INFO")
}

@Composable
private fun Centered(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { content() }
}
