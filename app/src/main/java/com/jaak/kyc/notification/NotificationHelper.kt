package com.jaak.kyc.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jaak.kyc.R
import com.jaak.kyc.ui.view.KycProcessesActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    companion object {
        const val CHANNEL_SYNC_ID = "kyc_sync_channel"
        const val CHANNEL_ERROR_ID = "kyc_error_channel"
        const val CHANNEL_GENERAL_ID = "kyc_general_channel"
        
        const val NOTIFICATION_SYNC_PROGRESS = 1001
        const val NOTIFICATION_SYNC_COMPLETE = 1002
        const val NOTIFICATION_SYNC_ERROR = 1003
        const val NOTIFICATION_NETWORK_RESTORED = 1004
        const val NOTIFICATION_PROCESS_COMPLETE = 1005
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Sync Channel
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC_ID,
                "KYC Synchronization",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for KYC data synchronization"
                enableLights(true)
                enableVibration(false)
            }

            // Error Channel
            val errorChannel = NotificationChannel(
                CHANNEL_ERROR_ID,
                "KYC Errors",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for KYC errors and failures"
                enableLights(true)
                enableVibration(true)
            }

            // General Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL_ID,
                "KYC General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General KYC notifications"
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(syncChannel, errorChannel, generalChannel))
        }
    }

    fun showSyncProgressNotification(
        processId: String,
        serviceName: String,
        progress: Int,
        total: Int
    ) {
        val intent = createMainActivityIntent()
        val pendingIntent = createPendingIntent(intent)

        val progressText = if (total > 0) "$progress/$total" else "In Progress"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC_ID)
            .setContentTitle("Syncing KYC Data")
            .setContentText("$serviceName - $progressText")
            .setSmallIcon(R.drawable.ic_service_pending)
            .setContentIntent(pendingIntent)
            .setProgress(total, progress, total == 0)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_SYNC_PROGRESS, notification)
    }

    fun showSyncCompleteNotification(
        processId: String,
        message: String,
        syncedCount: Int,
        failedCount: Int = 0
    ) {
        val intent = createMainActivityIntent()
        val pendingIntent = createPendingIntent(intent)

        val title = if (failedCount == 0) {
            "Sync Completed Successfully"
        } else {
            "Sync Completed with Issues"
        }

        val details = if (failedCount == 0) {
            "All $syncedCount services synced successfully"
        } else {
            "$syncedCount synced, $failedCount failed"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC_ID)
            .setContentTitle(title)
            .setContentText(details)
            .setSmallIcon(if (failedCount == 0) R.drawable.ic_service_completed else R.drawable.ic_service_pending)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\n$details"))
            .build()

        // Cancel progress notification
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_SYNC_PROGRESS)
        
        // Show completion notification
        NotificationManagerCompat.from(context).notify(NOTIFICATION_SYNC_COMPLETE, notification)
    }

    fun showSyncErrorNotification(
        processId: String,
        serviceName: String,
        errorMessage: String,
        canRetry: Boolean = true
    ) {
        val intent = createMainActivityIntent()
        val pendingIntent = createPendingIntent(intent)

        val actionText = if (canRetry) "Tap to retry" else "Tap to view details"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ERROR_ID)
            .setContentTitle("Sync Failed: $serviceName")
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_service_failed)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$errorMessage\n\n$actionText"))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_SYNC_ERROR, notification)
    }

    fun showNetworkRestoredNotification(pendingProcesses: Int) {
        if (pendingProcesses == 0) return

        val intent = createMainActivityIntent()
        val pendingIntent = createPendingIntent(intent)

        val message = if (pendingProcesses == 1) {
            "1 process is ready to sync"
        } else {
            "$pendingProcesses processes are ready to sync"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL_ID)
            .setContentTitle("Network Connection Restored")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_service_completed)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_NETWORK_RESTORED, notification)
    }

    fun showProcessCompleteNotification(
        processId: String,
        shortKey: String,
        completedOffline: Boolean
    ) {
        val intent = createMainActivityIntent()
        val pendingIntent = createPendingIntent(intent)

        val title = "KYC Process Completed"
        val message = if (completedOffline) {
            "Process $shortKey completed offline. Will sync when network is available."
        } else {
            "Process $shortKey completed successfully."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_service_completed)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_PROCESS_COMPLETE, notification)
    }

    fun dismissSyncProgressNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_SYNC_PROGRESS)
    }

    fun dismissAllSyncNotifications() {
        NotificationManagerCompat.from(context).apply {
            cancel(NOTIFICATION_SYNC_PROGRESS)
            cancel(NOTIFICATION_SYNC_COMPLETE)
            cancel(NOTIFICATION_SYNC_ERROR)
        }
    }

    private fun createMainActivityIntent(): Intent {
        return Intent(context, KycProcessesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    private fun createPendingIntent(intent: Intent): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}