package ai.jaak.kyc.data.local.dao

import androidx.room.*
import ai.jaak.kyc.data.local.entity.ErrorCategory
import ai.jaak.kyc.data.local.entity.ErrorSeverity
import ai.jaak.kyc.data.local.entity.KycServiceType
import ai.jaak.kyc.data.local.entity.ProcessErrorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessErrorDao {
    
    @Query("SELECT * FROM process_errors WHERE processId = :processId ORDER BY timestamp DESC")
    suspend fun getErrorsByProcessId(processId: String): List<ProcessErrorEntity>
    
    @Query("SELECT * FROM process_errors WHERE processId = :processId ORDER BY timestamp DESC")
    fun getErrorsByProcessIdFlow(processId: String): Flow<List<ProcessErrorEntity>>
    
    @Query("SELECT * FROM process_errors WHERE processId = :processId AND serviceType = :serviceType ORDER BY timestamp DESC")
    suspend fun getErrorsByProcessAndService(processId: String, serviceType: KycServiceType): List<ProcessErrorEntity>
    
    @Query("SELECT * FROM process_errors WHERE processId = :processId AND isResolved = 0 ORDER BY severity DESC, timestamp DESC")
    suspend fun getUnresolvedErrors(processId: String): List<ProcessErrorEntity>
    
    @Query("SELECT * FROM process_errors WHERE severity = :severity ORDER BY timestamp DESC")
    suspend fun getErrorsBySeverity(severity: ErrorSeverity): List<ProcessErrorEntity>
    
    @Query("SELECT * FROM process_errors WHERE errorCategory = :category ORDER BY timestamp DESC")
    suspend fun getErrorsByCategory(category: ErrorCategory): List<ProcessErrorEntity>
    
    @Query("SELECT * FROM process_errors WHERE isNetworkError = 1 AND isResolved = 0")
    suspend fun getUnresolvedNetworkErrors(): List<ProcessErrorEntity>
    
    @Query("SELECT * FROM process_errors WHERE retryCount < maxRetries AND nextRetryAt <= :currentTime")
    suspend fun getErrorsReadyForRetry(currentTime: Long = System.currentTimeMillis()): List<ProcessErrorEntity>
    
    @Query("SELECT * FROM process_errors WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getErrorsInTimeRange(startTime: Long, endTime: Long): List<ProcessErrorEntity>
    
    @Query("SELECT * FROM process_errors WHERE showToUser = 1 AND isResolved = 0 ORDER BY severity DESC, timestamp DESC")
    suspend fun getUserVisibleErrors(): List<ProcessErrorEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertError(error: ProcessErrorEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrors(errors: List<ProcessErrorEntity>)
    
    @Update
    suspend fun updateError(error: ProcessErrorEntity)
    
    @Query("UPDATE process_errors SET isResolved = 1, resolvedAt = :resolvedAt, resolutionAction = :action, resolutionNotes = :notes WHERE id = :errorId")
    suspend fun resolveError(
        errorId: String, 
        resolvedAt: Long = System.currentTimeMillis(), 
        action: String, 
        notes: String? = null
    )
    
    @Query("UPDATE process_errors SET retryCount = retryCount + 1, nextRetryAt = :nextRetryAt WHERE id = :errorId")
    suspend fun incrementRetryCount(errorId: String, nextRetryAt: Long)
    
    @Query("UPDATE process_errors SET userMessage = :message, showToUser = :showToUser WHERE id = :errorId")
    suspend fun updateUserMessage(errorId: String, message: String, showToUser: Boolean = true)
    
    @Delete
    suspend fun deleteError(error: ProcessErrorEntity)
    
    @Query("DELETE FROM process_errors WHERE processId = :processId")
    suspend fun deleteErrorsByProcessId(processId: String)
    
    @Query("DELETE FROM process_errors WHERE isResolved = 1 AND resolvedAt < :cutoffTime")
    suspend fun deleteResolvedErrorsOlderThan(cutoffTime: Long)
    
    @Query("DELETE FROM process_errors WHERE timestamp < :cutoffTime")
    suspend fun deleteErrorsOlderThan(cutoffTime: Long)
    
    // Analytics queries
    @Query("SELECT COUNT(*) FROM process_errors WHERE processId = :processId")
    suspend fun getErrorCountForProcess(processId: String): Int
    
    @Query("SELECT COUNT(*) FROM process_errors WHERE processId = :processId AND isResolved = 0")
    suspend fun getUnresolvedErrorCountForProcess(processId: String): Int
    
    @Query("SELECT COUNT(*) FROM process_errors WHERE processId = :processId AND errorCategory = :category")
    suspend fun getErrorCountByCategory(processId: String, category: ErrorCategory): Int
    
    @Query("SELECT COUNT(*) FROM process_errors WHERE processId = :processId AND serviceType = :serviceType")
    suspend fun getErrorCountByService(processId: String, serviceType: KycServiceType): Int
    
    @Query("SELECT AVG(retryCount) FROM process_errors WHERE processId = :processId AND isResolved = 1")
    suspend fun getAverageRetryCount(processId: String): Double
}