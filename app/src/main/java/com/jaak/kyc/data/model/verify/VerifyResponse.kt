package com.jaak.kyc.data.model.verify

import com.google.gson.annotations.SerializedName

data class VerifyResponse(
    @SerializedName("document") val document: Document,
    @SerializedName("documentType") val documentType: Int,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("processTime") val processTime: Long,
    @SerializedName("requestId") val requestId: String,
    @SerializedName("state") val state: State
)