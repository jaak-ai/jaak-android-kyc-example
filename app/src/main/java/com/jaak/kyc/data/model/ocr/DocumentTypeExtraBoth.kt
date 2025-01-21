package com.jaak.kyc.data.model.ocr

import com.google.gson.annotations.SerializedName

data class DocumentTypeExtraBoth(
    @SerializedName("type") val type: String,
    @SerializedName("side") val side: String,
    @SerializedName("country") val country: String,
    @SerializedName("icaoCode") val icaoCode: String
)