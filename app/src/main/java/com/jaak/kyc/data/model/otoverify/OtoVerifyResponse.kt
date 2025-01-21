package com.jaak.kyc.data.model.otoverify

import com.google.gson.annotations.SerializedName

data class OtoVerifyResponse(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("requestId") val requestId: String,
    @SerializedName("score") val score: Double,
    @SerializedName("distance") val distance: Double,
    @SerializedName("process_time") val processTime: Int,
    @SerializedName("metadata") val metadata: OtoVerifyMetadata,
    @SerializedName("codes") val codes: Double?, // Puede ser lista u objeto desconocido, usar Any? por seguridad
    @SerializedName("state") val state: OtoVerifyState
)