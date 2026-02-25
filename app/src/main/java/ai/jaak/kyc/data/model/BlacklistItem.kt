package ai.jaak.kyc.data.model

/**
 * Modelo para representar un item de lista de blacklist
 */
data class BlacklistItem(
    val name: String,               // OFAC, INE, CURP, etc.
    val status: BlacklistStatus,    // RISK, VALID, RELIABLE
    val description: String,
    val attempts: Int,
    val processingTime: String,     // En milisegundos
    val detailedData: List<DataRow>? = null,
    val isRiskList: Boolean         // true = riesgo (izquierda), false = validación (derecha)
)

enum class BlacklistStatus(val displayName: String) {
    RISK("Riesgo"),
    VALID("Válido"),
    RELIABLE("Confiable")
}

/**
 * Modelo para una fila de datos detallados
 */
data class DataRow(
    val property: String,
    val value: String
)
