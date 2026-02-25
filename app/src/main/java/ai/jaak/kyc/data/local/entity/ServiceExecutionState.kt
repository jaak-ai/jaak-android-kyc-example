package ai.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "service_execution_states",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["processId", "serviceName"], unique = true)]
)
data class ServiceExecutionState(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val processId: String,
    val serviceName: KycServiceType,
    val status: ServiceStatus = ServiceStatus.PENDING,
    val executionOrder: Int,
    
    // Execution details
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val executionDurationMs: Long? = null,
    val isOnlineExecution: Boolean = false,
    
    // Error handling
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val nextRetryAt: Long? = null,
    
    // Dependencies
    val requiredPrecedingServices: List<KycServiceType> = emptyList(),
    val canExecuteOffline: Boolean = true,
    
    // Sync status
    val requiresSync: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null,
    val syncError: String? = null
)

enum class KycServiceType(val displayName: String, val order: Int) {
    SESSION("Session", 1),
    VERIFY("Document Verify", 2),
    OCR("OCR Processing", 3),
    LIVENESS("Liveness Detection", 4),
    OTO_VERIFY("Face Matching", 5),
    FINISH("Process Finish", 6);
    
    companion object {
        fun getByOrder(order: Int): KycServiceType? = values().find { it.order == order }
        fun getExecutionOrder(): List<KycServiceType> = values().sortedBy { it.order }
    }
}

data class ServiceTransition(
    val fromStatus: ServiceStatus,
    val toStatus: ServiceStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val isAutomated: Boolean = false
)