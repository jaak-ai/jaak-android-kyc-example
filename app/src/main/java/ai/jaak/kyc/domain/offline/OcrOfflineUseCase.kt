package ai.jaak.kyc.domain.offline

import ai.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Request
import ai.jaak.kyc.data.repository.KycOfflineRepository
import javax.inject.Inject

class OcrOfflineUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    suspend operator fun invoke(processId: String, ocrRequest: DocumentExtractV4Request): Result<Unit> {
        return kycOfflineRepository.executeOcr(processId, ocrRequest)
    }
}