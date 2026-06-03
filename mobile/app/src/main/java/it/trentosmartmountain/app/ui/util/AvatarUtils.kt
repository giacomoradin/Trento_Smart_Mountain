package it.trentosmartmountain.app.ui.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Utility per gestire le foto profilo (avatar) end-to-end.
 *
 * Responsabilità:
 *  - Caricare un URI immagine (galleria o camera) → [Bitmap] correttamente
 *    orientato (lettura EXIF + matrix rotate).
 *  - Effettuare downscale + compressione JPEG + encoding Base64 → data URI
 *    pronto per `PATCH /api/v1/users/me/personal-info`.
 *  - Decodificare una stringa data-URI Base64 ricevuta dal server → [Bitmap]
 *    visualizzabile.
 *
 * Le funzioni di IO (`loadOrientedBitmapFromUri`) NON sono safe sul main thread
 * per file grandi: chiamarle dentro `Dispatchers.IO`. La decodifica di una
 * stringa Base64 piccola (avatar 500px ~ 30–100 KB) può essere eseguita sul
 * main, ma il composable `AvatarImage` la memoizza con `remember(avatarUrl)`
 * per evitare ridecoding ad ogni ricomposizione.
 *
 * Sicurezza: l'encode usa `Base64.NO_WRAP` (nessun newline) per garantire che
 * la stringa risultante non venga troncata da middleware HTTP che applicano
 * normalizzazione delle whitespace nei JSON body.
 */
object AvatarUtils {

    private const val TAG = "AvatarUtils"

    /**
     * Lato massimo dell'avatar dopo il downscale (px).
     *
     * Bump da 500 → 720: il vecchio valore generava avatar visibilmente sgranati
     * sui device Hi-DPI (un avatar 56dp su Pixel 7 = ~336 px reali). 720 mantiene
     * sample 1:1 anche su display ad alta densità senza far esplodere il base64.
     */
    const val TARGET_DIMENSION_PX: Int = 720

    /**
     * Qualità JPEG usata per la compressione (0–100).
     *
     * Bump da 70 → 88: 70 introduceva artefatti di blocking visibili sui dettagli
     * fini (volti) anche dopo downscale. 88 è il sweet-spot Strava/Instagram:
     * ~1.6× byte-size del 70, qualità percettiva pulita.
     */
    const val JPEG_QUALITY: Int = 88

    /** Prefisso del data URI che l'app produce e si aspetta dal server. */
    const val DATA_URI_PREFIX: String = "data:image/jpeg;base64,"

    /**
     * Carica il bitmap da un URI di content provider applicando la rotazione
     * EXIF (foto camera in portrait su molti device hanno orientation=6/8 e
     * apparirebbero ruotate altrimenti).
     *
     * @return [Bitmap] orientato correttamente, oppure `null` se la decode
     *   fallisce (URI non valido, formato non supportato, OutOfMemory).
     */
    fun loadOrientedBitmapFromUri(resolver: ContentResolver, uri: Uri): Bitmap? {
        // Leggiamo tutti i bytes una volta: ci servono sia per EXIF che per BitmapFactory.
        // L'InputStream una volta consumato non è ri-aperibile in modo affidabile su
        // alcuni ContentProvider (ad es. il picker temporaneo di GetContent).
        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (t: Throwable) {
            Log.w(TAG, "Impossibile leggere URI $uri: ${t.message}")
            null
        } ?: return null

        val bitmap = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (t: Throwable) {
            Log.w(TAG, "BitmapFactory.decodeByteArray fallito: ${t.message}")
            null
        } ?: return null

        val orientation = try {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (t: Throwable) {
            // File senza EXIF (es. PNG da screenshot) → orientation normale.
            ExifInterface.ORIENTATION_NORMAL
        }

        return rotateBitmapForExif(bitmap, orientation)
    }

    /**
     * Applica la matrix di rotazione/flip corrispondente al tag EXIF orientation.
     * Se l'orientazione è NORMAL ritorna il bitmap originale senza copie.
     */
    fun rotateBitmapForExif(bitmap: Bitmap, exifOrientation: Int): Bitmap {
        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f); matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (t: OutOfMemoryError) {
            // Fallback: ritorniamo il bitmap originale invece di crashare.
            Log.w(TAG, "OOM in rotateBitmapForExif, mantengo orientation originale")
            bitmap
        }
    }

    /**
     * Downscale del [Bitmap] mantenendo l'aspect ratio: il lato maggiore viene
     * portato a [TARGET_DIMENSION_PX] (default 500). Se l'immagine è già più
     * piccola del target, ritorna il bitmap invariato.
     *
     * Usiamo il lato MAGGIORE invece del solo width (come faceva la versione
     * pre-refactor) così foto verticali non restano enormi sull'altezza.
     */
    fun downscaleToBox(bitmap: Bitmap, maxSide: Int = TARGET_DIMENSION_PX): Bitmap {
        val long = maxOf(bitmap.width, bitmap.height)
        if (long <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / long
        val newW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    /**
     * Codifica un [Bitmap] come data URI Base64 JPEG, pronto per l'invio al
     * backend tramite `PATCH /personal-info`.
     *
     * Usa [Base64.NO_WRAP] per evitare newline che potrebbero (a) rompere il
     * JSON serializzato, (b) far fallire le regex di parsing del data URI.
     */
    fun encodeToDataUri(bitmap: Bitmap, jpegQuality: Int = JPEG_QUALITY): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        return DATA_URI_PREFIX + base64
    }

    /**
     * Decodifica un data URI Base64 in [Bitmap]. Ritorna `null` se la stringa
     * non rispetta il formato `data:image/...;base64,XXX` oppure se la decode
     * fallisce (Base64 corrotto, bytes non riconosciuti come immagine).
     *
     * Robusto sui prefissi: accetta sia `data:image/jpeg;base64,` che
     * `data:image/png;base64,` ecc. — taglia tutto fino al primo `base64,`.
     */
    fun decodeDataUri(dataUri: String?): Bitmap? {
        if (dataUri.isNullOrBlank()) return null
        if (!dataUri.startsWith("data:image")) return null
        val commaIdx = dataUri.indexOf("base64,")
        if (commaIdx < 0) return null
        val base64Body = dataUri.substring(commaIdx + "base64,".length)
        return try {
            val bytes = Base64.decode(base64Body, Base64.DEFAULT)
            // Storie/avatar grandi: decode con sample size per evitare OOM sul main thread.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val sample = calculateInSampleSize(bounds, TARGET_DIMENSION_PX * 2)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (t: OutOfMemoryError) {
            Log.w(TAG, "OOM decodeDataUri")
            null
        } catch (t: Throwable) {
            Log.w(TAG, "decodeDataUri fallito: ${t.message}")
            null
        }
    }

    /**
     * Pipeline completa per l'upload: URI → bitmap orientato → downscale →
     * JPEG q70 → Base64 data URI. Ritorna `null` se uno qualsiasi degli step
     * fallisce (così il chiamante può mostrare un Toast di errore).
     */
    private fun calculateInSampleSize(bounds: BitmapFactory.Options, maxSide: Int): Int {
        val long = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (long / sample > maxSide) sample *= 2
        return sample
    }

    fun prepareAvatarForUpload(resolver: ContentResolver, uri: Uri): String? {
        val bitmap = loadOrientedBitmapFromUri(resolver, uri) ?: return null
        val small = downscaleToBox(bitmap)
        return encodeToDataUri(small)
    }
}
