package ai.jaak.kyc.domain.service

import ai.jaak.kyc.data.local.dao.ProcessErrorDao
import ai.jaak.kyc.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessErrorManager @Inject constructor(
    private val processErrorDao: ProcessErrorDao
) {
    
    suspend fun logError(
        processId: String,
        serviceType: KycServiceType? = null,
        throwable: Throwable,
        requestData: String? = null,
        responseData: String? = null,
        context: ErrorContext? = null
    ): ProcessErrorEntity {
        
        val errorCategory = categorizeError(throwable, context)
        val severity = determineSeverity(throwable, errorCategory)
        val userMessage = generateUserMessage(throwable, errorCategory)
        
        val error = ProcessErrorEntity(
            processId = processId,
            serviceType = serviceType,
            errorCode = getErrorCode(throwable),
            errorMessage = throwable.message ?: "Unknown error",
            errorCategory = errorCategory,
            severity = severity,
            stackTrace = throwable.stackTraceToString(),
            requestData = requestData,
            responseData = responseData,
            isNetworkError = isNetworkError(throwable),
            networkStatusAtError = context?.isNetworkAvailable ?: true,
            httpStatusCode = context?.httpStatusCode,
            affectedUserFlow = context?.userFlow,
            userMessage = userMessage,
            showToUser = severity >= ErrorSeverity.MEDIUM,
            retryBackoffMs = calculateBackoffDelay(0)
        )
        
        processErrorDao.insertError(error)
        return error
    }
    
    suspend fun logCustomError(
        processId: String,
        serviceType: KycServiceType? = null,
        errorCode: String,
        errorMessage: String,
        category: ErrorCategory,
        severity: ErrorSeverity,
        showToUser: Boolean = false,
        userMessage: String? = null
    ): ProcessErrorEntity {
        
        val error = ProcessErrorEntity(
            processId = processId,
            serviceType = serviceType,
            errorCode = errorCode,
            errorMessage = errorMessage,
            errorCategory = category,
            severity = severity,
            userMessage = userMessage,
            showToUser = showToUser
        )
        
        processErrorDao.insertError(error)
        return error
    }
    
    suspend fun getUnresolvedErrors(processId: String): List<ProcessErrorEntity> {
        return processErrorDao.getUnresolvedErrors(processId)
    }
    
    suspend fun getErrorsForService(processId: String, serviceType: KycServiceType): List<ProcessErrorEntity> {
        return processErrorDao.getErrorsByProcessAndService(processId, serviceType)
    }
    
    suspend fun getUserVisibleErrors(processId: String): List<ProcessErrorEntity> {
        return processErrorDao.getUnresolvedErrors(processId).filter { it.showToUser }
    }
    
    fun getErrorsFlow(processId: String): Flow<List<ProcessErrorEntity>> {
        return processErrorDao.getErrorsByProcessIdFlow(processId)
    }
    
    suspend fun resolveError(
        errorId: String,
        resolutionAction: String,
        notes: String? = null
    ) {
        processErrorDao.resolveError(errorId, action = resolutionAction, notes = notes)
    }
    
    suspend fun markErrorForRetry(errorId: String): Boolean {
        val error = processErrorDao.getErrorsByProcessId("").find { it.id == errorId }
            ?: return false
            
        if (error.retryCount >= error.maxRetries) {
            return false
        }
        
        val nextRetryAt = System.currentTimeMillis() + calculateBackoffDelay(error.retryCount)
        processErrorDao.incrementRetryCount(errorId, nextRetryAt)
        return true
    }
    
    suspend fun getErrorsReadyForRetry(): List<ProcessErrorEntity> {
        return processErrorDao.getErrorsReadyForRetry()
    }
    
    suspend fun retryFailedService(processId: String, serviceType: KycServiceType): RetryResult {
        val errors = processErrorDao.getErrorsByProcessAndService(processId, serviceType)
            .filter { !it.isResolved && it.retryCount < it.maxRetries }
            .sortedByDescending { it.timestamp }
            
        return if (errors.isNotEmpty()) {
            val latestError = errors.first()
            val currentTime = System.currentTimeMillis()
            
            if (latestError.nextRetryAt == null || latestError.nextRetryAt <= currentTime) {
                markErrorForRetry(latestError.id)
                RetryResult.CanRetry(latestError)
            } else {
                RetryResult.MustWait(latestError.nextRetryAt - currentTime)
            }
        } else {
            RetryResult.NoRetryNeeded
        }
    }
    
    suspend fun getProcessHealthScore(processId: String): ProcessHealthScore {
        val allErrors = processErrorDao.getErrorsByProcessId(processId)
        val unresolvedErrors = allErrors.filter { !it.isResolved }
        val criticalErrors = unresolvedErrors.filter { it.severity == ErrorSeverity.CRITICAL }
        val highErrors = unresolvedErrors.filter { it.severity == ErrorSeverity.HIGH }
        
        val score = when {
            criticalErrors.isNotEmpty() -> 0
            highErrors.size > 3 -> 25
            highErrors.isNotEmpty() -> 50
            unresolvedErrors.size > 5 -> 75
            unresolvedErrors.isNotEmpty() -> 85
            else -> 100
        }
        
        return ProcessHealthScore(
            processId = processId,
            healthScore = score,
            totalErrors = allErrors.size,
            unresolvedErrors = unresolvedErrors.size,
            criticalErrors = criticalErrors.size,
            highSeverityErrors = highErrors.size,
            canProceed = score > 25
        )
    }
    
    suspend fun cleanupOldErrors(retentionDays: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        processErrorDao.deleteResolvedErrorsOlderThan(cutoffTime)
    }
    
    suspend fun generateErrorReport(processId: String): ErrorReport {
        val errors = processErrorDao.getErrorsByProcessId(processId)
        
        // Build category counts manually
        val categoryCounts = ErrorCategory.values().associateWith { category ->
            processErrorDao.getErrorCountByCategory(processId, category)
        }
        
        // Build service counts manually  
        val serviceCounts = KycServiceType.values().associateWith { service ->
            processErrorDao.getErrorCountByService(processId, service)
        }
        
        return ErrorReport(
            processId = processId,
            totalErrors = errors.size,
            unresolvedErrors = errors.count { !it.isResolved },
            errorsByCategory = categoryCounts,
            errorsByService = serviceCounts,
            averageRetryCount = processErrorDao.getAverageRetryCount(processId),
            mostFrequentErrorCode = errors.groupBy { it.errorCode }
                .maxByOrNull { it.value.size }?.key,
            recommendations = generateRecommendations(errors)
        )
    }
    
    private fun categorizeError(throwable: Throwable, context: ErrorContext?): ErrorCategory {
        val message = throwable.message?.lowercase() ?: ""
        val className = throwable::class.simpleName?.lowercase() ?: ""
        
        return when {
            isNetworkError(throwable) -> ErrorCategory.NETWORK
            message.contains("token") || message.contains("auth") -> ErrorCategory.AUTHENTICATION
            message.contains("validation") || message.contains("invalid") -> ErrorCategory.VALIDATION
            message.contains("timeout") -> ErrorCategory.TIMEOUT
            message.contains("quota") || message.contains("limit") -> ErrorCategory.QUOTA_EXCEEDED
            className.contains("security") -> ErrorCategory.AUTHENTICATION
            context?.httpStatusCode in 400..499 -> ErrorCategory.USER_INPUT
            context?.httpStatusCode in 500..599 -> ErrorCategory.SYSTEM
            else -> ErrorCategory.UNKNOWN
        }
    }
    
    private fun determineSeverity(throwable: Throwable, category: ErrorCategory): ErrorSeverity {
        return when (category) {
            ErrorCategory.NETWORK -> if (isNetworkError(throwable)) ErrorSeverity.HIGH else ErrorSeverity.MEDIUM
            ErrorCategory.AUTHENTICATION -> ErrorSeverity.HIGH
            ErrorCategory.SYSTEM -> ErrorSeverity.CRITICAL
            ErrorCategory.VALIDATION -> ErrorSeverity.MEDIUM
            ErrorCategory.USER_INPUT -> ErrorSeverity.LOW
            ErrorCategory.TIMEOUT -> ErrorSeverity.MEDIUM
            ErrorCategory.QUOTA_EXCEEDED -> ErrorSeverity.HIGH
            else -> ErrorSeverity.MEDIUM
        }
    }
    
    private fun generateUserMessage(throwable: Throwable, category: ErrorCategory): String {
        return when (category) {
            ErrorCategory.NETWORK -> "Connection problem. Please check your internet and try again."
            ErrorCategory.AUTHENTICATION -> "Authentication error. Please restart the process."
            ErrorCategory.VALIDATION -> "Invalid data provided. Please check your information."
            ErrorCategory.TIMEOUT -> "Request timed out. Please try again."
            ErrorCategory.QUOTA_EXCEEDED -> "Service temporarily unavailable. Please try again later."
            ErrorCategory.SYSTEM -> "System error occurred. Please contact support if this persists."
            else -> "An error occurred. Please try again."
        }
    }
    
    private fun isNetworkError(throwable: Throwable): Boolean {
        val message = throwable.message?.lowercase() ?: ""
        val className = throwable::class.simpleName?.lowercase() ?: ""
        
        return message.contains("network") || 
               message.contains("connection") ||
               message.contains("host") ||
               className.contains("network") ||
               className.contains("connect")
    }
    
    private fun getErrorCode(throwable: Throwable): String {
        return "${throwable::class.simpleName}_${throwable.message?.hashCode()?.toString(16) ?: "UNKNOWN"}"
    }
    
    private fun calculateBackoffDelay(retryCount: Int): Long {
        return (1000L * (1 shl retryCount)).coerceAtMost(300000L) // Exponential backoff, max 5 minutes
    }
    
    private fun generateRecommendations(errors: List<ProcessErrorEntity>): List<String> {
        val recommendations = mutableListOf<String>()
        
        val networkErrors = errors.count { it.isNetworkError }
        if (networkErrors > 3) {
            recommendations.add("Check internet connection stability")
        }
        
        val authErrors = errors.count { it.errorCategory == ErrorCategory.AUTHENTICATION }
        if (authErrors > 1) {
            recommendations.add("Review token management and refresh logic")
        }
        
        val highRetryCount = errors.any { it.retryCount > 3 }
        if (highRetryCount) {
            recommendations.add("Consider increasing retry delay or max retry count")
        }
        
        return recommendations
    }
}

data class ErrorContext(
    val isNetworkAvailable: Boolean,
    val httpStatusCode: Int? = null,
    val userFlow: String? = null,
    val additionalData: Map<String, String> = emptyMap()
)

sealed class RetryResult {
    data class CanRetry(val error: ProcessErrorEntity) : RetryResult()
    data class MustWait(val waitTimeMs: Long) : RetryResult()
    object NoRetryNeeded : RetryResult()
}

data class ProcessHealthScore(
    val processId: String,
    val healthScore: Int, // 0-100
    val totalErrors: Int,
    val unresolvedErrors: Int,
    val criticalErrors: Int,
    val highSeverityErrors: Int,
    val canProceed: Boolean
)

data class ErrorReport(
    val processId: String,
    val totalErrors: Int,
    val unresolvedErrors: Int,
    val errorsByCategory: Map<ErrorCategory, Int>,
    val errorsByService: Map<KycServiceType, Int>,
    val averageRetryCount: Double,
    val mostFrequentErrorCode: String?,
    val recommendations: List<String>
)