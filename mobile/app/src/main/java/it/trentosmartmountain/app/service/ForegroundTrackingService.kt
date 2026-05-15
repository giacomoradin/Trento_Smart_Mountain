package it.trentosmartmountain.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import it.trentosmartmountain.app.MainActivity
import it.trentosmartmountain.app.R
import it.trentosmartmountain.app.data.location.FusedLocationPublisher
import it.trentosmartmountain.app.data.location.TrackingLocationBus

/**
 * Mantiene il tracking GPS in foreground durante la registrazione (RF8).
 */
class ForegroundTrackingService : Service() {

  private lateinit var locationPublisher: FusedLocationPublisher

  override fun onCreate() {
    super.onCreate()
    locationPublisher = FusedLocationPublisher(this)
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopTracking()
        stopSelf()
        return START_NOT_STICKY
      }
      else -> startTracking()
    }
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    stopTracking()
    super.onDestroy()
  }

  private fun startTracking() {
    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
      )
    } else {
      @Suppress("DEPRECATION")
      startForeground(NOTIFICATION_ID, notification)
    }
    locationPublisher.start(
      intervalMs = RECORDING_INTERVAL_MS,
      minIntervalMs = RECORDING_MIN_INTERVAL_MS,
      onLocation = { TrackingLocationBus.emit(it) },
    )
  }

  private fun stopTracking() {
    locationPublisher.stop()
    stopForeground(STOP_FOREGROUND_REMOVE)
  }

  private fun buildNotification(): Notification {
    val openAppIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.tracking_notification_title))
      .setContentText(getString(R.string.tracking_notification_body))
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setOngoing(true)
      .setContentIntent(openAppIntent)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel =
      NotificationChannel(
        CHANNEL_ID,
        getString(R.string.tracking_notification_channel),
        NotificationManager.IMPORTANCE_LOW,
      )
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
  }

  companion object {
    const val ACTION_START = "it.trentosmartmountain.app.action.START_TRACKING"
    const val ACTION_STOP = "it.trentosmartmountain.app.action.STOP_TRACKING"

    private const val CHANNEL_ID = "tsm_tracking"
    private const val NOTIFICATION_ID = 42
    private const val RECORDING_INTERVAL_MS = 2_000L
    private const val RECORDING_MIN_INTERVAL_MS = 1_000L

    fun start(context: Context) {
      val intent =
        Intent(context, ForegroundTrackingService::class.java).apply {
          action = ACTION_START
        }
      context.startForegroundService(intent)
    }

    fun stop(context: Context) {
      val intent =
        Intent(context, ForegroundTrackingService::class.java).apply {
          action = ACTION_STOP
        }
      context.startService(intent)
    }
  }
}
