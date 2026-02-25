package ai.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "kyc_ocr",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KycOcrEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val processId: String,
    
    // Request data - Base64 images
    val documentFront: String, // base64
    val documentBack: String,  // base64
    
    // Response data (when available)
    val eventId: String? = null,
    val requestId: String? = null,
    val status: Boolean? = null,
    val message: String? = null,
    val documentType: String? = null, // JSON string of DocumentTypeExtraBoth
    val documentData: String? = null, // JSON string of DocumentDataExtraBoth
    val documentMetadata: String? = null,
    val processingTime: String? = null,
    val responseState: String? = null, // JSON string of DocumentStateExtraBoth
    val facePath: String? = null, // Path to saved face image file (from v4 API)
    val ocrResponseJson: String? = null, // Full OCR V4 response JSON (for blacklist services)

    // Status tracking
    val serviceStatus: ServiceStatus = ServiceStatus.PENDING,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncedAt: Long? = null
)