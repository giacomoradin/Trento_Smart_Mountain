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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import it.trentosmartmountain.app.ui.components.AvatarImage
import it.trentosmartmountain.app.ui.components.TsmAnimatedCounter
import it.trentosmartmountain.app.ui.components.TsmAuroraBackground
import it.trentosmartmountain.app.ui.components.TsmGlassCard
import it.trentosmartmountain.app.ui.components.TsmHeroActionChip
import it.trentosmartmountain.app.ui.components.TsmHeroHeader
import it.trentosmartmountain.app.ui.components.tsmSweepBorder
import it.trentosmartmountain.app.ui.theme.TsmColors
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val nfcAvailable = nfcAdapter != null
    
    // Stato reattivo dell'NFC (aggiornato tramite BroadcastReceiver e OnResume)
    var nfcEnabled by remember { mutableStateOf(nfcAvailable && nfcAdapter?.isEnabled == true) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(context, nfcAvailable, lifecycleOwner) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                    nfcEnabled = nfcAdapter?.isEnabled == true
                }
            }
        }
        if (nfcAvailable) {
            context.registerReceiver(receiver, android.content.IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED))
        }

        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                nfcEnabled = nfcAvailable && nfcAdapter?.isEnabled == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            if (nfcAvailable) {
                runCatching { context.unregisterReceiver(receiver) }
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
        it.trentosmartmountain.app.ui.components.TsmAlertDialog(
            onDismiss = { showRemoveAvatarDialog = false },
            title = "Rimuovere la foto profilo?",
            text = "L'avatar tornerà a essere visualizzato con le iniziali del tuo username.",
            confirmLabel = "Rimuovi",
            destructive = true,
            onConfirm = {
                showRemoveAvatarDialog = false
                profileV2ViewModel.removeAvatar()
            },
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

        PullToRefreshBox(
            isRefreshing = profileV2State.isRefreshing || uiState.showInlineRefresh,
            onRefresh = {
                viewModel.loadProfile()
                profileV2ViewModel.loadProfile(manualRefresh = true)
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
            TsmHeroHeader(
                overline = "TRENTO SMART MOUNTAIN",
                title = "Profilo",
                actions = {
                    TsmHeroActionChip(
                        icon = Icons.Default.AccountCircle,
                        contentDescription = "Vedi profilo",
                        onClick = onNavigateToProfileView,
                    )
                    Spacer(Modifier.width(10.dp))
                    TsmHeroActionChip(
                        icon = Icons.Default.Settings,
                        contentDescription = "Impostazioni",
                        onClick = onNavigateToAccount,
                    )
                },
            )

            Spacer(Modifier.height(12.dp))

            if (!profileV2State.isLoadingProfile && profileV2State.profileCompletedAt == null) {
                CompleteProfileBanner(onNavigateToOnboarding = onNavigateToOnboarding)
                Spacer(Modifier.height(12.dp))
            }

            TsmGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
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
                // Hero del profilo: bordo "luce viaggiante" per l'effetto extreme premium.
                modifier = Modifier.fillMaxWidth().tsmSweepBorder(cornerRadius = 14.dp),
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
                            // Count-up animato all'apertura del profilo (dashboard sportiva).
                            TsmAnimatedCounter(
                                target = uiState.socialCredits.toFloat(),
                                format = { "%,d".format(it.toInt()) },
                                color = AccentCyan,
                                style = androidx.compose.ui.text.TextStyle(fontSize = 36.sp),
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
                        KpiCell(target = uiState.totalActivities.toFloat(), format = { "%.0f".format(it) }, label = "esc.")
                        KpiDivider()
                        KpiCell(target = uiState.totalDistanceKm.toFloat(), format = { "%.0f".format(it) }, label = "km")
                        KpiDivider()
                        KpiCell(
                            target = uiState.totalElevationM.toFloat(),
                            format = { v -> if (v >= 1000f) "${"%.0f".format(v / 1000.0)}k" else "%.0f".format(v) },
                            label = "m dis.",
                        )
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

            TsmGlassCard(
                onClick = { if (nfcAvailable) onNavigateToNfcScan() },
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconChip(Icons.Outlined.Nfc, TsmColors.Info)
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
                                when {
                                    nfcEnabled -> {
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
                                    }
                                    nfcAvailable -> {
                                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF3D2E1A)) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFFB454)))
                                                Spacer(Modifier.width(4.dp))
                                                Text("NFC DISATTIVATO", color = Color(0xFFFFB454), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    else -> {
                                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF3A1A1A)) {
                                            Text("NFC NON DISPONIBILE", color = Color(0xFFFF6B6B), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
                                        }
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

            TsmGlassCard(
                onClick = onNavigateToFormazione,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconChip(Icons.Outlined.School, TsmColors.Cyan)
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

            ProfileLinkCard(
                icon = Icons.Filled.WorkspacePremium,
                iconTint = Color(0xFFFFC107),
                title = "Bacheca",
                subtitle = "I tuoi badge e certificati conquistati",
                onClick = onNavigateToBadges,
            )

            Spacer(Modifier.height(12.dp))

            ProfileLinkCard(
                icon = Icons.Filled.EmojiEvents,
                iconTint = TsmColors.Primary,
                title = "Sfide",
                subtitle = "Crea o partecipa a sfide con altri escursionisti",
                onClick = onNavigateToChallenges,
            )

            Spacer(Modifier.height(12.dp))

            ProfileLinkCard(
                icon = Icons.Outlined.Campaign,
                iconTint = TsmColors.Success,
                title = "Bacheca rifugi",
                subtitle = "Avvisi e segnalazioni dai rifugi",
                onClick = onNavigateToBoard,
            )

            Spacer(Modifier.height(12.dp))

            ProfileLinkCard(
                icon = Icons.Outlined.ManageAccounts,
                iconTint = TsmColors.Cyan,
                title = "Account e dati personali",
                subtitle = "Email, password, privacy e cancellazione",
                onClick = onNavigateToAccount,
            )

            Spacer(Modifier.height(12.dp))

            // Logout: glass card a tinta "danger" (coerente col resto, ma chiara).
            TsmGlassCard(
                onClick = { viewModel.logout(onLoggedOut) },
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                topColor = Color(0xFF2A1620),
                bottomColor = Color(0xFF1E1014),
                border = Color(0xFFFF6B6B).copy(alpha = 0.30f),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ESCI", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(24.dp))
            }
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

/**
 * Chip-icona quadrato arrotondato (40dp) con tinta accent: dà alle voci del
 * profilo un linguaggio visivo coerente e "premium" al posto dell'emoji nuda.
 */
@Composable
private fun IconChip(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/**
 * Riga-link del profilo su materiale glass (gradiente + hairline + press-scale via
 * [TsmGlassCard]): icon-chip accent + titolo + sottotitolo + chevron. Sostituisce
 * le vecchie card flat grigie con emoji.
 */
@Composable
private fun ProfileLinkCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    TsmGlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(icon, iconTint)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = TsmColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TsmColors.TextTertiary)
        }
    }
}

@Composable
private fun KpiCell(target: Float, format: (Float) -> String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TsmAnimatedCounter(
            target = target,
            format = format,
            color = Color.White,
            style = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
        )
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
    TsmGlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
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
    TsmGlassCard(
        onClick = onNavigateToOnboarding,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        border = AccentCyan.copy(alpha = 0.5f),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconChip(Icons.Default.Person, AccentCyan)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Completa il tuo profilo", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Dati personali, esperienza, preferenze. Migliora le stime e i crediti.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentCyan)
        }
    }
}
