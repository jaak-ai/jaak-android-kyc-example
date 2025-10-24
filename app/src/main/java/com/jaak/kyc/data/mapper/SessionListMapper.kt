package com.jaak.kyc.data.mapper

import com.jaak.kyc.data.model.KycSessionItem
import com.jaak.kyc.data.model.KycStatus
import com.jaak.kyc.data.model.api.SessionListItem
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Mapper para convertir SessionListItem (API) a KycSessionItem (UI)
 */
object SessionListMapper {

    private val apiDateFormatWithoutMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val apiDateFormatWithMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun mapToKycSessionItem(item: SessionListItem): KycSessionItem {
        android.util.Log.d("SessionListMapper", "========== MAPPING SESSION ==========")
        android.util.Log.d("SessionListMapper", "SessionID: ${item.sessionID}")
        android.util.Log.d("SessionListMapper", "ShortKey: ${item.shortKey}")
        android.util.Log.d("SessionListMapper", "CreatedAt RAW: ${item.createdAt}")
        android.util.Log.d("SessionListMapper", "Status: ${item.status}")
        android.util.Log.d("SessionListMapper", "StatusDetail.stage: ${item.statusDetail?.stage}")
        android.util.Log.d("SessionListMapper", "StatusDetail.state: ${item.statusDetail?.state}")
        android.util.Log.d("SessionListMapper", "Score: ${item.score}")

        val formattedDate = formatDate(item.createdAt)
        val mappedStatus = mapKycStatus(item.status, item.statusDetail?.stage, item.statusDetail?.state)

        android.util.Log.d("SessionListMapper", "FormattedDate: $formattedDate")
        android.util.Log.d("SessionListMapper", "MappedStatus: $mappedStatus")
        android.util.Log.d("SessionListMapper", "====================================")

        return KycSessionItem(
            sessionID = item.sessionID,
            shortkey = item.shortKey ?: "",
            userName = item.contactName ?: "Unknown",
            flowName = item.flowName ?: "",
            dateTime = formattedDate,
            score = (item.score ?: 0.0).toInt(),
            documentStatus = mapDocumentStatus(item.status, item.statusDetail?.stage),
            processStatus = mapProcessStatus(item.status, item.statusDetail?.stage, item.statusDetail?.state),
            kycStatus = mappedStatus
        )
    }

    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            // Intentar primero sin milisegundos (formato más común)
            val date = apiDateFormatWithoutMs.parse(dateString)
            date?.let { displayDateFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            // Si falla, intentar con milisegundos
            try {
                val date = apiDateFormatWithMs.parse(dateString)
                date?.let { displayDateFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                android.util.Log.e("SessionListMapper", "Error parsing date: $dateString", e)
                dateString
            }
        }
    }

    private fun mapDocumentStatus(status: String?, stage: String?): String {
        return when {
            status?.uppercase() == "PASS" -> "Aprobado"
            status?.uppercase() == "FAIL" -> "Rechazado"
            status?.uppercase() == "EXPIRED" -> "Expirado"
            stage == "approved" -> "Aprobado"
            stage == "rejected" -> "Rechazado"
            stage == "document" -> "Documento"
            stage == "liveness" -> "Liveness"
            stage == "oto" -> "OTO"
            stage == "finished" -> "Completado"
            else -> "Pendiente"
        }
    }

    private fun mapProcessStatus(status: String?, stage: String?, state: String?): String {
        return when {
            status?.uppercase() == "PASS" || stage == "approved" -> "Finalizado"
            status?.uppercase() == "EXPIRED" || stage == "lapsed_motor" -> "Finalizado"
            status?.uppercase() == "FAIL" || stage == "rejected" -> "Finalizado"
            state == "finished" || stage == "finished" -> "Finalizado"
            status == "in_progress" || state == "in_process" -> "En proceso"
            else -> "Pendiente"
        }
    }

    private fun mapKycStatus(status: String?, stage: String?, state: String?): KycStatus {
        return when {
            // Exitoso: PASS o approved
            status?.uppercase() == "PASS" || stage == "approved" -> KycStatus.EXITOSO

            // Rechazado: FAIL o rejected
            status?.uppercase() == "FAIL" || stage == "rejected" -> KycStatus.RECHAZADO

            // Expirado: EXPIRED
            status?.uppercase() == "EXPIRED" || stage == "lapsed_motor" -> KycStatus.EXPIRADO

            // Pendiente: todo lo demás
            else -> KycStatus.PENDIENTE
        }
    }
}
