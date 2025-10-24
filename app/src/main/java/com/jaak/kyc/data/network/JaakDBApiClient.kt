package com.jaak.kyc.data.network

import com.jaak.kyc.data.model.finish.FinishResponse
import com.jaak.kyc.data.model.geocoding.GeocodingRequest
import com.jaak.kyc.data.model.geocoding.GeocodingResponse
import com.jaak.kyc.data.model.blacklist.BlacklistRequest
import com.jaak.kyc.data.model.blacklist.BlacklistResponse
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyResponse
import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.model.ocr.DocumentExtraBothResponse
import com.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Request
import com.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Response
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.data.model.otoverify.OtoVerifyResponse
import com.jaak.kyc.data.model.session.SessionResponse
import com.jaak.kyc.data.model.verify.VerifyRequest
import com.jaak.kyc.data.model.verify.VerifyResponse
import com.jaak.kyc.data.model.api.SessionListResponse
import com.jaak.kyc.data.model.api.SessionDetailResponse
import com.jaak.kyc.data.model.api.LoginRequest
import com.jaak.kyc.data.model.api.LoginResponse
import com.jaak.kyc.data.model.flow.CreateFlowRequest
import com.jaak.kyc.data.model.flow.CreateFlowResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JaakDBApiClient {

    // POST /api/v1/kyc/session - CREAR nueva sesión con shortkey
    @POST("api/v1/kyc/session")
    suspend fun sessionApi(@Header("Short-Key") shortKey: String,
                           @Header("Origin-Device") originDevice: String): Response<SessionResponse>

    @POST("api/v1/location/reverse-geocode")
    suspend fun geocodingApi(@Header("Authorization") auth: String,
                             @Body request: GeocodingRequest): Response<GeocodingResponse>

    @POST("api/v3/document/verify")
    suspend fun verifyApi(@Header("Authorization") auth: String, @Body request: VerifyRequest): Response<VerifyResponse>

    @POST("api/v4/document/extract")
    suspend fun documentExtraBothApi(@Header("Authorization") auth: String, @Body request: DocumentExtraBothRequest): Response<DocumentExtraBothResponse>

    @POST("api/v4/document/extract")
    suspend fun documentExtractV4Api(@Header("Authorization") auth: String, @Body request: DocumentExtractV4Request): Response<DocumentExtractV4Response>

    @POST("api/v1/liveness/verify-and-bestframe")
    suspend fun livenessVerifyApi(@Header("Authorization") auth: String,
                                  @Body request: LivenessVerifyRequest): Response<LivenessVerifyResponse>

    @POST("api/v2/oto/verify")
    suspend fun otoVerifyApi(@Header("Authorization") auth: String,
                             @Body request: OtoVerifyRequest): Response<OtoVerifyResponse>

    @POST("api/v1/kyc/session/finish")
    suspend fun finishApi(@Header("Authorization") auth: String): Response<FinishResponse>

    @POST("api/v2/blacklist/investigate")
    suspend fun blacklistInvestigateApi(@Header("Authorization") auth: String,
                                        @Body request: BlacklistRequest): Response<BlacklistResponse>

    // GET /api/v1/kyc/session - LISTAR sesiones existentes
    @GET("api/v1/kyc/session")
    suspend fun getSessionListApi(
        @Header("Authorization") auth: String,
        @Header("Language") language: String = "es",
        @Query("id") id: String? = null,
        @Query("shortKey") shortKey: String? = null,
        @Query("contactName") contactName: String? = null,
        @Query("flowName") flowName: String? = null,
        @Query("limit") limit: Int? = 20,
        @Query("page") page: Int? = 1,
        @Query("min-created-at") minCreatedAt: String? = null,
        @Query("max-created-at") maxCreatedAt: String? = null
    ): Response<SessionListResponse>

    // GET /api/v1/kyc/session/{id} - OBTENER detalle de sesión específica
    @GET("api/v1/kyc/session/{id}")
    suspend fun getSessionDetailApi(
        @Path("id") sessionId: String,
        @Header("Authorization") auth: String
    ): Response<SessionDetailResponse>

    // POST /api/auth/sign-in - LOGIN de usuario
    @POST("api/auth/sign-in")
    suspend fun loginApi(@Body request: LoginRequest): Response<LoginResponse>

    // POST /api/v1/kyc/flow - CREAR flujo KYC y obtener sessionUrl
    @POST("api/v1/kyc/flow")
    suspend fun createFlowApi(
        @Header("Authorization") auth: String,
        @Body request: CreateFlowRequest
    ): Response<CreateFlowResponse>

}