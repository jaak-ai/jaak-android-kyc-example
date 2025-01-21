package com.jaak.kyc.data.model.livenessverify

import com.google.gson.annotations.SerializedName

data class LivenessVerifyRequest(
    @SerializedName("video") val video: String, //base64
)