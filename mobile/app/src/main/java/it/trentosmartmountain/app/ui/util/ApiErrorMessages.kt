package it.trentosmartmountain.app.ui.util

import com.google.gson.JsonParser
import retrofit2.Response

object ApiErrorMessages {
    fun fromResponse(response: Response<*>): String {
        val code = response.code()
        val body = response.errorBody()?.string().orEmpty()
        val detail = parseDetail(body)
        return when (code) {
            422 -> detail?.let { "Dati non validi: $it" } ?: "Dati non validi (controlla data, difficoltà e partecipanti)."
            409 -> detail ?: "Operazione non consentita (conflitto con lo stato attuale)."
            401 -> "Sessione scaduta: effettua di nuovo il login."
            else -> detail?.let { "Errore server ($code): $it" } ?: "Errore server ($code)."
        }
    }

    private fun parseDetail(body: String): String? {
        if (body.isBlank()) return null
        return try {
            val json = JsonParser.parseString(body).asJsonObject
            json.getAsJsonArray("details")?.firstOrNull()?.asJsonObject?.get("message")?.asString
                ?: json.get("message")?.asString
                ?: json.get("error")?.asString
        } catch (_: Exception) {
            body.take(200).ifBlank { null }
        }
    }
}
