package it.trentosmartmountain.app.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.sync.EmergencyUploadScheduler
import it.trentosmartmountain.app.data.local.db.PendingEmergencyEntity
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.CreateEmergencyRequest
import it.trentosmartmountain.app.data.remote.dto.GeoPointDto
import it.trentosmartmountain.app.data.remote.dto.EmergencyResponse
import it.trentosmartmountain.app.data.remote.dto.PatchEmergencyRequest
import it.trentosmartmountain.app.data.remote.dto.SessionEmergenciesResponse
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

    suspend fun hasPendingEmergencies(): Boolean =
        withContext(Dispatchers.IO) {
            pendingDao.count() > 0
        }

    suspend fun createEmergency(
        sessionId: String,
        emergencyType: String,
        longitude: Double,
        latitude: Double,
        beaconInstanceId: String,
        idempotencyKey: String,
        beaconActive: Boolean = true,
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
                EmergencyUploadScheduler.enqueue(context.applicationContext)
                return@withContext Result.failure(OfflineEmergencyException())
            }
            postEmergency(
                sessionId,
                emergencyType,
                longitude,
                latitude,
                beaconInstanceId,
                idempotencyKey,
                beaconActive,
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
        beaconActive: Boolean = true,
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
                        beaconActive = beaconActive,
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

    suspend fun listSessionEmergencies(sessionId: String): Result<SessionEmergenciesResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getSessionEmergencies(sessionId)
                if (res.isSuccessful && res.body() != null) {
                    Result.success(res.body()!!)
                } else {
                    Result.failure(Exception(res.errorBody()?.string() ?: "HTTP ${res.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getEmergency(emergencyId: String): Result<EmergencyResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getEmergency(emergencyId)
                if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
                else Result.failure(Exception(res.errorBody()?.string() ?: "HTTP ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun cancelEmergency(emergencyId: String, reason: String): Result<EmergencyResponse> =
        patchEmergency(emergencyId, PatchEmergencyRequest(action = "cancel", reason = reason))

    suspend fun dismissEmergency(emergencyId: String): Result<EmergencyResponse> =
        patchEmergency(emergencyId, PatchEmergencyRequest(action = "dismiss"))

    suspend fun shareEmergencyWithGroup(emergencyId: String): Result<EmergencyResponse> =
        patchEmergency(emergencyId, PatchEmergencyRequest(action = "share_with_group"))

    suspend fun unshareEmergencyWithGroup(emergencyId: String): Result<EmergencyResponse> =
        patchEmergency(emergencyId, PatchEmergencyRequest(action = "unshare_with_group"))

    suspend fun ackEmergency(emergencyId: String): Result<EmergencyResponse> =
        patchEmergency(emergencyId, PatchEmergencyRequest(action = "ack"))

    private suspend fun patchEmergency(
        emergencyId: String,
        body: PatchEmergencyRequest,
    ): Result<EmergencyResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.patchEmergency(emergencyId, body)
                if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
                else Result.failure(Exception(res.errorBody()?.string() ?: "HTTP ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun hasPendingUpload(): Boolean =
        withContext(Dispatchers.IO) {
            pendingDao.count() > 0
        }
}

class OfflineEmergencyException : Exception("OFFLINE_QUEUED")
