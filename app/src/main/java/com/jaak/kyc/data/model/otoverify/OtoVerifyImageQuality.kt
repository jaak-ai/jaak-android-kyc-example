package com.jaak.kyc.data.model.otoverify

import com.google.gson.annotations.SerializedName

data class OtoVerifyImageQuality(
    @SerializedName("isCorrectBrightness") val isCorrectBrightness: Boolean,
    @SerializedName("isCorrectBlur") val isCorrectBlur: Boolean,
    @SerializedName("isCorrectHeight") val isCorrectHeight: Boolean,
    @SerializedName("isCorrectWidth") val isCorrectWidth: Boolean,
    @SerializedName("isCorrectResolution") val isCorrectResolution: Boolean,
    @SerializedName("isCorrectVerticalRotation") val isCorrectVerticalRotation: Boolean,
    @SerializedName("isCorrectHorizontalRotation") val isCorrectHorizontalRotation: Boolean,
    @SerializedName("isCorrectRotation") val isCorrectRotation: Boolean,
    @SerializedName("isCorrectSizeFace") val isCorrectSizeFace: Boolean,
    @SerializedName("isCorrectNumberFaces") val isCorrectNumberFaces: Boolean
)