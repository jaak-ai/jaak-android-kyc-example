package com.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "kyc_verify",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KycVerifyEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val processId: String,
    
    // Request data - Base64 images (can be large ~15-25MB)
    val imageFront: String, // base64
    val imageBack: String?, // base64, nullable
    val documentType: Boolean,
    
    // Response data (when available)
    val responseDocument: String? = null, // JSON string of Document object
    val responseDocumentType: Int? = null,
    val eventId: String? = null,
    val processTime: Long? = null,
    val requestId: String? = null,
    val responseState: String? = null, // JSON string of State object
    
    // Status tracking
    val status: ServiceStatus = ServiceStatus.PENDING,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncedAt: Long? = null
)