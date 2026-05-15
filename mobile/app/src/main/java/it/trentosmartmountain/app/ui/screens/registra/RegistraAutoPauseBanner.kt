package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.trentosmartmountain.app.R

private val AutoPauseBackground = Color(0xCCBF360C)

@Composable
fun RegistraAutoPauseBanner(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = AutoPauseBackground,
  ) {
    Text(
      text = stringResource(R.string.registra_auto_pause_hint),
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      style =
        MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
        ),
      color = Color.White,
      textAlign = TextAlign.Center,
    )
  }
}
