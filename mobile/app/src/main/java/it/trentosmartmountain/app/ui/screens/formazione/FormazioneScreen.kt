package it.trentosmartmountain.app.ui.screens.formazione

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.remote.dto.QuizCategoryProgressResponse
import it.trentosmartmountain.app.ui.components.ListSkeleton
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.FormazioneViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val TextSecondary = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormazioneScreen(
    onBack: () -> Unit,
    onNavigateToQuiz: (categorySlug: String, categoryName: String) -> Unit,
    viewModel: FormazioneViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh della progressione a ogni ritorno sulla schermata (es. dopo aver
    // completato un quiz): il backend ha già i tentativi `passed`, qui li ri-leggiamo
    // così punti e "N/M completati" non restano fermi sui valori stale.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) {
    it.trentosmartmountain.app.ui.components.TsmAuroraBackground(
        modifier = Modifier.fillMaxSize(),
        particleCount = 14,
    )
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Formazione", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> ListSkeleton(modifier = Modifier.fillMaxSize().padding(padding))
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "", color = Color.Red)
            }
            uiState.categories.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        tint = AccentCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(52.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Nessuna categoria disponibile",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "I contenuti formativi verranno caricati a breve.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            else -> LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(uiState.categories) { cat ->
                    CategoryCard(cat = cat, onClick = {
                        onNavigateToQuiz(cat.category.slug, cat.category.name)
                    })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
    }
}

@Composable
private fun CategoryCard(cat: QuizCategoryProgressResponse, onClick: () -> Unit) {
    val barColor = runCatching { Color(android.graphics.Color.parseColor(cat.category.color)) }.getOrDefault(AccentCyan)

    val completed = cat.totalQuizzes > 0 && cat.passedByMe >= cat.totalQuizzes

    TsmGlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Chip-icona nella tinta della categoria: distingue i temi a colpo d'occhio.
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(barColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.School, contentDescription = null, tint = barColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(cat.category.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${cat.passedByMe}/${cat.totalQuizzes} quiz · ${cat.earnedByMe}/${cat.totalCredits} pt",
                        color = barColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (completed) {
                    Surface(shape = RoundedCornerShape(8.dp), color = TsmColors.Success.copy(alpha = 0.18f)) {
                        Text("✓ Completato", color = TsmColors.Success, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TsmColors.TextTertiary)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { cat.progressPct.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = Color(0xFF3A3A3C),
            )
        }
    }
}
