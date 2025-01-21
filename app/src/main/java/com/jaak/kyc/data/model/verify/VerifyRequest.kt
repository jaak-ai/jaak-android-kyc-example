package com.jaak.kyc.data.model.verify

import com.google.gson.annotations.SerializedName

data class VerifyRequest(
    @SerializedName("imageFront") val document: String, //base64
    @SerializedName("imageBack") val document2: String?, //base64
    @SerializedName("documentType") val documentType: Boolean
)