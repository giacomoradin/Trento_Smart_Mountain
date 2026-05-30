package it.trentosmartmountain.app.ui.screens.nfc

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import it.trentosmartmountain.app.data.remote.dto.NfcScanResponse
import it.trentosmartmountain.app.viewmodel.NfcScanUiState
import it.trentosmartmountain.app.viewmodel.NfcScanViewModel

private val DarkSurface = Color(0xFF1C1C1E)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFE91E63)
private val CardBackground = Color(0xFF2C2C2E)
private val TextSecondary = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcScanScreen(
    onBack: () -> Unit,
    onResult: (NfcScanResponse) -> Unit,
    viewModel: NfcScanViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    var location by remember { mutableStateOf<Location?>(null) }

    // Stato hardware NFC: distinguiamo 3 condizioni che vanno trattate diversamente.
    //  - Hardware:   NfcAdapter.getDefaultAdapter() != null → device ha il chip NFC
    //  - Enabled:    adapter.isEnabled → utente ha attivato NFC nelle impostazioni
    // Lo stato `nfcEnabled` viene rivalutato a ogni ON_RESUME perché l'utente
    // può uscire dall'app, attivare NFC nelle Impostazioni, tornare indietro →
    // dobbiamo accorgercene senza richiedere un restart manuale.
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val hasNfcHardware = nfcAdapter != null
    var nfcEnabled by remember { mutableStateOf(nfcAdapter?.isEnabled == true) }

    DisposableEffect(lifecycleOwner, nfcAdapter) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                nfcEnabled = nfcAdapter?.isEnabled == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedClient.lastLocation.addOnSuccessListener { loc -> location = loc }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                location = loc
                viewModel.currentLocation = loc
            }
        } else {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // NFC foreground dispatch — attivo SOLO se l'adapter c'è ed è enabled.
    // Se NFC è disabilitato (utente non l'ha attivato), saltiamo l'enable: tanto
    // non riceveremmo mai il tag, e Android lancerebbe SecurityException.
    // Dipendiamo da `nfcEnabled` così che riattivando NFC dalle Impostazioni e
    // tornando in-app, il DisposableEffect si ri-esegua e abiliti il dispatch.
    DisposableEffect(activity, nfcEnabled) {
        if (!nfcEnabled || nfcAdapter == null || activity == null) {
            return@DisposableEffect onDispose {}
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, activity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE,
        )
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))

        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, null)
        onDispose { nfcAdapter.disableForegroundDispatch(activity) }
    }

    // Handle NFC intent from activity (activity must call viewModel.onTagScanned in onNewIntent)
    LaunchedEffect(state) {
        if (state is NfcScanUiState.Success) {
            onResult((state as NfcScanUiState.Success).response)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scansiona Totem NFC", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
            )
        },
        containerColor = DarkSurface,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                // Priorità ai problemi hardware: se il device non ha NFC,
                // mostriamo un blocco definitivo (no retry, no settings).
                !hasNfcHardware -> NfcUnsupportedView(onBack = onBack)
                // Hardware presente ma disabilitato → CTA verso Impostazioni NFC.
                !nfcEnabled -> NfcDisabledView(
                    onOpenSettings = {
                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    },
                )
                else -> when (state) {
                    is NfcScanUiState.Waiting -> WaitingAnimation()
                    is NfcScanUiState.Scanning -> {
                        CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(80.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Verifica in corso…", color = TextSecondary)
                    }
                    is NfcScanUiState.Success -> {
                        // Handled by LaunchedEffect above — show brief success feedback
                        Icon(Icons.Default.Nfc, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(80.dp))
                        Text("Scansione completata!", color = AccentGreen, fontWeight = FontWeight.Bold)
                    }
                    is NfcScanUiState.Error -> {
                        val err = (state as NfcScanUiState.Error).message
                        Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Errore", color = AccentRed, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(err, color = TextSecondary, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.reset() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                    shape = RoundedCornerShape(8.dp),
                                ) { Text("Riprova", color = Color.White, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mostrata quando il device NON ha hardware NFC (es. tablet economici, alcuni emulatori).
 * Niente CTA: blocco definitivo che invita solo a tornare indietro.
 */
@Composable
private fun NfcUnsupportedView(onBack: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = AccentRed,
                modifier = Modifier.size(72.dp),
            )
            Text(
                "NFC non supportato",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Text(
                "Il tuo dispositivo non ha un chip NFC, quindi non puoi scansionare i totem dei sentieri.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Torna indietro", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * Mostrata quando il device ha NFC ma è disabilitato dalle Impostazioni di sistema.
 * Apre direttamente la schermata Impostazioni NFC: tornando in-app, l'osservatore
 * di lifecycle ricontrolla `isEnabled` e mostra la schermata di scansione.
 */
@Composable
private fun NfcDisabledView(onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Nfc,
                contentDescription = null,
                tint = AccentRed,
                modifier = Modifier.size(72.dp),
            )
            Text(
                "NFC disattivato",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Text(
                "Per scansionare i totem devi attivare l'NFC dalle impostazioni del telefono.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Apri impostazioni NFC", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun WaitingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .border(3.dp, AccentCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Nfc, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(72.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Avvicina al Totem NFC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("Posiziona il telefono vicino al tag NFC del checkpoint per guadagnare crediti.", color = TextSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
    }
}
// Dead-code rimosso: la conversione tag → hex string ora vive in
// it.trentosmartmountain.app.data.nfc.NfcUtils.extractTagId/bytesToTagId.
