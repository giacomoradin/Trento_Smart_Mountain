package it.trentosmartmountain.app.data.ble

import android.bluetooth.le.ScanResult
import java.util.UUID

/**
 * Beacon BLE proprietario TSM (formato iBeacon-like, manufacturer ID **non** Apple).
 *
 * Su Android la pubblicità con company ID 0x004C (Apple) spesso fallisce o non viene trasmessa;
 * usiamo 0x5453 ("TS") per mittente e ricevitore.
 */
object SosBeaconProtocol {
  /** Manufacturer ID Bluetooth (0x5453 = "TS"). */
  const val MANUFACTURER_ID = 0x5453

  val PROXIMITY_UUID: UUID = UUID.fromString("F7826DA6-4FA2-4E98-8024-BC5B71E0893E")

  const val TX_POWER_AT_1M: Byte = 0xC5.toByte() // -59 dBm

  private const val IBEACON_PREFIX_0 = 0x02.toByte()
  private const val IBEACON_PREFIX_1 = 0x15.toByte()

  data class Match(
    val rssi: Int,
    val major: Int,
    val minor: Int,
  )

  /** Converte 12 hex in major (primi 4) e minor (successivi 4). */
  fun beaconInstanceIdToMajorMinor(hex: String): Pair<Int, Int> {
    val clean = hex.lowercase().filter { it in '0'..'9' || it in 'a'..'f' }.padEnd(12, '0').take(12)
    val major = clean.substring(0, 4).toInt(16) and 0xFFFF
    val minor = clean.substring(4, 8).toInt(16) and 0xFFFF
    return major to minor
  }

  fun buildManufacturerPayload(uuid: UUID, major: Int, minor: Int, txPower: Byte = TX_POWER_AT_1M): ByteArray {
    val msb = uuid.mostSignificantBits
    val lsb = uuid.leastSignificantBits
    val uuidBytes = ByteArray(16)
    for (i in 0 until 8) {
      uuidBytes[i] = (msb shr (8 * (7 - i))).toByte()
      uuidBytes[8 + i] = (lsb shr (8 * (7 - i))).toByte()
    }
    return byteArrayOf(
      IBEACON_PREFIX_0,
      IBEACON_PREFIX_1,
      *uuidBytes,
      ((major shr 8) and 0xFF).toByte(),
      (major and 0xFF).toByte(),
      ((minor shr 8) and 0xFF).toByte(),
      (minor and 0xFF).toByte(),
      txPower,
    )
  }

  fun matchesBeaconInstanceId(scanResult: ScanResult, beaconInstanceId: String): Match? {
    val data = scanResult.scanRecord?.getManufacturerSpecificData(MANUFACTURER_ID) ?: return null
    return parsePayload(data, scanResult.rssi, beaconInstanceId)
  }

  internal fun parsePayload(data: ByteArray, rssi: Int, beaconInstanceId: String): Match? {
    if (data.size < 23 || data[0] != IBEACON_PREFIX_0 || data[1] != IBEACON_PREFIX_1) return null

    val uuidBytes = data.copyOfRange(2, 18)
    if (!uuidBytes.contentEquals(uuidToBytes(PROXIMITY_UUID))) return null

    val major = ((data[18].toInt() and 0xFF) shl 8) or (data[19].toInt() and 0xFF)
    val minor = ((data[20].toInt() and 0xFF) shl 8) or (data[21].toInt() and 0xFF)
    val (targetMajor, targetMinor) = beaconInstanceIdToMajorMinor(beaconInstanceId)
    if (major != targetMajor || minor != targetMinor) return null

    return Match(rssi = rssi, major = major, minor = minor)
  }

  private fun uuidToBytes(uuid: UUID): ByteArray {
    val msb = uuid.mostSignificantBits
    val lsb = uuid.leastSignificantBits
    val bytes = ByteArray(16)
    for (i in 0 until 8) {
      bytes[i] = (msb shr (8 * (7 - i))).toByte()
      bytes[8 + i] = (lsb shr (8 * (7 - i))).toByte()
    }
    return bytes
  }
}
