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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import it.trentosmartmountain.app.MainActivity
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.ble.BluetoothHelper
import it.trentosmartmountain.app.data.ble.SosBeaconProtocol

/**
 * Foreground service che trasmette un beacon BLE TSM per il soccorso in prossimità.
 * La notifica «in trasmissione» compare solo dopo avvio riuscito dell'advertising.
 */
class SosBeaconService : Service() {

  private var advertiser: BluetoothLeAdvertiser? = null
  private var advertiseCallback: AdvertiseCallback? = null
  private val mainHandler = Handler(Looper.getMainLooper())

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
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildPreparingNotification())
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
    if (!BluetoothHelper.isBluetoothEnabled(this)) {
      Log.e(TAG, "Bluetooth spento — beacon non avviato")
      showNotification(NotificationKind.BLUETOOTH_OFF)
      mainHandler.postDelayed({ stopSelf() }, 2_500)
      return
    }

    val adapter = BluetoothHelper.adapter(this)
    if (adapter == null) {
      showNotification(NotificationKind.BLUETOOTH_OFF)
      mainHandler.postDelayed({ stopSelf() }, 2_500)
      return
    }

    advertiser = adapter.bluetoothLeAdvertiser
    if (advertiser == null) {
      Log.e(TAG, "BluetoothLeAdvertiser null")
      showNotification(NotificationKind.FAILED)
      mainHandler.postDelayed({ stopSelf() }, 2_500)
      return
    }

    val (major, minor) = SosBeaconProtocol.beaconInstanceIdToMajorMinor(beaconInstanceId)
    val manufacturerData =
      SosBeaconProtocol.buildManufacturerPayload(
        SosBeaconProtocol.PROXIMITY_UUID,
        major,
        minor,
      )

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
        .addManufacturerData(SosBeaconProtocol.MANUFACTURER_ID, manufacturerData)
        .build()

    stopAdvertising()

    advertiseCallback =
      object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
          Log.i(TAG, "Beacon SOS in trasmissione id=$beaconInstanceId major=$major minor=$minor")
          showNotification(NotificationKind.TRANSMITTING)
        }

        override fun onStartFailure(errorCode: Int) {
          Log.e(TAG, "Advertising fallito: $errorCode (${advertiseErrorLabel(errorCode)})")
          showNotification(NotificationKind.FAILED)
          mainHandler.postDelayed({ stopSelf() }, 3_000)
        }
      }

    advertiser?.startAdvertising(settings, data, advertiseCallback)
  }

  private fun stopAdvertising() {
    val cb = advertiseCallback ?: return
    advertiser?.stopAdvertising(cb)
    advertiseCallback = null
  }

  private enum class NotificationKind {
    PREPARING,
    TRANSMITTING,
    BLUETOOTH_OFF,
    FAILED,
  }

  private fun showNotification(kind: NotificationKind) {
    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIFICATION_ID, buildNotification(kind))
  }

  private fun buildPreparingNotification(): Notification = buildNotification(NotificationKind.PREPARING)

  private fun buildNotification(kind: NotificationKind): Notification {
    val (titleRes, bodyRes) =
      when (kind) {
        NotificationKind.TRANSMITTING ->
          R.string.sos_beacon_notification_title to R.string.sos_beacon_notification_body
        NotificationKind.PREPARING ->
          R.string.sos_beacon_notification_title to R.string.sos_beacon_notification_preparing
        NotificationKind.BLUETOOTH_OFF ->
          R.string.sos_beacon_notification_title to R.string.sos_beacon_notification_bt_off
        NotificationKind.FAILED ->
          R.string.sos_beacon_notification_title to R.string.sos_beacon_notification_failed
      }
  return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(getString(titleRes))
      .setContentText(getString(bodyRes))
      .setContentIntent(openAppPendingIntent())
      .setOngoing(kind == NotificationKind.TRANSMITTING || kind == NotificationKind.PREPARING)
      .build()
  }

  private fun openAppPendingIntent(): PendingIntent =
    PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

  private fun ensureChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel =
      NotificationChannel(
        CHANNEL_ID,
        getString(R.string.sos_beacon_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      )
    (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
  }

  companion object {
    const val ACTION_START = "it.trentosmartmountain.SOS_BEACON_START"
    const val ACTION_STOP = "it.trentosmartmountain.SOS_BEACON_STOP"
    const val EXTRA_BEACON_INSTANCE_ID = "beaconInstanceId"

    private const val CHANNEL_ID = "sos_beacon"
    private const val NOTIFICATION_ID = 9102
    private const val TAG = "SosBeaconService"

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

    fun beaconInstanceIdToMajorMinor(hex: String): Pair<Int, Int> =
      SosBeaconProtocol.beaconInstanceIdToMajorMinor(hex)

    private fun advertiseErrorLabel(code: Int): String =
      when (code) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "dati troppo grandi"
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "troppi advertiser"
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "già attivo"
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "errore interno"
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "non supportato"
        else -> "codice $code"
      }
  }
}
