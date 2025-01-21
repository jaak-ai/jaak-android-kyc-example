package com.jaak.kyc.data.model

import com.google.gson.annotations.SerializedName

data class ErrorModel(
    @SerializedName("message") val message: String,
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: Int
)