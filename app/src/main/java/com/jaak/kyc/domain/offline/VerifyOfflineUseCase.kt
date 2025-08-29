package com.jaak.kyc.domain.offline

import com.jaak.kyc.data.model.verify.VerifyRequest
import com.jaak.kyc.data.repository.KycOfflineRepository
import javax.inject.Inject

class VerifyOfflineUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    suspend operator fun invoke(processId: String, verifyRequest: VerifyRequest): Result<Unit> {
        return kycOfflineRepository.executeVerify(processId, verifyRequest)
    }
}