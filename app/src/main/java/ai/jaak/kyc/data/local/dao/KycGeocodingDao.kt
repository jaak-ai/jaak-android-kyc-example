package ai.jaak.kyc.data.local.dao

import androidx.room.*
import ai.jaak.kyc.data.local.entity.KycGeocodingEntity
import ai.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KycGeocodingDao {
    
    @Query("SELECT * FROM kyc_geocoding WHERE processId = :processId")
    suspend fun getGeocodingByProcessId(processId: String): KycGeocodingEntity?
    
    @Query("SELECT * FROM kyc_geocoding WHERE processId = :processId")
    fun getGeocodingByProcessIdFlow(processId: String): Flow<KycGeocodingEntity?>
    
    @Query("SELECT * FROM kyc_geocoding WHERE status = :status")
    suspend fun getGeocodingByStatus(status: ServiceStatus): List<KycGeocodingEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeocoding(geocoding: KycGeocodingEntity)
    
    @Update
    suspend fun updateGeocoding(geocoding: KycGeocodingEntity)
    
    @Query("DELETE FROM kyc_geocoding WHERE processId = :processId")
    suspend fun deleteGeocodingByProcessId(processId: String)
    
    @Query("DELETE FROM kyc_geocoding")
    suspend fun deleteAllGeocoding()
    
    // Update methods for specific fields
    @Query("""
        UPDATE kyc_geocoding 
        SET status = :status, 
            errorMessage = :errorMessage, 
            retryCount = :retryCount,
            completedAt = :completedAt
        WHERE processId = :processId
    """)
    suspend fun updateGeocodingStatus(
        processId: String,
        status: ServiceStatus,
        errorMessage: String?,
        retryCount: Int,
        completedAt: Long? = null
    )
    
    @Query("""
        UPDATE kyc_geocoding 
        SET eventId = :eventId,
            requestId = :requestId,
            country = :country,
            state = :state,
            locality = :locality,
            subLocality = :subLocality,
            postalCode = :postalCode,
            route = :route,
            streetNumber = :streetNumber,
            status = :status,
            completedAt = :completedAt,
            syncedAt = :syncedAt
        WHERE processId = :processId
    """)
    suspend fun updateGeocodingResponse(
        processId: String,
        eventId: String,
        requestId: String,
        country: String?,
        state: String?,
        locality: String?,
        subLocality: String?,
        postalCode: String?,
        route: String?,
        streetNumber: String?,
        status: ServiceStatus,
        completedAt: Long,
        syncedAt: Long
    )
}