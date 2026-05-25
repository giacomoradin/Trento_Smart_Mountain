package it.trentosmartmountain.app.ui.util

import androidx.compose.runtime.saveable.Saver
import com.google.gson.Gson

/**
 * Saver basato su Gson per DTO non-Parcelable. Salva l'oggetto come stringa JSON
 * nel Bundle del rememberSaveable; al restore lo deserializza con Gson.
 *
 * Usato per i pendingNfcResult / pendingQuizResult tenuti dal NavHost: senza Saver
 * sopravvivono solo a recomposition/config change ma vengono persi se il sistema
 * uccide il processo in background — l'utente al rientro vede schermata vuota.
 *
 * **Limite**: i campi `transient` di Gson non vengono salvati. Tutti i DTO usati
 * con questo Saver sono semplici data class con campi serializzabili, quindi OK.
 */
inline fun <reified T : Any> gsonSaver(): Saver<T?, String> {
    val gson = Gson()
    return Saver(
        save = { value -> if (value == null) "" else gson.toJson(value) },
        restore = { json -> if (json.isEmpty()) null else gson.fromJson(json, T::class.java) },
    )
}
