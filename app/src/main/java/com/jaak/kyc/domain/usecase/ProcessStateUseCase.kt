package com.jaak.kyc.domain.usecase

import com.jaak.kyc.data.local.entity.KycServiceType
import com.jaak.kyc.data.local.entity.ServiceExecutionState
import com.jaak.kyc.domain.service.ProcessStateManager
import com.jaak.kyc.domain.service.ProcessProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProcessStateUseCase @Inject constructor(
    private val processStateManager: ProcessStateManager
) {
    
    suspend fun initializeProcess(processId: String) {
        processStateManager.initializeProcessStates(processId)
    }
    
    suspend fun canExecuteService(processId: String, serviceType: KycServiceType): Boolean {
        return processStateManager.canExecuteService(processId, serviceType)
    }
    
    suspend fun startService(
        processId: String, 
        serviceType: KycServiceType, 
        isOnlineExecution: Boolean
    ): Boolean {
        return processStateManager.startServiceExecution(processId, serviceType, isOnlineExecution)
    }
    
    suspend fun completeService(
        processId: String,
        serviceType: KycServiceType,
        isSuccessful: Boolean,
        errorMessage: String? = null,
        errorCode: String? = null
    ) {
        processStateManager.completeServiceExecution(
            processId, serviceType, isSuccessful, errorMessage, errorCode
        )
    }
    
    suspend fun retryService(processId: String, serviceType: KycServiceType): Boolean {
        return processStateManager.retryFailedService(processId, serviceType)
    }
    
    suspend fun syncService(processId: String, serviceType: KycServiceType): Boolean {
        return processStateManager.syncService(processId, serviceType)
    }
    
    suspend fun getProcessProgress(processId: String): ProcessProgress {
        return processStateManager.getProcessProgress(processId)
    }
    
    suspend fun getNextExecutableService(processId: String): KycServiceType? {
        return processStateManager.getNextExecutableService(processId)
    }
    
    fun getServiceStatesFlow(processId: String): Flow<List<ServiceExecutionState>> {
        return processStateManager.getServiceStatesFlow(processId)
    }
    
    suspend fun markServiceForSync(processId: String, serviceType: KycServiceType) {
        processStateManager.markServiceForSync(processId, serviceType)
    }
    
    suspend fun executeServiceWithStateManagement(
        processId: String,
        serviceType: KycServiceType,
        isOnlineMode: Boolean,
        serviceExecution: suspend () -> Result<Unit>
    ): Result<Unit> {
        return try {
            if (!canExecuteService(processId, serviceType)) {
                return Result.failure(IllegalStateException("Service cannot be executed at this time"))
            }
            
            if (!startService(processId, serviceType, isOnlineMode)) {
                return Result.failure(IllegalStateException("Failed to start service execution"))
            }
            
            val result = serviceExecution()
            
            if (result.isSuccess) {
                completeService(processId, serviceType, true)
                if (!isOnlineMode) {
                    markServiceForSync(processId, serviceType)
                }
            } else {
                val error = result.exceptionOrNull()
                completeService(
                    processId, 
                    serviceType, 
                    false, 
                    error?.message, 
                    error?.javaClass?.simpleName
                )
            }
            
            result
        } catch (e: Exception) {
            completeService(processId, serviceType, false, e.message, e.javaClass.simpleName)
            Result.failure(e)
        }
    }
}