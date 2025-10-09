package com.jaak.kyc.data.local.dao

import androidx.room.*
import com.jaak.kyc.data.local.entity.KycProcessEntity
import com.jaak.kyc.data.local.entity.KycProcessWithDetails
import com.jaak.kyc.data.local.entity.KycProcessStatus
import com.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KycProcessDao {

    @Query("SELECT * FROM kyc_processes ORDER BY createdAt DESC")
    fun getAllProcesses(): Flow<List<KycProcessEntity>>

    @Query("SELECT * FROM kyc_processes WHERE id = :processId")
    suspend fun getProcessById(processId: String): KycProcessEntity?

    @Query("SELECT * FROM kyc_processes WHERE shortKey = :shortKey")
    suspend fun getProcessByShortKey(shortKey: String): KycProcessEntity?

    @Query("SELECT * FROM kyc_processes WHERE id = :processId")
    fun getProcessByIdFlow(processId: String): Flow<KycProcessEntity?>

    @Transaction
    @Query("SELECT * FROM kyc_processes WHERE id = :processId")
    suspend fun getProcessWithDetails(processId: String): KycProcessWithDetails?

    @Transaction
    @Query("SELECT * FROM kyc_processes ORDER BY createdAt DESC")
    fun getAllProcessesWithDetails(): Flow<List<KycProcessWithDetails>>

    @Query("SELECT * FROM kyc_processes WHERE overallStatus = :status ORDER BY createdAt DESC")
    fun getProcessesByStatus(status: KycProcessStatus): Flow<List<KycProcessEntity>>

    @Query("SELECT * FROM kyc_processes WHERE requiresSync = 1 ORDER BY createdAt DESC")
    fun getProcessesRequiringSync(): Flow<List<KycProcessEntity>>

    @Query("SELECT COUNT(*) FROM kyc_processes WHERE overallStatus = :status")
    suspend fun getCountByStatus(status: KycProcessStatus): Int

    @Query("SELECT COUNT(*) FROM kyc_processes WHERE requiresSync = 1")
    suspend fun getCountRequiringSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcess(process: KycProcessEntity): Long

    @Update
    suspend fun updateProcess(process: KycProcessEntity)

    @Delete
    suspend fun deleteProcess(process: KycProcessEntity)

    @Query("DELETE FROM kyc_processes WHERE id = :processId")
    suspend fun deleteProcessById(processId: String)

    @Query("DELETE FROM kyc_processes WHERE shortKey = :shortKey")
    suspend fun deleteProcessByShortKey(shortKey: String)

    @Query("DELETE FROM kyc_processes WHERE overallStatus = :status")
    suspend fun deleteProcessesByStatus(status: KycProcessStatus)

    // Update specific service status
    @Query("UPDATE kyc_processes SET sessionStatus = :status, sessionError = :error, sessionRetryCount = :retryCount, updatedAt = :timestamp WHERE id = :processId")
    suspend fun updateSessionStatus(processId: String, status: ServiceStatus, error: String?, retryCount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE kyc_processes SET verifyStatus = :status, verifyError = :error, verifyRetryCount = :retryCount, updatedAt = :timestamp WHERE id = :processId")
    suspend fun updateVerifyStatus(processId: String, status: ServiceStatus, error: String?, retryCount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE kyc_processes SET ocrStatus = :status, ocrError = :error, ocrRetryCount = :retryCount, updatedAt = :timestamp WHERE id = :processId")
    suspend fun updateOcrStatus(processId: String, status: ServiceStatus, error: String?, retryCount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE kyc_processes SET livenessStatus = :status, livenessError = :error, livenessRetryCount = :retryCount, updatedAt = :timestamp WHERE id = :processId")
    suspend fun updateLivenessStatus(processId: String, status: ServiceStatus, error: String?, retryCount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE kyc_processes SET otoVerifyStatus = :status, otoVerifyError = :error, otoVerifyRetryCount = :retryCount, updatedAt = :timestamp WHERE id = :processId")
    suspend fun updateOtoVerifyStatus(processId: String, status: ServiceStatus, error: String?, retryCount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE kyc_processes SET finishStatus = :status, finishError = :error, finishRetryCount = :retryCount, updatedAt = :timestamp WHERE id = :processId")
    suspend fun updateFinishStatus(processId: String, status: ServiceStatus, error: String?, retryCount: Int, timestamp: Long = System.currentTimeMillis())

    // Update overall status and sync tracking
    @Query("UPDATE kyc_processes SET overallStatus = :status, updatedAt = :timestamp WHERE id = :processId")
    suspend fun updateOverallStatus(processId: String, status: KycProcessStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE kyc_processes SET requiresSync = :requiresSync, syncAttempts = :syncAttempts, lastSyncAttempt = :lastAttempt, updatedAt = :timestamp WHERE id = :processId")
    suspend fun updateSyncStatus(processId: String, requiresSync: Boolean, syncAttempts: Int, lastAttempt: Long?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE kyc_processes SET sessionId = :sessionId, accessToken = :accessToken, updatedAt = :timestamp WHERE id = :processId")
    suspend fun updateSessionData(processId: String, sessionId: String?, accessToken: String?, timestamp: Long = System.currentTimeMillis())
}