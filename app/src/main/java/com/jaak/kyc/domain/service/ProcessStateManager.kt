package com.jaak.kyc.domain.service

import com.jaak.kyc.data.local.dao.ServiceExecutionStateDao
import com.jaak.kyc.data.local.dao.KycProcessDao
import com.jaak.kyc.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessStateManager @Inject constructor(
    private val serviceStateDao: ServiceExecutionStateDao,
    private val processDao: KycProcessDao
) {
    
    suspend fun initializeProcessStates(processId: String) {
        val existingStates = serviceStateDao.getServiceStatesForProcess(processId)
        if (existingStates.isEmpty()) {
            val initialStates = KycServiceType.getExecutionOrder().map { serviceType ->
                ServiceExecutionState(
                    processId = processId,
                    serviceName = serviceType,
                    executionOrder = serviceType.order,
                    status = ServiceStatus.PENDING,
                    requiredPrecedingServices = getPrecedingServices(serviceType),
                    canExecuteOffline = canServiceExecuteOffline(serviceType)
                )
            }
            serviceStateDao.insertServiceStates(initialStates)
        }
    }
    
    suspend fun canExecuteService(processId: String, serviceType: KycServiceType): Boolean {
        val serviceState = serviceStateDao.getServiceState(processId, serviceType)
            ?: return false
            
        if (serviceState.status != ServiceStatus.PENDING) {
            return false
        }
        
        val blockingServices = serviceStateDao.getBlockingServices(
            processId = processId,
            currentOrder = serviceType.order
        )
        
        return blockingServices.isEmpty()
    }
    
    suspend fun startServiceExecution(
        processId: String, 
        serviceType: KycServiceType, 
        isOnlineExecution: Boolean
    ): Boolean {
        if (!canExecuteService(processId, serviceType)) {
            return false
        }
        
        val currentState = serviceStateDao.getServiceState(processId, serviceType)
            ?: return false
            
        val updatedState = currentState.copy(
            status = ServiceStatus.RETRYING,
            startedAt = System.currentTimeMillis(),
            isOnlineExecution = isOnlineExecution,
            errorMessage = null,
            errorCode = null
        )
        
        serviceStateDao.updateServiceState(updatedState)
        updateOverallProcessStatus(processId)
        return true
    }
    
    suspend fun completeServiceExecution(
        processId: String,
        serviceType: KycServiceType,
        isSuccessful: Boolean,
        errorMessage: String? = null,
        errorCode: String? = null
    ) {
        val currentState = serviceStateDao.getServiceState(processId, serviceType)
            ?: return
            
        val now = System.currentTimeMillis()
        val executionDuration = currentState.startedAt?.let { now - it }
        
        val updatedState = if (isSuccessful) {
            currentState.copy(
                status = ServiceStatus.COMPLETED,
                completedAt = now,
                executionDurationMs = executionDuration,
                requiresSync = !currentState.isOnlineExecution,
                errorMessage = null,
                errorCode = null
            )
        } else {
            val shouldRetry = currentState.retryCount < currentState.maxRetries
            val nextRetryTime = if (shouldRetry) {
                now + calculateRetryDelay(currentState.retryCount)
            } else null
            
            currentState.copy(
                status = if (shouldRetry) ServiceStatus.FAILED else ServiceStatus.FAILED,
                errorMessage = errorMessage,
                errorCode = errorCode,
                retryCount = currentState.retryCount + 1,
                nextRetryAt = nextRetryTime
            )
        }
        
        serviceStateDao.updateServiceState(updatedState)
        updateOverallProcessStatus(processId)
    }
    
    suspend fun retryFailedService(processId: String, serviceType: KycServiceType): Boolean {
        val currentState = serviceStateDao.getServiceState(processId, serviceType)
            ?: return false
            
        if (currentState.status != ServiceStatus.FAILED || 
            currentState.retryCount >= currentState.maxRetries ||
            (currentState.nextRetryAt != null && currentState.nextRetryAt > System.currentTimeMillis())) {
            return false
        }
        
        val updatedState = currentState.copy(
            status = ServiceStatus.RETRYING,
            startedAt = System.currentTimeMillis(),
            errorMessage = null,
            errorCode = null
        )
        
        serviceStateDao.updateServiceState(updatedState)
        return true
    }
    
    suspend fun markServiceForSync(processId: String, serviceType: KycServiceType) {
        serviceStateDao.getServiceState(processId, serviceType)?.let { currentState ->
            val updatedState = currentState.copy(requiresSync = true)
            serviceStateDao.updateServiceState(updatedState)
        }
        updateOverallProcessStatus(processId)
    }
    
    suspend fun syncService(processId: String, serviceType: KycServiceType): Boolean {
        val currentState = serviceStateDao.getServiceState(processId, serviceType)
            ?: return false
            
        if (!currentState.requiresSync || currentState.status != ServiceStatus.COMPLETED) {
            return false
        }
        
        try {
            serviceStateDao.recordSyncAttempt(currentState.id)
            
            val updatedState = currentState.copy(
                status = ServiceStatus.SYNCED,
                requiresSync = false,
                syncError = null
            )
            
            serviceStateDao.updateServiceState(updatedState)
            updateOverallProcessStatus(processId)
            return true
            
        } catch (e: Exception) {
            serviceStateDao.recordSyncAttempt(currentState.id, error = e.message)
            return false
        }
    }
    
    suspend fun getProcessProgress(processId: String): ProcessProgress {
        val serviceStates = serviceStateDao.getServiceStatesForProcess(processId)
        val totalServices = serviceStates.size
        val completedServices = serviceStates.count { 
            it.status == ServiceStatus.COMPLETED || it.status == ServiceStatus.SYNCED 
        }
        val failedServices = serviceStates.count { it.status == ServiceStatus.FAILED }
        val pendingSync = serviceStates.count { it.requiresSync }
        
        return ProcessProgress(
            totalServices = totalServices,
            completedServices = completedServices,
            failedServices = failedServices,
            pendingSync = pendingSync,
            progressPercentage = if (totalServices > 0) (completedServices * 100) / totalServices else 0
        )
    }
    
    fun getServiceStatesFlow(processId: String): Flow<List<ServiceExecutionState>> {
        return serviceStateDao.getServiceStatesForProcessFlow(processId)
    }
    
    suspend fun getNextExecutableService(processId: String): KycServiceType? {
        val serviceStates = serviceStateDao.getServiceStatesForProcess(processId)
        
        return serviceStates
            .filter { it.status == ServiceStatus.PENDING }
            .minByOrNull { it.executionOrder }
            ?.let { nextService ->
                if (canExecuteService(processId, nextService.serviceName)) {
                    nextService.serviceName
                } else null
            }
    }
    
    private suspend fun updateOverallProcessStatus(processId: String) {
        val progress = getProcessProgress(processId)
        val process = processDao.getProcessById(processId) ?: return
        
        val newStatus = when {
            progress.failedServices > 0 && progress.completedServices == 0 -> KycProcessStatus.FAILED
            progress.completedServices == progress.totalServices && progress.pendingSync == 0 -> KycProcessStatus.COMPLETED
            progress.completedServices == progress.totalServices && progress.pendingSync > 0 -> KycProcessStatus.COMPLETED_OFFLINE
            progress.completedServices > 0 -> KycProcessStatus.IN_PROGRESS
            else -> KycProcessStatus.PENDING
        }
        
        if (process.overallStatus != newStatus) {
            val updatedProcess = process.copy(
                overallStatus = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            processDao.updateProcess(updatedProcess)
        }
    }
    
    private fun getPrecedingServices(serviceType: KycServiceType): List<KycServiceType> {
        return KycServiceType.getExecutionOrder()
            .filter { it.order < serviceType.order }
    }
    
    private fun canServiceExecuteOffline(serviceType: KycServiceType): Boolean {
        return when (serviceType) {
            KycServiceType.SESSION -> false // Requires server interaction
            KycServiceType.VERIFY -> true  // Can capture and store
            KycServiceType.OCR -> true     // Can process locally
            KycServiceType.LIVENESS -> true // Can capture and store
            KycServiceType.OTO_VERIFY -> true // Can process locally
            KycServiceType.FINISH -> false // Requires server confirmation
        }
    }
    
    private fun calculateRetryDelay(retryCount: Int): Long {
        return (1000L * (1 shl retryCount)).coerceAtMost(300000L) // Exponential backoff, max 5 minutes
    }
}

data class ProcessProgress(
    val totalServices: Int,
    val completedServices: Int,
    val failedServices: Int,
    val pendingSync: Int,
    val progressPercentage: Int
)