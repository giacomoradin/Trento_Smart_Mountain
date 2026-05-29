package it.trentosmartmountain.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.ui.theme.TsmColors

/**
 * Componenti di **stato** riusabili: loading / vuoto / errore.
 *
 * Perché: finora ogni schermata ridisegnava da sé lo spinner centrato, il box
 * "nessun dato" e (quando andava bene) un messaggio d'errore. Risultato: UI
 * incoerente e, in più punti, un errore di rete che appariva identico a "lista
 * vuota" — confondendo l'utente. Questi tre composable danno un linguaggio
 * visivo unico e distinguono chiaramente i tre casi.
 */

/** Spinner centrato a tutta area. Per il caricamento iniziale di una schermata. */
@Composable
fun TsmLoadingState(
    modifier: Modifier = Modifier,
    color: Color = TsmColors.Cyan,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = color)
    }
}

/**
 * Stato "vuoto" (nessun dato, ma nessun errore): icona/emoji + titolo +
 * messaggio + azione opzionale. Usare quando la lista è **legittimamente**
 * vuota (es. feed senza follow), NON per gli errori di rete.
 */
@Composable
fun TsmEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (emoji != null) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(16.dp))
        }
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = TsmColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * Stato di **errore** con possibilità di retry. Visivamente distinto dal vuoto
 * (icona "cloud off" + tinta d'accento) così l'utente capisce che è un problema
 * transitorio di connessione, non un'assenza di contenuti.
 */
@Composable
fun TsmErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = TsmColors.TextSecondary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Qualcosa è andato storto",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = TsmColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text("Riprova") }
    }
}
