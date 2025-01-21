package com.jaak.kyc.data.model.otoverify

import com.google.gson.annotations.SerializedName

data class OtoVerifyRequest(
    @SerializedName("image1") val image1: String,
    @SerializedName("image2") val image2: String
)