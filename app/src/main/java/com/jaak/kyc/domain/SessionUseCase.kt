package com.jaak.kyc.domain

import com.jaak.kyc.data.JaakDBRepository
import com.jaak.kyc.data.model.session.SessionResponse
import retrofit2.Response
import javax.inject.Inject

class SessionUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(shortKey : String, originDevioce: String): Response<SessionResponse> {
        return repository.sessionApi(shortKey, originDevioce)
    }
}