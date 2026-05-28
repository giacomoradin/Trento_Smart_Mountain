package it.trentosmartmountain.app.data.ble

import android.bluetooth.le.ScanResult
import kotlin.math.pow

object SosBeaconParser {
  private const val TX_POWER_AT_1M = -59

  fun matchesBeaconInstanceId(scanResult: ScanResult, beaconInstanceId: String): SosBeaconProtocol.Match? =
    SosBeaconProtocol.matchesBeaconInstanceId(scanResult, beaconInstanceId)

  fun estimateDistanceMeters(rssi: Int, txPower: Int = TX_POWER_AT_1M): Double? {
    if (rssi >= 0) return null
    val ratio = rssi.toDouble() / txPower.toDouble()
    val meters =
      if (ratio < 1.0) {
        ratio.pow(10.0)
      } else {
        0.89976 * ratio.pow(7.7095) + 0.111
      }
    return meters.coerceIn(0.0, 999.0)
  }

  enum class SignalBand { EXCELLENT, GOOD, WEAK, LOST }

  fun signalBand(rssi: Int): SignalBand =
    when {
      rssi >= -60 -> SignalBand.EXCELLENT
      rssi >= -75 -> SignalBand.GOOD
      rssi >= -88 -> SignalBand.WEAK
      else -> SignalBand.LOST
    }
}
