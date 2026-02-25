package ai.jaak.kyc.data.local.dao

import androidx.room.*
import ai.jaak.kyc.data.local.entity.KycBlacklistEntity
import ai.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KycBlacklistDao {
    
    @Query("SELECT * FROM kyc_blacklist WHERE processId = :processId")
    suspend fun getBlacklistByProcessId(processId: String): List<KycBlacklistEntity>
    
    @Query("SELECT * FROM kyc_blacklist WHERE processId = :processId AND organization = :organization")
    suspend fun getBlacklistByProcessAndOrganization(processId: String, organization: String): KycBlacklistEntity?
    
    @Query("SELECT * FROM kyc_blacklist WHERE processId = :processId")
    fun getBlacklistByProcessIdFlow(processId: String): Flow<List<KycBlacklistEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlacklist(blacklist: KycBlacklistEntity)
    
    @Update
    suspend fun updateBlacklist(blacklist: KycBlacklistEntity)
    
    @Query("UPDATE kyc_blacklist SET status = :status, errorMessage = :errorMessage, retryCount = :retryCount WHERE processId = :processId AND organization = :organization")
    suspend fun updateBlacklistStatus(processId: String, organization: String, status: ServiceStatus, errorMessage: String?, retryCount: Int)
    
    @Query("UPDATE kyc_blacklist SET eventId = :eventId, responseId = :responseId, processTime = :processTime, result = :result, stateMessage = :stateMessage, foundInService = :foundInService, mustBeFound = :mustBeFound, status = :status, completedAt = :completedAt WHERE processId = :processId AND organization = :organization")
    suspend fun updateBlacklistResponse(
        processId: String, 
        organization: String,
        eventId: String,
        responseId: String,
        processTime: Double,
        result: String?,
        stateMessage: String,
        foundInService: Boolean,
        mustBeFound: Boolean,
        status: ServiceStatus,
        completedAt: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE kyc_blacklist SET syncedAt = :syncedAt WHERE processId = :processId AND organization = :organization")
    suspend fun updateBlacklistSyncTime(processId: String, organization: String, syncedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM kyc_blacklist WHERE processId = :processId")
    suspend fun deleteBlacklistByProcessId(processId: String)
    
    @Query("DELETE FROM kyc_blacklist WHERE processId = :processId AND organization = :organization")
    suspend fun deleteBlacklistByProcessAndOrganization(processId: String, organization: String)
    
    @Query("SELECT * FROM kyc_blacklist WHERE status = :status")
    suspend fun getBlacklistByStatus(status: ServiceStatus): List<KycBlacklistEntity>
    
    @Query("SELECT COUNT(*) FROM kyc_blacklist WHERE processId = :processId AND status = 'PENDING'")
    suspend fun getPendingBlacklistCount(processId: String): Int
}