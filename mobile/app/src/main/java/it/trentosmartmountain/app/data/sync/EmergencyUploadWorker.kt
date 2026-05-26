package it.trentosmartmountain.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.trentosmartmountain.app.repository.EmergencyRepository

/**
 * Carica in background le emergenze in coda Room quando torna la connettività.
 */
class EmergencyUploadWorker(
  context: Context,
  params: WorkerParameters,
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    val repo = EmergencyRepository(applicationContext)
    if (!repo.isNetworkAvailable()) return Result.retry()
    repo.flushPendingQueue()
    return if (repo.hasPendingEmergencies()) Result.retry() else Result.success()
  }

  companion object {
    const val UNIQUE_WORK_NAME = "emergency_upload_queue"
  }
}
