package com.jaak.kyc.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.jaak.kyc.data.local.dao.*
import com.jaak.kyc.data.local.entity.*
import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.model.verify.VerifyRequest
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.data.network.JaakDBService
import com.jaak.kyc.utils.Constants
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycOfflineRepository @Inject constructor(
    private val context: Context,
    private val kycProcessDao: KycProcessDao,
    private val kycSessionDao: KycSessionDao,
    private val kycVerifyDao: KycVerifyDao,
    private val kycOcrDao: KycOcrDao,
    private val kycLivenessDao: KycLivenessDao,
    private val kycOtoVerifyDao: KycOtoVerifyDao,
    private val kycFinishDao: KycFinishDao,
    private val serviceExecutionStateDao: ServiceExecutionStateDao,
    private val jaakDBService: JaakDBService,
    private val gson: Gson = Gson()
) {
    
    // CONNECTIVITY MANAGEMENT
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    // MIXTO INTELIGENTE - Nueva lógica de ejecución
    private suspend fun hasValidToken(processId: String): Boolean {
        val process = kycProcessDao.getProcessById(processId)
        
        // ✅ Token válido si:
        // 1. El proceso tiene token (del Session online exitoso) O
        // 2. Constants.TOKEN tiene valor (fallback global)
        return !process?.accessToken.isNullOrEmpty() || !Constants.TOKEN.isNullOrEmpty()
    }
    
    private suspend fun shouldForceOfflineMode(processId: String): Boolean {
        val process = kycProcessDao.getProcessById(processId)
        
        val sessionIsCompleted = process?.sessionStatus == ServiceStatus.COMPLETED
        val noAccessToken = process?.accessToken.isNullOrEmpty()
        val result = sessionIsCompleted && noAccessToken
        
        // 🔍 DEBUG: Ver el estado del proceso
        android.util.Log.d("KycOfflineRepository", """
            shouldForceOfflineMode Debug:
            - processId: $processId
            - process found: ${process != null}
            - sessionStatus: ${process?.sessionStatus}
            - accessToken: ${if (process?.accessToken.isNullOrEmpty()) "NULL/EMPTY" else "HAS_VALUE"}
            - sessionIsCompleted: $sessionIsCompleted
            - noAccessToken: $noAccessToken
            - shouldForceOffline: $result
        """.trimIndent())
        
        // 🚨 EXCEPCIÓN CRÍTICA: Si Session fue offline (sin token) → Forzar todo offline
        // Solo forzar offline si Session está COMPLETED (offline) Y no hay token
        return result
    }
    
    private suspend fun canExecuteOnline(processId: String): Boolean {
        val forceOffline = shouldForceOfflineMode(processId)
        val hasInternet = isNetworkAvailable()
        val hasToken = hasValidToken(processId)
        
        // 🔍 DEBUG: Agregar logs para entender qué está pasando
        android.util.Log.d("KycOfflineRepository", """
            canExecuteOnline Debug:
            - processId: $processId
            - shouldForceOfflineMode: $forceOffline
            - isNetworkAvailable: $hasInternet
            - hasValidToken: $hasToken
            - Result: ${!forceOffline && hasInternet && hasToken}
        """.trimIndent())
        
        // ✅ Puede ejecutar online si:
        // 1. NO está en modo forzado offline Y
        // 2. Hay internet Y  
        // 3. Tiene token válido
        return !forceOffline && hasInternet && hasToken
    }
    
    // PROCESS MANAGEMENT
    suspend fun createKycProcess(shortKey: String): String {
        val process = KycProcessEntity(
            shortKey = shortKey,
            overallStatus = KycProcessStatus.PENDING,
            requiresSync = false
        )
        
        kycProcessDao.insertProcess(process)
        return process.id
    }
    
    fun getAllProcesses(): Flow<List<KycProcessEntity>> {
        return kycProcessDao.getAllProcesses()
    }
    
    fun getAllProcessesWithDetails(): Flow<List<KycProcessWithDetails>> {
        return kycProcessDao.getAllProcessesWithDetails()
    }
    
    suspend fun getProcessWithDetails(processId: String): KycProcessWithDetails? {
        return kycProcessDao.getProcessWithDetails(processId)
    }
    
    fun getProcessesRequiringSync(): Flow<List<KycProcessEntity>> {
        return kycProcessDao.getProcessesRequiringSync()
    }
    
    suspend fun getCountRequiringSync(): Int {
        return kycProcessDao.getCountRequiringSync()
    }
    
    // SESSION SERVICE
    suspend fun executeSession(processId: String, shortKey: String): Result<Unit> {
        return try {
            val hasInternet = isNetworkAvailable()
            
            // 🔍 DEBUG: Session execution
            android.util.Log.d("KycOfflineRepository", """
                executeSession Debug:
                - processId: $processId
                - shortKey: $shortKey
                - hasInternet: $hasInternet
                - Will execute: ${if (hasInternet) "ONLINE" else "OFFLINE"}
            """.trimIndent())
            
            if (hasInternet) {
                // Online execution
                executeSessionOnline(processId, shortKey)
            } else {
                // Offline execution - just store data for later sync
                executeSessionOffline(processId, shortKey)
            }
        } catch (e: Exception) {
            kycProcessDao.updateSessionStatus(
                processId = processId,
                status = ServiceStatus.FAILED,
                error = e.message,
                retryCount = 0
            )
            Result.failure(e)
        }
    }
    
    private suspend fun executeSessionOnline(processId: String, shortKey: String): Result<Unit> {
        return try {
            kycProcessDao.updateSessionStatus(processId, ServiceStatus.RETRYING, null, 0)
            
            val response = jaakDBService.sessionApi(shortKey, Constants.ORIGIN_DEVICE)
            
            if (response.isSuccessful && response.body() != null) {
                val sessionResponse = response.body()!!
                
                // Store in local database
                val sessionEntity = KycSessionEntity(
                    processId = processId,
                    shortKey = shortKey,
                    accessToken = sessionResponse.accessToken,
                    step = sessionResponse.step,
                    sessionId = sessionResponse.sessionId,
                    assets = gson.toJson(sessionResponse.assets),
                    document = sessionResponse.document,
                    status = ServiceStatus.SYNCED
                )
                kycSessionDao.insertSession(sessionEntity)
                
                // Update process
                kycProcessDao.updateSessionStatus(processId, ServiceStatus.SYNCED, null, 0)
                kycProcessDao.updateSessionData(processId, sessionResponse.sessionId, sessionResponse.accessToken)
                
                Result.success(Unit)
            } else {
                kycProcessDao.updateSessionStatus(processId, ServiceStatus.FAILED, "Session API failed", 0)
                Result.failure(Exception("Session API failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateSessionStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeSessionOffline(processId: String, shortKey: String): Result<Unit> {
        // Store session data for later sync
        val sessionEntity = KycSessionEntity(
            processId = processId,
            shortKey = shortKey,
            status = ServiceStatus.COMPLETED
        )
        kycSessionDao.insertSession(sessionEntity)
        
        // Update process status
        kycProcessDao.updateSessionStatus(processId, ServiceStatus.COMPLETED, null, 0)
        kycProcessDao.updateSyncStatus(processId, requiresSync = true, syncAttempts = 0, lastAttempt = null)
        
        return Result.success(Unit)
    }
    
    // VERIFY SERVICE
    suspend fun executeVerify(processId: String, verifyRequest: VerifyRequest): Result<Unit> {
        return try {
            if (canExecuteOnline(processId)) {
                // ✅ Modo online: Hay internet y token válido
                executeVerifyOnline(processId, verifyRequest)
            } else {
                // 📱 Modo offline: Sin internet O Session fue offline
                executeVerifyOffline(processId, verifyRequest)
            }
        } catch (e: Exception) {
            kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeVerifyOnline(processId: String, verifyRequest: VerifyRequest): Result<Unit> {
        return try {
            kycProcessDao.updateVerifyStatus(processId, ServiceStatus.RETRYING, null, 0)
            
            // 🔑 Usar token del proceso o fallback a Constants.TOKEN
            val process = kycProcessDao.getProcessById(processId)
            val accessToken = process?.accessToken ?: Constants.TOKEN
            
            val response = jaakDBService.verifyApi(accessToken, verifyRequest)
            
            if (response.isSuccessful && response.body() != null) {
                val verifyResponse = response.body()!!
                
                val verifyEntity = KycVerifyEntity(
                    processId = processId,
                    imageFront = verifyRequest.document,
                    imageBack = verifyRequest.document2,
                    documentType = verifyRequest.documentType,
                    responseDocument = gson.toJson(verifyResponse.document),
                    responseDocumentType = verifyResponse.documentType,
                    eventId = verifyResponse.eventId,
                    processTime = verifyResponse.processTime,
                    requestId = verifyResponse.requestId,
                    responseState = gson.toJson(verifyResponse.state),
                    status = ServiceStatus.SYNCED
                )
                kycVerifyDao.insertVerify(verifyEntity)
                
                kycProcessDao.updateVerifyStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, "Verify API failed", 0)
                Result.failure(Exception("Verify API failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeVerifyOffline(processId: String, verifyRequest: VerifyRequest): Result<Unit> {
        val verifyEntity = KycVerifyEntity(
            processId = processId,
            imageFront = verifyRequest.document,
            imageBack = verifyRequest.document2,
            documentType = verifyRequest.documentType,
            status = ServiceStatus.COMPLETED
        )
        kycVerifyDao.insertVerify(verifyEntity)
        
        kycProcessDao.updateVerifyStatus(processId, ServiceStatus.COMPLETED, null, 0)
        kycProcessDao.updateSyncStatus(processId, requiresSync = true, syncAttempts = 0, lastAttempt = null)
        
        return Result.success(Unit)
    }
    
    // OCR SERVICE
    suspend fun executeOcr(processId: String, ocrRequest: DocumentExtraBothRequest): Result<Unit> {
        return try {
            if (canExecuteOnline(processId)) {
                // ✅ Modo online: Hay internet y token válido
                executeOcrOnline(processId, ocrRequest)
            } else {
                // 📱 Modo offline: Sin internet O Session fue offline
                executeOcrOffline(processId, ocrRequest)
            }
        } catch (e: Exception) {
            kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeOcrOnline(processId: String, ocrRequest: DocumentExtraBothRequest): Result<Unit> {
        return try {
            kycProcessDao.updateOcrStatus(processId, ServiceStatus.RETRYING, null, 0)
            
            // 🔑 Usar token del proceso o fallback a Constants.TOKEN
            val process = kycProcessDao.getProcessById(processId)
            val accessToken = process?.accessToken ?: Constants.TOKEN
            
            val response = jaakDBService.ocrApi(accessToken, ocrRequest)
            
            if (response.isSuccessful && response.body() != null) {
                val ocrResponse = response.body()!!
                
                val ocrEntity = KycOcrEntity(
                    processId = processId,
                    documentFront = ocrRequest.documentFront,
                    documentBack = ocrRequest.documentBack,
                    eventId = ocrResponse.eventId,
                    requestId = ocrResponse.requestId,
                    status = ocrResponse.status,
                    message = ocrResponse.message,
                    documentType = gson.toJson(ocrResponse.documentType),
                    documentData = gson.toJson(ocrResponse.documentData),
                    documentMetadata = ocrResponse.documentMetadata,
                    processingTime = ocrResponse.processingTime,
                    responseState = gson.toJson(ocrResponse.state),
                    serviceStatus = ServiceStatus.SYNCED
                )
                kycOcrDao.insertOcr(ocrEntity)
                
                kycProcessDao.updateOcrStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, "OCR API failed", 0)
                Result.failure(Exception("OCR API failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeOcrOffline(processId: String, ocrRequest: DocumentExtraBothRequest): Result<Unit> {
        val ocrEntity = KycOcrEntity(
            processId = processId,
            documentFront = ocrRequest.documentFront,
            documentBack = ocrRequest.documentBack,
            serviceStatus = ServiceStatus.COMPLETED
        )
        kycOcrDao.insertOcr(ocrEntity)
        
        kycProcessDao.updateOcrStatus(processId, ServiceStatus.COMPLETED, null, 0)
        kycProcessDao.updateSyncStatus(processId, requiresSync = true, syncAttempts = 0, lastAttempt = null)
        
        return Result.success(Unit)
    }
    
    // LIVENESS SERVICE
    suspend fun executeLiveness(processId: String, livenessRequest: LivenessVerifyRequest): Result<Unit> {
        return try {
            if (canExecuteOnline(processId)) {
                // ✅ Modo online: Hay internet y token válido
                executeLivenessOnline(processId, livenessRequest)
            } else {
                // 📱 Modo offline: Sin internet O Session fue offline
                executeLivenessOffline(processId, livenessRequest)
            }
        } catch (e: Exception) {
            kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeLivenessOnline(processId: String, livenessRequest: LivenessVerifyRequest): Result<Unit> {
        return try {
            kycProcessDao.updateLivenessStatus(processId, ServiceStatus.RETRYING, null, 0)
            
            // 🔑 Usar token del proceso o fallback a Constants.TOKEN
            val process = kycProcessDao.getProcessById(processId)
            val accessToken = process?.accessToken ?: Constants.TOKEN
            
            val response = jaakDBService.livenessVerifyApi(accessToken, livenessRequest)
            
            if (response.isSuccessful && response.body() != null) {
                val livenessResponse = response.body()!!
                
                val livenessEntity = KycLivenessEntity(
                    processId = processId,
                    video = livenessRequest.video,
                    eventId = livenessResponse.eventId,
                    requestId = livenessResponse.requestId,
                    processTime = livenessResponse.processTime,
                    bestFrame = livenessResponse.bestFrame,
                    status = ServiceStatus.SYNCED
                )
                kycLivenessDao.insertLiveness(livenessEntity)
                
                kycProcessDao.updateLivenessStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, "Liveness API failed", 0)
                Result.failure(Exception("Liveness API failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeLivenessOffline(processId: String, livenessRequest: LivenessVerifyRequest): Result<Unit> {
        val livenessEntity = KycLivenessEntity(
            processId = processId,
            video = livenessRequest.video,
            status = ServiceStatus.COMPLETED
        )
        kycLivenessDao.insertLiveness(livenessEntity)
        
        kycProcessDao.updateLivenessStatus(processId, ServiceStatus.COMPLETED, null, 0)
        kycProcessDao.updateSyncStatus(processId, requiresSync = true, syncAttempts = 0, lastAttempt = null)
        
        return Result.success(Unit)
    }
    
    // OTO VERIFY SERVICE  
    suspend fun executeOtoVerify(processId: String, otoVerifyRequest: OtoVerifyRequest): Result<Unit> {
        return try {
            if (canExecuteOnline(processId)) {
                // ✅ Modo online: Hay internet y token válido
                executeOtoVerifyOnline(processId, otoVerifyRequest)
            } else {
                // 📱 Modo offline: Sin internet O Session fue offline
                executeOtoVerifyOffline(processId, otoVerifyRequest)
            }
        } catch (e: Exception) {
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeOtoVerifyOnline(processId: String, otoVerifyRequest: OtoVerifyRequest): Result<Unit> {
        return try {
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.RETRYING, null, 0)
            
            // 🔑 Usar token del proceso o fallback a Constants.TOKEN
            val process = kycProcessDao.getProcessById(processId)
            val accessToken = process?.accessToken ?: Constants.TOKEN
            
            val response = jaakDBService.otoVerifyApi(accessToken, otoVerifyRequest)
            
            if (response.isSuccessful && response.body() != null) {
                val otoVerifyResponse = response.body()!!
                
                val otoVerifyEntity = KycOtoVerifyEntity(
                    processId = processId,
                    image1 = otoVerifyRequest.image1,
                    image2 = otoVerifyRequest.image2,
                    eventId = otoVerifyResponse.eventId,
                    requestId = otoVerifyResponse.requestId,
                    processTime = otoVerifyResponse.processTime.toLong(),
                    responseState = gson.toJson(otoVerifyResponse.state),
                    status = ServiceStatus.SYNCED
                )
                kycOtoVerifyDao.insertOtoVerify(otoVerifyEntity)
                
                kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, "OtoVerify API failed", 0)
                Result.failure(Exception("OtoVerify API failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeOtoVerifyOffline(processId: String, otoVerifyRequest: OtoVerifyRequest): Result<Unit> {
        val otoVerifyEntity = KycOtoVerifyEntity(
            processId = processId,
            image1 = otoVerifyRequest.image1,
            image2 = otoVerifyRequest.image2,
            status = ServiceStatus.COMPLETED
        )
        kycOtoVerifyDao.insertOtoVerify(otoVerifyEntity)
        
        kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.COMPLETED, null, 0)
        kycProcessDao.updateSyncStatus(processId, requiresSync = true, syncAttempts = 0, lastAttempt = null)
        
        return Result.success(Unit)
    }
    
    // FINISH SERVICE
    suspend fun executeFinish(processId: String): Result<Unit> {
        return try {
            if (canExecuteOnline(processId)) {
                // ✅ Modo online: Hay internet y token válido
                val process = kycProcessDao.getProcessById(processId)
                val accessToken = process?.accessToken ?: Constants.TOKEN
                executeFinishOnline(processId, accessToken)
            } else {
                // 📱 Modo offline: Sin internet O Session fue offline
                val process = kycProcessDao.getProcessById(processId)
                val accessToken = process?.accessToken ?: Constants.TOKEN
                executeFinishOffline(processId, accessToken)
            }
        } catch (e: Exception) {
            kycProcessDao.updateFinishStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeFinishOnline(processId: String, accessToken: String): Result<Unit> {
        return try {
            kycProcessDao.updateFinishStatus(processId, ServiceStatus.RETRYING, null, 0)
            
            val response = jaakDBService.finishApi(accessToken)
            
            if (response.isSuccessful) {
                val finishEntity = KycFinishEntity(
                    processId = processId,
                    accessToken = accessToken,
                    isFinished = true,
                    status = ServiceStatus.SYNCED
                )
                kycFinishDao.insertFinish(finishEntity)
                
                kycProcessDao.updateFinishStatus(processId, ServiceStatus.SYNCED, null, 0)
                kycProcessDao.updateOverallStatus(processId, KycProcessStatus.COMPLETED)
                
                Result.success(Unit)
            } else {
                kycProcessDao.updateFinishStatus(processId, ServiceStatus.FAILED, "Finish API failed", 0)
                Result.failure(Exception("Finish API failed"))
            }
        } catch (e: Exception) {
            kycProcessDao.updateFinishStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }
    
    private suspend fun executeFinishOffline(processId: String, accessToken: String): Result<Unit> {
        val finishEntity = KycFinishEntity(
            processId = processId,
            accessToken = accessToken,
            isFinished = true,
            status = ServiceStatus.COMPLETED
        )
        kycFinishDao.insertFinish(finishEntity)
        
        kycProcessDao.updateFinishStatus(processId, ServiceStatus.COMPLETED, null, 0)
        kycProcessDao.updateOverallStatus(processId, KycProcessStatus.COMPLETED_OFFLINE)
        kycProcessDao.updateSyncStatus(processId, requiresSync = true, syncAttempts = 0, lastAttempt = null)
        
        return Result.success(Unit)
    }
}