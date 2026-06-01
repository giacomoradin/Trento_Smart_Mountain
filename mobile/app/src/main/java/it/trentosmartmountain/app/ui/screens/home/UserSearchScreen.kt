package it.trentosmartmountain.app.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.UserSearchItem
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.viewmodel.UserSearchViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val TextSecondary = Color(0xFF8E8E93)

/**
 * Schermata "Cerca persone da seguire" — cuore del flusso "aggiungi amici".
 *
 * Campo di ricerca con debounce (vedi [UserSearchViewModel]); ogni risultato ha
 * avatar + username (tap → profilo) e un bottone Segui/Seguito (follow
 * ottimistico). Tre stati: hint iniziale, nessun risultato, lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    viewModel: UserSearchViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = { Text("Trova persone", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Cerca per username", color = TextSecondary) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancella", tint = TextSecondary)
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                    cursorColor = AccentCyan,
                ),
            )

            when {
                state.isLoading && state.results.isEmpty() -> CenteredBox(padding = PaddingValues(0.dp)) {
                    CircularProgressIndicator(color = AccentCyan)
                }
                !state.hasSearched -> HintBlock(
                    emoji = "🔍",
                    text = "Digita almeno 2 caratteri per cercare altri escursionisti da seguire.",
                )
                state.results.isEmpty() -> HintBlock(
                    emoji = "🤷",
                    text = "Nessun utente trovato per \"${state.query.trim()}\".",
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.results, key = { it.user?._id ?: it.hashCode().toString() }) { item ->
                        UserSearchRow(
                            item = item,
                            followInFlight = item.user?._id in state.followInFlight,
                            onClick = { item.user?._id?.let(onUserClick) },
                            onToggleFollow = { viewModel.toggleFollow(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSearchRow(
    item: UserSearchItem,
    followInFlight: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
) {
    val user = item.user
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(avatarUrl = user?.avatarUrl, fallbackName = user?.username ?: "?", size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            user?.username ?: "—",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        FollowPill(
            isFollowing = item.isFollowedByMe,
            isLoading = followInFlight,
            onClick = onToggleFollow,
        )
    }
}

@Composable
private fun FollowPill(
    isFollowing: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            } else {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Seguito")
            }
        }
    } else {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = DarkSurface, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            } else {
                Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(16.dp), tint = DarkSurface)
                Spacer(Modifier.width(4.dp))
                Text("Segui", color = DarkSurface, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HintBlock(emoji: String, text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CenteredBox(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { content() }
}
