package com.jaak.kyc.data.model.ocr

import com.google.gson.annotations.SerializedName

data class GeneralDataExtraBoth(
    @SerializedName("name") val name: String,
    @SerializedName("birthdate") val birthdate: String, // yyyy-mm-dd
    @SerializedName("gender") val gender: String,
    @SerializedName("nationality") val nationality: String,
    @SerializedName("documentNumber") val documentNumber: String
)