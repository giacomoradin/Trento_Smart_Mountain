package it.trentosmartmountain.app.ui.screens.quiz

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.trentosmartmountain.app.data.remote.dto.BreakdownItem
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionResponse

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentRed = Color(0xFFE91E63)
private val TextSecondary = Color(0xFF8E8E93)
private val CorrectBg = Color(0xFF1A3D1A)
private val WrongBg = Color(0xFF3D1A2A)

@Composable
fun QuizResultScreen(
    submission: QuizSubmissionResponse,
    quizTitle: String,
    onBackToFormazione: () -> Unit,
    onRetry: () -> Unit,
) {
    val score = (submission.score * 100).toInt()
    val passed = submission.passed
    val accentColor = if (passed) AccentGreen else AccentRed

    Surface(modifier = Modifier.fillMaxSize(), color = DarkSurface) {
        // LazyColumn so the breakdown list is scrollable and we don't blow up the
        // composition with many Card children inside a verticalScroll Column.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Header: ring + score ───────────────────────────────────────
            item {
                Spacer(Modifier.height(32.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    CircularProgressIndicator(
                        progress = { submission.score.toFloat() },
                        modifier = Modifier.fillMaxSize(),
                        color = accentColor,
                        trackColor = Color(0xFF3A3A3C),
                        strokeWidth = 10.dp,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$score%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                        Text("${submission.correctCount}/${submission.totalQuestions} corrette", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = if (passed) "Quiz Superato! 🎉" else "Non hai superato",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
                Text(
                    text = if (passed) "Hai raggiunto il punteggio minimo del 70%" else "Punteggio minimo 70% non raggiunto",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(20.dp))
            }

            // ── Credits card (only if earned) ──────────────────────────────
            if (submission.creditsAwarded > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A3A)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("+${submission.creditsAwarded}", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                                Text("Social Credits", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(quizTitle, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                submission.newTotalCredits?.let {
                                    Text("Totale: %,d crediti".format(it), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            } else if (passed) {
                // Quiz superato ma 0 crediti → era già stato passato in precedenza.
                // Lo segnaliamo all'utente per evitare confusione ("perché non ho avuto crediti?").
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A1A)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            "Quiz già superato in precedenza — nessun credito aggiuntivo assegnato.",
                            modifier = Modifier.padding(14.dp),
                            color = Color(0xFFE6B800),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Stats row ──────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatCell("CORRETTE", "${submission.correctCount}", AccentGreen)
                        StatCell("ERRATE", "${submission.totalQuestions - submission.correctCount}", AccentRed)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Breakdown header ───────────────────────────────────────────
            if (submission.breakdown.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Rivedi le risposte",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }

                // ── Per-question breakdown ─────────────────────────────────
                itemsIndexed(submission.breakdown, key = { _, it -> it.questionId }) { index, item ->
                    BreakdownCard(index = index + 1, item = item)
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── Action buttons ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onBackToFormazione,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                ) {
                    Text("TORNA ALLA FORMAZIONE", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A3C)),
                ) {
                    Text("RIPROVA", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BreakdownCard(index: Int, item: BreakdownItem) {
    val (bg, accent, icon) = if (item.isCorrect) {
        Triple(CorrectBg, AccentGreen, Icons.Default.Check)
    } else {
        Triple(WrongBg, AccentRed, Icons.Default.Close)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Domanda $index — ${if (item.isCorrect) "Corretta" else "Errata"}",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (!item.isCorrect) {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Hai scelto l'opzione ${('A' + item.choiceIndex)}; la risposta corretta è ${('A' + item.correctIndex)}.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (item.explanation.isNotBlank()) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = item.explanation,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 28.sp)
    }
}
