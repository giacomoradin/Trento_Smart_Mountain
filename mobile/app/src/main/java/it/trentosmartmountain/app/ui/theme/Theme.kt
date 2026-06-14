package it.trentosmartmountain.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary            = TsmPrimary,
    onPrimary          = Color.White,
    primaryContainer   = TsmTechNavy,
    onPrimaryContainer = TsmAccent,
    secondary          = TsmAccent,
    onSecondary        = Color(0xFF001F2A),
    surface            = TsmSurface,
    onSurface          = TsmOnSurface,
    surfaceVariant     = TsmSurfaceVariant,
    onSurfaceVariant   = Color(0xFF94A3B8), // Slate 400 per metadati ad alta leggibilità
    background         = TsmBackground,
    onBackground       = Color.White,
    outline            = TsmBorder,
    error              = TsmSos,
    onError            = Color.White,
)

/** Theme entry-point per l'architettura mobile TSM. */
@Composable
fun TsmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        // Identità tipografica TSM: titoli athletic (tracking stretto) +
        // label telemetria (tracking largo). Vedi Type.kt.
        typography = TsmTypography,
        content = content,
    )
}