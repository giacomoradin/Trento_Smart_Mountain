package it.trentosmartmountain.app.data.remote.dto

/**
 * Body per `POST /auth/register/refuge` (registrazione rifugio).
 *
 * I metadati struttura (rifugioName, caiCode, quota, posti, coordinates)
 * sono ora **campi flat** nel body — il backend con discriminator "rifugio"
 * li mappa direttamente sul documento Mongoose Refuge.
 *
 * Il ruolo è impostato implicitamente dal backend; nessun campo `role` esplicito.
 */
data class RegisterRifugioRequest(
    val username: String,
    val email: String,
    val password: String,
    val rifugioName: String,
    val caiCode: String?,
    val quota: Int?,
    val posti: Int?,
    val coordinates: String?,
)

/**
 * Wrapper opzionale per i metadati anagrafici rifugio.
 * Mantenuto per backward compatibility durante la transizione del refactor:
 * il vecchio backend usava `rifugioDetails: { ... }` annidato.
 * Da rimuovere quando tutti i client sono migrati.
 */
data class RifugioDetails(
    val rifugioName: String,
    val caiCode: String?,
    val quota: Int?,
    val posti: Int?,
    val coordinates: String?,
)
