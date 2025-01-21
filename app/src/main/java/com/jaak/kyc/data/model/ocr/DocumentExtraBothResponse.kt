package com.jaak.kyc.data.model.ocr

import com.google.gson.annotations.SerializedName

data class DocumentExtraBothResponse(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("requestId") val requestId: String,
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("documentType") val documentType: DocumentTypeExtraBoth,
    @SerializedName("documentData") val documentData: DocumentDataExtraBoth,
    @SerializedName("documentMetadata") val documentMetadata: String,
    @SerializedName("processingTime") val processingTime: String, // milliseconds
    @SerializedName("state") val state: DocumentStateExtraBoth
)