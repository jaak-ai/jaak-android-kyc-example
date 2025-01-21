package com.jaak.kyc.data.model.livenessverify

import com.google.gson.annotations.SerializedName

data class LivenessState(
    @SerializedName("isRealPerson") val isRealPerson: Boolean,
    @SerializedName("message") val message: String
)