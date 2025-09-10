package com.jaak.kyc.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaak.kyc.data.local.entity.KycProcessEntity
import com.jaak.kyc.data.local.entity.KycProcessWithDetails
import com.jaak.kyc.data.local.entity.ServiceStatus
import com.jaak.kyc.data.model.ErrorModel
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.data.model.verify.VerifyRequest
import com.jaak.kyc.domain.offline.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                        // ✅ Sesión online exitosa con token
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
        val processId = _currentProcessId.value
        if (processId == null) {
            errorModel.value = ErrorModel("No active process", false, 400)
            return
        }
        
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
    
    fun executeOcr(ocrRequest: DocumentExtraBothRequest) {
        val processId = _currentProcessId.value
        if (processId == null) {
            errorModel.value = ErrorModel("No active process", false, 400)
            return
        }
        
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
        val processId = _currentProcessId.value
        if (processId == null) {
            errorModel.value = ErrorModel("No active process", false, 400)
            return
        }
        
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val result = livenessUseCase(processId, livenessRequest)
                if (result.isSuccess) {
                    successMessage.postValue("Liveness verification completed")
                    loadProcessDetails(processId)
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
        val processId = _currentProcessId.value
        if (processId == null) {
            errorModel.value = ErrorModel("No active process", false, 400)
            return
        }
        
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val result = otoVerifyUseCase(processId, otoVerifyRequest)
                if (result.isSuccess) {
                    successMessage.postValue("Face comparison completed")
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
    
    fun executeFinish() {
        val processId = _currentProcessId.value
        if (processId == null) {
            errorModel.value = ErrorModel("No active process", false, 400)
            return
        }
        
        viewModelScope.launch {
            isLoading.postValue(true)
            try {
                val result = finishUseCase(processId)
                if (result.isSuccess) {
                    successMessage.postValue("KYC process completed successfully")
                    loadProcessDetails(processId)
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
    
    fun clearMessages() {
        errorModel.value = null
        successMessage.value = null
        _syncResult.value = null
    }
}