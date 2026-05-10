package it.trentosmartmountain.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Schema colori Material 3 (per ora solo tema chiaro). */
private val LightColors =
  lightColorScheme(
    primary = TsmPrimary,
  )

/** Wrapper Material3 per tipografia e colori coerenti in tutta l’app. */
@Composable
fun TsmTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColors,
    content = content,
  )
}
