package it.trentosmartmountain.app.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log

/**
 * Scansione BLE per beacon SOS TSM (manufacturer 0x5453).
 */
class SosBeaconScanner(
  context: Context,
  private val onRssi: (Int) -> Unit,
  private val onError: (String) -> Unit,
  private val onDebugScan: ((Int) -> Unit)? = null,
) {
  private val bluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val adapter: BluetoothAdapter? = bluetoothManager.adapter
  private var scanner = adapter?.bluetoothLeScanner
  private var targetBeaconId: String? = null
  private var scanning = false

  private val callback =
    object : ScanCallback() {
      override fun onScanResult(callbackType: Int, result: ScanResult) {
        onDebugScan?.invoke(result.rssi)
        val target = targetBeaconId ?: return
        val match = SosBeaconParser.matchesBeaconInstanceId(result, target) ?: return
        onRssi(match.rssi)
      }

      override fun onScanFailed(errorCode: Int) {
        Log.e(TAG, "onScanFailed code=$errorCode")
        onError("Scansione BLE fallita (codice $errorCode)")
      }
    }

  @SuppressLint("MissingPermission")
  fun start(beaconInstanceId: String) {
    if (scanning) stop()
    val ad = adapter
    if (ad == null || !ad.isEnabled) {
      onError("Bluetooth spento o non disponibile")
      return
    }
    scanner = ad.bluetoothLeScanner
    if (scanner == null) {
      onError("Scanner BLE non disponibile")
      return
    }
    targetBeaconId = beaconInstanceId.trim().lowercase()

    // Filtro manufacturer TSM; se il dispositivo non supporta il filtro, la callback filtra comunque.
    val prefix = byteArrayOf(0x02, 0x15)
    val mask = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
    val filter =
      ScanFilter.Builder()
        .setManufacturerData(SosBeaconProtocol.MANUFACTURER_ID, prefix, mask)
        .build()

    val settings =
      ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setReportDelay(0L)
        .build()

    runCatching {
      scanner?.startScan(listOf(filter), settings, callback)
      scanning = true
      Log.d(TAG, "Scan avviata per beacon=$targetBeaconId")
    }.onFailure { err ->
      Log.e(TAG, "startScan fallita", err)
      onError("Impossibile avviare la scansione: ${err.message}")
    }
  }

  @SuppressLint("MissingPermission")
  fun stop() {
    if (!scanning) return
    runCatching { scanner?.stopScan(callback) }
    scanning = false
    targetBeaconId = null
  }

  fun hasBluetooth(): Boolean = adapter != null

  fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

  companion object {
    private const val TAG = "SosBeaconScanner"
  }
}
