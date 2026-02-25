package ai.jaak.kyc.work

import androidx.lifecycle.LiveData
import androidx.work.*
import ai.jaak.kyc.data.local.entity.KycServiceType
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    
    companion object {
        private const val TAG_SYNC_SERVICE = "sync_service"
        private const val TAG_SYNC_PROCESS = "sync_process" 
        private const val TAG_SYNC_BULK = "sync_bulk"
        private const val TAG_NETWORK_MONITOR = "network_monitor"
        private const val TAG_CLEANUP = "cleanup"
        
        private const val UNIQUE_NETWORK_MONITOR = "unique_network_monitor"
        private const val UNIQUE_DAILY_CLEANUP = "unique_daily_cleanup"
    }

    fun scheduleServiceSync(
        processId: String,
        serviceType: String,
        delayMinutes: Long = 0,
        requiresNetwork: Boolean = true
    ): String {
        val workName = "sync_service_${processId}_$serviceType"
        
        val inputData = Data.Builder()
            .putString(SyncWorker.KEY_PROCESS_ID, processId)
            .putString(SyncWorker.KEY_SERVICE_TYPE, serviceType)
            .putString(SyncWorker.KEY_SYNC_TYPE, SyncWorker.SYNC_TYPE_INDIVIDUAL)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requiresNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(TAG_SYNC_SERVICE)
            .addTag("process_$processId")
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return workRequest.id.toString()
    }

    fun scheduleProcessSync(
        processId: String,
        delayMinutes: Long = 0,
        requiresNetwork: Boolean = true
    ): String {
        val workName = "sync_process_$processId"
        
        val inputData = Data.Builder()
            .putString(SyncWorker.KEY_PROCESS_ID, processId)
            .putString(SyncWorker.KEY_SYNC_TYPE, SyncWorker.SYNC_TYPE_PROCESS)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requiresNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(TAG_SYNC_PROCESS)
            .addTag("process_$processId")
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return workRequest.id.toString()
    }

    fun scheduleBulkSync(
        processIds: List<String>,
        delayMinutes: Long = 0,
        requiresNetwork: Boolean = true
    ): String {
        val workName = "bulk_sync_${System.currentTimeMillis()}"
        
        val inputData = Data.Builder()
            .putString(SyncWorker.KEY_SYNC_TYPE, SyncWorker.SYNC_TYPE_BULK)
            .putStringArray("process_ids", processIds.toTypedArray())
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requiresNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(processIds.size > 10) // Require charging for large bulk operations
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(TAG_SYNC_BULK)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return workRequest.id.toString()
    }

    fun scheduleNetworkMonitoring() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NetworkMonitorWorker>(
            15, TimeUnit.MINUTES // Check every 15 minutes
        )
            .setConstraints(constraints)
            .addTag(TAG_NETWORK_MONITOR)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_NETWORK_MONITOR,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun scheduleDailyCleanup(
        retentionDays: Int = 30,
        hourOfDay: Int = 2 // 2 AM
    ) {
        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            
            // If the time has passed today, schedule for tomorrow
            if (timeInMillis <= currentTime) {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val delayMs = calendar.timeInMillis - currentTime
        
        val inputData = Data.Builder()
            .putString(CleanupWorker.KEY_CLEANUP_TYPE, CleanupWorker.CLEANUP_TYPE_ALL)
            .putInt(CleanupWorker.KEY_RETENTION_DAYS, retentionDays)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresCharging(true) // Only run when charging
            .setRequiresDeviceIdle(true) // Only run when device is idle
            .build()

        val workRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            1, TimeUnit.DAYS
        )
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(TAG_CLEANUP)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_DAILY_CLEANUP,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun scheduleImmediateSync(processId: String, serviceType: KycServiceType? = null): String {
        return if (serviceType != null) {
            scheduleServiceSync(processId, serviceType.name, delayMinutes = 0)
        } else {
            scheduleProcessSync(processId, delayMinutes = 0)
        }
    }

    fun cancelSyncWork(processId: String) {
        workManager.cancelAllWorkByTag("process_$processId")
    }

    fun cancelAllSyncWork() {
        workManager.cancelAllWorkByTag(TAG_SYNC_SERVICE)
        workManager.cancelAllWorkByTag(TAG_SYNC_PROCESS)
        workManager.cancelAllWorkByTag(TAG_SYNC_BULK)
    }

    fun getWorkInfo(workId: String): LiveData<WorkInfo?> {
        return workManager.getWorkInfoByIdLiveData(java.util.UUID.fromString(workId))
    }

    fun getWorkInfosByTag(tag: String): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData(tag)
    }

    fun getSyncWorkStatus(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData(TAG_SYNC_SERVICE)
    }

    fun getNetworkMonitorStatus(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData(TAG_NETWORK_MONITOR)
    }

    fun pruneFinishedWork() {
        workManager.pruneWork()
    }
}