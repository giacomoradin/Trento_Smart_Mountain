package it.trentosmartmountain.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dialog "Pubblica" per condividere un'attività o sessione sul feed sociale.
 *
 * Authoring manuale come da decisione di prodotto (Sprint 2): l'attività è
 * privata di default; l'utente può scegliere se e quando pubblicarla.
 *
 * Cap caption a 200 caratteri (vincolo server-side Joi). Counter visibile
 * sotto il TextField fornisce feedback live e diventa rosso oltre il limite.
 * Il bottone "Pubblica" rimane abilitato anche con caption vuota (è opzionale).
 *
 * @param activityName Nome dell'attività mostrato nell'header del dialog.
 * @param onDismiss Chiusura senza pubblicare.
 * @param onShare Conferma: invocato con la caption (trim-ed, può essere blank).
 */
@Composable
fun ShareActivityDialog(
    activityName: String,
    onDismiss: () -> Unit,
    onShare: (caption: String) -> Unit,
) {
    var caption by remember { mutableStateOf("") }
    val overLimit = caption.length > MAX_CAPTION_LENGTH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Condividi sul feed",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    activityName.ifBlank { "Attività" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Aggiungi una caption opzionale (max $MAX_CAPTION_LENGTH caratteri):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Es. \"Bellissimo giro in Brenta!\"") },
                    minLines = 2,
                    maxLines = 5,
                    isError = overLimit,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "${caption.length}/$MAX_CAPTION_LENGTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overLimit) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (!overLimit) onShare(caption.trim()) },
                enabled = !overLimit,
                colors = ButtonDefaults.buttonColors(),
            ) { Text("Pubblica") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = Color(0xFF8E8E93))
            }
        },
    )
}

private const val MAX_CAPTION_LENGTH = 200
