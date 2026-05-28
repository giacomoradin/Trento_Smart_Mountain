package it.trentosmartmountain.app.data.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holder process-wide delle preferenze dell'utente. Singleton object — alimentato
 * da ProfileV2ViewModel quando carica il profilo, letto da:
 *   - TsmTheme (theme switch)
 *   - tutte le UI che mostrano distanze/elevazione
 *
 * Pattern alternativo sarebbe un `CompositionLocal` ma renderebbe più scomodo
 * leggere le preferenze dai ViewModel (non-Composable). Singleton object con
 * StateFlow è il compromesso più semplice per questo MVP.
 *
 * Persistence: gli aggiornamenti qui non vengono salvati su disco lato client —
 * il source of truth resta il backend. Al cold start, ProfileV2ViewModel ricarica
 * via API e ri-popola questo holder. Per offline-first si potrebbe aggiungere
 * una cache SharedPreferences ma è oltre lo scope MVP.
 */
object PreferencesHolder {

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    data class AppPreferences(
        val units: UnitsFormatter.Units = UnitsFormatter.Units.METRIC,
        val themeMode: ThemeMode = ThemeMode.DARK,
        val language: String = "it",
    )

    private val _prefs = MutableStateFlow(AppPreferences())
    val prefs: StateFlow<AppPreferences> = _prefs.asStateFlow()

    /**
     * Aggiorna le preferenze dal payload server. Chiamato da ProfileV2ViewModel
     * ogni volta che riceve il body di GET /users/:id o di PATCH /me/preferences.
     */
    fun update(units: String?, language: String?) {
        // Il theme mode non è ancora esposto via API — per ora resta DARK fisso
        // (coerente col design system esistente). Aggiungere a Preferences DTO
        // quando il design avrà un theme switcher concreto.
        _prefs.value = _prefs.value.copy(
            units = UnitsFormatter.parse(units),
            language = language?.takeIf { it.isNotBlank() } ?: _prefs.value.language,
        )
    }

    /** Reset al logout. */
    fun clear() {
        _prefs.value = AppPreferences()
    }
}
