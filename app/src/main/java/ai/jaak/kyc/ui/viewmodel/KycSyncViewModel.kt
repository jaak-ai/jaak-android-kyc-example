package ai.jaak.kyc.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import ai.jaak.kyc.domain.service.ProcessErrorManager
import ai.jaak.kyc.domain.service.ProcessTokenManager
import ai.jaak.kyc.domain.service.NetworkConnectivityService
import ai.jaak.kyc.data.repository.KycOfflineRepository
import ai.jaak.kyc.data.local.entity.KycProcessStatus
import ai.jaak.kyc.data.local.entity.ServiceStatus
import ai.jaak.kyc.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class KycSyncViewModel @Inject constructor(
    private val repository: KycOfflineRepository,
    private val workScheduler: WorkScheduler,
    private val networkConnectivityService: NetworkConnectivityService,
    private val errorManager: ProcessErrorManager,
    private val tokenManager: ProcessTokenManager
) : ViewModel() {

    private val _networkStatus = MutableLiveData<NetworkStatus>()
    val networkStatus: LiveData<NetworkStatus> = _networkStatus

    private val _syncStats = MutableLiveData<SyncStatistics>()
    val syncStats: LiveData<SyncStatistics> = _syncStats

    private val _currentSyncProgress = MutableLiveData<SyncProgressInfo?>()
    val currentSyncProgress: LiveData<SyncProgressInfo?> = _currentSyncProgress

    private val _activeWorks = MutableLiveData<List<ActiveWorkInfo>>()
    val activeWorks: LiveData<List<ActiveWorkInfo>> = _activeWorks

    private val _syncHistory = MutableLiveData<List<SyncHistoryItem>>()
    val syncHistory: LiveData<List<SyncHistoryItem>> = _syncHistory

    private val _canStartSync = MutableLiveData<Boolean>()
    val canStartSync: LiveData<Boolean> = _canStartSync

    private var currentSyncWorkId: String? = null

    fun loadSyncData() {
        viewModelScope.launch {
            updateNetworkStatus()
            updateSyncStatistics()
            updateActiveWorks()
            updateSyncHistory()
            updateSyncAvailability()
        }
    }
    
    fun refreshData() {
        loadSyncData()
    }

    fun startFullSync() {
        viewModelScope.launch {
            try {
                val pendingProcesses = repository.getAllProcesses().first()
                if (pendingProcesses.isNotEmpty()) {
                    val processIds = pendingProcesses.map { it.id }
                    currentSyncWorkId = workScheduler.scheduleBulkSync(processIds)
                    
                    _currentSyncProgress.value = SyncProgressInfo(
                        statusMessage = "Iniciando sincronización completa...",
                        progressText = "Preparando ${processIds.size} procesos",
                        percentage = 0,
                        isIndeterminate = true
                    )
                    
                    _canStartSync.value = false
                    observeSyncProgress(currentSyncWorkId!!)
                }
            } catch (e: Exception) {
                // Handle error
                _currentSyncProgress.value = null
                _canStartSync.value = true
            }
        }
    }

    fun startBulkSync() {
        viewModelScope.launch {
            try {
                val pendingProcesses = repository.getAllProcesses().first()
                val bulkProcesses = pendingProcesses.take(5) // Limit for bulk sync
                
                if (bulkProcesses.isNotEmpty()) {
                    val processIds = bulkProcesses.map { it.id }
                    currentSyncWorkId = workScheduler.scheduleBulkSync(processIds, delayMinutes = 1)
                    
                    _currentSyncProgress.value = SyncProgressInfo(
                        statusMessage = "Iniciando sincronización masiva...",
                        progressText = "Seleccionados ${processIds.size} procesos",
                        percentage = 0,
                        isIndeterminate = true
                    )
                    
                    _canStartSync.value = false
                    observeSyncProgress(currentSyncWorkId!!)
                }
            } catch (e: Exception) {
                _currentSyncProgress.value = null
                _canStartSync.value = true
            }
        }
    }

    fun schedulePeriodicSync() {
        viewModelScope.launch {
            workScheduler.scheduleNetworkMonitoring()
            workScheduler.scheduleDailyCleanup()
            
            // Show confirmation or feedback
            addToSyncHistory(SyncHistoryItem(
                title = "Sincronización periódica programada",
                details = "Monitoreo de red y limpieza automática activados",
                timestamp = System.currentTimeMillis(),
                status = SyncHistoryStatus.SUCCESS,
                duration = 0
            ))
        }
    }

    fun cancelCurrentSync() {
        currentSyncWorkId?.let { workId ->
            // Cancel the work - WorkManager doesn't have direct cancel by ID for running work
            // But we can track and handle cancellation
            viewModelScope.launch {
                workScheduler.cancelAllSyncWork()
                _currentSyncProgress.value = null
                _canStartSync.value = true
                currentSyncWorkId = null
                
                addToSyncHistory(SyncHistoryItem(
                    title = "Sincronización cancelada",
                    details = "Operación cancelada por el usuario",
                    timestamp = System.currentTimeMillis(),
                    status = SyncHistoryStatus.CANCELLED,
                    duration = 0
                ))
            }
        }
    }

    fun clearSyncHistory() {
        _syncHistory.value = emptyList()
    }

    private suspend fun updateNetworkStatus() {
        val isConnected = networkConnectivityService.isNetworkAvailable()
        val details = networkConnectivityService.getNetworkStatusDescription()
        _networkStatus.value = NetworkStatus(
            isConnected = isConnected,
            details = details
        )
    }

    private suspend fun updateSyncStatistics() {
        try {
            // 🔄 Obtener datos reales de la base de datos
            val allProcesses = repository.getAllProcesses().first()
            val pendingProcesses = allProcesses.filter { it.requiresSync == true }.size
            
            // Contar servicios que están COMPLETED (offline) pero no SYNCED
            val pendingServices = allProcesses.sumOf { process ->
                var count = 0
                if (process.sessionStatus == ServiceStatus.COMPLETED) count++
                if (process.verifyStatus == ServiceStatus.COMPLETED) count++
                if (process.ocrStatus == ServiceStatus.COMPLETED) count++
                if (process.livenessStatus == ServiceStatus.COMPLETED) count++
                if (process.otoVerifyStatus == ServiceStatus.COMPLETED) count++
                if (process.finishStatus == ServiceStatus.COMPLETED) count++
                count
            }
            
            // Buscar el último proceso sincronizado
            val lastSyncedProcess = allProcesses
                .filter { it.lastSyncAttempt != null }
                .maxByOrNull { it.lastSyncAttempt ?: 0L }
            
            val lastSyncTime = if (lastSyncedProcess?.lastSyncAttempt != null) {
                formatLastSyncTime(lastSyncedProcess.lastSyncAttempt!!)
            } else {
                "Nunca"
            }
            
            _syncStats.value = SyncStatistics(
                pendingProcesses = pendingProcesses,
                pendingServices = pendingServices,
                lastSyncTime = lastSyncTime
            )
        } catch (e: Exception) {
            // Fallback en caso de error
            _syncStats.value = SyncStatistics(
                pendingProcesses = 0,
                pendingServices = 0,
                lastSyncTime = "Error al cargar"
            )
        }
    }

    private suspend fun updateActiveWorks() {
        // Get active work info from WorkScheduler
        val activeWorksList = mutableListOf<ActiveWorkInfo>()
        
        // Mock data for demonstration
        activeWorksList.add(
            ActiveWorkInfo(
                id = "work1",
                title = "Sincronizando Proceso ABC123",
                details = "Servicio: Verificación de documento • Estado: En progreso",
                progress = 65,
                duration = "2min"
            )
        )
        
        _activeWorks.value = activeWorksList
    }

    private suspend fun updateSyncHistory() {
        try {
            // 🔄 Obtener datos reales de procesos
            val allProcesses = repository.getAllProcesses().first()
            val historyItems = mutableListOf<SyncHistoryItem>()
            
            // Crear historial basado en procesos reales
            allProcesses
                .sortedByDescending { it.updatedAt }
                .take(10) // Últimos 10 procesos
                .forEach { process ->
                    val shortKeyDisplay = process.shortKey.take(6).uppercase()
                    val status = when (process.overallStatus) {
                        KycProcessStatus.COMPLETED -> SyncHistoryStatus.SUCCESS
                        KycProcessStatus.FAILED -> SyncHistoryStatus.ERROR
                        KycProcessStatus.SYNCING -> SyncHistoryStatus.IN_PROGRESS
                        else -> SyncHistoryStatus.PENDING
                    }
                    
                    val serviceStatuses = listOf(
                        process.sessionStatus,
                        process.verifyStatus,
                        process.ocrStatus,
                        process.livenessStatus,
                        process.otoVerifyStatus,
                        process.finishStatus
                    )
                    
                    val syncedServices = serviceStatuses.count { it == ServiceStatus.SYNCED }
                    val completedServices = serviceStatuses.count { it == ServiceStatus.COMPLETED }
                    val totalCompletedServices = syncedServices + completedServices
                    
                    val details = when {
                        syncedServices == 6 -> "6 servicios sincronizados"
                        syncedServices > 0 && completedServices > 0 -> "$syncedServices sync, $completedServices pendientes de sync"
                        syncedServices > 0 -> "$syncedServices de 6 servicios sincronizados"
                        completedServices > 0 -> "$completedServices de 6 servicios completados (offline)"
                        totalCompletedServices == 0 -> "Sin servicios completados"
                        else -> "Proceso offline completado"
                    }
                    
                    val duration = if (process.lastSyncAttempt != null && process.createdAt < process.lastSyncAttempt!!) {
                        process.lastSyncAttempt!! - process.createdAt
                    } else {
                        0L
                    }
                    
                    historyItems.add(
                        SyncHistoryItem(
                            title = "Proceso $shortKeyDisplay",
                            details = details,
                            timestamp = process.lastSyncAttempt ?: process.updatedAt,
                            status = status,
                            duration = duration
                        )
                    )
                }
            
            // Si no hay procesos, mostrar mensaje informativo
            if (historyItems.isEmpty()) {
                historyItems.add(
                    SyncHistoryItem(
                        title = "Sin procesos KYC",
                        details = "Inicia un proceso KYC para ver el historial",
                        timestamp = System.currentTimeMillis(),
                        status = SyncHistoryStatus.PENDING,
                        duration = 0
                    )
                )
            }
            
            _syncHistory.value = historyItems
        } catch (e: Exception) {
            // Fallback en caso de error
            _syncHistory.value = listOf(
                SyncHistoryItem(
                    title = "Error al cargar historial",
                    details = "No se pudo acceder a la base de datos",
                    timestamp = System.currentTimeMillis(),
                    status = SyncHistoryStatus.ERROR,
                    duration = 0
                )
            )
        }
    }

    private suspend fun updateSyncAvailability() {
        val hasActiveSync = currentSyncWorkId != null
        val isNetworkAvailable = networkConnectivityService.isNetworkAvailable()
        _canStartSync.value = !hasActiveSync && isNetworkAvailable
    }

    private fun observeSyncProgress(workId: String) {
        viewModelScope.launch {
            // Observar progreso real basado en servicios
            while (currentSyncWorkId == workId) {
                try {
                    val allProcesses = repository.getAllProcesses().first()
                    val processesToSync = allProcesses.filter { it.requiresSync == true }
                    
                    if (processesToSync.isEmpty()) {
                        // No hay procesos que sincronizar, completar
                        _currentSyncProgress.value = null
                        _canStartSync.value = true
                        currentSyncWorkId = null
                        
                        addToSyncHistory(SyncHistoryItem(
                            title = "Sincronización completa",
                            details = "No hay procesos pendientes",
                            timestamp = System.currentTimeMillis(),
                            status = SyncHistoryStatus.SUCCESS,
                            duration = 0
                        ))
                        break
                    }
                    
                    // Calcular progreso basado en servicios
                    var totalServices = 0
                    var syncedServices = 0
                    var currentServiceName = ""
                    
                    processesToSync.forEach { process ->
                        val services = listOf(
                            "Session" to process.sessionStatus,
                            "Verify" to process.verifyStatus,
                            "OCR" to process.ocrStatus,
                            "Liveness" to process.livenessStatus,
                            "OtoVerify" to process.otoVerifyStatus,
                            "Finish" to process.finishStatus
                        )
                        
                        services.forEach { (serviceName, status) ->
                            totalServices++
                            if (status == ServiceStatus.SYNCED) {
                                syncedServices++
                            } else if (status == ServiceStatus.COMPLETED && currentServiceName.isEmpty()) {
                                currentServiceName = serviceName
                            }
                        }
                    }
                    
                    val progressPercentage = if (totalServices > 0) {
                        (syncedServices * 100) / totalServices
                    } else 0
                    
                    val statusMessage = if (currentServiceName.isNotEmpty()) {
                        "Sincronizando servicio: $currentServiceName"
                    } else {
                        "Sincronizando servicios de KYC..."
                    }
                    
                    _currentSyncProgress.value = SyncProgressInfo(
                        statusMessage = statusMessage,
                        progressText = "$syncedServices de $totalServices servicios sincronizados",
                        percentage = progressPercentage,
                        isIndeterminate = progressPercentage == 0
                    )
                    
                    // Si todos los servicios están sincronizados, completar
                    if (syncedServices == totalServices) {
                        kotlinx.coroutines.delay(1000) // Pequeña pausa para mostrar 100%
                        _currentSyncProgress.value = null
                        _canStartSync.value = true
                        currentSyncWorkId = null
                        
                        addToSyncHistory(SyncHistoryItem(
                            title = "Sincronización completa exitosa",
                            details = "$syncedServices servicios sincronizados",
                            timestamp = System.currentTimeMillis(),
                            status = SyncHistoryStatus.SUCCESS,
                            duration = 0
                        ))
                        break
                    }
                    
                } catch (e: Exception) {
                    // Error en sincronización
                    _currentSyncProgress.value = null
                    _canStartSync.value = true
                    currentSyncWorkId = null
                    
                    addToSyncHistory(SyncHistoryItem(
                        title = "Error en sincronización",
                        details = "No se pudo completar la sincronización",
                        timestamp = System.currentTimeMillis(),
                        status = SyncHistoryStatus.ERROR,
                        duration = 0
                    ))
                    break
                }
                
                // Verificar cada 2 segundos
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun addToSyncHistory(item: SyncHistoryItem) {
        val currentHistory = _syncHistory.value?.toMutableList() ?: mutableListOf()
        currentHistory.add(0, item) // Add to beginning
        if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.size - 1) // Keep only last 10
        }
        _syncHistory.value = currentHistory
    }

    private fun formatLastSyncTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val hours = diff / (60 * 60 * 1000)
        val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)
        
        return when {
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}min ago"
            else -> "Just now"
        }
    }

    // Data classes
    data class NetworkStatus(
        val isConnected: Boolean,
        val details: String
    )

    data class SyncStatistics(
        val pendingProcesses: Int,
        val pendingServices: Int,
        val lastSyncTime: String
    )

    data class SyncProgressInfo(
        val statusMessage: String,
        val progressText: String,
        val percentage: Int,
        val isIndeterminate: Boolean
    )

    data class ActiveWorkInfo(
        val id: String,
        val title: String,
        val details: String,
        val progress: Int,
        val duration: String
    )

    data class SyncHistoryItem(
        val title: String,
        val details: String,
        val timestamp: Long,
        val status: SyncHistoryStatus,
        val duration: Long // in milliseconds
    )

    enum class SyncHistoryStatus {
        SUCCESS, ERROR, WARNING, CANCELLED, IN_PROGRESS, PENDING
    }
}