package it.trentosmartmountain.app.ui.screens.nfc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import it.trentosmartmountain.app.data.remote.dto.NfcScanResponse

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentRed = Color(0xFFE91E63)
private val TextSecondary = Color(0xFF8E8E93)

@Composable
fun NfcResultScreen(
    response: NfcScanResponse,
    onBack: () -> Unit,
) {
    val isSuccess = response.ok && (response.creditsAwarded ?: 0) > 0
    val isAlreadyScanned = response.ok && response.alreadyScannedToday == true
    val isOutOfRange = !response.ok && response.reason == "OUT_OF_RANGE"

    val accentColor = when {
        isSuccess -> AccentGreen
        isAlreadyScanned -> AccentCyan
        else -> AccentRed
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkSurface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val icon = when {
                isSuccess -> Icons.Default.Check
                isAlreadyScanned -> Icons.Default.Repeat
                else -> Icons.Default.LocationOff
            }
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(90.dp))
            Spacer(Modifier.height(20.dp))

            Text(
                text = when {
                    isSuccess -> "Checkpoint registrato!"
                    isAlreadyScanned -> "Già scansionato oggi"
                    isOutOfRange -> "Troppo lontano dal totem"
                    else -> "Scansione non riuscita"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    isSuccess -> "Hai guadagnato ${response.creditsAwarded} Social Credits!"
                    isAlreadyScanned -> "Potrai scansionare di nuovo questo totem tra 24 ore."
                    isOutOfRange -> "Sei a ${response.distance} m dal totem (max ${response.totem?.radius ?: 50} m)."
                    else -> "Verifica la connessione e riprova."
                },
                color = TextSecondary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(24.dp))

            if (isSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A3A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("+${response.creditsAwarded}", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                        Text("Social Credits guadagnati", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        response.totem?.name?.let { name ->
                            Spacer(Modifier.height(8.dp))
                            Text(name, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        response.newTotalCredits?.let { total ->
                            Text("Totale: %,d crediti".format(total), color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            ) {
                Text("TORNA AL PROFILO", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
