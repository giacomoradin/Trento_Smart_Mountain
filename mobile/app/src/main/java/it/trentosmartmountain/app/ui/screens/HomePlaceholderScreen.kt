package it.trentosmartmountain.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Home temporanea dopo login demo; le feature MVP useranno questa destinazione come hub. */
@Composable
fun HomePlaceholderScreen() {
  Column(Modifier.fillMaxSize().padding(24.dp)) {
    Text("Home — placeholder")
  }
}
