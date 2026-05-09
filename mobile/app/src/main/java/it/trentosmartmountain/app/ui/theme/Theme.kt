package it.trentosmartmountain.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
  lightColorScheme(
    primary = TsmPrimary,
  )

@Composable
fun TsmTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColors,
    content = content,
  )
}
