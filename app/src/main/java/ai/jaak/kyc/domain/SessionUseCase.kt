package ai.jaak.kyc.domain

import ai.jaak.kyc.data.JaakDBRepository
import ai.jaak.kyc.data.model.session.SessionResponse
import retrofit2.Response
import javax.inject.Inject

class SessionUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(shortKey : String, originDevioce: String): Response<SessionResponse> {
        return repository.sessionApi(shortKey, originDevioce)
    }
}