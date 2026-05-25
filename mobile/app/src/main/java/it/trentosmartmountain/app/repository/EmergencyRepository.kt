package it.trentosmartmountain.app.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.local.db.PendingEmergencyEntity
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.CreateEmergencyRequest
import it.trentosmartmountain.app.data.remote.dto.GeoPointDto
import it.trentosmartmountain.app.data.remote.dto.PatchEmergencyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmergencyRepository(private val context: Context) {

    private val api get() = TsmApiClient.service()
    private val pendingDao get() = (context.applicationContext as TsmApplication).database.pendingEmergencyDao()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun createEmergency(
        sessionId: String,
        emergencyType: String,
        longitude: Double,
        latitude: Double,
        beaconInstanceId: String,
        idempotencyKey: String,
    ): Result<it.trentosmartmountain.app.data.remote.dto.EmergencyResponse> =
        withContext(Dispatchers.IO) {
            if (!isNetworkAvailable()) {
                pendingDao.upsert(
                    PendingEmergencyEntity(
                        idempotencyKey = idempotencyKey,
                        sessionId = sessionId,
                        emergencyType = emergencyType,
                        longitude = longitude,
                        latitude = latitude,
                        beaconInstanceId = beaconInstanceId,
                        createdAtMs = System.currentTimeMillis(),
                    ),
                )
                return@withContext Result.failure(OfflineEmergencyException())
            }
            postEmergency(
                sessionId,
                emergencyType,
                longitude,
                latitude,
                beaconInstanceId,
                idempotencyKey,
            )
        }

    suspend fun flushPendingQueue(): Int =
        withContext(Dispatchers.IO) {
            if (!isNetworkAvailable()) return@withContext 0
            var uploaded = 0
            for (pending in pendingDao.getAll()) {
                val result =
                    postEmergency(
                        pending.sessionId,
                        pending.emergencyType,
                        pending.longitude,
                        pending.latitude,
                        pending.beaconInstanceId,
                        pending.idempotencyKey,
                    )
                if (result.isSuccess) {
                    pendingDao.deleteByKey(pending.idempotencyKey)
                    uploaded++
                }
            }
            uploaded
        }

    private suspend fun postEmergency(
        sessionId: String,
        emergencyType: String,
        longitude: Double,
        latitude: Double,
        beaconInstanceId: String,
        idempotencyKey: String,
    ): Result<it.trentosmartmountain.app.data.remote.dto.EmergencyResponse> {
        return try {
            val response =
                api.createEmergency(
                    CreateEmergencyRequest(
                        sessionId = sessionId,
                        emergencyType = emergencyType,
                        coordinates = GeoPointDto(coordinates = listOf(longitude, latitude)),
                        beaconInstanceId = beaconInstanceId,
                        idempotencyKey = idempotencyKey,
                    ),
                )
            if (response.isSuccessful && response.body() != null) {
                pendingDao.deleteByKey(idempotencyKey)
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listSessionEmergencies(sessionId: String) =
        withContext(Dispatchers.IO) {
            api.getSessionEmergencies(sessionId)
        }

    suspend fun cancelEmergency(emergencyId: String, reason: String) =
        withContext(Dispatchers.IO) {
            api.patchEmergency(emergencyId, PatchEmergencyRequest(action = "cancel", reason = reason))
        }

    suspend fun hasPendingUpload(): Boolean =
        withContext(Dispatchers.IO) {
            pendingDao.count() > 0
        }
}

class OfflineEmergencyException : Exception("OFFLINE_QUEUED")
