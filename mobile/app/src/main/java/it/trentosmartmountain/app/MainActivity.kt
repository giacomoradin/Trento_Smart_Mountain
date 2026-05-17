package it.trentosmartmountain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.trentosmartmountain.app.ui.navigation.TsmNavHost
import it.trentosmartmountain.app.ui.theme.TsmTheme

/**
 * Activity principale dichiarata nel manifest; ospita tutta la UI Jetpack Compose.
 *
 * Applica [it.trentosmartmountain.app.ui.theme.TsmTheme] e avvia [it.trentosmartmountain.app.ui.navigation.TsmNavHost]
 * (destinazione iniziale: auth o shell escursionista/rifugio se esiste un JWT valido).
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      TsmTheme {
        TsmNavHost()
      }
    }
  }
}
