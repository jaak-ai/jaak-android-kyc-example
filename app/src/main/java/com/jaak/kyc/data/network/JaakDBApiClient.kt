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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface JaakDBApiClient {

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

}