package it.trentosmartmountain.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGradientButton

private val DarkSurface = Color(0xFF1C1C1E)
private val AccentCyan = Color(0xFF4DD0E1)
private val TextSecondary = Color(0xFF8E8E93)

/**
 * Scaffold comune ai 3 step di onboarding. Riusa le edit screen profilo v2
 * come "body" — la stessa UI di edit, con barra di progresso, titolo step,
 * pulsanti "Salta" / "Continua" / "Salta tutto".
 *
 * Pattern: l'utente vede la stessa form della schermata di edit (così non
 * deve impararla due volte), ma circondata da un wrapper con navigation
 * onboarding. Niente AppBar — full-screen.
 */
@Composable
fun OnboardingStepScaffold(
    stepIndex: Int, // 1..3
    totalSteps: Int = 3,
    title: String,
    subtitle: String,
    isSaving: Boolean = false,
    onSkipStep: () -> Unit,
    onSkipAll: () -> Unit,
    onSaveAndContinue: () -> Unit,
    saveLabel: String = if (stepIndex == totalSteps) "TERMINA" else "SALVA E CONTINUA",
    body: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(DarkSurface)) {
        TsmAuroraBackground(modifier = Modifier.fillMaxSize(), particleCount = 14)
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            // Progress bar + step counter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Passo $stepIndex di $totalSteps",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSkipAll) {
                    Text("Salta tutto", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { stepIndex.toFloat() / totalSteps },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = AccentCyan,
                trackColor = Color(0xFF3A3A3C),
            )

            Spacer(Modifier.height(20.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))

            // Body scroll-able dell'utente (form fields)
            Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                body()
            }

            Spacer(Modifier.height(12.dp))

            // Bottoni di azione: "Salta passo" outline + "Salva e continua" pieno
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onSkipStep,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("SALTA", color = Color.White, fontWeight = FontWeight.Bold)
                }
                TsmGradientButton(
                    text = if (isSaving) "ATTENDI…" else saveLabel,
                    onClick = onSaveAndContinue,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1.4f),
                    height = 48.dp,
                    fill = Brush.horizontalGradient(listOf(AccentCyan, Color(0xFF0097A7))),
                    contentColor = DarkSurface,
                )
            }
        }
    }
}
