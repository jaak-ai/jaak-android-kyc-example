package com.jaak.kyc.data.model.ocr.v4

import com.google.gson.annotations.SerializedName

data class DocumentExtractV4Response(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("requestId") val requestId: String,
    @SerializedName("status") val status: String,
    @SerializedName("content") val content: DocumentExtractV4Content,
    @SerializedName("processingTime") val processingTime: String,
    @SerializedName("state") val state: DocumentExtractV4State
)

data class DocumentExtractV4Content(
    @SerializedName("data") val data: DocumentExtractV4Data
)

data class DocumentExtractV4Data(
    @SerializedName("personal") val personal: DocumentExtractV4Personal,
    @SerializedName("document") val document: DocumentExtractV4Document
)

data class DocumentExtractV4Personal(
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("surname") val surname: String?,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("sex") val sex: String?,
    @SerializedName("dateOfBirth") val dateOfBirth: String?,
    @SerializedName("face") val face: String? // base64 image - needs to be saved as file
)

data class DocumentExtractV4Document(
    @SerializedName("type") val type: String?,
    @SerializedName("number") val number: String?,
    @SerializedName("country") val country: DocumentExtractV4Country?,
    @SerializedName("expiration") val expiration: DocumentExtractV4Expiration?
)

data class DocumentExtractV4Country(
    @SerializedName("name") val name: String?,
    @SerializedName("isoAlpha3Code") val isoAlpha3Code: String?
)

data class DocumentExtractV4Expiration(
    @SerializedName("date") val date: String?,
    @SerializedName("isPermanent") val isPermanent: Boolean?
)

data class DocumentExtractV4State(
    @SerializedName("documentCompleteSides") val documentCompleteSides: Boolean?,
    @SerializedName("documentExpired") val documentExpired: Boolean?,
    @SerializedName("ageValid") val ageValid: Boolean?
)
