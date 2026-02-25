package ai.jaak.kyc.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import ai.jaak.kyc.R
import ai.jaak.kyc.data.local.entity.KycServiceType
import ai.jaak.kyc.domain.service.KycSyncServiceSimple
import ai.jaak.kyc.domain.service.NotificationService
import ai.jaak.kyc.ui.view.KycProcessesActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val syncService: KycSyncServiceSimple,
    private val notificationService: NotificationService
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PROCESS_ID = "process_id"
        const val KEY_SERVICE_TYPE = "service_type"
        const val KEY_SYNC_TYPE = "sync_type"
        
        const val SYNC_TYPE_INDIVIDUAL = "individual"
        const val SYNC_TYPE_PROCESS = "process"
        const val SYNC_TYPE_BULK = "bulk"
        
        private const val NOTIFICATION_CHANNEL_ID = "kyc_sync_channel"
        private const val SYNC_NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("SyncWorker", "=== doWork() STARTED ===")
        return try {
            val processId = inputData.getString(KEY_PROCESS_ID)
            val serviceType = inputData.getString(KEY_SERVICE_TYPE)?.let { 
                KycServiceType.valueOf(it) 
            }
            val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_INDIVIDUAL

            android.util.Log.d("SyncWorker", """
                Input parameters:
                - processId: $processId
                - serviceType: $serviceType
                - syncType: $syncType
            """.trimIndent())

            createNotificationChannel()
            setForeground(createForegroundInfo("Starting sync..."))

            when (syncType) {
                SYNC_TYPE_INDIVIDUAL -> {
                    if (processId != null && serviceType != null) {
                        syncIndividualService(processId, serviceType)
                    } else {
                        Result.failure(createErrorData("Missing process ID or service type"))
                    }
                }
                SYNC_TYPE_PROCESS -> {
                    if (processId != null) {
                        syncAllServicesForProcess(processId)
                    } else {
                        Result.failure(createErrorData("Missing process ID"))
                    }
                }
                SYNC_TYPE_BULK -> {
                    syncBulkProcesses()
                }
                else -> Result.failure(createErrorData("Unknown sync type: $syncType"))
            }
        } catch (e: Exception) {
            Result.failure(createErrorData("Sync failed: ${e.message}"))
        }
    }

    private suspend fun syncIndividualService(processId: String, serviceType: KycServiceType): Result {
        return try {
            setForeground(createForegroundInfo("Syncing ${serviceType.displayName}..."))
            
            // Show sync started notification
            notificationService.showSyncStartedNotification(processId, serviceType.displayName)
            
            val result = syncService.syncIndividualService(processId, serviceType)
            
            when (result) {
                is ai.jaak.kyc.domain.service.SyncResult.Success -> {
                    notificationService.showSyncCompletedNotification(
                        processId = processId,
                        serviceName = serviceType.displayName,
                        isSuccess = true,
                        message = result.message
                    )
                    Result.success(createSuccessData(result.message))
                }
                is ai.jaak.kyc.domain.service.SyncResult.Error -> {
                    notificationService.showSyncCompletedNotification(
                        processId = processId,
                        serviceName = serviceType.displayName,
                        isSuccess = false,
                        message = result.message
                    )
                    Result.failure(createErrorData(result.message))
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to sync ${serviceType.displayName}: ${e.message}"
            notificationService.showSyncCompletedNotification(
                processId = processId,
                serviceName = serviceType.displayName,
                isSuccess = false,
                message = errorMsg
            )
            Result.failure(createErrorData(errorMsg))
        }
    }

    private suspend fun syncAllServicesForProcess(processId: String): Result {
        return try {
            var successCount = 0
            var failureCount = 0
            val results = mutableListOf<String>()

            syncService.syncAllServicesForProcess(processId).collect { progress ->
                when (progress) {
                    is ai.jaak.kyc.domain.service.SyncProgress.Started -> {
                        setForeground(createForegroundInfo("Syncing ${progress.totalServices} services..."))
                    }
                    is ai.jaak.kyc.domain.service.SyncProgress.Syncing -> {
                        setForeground(createForegroundInfo("Syncing ${progress.serviceName.displayName}..."))
                    }
                    is ai.jaak.kyc.domain.service.SyncProgress.ServiceCompleted -> {
                        if (progress.success) {
                            successCount++
                        } else {
                            failureCount++
                        }
                        results.add("${progress.serviceName.displayName}: ${if (progress.success) "✓" else "✗"}")
                    }
                    is ai.jaak.kyc.domain.service.SyncProgress.Completed -> {
                        val message = "Sync completed: $successCount success, $failureCount failed"
                        if (failureCount == 0) {
                            showCompletionNotification("Process synced", message)
                        } else {
                            showWarningNotification("Partial sync", message)
                        }
                    }
                }
            }

            if (failureCount == 0) {
                Result.success(createSuccessData("All services synced successfully"))
            } else {
                Result.failure(createErrorData("$failureCount services failed to sync"))
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to sync process: ${e.message}"
            showErrorNotification("Process sync failed", errorMsg)
            Result.failure(createErrorData(errorMsg))
        }
    }

    private suspend fun syncBulkProcesses(): Result {
        android.util.Log.d("SyncWorker", "=== syncBulkProcesses() STARTED ===")
        return try {
            setForeground(createForegroundInfo("Loading processes to sync..."))
            
            android.util.Log.d("SyncWorker", "Calling syncService.getBulkSyncCandidates()")
            val candidates = syncService.getBulkSyncCandidates()
            val processIds = candidates.map { it.processId }
            
            android.util.Log.d("SyncWorker", "Found ${candidates.size} candidates: $processIds")
            
            if (processIds.isEmpty()) {
                android.util.Log.d("SyncWorker", "No processes to sync - returning success")
                showCompletionNotification("Bulk sync", "No processes need synchronization")
                return Result.success(createSuccessData("No processes to sync"))
            }

            var successCount = 0
            var failureCount = 0

            syncService.executeBulkSync(processIds).collect { progress ->
                when (progress) {
                    is ai.jaak.kyc.domain.service.BulkSyncProgress.Started -> {
                        setForeground(createForegroundInfo("Syncing ${progress.totalProcesses} processes..."))
                    }
                    is ai.jaak.kyc.domain.service.BulkSyncProgress.ProcessStarted -> {
                        setForeground(createForegroundInfo("Syncing ${progress.shortKey}..."))
                    }
                    is ai.jaak.kyc.domain.service.BulkSyncProgress.ProcessCompleted -> {
                        if (progress.success) {
                            successCount++
                        } else {
                            failureCount++
                        }
                    }
                    is ai.jaak.kyc.domain.service.BulkSyncProgress.AllCompleted -> {
                        notificationService.showBatchSyncCompleteNotification(
                            syncedCount = successCount,
                            failedCount = failureCount,
                            totalProcesses = processIds.size
                        )
                    }
                }
            }

            if (failureCount == 0) {
                Result.success(createSuccessData("All processes synced successfully"))
            } else {
                Result.failure(createErrorData("$failureCount processes failed to sync"))
            }
        } catch (e: Exception) {
            val errorMsg = "Bulk sync failed: ${e.message}"
            showErrorNotification("Bulk sync failed", errorMsg)
            Result.failure(createErrorData(errorMsg))
        }
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        val intent = Intent(context, KycProcessesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("KYC Sync in Progress")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_service_pending)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        return ForegroundInfo(SYNC_NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(title: String, message: String) {
        val intent = Intent(context, KycProcessesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_service_completed)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SYNC_NOTIFICATION_ID + 1, notification)
    }

    private fun showWarningNotification(title: String, message: String) {
        val intent = Intent(context, KycProcessesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_service_pending)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SYNC_NOTIFICATION_ID + 2, notification)
    }

    private fun showErrorNotification(title: String, message: String) {
        val intent = Intent(context, KycProcessesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_service_failed)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SYNC_NOTIFICATION_ID + 3, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "KYC Sync Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for KYC synchronization background tasks"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createSuccessData(message: String): Data {
        return Data.Builder()
            .putString("result", "success")
            .putString("message", message)
            .build()
    }

    private fun createErrorData(message: String): Data {
        return Data.Builder()
            .putString("result", "error")
            .putString("message", message)
            .build()
    }
}