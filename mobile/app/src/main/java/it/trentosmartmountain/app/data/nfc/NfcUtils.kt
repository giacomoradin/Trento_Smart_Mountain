package it.trentosmartmountain.app.data.nfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag

/**
 * Utility centralizzate per la gestione dei tag NFC.
 *
 * Un solo punto di conversione `bytes → hex uppercase` evita derive future
 * (es. qualcuno che usa `%02x` minuscolo o `joinToString(":")` per debug → il
 * server riceve un formato diverso e il tagId non matcha più il record nel DB).
 */
object NfcUtils {

    /** Formato canonico del `tagId` lato server: hex uppercase concatenato, senza separatori. */
    fun bytesToTagId(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it) }

    /** Estrae il `tagId` dal Tag scansionato, o `null` se il tag non ha un id (caso patologico). */
    fun extractTagId(tag: Tag): String? = tag.id?.let { bytesToTagId(it) }

    /** Restituisce true se l'intent in arrivo è uno scan NFC (NDEF o Tag generico). */
    fun isNfcIntent(intent: Intent): Boolean =
        intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TAG_DISCOVERED

    /**
     * Estrae il tagId direttamente dall'intent di sistema. Restituisce null se:
     *  - l'intent non è uno scan NFC,
     *  - manca l'EXTRA_TAG (può capitare con alcune ROM custom),
     *  - il tag non espone un id (raro, ma possibile con tag NDEF puramente Type-4).
     */
    fun extractTagIdFromIntent(intent: Intent): String? {
        if (!isNfcIntent(intent)) return null
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return null
        return extractTagId(tag)
    }
}
