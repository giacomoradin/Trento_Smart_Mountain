package it.trentosmartmountain.app.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.nfc.NfcTagBus
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.NfcScanRequest
import it.trentosmartmountain.app.data.remote.dto.NfcScanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NfcScanUiState {
    object Waiting : NfcScanUiState()
    object Scanning : NfcScanUiState()
    data class Success(val response: NfcScanResponse) : NfcScanUiState()
    data class Error(val message: String) : NfcScanUiState()
}

class NfcScanViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()

    private val _state = MutableStateFlow<NfcScanUiState>(NfcScanUiState.Waiting)
    val state: StateFlow<NfcScanUiState> = _state.asStateFlow()

    var currentLocation: Location? = null

    init {
        viewModelScope.launch {
            NfcTagBus.tagId.collect { tagId -> onTagScanned(tagId, currentLocation) }
        }
    }

    fun onTagScanned(tagId: String, location: Location?) {
        if (_state.value is NfcScanUiState.Scanning) return
        // Senza coordinate GPS lo scan è inutile: server calcolerebbe distanza da
        // (0,0) → sempre OUT_OF_RANGE. Avvisiamo l'utente invece di buttare via il tag.
        if (location == null) {
            _state.value = NfcScanUiState.Error("Posizione GPS non disponibile. Attiva il GPS e riprova.")
            return
        }
        _state.value = NfcScanUiState.Scanning
        // Consumiamo subito il tag dal bus: se l'utente riapre la schermata non
        // viene riprocessato lo stesso tagId (il replay=1 serve solo per cold start).
        NfcTagBus.consume()
        viewModelScope.launch {
            runCatching {
                api.scanNfcTotem(NfcScanRequest(tagId = tagId, gpsLon = location.longitude, gpsLat = location.latitude))
            }.onSuccess { resp ->
                val body = resp.body()
                if (resp.isSuccessful && body != null) {
                    _state.value = NfcScanUiState.Success(body)
                } else {
                    _state.value = NfcScanUiState.Error("Errore server: ${resp.code()}")
                }
            }.onFailure {
                _state.value = NfcScanUiState.Error(it.message ?: "Errore di rete")
            }
        }
    }

    fun reset() { _state.value = NfcScanUiState.Waiting }
}
