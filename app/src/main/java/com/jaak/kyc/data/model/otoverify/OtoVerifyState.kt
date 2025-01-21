package com.jaak.kyc.data.model.otoverify

import com.google.gson.annotations.SerializedName

data class OtoVerifyState(
    @SerializedName("rejectedBadQuality") val rejectedBadQuality: Boolean,
    @SerializedName("rejectedAccessories") val rejectedAccessories: Boolean,
    @SerializedName("isSamePerson") val isSamePerson: Boolean,
    @SerializedName("message") val message: String
)