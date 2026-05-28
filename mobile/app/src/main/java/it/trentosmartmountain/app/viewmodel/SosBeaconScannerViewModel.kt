package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.ble.SosBeaconParser
import it.trentosmartmountain.app.data.ble.SosBeaconProtocol
import it.trentosmartmountain.app.data.ble.SosBeaconScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

class SosBeaconScannerViewModel(application: Application) : AndroidViewModel(application) {

  data class UiState(
    val beaconInstanceId: String = "",
    val isScanning: Boolean = false,
    val rssi: Int? = null,
    val smoothedRssi: Int? = null,
    val estimatedDistanceM: Double? = null,
    val signalBand: SosBeaconParser.SignalBand? = null,
    val errorMessage: String? = null,
    val permissionDenied: Boolean = false,
    /** true se la scansione BLE riceve almeno un pacchetto (non necessariamente il beacon target). */
    val blePacketsSeen: Boolean = false,
    val targetMajorMinorLabel: String = "",
  )

  private val _state = MutableStateFlow(UiState())
  val state: StateFlow<UiState> = _state.asStateFlow()

  private var scanner: SosBeaconScanner? = null
  private val rssiWindow = ArrayDeque<Int>(8)

  fun onPermissionsResult(granted: Boolean) {
    if (granted) {
      _state.update { it.copy(permissionDenied = false, errorMessage = null) }
      startScan(_state.value.beaconInstanceId)
    } else {
      _state.update { it.copy(permissionDenied = true, isScanning = false) }
    }
  }

  fun startScan(beaconInstanceId: String) {
    if (beaconInstanceId.isBlank()) return
    val (major, minor) = SosBeaconProtocol.beaconInstanceIdToMajorMinor(beaconInstanceId)
    _state.update {
      it.copy(
        beaconInstanceId = beaconInstanceId.trim().lowercase(),
        errorMessage = null,
        permissionDenied = false,
        blePacketsSeen = false,
        rssi = null,
        smoothedRssi = null,
        estimatedDistanceM = null,
        signalBand = null,
        targetMajorMinorLabel = "major=$major minor=$minor",
      )
    }
    val app = getApplication<Application>()
    val ble =
      scanner
        ?: SosBeaconScanner(
          app,
          onRssi = ::onRssiSample,
          onError = { msg -> _state.update { s -> s.copy(errorMessage = msg, isScanning = false) } },
          onDebugScan = { _state.update { s -> s.copy(blePacketsSeen = true) } },
        ).also { scanner = it }

    if (!ble.hasBluetooth()) {
      _state.update { it.copy(errorMessage = "Bluetooth non supportato", isScanning = false) }
      return
    }
    if (!ble.isBluetoothEnabled()) {
      _state.update { it.copy(errorMessage = "Attiva il Bluetooth per cercare il segnale", isScanning = false) }
      return
    }

    rssiWindow.clear()
    ble.start(beaconInstanceId)
    _state.update { it.copy(isScanning = true, errorMessage = null) }
  }

  fun stopScan() {
    scanner?.stop()
    _state.update { it.copy(isScanning = false) }
  }

  private fun onRssiSample(rssi: Int) {
    rssiWindow.addLast(rssi)
    if (rssiWindow.size > 8) rssiWindow.removeFirst()
    val smoothed = rssiWindow.average().roundToInt()
    val distance = SosBeaconParser.estimateDistanceMeters(smoothed)
    _state.update {
      it.copy(
        rssi = rssi,
        smoothedRssi = smoothed,
        estimatedDistanceM = distance,
        signalBand = SosBeaconParser.signalBand(smoothed),
        errorMessage = null,
      )
    }
  }

  override fun onCleared() {
    stopScan()
    super.onCleared()
  }
}
