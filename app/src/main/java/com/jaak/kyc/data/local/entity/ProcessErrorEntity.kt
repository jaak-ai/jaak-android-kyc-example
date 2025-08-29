package com.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "process_errors",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["processId", "serviceType", "timestamp"])]
)
data class ProcessErrorEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val processId: String,
    val serviceType: KycServiceType? = null,
    
    // Error details
    val errorCode: String,
    val errorMessage: String,
    val errorCategory: ErrorCategory,
    val severity: ErrorSeverity,
    
    // Context information
    val stackTrace: String? = null,
    val requestData: String? = null, // JSON of request that caused error
    val responseData: String? = null, // JSON of server response if available
    val timestamp: Long = System.currentTimeMillis(),
    
    // Resolution tracking
    val isResolved: Boolean = false,
    val resolvedAt: Long? = null,
    val resolutionAction: String? = null,
    val resolutionNotes: String? = null,
    
    // Retry information
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val nextRetryAt: Long? = null,
    val retryBackoffMs: Long = 1000L,
    
    // Network context
    val isNetworkError: Boolean = false,
    val networkStatusAtError: Boolean = true,
    val httpStatusCode: Int? = null,
    
    // User impact
    val affectedUserFlow: String? = null,
    val userMessage: String? = null,
    val showToUser: Boolean = false
)

enum class ErrorCategory {
    NETWORK,           // Network connectivity issues
    AUTHENTICATION,    // Token/auth related errors
    VALIDATION,        // Data validation errors
    BUSINESS_LOGIC,    // Business rule violations
    SYSTEM,           // System/infrastructure errors
    USER_INPUT,       // User provided invalid input
    TIMEOUT,          // Request timeout errors
    QUOTA_EXCEEDED,   // Rate limiting/quota errors
    UNKNOWN           // Unclassified errors
}

enum class ErrorSeverity {
    LOW,              // Non-blocking, informational
    MEDIUM,           // May impact user experience
    HIGH,             // Blocks user progress
    CRITICAL          // System-level failure
}