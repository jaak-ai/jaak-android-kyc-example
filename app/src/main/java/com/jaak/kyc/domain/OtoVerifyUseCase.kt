package com.jaak.kyc.domain

import com.jaak.kyc.data.JaakDBRepository
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.data.model.otoverify.OtoVerifyResponse
import retrofit2.Response
import javax.inject.Inject

class OtoVerifyUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(apiKey: String, otoVerifyRequest: OtoVerifyRequest): Response<OtoVerifyResponse> {
        return repository.otoVerifyApi(apiKey, otoVerifyRequest)
    }
}