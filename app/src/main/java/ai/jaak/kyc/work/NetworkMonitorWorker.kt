package ai.jaak.kyc.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import ai.jaak.kyc.data.local.dao.KycProcessDao
import ai.jaak.kyc.data.local.dao.ServiceExecutionStateDao
import ai.jaak.kyc.data.local.entity.ServiceStatus
import ai.jaak.kyc.domain.service.KycSyncServiceSimple
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class NetworkMonitorWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val processDao: KycProcessDao,
    private val serviceExecutionStateDao: ServiceExecutionStateDao,
    private val syncService: KycSyncServiceSimple,
    private val workScheduler: WorkScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Check if network is available (would normally use ConnectivityManager)
            val isNetworkAvailable = checkNetworkConnectivity()
            
            if (isNetworkAvailable) {
                // Network is available, check for pending sync items
                val pendingProcesses = getPendingProcesses()
                val pendingSyncServices = getPendingSyncServices()
                
                if (pendingProcesses.isNotEmpty() || pendingSyncServices.isNotEmpty()) {
                    // Schedule sync work
                    scheduleAutoSync(pendingProcesses, pendingSyncServices)
                    
                    Result.success(createResultData(
                        "Network available",
                        "Scheduled sync for ${pendingProcesses.size} processes, ${pendingSyncServices.size} services"
                    ))
                } else {
                    Result.success(createResultData("Network available", "No pending sync items"))
                }
            } else {
                Result.success(createResultData("Network unavailable", "Will retry when network is restored"))
            }
        } catch (e: Exception) {
            Result.failure(createErrorData("Network monitoring failed: ${e.message}"))
        }
    }

    private fun checkNetworkConnectivity(): Boolean {
        // Simplified network check - in real implementation use ConnectivityManager
        return true // Assume network is available for demo
    }

    private suspend fun getPendingProcesses(): List<String> {
        return processDao.getProcessesRequiringSync().first().map { it.id }
    }

    private suspend fun getPendingSyncServices(): List<Pair<String, String>> {
        return serviceExecutionStateDao.getServicesRequiringSync()
            .filter { it.status == ServiceStatus.COMPLETED }
            .map { it.processId to it.serviceName.name }
    }

    private suspend fun scheduleAutoSync(pendingProcesses: List<String>, pendingSyncServices: List<Pair<String, String>>) {
        // Schedule bulk sync if there are many pending processes
        if (pendingProcesses.size >= 3) {
            workScheduler.scheduleBulkSync(
                processIds = pendingProcesses,
                delayMinutes = 1 // Start immediately
            )
        } else {
            // Schedule individual process syncs
            pendingProcesses.forEach { processId ->
                workScheduler.scheduleProcessSync(
                    processId = processId,
                    delayMinutes = 1
                )
            }
        }

        // Schedule individual service syncs for high-priority services
        pendingSyncServices.take(5).forEach { (processId, serviceType) ->
            workScheduler.scheduleServiceSync(
                processId = processId,
                serviceType = serviceType,
                delayMinutes = 2
            )
        }
    }

    private fun createResultData(status: String, message: String): Data {
        return Data.Builder()
            .putString("status", status)
            .putString("message", message)
            .putLong("timestamp", System.currentTimeMillis())
            .build()
    }

    private fun createErrorData(message: String): Data {
        return Data.Builder()
            .putString("result", "error")
            .putString("message", message)
            .putLong("timestamp", System.currentTimeMillis())
            .build()
    }
}