package it.trentosmartmountain.app.ui.theme

import androidx.compose.ui.graphics.Color

object TsmColors {
    // ── Nuova Identità Ingegneristica Outdoor ─────────────────────────────
    val Primary = Color(0xFFFC5200)          // Athletic Orange (High-Vis)
    val PrimaryDark = Color(0xFFCC4200)      // State Press Container
    val Cyan = Color(0xFF30C5CA)             // Electric Data Blue
    val AlpinePineDark = Color(0xFF004225)   // British Racing Green profondo

    // ── Superfici e Layering Ingegneristico ───────────────────────────────
    val FeedBackground = Color(0xFF080B11)   // Sincronizzato con il Tech Background assoluto
    val Card = Color(0xFF101622)             // Struttura Slate Navy Card
    val CardElevated = Color(0xFF182030)     // Moduli input e chip
    
    val HeroTop = Color(0xFF003748)          // Inizio gradiente: Tech Navy
    val HeroBottom = Color(0xFF080B11)       // Chiusura gradiente convergente
    val Divider = Color(0xFF1A2332)          // Linea netta di separazione telemetria

    // Dashboard IoT Rifugio
    val DashboardBackground = Color(0xFF04060A)
    val DashboardCard = Color(0xFF0D121F)
    val DashboardBorder = Color(0xFF161F33)

    // ── Accenti di Stato per Sensori e Badge ──────────────────────────────
    val Success = Color(0xFF00E676)          // Sincronizzato con il vecchio Online
    val Danger = Color(0xFFE53A3A)           // Azioni affettive / like
    val Gold = Color(0xFFFFC729)             // Classifica ed educazione
    val Online = Color(0xFF00E676)
    val Offline = Color(0xFF9D0922)
    val Warning = Color(0xFFFF9100)
    val Info = Color(0xFF29B6F6)
    
    val Peach = Color(0xFFFFA726)
    val Wind = Color(0xFF66BB6A)

    // ── Ottimizzazione Tipografica (Anti-Fatigue Slate Spectrum) ──────────
    val TextPrimary = Color(0xFFF1F5F9)      // Slate 50
    val TextSecondary = Color(0xFF94A3B8)    // Slate 400
    val TextTertiary = Color(0xFF64748B)     // Slate 500
    val TextDim = Color(0xFF475569)          // Slate 600
}

// ── Alias Top-Level per Retrocompatibilità e Theme Engine ─────────────
val TsmPrimary = TsmColors.Primary
val TsmTechNavy = TsmColors.HeroTop
val TsmAccent = TsmColors.Cyan
val TsmSurface = TsmColors.Card
val TsmOnSurface = TsmColors.TextPrimary
val TsmSurfaceVariant = TsmColors.CardElevated
val TsmBackground = TsmColors.FeedBackground
val TsmBorder = TsmColors.Divider
val TsmSos = TsmColors.Offline

/**
 * Calcolo semantico della difficoltà escursionistica aggiornato.
 * Il livello "T" adotta il nuovo verde profondo istituzionale alpino, 
 * lasciando i livelli avanzati sulle tinte calde d'avviso.
 */
fun difficultyColor(level: String?): Color = when (level?.uppercase()) {
    "T"   -> Color(0xFF004225)   // Integrazione del Brand Alpine Pine (Turistico: Sicuro/Protetto)
    "E"   -> Color(0xFF29B6F6)   // Escursionistico standard (Azzurro info)
    "EE"  -> Color(0xFFFF9100)   // Esperti (Arancione warning)
    "EEA" -> Color(0xFFE53A3A)   // Esperti Attrezzatura (Rosso critico / SOS)
    else  -> Color(0xFF64748B)   // Non specificato (Slate 500)
}