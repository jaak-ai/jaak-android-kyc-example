package com.jaak.kyc.domain.offline

import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.data.repository.KycOfflineRepository
import javax.inject.Inject

class OtoVerifyOfflineUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    suspend operator fun invoke(processId: String, otoVerifyRequest: OtoVerifyRequest): Result<Unit> {
        return kycOfflineRepository.executeOtoVerify(processId, otoVerifyRequest)
    }
}