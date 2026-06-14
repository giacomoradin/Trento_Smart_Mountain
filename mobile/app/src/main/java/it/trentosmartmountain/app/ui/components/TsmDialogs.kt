package it.trentosmartmountain.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import it.trentosmartmountain.app.ui.theme.TsmColors

/**
 * Dialog di conferma "glass" coerente col design system (sostituisce gli
 * `AlertDialog` Material default). Card glass + icon-chip opzionale + CTA a
 * gradiente. [destructive] tinge la conferma di rosso per le azioni irreversibili.
 */
@Composable
fun TsmAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Annulla",
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    val accent = if (destructive) Color(0xFFFF6B6B) else TsmColors.Cyan
    Dialog(onDismissRequest = onDismiss) {
        TsmGlassCard(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            border = accent.copy(alpha = 0.35f),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (icon != null) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.size(14.dp))
                }
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(8.dp))
                Text(text, color = TsmColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.size(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissLabel, color = TsmColors.TextSecondary, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(8.dp))
                    TsmGradientButton(
                        text = confirmLabel,
                        onClick = onConfirm,
                        height = 44.dp,
                        fill = if (destructive) {
                            Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFB71C1C)))
                        } else {
                            Brush.horizontalGradient(listOf(TsmColors.Primary, TsmColors.PrimaryDark))
                        },
                    )
                }
            }
        }
    }
}

/**
 * Snackbar "glass" brandizzata da passare allo slot `snackbar` di un
 * `SnackbarHost`: card glass che "galleggia" + azione opzionale in accent.
 */
@Composable
fun TsmSnackbar(data: SnackbarData) {
    TsmGlassCard(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        cornerRadius = 14.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                data.visuals.message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            data.visuals.actionLabel?.let { label ->
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { data.performAction() }) {
                    Text(label, color = TsmColors.Cyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
