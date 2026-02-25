package ai.jaak.kyc.data.model.api

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del endpoint GET /api/v1/kyc/session
 * Usado para LISTAR/OBTENER sesiones KYC existentes (con paginación)
 */
data class SessionListResponse(
    @SerializedName("page")
    val page: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("totalDocs")
    val totalDocs: Int,

    @SerializedName("totalPages")
    val totalPages: Int,

    @SerializedName("nextPage")
    val nextPage: Boolean,

    @SerializedName("prevPage")
    val prevPage: Boolean,

    @SerializedName("docList")
    val docList: List<SessionListItem>
)

/**
 * Cada item individual en el listado de sesiones
 */
data class SessionListItem(
    @SerializedName("sessionID")
    val sessionID: String,

    @SerializedName("contactName")
    val contactName: String?,

    @SerializedName("verificationType")
    val verificationType: String?,

    @SerializedName("verification")
    val verification: SessionListVerification?,

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
    val location: SessionListLocation?,

    @SerializedName("statusDetail")
    val statusDetail: SessionListStatusDetail?
)

data class SessionListVerification(
    @SerializedName("EMAIL")
    val email: String?,

    @SerializedName("WHATSAPP")
    val whatsapp: String?,

    @SerializedName("whatsappDetail")
    val whatsappDetail: SessionListChannelDetail?,

    @SerializedName("SMS")
    val sms: String?,

    @SerializedName("smsDetail")
    val smsDetail: SessionListChannelDetail?
)

data class SessionListChannelDetail(
    @SerializedName("status")
    val status: String?,

    @SerializedName("message")
    val message: String?
)

data class SessionListLocation(
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

data class SessionListStatusDetail(
    @SerializedName("state")
    val state: String?,

    @SerializedName("stage")
    val stage: String?
)
