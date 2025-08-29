package com.jaak.kyc.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.work.WorkManager
import com.jaak.kyc.data.local.entity.KycServiceType
import com.jaak.kyc.domain.service.NotificationService
import com.jaak.kyc.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            NotificationService.ACTION_RETRY_SYNC -> {
                handleRetrySync(context, intent)
            }
            NotificationService.ACTION_CANCEL_SYNC -> {
                handleCancelSync(context, intent)
            }
            NotificationService.ACTION_SYNC_ALL -> {
                handleSyncAll(context, intent)
            }
        }
    }

    private fun handleRetrySync(context: Context, intent: Intent) {
        val processId = intent.getStringExtra(NotificationService.EXTRA_PROCESS_ID)
        val serviceTypeString = intent.getStringExtra(NotificationService.EXTRA_SERVICE_TYPE)
        
        applicationScope.launch {
            try {
                val workScheduler = WorkScheduler(WorkManager.getInstance(context))
                
                if (processId != null && serviceTypeString != null) {
                    // Retry specific service
                    val serviceType = KycServiceType.valueOf(serviceTypeString)
                    workScheduler.scheduleServiceSync(
                        processId = processId,
                        serviceType = serviceType.name,
                        delayMinutes = 1,
                        requiresNetwork = true
                    )
                    
                    showToast(context, "Reintentando sincronización de $serviceTypeString...")
                } else if (processId != null) {
                    // Retry entire process
                    workScheduler.scheduleProcessSync(
                        processId = processId,
                        delayMinutes = 1,
                        requiresNetwork = true
                    )
                    
                    showToast(context, "Reintentando sincronización del proceso...")
                } else {
                    // Retry all failed syncs
                    scheduleFailedSyncs(context)
                }
                
                // Dismiss error notification
                val notificationId = intent.getIntExtra(NotificationService.EXTRA_NOTIFICATION_ID, -1)
                if (notificationId != -1) {
                    NotificationHelper(context).dismissSyncProgressNotification()
                }
                
            } catch (e: Exception) {
                showToast(context, "Error al programar reintento: ${e.message}")
            }
        }
    }

    private fun handleCancelSync(context: Context, intent: Intent) {
        val processId = intent.getStringExtra(NotificationService.EXTRA_PROCESS_ID)
        
        applicationScope.launch {
            try {
                val workScheduler = WorkScheduler(WorkManager.getInstance(context))
                val notificationHelper = NotificationHelper(context)
                val notificationService = NotificationService(context, notificationHelper)
                
                if (processId != null) {
                    workScheduler.cancelSyncWork(processId)
                    showToast(context, "Sincronización cancelada")
                } else {
                    workScheduler.cancelAllSyncWork()
                    showToast(context, "Todas las sincronizaciones canceladas")
                }
                
                notificationService.dismissSyncNotifications()
                
            } catch (e: Exception) {
                showToast(context, "Error al cancelar sincronización: ${e.message}")
            }
        }
    }

    private fun handleSyncAll(context: Context, intent: Intent) {
        val pendingCount = intent.getIntExtra(NotificationService.EXTRA_NOTIFICATION_ID, 0)
        
        applicationScope.launch {
            try {
                val workScheduler = WorkScheduler(WorkManager.getInstance(context))
                
                // This would typically get pending processes from repository
                // For now, we'll schedule a bulk sync
                val mockProcessIds = listOf("proc1", "proc2", "proc3").take(pendingCount)
                
                if (mockProcessIds.isNotEmpty()) {
                    workScheduler.scheduleBulkSync(
                        processIds = mockProcessIds,
                        delayMinutes = 1,
                        requiresNetwork = true
                    )
                    
                    val message = if (pendingCount == 1) {
                        "Iniciando sincronización de 1 proceso"
                    } else {
                        "Iniciando sincronización de $pendingCount procesos"
                    }
                    showToast(context, message)
                }
                
                // Dismiss the connectivity notification
                NotificationHelper(context).dismissAllSyncNotifications()
                
            } catch (e: Exception) {
                showToast(context, "Error al iniciar sincronización: ${e.message}")
            }
        }
    }

    private suspend fun scheduleFailedSyncs(context: Context) {
        // This would typically query the database for failed syncs
        // For now, we'll schedule a general bulk sync
        try {
            val workScheduler = WorkScheduler(WorkManager.getInstance(context))
            val mockFailedProcesses = listOf("failed_proc1", "failed_proc2")
            
            if (mockFailedProcesses.isNotEmpty()) {
                workScheduler.scheduleBulkSync(
                    processIds = mockFailedProcesses,
                    delayMinutes = 2,
                    requiresNetwork = true
                )
                
                showToast(context, "Reintentando ${mockFailedProcesses.size} procesos fallidos...")
            } else {
                showToast(context, "No hay procesos fallidos para reintentar")
            }
        } catch (e: Exception) {
            showToast(context, "Error al programar reintentos: ${e.message}")
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}