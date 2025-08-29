package com.jaak.kyc.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jaak.kyc.domain.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ErrorReminderReceiver : BroadcastReceiver() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        val processId = intent.getStringExtra("process_id") ?: return

        applicationScope.launch {
            try {
                val notificationHelper = NotificationHelper(context)
                val notificationService = NotificationService(context, notificationHelper)
                
                // Check if the process still has errors
                val hasErrors = checkProcessErrors(processId)
                
                if (hasErrors) {
                    val errors = getProcessErrors(processId)
                    notificationService.showErrorBatchNotification(errors)
                }
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }

    private suspend fun checkProcessErrors(processId: String): Boolean {
        // Mock implementation - in real app, query the error database
        return true // Assume there are still errors for demo
    }

    private suspend fun getProcessErrors(processId: String): List<NotificationService.SyncError> {
        // Mock implementation - in real app, get actual errors from database
        return listOf(
            NotificationService.SyncError(
                processId = processId,
                processShortKey = "PROC-${processId.take(4)}",
                serviceName = "Document Verification",
                errorMessage = "Network timeout"
            )
        )
    }
}