package com.jaak.kyc.data.model.livenessverify

import com.google.gson.annotations.SerializedName

data class LivenessVerifyResponse(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("requestId") val requestId: String,
    @SerializedName("processTime") val processTime: Long,
    @SerializedName("score") val score: Double,
    @SerializedName("bestFrame") val bestFrame: String
)