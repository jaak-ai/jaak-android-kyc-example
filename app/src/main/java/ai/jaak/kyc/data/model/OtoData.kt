package ai.jaak.kyc.data.model

/**
 * Modelos para datos de comparación One-to-One (1:1)
 */

/**
 * Item para filas de propiedad-resultado (ej: Gafas - No detectado)
 */
data class PropertyResultItem(
    val property: String,
    val result: String,
    val isCorrect: Boolean  // true = verde (correcto), false = rojo (incorrecto)
)

/**
 * Item para tabla de comparación con 4 columnas
 */
data class ComparisonItem(
    val property: String,
    val dataReceived: String,
    val dataExpected: String,
    val result: String,
    val isCorrect: Boolean
)

/**
 * Item para tabla de calidad de imagen con 4 columnas
 * (Propiedad, Datos de imagen, Datos esperados, Resultados)
 */
data class ImageQualityItem(
    val property: String,
    val imageData: String,      // Valor del servicio
    val expectedData: String,   // Valor esperado (fijo)
    val isCorrect: Boolean      // Resultado de la comparación
)
