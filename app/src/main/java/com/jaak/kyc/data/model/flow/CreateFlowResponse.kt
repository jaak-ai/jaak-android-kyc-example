package com.jaak.kyc.data.model.flow

import com.google.gson.annotations.SerializedName

/**
 * Response del endpoint de crear flujo KYC
 * POST /v1/kyc/flow
 */
data class CreateFlowResponse(
    @SerializedName("sessionUrl")
    val sessionUrl: String // URL de la sesión (ej: "https://kyc.jaak.ai/session/ABC123D")
) {
    /**
     * Extrae el shortKey de la sessionUrl
     * Ejemplo: "https://kyc.jaak.ai/session/ABC123D" -> "ABC123D"
     */
    fun extractShortKey(): String {
        return sessionUrl.substringAfterLast("/")
    }
}
