package it.trentosmartmountain.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.ui.screens.session.TsmSentieriMapView
import org.osmdroid.util.GeoPoint

/**
 * Dialog a tutto schermo con la mappa OSMdroid **interattiva** del tracciato GPX:
 * pinch per zoom IN e OUT, pan libero. Essendo full-screen non c'è conflitto di
 * gesti col pager mappa↔altimetria o con lo scroll del feed.
 */
@Composable
fun TsmRouteMapDialog(
    routePoints: List<RoutePoint>,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                TsmSentieriMapView(
                    modifier = Modifier.fillMaxSize(),
                    markers = emptyList(),
                    polyline = routePoints.map { GeoPoint(it.lat, it.lon) },
                )
                Surface(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Chiudi mappa",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}
