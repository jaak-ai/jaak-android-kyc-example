package com.jaak.kyc.data.local.dao

import androidx.room.*
import com.jaak.kyc.data.local.entity.KycFinishEntity
import com.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface KycFinishDao {
    
    @Query("SELECT * FROM kyc_finish WHERE processId = :processId")
    suspend fun getFinishByProcessId(processId: String): KycFinishEntity?
    
    @Query("SELECT * FROM kyc_finish WHERE processId = :processId")
    fun getFinishByProcessIdFlow(processId: String): Flow<KycFinishEntity?>
    
    @Query("SELECT * FROM kyc_finish WHERE status = :status ORDER BY createdAt DESC")
    fun getFinishesByStatus(status: ServiceStatus): Flow<List<KycFinishEntity>>
    
    @Query("SELECT * FROM kyc_finish WHERE status = :status AND syncedAt IS NULL ORDER BY createdAt DESC")
    fun getUnsyncedFinishesByStatus(status: ServiceStatus): Flow<List<KycFinishEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinish(finish: KycFinishEntity): Long
    
    @Update
    suspend fun updateFinish(finish: KycFinishEntity)
    
    @Delete
    suspend fun deleteFinish(finish: KycFinishEntity)
    
    @Query("DELETE FROM kyc_finish WHERE processId = :processId")
    suspend fun deleteFinishByProcessId(processId: String)
    
    @Query("UPDATE kyc_finish SET status = :status, errorMessage = :errorMessage, retryCount = :retryCount WHERE processId = :processId")
    suspend fun updateFinishStatus(processId: String, status: ServiceStatus, errorMessage: String?, retryCount: Int)
    
    @Query("UPDATE kyc_finish SET isFinished = :isFinished, completionMessage = :completionMessage, status = :status, completedAt = :completedAt WHERE processId = :processId")
    suspend fun updateFinishResponse(
        processId: String,
        isFinished: Boolean,
        completionMessage: String?,
        status: ServiceStatus,
        completedAt: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE kyc_finish SET syncedAt = :syncedAt WHERE processId = :processId")
    suspend fun markAsSynced(processId: String, syncedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE kyc_finish SET retryCount = retryCount + 1 WHERE processId = :processId")
    suspend fun incrementRetryCount(processId: String)
}