package ai.jaak.kyc.data

import ai.jaak.kyc.data.model.finish.FinishResponse
import ai.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import ai.jaak.kyc.data.model.livenessverify.LivenessVerifyResponse
import ai.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import ai.jaak.kyc.data.model.ocr.DocumentExtraBothResponse
import ai.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import ai.jaak.kyc.data.model.otoverify.OtoVerifyResponse
import ai.jaak.kyc.data.model.session.SessionResponse
import ai.jaak.kyc.data.model.verify.VerifyRequest
import ai.jaak.kyc.data.model.verify.VerifyResponse
import ai.jaak.kyc.data.network.JaakDBService
import retrofit2.Response
import javax.inject.Inject

class JaakDBRepository @Inject constructor(
    private val api: JaakDBService
) {

    suspend fun sessionApi(shortKey: String, originDevice: String): Response<SessionResponse> {
        return api.sessionApi(shortKey, originDevice)
    }

    suspend fun verifyApi(apiKey: String, verifyRequest: VerifyRequest): Response<VerifyResponse> {
        return api.verifyApi(apiKey, verifyRequest)
    }

    suspend fun ocrApi(apiKey: String, documentExtraBothRequest: DocumentExtraBothRequest): Response<DocumentExtraBothResponse> {
        return api.ocrApi(apiKey, documentExtraBothRequest)
    }

    suspend fun livenessVerifyApi(apiKey: String, livenessVerifyRequest: LivenessVerifyRequest): Response<LivenessVerifyResponse> {
        return api.livenessVerifyApi(apiKey, livenessVerifyRequest)
    }

    suspend fun otoVerifyApi(apiKey: String, otoVerifyRequest: OtoVerifyRequest): Response<OtoVerifyResponse> {
        return api.otoVerifyApi(apiKey, otoVerifyRequest)
    }

    suspend fun finishApi(apiKey: String): Response<FinishResponse> {
        return api.finishApi(apiKey)
    }
}