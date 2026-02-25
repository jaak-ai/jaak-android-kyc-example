package ai.jaak.kyc.domain.offline

import ai.jaak.kyc.data.repository.KycOfflineRepository
import javax.inject.Inject

class FinishOfflineUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    suspend operator fun invoke(processId: String): Result<Unit> {
        return kycOfflineRepository.executeFinish(processId)
    }
}