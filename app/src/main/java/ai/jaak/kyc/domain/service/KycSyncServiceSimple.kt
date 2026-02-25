package ai.jaak.kyc.domain.service

import ai.jaak.kyc.data.local.entity.KycServiceType
import ai.jaak.kyc.data.local.entity.ServiceStatus
import ai.jaak.kyc.data.repository.KycOfflineRepository
import ai.jaak.kyc.data.model.verify.VerifyRequest
import ai.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import ai.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import ai.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import ai.jaak.kyc.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycSyncServiceSimple @Inject constructor(
    private val repository: KycOfflineRepository,
    private val notificationHelper: NotificationHelper
) {
    
    suspend fun syncIndividualService(processId: String, serviceType: KycServiceType): SyncResult {
        android.util.Log.d("KycSyncServiceSimple", "=== syncIndividualService() REAL ===")
        android.util.Log.d("KycSyncServiceSimple", "Syncing REAL service - processId: $processId, serviceType: ${serviceType.displayName}")
        
        return try {
            // Obtener el proceso actual y sus datos
            val processWithDetails = repository.getProcessWithDetails(processId)
            if (processWithDetails == null) {
                android.util.Log.e("KycSyncServiceSimple", "Process not found: $processId")
                return SyncResult.Error("Process not found: $processId")
            }
            
            android.util.Log.d("KycSyncServiceSimple", "Found process ${processWithDetails.process.shortKey}")
            
            // Ejecutar el servicio específico usando los métodos reales del repositorio
            val result = when (serviceType) {
                KycServiceType.SESSION -> {
                    android.util.Log.d("KycSyncServiceSimple", "Executing REAL Session service...")
                    repository.executeSession(processId, processWithDetails.process.shortKey)
                }
                
                KycServiceType.VERIFY -> {
                    android.util.Log.d("KycSyncServiceSimple", "Executing REAL Verify service...")
                    val verifyData = processWithDetails.verify
                    if (verifyData != null) {
                        val verifyRequest = VerifyRequest(
                            document = verifyData.imageFront,
                            document2 = verifyData.imageBack,
                            documentType = verifyData.documentType
                        )
                        repository.executeVerify(processId, verifyRequest)
                    } else {
                        android.util.Log.w("KycSyncServiceSimple", "No verify data found for process $processId")
                        Result.failure(Exception("No verify data available for sync"))
                    }
                }
                
                KycServiceType.OCR -> {
                    android.util.Log.d("KycSyncServiceSimple", "Executing REAL OCR service...")
                    val ocrData = processWithDetails.ocr
                    if (ocrData != null) {
                        // Paths are already stored in BD, pass them to repository
                        val ocrRequest = ai.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Request(
                            imageFront = ocrData.documentFront, // Path del archivo
                            imageBack = ocrData.documentBack, // Path del archivo
                            allowedCountries = listOf("MEX", "COL")
                        )
                        repository.executeOcr(processId, ocrRequest)
                    } else {
                        android.util.Log.w("KycSyncServiceSimple", "No OCR data found for process $processId")
                        Result.failure(Exception("No OCR data available for sync"))
                    }
                }
                
                KycServiceType.LIVENESS -> {
                    android.util.Log.d("KycSyncServiceSimple", "Executing REAL Liveness service...")
                    val livenessData = processWithDetails.liveness
                    if (livenessData != null) {
                        val livenessRequest = LivenessVerifyRequest(
                            video = livenessData.video
                        )
                        repository.executeLiveness(processId, livenessRequest)
                    } else {
                        android.util.Log.w("KycSyncServiceSimple", "No liveness data found for process $processId")
                        Result.failure(Exception("No liveness data available for sync"))
                    }
                }
                
                KycServiceType.OTO_VERIFY -> {
                    android.util.Log.d("KycSyncServiceSimple", "Executing REAL OtoVerify service...")
                    val otoVerifyData = processWithDetails.otoVerify
                    if (otoVerifyData != null) {
                        val otoVerifyRequest = OtoVerifyRequest(
                            image1 = otoVerifyData.image1,
                            image2 = otoVerifyData.image2
                        )
                        repository.executeOtoVerify(processId, otoVerifyRequest)
                    } else {
                        android.util.Log.w("KycSyncServiceSimple", "No otoVerify data found for process $processId")
                        Result.failure(Exception("No otoVerify data available for sync"))
                    }
                }
                
                KycServiceType.FINISH -> {
                    android.util.Log.d("KycSyncServiceSimple", "Executing REAL Finish service...")
                    repository.executeFinish(processId)
                }
            }
            
            // Evaluar el resultado
            if (result.isSuccess) {
                android.util.Log.d("KycSyncServiceSimple", "${serviceType.displayName} REAL service completed successfully!")
                SyncResult.Success("${serviceType.displayName} synced successfully with real data")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                android.util.Log.e("KycSyncServiceSimple", "${serviceType.displayName} REAL service failed: $error")
                SyncResult.Error("Failed to sync ${serviceType.displayName}: $error")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("KycSyncServiceSimple", "${serviceType.displayName} REAL sync failed with exception: ${e.message}")
            SyncResult.Error("Failed to sync ${serviceType.displayName}: ${e.message}")
        }
    }
    
    suspend fun syncAllServicesForProcess(processId: String): Flow<SyncProgress> = flow {
        val services = KycServiceType.values()
        val totalServices = services.size
        
        emit(SyncProgress.Started(totalServices))
        
        var syncedCount = 0
        var failedCount = 0
        
        for ((index, service) in services.withIndex()) {
            emit(SyncProgress.Syncing(service, syncedCount, totalServices))
            
            val result = syncIndividualService(processId, service)
            
            when (result) {
                is SyncResult.Success -> {
                    syncedCount++
                    emit(SyncProgress.ServiceCompleted(
                        service, 
                        true, 
                        result.message, 
                        syncedCount, 
                        totalServices
                    ))
                }
                is SyncResult.Error -> {
                    failedCount++
                    emit(SyncProgress.ServiceCompleted(
                        service, 
                        false, 
                        result.message, 
                        syncedCount, 
                        totalServices
                    ))
                }
            }
        }
        
        emit(SyncProgress.Completed(syncedCount, failedCount, 
            if (failedCount == 0) "All services synced successfully" 
            else "$syncedCount synced, $failedCount failed"))
    }
    
    suspend fun getBulkSyncCandidates(): List<BulkSyncCandidate> {
        android.util.Log.d("KycSyncServiceSimple", "=== getBulkSyncCandidates() CALLED ===")
        
        return try {
            // Obtener procesos reales que requieren sincronización
            val allProcesses = repository.getAllProcesses().first()
            val processesToSync = allProcesses.filter { it.requiresSync == true }
            
            android.util.Log.d("KycSyncServiceSimple", "Found ${processesToSync.size} processes requiring sync from ${allProcesses.size} total processes")
            
            val candidates = processesToSync.map { process ->
                // Contar servicios que están COMPLETED (offline) pero no SYNCED
                var servicesToSync = 0
                if (process.sessionStatus == ServiceStatus.COMPLETED) servicesToSync++
                if (process.verifyStatus == ServiceStatus.COMPLETED) servicesToSync++
                if (process.ocrStatus == ServiceStatus.COMPLETED) servicesToSync++
                if (process.livenessStatus == ServiceStatus.COMPLETED) servicesToSync++
                if (process.otoVerifyStatus == ServiceStatus.COMPLETED) servicesToSync++
                if (process.finishStatus == ServiceStatus.COMPLETED) servicesToSync++
                
                android.util.Log.d("KycSyncServiceSimple", 
                    "Process ${process.shortKey}: $servicesToSync services to sync" +
                    " (Session:${process.sessionStatus}, Verify:${process.verifyStatus}, OCR:${process.ocrStatus}, " +
                    "Liveness:${process.livenessStatus}, OtoVerify:${process.otoVerifyStatus}, Finish:${process.finishStatus})")
                
                BulkSyncCandidate(
                    processId = process.id,
                    shortKey = process.shortKey,
                    servicesToSync = servicesToSync,
                    createdAt = process.createdAt,
                    lastSyncAttempt = process.lastSyncAttempt
                )
            }
            
            android.util.Log.d("KycSyncServiceSimple", "Returning ${candidates.size} REAL candidates: ${candidates.map { "${it.shortKey}(${it.servicesToSync} services)" }}")
            candidates
        } catch (e: Exception) {
            android.util.Log.e("KycSyncServiceSimple", "Error getting bulk sync candidates: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun executeBulkSync(processIds: List<String>): Flow<BulkSyncProgress> = flow {
        val totalProcesses = processIds.size
        var processedCount = 0
        var successfulProcesses = 0
        var failedProcesses = 0
        
        emit(BulkSyncProgress.Started(totalProcesses))
        
        for (processId in processIds) {
            emit(BulkSyncProgress.ProcessStarted("Process $processId", processedCount + 1, totalProcesses))
            
            var processSuccess = true
            
            // Simulate process sync by syncing its services
            syncAllServicesForProcess(processId).collect { syncProgress ->
                when (syncProgress) {
                    is SyncProgress.Completed -> {
                        if (syncProgress.failedCount > 0) {
                            processSuccess = false
                        }
                    }
                    else -> { /* Handle other progress types if needed */ }
                }
            }
            
            processedCount++
            
            if (processSuccess) {
                successfulProcesses++
                emit(BulkSyncProgress.ProcessCompleted(
                    "Process $processId", 
                    true, 
                    "Process synced successfully", 
                    processedCount, 
                    totalProcesses
                ))
            } else {
                failedProcesses++
                emit(BulkSyncProgress.ProcessCompleted(
                    "Process $processId", 
                    false, 
                    "Some services failed to sync", 
                    processedCount, 
                    totalProcesses
                ))
            }
        }
        
        emit(BulkSyncProgress.AllCompleted(
            successfulProcesses, 
            failedProcesses,
            if (failedProcesses == 0) "All processes synced successfully" 
            else "$successfulProcesses synced, $failedProcesses failed"
        ))
    }
}

// Keep the same sealed classes and data classes
sealed class SyncResult(val isSuccess: Boolean, val errorMessage: String? = null) {
    data class Success(val message: String) : SyncResult(true)
    data class Error(val message: String) : SyncResult(false, message)
}

sealed class SyncProgress {
    data class Started(val totalServices: Int) : SyncProgress()
    data class Syncing(val serviceName: KycServiceType, val completed: Int, val total: Int) : SyncProgress()
    data class ServiceCompleted(
        val serviceName: KycServiceType, 
        val success: Boolean, 
        val message: String, 
        val completed: Int, 
        val total: Int
    ) : SyncProgress()
    data class Completed(val syncedCount: Int, val failedCount: Int, val message: String) : SyncProgress()
}

sealed class BulkSyncProgress {
    data class Started(val totalProcesses: Int) : BulkSyncProgress()
    data class ProcessStarted(val shortKey: String, val current: Int, val total: Int) : BulkSyncProgress()
    data class ProcessCompleted(
        val shortKey: String, 
        val success: Boolean, 
        val message: String, 
        val completed: Int, 
        val total: Int
    ) : BulkSyncProgress()
    data class AllCompleted(val successCount: Int, val failedCount: Int, val message: String) : BulkSyncProgress()
}

data class BulkSyncCandidate(
    val processId: String,
    val shortKey: String,
    val servicesToSync: Int,
    val createdAt: Long,
    val lastSyncAttempt: Long?
)

data class SyncStats(
    val processesRequiringSync: Int,
    val servicesRequiringSync: Int,
    val isNetworkAvailable: Boolean,
    val lastSyncCheck: Long
)