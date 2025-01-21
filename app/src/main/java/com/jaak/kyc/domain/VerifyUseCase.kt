package com.jaak.kyc.domain

import com.jaak.kyc.data.JaakDBRepository
import com.jaak.kyc.data.model.verify.VerifyRequest
import com.jaak.kyc.data.model.verify.VerifyResponse
import retrofit2.Response
import javax.inject.Inject

class VerifyUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(apiKey: String, verifyRequest: VerifyRequest): Response<VerifyResponse> {
        return repository.verifyApi(apiKey, verifyRequest)
    }
}