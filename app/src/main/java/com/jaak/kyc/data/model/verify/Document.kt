package com.jaak.kyc.data.model.verify

import com.google.gson.annotations.SerializedName

data class Document(
    @SerializedName("country") val country: String,
    @SerializedName("icaoCode") val icaoCode: String,
    @SerializedName("side") val side: String,
    @SerializedName("type") val type: String,
    @SerializedName("evaluation") val evaluation: String
)