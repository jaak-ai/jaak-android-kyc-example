package ai.jaak.kyc.data.local.dao

import androidx.room.*
import ai.jaak.kyc.data.local.entity.KycOcrEntity
import ai.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KycOcrDao {
    
    @Query("SELECT * FROM kyc_ocr WHERE processId = :processId")
    suspend fun getOcrByProcessId(processId: String): KycOcrEntity?
    
    @Query("SELECT * FROM kyc_ocr WHERE processId = :processId")
    fun getOcrByProcessIdFlow(processId: String): Flow<KycOcrEntity?>
    
    @Query("SELECT * FROM kyc_ocr WHERE serviceStatus = :status ORDER BY createdAt DESC")
    fun getOcrsByStatus(status: ServiceStatus): Flow<List<KycOcrEntity>>
    
    @Query("SELECT * FROM kyc_ocr WHERE serviceStatus = :status AND syncedAt IS NULL ORDER BY createdAt DESC")
    fun getUnsyncedOcrsByStatus(status: ServiceStatus): Flow<List<KycOcrEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcr(ocr: KycOcrEntity): Long
    
    @Update
    suspend fun updateOcr(ocr: KycOcrEntity)
    
    @Delete
    suspend fun deleteOcr(ocr: KycOcrEntity)
    
    @Query("DELETE FROM kyc_ocr WHERE processId = :processId")
    suspend fun deleteOcrByProcessId(processId: String)
    
    @Query("UPDATE kyc_ocr SET serviceStatus = :status, errorMessage = :errorMessage, retryCount = :retryCount WHERE processId = :processId")
    suspend fun updateOcrStatus(processId: String, status: ServiceStatus, errorMessage: String?, retryCount: Int)
    
    @Query("UPDATE kyc_ocr SET eventId = :eventId, requestId = :requestId, status = :status, message = :message, documentType = :documentType, documentData = :documentData, documentMetadata = :documentMetadata, processingTime = :processingTime, responseState = :responseState, serviceStatus = :serviceStatus, completedAt = :completedAt WHERE processId = :processId")
    suspend fun updateOcrResponse(
        processId: String,
        eventId: String?,
        requestId: String?,
        status: Boolean?,
        message: String?,
        documentType: String?,
        documentData: String?,
        documentMetadata: String?,
        processingTime: String?,
        responseState: String?,
        serviceStatus: ServiceStatus,
        completedAt: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE kyc_ocr SET syncedAt = :syncedAt WHERE processId = :processId")
    suspend fun markAsSynced(processId: String, syncedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE kyc_ocr SET retryCount = retryCount + 1 WHERE processId = :processId")
    suspend fun incrementRetryCount(processId: String)
    
    // Size monitoring for large base64 images
    @Query("SELECT LENGTH(documentFront) + LENGTH(documentBack) as totalSize FROM kyc_ocr WHERE processId = :processId")
    suspend fun getDocumentDataSize(processId: String): Long?
}