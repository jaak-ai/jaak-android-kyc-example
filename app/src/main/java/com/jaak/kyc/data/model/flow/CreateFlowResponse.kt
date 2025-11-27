package com.jaak.kyc.data.model.flow

import com.google.gson.annotations.SerializedName

/**
 * Response del endpoint de crear flujo KYC
 * POST /v1/kyc/flow
 */
data class CreateFlowResponse(
    @SerializedName("sessionUrl")
    val sessionUrl: String, // URL de la sesión (ej: "https://kyc.jaak.ai/session/ABC123D")
    
    @SerializedName("transparent")
    val transparent: String? = null // Licencia para OpenTelemetry (ej: "00-abc123def456-xyz789-01")
) {
    /**
     * Extrae el shortKey de la sessionUrl
     * Ejemplo: "https://kyc.jaak.ai/session/ABC123D" -> "ABC123D"
     */
    fun extractShortKey(): String {
        return sessionUrl.substringAfterLast("/")
    }
    
    /**
     * Extrae la licencia del campo transparent
     * Formato: "00-LICENCIA-resto-01" -> "LLICENCIA"
     * Ejemplo: "00-abc123def456-xyz789-01" -> "Labc123def456"
     */
    fun extractLicense(): String? {
        if (transparent.isNullOrEmpty()) return null
        
        // Separar por guiones
        val parts = transparent.split("-")
        if (parts.size < 2) return null
        
        // Tomar la segunda parte y agregar "L" al inicio
        return "L${parts[1]}"
    }
    
    companion object {
        /**
         * Extrae la licencia del header traceparent de la respuesta HTTP
         * Formato: "00-LICENCIA-resto-01" -> "LLICENCIA"
         * Ejemplo: "00-abc123def456-xyz789-01" -> "Labc123def456"
         */
        fun extractLicenseFromHeader(traceparent: String?): String? {
            if (traceparent.isNullOrEmpty()) return null
            
            // Separar por guiones
            val parts = traceparent.split("-")
            if (parts.size < 2) return null
            
            // Tomar la segunda parte y agregar "L" al inicio
            return "L${parts[1]}"
        }
    }
}
