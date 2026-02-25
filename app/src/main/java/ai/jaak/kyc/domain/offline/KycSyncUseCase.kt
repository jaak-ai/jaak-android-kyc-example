package ai.jaak.kyc.domain.offline

import ai.jaak.kyc.data.repository.KycSyncRepository
import javax.inject.Inject

class KycSyncUseCase @Inject constructor(
    private val kycSyncRepository: KycSyncRepository
) {
    
    suspend fun syncProcess(processId: String): KycSyncRepository.SyncResult {
        return kycSyncRepository.syncProcess(processId)
    }
    
    suspend fun syncAllProcesses(): List<Pair<String, KycSyncRepository.SyncResult>> {
        return kycSyncRepository.syncAllProcesses()
    }
}