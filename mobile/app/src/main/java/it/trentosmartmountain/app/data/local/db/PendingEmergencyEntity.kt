package it.trentosmartmountain.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SOS in attesa di upload quando il dispositivo è offline.
 * Il beacon BLE è già attivo sul dispositivo; il POST avviene al ripristino della rete.
 */
@Entity(tableName = "pending_emergencies")
data class PendingEmergencyEntity(
    @PrimaryKey val idempotencyKey: String,
    val sessionId: String,
    val emergencyType: String,
    val longitude: Double,
    val latitude: Double,
    val beaconInstanceId: String,
    val createdAtMs: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,
)
