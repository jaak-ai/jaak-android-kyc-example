package ai.jaak.kyc.domain.offline

import ai.jaak.kyc.data.local.entity.KycProcessEntity
import ai.jaak.kyc.data.local.entity.KycProcessWithDetails
import ai.jaak.kyc.data.repository.KycOfflineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class KycProcessManagementUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    
    suspend fun createProcess(shortKey: String): String {
        return kycOfflineRepository.createKycProcess(shortKey)
    }
    
    fun getAllProcesses(): Flow<List<KycProcessEntity>> {
        return kycOfflineRepository.getAllProcesses()
    }
    
    fun getAllProcessesWithDetails(): Flow<List<KycProcessWithDetails>> {
        return kycOfflineRepository.getAllProcessesWithDetails()
    }
    
    suspend fun getProcessWithDetails(processId: String): KycProcessWithDetails? {
        return kycOfflineRepository.getProcessWithDetails(processId)
    }
    
    fun getProcessesRequiringSync(): Flow<List<KycProcessEntity>> {
        return kycOfflineRepository.getProcessesRequiringSync()
    }
    
    suspend fun getCountRequiringSync(): Int {
        return kycOfflineRepository.getCountRequiringSync()
    }
    
    fun isNetworkAvailable(): Boolean {
        return kycOfflineRepository.isNetworkAvailable()
    }
}