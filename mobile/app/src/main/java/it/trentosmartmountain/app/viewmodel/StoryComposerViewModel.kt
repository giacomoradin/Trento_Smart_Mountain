package it.trentosmartmountain.app.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.CreateStoryRequest
import it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs
import it.trentosmartmountain.app.data.remote.dto.StoryMedia
import it.trentosmartmountain.app.ui.util.AvatarUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * VM del composer storie (Fase C). Gestisce il pick + encoding dei media in
 * Base64 (immagine compressa JPEG, video breve con cap dimensione) e la
 * pubblicazione via POST /stories.
 */
class StoryComposerViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()

    data class UiState(
        val mediaKind: String? = null, // "image" | "video"
        val mediaDataUri: String? = null,
        val caption: String = "",
        val isEncoding: Boolean = false,
        val isPublishing: Boolean = false,
        val error: String? = null,
        val published: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onCaptionChange(value: String) {
        _state.update { it.copy(caption = value.take(200)) }
    }

    fun clearMedia() {
        _state.update { it.copy(mediaKind = null, mediaDataUri = null) }
    }

    /** Codifica il media scelto in Base64 (immagine compressa o video capped). */
    fun onMediaPicked(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isEncoding = true, error = null) }
            val mime = resolver.getType(uri).orEmpty()
            val result: Pair<String, String>? = withContext(Dispatchers.IO) {
                runCatching {
                    if (mime.startsWith("video")) {
                        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: return@runCatching null
                        if (bytes.size > VIDEO_MAX_BYTES) return@runCatching null
                        "video" to ("data:video/mp4;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP))
                    } else {
                        val bmp = AvatarUtils.loadOrientedBitmapFromUri(resolver, uri)
                            ?: return@runCatching null
                        val scaled = AvatarUtils.downscaleToBox(bmp, STORY_IMAGE_MAX_SIDE)
                        "image" to AvatarUtils.encodeToDataUri(scaled, STORY_IMAGE_QUALITY)
                    }
                }.getOrNull()
            }
            if (result == null) {
                _state.update {
                    it.copy(
                        isEncoding = false,
                        error = "Media non valido o troppo grande (video ≤ ~3.5MB / ~10s).",
                    )
                }
            } else {
                _state.update {
                    it.copy(isEncoding = false, mediaKind = result.first, mediaDataUri = result.second)
                }
            }
        }
    }

    /** Pubblica la storia. Il media è opzionale (es. sola card pianificazione + overlay). */
    fun publish(args: StoryComposerArgs) {
        val s = _state.value
        if (s.isPublishing) return
        val media = if (s.mediaDataUri != null && s.mediaKind != null) {
            listOf(StoryMedia(kind = s.mediaKind, dataUri = s.mediaDataUri))
        } else emptyList()
        viewModelScope.launch {
            _state.update { it.copy(isPublishing = true, error = null) }
            val req = CreateStoryRequest(
                type = args.type,
                sessionId = args.sessionId,
                activityId = args.activityId,
                caption = s.caption.trim().ifBlank { null },
                media = media,
                overlay = args.overlay,
            )
            runCatching { api.createStory(req) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.update { it.copy(isPublishing = false, published = true) }
                    } else {
                        _state.update {
                            it.copy(
                                isPublishing = false,
                                error = when (resp.code()) {
                                    413 -> "Media troppo grande."
                                    403 -> "Non puoi creare una storia per questo contenuto."
                                    422 -> "Dati storia non validi."
                                    else -> "Errore server (${resp.code()})."
                                },
                            )
                        }
                    }
                }
                .onFailure { _state.update { it.copy(isPublishing = false, error = "Errore di rete.") } }
        }
    }

    companion object {
        // Cap allineato al backend (≈3.8MB per media, teniamo margine sul client).
        const val VIDEO_MAX_BYTES = 3_500_000
        const val STORY_IMAGE_MAX_SIDE = 1080
        const val STORY_IMAGE_QUALITY = 75
    }
}
