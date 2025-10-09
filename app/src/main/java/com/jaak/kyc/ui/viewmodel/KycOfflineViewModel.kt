package com.jaak.kyc.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaak.kyc.data.local.entity.KycProcessEntity
import com.jaak.kyc.data.local.entity.KycProcessWithDetails
import com.jaak.kyc.data.local.entity.ServiceStatus
import com.jaak.kyc.data.local.entity.KycProcessStatus
import com.jaak.kyc.data.model.ErrorModel
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.model.ocr.DocumentDataExtraBoth
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.data.model.verify.VerifyRequest
import com.google.gson.Gson
import com.jaak.kyc.domain.offline.*
import com.jaak.kyc.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KycOfflineViewModel @Inject constructor(
    private val sessionUseCase: SessionOfflineUseCase,
    private val verifyUseCase: VerifyOfflineUseCase,
    private val ocrUseCase: OcrOfflineUseCase,
    private val livenessUseCase: LivenessOfflineUseCase,
    private val otoVerifyUseCase: OtoVerifyOfflineUseCase,
    private val finishUseCase: FinishOfflineUseCase,
    private val processManagementUseCase: KycProcessManagementUseCase,
    private val syncUseCase: KycSyncUseCase
) : ViewModel() {

    // Current process tracking
    private val _currentProcessId = MutableStateFlow<String?>(null)
    val currentProcessId: StateFlow<String?> = _currentProcessId.asStateFlow()
    
    private val _currentProcessDetails = MutableStateFlow<KycProcessWithDetails?>(null)
    val currentProcessDetails: StateFlow<KycProcessWithDetails?> = _currentProcessDetails.asStateFlow()
    
    // All processes
    private val _allProcesses = MutableStateFlow<List<KycProcessEntity>>(emptyList())
    val allProcesses: StateFlow<List<KycProcessEntity>> = _allProcesses.asStateFlow()
    
    // Network status
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    // Loading and error states
    val isLoading = MutableLiveData<Boolean>()
    val errorModel = MutableLiveData<ErrorModel?>()
    val successMessage = MutableLiveData<String?>()
    
    // Sync status
    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress.asStateFlow()
    
    private val _syncResult = MutableStateFlow<String?>(null)
    val syncResult: StateFlow<String?> = _syncResult.asStateFlow()

    init {
        updateNetworkStatus()
        loadAllProcesses()
    }
    
    // PROCESS MANAGEMENT
    fun createNewProcess(shortKey: String) {
        if (shortKey.isBlank()) {
            errorModel.value = ErrorModel("Short key cannot be empty", false, 400)
            return
        }
        
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val processId = processManagementUseCase.createProcess(shortKey)
                _currentProcessId.value = processId
                loadProcessDetails(processId)
                successMessage.postValue("New KYC process created")
            } catch (e: Exception) {
                errorModel.value = ErrorModel("Failed to create process: ${e.message}", false, 500)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
    
    fun setCurrentProcess(processId: String) {
        _currentProcessId.value = processId
        loadProcessDetails(processId)
    }
    
    /**
     * Crea un nuevo proceso y ejecuta la sesión de forma secuencial
     * Soluciona el race condition entre createNewProcess() y executeSession()
     */
    fun createProcessAndExecuteSession(shortKey: String) {
        if (shortKey.isBlank()) {
            errorModel.value = ErrorModel("Short key cannot be empty", false, 400)
            return
        }
        
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                // 1. Crear el proceso y esperar a que termine
                val processId = processManagementUseCase.createProcess(shortKey)
                _currentProcessId.value = processId
                loadProcessDetails(processId)
                
                // 2. Ahora ejecutar la sesión con el processId ya disponible
                val result = sessionUseCase(processId, shortKey)
                if (result.isSuccess) {
                    // ✅ Verificar el estado directamente desde la BD después de ejecutar
                    val processDetails = processManagementUseCase.getProcessWithDetails(processId)
                    val sessionStatus = processDetails?.process?.sessionStatus
                    val hasToken = !processDetails?.process?.accessToken.isNullOrEmpty()
                    
                    // Actualizar el estado local
                    _currentProcessDetails.value = processDetails
                    
                    if (sessionStatus == ServiceStatus.SYNCED && hasToken) {
                        // ✅ Sesión online exitosa con token - asignar a Constants
                        processDetails?.process?.accessToken?.let { token ->
                            Constants.API_TOKEN = token
                            Constants.TOKEN = Constants.BEARER + token
                        }
                        successMessage.postValue("Session executed successfully")
                    } else if (sessionStatus == ServiceStatus.COMPLETED) {
                        // 📱 Sesión offline exitosa (sin token)
                        successMessage.postValue("Session executed offline successfully")
                    } else {
                        // ⚠️ Estado inesperado
                        errorModel.value = ErrorModel("Session completed but in unexpected state: $sessionStatus", false, 500)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    errorModel.value = ErrorModel("Session execution failed: ${exception?.message}", false, 500)
                }
            } catch (e: Exception) {
                errorModel.value = ErrorModel("Failed to create process and execute session: ${e.message}", false, 500)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
    
    private fun loadProcessDetails(processId: String) {
        viewModelScope.launch {
            try {
                val details = processManagementUseCase.getProcessWithDetails(processId)
                _currentProcessDetails.value = details
            } catch (e: Exception) {
                errorModel.value = ErrorModel("Failed to load process details: ${e.message}", false, 500)
            }
        }
    }
    
    private fun loadAllProcesses() {
        viewModelScope.launch {
            processManagementUseCase.getAllProcesses().collect { processes ->
                _allProcesses.value = processes
            }
        }
    }
    
    // KYC SERVICE EXECUTION
    fun executeSession(shortKey: String) {
        val processId = _currentProcessId.value
        if (processId == null) {
            errorModel.value = ErrorModel("No active process", false, 400)
            return
        }
        
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val result = sessionUseCase(processId, shortKey)
                if (result.isSuccess) {
                    // ✅ Verificar el estado directamente desde la BD después de ejecutar
                    val processDetails = processManagementUseCase.getProcessWithDetails(processId)
                    val sessionStatus = processDetails?.process?.sessionStatus
                    val hasToken = !processDetails?.process?.accessToken.isNullOrEmpty()
                    
                    // Actualizar el estado local
                    _currentProcessDetails.value = processDetails
                    
                    if (sessionStatus == ServiceStatus.SYNCED && hasToken) {
                        // ✅ Sesión online exitosa con token - asignar a Constants
                        processDetails?.process?.accessToken?.let { token ->
                            Constants.API_TOKEN = token
                            Constants.TOKEN = Constants.BEARER + token
                        }
                        successMessage.postValue("Session executed successfully")
                    } else if (sessionStatus == ServiceStatus.COMPLETED) {
                        // 📱 Sesión offline exitosa (sin token)
                        successMessage.postValue("Session executed offline successfully")
                    } else {
                        // ⚠️ Estado inesperado
                        errorModel.value = ErrorModel("Session completed but in unexpected state: $sessionStatus", false, 500)
                    }
                } else {
                    errorModel.value = ErrorModel("Session failed: ${result.exceptionOrNull()?.message}", false, 500)
                }
            } catch (e: Exception) {
                errorModel.value = ErrorModel("Session error: ${e.message}", false, 500)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
    
    fun executeVerify(verifyRequest: VerifyRequest) {
        viewModelScope.launch {
            // 🔧 NUEVA LÓGICA: Siempre buscar processId desde BD
            val processId = getActiveProcessFromBD()
            
            if (processId == null) {
                android.util.Log.e("KycOfflineViewModel", "No active process found in BD for Verify")
                errorModel.value = ErrorModel("No active KYC process found. Please start KYC process first.", false, 400)
                return@launch
            }
            
            executeVerifyWithProcess(processId, verifyRequest)
        }
    }
    
    private fun executeVerifyWithProcess(processId: String, verifyRequest: VerifyRequest) {
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val result = verifyUseCase(processId, verifyRequest)
                if (result.isSuccess) {
                    successMessage.postValue("Document verification completed")
                    loadProcessDetails(processId)
                } else {
                    errorModel.value = ErrorModel("Verify failed: ${result.exceptionOrNull()?.message}", false, 500)
                }
            } catch (e: Exception) {
                errorModel.value = ErrorModel("Verify error: ${e.message}", false, 500)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
    
    fun executeOcr(ocrRequest: com.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Request) {
        viewModelScope.launch {
            // 🔧 NUEVA LÓGICA: Siempre buscar processId desde BD
            val processId = getActiveProcessFromBD()

            if (processId == null) {
                android.util.Log.e("KycOfflineViewModel", "No active process found in BD for OCR")
                errorModel.value = ErrorModel("No active KYC process found. Please start KYC process first.", false, 400)
                return@launch
            }

            executeOcrWithProcess(processId, ocrRequest)
        }
    }
    
    private fun executeOcrWithProcess(processId: String, ocrRequest: com.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Request) {

        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val result = ocrUseCase(processId, ocrRequest)
                if (result.isSuccess) {
                    successMessage.postValue("OCR processing completed")
                    loadProcessDetails(processId)
                } else {
                    errorModel.value = ErrorModel("OCR failed: ${result.exceptionOrNull()?.message}", false, 500)
                }
            } catch (e: Exception) {
                errorModel.value = ErrorModel("OCR error: ${e.message}", false, 500)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
    
    fun executeLiveness(livenessRequest: LivenessVerifyRequest) {
        viewModelScope.launch {
            // 🔧 NUEVA LÓGICA: Siempre buscar processId desde BD
            val processId = getActiveProcessFromBD()
            
            if (processId == null) {
                android.util.Log.e("KycOfflineViewModel", "No active process found in BD for Liveness")
                errorModel.value = ErrorModel("No active KYC process found. Please start KYC process first.", false, 400)
                return@launch
            }
            
            executeLivenessWithProcess(processId, livenessRequest)
        }
    }
    
    private fun executeLivenessWithProcess(processId: String, livenessRequest: LivenessVerifyRequest) {

        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val result = livenessUseCase(processId, livenessRequest)
                if (result.isSuccess) {
                    // ✅ IMPORTANTE: Cargar detalles ANTES de emitir successMessage
                    // para que currentProcessDetails tenga liveness actualizado
                    loadProcessDetails(processId)
                    android.util.Log.d("KycOfflineViewModel", "Liveness completed, processDetails updated")
                    successMessage.postValue("Liveness verification completed")
                } else {
                    errorModel.value = ErrorModel("Liveness failed: ${result.exceptionOrNull()?.message}", false, 500)
                }
            } catch (e: Exception) {
                errorModel.value = ErrorModel("Liveness error: ${e.message}", false, 500)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
    
    fun executeOtoVerify(otoVerifyRequest: OtoVerifyRequest) {
        viewModelScope.launch {
            // 🔧 NUEVA LÓGICA: Siempre buscar processId desde BD
            val processId = getActiveProcessFromBD()
            
            if (processId == null) {
                android.util.Log.e("KycOfflineViewModel", "No active process found in BD for OtoVerify")
                errorModel.value = ErrorModel("No active KYC process found. Please start KYC process first.", false, 400)
                return@launch
            }
            
            executeOtoVerifyWithProcess(processId, otoVerifyRequest)
        }
    }
    
    private fun executeOtoVerifyWithProcess(processId: String, otoVerifyRequest: OtoVerifyRequest) {
        
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val result = otoVerifyUseCase(processId, otoVerifyRequest)
                if (result.isSuccess) {
                    // ✅ Verificar el estado del proceso para determinar el mensaje
                    val processDetails = processManagementUseCase.getProcessWithDetails(processId)
                    val otoVerifyStatus = processDetails?.process?.otoVerifyStatus
                    
                    when (otoVerifyStatus) {
                        ServiceStatus.SYNCED -> {
                            // 🌐 Online exitoso con validación de score
                            successMessage.postValue("Face comparison completed")
                        }
                        ServiceStatus.COMPLETED -> {
                            // 🌐 Online exitoso (sin validación de score online)
                            successMessage.postValue("Face comparison completed")
                        }
                        ServiceStatus.PENDING -> {
                            // 📱 Offline sin bestFrame, guardado para sync posterior
                            successMessage.postValue("Face comparison pending")
                        }
                        else -> {
                            successMessage.postValue("Face comparison completed")
                        }
                    }
                    loadProcessDetails(processId)
                } else {
                    errorModel.value = ErrorModel("Face comparison failed: ${result.exceptionOrNull()?.message}", false, 500)
                }
            } catch (e: Exception) {
                errorModel.value = ErrorModel("Face comparison error: ${e.message}", false, 500)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    // BLACKLIST FUNCTIONALITY REMOVED - No longer needed
    /*
    fun executeBlacklist() {
        viewModelScope.launch {
            // 🔧 NUEVA LÓGICA: Siempre buscar processId desde BD
            val processId = getActiveProcessFromBD()

            if (processId == null) {
                android.util.Log.e("KycOfflineViewModel", "No active process found in BD for Blacklist")
                errorModel.value = ErrorModel("No active KYC process found. Please start KYC process first.", false, 400)
                return@launch
            }

            executeBlacklistWithProcess(processId)
        }
    }

    private fun executeBlacklistWithProcess(processId: String) {
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                // Obtener datos del proceso para crear el payload
                val processDetails = processManagementUseCase.getProcessWithDetails(processId)
                val ocrData = processDetails?.ocr

                if (ocrData == null || ocrData.documentData == null) {
                    // ⚠️ OCR no tiene datos válidos - ejecutar finish para cerrar el proceso
                    android.util.Log.w("KycOfflineViewModel", "OCR data not available for blacklist - executing finish to close process")
                    executeFinish() // Ejecutar finish service para cerrar todo el proceso
                    isLoading.postValue(false)
                    return@launch
                }

                // Parsear documentData JSON
                val gson = Gson()
                val documentData = try {
                    gson.fromJson(ocrData.documentData, DocumentDataExtraBoth::class.java)
                } catch (e: Exception) {
                    android.util.Log.e("KycOfflineViewModel", "Error parsing OCR documentData: ${e.message}")
                    null
                }

                if (documentData == null) {
                    // ⚠️ Error al parsear documentData - ejecutar finish para cerrar el proceso
                    android.util.Log.w("KycOfflineViewModel", "Unable to parse OCR documentData - executing finish to close process")
                    executeFinish() // Ejecutar finish service para cerrar todo el proceso
                    isLoading.postValue(false)
                    return@launch
                }

                // Extraer campos específicos
                val specificDataMap = documentData.specificData.associate {
                    it.field to (it.value ?: "")
                }

                // Crear payload con datos del OCR
                val payload = com.jaak.kyc.utils.BlacklistRequestBuilder.createBasePayload(
                    name = documentData.generalData.name,
                    lastName = specificDataMap["lastName"] ?: "",
                    secondLastName = specificDataMap["secondLastName"] ?: "",
                    birthDate = documentData.generalData.birthdate,
                    curp = specificDataMap["curp"] ?: "",
                    electorKey = specificDataMap["electorKey"] ?: specificDataMap["claveElector"] ?: "",
                    ineCic = specificDataMap["cic"] ?: "",
                    ineOcr = specificDataMap["ocr"] ?: ""
                )

                // Ejecutar los 5 servicios de blacklist (fire-and-forget)
                val result = blacklistUseCase.executeAllBlacklists(processId, payload)
                if (result.isSuccess) {
                    // ✅ Los servicios fueron lanzados exitosamente (no esperamos respuestas)
                    successMessage.postValue("Blacklist services launched")
                    loadProcessDetails(processId)
                } else {
                    errorModel.value = ErrorModel("Failed to launch blacklist services: ${result.exceptionOrNull()?.message}", false, 500)
                }
            } catch (e: Exception) {
                errorModel.value = ErrorModel("Blacklist verification error: ${e.message}", false, 500)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
    */

    fun executeFinish() {
        viewModelScope.launch {
            // 🔧 NUEVA LÓGICA: Siempre buscar processId desde BD
            val processId = getActiveProcessFromBD()
            
            if (processId == null) {
                android.util.Log.e("KycOfflineViewModel", "No active process found in BD for Finish")
                errorModel.value = ErrorModel("No active KYC process found. Please start KYC process first.", false, 400)
                return@launch
            }
            
            executeFinishWithProcess(processId)
        }
    }
    
    private fun executeFinishWithProcess(processId: String) {
        
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val result = finishUseCase(processId)
                if (result.isSuccess) {
                    successMessage.postValue("KYC process completed successfully")
                    // 🚨 NO cargar detalles después de Finish exitoso - los registros se limpian automáticamente
                    _currentProcessDetails.value = null
                    _currentProcessId.value = null
                } else {
                    errorModel.value = ErrorModel("Finish failed: ${result.exceptionOrNull()?.message}", false, 500)
                }
            } catch (e: Exception) {
                errorModel.value = ErrorModel("Finish error: ${e.message}", false, 500)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
    
    // SYNC OPERATIONS
    fun syncCurrentProcess() {
        val processId = _currentProcessId.value
        if (processId == null) {
            errorModel.value = ErrorModel("No active process to sync", false, 400)
            return
        }
        
        viewModelScope.launch {
            _syncInProgress.value = true
            try {
                val result = syncUseCase.syncProcess(processId)
                if (result.success) {
                    _syncResult.value = "Process synced successfully (${result.processedServices}/${result.totalServices})"
                    loadProcessDetails(processId)
                } else {
                    _syncResult.value = "Sync partially failed: ${result.errors.joinToString(", ")}"
                }
            } catch (e: Exception) {
                _syncResult.value = "Sync error: ${e.message}"
            } finally {
                _syncInProgress.value = false
            }
        }
    }
    
    fun syncAllProcesses() {
        viewModelScope.launch {
            _syncInProgress.value = true
            try {
                val results = syncUseCase.syncAllProcesses()
                val totalProcesses = results.size
                val successfulProcesses = results.count { it.second.success }
                
                _syncResult.value = "Synced $successfulProcesses/$totalProcesses processes"
                loadAllProcesses() // Refresh all processes
            } catch (e: Exception) {
                _syncResult.value = "Bulk sync error: ${e.message}"
            } finally {
                _syncInProgress.value = false
            }
        }
    }
    
    // UTILITIES
    fun updateNetworkStatus() {
        _isNetworkAvailable.value = processManagementUseCase.isNetworkAvailable()
    }
    
    // 🔧 NUEVA FUNCIÓN: Obtener proceso activo desde BD
    private suspend fun getActiveProcessFromBD(): String? {
        return try {
            val processes = processManagementUseCase.getAllProcesses().first()
            val activeProcess = processes
                .filter { it.overallStatus != KycProcessStatus.COMPLETED }
                .maxByOrNull { it.createdAt }
            
            activeProcess?.let { process ->
                android.util.Log.d("KycOfflineViewModel", "Found active process from BD: ${process.id}, shortKey: ${process.shortKey}")
                // Actualizar el currentProcessId en memoria para consistencia
                _currentProcessId.value = process.id
                loadProcessDetails(process.id)
                process.id
            } ?: run {
                android.util.Log.w("KycOfflineViewModel", "No active process found in BD")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("KycOfflineViewModel", "Error getting active process from BD: ${e.message}")
            null
        }
    }
    
    fun clearMessages() {
        errorModel.value = null
        successMessage.value = null
        _syncResult.value = null
    }
}