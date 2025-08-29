package com.jaak.kyc.data.local.dao

import androidx.room.*
import com.jaak.kyc.data.local.entity.KycSessionEntity
import com.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KycSessionDao {
    
    @Query("SELECT * FROM kyc_sessions WHERE processId = :processId")
    suspend fun getSessionByProcessId(processId: String): KycSessionEntity?
    
    @Query("SELECT * FROM kyc_sessions WHERE processId = :processId")
    fun getSessionByProcessIdFlow(processId: String): Flow<KycSessionEntity?>
    
    @Query("SELECT * FROM kyc_sessions WHERE status = :status ORDER BY createdAt DESC")
    fun getSessionsByStatus(status: ServiceStatus): Flow<List<KycSessionEntity>>
    
    @Query("SELECT * FROM kyc_sessions WHERE status = :status AND syncedAt IS NULL ORDER BY createdAt DESC")
    fun getUnsyncedSessionsByStatus(status: ServiceStatus): Flow<List<KycSessionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: KycSessionEntity): Long
    
    @Update
    suspend fun updateSession(session: KycSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: KycSessionEntity)
    
    @Query("DELETE FROM kyc_sessions WHERE processId = :processId")
    suspend fun deleteSessionByProcessId(processId: String)
    
    @Query("UPDATE kyc_sessions SET status = :status, errorMessage = :errorMessage, retryCount = :retryCount WHERE processId = :processId")
    suspend fun updateSessionStatus(processId: String, status: ServiceStatus, errorMessage: String?, retryCount: Int)
    
    @Query("UPDATE kyc_sessions SET accessToken = :accessToken, step = :step, sessionId = :sessionId, assets = :assets, document = :document, status = :status, completedAt = :completedAt WHERE processId = :processId")
    suspend fun updateSessionResponse(
        processId: String, 
        accessToken: String, 
        step: Int, 
        sessionId: String, 
        assets: String?, 
        document: String?, 
        status: ServiceStatus, 
        completedAt: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE kyc_sessions SET syncedAt = :syncedAt WHERE processId = :processId")
    suspend fun markAsSynced(processId: String, syncedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE kyc_sessions SET retryCount = retryCount + 1 WHERE processId = :processId")
    suspend fun incrementRetryCount(processId: String)
}