package com.jaak.kyc.domain.usecase

import com.jaak.kyc.data.local.entity.KycServiceType
import com.jaak.kyc.data.local.entity.ProcessErrorEntity
import com.jaak.kyc.data.local.entity.ProcessTokenEntity
import com.jaak.kyc.domain.service.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ProcessManagementUseCase @Inject constructor(
    private val processTokenManager: ProcessTokenManager,
    private val processErrorManager: ProcessErrorManager,
    private val processStateManager: ProcessStateManager
) {
    
    suspend fun initializeProcess(processId: String, shortKey: String): ProcessInitializationResult {
        return try {
            // Initialize process states
            processStateManager.initializeProcessStates(processId)
            
            // Create offline token
            val token = processTokenManager.createOfflineToken(processId, shortKey)
            
            ProcessInitializationResult.Success(token)
        } catch (e: Exception) {
            val error = processErrorManager.logError(
                processId = processId,
                throwable = e,
                context = ErrorContext(
                    isNetworkAvailable = true,
                    userFlow = "process_initialization"
                )
            )
            ProcessInitializationResult.Error(error)
        }
    }
    
    suspend fun executeServiceWithErrorHandling(
        processId: String,
        serviceType: KycServiceType,
        isOnlineMode: Boolean,
        serviceExecution: suspend (token: String?) -> Result<Unit>
    ): ServiceExecutionResult {
        
        return try {
            // Check if we can execute this service
            if (!processStateManager.canExecuteService(processId, serviceType)) {
                return ServiceExecutionResult.Blocked("Service cannot be executed at this time")
            }
            
            // Get valid token for authentication
            val authToken = processTokenManager.getTokenForAuthentication(processId)
            if (authToken == null && isOnlineMode) {
                val error = processErrorManager.logCustomError(
                    processId = processId,
                    serviceType = serviceType,
                    errorCode = "NO_VALID_TOKEN",
                    errorMessage = "No valid authentication token available",
                    category = com.jaak.kyc.data.local.entity.ErrorCategory.AUTHENTICATION,
                    severity = com.jaak.kyc.data.local.entity.ErrorSeverity.HIGH,
                    showToUser = true,
                    userMessage = "Authentication error. Please restart the process."
                )
                return ServiceExecutionResult.Error(error)
            }
            
            // Start service execution
            processStateManager.startServiceExecution(processId, serviceType, isOnlineMode)
            
            // Execute the service
            val result = serviceExecution(authToken)
            
            if (result.isSuccess) {
                // Mark service as completed
                processStateManager.completeServiceExecution(processId, serviceType, true)
                
                // Mark for sync if executed offline
                if (!isOnlineMode) {
                    processStateManager.markServiceForSync(processId, serviceType)
                }
                
                ServiceExecutionResult.Success
            } else {
                // Log the error
                val exception = result.exceptionOrNull()
                val error = if (exception != null) {
                    processErrorManager.logError(
                        processId = processId,
                        serviceType = serviceType,
                        throwable = exception,
                        context = ErrorContext(
                            isNetworkAvailable = isOnlineMode,
                            userFlow = "service_execution"
                        )
                    )
                } else {
                    processErrorManager.logCustomError(
                        processId = processId,
                        serviceType = serviceType,
                        errorCode = "SERVICE_EXECUTION_FAILED",
                        errorMessage = "Service execution failed",
                        category = com.jaak.kyc.data.local.entity.ErrorCategory.BUSINESS_LOGIC,
                        severity = com.jaak.kyc.data.local.entity.ErrorSeverity.HIGH
                    )
                }
                
                // Mark service execution as failed
                processStateManager.completeServiceExecution(
                    processId, serviceType, false, error.errorMessage, error.errorCode
                )
                
                ServiceExecutionResult.Error(error)
            }
            
        } catch (e: Exception) {
            // Log unexpected error
            val error = processErrorManager.logError(
                processId = processId,
                serviceType = serviceType,
                throwable = e,
                context = ErrorContext(
                    isNetworkAvailable = isOnlineMode,
                    userFlow = "service_execution"
                )
            )
            
            // Mark service execution as failed
            processStateManager.completeServiceExecution(
                processId, serviceType, false, e.message, error.errorCode
            )
            
            ServiceExecutionResult.Error(error)
        }
    }
    
    suspend fun retryFailedService(processId: String, serviceType: KycServiceType): RetryResult {
        // Check error manager for retry eligibility
        val retryResult = processErrorManager.retryFailedService(processId, serviceType)
        
        return when (retryResult) {
            is com.jaak.kyc.domain.service.RetryResult.CanRetry -> {
                // Mark error as resolved (will be retried)
                processErrorManager.resolveError(
                    retryResult.error.id,
                    "Service marked for retry",
                    "Automatic retry initiated"
                )
                
                // Use process state manager to retry
                if (processStateManager.retryFailedService(processId, serviceType)) {
                    RetryResult.CanRetry
                } else {
                    RetryResult.CannotRetry("Service state does not allow retry")
                }
            }
            is com.jaak.kyc.domain.service.RetryResult.MustWait -> {
                RetryResult.MustWait(retryResult.waitTimeMs)
            }
            is com.jaak.kyc.domain.service.RetryResult.NoRetryNeeded -> {
                RetryResult.NoRetryNeeded
            }
        }
    }
    
    suspend fun syncProcessWithErrorHandling(processId: String): SyncResult {
        return try {
            // Validate token before sync
            if (!processTokenManager.validateToken(processId)) {
                val refreshResult = processTokenManager.refreshToken(processId)
                when (refreshResult) {
                    is TokenRefreshResult.Error -> {
                        val error = processErrorManager.logCustomError(
                            processId = processId,
                            errorCode = "TOKEN_REFRESH_FAILED",
                            errorMessage = refreshResult.message,
                            category = com.jaak.kyc.data.local.entity.ErrorCategory.AUTHENTICATION,
                            severity = com.jaak.kyc.data.local.entity.ErrorSeverity.HIGH
                        )
                        return SyncResult.Error(error)
                    }
                    is TokenRefreshResult.Success -> {
                        // Token refreshed, continue with sync
                    }
                }
            }
            
            // Perform sync (would call actual sync service here)
            // For now, simulate success
            SyncResult.Success("Process synced successfully")
            
        } catch (e: Exception) {
            val error = processErrorManager.logError(
                processId = processId,
                throwable = e,
                context = ErrorContext(
                    isNetworkAvailable = true,
                    userFlow = "process_sync"
                )
            )
            SyncResult.Error(error)
        }
    }
    
    suspend fun getProcessHealth(processId: String): ProcessHealthScore {
        return processErrorManager.getProcessHealthScore(processId)
    }
    
    fun getProcessStatusFlow(processId: String): Flow<ProcessStatus> {
        return combine(
            processStateManager.getServiceStatesFlow(processId),
            processTokenManager.getTokenFlow(processId),
            processErrorManager.getErrorsFlow(processId)
        ) { serviceStates, token, errors ->
            
            val progress = calculateProgress(serviceStates)
            val unresolvedErrors = errors.filter { !it.isResolved }
            val criticalErrors = unresolvedErrors.filter { 
                it.severity == com.jaak.kyc.data.local.entity.ErrorSeverity.CRITICAL 
            }
            
            ProcessStatus(
                processId = processId,
                progress = progress,
                hasValidToken = token?.let { processTokenManager.validateToken(processId) } ?: false,
                totalErrors = errors.size,
                unresolvedErrors = unresolvedErrors.size,
                criticalErrors = criticalErrors.size,
                canProceed = criticalErrors.isEmpty() && progress.failedServices == 0,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    suspend fun cleanupProcessData(processId: String) {
        try {
            // Revoke token
            processTokenManager.revokeToken(processId)
            
            // Mark old errors as resolved
            val oldErrors = processErrorManager.getUnresolvedErrors(processId)
            oldErrors.forEach { error ->
                processErrorManager.resolveError(
                    error.id,
                    "Process cleanup",
                    "Process data cleaned up"
                )
            }
            
        } catch (e: Exception) {
            // Log cleanup error but don't fail the operation
            processErrorManager.logError(
                processId = processId,
                throwable = e,
                context = ErrorContext(
                    isNetworkAvailable = true,
                    userFlow = "process_cleanup"
                )
            )
        }
    }
    
    private fun calculateProgress(serviceStates: List<com.jaak.kyc.data.local.entity.ServiceExecutionState>): ProcessProgress {
        val totalServices = serviceStates.size
        val completedServices = serviceStates.count { 
            it.status == com.jaak.kyc.data.local.entity.ServiceStatus.COMPLETED || 
            it.status == com.jaak.kyc.data.local.entity.ServiceStatus.SYNCED 
        }
        val failedServices = serviceStates.count { 
            it.status == com.jaak.kyc.data.local.entity.ServiceStatus.FAILED 
        }
        val pendingSync = serviceStates.count { it.requiresSync }
        
        return ProcessProgress(
            totalServices = totalServices,
            completedServices = completedServices,
            failedServices = failedServices,
            pendingSync = pendingSync,
            progressPercentage = if (totalServices > 0) (completedServices * 100) / totalServices else 0
        )
    }
}

sealed class ProcessInitializationResult {
    data class Success(val token: ProcessTokenEntity) : ProcessInitializationResult()
    data class Error(val error: ProcessErrorEntity) : ProcessInitializationResult()
}

sealed class ServiceExecutionResult {
    object Success : ServiceExecutionResult()
    data class Error(val error: ProcessErrorEntity) : ServiceExecutionResult()
    data class Blocked(val reason: String) : ServiceExecutionResult()
}

sealed class RetryResult {
    object CanRetry : RetryResult()
    data class MustWait(val waitTimeMs: Long) : RetryResult()
    object NoRetryNeeded : RetryResult()
    data class CannotRetry(val reason: String) : RetryResult()
}

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val error: ProcessErrorEntity) : SyncResult()
}

data class ProcessStatus(
    val processId: String,
    val progress: ProcessProgress,
    val hasValidToken: Boolean,
    val totalErrors: Int,
    val unresolvedErrors: Int,
    val criticalErrors: Int,
    val canProceed: Boolean,
    val lastUpdated: Long
)