package ai.jaak.kyc.domain.offline

import ai.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import ai.jaak.kyc.data.repository.KycOfflineRepository
import javax.inject.Inject

class LivenessOfflineUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    suspend operator fun invoke(processId: String, livenessRequest: LivenessVerifyRequest): Result<Unit> {
        return kycOfflineRepository.executeLiveness(processId, livenessRequest)
    }
}