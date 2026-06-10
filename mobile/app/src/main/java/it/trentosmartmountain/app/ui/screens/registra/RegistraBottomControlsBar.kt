package it.trentosmartmountain.app.ui.screens.registra

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.theme.TsmColors
import it.trentosmartmountain.app.ui.theme.TsmPrimary

/**
 * Barra inferiore: partecipanti (sinistra), controlli centrali (REC / play-stop),
 * SOS (destra) — allineati sulla stessa riga.
 *
 * Pass di coerenza (B9): partecipanti e SOS ora usano lo stesso GlassFab di
 * play/pause/stop — stesso bordo, ombra e gradiente — così l'intera riga
 * comandi della tab Registra ha il medesimo linguaggio visivo.
 */
@Composable
fun RegistraBottomControlsBar(
    showParticipants: Boolean,
    onParticipantsClick: () -> Unit,
    showSos: Boolean,
    onSosClick: () -> Unit,
    centerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RegistraLayout.bottomBarHorizontalPadding),
    ) {
        if (showParticipants) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                GlassFab(
                    onClick = onParticipantsClick,
                    icon = Icons.Filled.Groups,
                    contentDescription = stringResource(R.string.group_roster_cd),
                    size = RegistraLayout.primaryFabSize,
                    iconSize = RegistraLayout.primaryFabIconSize,
                    containerColor = TsmColors.AlpinePineDark,
                    iconTint = Color.White,
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.Center)) {
            centerContent()
        }
        if (showSos) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                GlassFab(
                    onClick = onSosClick,
                    icon = Icons.Filled.Warning,
                    contentDescription = stringResource(R.string.registra_sos_cd),
                    size = RegistraLayout.primaryFabSize,
                    iconSize = RegistraLayout.primaryFabIconSize,
                    containerColor = TsmPrimary,
                    iconTint = Color.White,
                    borderTint = TsmPrimary.copy(alpha = 0.55f),
                )
            }
        }
    }
}
