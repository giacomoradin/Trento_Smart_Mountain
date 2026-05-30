package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.BadgeItem
import it.trentosmartmountain.app.data.remote.dto.CertificateItem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BadgesUiState(
    val isLoading: Boolean = true,
    val badges: List<BadgeItem> = emptyList(),
    val certificates: List<CertificateItem> = emptyList(),
    val error: String? = null,
)

class BadgesViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()

    private val _state = MutableStateFlow(BadgesUiState())
    val state: StateFlow<BadgesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            // Carico in parallelo le due liste; ognuna ha il proprio runCatching così
            // se una fallisce l'altra resta comunque popolata.
            val badgesDeferred = async { runCatching { api.getMyBadges() } }
            val certsDeferred = async { runCatching { api.getMyCertificates() } }

            val badges = badgesDeferred.await()
                .mapCatching { it.body() ?: emptyList() }
                .getOrElse { emptyList() }
            val certs = certsDeferred.await()
                .mapCatching { it.body() ?: emptyList() }
                .getOrElse { emptyList() }

            _state.value = BadgesUiState(
                isLoading = false,
                badges = badges,
                certificates = certs,
            )
        }
    }
}
