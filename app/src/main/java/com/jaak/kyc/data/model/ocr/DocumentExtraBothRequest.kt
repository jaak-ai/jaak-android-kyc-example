package com.jaak.kyc.data.model.ocr

import com.google.gson.annotations.SerializedName

data class DocumentExtraBothRequest(
    @SerializedName("documentFront") val documentFront: String, //base64
    @SerializedName("documentBack") val documentBack: String //base64
)