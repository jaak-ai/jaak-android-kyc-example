package com.jaak.kyc.data.local.dao

import androidx.room.*
import com.jaak.kyc.data.local.entity.KycLivenessEntity
import com.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KycLivenessDao {
    
    @Query("SELECT * FROM kyc_liveness WHERE processId = :processId")
    suspend fun getLivenessByProcessId(processId: String): KycLivenessEntity?
    
    @Query("SELECT * FROM kyc_liveness WHERE processId = :processId")
    fun getLivenessByProcessIdFlow(processId: String): Flow<KycLivenessEntity?>
    
    @Query("SELECT * FROM kyc_liveness WHERE status = :status ORDER BY createdAt DESC")
    fun getLivenessByStatus(status: ServiceStatus): Flow<List<KycLivenessEntity>>
    
    @Query("SELECT * FROM kyc_liveness WHERE status = :status AND syncedAt IS NULL ORDER BY createdAt DESC")
    fun getUnsyncedLivenessByStatus(status: ServiceStatus): Flow<List<KycLivenessEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveness(liveness: KycLivenessEntity): Long
    
    @Update
    suspend fun updateLiveness(liveness: KycLivenessEntity)
    
    @Delete
    suspend fun deleteLiveness(liveness: KycLivenessEntity)
    
    @Query("DELETE FROM kyc_liveness WHERE processId = :processId")
    suspend fun deleteLivenessByProcessId(processId: String)
    
    @Query("UPDATE kyc_liveness SET status = :status, errorMessage = :errorMessage, retryCount = :retryCount WHERE processId = :processId")
    suspend fun updateLivenessStatus(processId: String, status: ServiceStatus, errorMessage: String?, retryCount: Int)
    
    @Query("UPDATE kyc_liveness SET eventId = :eventId, requestId = :requestId, processTime = :processTime, responseState = :responseState, bestFrame = :bestFrame, confidence = :confidence, isLive = :isLive, status = :status, completedAt = :completedAt WHERE processId = :processId")
    suspend fun updateLivenessResponse(
        processId: String,
        eventId: String?,
        requestId: String?,
        processTime: Long?,
        responseState: String?,
        bestFrame: String?,
        confidence: Float?,
        isLive: Boolean?,
        status: ServiceStatus,
        completedAt: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE kyc_liveness SET syncedAt = :syncedAt WHERE processId = :processId")
    suspend fun markAsSynced(processId: String, syncedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE kyc_liveness SET retryCount = retryCount + 1 WHERE processId = :processId")
    suspend fun incrementRetryCount(processId: String)
    
    // Size monitoring for large base64 video
    @Query("SELECT LENGTH(video) + COALESCE(LENGTH(bestFrame), 0) as totalSize FROM kyc_liveness WHERE processId = :processId")
    suspend fun getVideoDataSize(processId: String): Long?
}