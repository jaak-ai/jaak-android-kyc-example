package ai.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "kyc_liveness",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KycLivenessEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val processId: String,
    
    // Request data - Video Base64 (can be large ~5-10MB)
    val video: String, // base64
    
    // Response data (when available)
    val eventId: String? = null,
    val requestId: String? = null,
    val processTime: Long? = null,
    val responseState: String? = null, // JSON string of LivenessState object
    val bestFrame: String? = null, // base64 of best frame
    val confidence: Float? = null,
    val isLive: Boolean? = null,
    
    // Status tracking
    val status: ServiceStatus = ServiceStatus.PENDING,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncedAt: Long? = null
)