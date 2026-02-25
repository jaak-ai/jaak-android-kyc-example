package ai.jaak.kyc.data.model.flow

import com.google.gson.annotations.SerializedName

/**
 * Request para crear un flujo KYC
 * POST /v1/kyc/flow
 */
data class CreateFlowRequest(
    @SerializedName("name")
    val name: String, // Nombre de la persona a autenticar

    @SerializedName("flow")
    val flow: String, // Nombre del flujo

    @SerializedName("redirectUrl")
    val redirectUrl: String? = "", // URL de redirección (opcional)

    @SerializedName("countryDocument")
    val countryDocument: String, // País del documento (MEX, COL, etc.)

    @SerializedName("flowType")
    val flowType: String = "KYC", // Siempre "KYC"

    @SerializedName("verificationType")
    val verificationType: String = "", // WHATSAPP, SMS, EMAIL o vacío

    @SerializedName("verification")
    val verification: VerificationData = VerificationData()
)

/**
 * Datos de verificación para el flujo
 * IMPORTANTE: Los campos no pueden ser null, deben ser strings vacíos
 */
data class VerificationData(
    @SerializedName("EMAIL")
    val email: String = "",

    @SerializedName("SMS")
    val sms: String = "",

    @SerializedName("WHATSAPP")
    val whatsapp: String = ""
)
