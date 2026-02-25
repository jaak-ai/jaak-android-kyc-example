package ai.jaak.kyc.domain.offline

import ai.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import ai.jaak.kyc.data.repository.KycOfflineRepository
import javax.inject.Inject

class OtoVerifyOfflineUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    suspend operator fun invoke(processId: String, otoVerifyRequest: OtoVerifyRequest): Result<Unit> {
        return kycOfflineRepository.executeOtoVerify(processId, otoVerifyRequest)
    }
}