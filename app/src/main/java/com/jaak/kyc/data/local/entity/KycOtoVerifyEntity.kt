package com.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "kyc_oto_verify",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KycOtoVerifyEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val processId: String,
    
    // Request data - Images to compare (document vs face)
    val image1: String, // base64 - document image
    val image2: String, // base64 - face image from liveness
    
    // Response data (when available)
    val eventId: String? = null,
    val requestId: String? = null,
    val processTime: Long? = null,
    val responseState: String? = null, // JSON string of OtoVerifyState
    val matchConfidence: Float? = null,
    val isMatch: Boolean? = null,
    val imageQuality: String? = null, // JSON string of OtoVerifyImageQuality
    val imageDetails: String? = null, // JSON string of OtoVerifyImageDetails
    val accessories: String? = null, // JSON string of OtoVerifyAccessories
    val metadata: String? = null, // JSON string of OtoVerifyMetadata
    
    // Status tracking
    val status: ServiceStatus = ServiceStatus.PENDING,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncedAt: Long? = null
)