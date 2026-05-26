package it.trentosmartmountain.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import it.trentosmartmountain.app.R

/**
 * Notifiche locali quando il poll rileva un nuovo SOS (fallback senza FCM configurato).
 */
object SosNotificationHelper {
  private const val CHANNEL_ID = "sos_incoming"
  private const val NOTIFICATION_ID = 9201

  fun showIncomingSos(context: Context, senderName: String) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          context.getString(R.string.sos_incoming_channel_name),
          NotificationManager.IMPORTANCE_HIGH,
        )
      nm.createNotificationChannel(channel)
    }
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(context.getString(R.string.sos_incoming_notification_title))
        .setContentText(
          context.getString(R.string.sos_incoming_notification_body, senderName),
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    nm.notify(NOTIFICATION_ID, notification)
  }
}
