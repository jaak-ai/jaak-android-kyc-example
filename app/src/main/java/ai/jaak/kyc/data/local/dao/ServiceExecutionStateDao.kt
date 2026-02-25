package ai.jaak.kyc.data.local.dao

import androidx.room.*
import ai.jaak.kyc.data.local.entity.KycServiceType
import ai.jaak.kyc.data.local.entity.ServiceExecutionState
import ai.jaak.kyc.data.local.entity.ServiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceExecutionStateDao {
    
    @Query("SELECT * FROM service_execution_states WHERE processId = :processId ORDER BY executionOrder")
    suspend fun getServiceStatesForProcess(processId: String): List<ServiceExecutionState>
    
    @Query("SELECT * FROM service_execution_states WHERE processId = :processId ORDER BY executionOrder")
    fun getServiceStatesForProcessFlow(processId: String): Flow<List<ServiceExecutionState>>
    
    @Query("SELECT * FROM service_execution_states WHERE processId = :processId AND serviceName = :serviceName")
    suspend fun getServiceState(processId: String, serviceName: KycServiceType): ServiceExecutionState?
    
    @Query("SELECT * FROM service_execution_states WHERE processId = :processId AND serviceName = :serviceName")
    fun getServiceStateFlow(processId: String, serviceName: KycServiceType): Flow<ServiceExecutionState?>
    
    @Query("SELECT * FROM service_execution_states WHERE status = :status")
    suspend fun getServicesByStatus(status: ServiceStatus): List<ServiceExecutionState>
    
    @Query("SELECT * FROM service_execution_states WHERE requiresSync = 1")
    suspend fun getServicesRequiringSync(): List<ServiceExecutionState>
    
    @Query("SELECT * FROM service_execution_states WHERE status = :status AND retryCount < maxRetries AND (nextRetryAt IS NULL OR nextRetryAt <= :currentTime)")
    suspend fun getServicesReadyForRetry(status: ServiceStatus = ServiceStatus.FAILED, currentTime: Long): List<ServiceExecutionState>
    
    @Query("SELECT * FROM service_execution_states WHERE processId = :processId AND status IN (:statuses)")
    suspend fun getServicesByStatuses(processId: String, statuses: List<ServiceStatus>): List<ServiceExecutionState>
    
    @Query("SELECT COUNT(*) FROM service_execution_states WHERE processId = :processId AND status = :status")
    suspend fun countServicesByStatus(processId: String, status: ServiceStatus): Int
    
    @Query("SELECT COUNT(*) FROM service_execution_states WHERE processId = :processId")
    suspend fun getTotalServicesCount(processId: String): Int
    
    @Query("SELECT * FROM service_execution_states WHERE processId = :processId AND executionOrder < :currentOrder AND status != :requiredStatus")
    suspend fun getBlockingServices(
        processId: String, 
        currentOrder: Int, 
        requiredStatus: ServiceStatus = ServiceStatus.COMPLETED
    ): List<ServiceExecutionState>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceState(state: ServiceExecutionState)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceStates(states: List<ServiceExecutionState>)
    
    @Update
    suspend fun updateServiceState(state: ServiceExecutionState)
    
    @Update
    suspend fun updateServiceStates(states: List<ServiceExecutionState>)
    
    @Query("UPDATE service_execution_states SET status = :status, completedAt = :completedAt WHERE id = :stateId")
    suspend fun updateServiceStatus(stateId: String, status: ServiceStatus, completedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE service_execution_states SET retryCount = retryCount + 1, nextRetryAt = :nextRetryAt, errorMessage = :errorMessage WHERE id = :stateId")
    suspend fun incrementRetryCount(stateId: String, nextRetryAt: Long, errorMessage: String)
    
    @Query("UPDATE service_execution_states SET requiresSync = :requiresSync WHERE id = :stateId")
    suspend fun updateSyncRequirement(stateId: String, requiresSync: Boolean)
    
    @Query("UPDATE service_execution_states SET syncAttempts = syncAttempts + 1, lastSyncAttempt = :timestamp, syncError = :error WHERE id = :stateId")
    suspend fun recordSyncAttempt(stateId: String, timestamp: Long = System.currentTimeMillis(), error: String? = null)
    
    @Delete
    suspend fun deleteServiceState(state: ServiceExecutionState)
    
    @Query("DELETE FROM service_execution_states WHERE processId = :processId")
    suspend fun deleteServiceStatesForProcess(processId: String)
    
    @Query("DELETE FROM service_execution_states WHERE processId = :processId AND serviceName = :serviceName")
    suspend fun deleteServiceState(processId: String, serviceName: KycServiceType)
}