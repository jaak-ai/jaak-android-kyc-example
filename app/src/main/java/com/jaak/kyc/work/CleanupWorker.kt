package com.jaak.kyc.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.jaak.kyc.domain.service.ProcessTokenManager
import com.jaak.kyc.domain.service.ProcessErrorManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val tokenManager: ProcessTokenManager,
    private val errorManager: ProcessErrorManager
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_CLEANUP_TYPE = "cleanup_type"
        const val KEY_RETENTION_DAYS = "retention_days"
        
        const val CLEANUP_TYPE_TOKENS = "tokens"
        const val CLEANUP_TYPE_ERRORS = "errors"
        const val CLEANUP_TYPE_ALL = "all"
        
        const val DEFAULT_RETENTION_DAYS = 30
    }

    override suspend fun doWork(): Result {
        return try {
            val cleanupType = inputData.getString(KEY_CLEANUP_TYPE) ?: CLEANUP_TYPE_ALL
            val retentionDays = inputData.getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)
            
            var cleanedTokens = 0
            var cleanedErrors = 0
            
            when (cleanupType) {
                CLEANUP_TYPE_TOKENS -> {
                    cleanedTokens = cleanupTokens()
                }
                CLEANUP_TYPE_ERRORS -> {
                    cleanedErrors = cleanupErrors(retentionDays)
                }
                CLEANUP_TYPE_ALL -> {
                    cleanedTokens = cleanupTokens()
                    cleanedErrors = cleanupErrors(retentionDays)
                }
            }
            
            val resultMessage = buildString {
                append("Cleanup completed: ")
                if (cleanedTokens > 0) append("$cleanedTokens expired tokens removed")
                if (cleanedErrors > 0) {
                    if (cleanedTokens > 0) append(", ")
                    append("$cleanedErrors old errors cleaned")
                }
                if (cleanedTokens == 0 && cleanedErrors == 0) {
                    append("no items needed cleanup")
                }
            }
            
            Result.success(createResultData("success", resultMessage, cleanedTokens + cleanedErrors))
            
        } catch (e: Exception) {
            Result.failure(createResultData("error", "Cleanup failed: ${e.message}", 0))
        }
    }

    private suspend fun cleanupTokens(): Int {
        val beforeStats = tokenManager.getTokenStatistics()
        
        // Clean up expired tokens
        tokenManager.cleanupExpiredTokens()
        
        val afterStats = tokenManager.getTokenStatistics()
        
        return beforeStats.expiredTokens
    }

    private suspend fun cleanupErrors(retentionDays: Int): Int {
        // Clean up old resolved errors
        errorManager.cleanupOldErrors(retentionDays)
        
        // Return approximate count (in real implementation, track actual count)
        return 0 // Would need to track actual deleted count
    }

    private fun createResultData(status: String, message: String, itemsProcessed: Int): Data {
        return Data.Builder()
            .putString("status", status)
            .putString("message", message)
            .putInt("items_processed", itemsProcessed)
            .putLong("timestamp", System.currentTimeMillis())
            .build()
    }
}