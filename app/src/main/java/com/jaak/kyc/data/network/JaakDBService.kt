package com.jaak.kyc.data.network

import android.util.Log
import com.jaak.kyc.data.model.ErrorModel
import com.jaak.kyc.data.model.finish.FinishResponse
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyResponse
import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.model.ocr.DocumentExtraBothResponse
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.data.model.otoverify.OtoVerifyResponse
import com.jaak.kyc.data.model.session.SessionResponse
import com.jaak.kyc.data.model.verify.VerifyRequest
import com.jaak.kyc.data.model.verify.VerifyResponse
import com.jaak.kyc.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class JaakDBService @Inject constructor(private val api: JaakDBApiClient) {
    private suspend fun <T> callService(serviceCall: suspend () -> Response<T>): Response<T> {
        try {
            val response = serviceCall.invoke()
            Log.e("respuesta servicio",response.toString())
            if (!response.isSuccessful) {
                val errorModel = ErrorModel("Error en la llamada al servicio: ${response.message()}",false, response.code())
                val errorBody = Utils.resultsModelToResponseBody(errorModel)
                return Response.error(response.code(), errorBody)
            }
            return if (response.body() != null){
                Response.success(response.code(),response.body())
            }else{
                val errorModel = ErrorModel("El cuerpo de la respuesta está vacío", false,response.code())
                val errorBody = Utils.resultsModelToResponseBody(errorModel)
                Response.error(response.code(), errorBody)
            }
        } catch (e: IOException) {
            val errorModel = ErrorModel("Error de conexión: ${e.message}", false,500)
            val errorBody = Utils.resultsModelToResponseBody(errorModel)
            return Response.error(500, errorBody)
        } catch (e: Exception) {
            val errorModel = ErrorModel("Error inesperado: ${e.message}", false,500)
            val errorBody = Utils.resultsModelToResponseBody(errorModel)
            return Response.error(500, errorBody)
        }
    }

    suspend fun verifyApi(apiKey: String, verifyRequest: VerifyRequest): Response<VerifyResponse> {
        return withContext(Dispatchers.IO) {
            callService{ api.verifyApi(apiKey, verifyRequest) }
        }
    }

    suspend fun ocrApi(apiKey: String, documentExtraBothRequest: DocumentExtraBothRequest): Response<DocumentExtraBothResponse> {
        return withContext(Dispatchers.IO) {
            callService{ api.documentExtraBothApi(apiKey, documentExtraBothRequest) }
        }
    }

    suspend fun livenessVerifyApi(apiKey: String, livenessVerifyRequest: LivenessVerifyRequest): Response<LivenessVerifyResponse> {
        return withContext(Dispatchers.IO) {
            callService{ api.livenessVerifyApi(apiKey, livenessVerifyRequest) }
        }
    }

    suspend fun otoVerifyApi(apiKey: String, otoVerifyRequest: OtoVerifyRequest): Response<OtoVerifyResponse> {
        return withContext(Dispatchers.IO) {
            callService{ api.otoVerifyApi(apiKey, otoVerifyRequest) }
        }
    }

    suspend fun sessionApi(shortKey: String, originDevice: String): Response<SessionResponse> {
        return withContext(Dispatchers.IO) {
            callService{ api.sessionApi(shortKey, originDevice) }
        }
    }

    suspend fun finishApi(apiKey: String): Response<FinishResponse> {
        return withContext(Dispatchers.IO) {
            callService{ api.finishApi(apiKey) }
        }
    }
}