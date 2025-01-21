package com.jaak.kyc.data.model.otoverify

import com.google.gson.annotations.SerializedName

data class OtoVerifyAccessories(
    @SerializedName("glass") val glass: Boolean,
    @SerializedName("sunGlass") val sunGlass: Boolean,
    @SerializedName("hat") val hat: Boolean,
    @SerializedName("mask") val mask: Boolean
)