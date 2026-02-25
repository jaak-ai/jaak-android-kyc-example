package ai.jaak.kyc.domain

import ai.jaak.kyc.data.JaakDBRepository
import ai.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import ai.jaak.kyc.data.model.livenessverify.LivenessVerifyResponse
import retrofit2.Response
import javax.inject.Inject

class LivenessVerifyUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(apiKey: String, livenessVerifyRequest: LivenessVerifyRequest): Response<LivenessVerifyResponse> {
        return repository.livenessVerifyApi(apiKey, livenessVerifyRequest)
    }
}