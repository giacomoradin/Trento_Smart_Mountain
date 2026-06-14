package it.trentosmartmountain.app.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.tsmSweepBorder
import it.trentosmartmountain.app.ui.theme.TsmAccent
import it.trentosmartmountain.app.ui.theme.TsmBackground
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.theme.TsmSurface

/**
 * Prima schermata per utenti non autenticati: scelta tra login e registrazione.
 *
 * @param onRegisterUserClick naviga al form registrazione escursionista
 * @param onRegisterRifugioClick naviga al form registrazione rifugio
 * @param onLoginClick naviga al form di accesso
 */
@Composable
fun AuthEntryScreen(
    onRegisterUserClick: () -> Unit,
    onRegisterRifugioClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Materiale premium: aurora + particelle dietro al contenuto (effetto brand).
        TsmAuroraBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo TSM UFFICIALE (immagine reale dell'utente) sulla schermata di
            // benvenuto. Nel resto dell'app si usa il vettoriale `TsmMountainLogo`.
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.tsm_logo_photo),
                contentDescription = "Logo TSM",
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "TSM",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.auth_entry_tagline),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.auth_entry_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(56.dp))

            // CTA principale col gradiente premium del design system + glow
            // pulsante dietro: punto focale immediato della welcome.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                it.trentosmartmountain.app.ui.components.TsmPulseGlow(
                    color = TsmPrimary,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                )
                it.trentosmartmountain.app.ui.components.TsmGradientButton(
                    text = stringResource(R.string.auth_entry_login_button),
                    onClick = onLoginClick,
                    // Bordo "luce viaggiante" sulla CTA di ingresso: primo impatto premium.
                    modifier = Modifier.fillMaxWidth().tsmSweepBorder(cornerRadius = 16.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRegisterUserClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White),
            ) {
                Text(
                    text = stringResource(R.string.auth_entry_register_user_button),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRegisterRifugioClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            ) {
                Text(
                    text = stringResource(R.string.auth_entry_register_rifugio_button),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        Text(
            text = stringResource(R.string.auth_version),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Logo TSM (vector drawable brandizzato) riusato in auth e login. */
@Composable
fun TsmMountainLogo(iconSize: Dp = 48.dp) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = R.drawable.tsm_logo),
        contentDescription = "Logo TSM",
        modifier = Modifier.size(iconSize),
    )
}
