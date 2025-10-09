package com.jaak.kyc.data.model.ocr.v4

import com.google.gson.annotations.SerializedName

data class DocumentExtractV4Request(
    @SerializedName("imageFront") val imageFront: String, // base64
    @SerializedName("imageBack") val imageBack: String, // base64
    @SerializedName("allowedCountries") val allowedCountries: List<String> = listOf("MEX", "COL") // Default countries
)
