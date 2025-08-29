package com.jaak.kyc.data.model.ocr

import com.google.gson.annotations.SerializedName

data class DocumentExtraBothRequest(
    @SerializedName("imageFront") val documentFront: String, //base64
    @SerializedName("imageBack") val documentBack: String //base64
)