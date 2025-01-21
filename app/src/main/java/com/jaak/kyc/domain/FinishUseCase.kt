package com.jaak.kyc.domain

import com.jaak.kyc.data.JaakDBRepository
import com.jaak.kyc.data.model.finish.FinishResponse
import retrofit2.Response
import javax.inject.Inject

class FinishUseCase @Inject constructor(private val repository: JaakDBRepository) {
    suspend operator fun invoke(apiKey: String): Response<FinishResponse> {
        return repository.finishApi(apiKey)
    }
}