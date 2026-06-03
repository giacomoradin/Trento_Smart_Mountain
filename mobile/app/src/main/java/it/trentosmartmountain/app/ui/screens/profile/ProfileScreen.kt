package it.trentosmartmountain.app.ui.screens.profile

import android.app.Application
import android.nfc.NfcAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.trentosmartmountain.app.data.preferences.PreferencesHolder
import it.trentosmartmountain.app.data.preferences.UnitsFormatter
import it.trentosmartmountain.app.data.remote.dto.WeeklyGoals
import it.trentosmartmountain.app.data.remote.dto.WeeklyStatsResponse
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.theme.TsmPrimary
import it.trentosmartmountain.app.ui.util.AvatarUtils
import it.trentosmartmountain.app.viewmodel.ProfileV2ViewModel
import it.trentosmartmountain.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DarkSurface = Color(0xFF1C1C1E)
private val CardBackground = Color(0xFF2C2C2E)
private val AccentCyan = Color(0xFF4DD0E1)
private val AccentGreen = Color(0xFF4CAF50)
private val TextSecondary = Color(0xFF8E8E93)
private val ChipBlue = Color(0xFF1A3A5C)
private val ChipGreen = Color(0xFF1A3D1A)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onLoggedOut: () -> Unit,
    onNavigateToFormazione: () -> Unit = {},
    onNavigateToNfcScan: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToChallenges: () -> Unit = {},
    onNavigateToBadges: () -> Unit = {},
    onNavigateToProfileView: () -> Unit = {},
    onNavigateToBoard: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
    profileV2ViewModel: ProfileV2ViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            (LocalContext.current as ComponentActivity).application,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileV2State by profileV2ViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val nfcAvailable = NfcAdapter.getDefaultAdapter(context) != null
    val coroutineScope = rememberCoroutineScope()
    var showRemoveAvatarDialog by remember { mutableStateOf(false) }

    // Toast su esito upload/rimozione avatar. Reagisce ai messaggi prodotti
    // dal ViewModel (sectionSuccess/sectionError) e poi li resetta per non
    // ri-mostrarsi a ogni ricomposizione.
    LaunchedEffect(profileV2State.sectionSuccess) {
        profileV2State.sectionSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            profileV2ViewModel.clearSectionMessages()
        }
    }
    LaunchedEffect(profileV2State.sectionError) {
        profileV2State.sectionError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            profileV2ViewModel.clearSectionMessages()
        }
    }

    // Photo picker: la conversione URI → data URI Base64 viene fatta in
    // Dispatchers.IO (read bytes, EXIF, BitmapFactory, downscale, JPEG, Base64)
    // per non bloccare il main thread su immagini grandi.
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val dataUri = withContext(Dispatchers.IO) {
                AvatarUtils.prepareAvatarForUpload(context.contentResolver, uri)
            }
            if (dataUri == null) {
                Toast.makeText(
                    context,
                    "Impossibile leggere la foto selezionata. Riprova.",
                    Toast.LENGTH_LONG,
                ).show()
            } else {
                profileV2ViewModel.uploadAvatar(dataUri)
            }
        }
    }

    if (showRemoveAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveAvatarDialog = false },
            title = { Text("Rimuovere la foto profilo?") },
            text = {
                Text(
                    "L'avatar tornerà a essere visualizzato con le iniziali del tuo username.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveAvatarDialog = false
                    profileV2ViewModel.removeAvatar()
                }) { Text("Rimuovi", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAvatarDialog = false }) { Text("Annulla") }
            },
            containerColor = CardBackground,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
      // Aurora di profondità dietro al profilo (materiale premium percepibile).
      TsmAuroraBackground(
          modifier = Modifier.fillMaxSize(),
          baseColor = DarkSurface,
          particleCount = 18,
      )
      Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        if (uiState.showBlockingLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Profilo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Row {
                    IconButton(onClick = onNavigateToProfileView) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Vedi profilo", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToAccount) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!profileV2State.isLoadingProfile && profileV2State.profileCompletedAt == null) {
                CompleteProfileBanner(onNavigateToOnboarding = onNavigateToOnboarding)
                Spacer(Modifier.height(12.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar interattivo: tap = scegli foto, long-press = dialog rimuovi.
                    // L'overlay loader (isLoading) appare quando il VM sta caricando o
                    // rimuovendo l'avatar, così l'utente vede chiaramente che qualcosa
                    // sta succedendo (era il vecchio "click e nessun feedback" che
                    // confondeva — bug #6 del report).
                    val hasAvatar = !profileV2State.personalInfo?.avatarUrl.isNullOrBlank()
                    Box(contentAlignment = Alignment.Center) {
                        // Glow brand dietro l'avatar (materiale premium).
                        it.trentosmartmountain.app.ui.components.TsmGlow(
                            color = TsmPrimary,
                            modifier = Modifier.size(96.dp),
                            alpha = 0.30f,
                        )
                        Box(modifier = Modifier.size(64.dp)) {
                            AvatarImage(
                                avatarUrl = profileV2State.personalInfo?.avatarUrl,
                                fallbackName = uiState.username,
                                size = 64.dp,
                                isLoading = profileV2State.isSavingSection,
                                backgroundColorOverride = Color(0xFF003748),
                                modifier = Modifier.combinedClickable(
                                    enabled = !profileV2State.isSavingSection,
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    onLongClick = {
                                        if (hasAvatar) showRemoveAvatarDialog = true
                                    },
                                ),
                            )
                            // Badge "fotocamera" in basso a destra: l'avatar è tappabile.
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(TsmPrimary, CircleShape)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Cambia foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = uiState.username ?: "—",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = uiState.email ?: "",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(text = "Trentino", background = ChipBlue, textColor = AccentCyan)
                            when (uiState.isVerified) {
                                true -> Chip(text = "Verificato", background = ChipGreen, textColor = AccentGreen)
                                false -> Chip(text = "Email da confermare", background = Color(0xFF3D2A1A), textColor = Color(0xFFE6B800))
                                null -> Unit
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val level = uiState.level
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    it.trentosmartmountain.app.ui.theme.TsmColors.CardElevated,
                                    it.trentosmartmountain.app.ui.theme.TsmColors.Card,
                                ),
                            ),
                        )
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "SOCIAL CREDITS",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                text = "%,d".format(uiState.socialCredits),
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                            )
                        }
                        if (level != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ChipBlue,
                            ) {
                                Text(
                                    text = "Lv. ${level.lv} · ${level.name}",
                                    color = AccentCyan,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    if (level != null && level.creditsToNext > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "+${level.creditsToNext} al prossimo livello",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { level?.progressPct ?: 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = AccentCyan,
                        trackColor = Color(0xFF3A3A3C),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        KpiCell(value = "${uiState.totalActivities}", label = "esc.")
                        KpiDivider()
                        KpiCell(value = "%.0f".format(uiState.totalDistanceKm), label = "km")
                        KpiDivider()
                        val elevLabel = if (uiState.totalElevationM >= 1000)
                            "${"%.0f".format(uiState.totalElevationM / 1000.0)}k" else "${uiState.totalElevationM}"
                        KpiCell(value = elevLabel, label = "m dis.")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            WeeklyGoalsCard(
                goals = profileV2State.weeklyGoals,
                stats = profileV2State.weeklyStats,
                onClick = onNavigateToGoals,
            )

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = nfcAvailable) { onNavigateToNfcScan() },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A3C)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1A2A3A),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Totem NFC",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Spacer(Modifier.width(8.dp))
                                if (nfcAvailable) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1A3D1A),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                                            Spacer(Modifier.width(4.dp))
                                            Text("NFC ATTIVO", color = AccentGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF3A1A1A)) {
                                        Text("NFC NON DISPONIBILE", color = Color(0xFFFF6B6B), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Text(
                                text = "Avvicina il telefono al totem per guadagnare crediti al checkpoint.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("TOTEM SCANSIONATI", color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp)
                            Text("${uiState.nfcScansCount}", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("CREDITI DA TOTEM", color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp)
                            Text("${uiState.nfcScansCredits} pt", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToFormazione() },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A3C)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFF1A2A3A)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🎓", fontSize = 22.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Formazione", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.width(8.dp))
                                    Surface(shape = RoundedCornerShape(12.dp), color = ChipBlue) {
                                        Text(
                                            "${uiState.passedQuizzes}/${uiState.totalQuizzes} quiz",
                                            color = AccentCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                Text(
                                    "${uiState.quizCreditsEarned} pt quiz · ${uiState.quizCreditsTotal} disponibili",
                                    color = AccentCyan,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (uiState.totalQuizzes > 0)
                                uiState.passedQuizzes.toFloat() / uiState.totalQuizzes
                            else 0f
                        },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = AccentCyan,
                        trackColor = Color(0xFF3A3A3C),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToBadges() },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎖️", fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Bacheca", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("I tuoi badge e certificati conquistati", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToChallenges() },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Sfide", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Crea o partecipa a sfide con altri escursionisti", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToBoard() },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📋", fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Bacheca rifugi", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Avvisi e segnalazioni dai rifugi", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToAccount() },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentCyan)
                        Spacer(Modifier.width(12.dp))
                        Text("Account e dati personali", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.logout(onLoggedOut) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A1A)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D1A1A)),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("ESCI", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
      }
    }
}

@Composable
private fun Chip(text: String, background: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun KpiCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun KpiDivider() {
    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFF3A3A3C)))
}

@Composable
private fun WeeklyGoalsCard(goals: WeeklyGoals?, stats: WeeklyStatsResponse?, onClick: () -> Unit) {
    val hasAnyGoal = goals != null && (goals.km > 0 || goals.elevM > 0 || goals.count > 0)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("OBIETTIVI SETTIMANALI", color = TextSecondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                    if (!hasAnyGoal) {
                        Text("Imposta i tuoi target →", color = AccentCyan, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
            if (hasAnyGoal) {
                Spacer(Modifier.height(10.dp))
                val prefs by PreferencesHolder.prefs.collectAsStateWithLifecycle()
                val units = prefs.units
                if (goals!!.km > 0) {
                    GoalRowFormatted("Distanza", UnitsFormatter.distance(stats?.km ?: 0.0, units, 1), UnitsFormatter.distance(goals.km.toDouble(), units, 1), if (goals.km > 0) ((stats?.km ?: 0.0) / goals.km).toFloat().coerceIn(0f, 1f) else 0f, (stats?.km ?: 0.0) >= goals.km)
                    Spacer(Modifier.height(8.dp))
                }
                if (goals.elevM > 0) {
                    GoalRowFormatted("Dislivello", UnitsFormatter.elevation(stats?.elevM ?: 0, units), UnitsFormatter.elevation(goals.elevM, units), if (goals.elevM > 0) ((stats?.elevM ?: 0).toFloat() / goals.elevM).coerceIn(0f, 1f) else 0f, (stats?.elevM ?: 0) >= goals.elevM)
                    Spacer(Modifier.height(8.dp))
                }
                if (goals.count > 0) {
                    GoalRowFormatted("Escursioni", "${stats?.count ?: 0}", "${goals.count}", if (goals.count > 0) ((stats?.count ?: 0).toFloat() / goals.count).coerceIn(0f, 1f) else 0f, (stats?.count ?: 0) >= goals.count)
                }
            }
        }
    }
}

@Composable
private fun GoalRowFormatted(label: String, currentText: String, targetText: String, progress: Float, reached: Boolean) {
    val accentColor = if (reached) AccentGreen else AccentCyan
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text("$currentText / $targetText", color = accentColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = accentColor, trackColor = Color(0xFF3A3A3C))
    }
}

@Composable
private fun CompleteProfileBanner(onNavigateToOnboarding: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNavigateToOnboarding() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A3A)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("👤", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Completa il tuo profilo", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Dati personali, esperienza, preferenze. Migliora le stime e i crediti.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentCyan)
        }
    }
}
