package ai.jaak.kyc.data.mapper

import ai.jaak.kyc.data.model.api.SessionListItem
import ai.jaak.kyc.data.model.KycSessionItem
import ai.jaak.kyc.data.model.KycStatus
import java.text.SimpleDateFormat
import java.util.*

object SessionMapper {

    /**
     * Convierte SessionListItem (de la API GET) a KycSessionItem (modelo UI)
     */
    fun toKycSessionItem(dto: SessionListItem): KycSessionItem {
        android.util.Log.d("SessionMapper", "========== MAPPING SESSION ==========")
        android.util.Log.d("SessionMapper", "SessionID: ${dto.sessionID}")
        android.util.Log.d("SessionMapper", "ShortKey: ${dto.shortKey}")
        android.util.Log.d("SessionMapper", "CreatedAt RAW: ${dto.createdAt}")
        android.util.Log.d("SessionMapper", "Status: ${dto.status}")
        android.util.Log.d("SessionMapper", "Validation: ${dto.validation}")
        android.util.Log.d("SessionMapper", "Score: ${dto.score}")

        val formattedDate = formatDateTime(dto.createdAt)
        val mappedStatus = mapKycStatus(dto.validation, dto.status)

        android.util.Log.d("SessionMapper", "FormattedDate: $formattedDate")
        android.util.Log.d("SessionMapper", "MappedStatus: $mappedStatus")
        android.util.Log.d("SessionMapper", "====================================")

        return KycSessionItem(
            sessionID = dto.sessionID,
            shortkey = dto.shortKey ?: dto.sessionID,
            userName = dto.contactName ?: "Sin nombre",
            flowName = dto.flowName ?: "Sin flujo",
            dateTime = formattedDate,
            score = calculateScore(dto.score),
            documentStatus = mapDocumentStatus(dto.validation, dto.status),
            processStatus = mapProcessStatus(dto.status, dto.statusDetail?.stage),
            kycStatus = mappedStatus
        )
    }

    /**
     * Formatea la fecha de la API al formato local
     * De: "2024-01-15T10:30:45.123Z" o "2024-01-15T10:30:45Z"
     * A: "15/01/2024 10:30"
     */
    private fun formatDateTime(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "-"

        return try {
            // Intentar primero formato sin milisegundos (más común)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)

            // Output format local
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: "-"
        } catch (e: Exception) {
            // Si falla, intentar con milisegundos
            try {
                val alternativeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                alternativeFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = alternativeFormat.parse(dateString)

                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                date?.let { outputFormat.format(it) } ?: "-"
            } catch (e: Exception) {
                android.util.Log.e("SessionMapper", "Error parsing date: $dateString", e)
                dateString // Fallback: mostrar fecha original
            }
        }
    }

    /**
     * Calcula el score como entero de 0-100
     */
    private fun calculateScore(score: Double?): Int {
        if (score == null) return 0
        return (score * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Mapea el estado del documento basado en validation y status
     */
    private fun mapDocumentStatus(validation: String?, status: String?): String {
        return when {
            status?.uppercase() == "PASS" -> "Aprobado"
            validation == "approved" -> "Aprobado"
            status?.uppercase() == "FAIL" -> "Rechazado"
            validation == "rejected" -> "Rechazado"
            validation == "pending" -> "Pendiente"
            status?.uppercase() == "EXPIRED" -> "Expirado"
            status == "completed" && validation == null -> "En revisión"
            status == "in_progress" -> "En proceso"
            else -> "Pendiente"
        }
    }

    /**
     * Mapea el estado del proceso basado en status y stage
     */
    private fun mapProcessStatus(status: String?, stage: String?): String {
        return when {
            status?.uppercase() == "PASS" || stage == "approved" || stage == "completed" -> "Finalizado"
            status?.uppercase() == "EXPIRED" || stage == "lapsed_motor" -> "Finalizado"
            status?.uppercase() == "FAIL" || stage == "rejected" -> "Finalizado"
            status == "in_progress" -> "En proceso"
            status == "pending" -> "Pendiente"
            stage == "finished" -> "Finalizado"
            else -> "Pendiente"
        }
    }

    /**
     * Mapea el estado de la sesión al enum KycStatus
     */
    private fun mapKycStatus(validation: String?, status: String?): KycStatus {
        return when {
            // Exitoso: PASS o (completed + approved)
            status?.uppercase() == "PASS" -> KycStatus.EXITOSO
            status == "completed" && validation == "approved" -> KycStatus.EXITOSO

            // Rechazado: FAIL o validation rejected
            status?.uppercase() == "FAIL" -> KycStatus.RECHAZADO
            validation == "rejected" -> KycStatus.RECHAZADO

            // Expirado: EXPIRED (uppercase)
            status?.uppercase() == "EXPIRED" -> KycStatus.EXPIRADO

            // Pendiente: todo lo demás (in_progress, pending, etc)
            else -> KycStatus.PENDIENTE
        }
    }

    /**
     * Convierte una lista de SessionListItem a lista de KycSessionItem
     */
    fun toKycSessionItemList(dtoList: List<SessionListItem>): List<KycSessionItem> {
        return dtoList.map { toKycSessionItem(it) }
    }
}
