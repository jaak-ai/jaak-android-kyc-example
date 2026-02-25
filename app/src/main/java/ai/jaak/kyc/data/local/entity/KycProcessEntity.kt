package ai.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "kyc_processes")
data class KycProcessEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val shortKey: String,
    val sessionId: String? = null,
    val accessToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val overallStatus: KycProcessStatus = KycProcessStatus.PENDING,
    
    // Individual service statuses
    val sessionStatus: ServiceStatus = ServiceStatus.PENDING,
    val verifyStatus: ServiceStatus = ServiceStatus.PENDING,
    val ocrStatus: ServiceStatus = ServiceStatus.PENDING,
    val livenessStatus: ServiceStatus = ServiceStatus.PENDING,
    val otoVerifyStatus: ServiceStatus = ServiceStatus.PENDING,
    val finishStatus: ServiceStatus = ServiceStatus.PENDING,
    
    // Error tracking
    val sessionError: String? = null,
    val verifyError: String? = null,
    val ocrError: String? = null,
    val livenessError: String? = null,
    val otoVerifyError: String? = null,
    val finishError: String? = null,
    
    // Retry counters
    val sessionRetryCount: Int = 0,
    val verifyRetryCount: Int = 0,
    val ocrRetryCount: Int = 0,
    val livenessRetryCount: Int = 0,
    val otoVerifyRetryCount: Int = 0,
    val finishRetryCount: Int = 0,
    
    // Network connectivity
    val requiresSync: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null
)

enum class KycProcessStatus {
    PENDING,           // Not started
    IN_PROGRESS,       // Some services completed
    COMPLETED_OFFLINE, // All done offline, needs sync
    SYNCING,           // Currently syncing
    COMPLETED,         // Fully completed and synced
    FAILED,            // Irrecoverable failure
    CANCELLED          // User cancelled
}

enum class ServiceStatus {
    PENDING,     // Not executed yet
    COMPLETED,   // Completed successfully (offline or online)
    SYNCED,      // Completed and synced to server
    FAILED,      // Failed execution
    RETRYING     // Currently retrying
}