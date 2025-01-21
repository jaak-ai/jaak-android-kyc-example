package com.jaak.kyc.data.model.session

import com.google.gson.annotations.SerializedName

data class SessionResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("step") val step: Int,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("assets") val assets: SessionAssets,
    @SerializedName("document") val document: String
)