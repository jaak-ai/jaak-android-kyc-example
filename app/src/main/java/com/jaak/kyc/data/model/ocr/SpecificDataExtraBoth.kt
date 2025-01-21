package com.jaak.kyc.data.model.ocr

import com.google.gson.annotations.SerializedName

data class SpecificDataExtraBoth(
    @SerializedName("field") val field: String?,
    @SerializedName("value") val value: String?
)