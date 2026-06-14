package it.trentosmartmountain.app.ui.screens.quiz

import android.app.Application
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Brush
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionResponse
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.ui.components.TsmGradientButton
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.viewmodel.QuizState
import it.trentosmartmountain.app.viewmodel.QuizViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentPink = Color(0xFFE91E8C)
private val AccentCyan = Color(0xFF4DD0E1)
private val TextSecondary = Color(0xFF8E8E93)
private val GreenBg = Color(0xFF1A3D1A)
private val PinkBg = Color(0xFF3D1A2A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizId: String? = null,
    categorySlug: String? = null,
    onClose: () -> Unit,
    onResult: (QuizSubmissionResponse, String, String) -> Unit,
    viewModel: QuizViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(quizId, categorySlug) {
        when {
            !quizId.isNullOrBlank() -> viewModel.loadQuiz(quizId)
            !categorySlug.isNullOrBlank() -> viewModel.loadQuizFromCategory(categorySlug)
        }
    }

    Scaffold(
        topBar = {
            when (val s = state) {
                is QuizState.Question -> {
                    TopAppBar(
                        title = {
                            Column {
                                Text(s.quiz.category.name, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Text("Domanda ${s.currentIndex + 1} di ${s.quiz.questions.size}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                            }
                        },
                        actions = {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("+${s.quiz.creditsReward}", color = AccentCyan, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                    )
                }
                else -> TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                )
            }
        },
        containerColor = DarkSurface,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is QuizState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan)
                }
                is QuizState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = Color.Red)
                }
                is QuizState.Submitting -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentCyan)
                        Spacer(Modifier.height(12.dp))
                        Text("Invio in corso…", color = TextSecondary)
                    }
                }
                is QuizState.Result -> {
                    LaunchedEffect(s) { onResult(s.submission, s.quiz.id, s.quiz.title) }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentCyan)
                    }
                }
                is QuizState.AllCompleted -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Tutti i quiz superati!", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Hai completato l'intera categoria.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(20.dp))
                        TsmGradientButton(
                            text = "TORNA ALLA FORMAZIONE",
                            onClick = onClose,
                            fill = Brush.horizontalGradient(listOf(AccentGreen, Color(0xFF2E7D32))),
                        )
                    }
                }
                is QuizState.Question -> {
                    val quiz = s.quiz
                    val question = quiz.questions[s.currentIndex]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Spacer(Modifier.height(8.dp))
                        // Already-passed banner: solo prima domanda così non distrae durante il quiz.
                        // L'utente vede subito che sta facendo "ripasso" (no crediti aggiuntivi).
                        if (quiz.alreadyPassed && s.currentIndex == 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A1A)),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text(
                                    "Quiz già superato — puoi rispondere per ripassare ma non riceverai crediti aggiuntivi.",
                                    modifier = Modifier.padding(12.dp),
                                    color = Color(0xFFE6B800),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        LinearProgressIndicator(
                            progress = { (s.currentIndex + 1).toFloat() / quiz.questions.size },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = AccentGreen,
                            trackColor = Color(0xFF3A3A3C),
                        )
                        Spacer(Modifier.height(16.dp))

                        // Question card (glass)
                        TsmGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                            Text(
                                text = question.text,
                                modifier = Modifier.padding(16.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Options — stato selezionato evidenziato (bordo+sfondo accent, lettera accent).
                        question.choices.forEachIndexed { idx, choice ->
                            val isSelected = s.selectedChoice == idx
                            val borderColor = if (isSelected && !s.isAnswered) AccentCyan else Color(0xFF3A3A3C)
                            val bgColor = if (isSelected && !s.isAnswered) AccentCyan.copy(alpha = 0.12f) else CardBackground
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clickable(enabled = !s.isAnswered) { viewModel.selectChoice(idx) },
                                shape = RoundedCornerShape(12.dp),
                                color = bgColor,
                                border = BorderStroke(if (isSelected && !s.isAnswered) 1.5.dp else 1.dp, borderColor),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected && !s.isAnswered) AccentCyan else Color(0xFF3A3A3C)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = ('A' + idx).toString(),
                                            color = if (isSelected && !s.isAnswered) Color(0xFF0A1420) else TextSecondary,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        choice,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val greenFill = Brush.horizontalGradient(listOf(AccentGreen, Color(0xFF2E7D32)))
                        // Confirm button (when not answered yet)
                        if (!s.isAnswered && s.selectedChoice != null) {
                            TsmGradientButton(
                                text = "CONFERMA RISPOSTA",
                                onClick = { viewModel.confirmAnswer() },
                                modifier = Modifier.fillMaxWidth(),
                                fill = greenFill,
                            )
                        }

                        // "Prossima domanda" button (after answered — no per-question feedback shown since we do batched submit)
                        if (s.isAnswered) {
                            val isLast = s.currentIndex == quiz.questions.size - 1
                            TsmGradientButton(
                                text = if (isLast) "INVIA QUIZ →" else "PROSSIMA DOMANDA →",
                                onClick = { viewModel.nextQuestion() },
                                modifier = Modifier.fillMaxWidth(),
                                fill = greenFill,
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
