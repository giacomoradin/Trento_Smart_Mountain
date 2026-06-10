package it.trentosmartmountain.app.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSurface
import it.trentosmartmountain.app.viewmodel.SessionPlanViewModel
import org.osmdroid.util.GeoPoint

/**
 * Popup di **anteprima del tracciato GPX** su mappa, senza navigare a "Registra".
 * Mostra la polyline del file importato + scheda metriche (km, dislivello, punti, start/end).
 */
@Composable
fun GpxPreviewDialog(
    gpx: SessionPlanViewModel.GpxParseResult,
    onDismiss: () -> Unit,
) {
    val polyline = remember(gpx) { gpx.trackLatLon.map { GeoPoint(it.first, it.second) } }
    val markers = remember(gpx) {
        buildList {
            gpx.firstPoint?.let {
                add(SentieroMapMarker("start", GeoPoint(it.first, it.second), "Partenza", SentieroMarkerType.START))
            }
            gpx.lastPoint?.let {
                add(SentieroMapMarker("end", GeoPoint(it.first, it.second), "Arrivo", SentieroMarkerType.SELECTED_DESTINATION))
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = TsmSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.94f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Anteprima tracciato",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Text(gpx.fileName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                Surface(shape = RoundedCornerShape(12.dp), color = TsmBackground, modifier = Modifier.fillMaxWidth()) {
                    TsmSentieriMapView(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        markers = markers,
                        polyline = polyline,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoChip("Distanza", "%.1f km".format(gpx.distanceKm))
                    InfoChip("Dislivello", "+%d m".format(gpx.elevationGainM))
                    InfoChip("Punti", gpx.trackPoints.toString())
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TsmPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Chiudi", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
internal fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TsmAccent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}
