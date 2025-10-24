package com.jaak.kyc.data.model

/**
 * Modelo para mostrar sesiones KYC en la lista
 */
data class KycSessionItem(
    val sessionID: String,           // ID de MongoDB (24 chars hex) - Para obtener detalle
    val shortkey: String,            // Código de la sesión (ej: FCtsxUJ) - Para mostrar
    val userName: String,            // Nombre del usuario (ej: qa, daniela)
    val flowName: String,            // Nombre del flujo (ej: qa test)
    val dateTime: String,            // Fecha y hora (ej: 09/10/2025 10:05)
    val score: Int,                  // Puntaje (0-100)
    val documentStatus: String,      // Estado del documento (Documento, Pendiente, Caducado, etc.)
    val processStatus: String,       // Estado del proceso (En proceso, Finalizado)
    val kycStatus: KycStatus         // Estado general del KYC
)

/**
 * Estados posibles del KYC
 */
enum class KycStatus {
    PENDIENTE,      // Gris - Reloj
    EXPIRADO,       // Amarillo - Reloj con alerta
    RECHAZADO,      // Rojo - X
    EXITOSO         // Verde - Check
}
