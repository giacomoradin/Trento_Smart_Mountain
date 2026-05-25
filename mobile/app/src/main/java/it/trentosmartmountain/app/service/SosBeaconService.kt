package it.trentosmartmountain.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import androidx.core.app.NotificationCompat
import it.trentosmartmountain.app.MainActivity
import it.trentosmartmountain.app.R
import java.util.UUID

/**
 * Foreground service che trasmette un beacon iBeacon-compatibile per il soccorso in prossimità.
 * UUID namespace TSM fisso; major/minor derivati da [beaconInstanceId] (12 hex).
 */
class SosBeaconService : Service() {

  private var advertiser: BluetoothLeAdvertiser? = null
  private var advertiseCallback: AdvertiseCallback? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopAdvertising()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      else -> {
        val beaconId = intent?.getStringExtra(EXTRA_BEACON_INSTANCE_ID)
        if (beaconId.isNullOrBlank()) {
          stopSelf()
          return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        startAdvertising(beaconId)
      }
    }
    return START_STICKY
  }

  override fun onDestroy() {
    stopAdvertising()
    super.onDestroy()
  }

  private fun startAdvertising(beaconInstanceId: String) {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
    if (!adapter.isEnabled) return
    advertiser = adapter.bluetoothLeAdvertiser ?: return

    val (major, minor) = beaconInstanceIdToMajorMinor(beaconInstanceId)
    val manufacturerData = buildIBeaconPayload(TSM_PROXIMITY_UUID, major, minor, TX_POWER_AT_1M)

    val settings =
      AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setConnectable(false)
        .setTimeout(0)
        .build()

    val data =
      AdvertiseData.Builder()
        .setIncludeDeviceName(false)
        .addManufacturerData(APPLE_MANUFACTURER_ID, manufacturerData)
        .build()

    advertiseCallback =
      object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {}

        override fun onStartFailure(errorCode: Int) {}
      }

    advertiser?.startAdvertising(settings, data, advertiseCallback)
  }

  private fun stopAdvertising() {
    val cb = advertiseCallback ?: return
    advertiser?.stopAdvertising(cb)
    advertiseCallback = null
  }

  private fun buildNotification(): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          getString(R.string.sos_beacon_channel_name),
          NotificationManager.IMPORTANCE_LOW,
        )
      (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
    val openIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(getString(R.string.sos_beacon_notification_title))
      .setContentText(getString(R.string.sos_beacon_notification_body))
      .setContentIntent(openIntent)
      .setOngoing(true)
      .build()
  }

  companion object {
    const val ACTION_START = "it.trentosmartmountain.SOS_BEACON_START"
    const val ACTION_STOP = "it.trentosmartmountain.SOS_BEACON_STOP"
    const val EXTRA_BEACON_INSTANCE_ID = "beaconInstanceId"

    /** UUID iBeacon namespace Trento Smart Mountain */
    val TSM_PROXIMITY_UUID: UUID =
      UUID.fromString("F7826DA6-4FA2-4E98-8024-BC5B71E0893E")

    private const val APPLE_MANUFACTURER_ID = 0x004C
    private const val TX_POWER_AT_1M: Byte = 0xC5.toByte() // -59 dBm
    private const val CHANNEL_ID = "sos_beacon"
    private const val NOTIFICATION_ID = 9102

    fun start(context: Context, beaconInstanceId: String) {
      val intent =
        Intent(context, SosBeaconService::class.java).apply {
          action = ACTION_START
          putExtra(EXTRA_BEACON_INSTANCE_ID, beaconInstanceId)
        }
      context.startForegroundService(intent)
    }

    fun stop(context: Context) {
      val intent =
        Intent(context, SosBeaconService::class.java).apply {
          action = ACTION_STOP
        }
      context.startService(intent)
    }

    /** Converte 12 hex in major (byte 2-3) e minor (byte 4-5). */
    fun beaconInstanceIdToMajorMinor(hex: String): Pair<Int, Int> {
      val clean = hex.lowercase().padEnd(12, '0').take(12)
      val major = clean.substring(0, 4).toInt(16) and 0xFFFF
      val minor = clean.substring(4, 8).toInt(16) and 0xFFFF
      return major to minor
    }

    private fun buildIBeaconPayload(uuid: UUID, major: Int, minor: Int, txPower: Byte): ByteArray {
      val msb = uuid.mostSignificantBits
      val lsb = uuid.leastSignificantBits
      val uuidBytes = ByteArray(16)
      for (i in 0 until 8) {
        uuidBytes[i] = (msb shr (8 * (7 - i))).toByte()
        uuidBytes[8 + i] = (lsb shr (8 * (7 - i))).toByte()
      }
      return byteArrayOf(
        0x02,
        0x15,
        *uuidBytes,
        ((major shr 8) and 0xFF).toByte(),
        (major and 0xFF).toByte(),
        ((minor shr 8) and 0xFF).toByte(),
        (minor and 0xFF).toByte(),
        txPower,
      )
    }
  }
}
