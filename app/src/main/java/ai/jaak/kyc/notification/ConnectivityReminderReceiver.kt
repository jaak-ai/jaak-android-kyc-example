package ai.jaak.kyc.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ai.jaak.kyc.domain.service.NetworkConnectivityService
import ai.jaak.kyc.domain.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ConnectivityReminderReceiver : BroadcastReceiver() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        applicationScope.launch {
            try {
                val networkConnectivityService = NetworkConnectivityService(context)
                val notificationHelper = NotificationHelper(context)
                val notificationService = NotificationService(context, notificationHelper)
                
                if (networkConnectivityService.isNetworkAvailable()) {
                    // Network is available, check for pending processes
                    val pendingProcesses = getPendingProcessCount()
                    
                    if (pendingProcesses > 0) {
                        notificationService.showConnectivityRestoredNotification(pendingProcesses)
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }

    private suspend fun getPendingProcessCount(): Int {
        // Mock implementation - in real app, query the database
        // for processes that require sync
        return 2 // Mock pending processes
    }
}