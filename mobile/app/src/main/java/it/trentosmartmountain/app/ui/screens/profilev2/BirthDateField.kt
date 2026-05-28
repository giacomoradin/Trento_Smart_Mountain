package it.trentosmartmountain.app.ui.screens.profilev2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Campo data di nascita con Material3 DatePicker.
 *
 * Input/output formato canonico ISO yyyy-MM-dd (compatibile col Joi backend
 * personalInfoSchema). Display formato locale italiano: "21 marzo 1995".
 *
 * Vincoli:
 *  - max selectable = oggi (no date future)
 *  - min selectable = 1900-01-01 (Joi backend ha lo stesso limite)
 *
 * Il TextField è readOnly + abbiamo aggiunto un clickable sull'intero modifier
 * → tappando ovunque nella riga si apre il picker. L'icona calendario è solo
 * un visual hint, non l'unica zona cliccabile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDateField(
    isoValue: String?,
    onIsoChange: (String?) -> Unit,
    label: String = "Data di nascita",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val initialMillis = remember(isoValue) {
        if (isoValue.isNullOrBlank()) null else parseIsoToMillis(isoValue)
    }
    val displayText = remember(isoValue) {
        if (isoValue.isNullOrBlank()) "" else formatForDisplay(isoValue)
    }

    OutlinedTextField(
        value = displayText,
        onValueChange = {}, // read-only: il valore cambia solo via DatePicker
        readOnly = true,
        label = { Text(label, color = TextSecondary) },
        trailingIcon = {
            IconButton(onClick = { if (enabled) showPicker = true }, enabled = enabled) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Apri date picker", tint = if (enabled) AccentCyan else TextSecondary)
            }
        },
        // Intero campo cliccabile → apre il picker. Niente ripple per non confondere
        // con un input editabile. Disabilitato se locked (anti-cheat).
        modifier = modifier
            .fillMaxWidth()
            .then(if (!enabled) Modifier.alpha(0.6f) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { showPicker = true },
            ),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCyan,
            unfocusedBorderColor = FieldBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = AccentCyan,
        ),
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Range [1900-01-01, oggi]
                    val now = System.currentTimeMillis()
                    val min = MIN_BIRTHDATE_MILLIS
                    return utcTimeMillis in min..now
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis -> onIsoChange(formatToIso(millis)) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Annulla") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ── helpers ───────────────────────────────────────────────────────────────

// 1900-01-01 UTC in millis. Calcolato hardcoded per evitare di costruire un
// Calendar a ogni recomposition.
private const val MIN_BIRTHDATE_MILLIS = -2208988800000L

private val isoFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    // ISO date semplice senza fuso → fissiamo UTC per coerenza col backend.
    timeZone = TimeZone.getTimeZone("UTC")
}
private val displayFormatter = SimpleDateFormat("d MMMM yyyy", Locale.ITALIAN).apply {
    // Display: fuso locale dell'utente → giorno mostrato non shifta per UTC.
    timeZone = TimeZone.getDefault()
}

private fun parseIsoToMillis(iso: String): Long? = runCatching {
    isoFormatter.parse(iso)?.time
}.getOrNull()

private fun formatToIso(millis: Long): String = isoFormatter.format(Date(millis))

private fun formatForDisplay(iso: String): String = runCatching {
    val date = isoFormatter.parse(iso) ?: return@runCatching iso
    displayFormatter.format(date)
}.getOrDefault(iso)
