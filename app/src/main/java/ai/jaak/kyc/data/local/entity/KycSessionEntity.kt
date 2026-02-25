package ai.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "kyc_sessions",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KycSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val processId: String,
    
    // Request data
    val shortKey: String,
    val originDevice: String = "Android",
    
    // Response data (when available)
    val accessToken: String? = null,
    val step: Int? = null,
    val sessionId: String? = null,
    val assets: String? = null, // JSON string
    val document: String? = null,
    
    // Status tracking
    val status: ServiceStatus = ServiceStatus.PENDING,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncedAt: Long? = null
)