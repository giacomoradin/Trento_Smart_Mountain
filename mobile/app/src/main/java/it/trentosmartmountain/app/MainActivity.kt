package it.trentosmartmountain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.trentosmartmountain.app.ui.navigation.TsmNavHost
import it.trentosmartmountain.app.ui.theme.TsmTheme

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
