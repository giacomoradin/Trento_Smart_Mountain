package it.trentosmartmountain.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.trentosmartmountain.app.ui.util.AvatarUtils
import kotlin.math.abs

/**
 * Avatar circolare riusabile in tutta l'app.
 *
 * Strategia di rendering:
 *  1. Se [avatarUrl] è un data URI valido → decodifica Base64 → mostra il bitmap
 *     come Image cropped circolare. Il bitmap è memoizzato con `remember(avatarUrl)`
 *     così non viene ridecodificato ad ogni ricomposizione (era un bug della
 *     versione inline in ProfileScreen.kt: re-decode per ogni cambio state).
 *  2. Se il dato URI è presente ma la decode fallisce (Base64 corrotto, formato
 *     non riconosciuto) → fallback alle iniziali invece di un box vuoto.
 *  3. Se [avatarUrl] è null/blank → mostra le iniziali derivate da [fallbackName]
 *     con un colore di sfondo deterministico (hash dello username), così lo
 *     stesso utente ha sempre lo stesso colore senza serverside.
 *  4. Se [isLoading] è true → mostra un overlay con [CircularProgressIndicator]
 *     sopra il contenuto corrente.
 *
 * Le iniziali sono prese dalle prime 2 lettere dei "word" del nome:
 *   "Giacomo"          → "GI"
 *   "Giacomo Radin"    → "GR"
 *   "Mario Mario Bros" → "MM"  (solo prime due parole)
 *
 * @param avatarUrl Data URI Base64 (es. "data:image/jpeg;base64,..."), oppure
 *   null/blank per usare le iniziali.
 * @param fallbackName Nome da cui ricavare iniziali e colore di sfondo
 *   (tipicamente l'username).
 * @param size Diametro del cerchio (default 64 dp).
 * @param isLoading Se true, mostra un overlay di caricamento.
 * @param backgroundColorOverride Forza il colore di sfondo del fallback
 *   (es. per la card profilo dove vogliamo `TsmPrimary` invariante). Se null
 *   usa il colore deterministico dal hash del nome.
 */
@Composable
fun AvatarImage(
    avatarUrl: String?,
    fallbackName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isLoading: Boolean = false,
    backgroundColorOverride: Color? = null,
) {
    // Decodifichiamo il bitmap UNA SOLA VOLTA per ogni valore distinto di avatarUrl.
    // Cambio dell'URL → ricalcolo; ricomposizione senza cambio URL → riuso del
    // bitmap già in memoria. Indispensabile per avatar in liste lunghe (sessioni
    // con molti partecipanti) dove altrimenti decodevamo Base64 ad ogni scroll.
    val bitmap = remember(avatarUrl) { AvatarUtils.decodeDataUri(avatarUrl) }
    val initials = remember(fallbackName) { initialsFrom(fallbackName) }
    val bgColor = backgroundColorOverride ?: remember(fallbackName) {
        deterministicAvatarColor(fallbackName)
    }
    val fontSizeSp = remember(size) { (size.value * 0.34f).sp }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto profilo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizeSp,
                ),
            )
        }

        if (isLoading) {
            // Overlay scuro + spinner: l'utente vede chiaramente che l'upload è in corso.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(size * 0.35f),
                )
            }
        }
    }
}

/**
 * Estrae fino a 2 lettere maiuscole dal nome. Pulisce gli spazi multipli e
 * tollera valori null/blank ritornando "?".
 */
internal fun initialsFrom(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.isEmpty()) return "?"
    val tokens = trimmed.split(Regex("\\s+"))
    val firsts = tokens.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
    val joined = firsts.joinToString(separator = "")
    return joined.ifBlank { "?" }
}

/**
 * Restituisce un colore deterministico in base al nome: stessa stringa → stesso
 * colore (utile perché lo stesso utente abbia sempre lo stesso avatar di
 * fallback senza che il server debba memorizzarlo).
 *
 * Palette: 8 tonalità "outdoor" che si abbinano con il dark theme dell'app.
 */
internal fun deterministicAvatarColor(seed: String?): Color {
    val palette = listOf(
        Color(0xFF2D5A2D), // bosco verde (default storico)
        Color(0xFF3F7020), // TsmPrimary verde
        Color(0xFF1A5A6A), // teal montano
        Color(0xFF4A3A6A), // viola crepuscolo
        Color(0xFF6A4A1A), // ocra terra
        Color(0xFF5A1A3A), // bordeaux roccia
        Color(0xFF1A3A5C), // blu cielo
        Color(0xFF6A3A1A), // marrone scuro
    )
    val key = seed?.trim()?.lowercase().orEmpty()
    if (key.isEmpty()) return palette[0]
    // hashCode di Kotlin è stabile cross-platform → stesso colore in qualunque
    // build dell'app (utile se in futuro lo decidiamo lato server).
    val idx = abs(key.hashCode()) % palette.size
    return palette[idx]
}
