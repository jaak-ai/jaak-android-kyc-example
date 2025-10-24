package com.jaak.kyc.data.model.api

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del endpoint GET /api/v1/kyc/session/{id}
 * Usado para obtener DETALLE COMPLETO de una sesión específica
 */
data class SessionDetailResponse(
    @SerializedName("session")
    val session: SessionDetailInfo,

    @SerializedName("summary")
    val summary: SessionDetailSummary,

    @SerializedName("flow")
    val flow: List<SessionDetailFlowEvent>
)

/**
 * Información general de la sesión
 */
data class SessionDetailInfo(
    @SerializedName("sessionID")
    val sessionID: String,

    @SerializedName("contactName")
    val contactName: String?,

    @SerializedName("verificationType")
    val verificationType: String?,

    @SerializedName("verification")
    val verification: SessionDetailVerification?,

    @SerializedName("flowName")
    val flowName: String?,

    @SerializedName("consent")
    val consent: String?,

    @SerializedName("createdAt")
    val createdAt: String?,

    @SerializedName("startDate")
    val startDate: String?,

    @SerializedName("updateDate")
    val updateDate: String?,

    @SerializedName("endDate")
    val endDate: String?,

    @SerializedName("origin")
    val origin: String?,

    @SerializedName("score")
    val score: Double?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("shortKey")
    val shortKey: String?,

    @SerializedName("validation")
    val validation: String?,

    @SerializedName("flowType")
    val flowType: String?,

    @SerializedName("rigelUrl")
    val rigelUrl: String?,

    @SerializedName("location")
    val location: SessionDetailLocation?,

    @SerializedName("statusDetail")
    val statusDetail: SessionDetailStatusDetail?
)

data class SessionDetailVerification(
    @SerializedName("EMAIL")
    val email: String?,

    @SerializedName("WHATSAPP")
    val whatsapp: String?,

    @SerializedName("whatsappDetail")
    val whatsappDetail: SessionDetailChannelDetail?,

    @SerializedName("SMS")
    val sms: String?,

    @SerializedName("smsDetail")
    val smsDetail: SessionDetailChannelDetail?
)

data class SessionDetailChannelDetail(
    @SerializedName("status")
    val status: String?,

    @SerializedName("message")
    val message: String?
)

data class SessionDetailLocation(
    @SerializedName("city")
    val city: String?,

    @SerializedName("state")
    val state: String?,

    @SerializedName("country")
    val country: String?,

    @SerializedName("latitude")
    val latitude: Double?,

    @SerializedName("longitude")
    val longitude: Double?
)

data class SessionDetailStatusDetail(
    @SerializedName("state")
    val state: String?,

    @SerializedName("stage")
    val stage: String?
)

/**
 * Resumen con scores y medios (foto base64, video URL)
 */
data class SessionDetailSummary(
    @SerializedName("photo")
    val photo: String?, // Base64 de la mejor foto del usuario

    @SerializedName("totalTime")
    val totalTime: Double?, // Tiempo total en minutos

    @SerializedName("name")
    val name: String?,

    @SerializedName("lastName")
    val lastName: String?,

    @SerializedName("scores")
    val scores: SessionDetailScores?,

    @SerializedName("rigelFullVideo")
    val rigelFullVideo: String? // URL firmada al video completo
)

data class SessionDetailScores(
    @SerializedName("liveness")
    val liveness: Double?,

    @SerializedName("document")
    val document: Double?,

    @SerializedName("oneToOne")
    val oneToOne: Double?,

    @SerializedName("total")
    val total: Double?,

    @SerializedName("status")
    val status: String? // "pass" o "fail"
)

/**
 * Evento en el flujo del proceso KYC (dinámico)
 */
data class SessionDetailFlowEvent(
    @SerializedName("action")
    val action: String, // Ej: "document-process", "liveness-process", "oto-process"

    @SerializedName("createdAt")
    val createdAt: String?,

    @SerializedName("eventId")
    val eventId: String?,

    @SerializedName("flow")
    val flow: List<SessionDetailFlowResource>?,

    @SerializedName("meta")
    val meta: SessionDetailFlowMeta?,

    @SerializedName("request")
    val request: SessionDetailFlowRequest?
)

/**
 * Recurso procesado dentro de un evento
 */
data class SessionDetailFlowResource(
    @SerializedName("resource")
    val resource: String, // Ej: "document-ocr", "liveness", "one-to-one"

    @SerializedName("score")
    val score: Double?,

    @SerializedName("status")
    val status: String?, // "completed", "failed", etc.

    @SerializedName("meta")
    val meta: SessionDetailResourceMeta?
)

data class SessionDetailResourceMeta(
    @SerializedName("extra")
    val extra: SessionDetailResourceExtra?
)

data class SessionDetailResourceExtra(
    @SerializedName("evaluation")
    val evaluation: Map<String, Any>? // Datos dinámicos de evaluación
)

data class SessionDetailFlowMeta(
    @SerializedName("extra")
    val extra: Map<String, Any>? // Metadata adicional
)

data class SessionDetailFlowRequest(
    @SerializedName("id")
    val id: String?,

    @SerializedName("ip")
    val ip: String?,

    @SerializedName("path")
    val path: String?,

    @SerializedName("method")
    val method: String?,

    @SerializedName("meta")
    val meta: SessionDetailRequestMeta?
)

data class SessionDetailRequestMeta(
    @SerializedName("request")
    val request: Map<String, Any>?,

    @SerializedName("response")
    val response: Map<String, Any>?
)
