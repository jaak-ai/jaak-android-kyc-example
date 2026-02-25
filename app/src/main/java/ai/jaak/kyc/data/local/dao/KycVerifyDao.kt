package ai.jaak.kyc.data.local.dao

import androidx.room.*
import ai.jaak.kyc.data.local.entity.KycVerifyEntity
import ai.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KycVerifyDao {
    
    @Query("SELECT * FROM kyc_verify WHERE processId = :processId")
    suspend fun getVerifyByProcessId(processId: String): KycVerifyEntity?
    
    @Query("SELECT * FROM kyc_verify WHERE processId = :processId")
    fun getVerifyByProcessIdFlow(processId: String): Flow<KycVerifyEntity?>
    
    @Query("SELECT * FROM kyc_verify WHERE status = :status ORDER BY createdAt DESC")
    fun getVerifiesByStatus(status: ServiceStatus): Flow<List<KycVerifyEntity>>
    
    @Query("SELECT * FROM kyc_verify WHERE status = :status AND syncedAt IS NULL ORDER BY createdAt DESC")
    fun getUnsyncedVerifiesByStatus(status: ServiceStatus): Flow<List<KycVerifyEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerify(verify: KycVerifyEntity): Long
    
    @Update
    suspend fun updateVerify(verify: KycVerifyEntity)
    
    @Delete
    suspend fun deleteVerify(verify: KycVerifyEntity)
    
    @Query("DELETE FROM kyc_verify WHERE processId = :processId")
    suspend fun deleteVerifyByProcessId(processId: String)
    
    @Query("UPDATE kyc_verify SET status = :status, errorMessage = :errorMessage, retryCount = :retryCount WHERE processId = :processId")
    suspend fun updateVerifyStatus(processId: String, status: ServiceStatus, errorMessage: String?, retryCount: Int)
    
    @Query("UPDATE kyc_verify SET responseDocument = :responseDocument, responseDocumentType = :responseDocumentType, eventId = :eventId, processTime = :processTime, requestId = :requestId, responseState = :responseState, status = :status, completedAt = :completedAt WHERE processId = :processId")
    suspend fun updateVerifyResponse(
        processId: String,
        responseDocument: String?,
        responseDocumentType: Int?,
        eventId: String?,
        processTime: Long?,
        requestId: String?,
        responseState: String?,
        status: ServiceStatus,
        completedAt: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE kyc_verify SET syncedAt = :syncedAt WHERE processId = :processId")
    suspend fun markAsSynced(processId: String, syncedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE kyc_verify SET retryCount = retryCount + 1 WHERE processId = :processId")
    suspend fun incrementRetryCount(processId: String)
    
    // Size monitoring for large base64 images
    @Query("SELECT LENGTH(imageFront) + COALESCE(LENGTH(imageBack), 0) as totalSize FROM kyc_verify WHERE processId = :processId")
    suspend fun getImageDataSize(processId: String): Long?
}