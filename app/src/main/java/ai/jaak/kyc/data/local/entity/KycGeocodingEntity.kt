package ai.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "kyc_geocoding",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KycGeocodingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val processId: String,
    
    // Request data - Coordenadas capturadas
    val latitude: Double,
    val longitude: Double,
    
    // Response data (cuando esté disponible)
    val eventId: String? = null,
    val requestId: String? = null,
    
    // Address components
    val country: String? = null,
    val state: String? = null,
    val locality: String? = null,
    val subLocality: String? = null,
    val postalCode: String? = null,
    val route: String? = null,
    val streetNumber: String? = null,
    
    // Status tracking
    val status: ServiceStatus = ServiceStatus.PENDING,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncedAt: Long? = null
)