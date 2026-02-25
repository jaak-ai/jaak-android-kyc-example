package ai.jaak.kyc.domain

import ai.jaak.kyc.data.JaakDBRepository
import ai.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import ai.jaak.kyc.data.model.ocr.DocumentExtraBothResponse
import retrofit2.Response
import javax.inject.Inject

class OcrUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(apiKey: String, documentExtraBothRequest: DocumentExtraBothRequest): Response<DocumentExtraBothResponse> {
        return repository.ocrApi(apiKey, documentExtraBothRequest)
    }
}