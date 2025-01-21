package com.jaak.kyc.data.model.otoverify

import com.google.gson.annotations.SerializedName

data class OtoVerifyImageDetails(
    @SerializedName("accessories") val accessories: OtoVerifyAccessories,
    @SerializedName("image_quality") val imageQuality: OtoVerifyImageQuality

)