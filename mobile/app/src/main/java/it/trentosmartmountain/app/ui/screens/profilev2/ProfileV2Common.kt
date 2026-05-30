package it.trentosmartmountain.app.ui.screens.profilev2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

internal val DarkSurface = Color(0xFF1C1C1E)
internal val CardBackground = Color(0xFF2C2C2E)
internal val AccentCyan = Color(0xFF4DD0E1)
internal val AccentGreen = Color(0xFF4CAF50)
internal val TextSecondary = Color(0xFF8E8E93)
internal val FieldBorder = Color(0xFF3A3A3C)
internal val SelectedBg = Color(0xFF1A2A3A)

/**
 * Chip-style "segmented" selector: lista di opzioni con la corrente evidenziata.
 * Usato per sesso, livello CAI, fitness baseline, units, ecc.
 *
 * `labelFromValue` consente di mostrare label localizzate diverse dai value
 * inviati al server (es. value "M" → label "Maschio").
 *
 * `locked = true` rende il selettore read-only (anti-cheat per caiLevel).
 */
@Composable
fun <T> SegmentedChips(
    options: List<T>,
    selected: T?,
    labelFromValue: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
) {
    Row(modifier = modifier.fillMaxWidth().then(if (locked) Modifier.alpha(0.6f) else Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val isSelected = opt == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !locked) { onSelect(opt) },
                color = if (isSelected) SelectedBg else CardBackground,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isSelected) AccentCyan else FieldBorder),
            ) {
                Text(
                    text = labelFromValue(opt),
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    color = if (isSelected) AccentCyan else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

/** TextField numerico opzionale (Int): consente input vuoto → null. */
@Composable
fun NumberFieldInt(
    value: Int?,
    onChange: (Int?) -> Unit,
    label: String,
    suffix: String? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { raw ->
            val cleaned = raw.filter { it.isDigit() }.take(4)
            onChange(cleaned.toIntOrNull())
        },
        label = { Text(label, color = TextSecondary) },
        suffix = suffix?.let { { Text(it, color = TextSecondary) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = textFieldColors(),
        singleLine = true,
    )
}

/** TextField numerico opzionale (Double): consente input vuoto → null. */
@Composable
fun NumberFieldDouble(
    value: Double?,
    onChange: (Double?) -> Unit,
    label: String,
    suffix: String? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value?.let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() }.orEmpty(),
        onValueChange = { raw ->
            // Accetta "70" o "70.5" — sostituisce virgola con punto per locale IT.
            val cleaned = raw.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)
            onChange(cleaned.toDoubleOrNull())
        },
        label = { Text(label, color = TextSecondary) },
        suffix = suffix?.let { { Text(it, color = TextSecondary) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = textFieldColors(),
        singleLine = true,
    )
}

@Composable
internal fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = FieldBorder,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = AccentCyan,
)

/** Header descrittivo per ogni sezione del form. */
@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Column(modifier) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Riga "icona + label + chevron" cliccabile usata per le entry nelle schermate elenco. */
@Composable
fun NavRow(icon: ImageVector, label: String, sublabel: String? = null, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = CardBackground,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = AccentCyan)
            }
            Spacer(Modifier.width(12.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.White, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                if (!sublabel.isNullOrBlank()) {
                    Text(sublabel, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (trailing != null) trailing()
            else Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

// ── Mapping value ↔ label localizzato ────────────────────────────────────────────

/** Etichette user-facing per i valori server-side. */
object ProfileV2Labels {
    val sexValues = listOf("M", "F", "X", "N")
    fun sexLabel(value: String?): String = when (value) {
        "M" -> "Maschio"
        "F" -> "Femmina"
        "X" -> "Altro"
        "N" -> "Preferisco non dire"
        else -> "—"
    }

    val caiLevels = listOf("T", "E", "EE", "EEA")
    fun caiLabel(value: String?): String = when (value) {
        "T" -> "T — Turistico"
        "E" -> "E — Escursionistico"
        "EE" -> "EE — Esperti"
        "EEA" -> "EEA — Attrezzatura"
        else -> "—"
    }

    val baselineFitnessValues = listOf("sedentary", "active", "sport", "athlete")
    fun fitnessLabel(value: String?): String = when (value) {
        "sedentary" -> "Sedentario"
        "active" -> "Attivo"
        "sport" -> "Sportivo"
        "athlete" -> "Atleta"
        else -> "—"
    }

    val trainingFreqValues = listOf("0-1", "2-3", "4+")
    fun trainingFreqLabel(value: String?): String = when (value) {
        "0-1" -> "0–1 volta/sett."
        "2-3" -> "2–3 volte/sett."
        "4+" -> "4+ volte/sett."
        else -> "—"
    }

    val unitsValues = listOf("metric", "imperial")
    fun unitsLabel(value: String?): String = when (value) {
        "metric" -> "Metrico (km, m, kg)"
        "imperial" -> "Imperiale (mi, ft, lb)"
        else -> "—"
    }

    val privacyValues = listOf("public", "friends", "private")
    fun privacyLabel(value: String?): String = when (value) {
        "public" -> "Pubblico"
        "friends" -> "Solo amici"
        "private" -> "Privato"
        else -> "—"
    }
}
