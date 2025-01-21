package com.jaak.kyc.data.model.otoverify

import com.google.gson.annotations.SerializedName

data class OtoVerifyMetadata(
    @SerializedName("image1") val image1: OtoVerifyImageDetails,
    @SerializedName("image2") val image2: OtoVerifyImageDetails
)