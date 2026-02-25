package ai.jaak.kyc.domain.offline

import ai.jaak.kyc.data.repository.KycOfflineRepository
import javax.inject.Inject

class SessionOfflineUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    suspend operator fun invoke(processId: String, shortKey: String): Result<Unit> {
        return kycOfflineRepository.executeSession(processId, shortKey)
    }
}