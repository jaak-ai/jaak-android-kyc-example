package com.jaak.kyc.data.local.dao

import androidx.room.*
import com.jaak.kyc.data.local.entity.KycOtoVerifyEntity
import com.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KycOtoVerifyDao {
    
    @Query("SELECT * FROM kyc_oto_verify WHERE processId = :processId")
    suspend fun getOtoVerifyByProcessId(processId: String): KycOtoVerifyEntity?
    
    @Query("SELECT * FROM kyc_oto_verify WHERE processId = :processId")
    fun getOtoVerifyByProcessIdFlow(processId: String): Flow<KycOtoVerifyEntity?>
    
    @Query("SELECT * FROM kyc_oto_verify WHERE status = :status ORDER BY createdAt DESC")
    fun getOtoVerifiesByStatus(status: ServiceStatus): Flow<List<KycOtoVerifyEntity>>
    
    @Query("SELECT * FROM kyc_oto_verify WHERE status = :status AND syncedAt IS NULL ORDER BY createdAt DESC")
    fun getUnsyncedOtoVerifiesByStatus(status: ServiceStatus): Flow<List<KycOtoVerifyEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtoVerify(otoVerify: KycOtoVerifyEntity): Long
    
    @Update
    suspend fun updateOtoVerify(otoVerify: KycOtoVerifyEntity)
    
    @Delete
    suspend fun deleteOtoVerify(otoVerify: KycOtoVerifyEntity)
    
    @Query("DELETE FROM kyc_oto_verify WHERE processId = :processId")
    suspend fun deleteOtoVerifyByProcessId(processId: String)
    
    @Query("UPDATE kyc_oto_verify SET status = :status, errorMessage = :errorMessage, retryCount = :retryCount WHERE processId = :processId")
    suspend fun updateOtoVerifyStatus(processId: String, status: ServiceStatus, errorMessage: String?, retryCount: Int)
    
    @Query("UPDATE kyc_oto_verify SET eventId = :eventId, requestId = :requestId, processTime = :processTime, responseState = :responseState, matchConfidence = :matchConfidence, isMatch = :isMatch, imageQuality = :imageQuality, imageDetails = :imageDetails, accessories = :accessories, metadata = :metadata, status = :status, completedAt = :completedAt WHERE processId = :processId")
    suspend fun updateOtoVerifyResponse(
        processId: String,
        eventId: String?,
        requestId: String?,
        processTime: Long?,
        responseState: String?,
        matchConfidence: Float?,
        isMatch: Boolean?,
        imageQuality: String?,
        imageDetails: String?,
        accessories: String?,
        metadata: String?,
        status: ServiceStatus,
        completedAt: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE kyc_oto_verify SET syncedAt = :syncedAt WHERE processId = :processId")
    suspend fun markAsSynced(processId: String, syncedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE kyc_oto_verify SET retryCount = retryCount + 1 WHERE processId = :processId")
    suspend fun incrementRetryCount(processId: String)
    
    // Size monitoring for comparison images
    @Query("SELECT LENGTH(image1) + LENGTH(image2) as totalSize FROM kyc_oto_verify WHERE processId = :processId")
    suspend fun getComparisonDataSize(processId: String): Long?
}