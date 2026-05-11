package it.trentosmartmountain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.trentosmartmountain.app.ui.navigation.TsmNavHost
import it.trentosmartmountain.app.ui.theme.TsmTheme

/**
 * Activity principale dichiarata nel manifest; ospita tutta la UI Jetpack Compose.
 * Il grafo di navigazione parte da [TsmNavHost] (schermata iniziale: scelta accesso o registrazione).
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
