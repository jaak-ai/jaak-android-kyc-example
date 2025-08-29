package com.jaak.kyc.domain.offline

import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.repository.KycOfflineRepository
import javax.inject.Inject

class OcrOfflineUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    suspend operator fun invoke(processId: String, ocrRequest: DocumentExtraBothRequest): Result<Unit> {
        return kycOfflineRepository.executeOcr(processId, ocrRequest)
    }
}