package com.jaak.kyc.domain.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.jaak.kyc.notification.NotificationActionReceiver
import com.jaak.kyc.notification.DailySummaryReceiver
import com.jaak.kyc.notification.ConnectivityReminderReceiver
import com.jaak.kyc.notification.ErrorReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationService: NotificationService,
    private val workManager: WorkManager
) {
    
    companion object {
        private const val DAILY_SUMMARY_REQUEST_CODE = 1000
        private const val CONNECTIVITY_CHECK_REQUEST_CODE = 1001
        private const val ERROR_REMINDER_REQUEST_CODE = 1002
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDailySummary(hour: Int = 20, minute: Int = 0) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If the time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(context, DailySummaryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun scheduleConnectivityReminder(delayMinutes: Int = 30) {
        val futureTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000)

        val intent = Intent(context, ConnectivityReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            CONNECTIVITY_CHECK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            futureTime,
            pendingIntent
        )
    }

    fun scheduleErrorReminder(processId: String, delayHours: Int = 2) {
        val futureTime = System.currentTimeMillis() + (delayHours * 60 * 60 * 1000)

        val intent = Intent(context, ErrorReminderReceiver::class.java).apply {
            putExtra("process_id", processId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ERROR_REMINDER_REQUEST_CODE + processId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            futureTime,
            pendingIntent
        )
    }

    fun cancelDailySummary() {
        val intent = Intent(context, DailySummaryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    fun cancelConnectivityReminder() {
        val intent = Intent(context, ConnectivityReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            CONNECTIVITY_CHECK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    fun cancelErrorReminder(processId: String) {
        val intent = Intent(context, ErrorReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ERROR_REMINDER_REQUEST_CODE + processId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    suspend fun checkAndNotifyWorkStatus() {
        try {
            val workInfos = workManager.getWorkInfosByTag("sync_service").get()
            
            val runningWork = workInfos.filter { it.state == WorkInfo.State.RUNNING }
            val failedWork = workInfos.filter { it.state == WorkInfo.State.FAILED }
            
            if (failedWork.isNotEmpty()) {
                val errors = failedWork.map { workInfo ->
                    NotificationService.SyncError(
                        processId = workInfo.tags.find { it.startsWith("process_") }?.substring(8) ?: "unknown",
                        processShortKey = "PROC-${System.currentTimeMillis() % 1000}",
                        serviceName = workInfo.tags.find { it != "sync_service" } ?: "Unknown Service",
                        errorMessage = workInfo.outputData.getString("error") ?: "Sync failed"
                    )
                }
                
                notificationService.showErrorBatchNotification(errors)
            }
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }
}