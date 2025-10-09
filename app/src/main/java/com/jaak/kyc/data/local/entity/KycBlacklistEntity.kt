package com.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "kyc_blacklist",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KycBlacklistEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val processId: String,
    val organization: String, // "ine", "interpol", "ofac", "renapo", "sat"
    val service: String, // "identification", "criminal", "sanctions", "population", "fiscal"
    
    // Request data
    val requestPayload: String, // JSON del payload completo
    
    // Response data (nullable para modo offline)
    val eventId: String? = null,
    val responseId: String? = null,
    val processTime: Double? = null,
    val result: String? = null, // JSON del result
    val stateMessage: String? = null,
    val foundInService: Boolean? = null,
    val mustBeFound: Boolean? = null,
    
    // Status tracking
    val status: ServiceStatus = ServiceStatus.PENDING,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncedAt: Long? = null
)