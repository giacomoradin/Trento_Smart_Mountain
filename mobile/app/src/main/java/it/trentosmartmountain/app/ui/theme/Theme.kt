package it.trentosmartmountain.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary            = TsmPrimary,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFF1A3A15),
    onPrimaryContainer = Color(0xFFB8F0A8),
    secondary          = TsmAccent,
    onSecondary        = Color(0xFF001F2A),
    surface            = TsmSurface,
    onSurface          = TsmOnSurface,
    surfaceVariant     = TsmSurfaceVariant,
    onSurfaceVariant   = Color(0xFFAAAAAA),
    background         = TsmBackground,
    onBackground       = Color.White,
    outline            = Color(0xFF3A3A3A),
    error              = Color(0xFFCF6679),
    onError            = Color.White,
)

@Composable
fun TsmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
