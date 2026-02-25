package ai.jaak.kyc.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ai.jaak.kyc.domain.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DailySummaryReceiver : BroadcastReceiver() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        applicationScope.launch {
            try {
                val notificationHelper = NotificationHelper(context)
                val notificationService = NotificationService(context, notificationHelper)
                
                // In a real implementation, these would come from the database
                val stats = getDailyStats()
                
                notificationService.showDailySummaryNotification(
                    processedToday = stats.processedToday,
                    syncedToday = stats.syncedToday,
                    failedToday = stats.failedToday,
                    pendingSync = stats.pendingSync
                )
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }

    private suspend fun getDailyStats(): DailyStats {
        // Mock implementation - in real app, query the database
        // for processes created/updated today
        return DailyStats(
            processedToday = 5,
            syncedToday = 4,
            failedToday = 1,
            pendingSync = 3
        )
    }

    data class DailyStats(
        val processedToday: Int,
        val syncedToday: Int,
        val failedToday: Int,
        val pendingSync: Int
    )
}