package com.jaak.kyc.domain

import com.jaak.kyc.data.JaakDBRepository
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyResponse
import retrofit2.Response
import javax.inject.Inject

class LivenessVerifyUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(apiKey: String, livenessVerifyRequest: LivenessVerifyRequest): Response<LivenessVerifyResponse> {
        return repository.livenessVerifyApi(apiKey, livenessVerifyRequest)
    }
}