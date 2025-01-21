package com.jaak.kyc.data.model.session

import com.google.gson.annotations.SerializedName

data class SessionAssets(
    @SerializedName("document") val document: String?, // Puede ajustarse según el tipo si se define más tarde
    @SerializedName("liveness") val liveness: String? // Puede ajustarse según el tipo si se define más tarde
)