package com.jaak.kyc.data.model.ocr

import com.google.gson.annotations.SerializedName

data class DocumentStateExtraBoth(
    @SerializedName("message") val message: String,
    @SerializedName("isExpired") val isExpired: Boolean,
    @SerializedName("isUnderAge") val isUnderAge: Boolean,
    @SerializedName("supportedDocument") val supportedDocument: Boolean
)