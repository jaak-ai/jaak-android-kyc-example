package com.jaak.kyc.domain

import com.jaak.kyc.data.JaakDBRepository
import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.model.ocr.DocumentExtraBothResponse
import retrofit2.Response
import javax.inject.Inject

class OcrUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(apiKey: String, documentExtraBothRequest: DocumentExtraBothRequest): Response<DocumentExtraBothResponse> {
        return repository.ocrApi(apiKey, documentExtraBothRequest)
    }
}