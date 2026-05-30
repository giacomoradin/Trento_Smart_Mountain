package it.trentosmartmountain.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.trentosmartmountain.app.data.local.TokenStorage
import it.trentosmartmountain.app.data.nfc.NfcTagBus
import it.trentosmartmountain.app.data.nfc.NfcUtils
import it.trentosmartmountain.app.ui.navigation.TsmNavHost
import it.trentosmartmountain.app.ui.theme.TsmTheme

/**
 * Activity principale dichiarata nel manifest; ospita tutta la UI Jetpack Compose.
 *
 * Gestisce anche i **deep link** in arrivo dall'email di verifica account:
 *  - `tsm://auth/success?jwt=<token>`: salva il JWT e fa auto-login → shell utente
 *  - `tsm://auth/error?message=<motivo>`: mostra un toast e porta alla schermata auth
 *
 * Applica [it.trentosmartmountain.app.ui.theme.TsmTheme] e avvia
 * [it.trentosmartmountain.app.ui.navigation.TsmNavHost] (destinazione iniziale: auth o shell
 * escursionista/rifugio in base alla presenza di un JWT valido).
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Gestione deep link al cold start: l'app viene aperta dal link nell'email.
    // Salviamo il JWT in TokenStorage PRIMA di comporre il NavHost così
    // AuthSession.startDestinationFor() trova già il token e parte dalla shell.
    handleAuthDeepLink(intent)
    // Se l'app è stata aperta direttamente da un intent NFC (cold start col telefono
    // appoggiato al totem), il tagId viene messo nel bus con replay=1 — appena il
    // NfcScanViewModel si iscrive lo riceve.
    handleNfcIntent(intent)

    setContent {
      TsmTheme {
        TsmNavHost()
      }
    }
  }

  /**
   * Gestione deep link quando l'app è già aperta in background.
   * Forziamo `recreate()` per riavviare la navigation dalla shell appena loggati.
   */
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (handleAuthDeepLink(intent)) {
      recreate()
      return
    }
    handleNfcIntent(intent)
  }

  private fun handleNfcIntent(intent: Intent) {
    // Conversione tag → hex string centralizzata in NfcUtils per evitare
    // divergenze di formato (lowercase/uppercase, separatori) tra MainActivity e altri call site.
    val tagId = NfcUtils.extractTagIdFromIntent(intent) ?: return
    Log.d(TAG, "NFC tag scansionato: $tagId")
    NfcTagBus.emit(tagId)
  }

  /**
   * Se l'intent contiene un deep link `tsm://auth/...`, esegue l'azione corrispondente.
   *
   * @return `true` se è stato gestito un deep link di auto-login (success), `false` altrimenti.
   */
  private fun handleAuthDeepLink(intent: Intent?): Boolean {
    val data: Uri = intent?.data ?: return false

    // Filtro: ci interessa solo lo scheme custom `tsm://auth/...`
    if (data.scheme != "tsm" || data.host != "auth") return false

    Log.d(TAG, "Deep link ricevuto: $data")

    return when (data.lastPathSegment) {
      "success" -> handleAuthSuccess(data)
      "error" -> {
        val msg = data.getQueryParameter("message") ?: "errore_sconosciuto"
        Log.w(TAG, "Verifica email fallita: $msg")
        Toast.makeText(this, "Link scaduto o non valido. Riprova la registrazione.", Toast.LENGTH_LONG).show()
        false
      }
      else -> {
        Log.w(TAG, "Deep link auth non riconosciuto: ${data.lastPathSegment}")
        false
      }
    }
  }

  /**
   * Estrae il JWT dalla query string `?jwt=...` e lo salva in [TokenStorage].
   * Mostra un toast di conferma.
   */
  private fun handleAuthSuccess(data: Uri): Boolean {
    val jwt = data.getQueryParameter("jwt")
    if (jwt.isNullOrBlank()) {
      Log.w(TAG, "Deep link success senza JWT: $data")
      Toast.makeText(this, "Verifica non completata: token mancante.", Toast.LENGTH_LONG).show()
      return false
    }

    val tokenStorage = TokenStorage.getInstance(this)
    tokenStorage.saveToken(jwt)
    Log.i(TAG, "Auto-login completato dal deep link email ✓")
    Toast.makeText(this, "Account verificato! Accesso effettuato.", Toast.LENGTH_SHORT).show()
    return true
  }

  companion object {
    private const val TAG = "MainActivity"
  }
}
