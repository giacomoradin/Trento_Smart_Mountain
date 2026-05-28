package it.trentosmartmountain.app.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object EmergencyUploadScheduler {
  fun enqueue(context: Context) {
    val request =
      OneTimeWorkRequestBuilder<EmergencyUploadWorker>()
        .setConstraints(
          Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build(),
        )
        .build()
    WorkManager.getInstance(context)
      .enqueueUniqueWork(
        EmergencyUploadWorker.UNIQUE_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        request,
      )
  }
}
