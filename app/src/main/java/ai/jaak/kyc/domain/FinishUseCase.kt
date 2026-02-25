package ai.jaak.kyc.domain

import ai.jaak.kyc.data.JaakDBRepository
import ai.jaak.kyc.data.model.finish.FinishResponse
import retrofit2.Response
import javax.inject.Inject

class FinishUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(apiKey: String): Response<FinishResponse> {
        return repository.finishApi(apiKey)
    }
}