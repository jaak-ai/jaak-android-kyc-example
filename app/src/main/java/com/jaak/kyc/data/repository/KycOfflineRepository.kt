package com.jaak.kyc.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.jaak.kyc.data.local.dao.*
import com.jaak.kyc.data.local.entity.*
import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Request
import com.jaak.kyc.data.model.verify.VerifyRequest
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.data.network.JaakDBService
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.Utils
import com.jaak.kyc.utils.FileStorageUtils
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
        val tokenByShortKey = getTokenByShortKey(process?.shortKey ?: "")

        // ✅ Token válido si:
        // 1. El proceso tiene token (del Session online exitoso) O
        // 2. Hay token por shortkey (del sync de Session) O
        // 3. Constants.TOKEN tiene valor (fallback global)
        return !process?.accessToken.isNullOrEmpty() ||
                !tokenByShortKey.isNullOrEmpty() ||
                !Constants.TOKEN.isNullOrEmpty()
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
        // 🚨 EVITAR DUPLICADOS: Buscar si ya existe un proceso con este shortKey
        val existingProcess = kycProcessDao.getProcessByShortKey(shortKey)

        if (existingProcess != null) {
            android.util.Log.d("KycOfflineRepository", "Process with shortKey '$shortKey' already exists, updating existing process: ${existingProcess.id}")

            // Resetear el proceso existente a estado inicial
            val updatedProcess = existingProcess.copy(
                overallStatus = KycProcessStatus.PENDING,
                requiresSync = false,
                // Resetear todos los estados de servicios
                sessionStatus = ServiceStatus.PENDING,
                verifyStatus = ServiceStatus.PENDING,
                ocrStatus = ServiceStatus.PENDING,
                livenessStatus = ServiceStatus.PENDING,
                otoVerifyStatus = ServiceStatus.PENDING,
                finishStatus = ServiceStatus.PENDING,
                // Limpiar errores y contadores
                sessionError = null,
                verifyError = null,
                ocrError = null,
                livenessError = null,
                otoVerifyError = null,
                finishError = null,
                sessionRetryCount = 0,
                verifyRetryCount = 0,
                ocrRetryCount = 0,
                livenessRetryCount = 0,
                otoVerifyRetryCount = 0,
                finishRetryCount = 0,
                // Actualizar timestamp
                updatedAt = System.currentTimeMillis()
            )

            kycProcessDao.updateProcess(updatedProcess)
            return existingProcess.id
        } else {
            android.util.Log.d("KycOfflineRepository", "Creating new process for shortKey '$shortKey'")

            // Crear nuevo proceso
            val process = KycProcessEntity(
                shortKey = shortKey,
                overallStatus = KycProcessStatus.PENDING,
                requiresSync = false
            )

            kycProcessDao.insertProcess(process)
            return process.id
        }
    }

    // TOKEN MANAGEMENT BY SHORTKEY
    suspend fun storeTokenByShortKey(shortKey: String, token: String, eventId: String? = null) {
        // Buscar el proceso por shortkey y actualizar su token
        val processes = kycProcessDao.getAllProcesses().first()
        val process = processes.find { it.shortKey == shortKey }

        if (process != null) {
            kycProcessDao.updateSessionData(process.id, eventId, token)
            android.util.Log.d("KycOfflineRepository", "Token stored for shortKey: $shortKey, token: ${token.take(20)}...")
        } else {
            android.util.Log.w("KycOfflineRepository", "Process not found for shortKey: $shortKey")
        }
    }

    suspend fun getTokenByShortKey(shortKey: String): String? {
        val processes = kycProcessDao.getAllProcesses().first()
        val process = processes.find { it.shortKey == shortKey }

        return process?.accessToken?.also {
            android.util.Log.d("KycOfflineRepository", "Retrieved token for shortKey: $shortKey, token: ${it.take(20)}...")
        }
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

                // 🚨 Validación código 200 (como en ViewModels)
                if (response.code() != 200) {
                    kycProcessDao.updateSessionStatus(processId, ServiceStatus.FAILED, "Session API returned code: ${response.code()}", 0)
                    return Result.failure(Exception("Session API returned code: ${response.code()}"))
                }

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

                // Almacenar token por shortkey para acceso global
                storeTokenByShortKey(shortKey, sessionResponse.accessToken, sessionResponse.sessionId)

                Result.success(Unit)
            } else {
                // 🔄 LÓGICA ORIGINAL: Parsear error del response body
                val errorModel = response.errorBody()?.let {
                    try {
                        Utils.responseBodyToResultsModel(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                val errorMessage = errorModel?.message ?: "Session API failed"

                kycProcessDao.updateSessionStatus(processId, ServiceStatus.FAILED, errorMessage, 0)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            kycProcessDao.updateSessionStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }

    private suspend fun executeSessionOffline(processId: String, shortKey: String): Result<Unit> {
        // Store session data for later sync - preservar shortkey para sync
        val sessionEntity = KycSessionEntity(
            processId = processId,
            shortKey = shortKey,
            status = ServiceStatus.PENDING // PENDING para indicar que necesita sync
        )
        kycSessionDao.insertSession(sessionEntity)

        // Update process status - COMPLETED indica que se guardó offline exitosamente
        kycProcessDao.updateSessionStatus(processId, ServiceStatus.COMPLETED, null, 0)
        kycProcessDao.updateSyncStatus(processId, requiresSync = true, syncAttempts = 0, lastAttempt = null)

        android.util.Log.d("KycOfflineRepository", "Session stored offline - shortKey: $shortKey, processId: $processId")

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

            // 🔑 Prioridad: 1) Token del proceso, 2) Token por shortkey, 3) Constants.TOKEN
            val process = kycProcessDao.getProcessById(processId)
            val tokenByShortKey = getTokenByShortKey(process?.shortKey ?: "")
            val rawToken = process?.accessToken ?: tokenByShortKey ?: Constants.API_TOKEN
            val accessToken = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

            // 🔧 Convertir paths a base64 para HTTP request
            val frontBase64 = FileStorageUtils.fileToBase64(verifyRequest.document)
            val backBase64 = verifyRequest.document2?.let { FileStorageUtils.fileToBase64(it) }

            if (frontBase64 == null) {
                kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, "Failed to convert images to base64", 0)
                return Result.failure(Exception("Failed to convert images to base64"))
            }

            // Crear request con base64 para envío HTTP
            val httpRequest = VerifyRequest(frontBase64, backBase64, verifyRequest.documentType)
            val response = jaakDBService.verifyApi(accessToken, httpRequest)

            if (response.isSuccessful && response.body() != null) {
                val verifyResponse = response.body()!!

                // 🚨 Validación código 200 (como en ViewModels)
                if (response.code() != 200) {
                    kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, "Verify API returned code: ${response.code()}", 0)
                    return Result.failure(Exception("Verify API returned code: ${response.code()}"))
                }

                // 🚨 Validación específica Servicio 2: document.type no puede ser null o empty
                if (verifyResponse.document?.type.isNullOrEmpty()) {
                    kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, "Document type is null or empty", 0)
                    return Result.failure(Exception("Document type is null or empty"))
                }

                // 🔧 Guardar PATHS (no base64) en BD
                val verifyEntity = KycVerifyEntity(
                    processId = processId,
                    imageFront = verifyRequest.document, // Path del archivo
                    imageBack = verifyRequest.document2, // Path del archivo
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
                // 🔄 LÓGICA ORIGINAL: Parsear error del response body
                val errorModel = response.errorBody()?.let {
                    try {
                        Utils.responseBodyToResultsModel(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                val errorMessage = errorModel?.message ?: "Verify API failed"

                kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, errorMessage, 0)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            kycProcessDao.updateVerifyStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }

    private suspend fun executeVerifyOffline(processId: String, verifyRequest: VerifyRequest): Result<Unit> {
        // 🔧 Guardar PATHS (no base64) en BD
        val verifyEntity = KycVerifyEntity(
            processId = processId,
            imageFront = verifyRequest.document, // Path del archivo
            imageBack = verifyRequest.document2, // Path del archivo
            documentType = verifyRequest.documentType,
            status = ServiceStatus.COMPLETED
        )
        kycVerifyDao.insertVerify(verifyEntity)

        kycProcessDao.updateVerifyStatus(processId, ServiceStatus.COMPLETED, null, 0)
        kycProcessDao.updateSyncStatus(processId, requiresSync = true, syncAttempts = 0, lastAttempt = null)

        return Result.success(Unit)
    }

    // OCR SERVICE - Using V4 API
    suspend fun executeOcr(processId: String, ocrRequest: DocumentExtractV4Request): Result<Unit> {
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

    private suspend fun executeOcrOnline(processId: String, ocrRequest: DocumentExtractV4Request): Result<Unit> {
        return try {
            kycProcessDao.updateOcrStatus(processId, ServiceStatus.RETRYING, null, 0)

            // 🔑 Prioridad: 1) Token del proceso, 2) Token por shortkey, 3) Constants.TOKEN
            val process = kycProcessDao.getProcessById(processId)
            val tokenByShortKey = getTokenByShortKey(process?.shortKey ?: "")
            val rawToken = process?.accessToken ?: tokenByShortKey ?: Constants.API_TOKEN
            val accessToken = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

            // 🔧 Convertir paths a base64 para HTTP request
            val frontBase64 = FileStorageUtils.fileToBase64(ocrRequest.imageFront)
            val backBase64 = FileStorageUtils.fileToBase64(ocrRequest.imageBack)

            if (frontBase64 == null || backBase64 == null) {
                kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, "Failed to convert images to base64", 0)
                return Result.failure(Exception("Failed to convert images to base64"))
            }

            // Crear request con base64 para envío HTTP
            val httpRequest = DocumentExtractV4Request(frontBase64, backBase64, ocrRequest.allowedCountries)
            val response = jaakDBService.ocrV4Api(accessToken, httpRequest)

            if (response.isSuccessful && response.body() != null) {
                val ocrResponse = response.body()!!

                // 🚨 Validación código 200 (como en ViewModels)
                if (response.code() != 200) {
                    kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, "OCR API returned code: ${response.code()}", 0)
                    return Result.failure(Exception("OCR API returned code: ${response.code()}"))
                }

                // ✅ Servicio 3 V4: status debe ser "SUCCESS"
                if (ocrResponse.status != "SUCCESS") {
                    kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, "OCR status: ${ocrResponse.status}", 0)
                    return Result.failure(Exception("OCR status: ${ocrResponse.status}"))
                }

                // 🔧 Guardar face (base64) como archivo permanente (similar a bestFrame de liveness)
                val facePath = if (!ocrResponse.content.data.personal.face.isNullOrEmpty()) {
                    FileStorageUtils.saveBase64ToPermanentFile(
                        context,
                        ocrResponse.content.data.personal.face,
                        "face_${System.currentTimeMillis()}.jpg"
                    )
                } else null

                // 🔧 Guardar PATHS (no base64) en BD
                val ocrEntity = KycOcrEntity(
                    processId = processId,
                    documentFront = ocrRequest.imageFront, // Path del archivo
                    documentBack = ocrRequest.imageBack, // Path del archivo
                    eventId = ocrResponse.eventId,
                    requestId = ocrResponse.requestId,
                    status = ocrResponse.status == "SUCCESS",
                    message = "Document extracted successfully",
                    documentType = gson.toJson(ocrResponse.content.data.document),
                    documentData = gson.toJson(ocrResponse.content.data.personal),
                    documentMetadata = null,
                    processingTime = ocrResponse.processingTime,
                    responseState = gson.toJson(ocrResponse.state),
                    facePath = facePath, // Path to saved face image file
                    ocrResponseJson = gson.toJson(ocrResponse), // Full response for blacklist services
                    serviceStatus = ServiceStatus.SYNCED
                )
                kycOcrDao.insertOcr(ocrEntity)

                kycProcessDao.updateOcrStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                // 🔄 LÓGICA ORIGINAL: Parsear error del response body
                val errorModel = response.errorBody()?.let {
                    try {
                        Utils.responseBodyToResultsModel(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                val errorMessage = errorModel?.message ?: "OCR API failed"

                kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, errorMessage, 0)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            kycProcessDao.updateOcrStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }

    private suspend fun executeOcrOffline(processId: String, ocrRequest: DocumentExtractV4Request): Result<Unit> {
        // 🔧 Guardar PATHS (no base64) en BD
        val ocrEntity = KycOcrEntity(
            processId = processId,
            documentFront = ocrRequest.imageFront, // Path del archivo
            documentBack = ocrRequest.imageBack, // Path del archivo
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

            // 🔑 Prioridad: 1) Token del proceso, 2) Token por shortkey, 3) Constants.TOKEN
            val process = kycProcessDao.getProcessById(processId)
            val tokenByShortKey = getTokenByShortKey(process?.shortKey ?: "")
            val rawToken = process?.accessToken ?: tokenByShortKey ?: Constants.API_TOKEN
            val accessToken = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

            // 🔧 Convertir video path a base64 para HTTP request
            val videoBase64 = FileStorageUtils.fileToBase64(livenessRequest.video)

            if (videoBase64 == null) {
                kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, "Failed to convert video to base64", 0)
                return Result.failure(Exception("Failed to convert video to base64"))
            }

            // Crear request HTTP con base64
            val httpRequest = LivenessVerifyRequest(videoBase64)
            val response = jaakDBService.livenessVerifyApi(accessToken, httpRequest)

            if (response.isSuccessful && response.body() != null) {
                val livenessResponse = response.body()!!

                // 🚨 Validación código 200 (como en ViewModels)
                if (response.code() != 200) {
                    kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, "Liveness API returned code: ${response.code()}", 0)
                    return Result.failure(Exception("Liveness API returned code: ${response.code()}"))
                }

                // 🚨 Validación específica Servicio 4: score > 0.7
                if (livenessResponse.score <= 0.7) {
                    kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, "Liveness score too low: ${livenessResponse.score}", 0)
                    return Result.failure(Exception("Liveness score too low: ${livenessResponse.score}"))
                }

                // 🔧 Guardar bestFrame (base64) como archivo permanente
                val bestFramePath = if (!livenessResponse.bestFrame.isNullOrEmpty()) {
                    FileStorageUtils.saveBase64ToPermanentFile(
                        context,
                        livenessResponse.bestFrame,
                        "best_frame_${System.currentTimeMillis()}.jpg"
                    )
                } else null

                val livenessEntity = KycLivenessEntity(
                    processId = processId,
                    video = livenessRequest.video, // Path to video file (NOT base64)
                    eventId = livenessResponse.eventId,
                    requestId = livenessResponse.requestId,
                    processTime = livenessResponse.processTime,
                    bestFrame = bestFramePath, // Path to saved best frame file
                    status = ServiceStatus.SYNCED
                )
                kycLivenessDao.insertLiveness(livenessEntity)

                kycProcessDao.updateLivenessStatus(processId, ServiceStatus.SYNCED, null, 0)
                Result.success(Unit)
            } else {
                // 🔄 LÓGICA ORIGINAL: Parsear error del response body
                val errorModel = response.errorBody()?.let {
                    try {
                        Utils.responseBodyToResultsModel(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                val errorMessage = errorModel?.message ?: "Liveness API failed"

                kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, errorMessage, 0)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            kycProcessDao.updateLivenessStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }

    private suspend fun executeLivenessOffline(processId: String, livenessRequest: LivenessVerifyRequest): Result<Unit> {
        val livenessEntity = KycLivenessEntity(
            processId = processId,
            video = livenessRequest.video, // Path to video file (NOT base64)
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
            // 🔧 Obtener bestFramePath desde BD si viene vacío
            val finalImage2Path = if (otoVerifyRequest.image2.isNullOrEmpty()) {
                val livenessData = kycLivenessDao.getLivenessByProcessId(processId)
                val bestFramePathFromDB = livenessData?.bestFrame

                android.util.Log.d("KycOfflineRepository", "OtoVerify bestFrame from request: empty")
                android.util.Log.d("KycOfflineRepository", "OtoVerify bestFrame path from DB: ${bestFramePathFromDB ?: "null"}")

                bestFramePathFromDB ?: ""
            } else {
                otoVerifyRequest.image2
            }

            // Crear request final con paths
            val finalRequest = OtoVerifyRequest(otoVerifyRequest.image1, finalImage2Path)

            if (canExecuteOnline(processId)) {
                // ✅ Modo online: Hay internet y token válido
                executeOtoVerifyOnline(processId, finalRequest)
            } else {
                // 📱 Modo offline: Sin internet O Session fue offline
                executeOtoVerifyOffline(processId, finalRequest)
            }
        } catch (e: Exception) {
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }

    private suspend fun executeOtoVerifyOnline(processId: String, otoVerifyRequest: OtoVerifyRequest): Result<Unit> {
        return try {
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.RETRYING, null, 0)

            // 🔑 Prioridad: 1) Token del proceso, 2) Token por shortkey, 3) Constants.TOKEN
            val process = kycProcessDao.getProcessById(processId)
            val tokenByShortKey = getTokenByShortKey(process?.shortKey ?: "")
            val rawToken = process?.accessToken ?: tokenByShortKey ?: Constants.API_TOKEN
            val accessToken = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

            // 🔧 Convertir paths a base64 para HTTP request
            val image1Base64 = FileStorageUtils.fileToBase64(otoVerifyRequest.image1)
            val image2Base64 = if (!otoVerifyRequest.image2.isNullOrEmpty()) {
                FileStorageUtils.fileToBase64(otoVerifyRequest.image2)
            } else null

            if (image1Base64 == null) {
                kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, "Failed to convert image1 to base64", 0)
                return Result.failure(Exception("Failed to convert image1 to base64"))
            }

            if (otoVerifyRequest.image2.isNotEmpty() && image2Base64 == null) {
                kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, "Failed to convert image2 to base64", 0)
                return Result.failure(Exception("Failed to convert image2 to base64"))
            }

            // Crear request HTTP con base64
            val httpRequest = OtoVerifyRequest(image1Base64, image2Base64 ?: "")
            val response = jaakDBService.otoVerifyApi(accessToken, httpRequest)

            if (response.isSuccessful && response.body() != null) {
                val otoVerifyResponse = response.body()!!

                // 🚨 Validación código 200 (como en ViewModels)
                if (response.code() != 200) {
                    kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, "OtoVerify API returned code: ${response.code()}", 0)
                    return Result.failure(Exception("OtoVerify API returned code: ${response.code()}"))
                }

                // 🚨 Validación específica Servicio 5: score > 80
                if (otoVerifyResponse.score <= 80) {
                    kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, "OtoVerify score too low: ${otoVerifyResponse.score}", 0)
                    return Result.failure(Exception("OtoVerify score too low: ${otoVerifyResponse.score}"))
                }

                val otoVerifyEntity = KycOtoVerifyEntity(
                    processId = processId,
                    image1 = otoVerifyRequest.image1, // Path to face image file (NOT base64)
                    image2 = otoVerifyRequest.image2, // Path to bestFrame file (NOT base64)
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
                // 🔄 LÓGICA ORIGINAL: Parsear error del response body
                val errorModel = response.errorBody()?.let {
                    try {
                        Utils.responseBodyToResultsModel(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                val errorMessage = errorModel?.message ?: "OtoVerify API failed"

                kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, errorMessage, 0)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.FAILED, e.message, 0)
            Result.failure(e)
        }
    }

    private suspend fun executeOtoVerifyOffline(processId: String, otoVerifyRequest: OtoVerifyRequest): Result<Unit> {
        // ✅ NUEVA LÓGICA: Siempre crear registro offline, incluso sin bestFrame
        // El estado será PENDING para indicar que necesita sync posterior

        val otoVerifyEntity = KycOtoVerifyEntity(
            processId = processId,
            image1 = otoVerifyRequest.image1, // Path to face image file (NOT base64)
            image2 = otoVerifyRequest.image2, // Path to bestFrame file (NOT base64, empty in offline)
            status = ServiceStatus.PENDING // PENDING indica que necesita sync
        )
        kycOtoVerifyDao.insertOtoVerify(otoVerifyEntity)

        if (otoVerifyRequest.image2.isNullOrEmpty()) {
            // 📱 Modo offline: Sin bestFrame, marcar como PENDING para sync
            android.util.Log.d("KycOfflineRepository", "OtoVerify executed offline without bestFrame - marked as PENDING for sync")
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.PENDING, "Awaiting bestFrame from sync", 0)
        } else {
            // 🌐 Modo offline con bestFrame: Marcar como COMPLETED
            android.util.Log.d("KycOfflineRepository", "OtoVerify executed offline with bestFrame - marked as COMPLETED")
            kycProcessDao.updateOtoVerifyStatus(processId, ServiceStatus.COMPLETED, null, 0)
        }

        kycProcessDao.updateSyncStatus(processId, requiresSync = true, syncAttempts = 0, lastAttempt = null)

        return Result.success(Unit)
    }

    // FINISH SERVICE
    suspend fun executeFinish(processId: String): Result<Unit> {
        return try {
            if (canExecuteOnline(processId)) {
                // ✅ Modo online: Hay internet y token válido
                val process = kycProcessDao.getProcessById(processId)
                val rawToken = process?.accessToken ?: Constants.API_TOKEN
                val accessToken = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

                // 📞 BLACKLIST: Ejecutar los 5 servicios ANTES del Finish
                executeBlacklistServices(processId, accessToken)

                // Luego ejecutar Finish
                executeFinishOnline(processId, accessToken)
            } else {
                // 📱 Modo offline: Sin internet O Session fue offline
                val process = kycProcessDao.getProcessById(processId)
                val rawToken = process?.accessToken ?: Constants.API_TOKEN
                val accessToken = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
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
                // 🚨 Validación código 200 (como en ViewModels)
                if (response.code() != 200) {
                    kycProcessDao.updateFinishStatus(processId, ServiceStatus.FAILED, "Finish API returned code: ${response.code()}", 0)
                    return Result.failure(Exception("Finish API returned code: ${response.code()}"))
                }

                // ✅ Servicio 6: No validaciones específicas, solo código 200

                val finishEntity = KycFinishEntity(
                    processId = processId,
                    accessToken = accessToken,
                    isFinished = true,
                    status = ServiceStatus.SYNCED
                )
                kycFinishDao.insertFinish(finishEntity)

                kycProcessDao.updateFinishStatus(processId, ServiceStatus.SYNCED, null, 0)
                kycProcessDao.updateOverallStatus(processId, KycProcessStatus.COMPLETED)

                // 🧹 LIMPIEZA DESPUÉS DE FINISH EXITOSO: Eliminar todos los registros
                cleanupProcessData(processId)

                Result.success(Unit)
            } else {
                // 🔄 LÓGICA ORIGINAL: Parsear error del response body
                val errorModel = response.errorBody()?.let {
                    try {
                        Utils.responseBodyToResultsModel(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                val errorMessage = errorModel?.message ?: "Finish API failed"

                kycProcessDao.updateFinishStatus(processId, ServiceStatus.FAILED, errorMessage, 0)
                Result.failure(Exception(errorMessage))
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

    // 📞 BLACKLIST: Ejecutar los 5 servicios de blacklist (fire-and-forget)
    private suspend fun executeBlacklistServices(processId: String, accessToken: String) {
        try {
            android.util.Log.d("KycOfflineRepository", "📞 ============ STARTING BLACKLIST SERVICES ============")
            android.util.Log.d("KycOfflineRepository", "📞 ProcessId: $processId")
            android.util.Log.d("KycOfflineRepository", "📞 AccessToken: ${accessToken.take(20)}...")

            // Obtener datos del OCR desde BD
            val ocrEntity = kycOcrDao.getOcrByProcessId(processId) ?: run {
                android.util.Log.w("KycOfflineRepository", "⚠️ No OCR data found for blacklist services")
                return
            }

            android.util.Log.d("KycOfflineRepository", "📞 OCR Entity found: ${ocrEntity.id}")

            // Validar que tengamos el response de OCR guardado
            val ocrResponseJson = ocrEntity.ocrResponseJson ?: run {
                android.util.Log.w("KycOfflineRepository", "⚠️ No OCR response JSON found for blacklist services")
                android.util.Log.w("KycOfflineRepository", "⚠️ OCR Entity: processId=${ocrEntity.processId}, status=${ocrEntity.serviceStatus}")
                return
            }

            android.util.Log.d("KycOfflineRepository", "📞 OCR Response JSON length: ${ocrResponseJson.length}")

            // Parsear el response de OCR
            val ocrResponse = try {
                com.google.gson.Gson().fromJson(ocrResponseJson, com.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Response::class.java)
            } catch (e: Exception) {
                android.util.Log.e("KycOfflineRepository", "❌ Failed to parse OCR response: ${e.message}")
                android.util.Log.e("KycOfflineRepository", "❌ JSON: ${ocrResponseJson.take(200)}...")
                return
            }

            android.util.Log.d("KycOfflineRepository", "📞 OCR Response parsed successfully")
            android.util.Log.d("KycOfflineRepository", "📞 Personal: name=${ocrResponse.content.data.personal.firstName}, curp=${ocrResponse.content.data.document.personalIdNumber}")

            // ✅ Imprimir OCR completo para validar datos disponibles
            android.util.Log.d("KycOfflineRepository", "📞 ========== OCR RESPONSE COMPLETO ==========")
            android.util.Log.d("KycOfflineRepository", "📞 OCR JSON: ${com.google.gson.Gson().toJson(ocrResponse)}")
            android.util.Log.d("KycOfflineRepository", "📞 ==========================================")

            // Crear el payload base desde el response de OCR
            val payload = com.jaak.kyc.utils.BlacklistRequestBuilder.createPayloadFromDocumentExtract(ocrResponse)

            android.util.Log.d("KycOfflineRepository", "📞 Payload created:")
            android.util.Log.d("KycOfflineRepository", "📞   - Name: ${payload.person.name} ${payload.person.lastName}")
            android.util.Log.d("KycOfflineRepository", "📞   - CURP: ${payload.identifications.curp}")
            android.util.Log.d("KycOfflineRepository", "📞   - RFC: ${payload.identifications.rfc}")
            android.util.Log.d("KycOfflineRepository", "📞   - INE OCR: ${payload.identifications.ine?.ocr}")
            android.util.Log.d("KycOfflineRepository", "📞   - INE CIC: ${payload.identifications.ine?.cic}")

            // Lanzar los 5 servicios secuencialmente (fire-and-forget)
            android.util.Log.d("KycOfflineRepository", "📞 ============ CALLING BLACKLIST SERVICES ============")

            // 1. INE
            try {
                android.util.Log.d("KycOfflineRepository", "📞 [1/5] Calling INE service...")
                val ineRequest = com.jaak.kyc.utils.BlacklistRequestBuilder.createIneRequest(payload)
                android.util.Log.d("KycOfflineRepository", "📞 INE Request JSON: ${com.google.gson.Gson().toJson(ineRequest)}")
                val ineResponse = jaakDBService.blacklistInvestigateApi(accessToken, ineRequest)
                android.util.Log.d("KycOfflineRepository", "✅ Blacklist INE service called - Response code: ${ineResponse.code()}")
                if (ineResponse.isSuccessful) {
                    android.util.Log.d("KycOfflineRepository", "📞 INE Response: ${ineResponse.body()}")
                } else {
                    val errorBody = ineResponse.errorBody()?.string()
                    android.util.Log.e("KycOfflineRepository", "❌ INE Error Response: $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("KycOfflineRepository", "❌ Blacklist INE failed: ${e.message}")
                e.printStackTrace()
            }

            // 2. INTERPOL
            try {
                android.util.Log.d("KycOfflineRepository", "📞 [2/5] Calling INTERPOL service...")
                val interpolRequest = com.jaak.kyc.utils.BlacklistRequestBuilder.createInterpolRequest(payload)
                android.util.Log.d("KycOfflineRepository", "📞 INTERPOL Request JSON: ${com.google.gson.Gson().toJson(interpolRequest)}")
                val interpolResponse = jaakDBService.blacklistInvestigateApi(accessToken, interpolRequest)
                android.util.Log.d("KycOfflineRepository", "✅ Blacklist INTERPOL service called - Response code: ${interpolResponse.code()}")
                if (interpolResponse.isSuccessful) {
                    android.util.Log.d("KycOfflineRepository", "📞 INTERPOL Response: ${interpolResponse.body()}")
                } else {
                    val errorBody = interpolResponse.errorBody()?.string()
                    android.util.Log.e("KycOfflineRepository", "❌ INTERPOL Error Response: $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("KycOfflineRepository", "❌ Blacklist INTERPOL failed: ${e.message}")
                e.printStackTrace()
            }

            // 3. OFAC
            try {
                android.util.Log.d("KycOfflineRepository", "📞 [3/5] Calling OFAC service...")
                val ofacRequest = com.jaak.kyc.utils.BlacklistRequestBuilder.createOfacRequest(payload)
                android.util.Log.d("KycOfflineRepository", "📞 OFAC Request JSON: ${com.google.gson.Gson().toJson(ofacRequest)}")
                val ofacResponse = jaakDBService.blacklistInvestigateApi(accessToken, ofacRequest)
                android.util.Log.d("KycOfflineRepository", "✅ Blacklist OFAC service called - Response code: ${ofacResponse.code()}")
                if (ofacResponse.isSuccessful) {
                    android.util.Log.d("KycOfflineRepository", "📞 OFAC Response: ${ofacResponse.body()}")
                } else {
                    val errorBody = ofacResponse.errorBody()?.string()
                    android.util.Log.e("KycOfflineRepository", "❌ OFAC Error Response: $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("KycOfflineRepository", "❌ Blacklist OFAC failed: ${e.message}")
                e.printStackTrace()
            }

            // 4. RENAPO/CURP
            try {
                android.util.Log.d("KycOfflineRepository", "📞 [4/5] Calling RENAPO service...")
                val renapoRequest = com.jaak.kyc.utils.BlacklistRequestBuilder.createRenapoRequest(payload)
                android.util.Log.d("KycOfflineRepository", "📞 RENAPO Request: services.renapo.curp=${renapoRequest.services.renapo?.curp}")
                val renapoResponse = jaakDBService.blacklistInvestigateApi(accessToken, renapoRequest)
                android.util.Log.d("KycOfflineRepository", "✅ Blacklist RENAPO service called - Response code: ${renapoResponse.code()}")
                if (renapoResponse.isSuccessful) {
                    android.util.Log.d("KycOfflineRepository", "📞 RENAPO Response: ${renapoResponse.body()}")
                } else {
                    val errorBody = renapoResponse.errorBody()?.string()
                    android.util.Log.e("KycOfflineRepository", "❌ RENAPO Error Response: $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("KycOfflineRepository", "❌ Blacklist RENAPO failed: ${e.message}")
                e.printStackTrace()
            }

            // 5. SAT69B
            try {
                android.util.Log.d("KycOfflineRepository", "📞 [5/5] Calling SAT service...")
                val satRequest = com.jaak.kyc.utils.BlacklistRequestBuilder.createSatRequest(payload)
                android.util.Log.d("KycOfflineRepository", "📞 SAT Request: services.sat.sat69b=${satRequest.services.sat?.sat69b}")
                val satResponse = jaakDBService.blacklistInvestigateApi(accessToken, satRequest)
                android.util.Log.d("KycOfflineRepository", "✅ Blacklist SAT service called - Response code: ${satResponse.code()}")
                android.util.Log.d("KycOfflineRepository", "📞 SAT Response: ${satResponse.body()}")
            } catch (e: Exception) {
                android.util.Log.e("KycOfflineRepository", "❌ Blacklist SAT failed: ${e.message}")
                e.printStackTrace()
            }

            android.util.Log.d("KycOfflineRepository", "📞 ============ ALL BLACKLIST SERVICES CALLED ============")

        } catch (e: Exception) {
            android.util.Log.e("KycOfflineRepository", "❌ Failed to launch blacklist services: ${e.message}")
            // No lanzar excepción para no afectar el resultado del Finish exitoso
        }
    }

    // 🧹 CLEANUP DESPUÉS DE FINISH EXITOSO
    private suspend fun cleanupProcessData(processId: String) {
        try {
            val process = kycProcessDao.getProcessById(processId)
            val shortKey = process?.shortKey ?: "unknown"

            android.util.Log.d("KycOfflineRepository", "🧹 Starting cleanup for processId: $processId, shortKey: $shortKey")

            // Eliminar todos los registros relacionados con este processId
            kycSessionDao.deleteSessionByProcessId(processId)
            kycVerifyDao.deleteVerifyByProcessId(processId)
            kycOcrDao.deleteOcrByProcessId(processId)
            kycLivenessDao.deleteLivenessByProcessId(processId)
            kycOtoVerifyDao.deleteOtoVerifyByProcessId(processId)
            kycFinishDao.deleteFinishByProcessId(processId)
            serviceExecutionStateDao.deleteServiceStatesForProcess(processId)

            // Por último, eliminar el proceso principal
            kycProcessDao.deleteProcessById(processId)

            android.util.Log.d("KycOfflineRepository", "✅ Cleanup completed for shortKey: $shortKey - all records deleted")

        } catch (e: Exception) {
            android.util.Log.e("KycOfflineRepository", "❌ Cleanup failed for processId: $processId - ${e.message}")
            // No lanzar excepción para no afectar el resultado del Finish exitoso
        }
    }

}