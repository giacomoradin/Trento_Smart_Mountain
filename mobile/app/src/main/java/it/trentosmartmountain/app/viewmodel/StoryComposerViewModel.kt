package it.trentosmartmountain.app.viewmodel

import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.CreateStoryRequest
import it.trentosmartmountain.app.data.remote.dto.StoryComposerArgs
import it.trentosmartmountain.app.data.remote.dto.StoryEditorDecor
import it.trentosmartmountain.app.data.remote.dto.StoryMedia
import it.trentosmartmountain.app.data.remote.dto.StoryStickerTransformDto
import it.trentosmartmountain.app.ui.screens.home.story.RouteOverlayMode
import androidx.compose.ui.graphics.toArgb
import it.trentosmartmountain.app.ui.screens.home.story.StoryBitmapExporter
import it.trentosmartmountain.app.ui.screens.home.story.StoryComposerExport
import it.trentosmartmountain.app.ui.screens.home.story.StoryMapSnapshotter
import it.trentosmartmountain.app.ui.screens.home.story.StoryStickerKind
import it.trentosmartmountain.app.ui.screens.home.story.StoryStickerTransform
import it.trentosmartmountain.app.ui.screens.home.story.toHexRgb
import kotlinx.coroutines.yield
import it.trentosmartmountain.app.ui.util.AvatarUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoryComposerViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()

    data class UiState(
        val routePoints: List<it.trentosmartmountain.app.data.remote.dto.RoutePoint> = emptyList(),
        val mediaKind: String? = null,
        val mediaDataUri: String? = null,
        val caption: String = "",
        val isEncoding: Boolean = false,
        val isPublishing: Boolean = false,
        val error: String? = null,
        val published: Boolean = false,
        val routeOverlayMode: RouteOverlayMode = RouteOverlayMode.NONE,
        val mapSceneTransform: StoryStickerTransform = StoryStickerTransform(),
        val routeTransform: StoryStickerTransform = StoryStickerTransform(),
        val mapWidgetTransform: StoryStickerTransform = StoryStickerTransform(scale = 0.85f),
        val routeColor: Color = Color(0xFF4DD0E1),
        val textTransform: StoryStickerTransform = StoryStickerTransform(),
        val textColor: Color = Color.White,
        val textFont: it.trentosmartmountain.app.ui.screens.home.story.StoryFont =
            it.trentosmartmountain.app.ui.screens.home.story.StoryFont.CLASSIC,
        val floatingText: String = "",
        val showTextSticker: Boolean = false,
        val textEditMode: Boolean = false,
        val selectedSticker: StoryStickerKind? = StoryStickerKind.MAP_SCENE,
    ) {
        /** True solo per FOTO: usato per l'export JPEG (background appiattito). */
        val hasCustomBackground: Boolean
            get() = mediaKind == "image" && !mediaDataUri.isNullOrBlank()

        /**
         * True per FOTO **o** VIDEO: il media fa da "sfondo" e gli overlay (traccia /
         * widget mappa) ci vanno SOPRA come sticker. Per il video non possiamo
         * appiattire in JPEG, ma possiamo comunque sovrapporre gli overlay via
         * editorDecor (renderizzati dal viewer sopra al video).
         */
        val hasMediaBackground: Boolean
            get() = !mediaDataUri.isNullOrBlank() && (mediaKind == "image" || mediaKind == "video")
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun initFromArgs(args: StoryComposerArgs) {
        val pts = args.overlay?.routePolyline.orEmpty()
        _state.update {
            it.copy(
                routePoints = pts,
                routeOverlayMode = RouteOverlayMode.NONE,
                selectedSticker = if (pts.size >= 2) StoryStickerKind.MAP_SCENE else null,
            )
        }
    }

    fun onCaptionChange(value: String) {
        _state.update { it.copy(caption = value.take(200)) }
    }

    fun clearMedia() {
        _state.update {
            it.copy(
                mediaKind = null,
                mediaDataUri = null,
                routeOverlayMode = RouteOverlayMode.NONE,
                selectedSticker = if (it.routePoints.size >= 2) StoryStickerKind.MAP_SCENE else null,
            )
        }
    }

    fun onMapSceneTransformChange(t: StoryStickerTransform) {
        _state.update { it.copy(mapSceneTransform = t) }
    }

    fun onRouteTransformChange(t: StoryStickerTransform) {
        _state.update { it.copy(routeTransform = t) }
    }

    fun onMapWidgetTransformChange(t: StoryStickerTransform) {
        _state.update { it.copy(mapWidgetTransform = t) }
    }

    fun onTextTransformChange(t: StoryStickerTransform) {
        _state.update { it.copy(textTransform = t) }
    }

    /** Cicla al font successivo per il testo della storia. */
    fun cycleTextFont() {
        _state.update { it.copy(textFont = it.textFont.next()) }
    }

    fun onRouteColorChange(color: Color) {
        _state.update { it.copy(routeColor = color) }
    }

    fun onTextColorChange(color: Color) {
        _state.update { it.copy(textColor = color) }
    }

    fun selectSticker(kind: StoryStickerKind?) {
        _state.update { state ->
            if (state.textEditMode && kind != StoryStickerKind.TEXT) {
                state
            } else {
                state.copy(selectedSticker = kind)
            }
        }
    }

    fun addTextSticker() {
        _state.update {
            it.copy(
                showTextSticker = true,
                floatingText = it.floatingText.ifBlank { "La tua avventura" },
                textTransform = StoryStickerTransform(),
                selectedSticker = StoryStickerKind.TEXT,
                textEditMode = true,
            )
        }
    }

    fun confirmTextSticker() {
        _state.update {
            it.copy(
                textEditMode = false,
                selectedSticker = defaultStickerAfterText(it),
            )
        }
    }

    private fun defaultStickerAfterText(state: UiState): StoryStickerKind? =
        when {
            !state.hasCustomBackground && state.routePoints.size >= 2 -> StoryStickerKind.MAP_SCENE
            state.routeOverlayMode == RouteOverlayMode.TRACE -> StoryStickerKind.TRACE
            state.routeOverlayMode == RouteOverlayMode.MAP_WIDGET -> StoryStickerKind.MAP_WIDGET
            else -> null
        }

    fun onFloatingTextChange(value: String) {
        _state.update { it.copy(floatingText = value.take(80)) }
    }

    fun removeTextSticker() {
        _state.update {
            it.copy(
                showTextSticker = false,
                floatingText = "",
                textEditMode = false,
                selectedSticker = defaultStickerAfterText(it.copy(showTextSticker = false)),
            )
        }
    }

    fun addTraceOverlay() {
        _state.update {
            it.copy(
                routeOverlayMode = RouteOverlayMode.TRACE,
                selectedSticker = StoryStickerKind.TRACE,
            )
        }
    }

    fun addMapWidgetOverlay() {
        _state.update {
            it.copy(
                routeOverlayMode = RouteOverlayMode.MAP_WIDGET,
                selectedSticker = StoryStickerKind.MAP_WIDGET,
            )
        }
    }

    fun removeRouteOverlay() {
        _state.update {
            it.copy(
                routeOverlayMode = RouteOverlayMode.NONE,
                // Senza media, NONE = mappa a tutto schermo: la selezioniamo subito
                // così colore/sposta/ingrandisci sono disponibili. Con media, NONE =
                // nessun overlay → nessuno sticker selezionato.
                selectedSticker =
                    if (!it.hasMediaBackground && it.routePoints.size >= 2) {
                        StoryStickerKind.MAP_SCENE
                    } else {
                        null
                    },
            )
        }
    }

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
                val isVideoMime = mime.startsWith("video")
                _state.update {
                    it.copy(
                        isEncoding = false,
                        error = if (isVideoMime) {
                            "Video troppo pesante. Registralo dalla fotocamera (max 10s, " +
                                "bassa risoluzione) o scegline uno più corto/leggero."
                        } else {
                            "Immagine non valida. Riprova con un'altra foto."
                        },
                    )
                }
            } else {
                val isImage = result.first == "image"
                val isVideo = result.first == "video"
                _state.update {
                    it.copy(
                        isEncoding = false,
                        mediaKind = result.first,
                        mediaDataUri = result.second,
                        // Foto e video partono SENZA overlay forzato: prima il video
                        // ereditava map_scene → la storia mostrava sempre la mappa sopra
                        // il video (#6). Ora l'utente aggiunge mappa/traccia se vuole (#5).
                        routeOverlayMode = if (isImage || isVideo) RouteOverlayMode.NONE else it.routeOverlayMode,
                        selectedSticker =
                            when {
                                isImage || isVideo -> null
                                it.routePoints.size >= 2 -> StoryStickerKind.MAP_SCENE
                                else -> null
                            },
                    )
                }
            }
        }
    }

    fun publish(
        args: StoryComposerArgs,
        hostActivity: Activity,
        editorCanvasWidthPx: Float,
        editorCanvasHeightPx: Float,
    ) {
        if (_state.value.isPublishing) return
        viewModelScope.launch {
            _state.update { it.copy(isPublishing = true, error = null, selectedSticker = null) }
            yield()
            val s = _state.value
            val isVideo = s.mediaKind == "video"
            val decor = buildEditorDecor(s)
            val overlayForPublish =
                args.overlay?.copy(
                    routePolyline = if (isVideo) args.overlay.routePolyline else null,
                    editorDecor = if (isVideo) decor else null,
                )

            val media: List<StoryMedia> =
                if (isVideo && s.mediaDataUri != null) {
                    listOf(StoryMedia(kind = "video", dataUri = s.mediaDataUri))
                } else {
                    val lineArgb = s.routeColor.toArgb()
                    // Mappa a tutto schermo: solo senza media + mode NONE (default).
                    val mapSceneBmp =
                        if (!s.hasCustomBackground &&
                            s.routeOverlayMode == RouteOverlayMode.NONE &&
                            s.routePoints.size >= 2
                        ) {
                            withContext(Dispatchers.Main) {
                                StoryMapSnapshotter.captureScene(
                                    hostActivity = hostActivity,
                                    points = s.routePoints,
                                    width = StoryComposerExport.WIDTH,
                                    height = StoryComposerExport.HEIGHT,
                                    lineColorArgb = lineArgb,
                                    storyScene = true,
                                )
                            }
                        } else {
                            null
                        }
                    // Widget mappa: ogni volta che l'utente sceglie MAP_WIDGET, con o
                    // senza media di sfondo.
                    val mapWidgetBmp =
                        if (s.routeOverlayMode == RouteOverlayMode.MAP_WIDGET &&
                            s.routePoints.size >= 2
                        ) {
                            withContext(Dispatchers.Main) {
                                StoryMapSnapshotter.captureScene(
                                    hostActivity = hostActivity,
                                    points = s.routePoints,
                                    width = (StoryComposerExport.WIDTH * 0.72f).toInt().coerceAtLeast(320),
                                    height = (StoryComposerExport.HEIGHT * 0.42f).toInt().coerceAtLeast(240),
                                    lineColorArgb = lineArgb,
                                    storyScene = false,
                                )
                            }
                        } else {
                            null
                        }
                    val dataUri =
                        withContext(Dispatchers.Default) {
                            try {
                                StoryBitmapExporter.exportJpegDataUri(
                                    width = StoryComposerExport.WIDTH,
                                    height = StoryComposerExport.HEIGHT,
                                    backgroundDataUri = if (s.hasCustomBackground) s.mediaDataUri else null,
                                    routePoints = s.routePoints,
                                    hasCustomBackground = s.hasCustomBackground,
                                    routeOverlayMode = s.routeOverlayMode,
                                    mapSceneTransform = s.mapSceneTransform,
                                    routeTransform = s.routeTransform,
                                    mapWidgetTransform = s.mapWidgetTransform,
                                    routeColor = s.routeColor,
                                    floatingText =
                                        if (s.showTextSticker) {
                                            s.floatingText.ifBlank { "La tua avventura" }
                                        } else {
                                            null
                                        },
                                    textTransform = if (s.showTextSticker) s.textTransform else null,
                                    textColor = s.textColor,
                                    textFont = s.textFont,
                                    mapSceneBitmap = mapSceneBmp,
                                    mapWidgetBitmap = mapWidgetBmp,
                                    editorCanvasWidthPx = editorCanvasWidthPx,
                                    editorCanvasHeightPx = editorCanvasHeightPx,
                                    quality = StoryComposerExport.QUALITY,
                                )
                            } finally {
                                mapSceneBmp?.recycle()
                                mapWidgetBmp?.recycle()
                            }
                        }
                    if (dataUri == null) {
                        _state.update { it.copy(isPublishing = false, error = "Impossibile generare l'immagine.") }
                        return@launch
                    }
                    listOf(StoryMedia(kind = "image", dataUri = dataUri))
                }

            val req =
                CreateStoryRequest(
                    type = args.type,
                    sessionId = args.sessionId,
                    activityId = args.activityId,
                    caption = s.caption.trim().ifBlank { null },
                    media = media,
                    overlay = overlayForPublish,
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

    private fun buildEditorDecor(s: UiState): StoryEditorDecor? {
        val hasText = s.showTextSticker && s.floatingText.isNotBlank()
        val hasRoute = s.routePoints.size >= 2
        if (!hasText && !hasRoute) return null

        val overlayKind =
            when {
                // Scelta ESPLICITA dell'utente (vale sempre, con o senza media).
                s.routeOverlayMode == RouteOverlayMode.TRACE -> "trace"
                s.routeOverlayMode == RouteOverlayMode.MAP_WIDGET -> "map_widget"
                // NONE senza media di sfondo → mappa a tutto schermo (scena).
                !s.hasMediaBackground && hasRoute -> "map_scene"
                else -> null
            }

        return StoryEditorDecor(
            routeOverlayKind = overlayKind,
            routeColor = if (hasRoute) s.routeColor.toHexRgb() else null,
            routeTransform =
                when (overlayKind) {
                    "trace" -> s.routeTransform.toDto()
                    "map_scene" -> s.mapSceneTransform.toDto()
                    else -> null
                },
            mapWidgetTransform =
                if (overlayKind == "map_widget") s.mapWidgetTransform.toDto() else null,
            floatingText = if (hasText) s.floatingText else null,
            textColor = if (hasText) s.textColor.toHexRgb() else null,
            textTransform = if (hasText) s.textTransform.toDto() else null,
            textFont = if (hasText) s.textFont.key else null,
        )
    }

    private fun StoryStickerTransform.toDto() =
        StoryStickerTransformDto(
            offsetX = offsetX,
            offsetY = offsetY,
            scale = scale,
            rotationDeg = rotationDeg,
        )

    companion object {
        // Cap RAW bytes del video: in base64 diventa ~1.37×, e deve restare sotto
        // il cap backend STORY_MEDIA_MAX_CHARS = 3_800_000 chars.
        //   2_700_000 raw × 1.37 ≈ 3_700_000 base64 < 3_800_000 ✓
        // Prima era 3_500_000 → 4_670_000 base64 > cap backend = 413/422 al publish.
        const val VIDEO_MAX_BYTES = 2_700_000

        // Lato massimo del media importato (prima dell'export con overlay).
        // Bump 1080 → 1440: con display moderni (1080p+) il 1080 mostrava
        // perdita di dettaglio già nel viewer; 1440 dà render nitido anche
        // dopo lo scaling del canvas storia (1080×1920).
        const val STORY_IMAGE_MAX_SIDE = 1440

        // Qualità JPEG del media importato. Bump 75 → 90: il composer applica
        // un secondo passaggio di compressione (StoryComposerExport.QUALITY)
        // quando flatten-a la scena, quindi conviene partire con qualità
        // alta per non accumulare artefatti su due encode consecutivi.
        const val STORY_IMAGE_QUALITY = 90
    }
}
