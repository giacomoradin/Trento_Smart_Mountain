package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.JwtDecoder
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.FeedItem
import it.trentosmartmountain.app.data.remote.dto.RoutePoint
import it.trentosmartmountain.app.util.ELEVATION_PROFILE_MAX_POINTS
import it.trentosmartmountain.app.util.downsampleByIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del dettaglio "social" di un post (Activity/HikeSession condivisa).
 *
 * Parte dal [FeedItem] del feed; arricchisce route e profilo altimetrico da API
 * (sessione) o da Room/API attività quando mancano nel payload del feed.
 */
class PostDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()
    private val gson = Gson()
    private val _item = MutableStateFlow<FeedItem?>(null)
    val item: StateFlow<FeedItem?> = _item.asStateFlow()

    /** Inizializza con l'item passato dal chiamante e arricchisce mappa/altimetria. */
    fun init(initial: FeedItem) {
        if (_item.value?.id != initial.id) {
            _item.value = initial
            enrichRouteAndElevation(initial)
        }
    }

    private fun enrichRouteAndElevation(seed: FeedItem) {
        viewModelScope.launch {
            val enriched = when (seed.kind) {
                "session" -> enrichFromSession(seed)
                else -> enrichFromActivity(seed)
            }
            if (enriched != null && _item.value?.id == seed.id) {
                _item.value = enriched
            }
        }
    }

    private suspend fun enrichFromSession(item: FeedItem): FeedItem? {
        val resp = runCatching { api.getSessionById(item.id) }.getOrNull() ?: return null
        if (!resp.isSuccessful) return null
        val session = resp.body() ?: return null
        val profile = session.gpxStats?.elevationProfile?.takeIf { it.size >= 2 }
            ?: item.elevationProfile
        val route = session.plannedRoute?.polylinePoints
            ?.map { RoutePoint(it.lat, it.lon) }
            ?.takeIf { it.size >= 2 }
            ?: item.routePolyline
        val stats = session.actualStats
        return item.copy(
            routePolyline = route,
            elevationProfile = profile,
            distanceMeters = item.distanceMeters
                ?: stats?.distanceMeters
                ?: session.gpxStats?.distanceKm?.let { it * 1000 },
            elevationGainM = item.elevationGainM
                ?: stats?.elevationGainM
                ?: session.gpxStats?.elevationGainM,
            movingSeconds = item.movingSeconds ?: stats?.movingSeconds,
            difficultyLevel = item.difficultyLevel ?: session.routeDetails?.difficultyLevel,
        )
    }

    private suspend fun enrichFromActivity(item: FeedItem): FeedItem? {
        val app = getApplication<TsmApplication>()
        val dao = app.database.completedActivityDao()
        val local = dao.getById(item.id) ?: dao.getByRemoteId(item.id)
        val track = local?.trackLatLng?.let { parseTrack(it) }.orEmpty()
        var profile = item.elevationProfile?.takeIf { it.size >= 2 }
        var route = item.routePolyline?.takeIf { it.size >= 2 }
        if (track.size >= 2) {
            if (profile == null) {
                val alts = track.map { it.third }
                profile = downsampleByIndex(alts, ELEVATION_PROFILE_MAX_POINTS)
            }
            if (route == null) {
                route = downsampleByIndex(
                    track.map { RoutePoint(it.first, it.second) },
                    80,
                )
            }
        }
        val token = app.tokenStorage.getToken().orEmpty()
        val selfId = JwtDecoder.userIdFrom(token)
        val isOwn = !selfId.isNullOrBlank() && item.user?._id == selfId
        if (isOwn && (profile == null || route == null)) {
            val resp = runCatching { api.getActivityById(item.id) }.getOrNull()
            if (resp?.isSuccessful == true) {
                val act = resp.body()
                if (profile == null) profile = act?.elevationProfile?.takeIf { it.size >= 2 }
                if (route == null) route = act?.routePolyline?.takeIf { it.size >= 2 }
            }
        }
        return item.copy(
            routePolyline = route ?: item.routePolyline,
            elevationProfile = profile ?: item.elevationProfile,
        )
    }

    private fun parseTrack(json: String): List<Triple<Double, Double, Double>> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val type = object : TypeToken<List<List<Double>>>() {}.type
            val raw: List<List<Double>> = gson.fromJson(json, type)
            raw.mapNotNull { pts ->
                if (pts.size >= 2) Triple(pts[0], pts[1], pts.getOrElse(2) { 0.0 }) else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Like ottimistico con rollback, allineato a SocialFeedViewModel.toggleLike. */
    fun toggleLike() {
        val current = _item.value ?: return
        val willLike = !current.likedByMe
        _item.value = current.copy(
            likedByMe = willLike,
            likesCount = (current.likesCount + if (willLike) 1 else -1).coerceAtLeast(0),
        )
        viewModelScope.launch {
            val resp = runCatching {
                if (current.kind == "activity") {
                    if (willLike) api.likeActivity(current.id) else api.unlikeActivity(current.id)
                } else {
                    if (willLike) api.likeSession(current.id) else api.unlikeSession(current.id)
                }
            }.getOrNull()
            if (resp != null && resp.isSuccessful && resp.body() != null) {
                val server = resp.body()!!
                _item.value = _item.value?.copy(likesCount = server.likesCount, likedByMe = server.likedByMe)
            } else {
                _item.value = current
            }
        }
    }

    fun setCommentCount(count: Int) {
        _item.value = _item.value?.let { if (it.commentsCount == count) it else it.copy(commentsCount = count) }
    }
}
