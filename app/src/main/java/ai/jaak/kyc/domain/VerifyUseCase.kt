package ai.jaak.kyc.domain

import ai.jaak.kyc.data.JaakDBRepository
import ai.jaak.kyc.data.model.verify.VerifyRequest
import ai.jaak.kyc.data.model.verify.VerifyResponse
import retrofit2.Response
import javax.inject.Inject

class VerifyUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(apiKey: String, verifyRequest: VerifyRequest): Response<VerifyResponse> {
        return repository.verifyApi(apiKey, verifyRequest)
    }
}