package ai.jaak.kyc.data.repository

import ai.jaak.kyc.data.local.dao.*
import ai.jaak.kyc.data.local.entity.*
import ai.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import ai.jaak.kyc.data.model.verify.VerifyRequest
import ai.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import ai.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import ai.jaak.kyc.data.network.JaakDBService
import ai.jaak.kyc.utils.Constants
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycSyncRepository @Inject constructor(
    private val kycProcessDao: KycProcessDao,
    private val kycSessionDao: KycSessionDao,
    private val kycVerifyDao: KycVerifyDao,
    private val kycOcrDao: KycOcrDao,
    private val kycLivenessDao: KycLivenessDao,
    private val kycOtoVerifyDao: KycOtoVerifyDao,
    private val kycFinishDao: KycFinishDao,
    private val jaakDBService: JaakDBService,
    private val gson: Gson = Gson()
) {
    
    data class SyncResult(
        val success: Boolean,
        val processedServices: Int,
        val totalServices: Int,
        val errors: List<String> = emptyList()
    )
    
    /**
     * Sync a single KYC process completely
     */
    suspend fun syncProcess(processId: String): SyncResult {
        val errors = mutableListOf<String>()
        var processedServices = 0
        val totalServices = 6 // session, verify, ocr, liveness, otoVerify, finish
        
        try {
            val processWithDetails = kycProcessDao.getProcessWithDetails(processId)
                ?: return SyncResult(false, 0, totalServices, listOf("Process not found"))
            
            // Update sync status
            kycProcessDao.updateSyncStatus(
                processId = processId,
                requiresSync = true,
                syncAttempts = processWithDetails.process.syncAttempts + 1,
                lastAttempt = System.currentTimeMillis()
            )
            kycProcessDao.updateOverallStatus(processId, KycProcessStatus.SYNCING)
            
            // 1. Sync Session
            if (processWithDetails.process.sessionStatus == ServiceStatus.COMPLETED) {
                val result = syncSessionService(processId)
                if (result.isSuccess) {
                    processedServices++
                } else {
                    errors.add("Session sync failed: ${result.exceptionOrNull()?.message}")
                }
            } else if (processWithDetails.process.sessionStatus == ServiceStatus.SYNCED) {
                processedServices++
            }
            
            // 2. Sync Verify
            if (processWithDetails.process.verifyStatus == ServiceStatus.COMPLETED) {
                val result = syncVerifyService(processId)
                if (result.isSuccess) {
                    processedServices++
                } else {
                    errors.add("Verify sync failed: ${result.exceptionOrNull()?.message}")
                }
            } else if (processWithDetails.process.verifyStatus == ServiceStatus.SYNCED) {
                processedServices++
            }
            
            // 3. Sync OCR
            if (processWithDetails.process.ocrStatus == ServiceStatus.COMPLETED) {
                val result = syncOcrService(processId)
                if (result.isSuccess) {
                    processedServices++
                } else {
                    errors.add("OCR sync failed: ${result.exceptionOrNull()?.message}")
                }
            } else if (processWithDetails.process.ocrStatus == ServiceStatus.SYNCED) {
                processedServices++
            }
            
            // 4. Sync Liveness
            if (processWithDetails.process.livenessStatus == ServiceStatus.COMPLETED) {
                val result = syncLivenessService(processId)
                if (result.isSuccess) {
                    processedServices++
                } else {
                    errors.add("Liveness sync failed: ${result.exceptionOrNull()?.message}")
                }
            } else if (processWithDetails.process.livenessStatus == ServiceStatus.SYNCED) {
                processedServices++
            }
            
            // 5. Sync OtoVerify
            if (processWithDetails.process.otoVerifyStatus == ServiceStatus.COMPLETED) {
                val result = syncOtoVerifyService(processId)
                if (result.isSuccess) {
                    processedServices++
                } else {
                    errors.add("OtoVerify sync failed: ${result.exceptionOrNull()?.message}")
                }
            } else if (processWithDetails.process.otoVerifyStatus == ServiceStatus.SYNCED) {
                processedServices++
            }
            
            // 6. Sync Finish
            if (processWithDetails.process.finishStatus == ServiceStatus.COMPLETED) {
                val result = syncFinishService(processId)
                if (result.isSuccess) {
                    processedServices++
                } else {
                    errors.add("Finish sync failed: ${result.exceptionOrNull()?.message}")
                }
            } else if (processWithDetails.process.finishStatus == ServiceStatus.SYNCED) {
                processedServices++
            }
            
            // Update final status
            if (processedServices == totalServices && errors.isEmpty()) {
                kycProcessDao.updateOverallStatus(processId, KycProcessStatus.COMPLETED)
                kycProcessDao.updateSyncStatus(processId, requiresSync = false, syncAttempts = 0, lastAttempt = null)
                return SyncResult(true, processedServices, totalServices)
            } else {
                kycProcessDao.updateOverallStatus(processId, KycProcessStatus.COMPLETED_OFFLINE)
                return SyncResult(false, processedServices, totalServices, errors)
            }
            
        } catch (e: Exception) {
            kycProcessDao.updateOverallStatus(processId, KycProcessStatus.FAILED)
            errors.add("Sync process failed: ${e.message}")
            return SyncResult(false, processedServices, totalServices, errors)
        }
    }
    
    /**
     * Sync all processes that require sync
     */
    suspend fun syncAllProcesses(): List<Pair<String, SyncResult>> {
        val results = mutableListOf<Pair<String, SyncResult>>()
        val processesToSync = kycProcessDao.getProcessesRequiringSync().first()
        
        for (process in processesToSync) {
            val result = syncProcess(process.id)
            results.add(process.id to result)
        }
        
        return results
    }
    
    private suspend fun syncSessionService(processId: String): Result<Unit> {
        return try {
            val sessionEntity = kycSessionDao.getSessionByProcessId(processId)
                ?: return Result.failure(Exception("Session data not found"))
            
            kycProcessDao.updateSessionStatus(processId, ServiceStatus.RETRYING, null, sessionEntity.retryCount)
            
            val response = jaakDBService.sessionApi(sessionEntity.shortKey, sessionEntity.originDevice)
            
            if (response.isSuccessful && response.body() != null) {
                val sessionResponse = response.body()!!
                
                // Update entity with response data
                val updatedEntity = sessionEntity.copy(
                    accessToken = sessionResponse.accessToken,
                    step = sessionResponse.step,
                    sessionId = sessionResponse.sessionId,
                    assets = gson.toJson(sessionResponse.assets),
                    document = sessionResponse.document,
                    status = ServiceStatus.SYNCED,
                    completedAt = System.currentTimeMillis(),
                    syncedAt = System.currentTimeMillis()
                )
                kycSessionDao.updateSession(updatedEntity)
                
                // Update process
                kycProcessDao.updateSessionStatus(processId, ServiceStatus.SYNCED, null, 0)
                kycProcessDao.updateSessionData(processId, sessionResponse.sessionId, sessionResponse.accessToken)
                
                Result.success(Unit)
            } else {
                kycSessionDao.incrementRetryCount(processId)
                kycProcessDao.updateSessionStatus(processId, ServiceStatus.FAILED, "Session API sync failed", sessionEntity.retryCount + 1)
                Result.failure(Exception("Session API sync failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateSessionStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun syncVerifyService(processId: String): Result<Unit> {
        return try {
            val verifyEntity = kycVerifyDao.getVerifyByProcessId(processId)
                ?: return Result.failure(Exception("Verify data not found"))
            
            kycProcessDao.updateVerifyStatus(processId, ServiceStatus.RETRYING, null, verifyEntity.retryCount)
            
            val verifyRequest = VerifyRequest(
                document = verifyEntity.imageFront,
                document2 = verifyEntity.imageBack,
                documentType = verifyEntity.documentType
            )
            
            val response = jaakDBService.verifyApi(Constants.TOKEN, verifyRequest)
            
            if (response.isSuccessful && response.body() != null) {
                val verifyResponse = response.body()!!
                
                val updatedEntity = verifyEntity.copy(
                    responseDocument = gson.toJson(verifyResponse.document),
                    responseDocumentType = verifyResponse.documentType,
                    eventId = verifyResponse.eventId,
                    processTime = verifyResponse.processTime,
                    requestId = verifyResponse.requestId,
                    responseState = gson.toJson(verifyResponse.state),
                    status = ServiceStatus.SYNCED,
                    completedAt = System.currentTimeMillis(),
                    syncedAt = System.currentTimeMillis()
                )
                kycVerifyDao.updateVerify(updatedEntity)
                
                kycProcessDao.updateVerifyStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                kycVerifyDao.incrementRetryCount(processId)
                kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, "Verify API sync failed", verifyEntity.retryCount + 1)
                Result.failure(Exception("Verify API sync failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun syncOcrService(processId: String): Result<Unit> {
        return try {
            val ocrEntity = kycOcrDao.getOcrByProcessId(processId)
                ?: return Result.failure(Exception("OCR data not found"))
            
            kycProcessDao.updateOcrStatus(processId, ServiceStatus.RETRYING, null, ocrEntity.retryCount)
            
            val ocrRequest = DocumentExtraBothRequest(
                documentFront = ocrEntity.documentFront,
                documentBack = ocrEntity.documentBack
            )
            
            val response = jaakDBService.ocrApi(Constants.TOKEN, ocrRequest)
            
            if (response.isSuccessful && response.body() != null) {
                val ocrResponse = response.body()!!
                
                val updatedEntity = ocrEntity.copy(
                    eventId = ocrResponse.eventId,
                    requestId = ocrResponse.requestId,
                    status = ocrResponse.status,
                    message = ocrResponse.message,
                    documentType = gson.toJson(ocrResponse.documentType),
                    documentData = gson.toJson(ocrResponse.documentData),
                    documentMetadata = ocrResponse.documentMetadata,
                    processingTime = ocrResponse.processingTime,
                    responseState = gson.toJson(ocrResponse.state),
                    serviceStatus = ServiceStatus.SYNCED,
                    completedAt = System.currentTimeMillis(),
                    syncedAt = System.currentTimeMillis()
                )
                kycOcrDao.updateOcr(updatedEntity)
                
                kycProcessDao.updateOcrStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                kycOcrDao.incrementRetryCount(processId)
                kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, "OCR API sync failed", ocrEntity.retryCount + 1)
                Result.failure(Exception("OCR API sync failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun syncLivenessService(processId: String): Result<Unit> {
        return try {
            val livenessEntity = kycLivenessDao.getLivenessByProcessId(processId)
                ?: return Result.failure(Exception("Liveness data not found"))
            
            kycProcessDao.updateLivenessStatus(processId, ServiceStatus.RETRYING, null, livenessEntity.retryCount)
            
            val livenessRequest = LivenessVerifyRequest(video = livenessEntity.video)
            
            val response = jaakDBService.livenessVerifyApi(Constants.TOKEN, livenessRequest)
            
            if (response.isSuccessful && response.body() != null) {
                val livenessResponse = response.body()!!
                
                val updatedEntity = livenessEntity.copy(
                    eventId = livenessResponse.eventId,
                    requestId = livenessResponse.requestId,
                    processTime = livenessResponse.processTime,
                    bestFrame = livenessResponse.bestFrame,
                    status = ServiceStatus.SYNCED,
                    completedAt = System.currentTimeMillis(),
                    syncedAt = System.currentTimeMillis()
                )
                kycLivenessDao.updateLiveness(updatedEntity)
                
                kycProcessDao.updateLivenessStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                kycLivenessDao.incrementRetryCount(processId)
                kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, "Liveness API sync failed", livenessEntity.retryCount + 1)
                Result.failure(Exception("Liveness API sync failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun syncOtoVerifyService(processId: String): Result<Unit> {
        return try {
            val otoVerifyEntity = kycOtoVerifyDao.getOtoVerifyByProcessId(processId)
                ?: return Result.failure(Exception("OtoVerify data not found"))
            
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.RETRYING, null, otoVerifyEntity.retryCount)
            
            val otoVerifyRequest = OtoVerifyRequest(
                image1 = otoVerifyEntity.image1,
                image2 = otoVerifyEntity.image2
            )
            
            val response = jaakDBService.otoVerifyApi(Constants.TOKEN, otoVerifyRequest)
            
            if (response.isSuccessful && response.body() != null) {
                val otoVerifyResponse = response.body()!!
                
                val updatedEntity = otoVerifyEntity.copy(
                    eventId = otoVerifyResponse.eventId,
                    requestId = otoVerifyResponse.requestId,
                    processTime = otoVerifyResponse.processTime.toLong(),
                    responseState = gson.toJson(otoVerifyResponse.state),
                    status = ServiceStatus.SYNCED,
                    completedAt = System.currentTimeMillis(),
                    syncedAt = System.currentTimeMillis()
                )
                kycOtoVerifyDao.updateOtoVerify(updatedEntity)
                
                kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                kycOtoVerifyDao.incrementRetryCount(processId)
                kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, "OtoVerify API sync failed", otoVerifyEntity.retryCount + 1)
                Result.failure(Exception("OtoVerify API sync failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun syncFinishService(processId: String): Result<Unit> {
        return try {
            val finishEntity = kycFinishDao.getFinishByProcessId(processId)
                ?: return Result.failure(Exception("Finish data not found"))
            
            kycProcessDao.updateFinishStatus(processId, ServiceStatus.RETRYING, null, finishEntity.retryCount)
            
            val response = jaakDBService.finishApi(finishEntity.accessToken)
            
            if (response.isSuccessful) {
                val updatedEntity = finishEntity.copy(
                    isFinished = true,
                    status = ServiceStatus.SYNCED,
                    completedAt = System.currentTimeMillis(),
                    syncedAt = System.currentTimeMillis()
                )
                kycFinishDao.updateFinish(updatedEntity)
                
                kycProcessDao.updateFinishStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                kycFinishDao.incrementRetryCount(processId)
                kycProcessDao.updateFinishStatus(processId, ServiceStatus.FAILED, "Finish API sync failed", finishEntity.retryCount + 1)
                Result.failure(Exception("Finish API sync failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateFinishStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
}