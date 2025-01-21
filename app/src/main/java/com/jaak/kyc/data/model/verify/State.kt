package com.jaak.kyc.data.model.verify

import com.google.gson.annotations.SerializedName


data class State(
    @SerializedName("dataConsistent") val dataConsistent: Boolean,
    @SerializedName("documentCompleteSides") val documentCompleteSides: Boolean,
    @SerializedName("documentLiveness") val documentLiveness: Boolean,
    @SerializedName("documentValidity") val documentValidity: Boolean,
    @SerializedName("handPresence") val handPresence: Boolean,
    @SerializedName("imageQuality") val imageQuality: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("securityFeatures") val securityFeatures: Boolean
)