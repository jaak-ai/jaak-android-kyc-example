package com.jaak.kyc.domain.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkInfo
import com.jaak.kyc.R
import com.jaak.kyc.data.local.entity.KycServiceType
import com.jaak.kyc.notification.NotificationHelper
import com.jaak.kyc.notification.NotificationActionReceiver
import com.jaak.kyc.ui.view.KycProcessesActivity
import com.jaak.kyc.ui.view.KycSyncActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationHelper: NotificationHelper
) {
    
    companion object {
        const val ACTION_RETRY_SYNC = "com.jaak.kyc.ACTION_RETRY_SYNC"
        const val ACTION_CANCEL_SYNC = "com.jaak.kyc.ACTION_CANCEL_SYNC"
        const val ACTION_VIEW_DETAILS = "com.jaak.kyc.ACTION_VIEW_DETAILS"
        const val ACTION_SYNC_ALL = "com.jaak.kyc.ACTION_SYNC_ALL"
        
        const val EXTRA_PROCESS_ID = "extra_process_id"
        const val EXTRA_SERVICE_TYPE = "extra_service_type"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        
        // Extended notification IDs
        const val NOTIFICATION_BATCH_COMPLETE = 2001
        const val NOTIFICATION_CONNECTIVITY_RESTORED = 2002
        const val NOTIFICATION_SYNC_SCHEDULED = 2003
        const val NOTIFICATION_DAILY_SUMMARY = 2004
        const val NOTIFICATION_ERROR_BATCH = 2005
    }

    fun showSyncStartedNotification(processId: String, serviceName: String) {
        notificationHelper.showSyncProgressNotification(
            processId = processId,
            serviceName = serviceName,
            progress = 0,
            total = 0
        )
    }

    fun updateSyncProgressNotification(
        processId: String,
        serviceName: String,
        progress: Int,
        total: Int
    ) {
        notificationHelper.showSyncProgressNotification(
            processId = processId,
            serviceName = serviceName,
            progress = progress,
            total = total
        )
    }

    fun showSyncCompletedNotification(
        processId: String,
        serviceName: String,
        isSuccess: Boolean,
        message: String = ""
    ) {
        if (isSuccess) {
            notificationHelper.showSyncCompleteNotification(
                processId = processId,
                message = message.ifEmpty { "$serviceName sincronizado exitosamente" },
                syncedCount = 1,
                failedCount = 0
            )
        } else {
            notificationHelper.showSyncErrorNotification(
                processId = processId,
                serviceName = serviceName,
                errorMessage = message.ifEmpty { "Error al sincronizar $serviceName" },
                canRetry = true
            )
        }
    }

    fun showBatchSyncCompleteNotification(
        syncedCount: Int,
        failedCount: Int,
        totalProcesses: Int
    ) {
        val intent = Intent(context, KycSyncActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            failedCount == 0 -> "Sincronización Masiva Completada"
            syncedCount == 0 -> "Sincronización Masiva Falló"
            else -> "Sincronización Masiva Parcial"
        }

        val message = when {
            failedCount == 0 -> "Todos los $syncedCount procesos sincronizados exitosamente"
            syncedCount == 0 -> "No se pudo sincronizar ninguno de los $totalProcesses procesos"
            else -> "$syncedCount exitosos, $failedCount fallaron de $totalProcesses procesos"
        }

        val icon = if (failedCount == 0) R.drawable.ic_service_completed else R.drawable.ic_service_failed
        val priority = if (failedCount == 0) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SYNC_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(priority)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .apply {
                if (failedCount > 0) {
                    addAction(createRetryAction())
                }
                addAction(createViewDetailsAction())
            }
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_BATCH_COMPLETE, notification)
    }

    fun showConnectivityRestoredNotification(pendingProcesses: Int) {
        if (pendingProcesses == 0) return

        val intent = Intent(context, KycProcessesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (pendingProcesses == 1) {
            "Conexión restaurada. 1 proceso listo para sincronizar."
        } else {
            "Conexión restaurada. $pendingProcesses procesos listos para sincronizar."
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_GENERAL_ID)
            .setContentTitle("🌐 Red Disponible")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_service_completed)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(createSyncAllAction(pendingProcesses))
            .addAction(createViewDetailsAction())
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_CONNECTIVITY_RESTORED, notification)
    }

    fun showSyncScheduledNotification(scheduledCount: Int, nextSyncTime: String) {
        val intent = Intent(context, KycSyncActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (scheduledCount == 1) {
            "1 proceso programado para sincronizar $nextSyncTime"
        } else {
            "$scheduledCount procesos programados para sincronizar $nextSyncTime"
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SYNC_ID)
            .setContentTitle("⏰ Sincronización Programada")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_SYNC_SCHEDULED, notification)
    }

    fun showDailySummaryNotification(
        processedToday: Int,
        syncedToday: Int,
        failedToday: Int,
        pendingSync: Int
    ) {
        if (processedToday == 0 && pendingSync == 0) return

        val intent = Intent(context, KycSyncActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "📊 Resumen Diario KYC"
        val summaryText = buildString {
            if (processedToday > 0) {
                append("Procesados hoy: $processedToday")
                if (syncedToday > 0 || failedToday > 0) {
                    append(" ($syncedToday exitosos, $failedToday fallidos)")
                }
            }
            if (pendingSync > 0) {
                if (processedToday > 0) append("\n")
                append("Pendientes de sync: $pendingSync")
            }
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_GENERAL_ID)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setSmallIcon(R.drawable.ic_service_pending)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .apply {
                if (pendingSync > 0) {
                    addAction(createSyncAllAction(pendingSync))
                }
            }
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_DAILY_SUMMARY, notification)
    }

    fun showErrorBatchNotification(errors: List<SyncError>) {
        if (errors.isEmpty()) return

        val intent = Intent(context, KycProcessesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚠️ Errores de Sincronización"
        val message = if (errors.size == 1) {
            "1 error de sincronización requiere atención"
        } else {
            "${errors.size} errores de sincronización requieren atención"
        }

        val bigText = buildString {
            append(message)
            append("\n\n")
            errors.take(3).forEach { error ->
                append("• ${error.processShortKey}: ${error.serviceName}\n")
            }
            if (errors.size > 3) {
                append("... y ${errors.size - 3} más")
            }
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ERROR_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_service_failed)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .addAction(createRetryAction())
            .addAction(createViewDetailsAction())
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ERROR_BATCH, notification)
    }

    fun dismissSyncNotifications() {
        notificationHelper.dismissAllSyncNotifications()
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_BATCH_COMPLETE)
        notificationManager.cancel(NOTIFICATION_CONNECTIVITY_RESTORED)
        notificationManager.cancel(NOTIFICATION_SYNC_SCHEDULED)
        notificationManager.cancel(NOTIFICATION_ERROR_BATCH)
    }

    fun dismissDailySummary() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_DAILY_SUMMARY)
    }

    private fun createRetryAction(): NotificationCompat.Action {
        val retryIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_RETRY_SYNC
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context, 0, retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_service_pending,
            "Reintentar",
            retryPendingIntent
        ).build()
    }

    private fun createViewDetailsAction(): NotificationCompat.Action {
        val detailsIntent = Intent(context, KycSyncActivity::class.java)
        val detailsPendingIntent = PendingIntent.getActivity(
            context, 0, detailsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_service_completed,
            "Ver detalles",
            detailsPendingIntent
        ).build()
    }

    private fun createSyncAllAction(count: Int): NotificationCompat.Action {
        val syncIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SYNC_ALL
            putExtra(EXTRA_NOTIFICATION_ID, count)
        }
        val syncPendingIntent = PendingIntent.getBroadcast(
            context, 0, syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val actionText = if (count == 1) "Sincronizar" else "Sincronizar todo"
        return NotificationCompat.Action.Builder(
            R.drawable.ic_service_pending,
            actionText,
            syncPendingIntent
        ).build()
    }

    data class SyncError(
        val processId: String,
        val processShortKey: String,
        val serviceName: String,
        val errorMessage: String
    )
}