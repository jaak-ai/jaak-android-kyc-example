package com.jaak.kyc.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.jaak.kyc.domain.service.ProcessErrorManager
import com.jaak.kyc.domain.service.ProcessTokenManager
import com.jaak.kyc.domain.service.NetworkConnectivityService
import com.jaak.kyc.data.repository.KycOfflineRepository
import com.jaak.kyc.work.WorkScheduler
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
        val pendingProcesses = repository.getAllProcesses().first().size
        val pendingServices = 0 // Mock count
        val lastSyncTime = formatLastSyncTime(System.currentTimeMillis() - (2 * 60 * 60 * 1000)) // 2 hours ago mock
        
        _syncStats.value = SyncStatistics(
            pendingProcesses = pendingProcesses,
            pendingServices = pendingServices,
            lastSyncTime = lastSyncTime
        )
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
        // Mock history data
        val historyItems = listOf(
            SyncHistoryItem(
                title = "Proceso ABC123 completado",
                details = "6 servicios sincronizados exitosamente",
                timestamp = System.currentTimeMillis() - (30 * 60 * 1000), // 30 minutes ago
                status = SyncHistoryStatus.SUCCESS,
                duration = 2300 // 2.3 seconds
            ),
            SyncHistoryItem(
                title = "Sincronización masiva",
                details = "3 procesos, 18 servicios completados",
                timestamp = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 hours ago
                status = SyncHistoryStatus.SUCCESS,
                duration = 15600 // 15.6 seconds
            ),
            SyncHistoryItem(
                title = "Error en Proceso DEF456",
                details = "Fallo en servicio de liveness",
                timestamp = System.currentTimeMillis() - (3 * 60 * 60 * 1000), // 3 hours ago
                status = SyncHistoryStatus.ERROR,
                duration = 5000 // 5 seconds
            )
        )
        
        _syncHistory.value = historyItems
    }

    private suspend fun updateSyncAvailability() {
        val hasActiveSync = currentSyncWorkId != null
        val isNetworkAvailable = networkConnectivityService.isNetworkAvailable()
        _canStartSync.value = !hasActiveSync && isNetworkAvailable
    }

    private fun observeSyncProgress(workId: String) {
        viewModelScope.launch {
            // Mock progress observation
            var progress = 0
            while (progress < 100 && currentSyncWorkId == workId) {
                kotlinx.coroutines.delay(1000)
                progress += 10
                
                _currentSyncProgress.value = SyncProgressInfo(
                    statusMessage = "Sincronizando servicios de KYC...",
                    progressText = "$progress% completado",
                    percentage = progress,
                    isIndeterminate = false
                )
            }
            
            if (currentSyncWorkId == workId) {
                // Sync completed
                _currentSyncProgress.value = null
                _canStartSync.value = true
                currentSyncWorkId = null
                
                addToSyncHistory(SyncHistoryItem(
                    title = "Sincronización completa exitosa",
                    details = "Todos los procesos sincronizados",
                    timestamp = System.currentTimeMillis(),
                    status = SyncHistoryStatus.SUCCESS,
                    duration = 10000
                ))
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
        SUCCESS, ERROR, WARNING, CANCELLED
    }
}