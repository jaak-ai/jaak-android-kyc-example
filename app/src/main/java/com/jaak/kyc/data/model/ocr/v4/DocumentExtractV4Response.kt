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
    @SerializedName("address") val address: DocumentExtractV4Address?,
    @SerializedName("document") val document: DocumentExtractV4Document
)

data class DocumentExtractV4Personal(
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("secondName") val secondName: String?,
    @SerializedName("surname") val surname: String?,
    @SerializedName("motherSurname") val motherSurname: String?,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("sex") val sex: String?,
    @SerializedName("dateOfBirth") val dateOfBirth: String?,
    @SerializedName("face") val face: String?, // base64 image - needs to be saved as file
    @SerializedName("placeOfBirth") val placeOfBirth: String?,
    @SerializedName("nationality") val nationality: String?,
    @SerializedName("maritalStatus") val maritalStatus: String?,
    @SerializedName("extra") val extra: DocumentExtractV4PersonalExtra?
)

data class DocumentExtractV4PersonalExtra(
    @SerializedName("ocr") val ocr: String?,
    @SerializedName("registerYear") val registerYear: String?,
    @SerializedName("rfc") val rfc: String?
)

data class DocumentExtractV4Document(
    @SerializedName("type") val type: String?,
    @SerializedName("side") val side: String?,
    @SerializedName("number") val number: String?,
    @SerializedName("personalIdNumber") val personalIdNumber: String?, // CURP
    @SerializedName("additionalNumber") val additionalNumber: String?, // CIC for INE
    @SerializedName("dateOfIssue") val dateOfIssue: String?,
    @SerializedName("country") val country: DocumentExtractV4Country?,
    @SerializedName("expiration") val expiration: DocumentExtractV4Expiration?,
    @SerializedName("issuingAuthority") val issuingAuthority: String?
)

data class DocumentExtractV4Address(
    @SerializedName("fullAddress") val fullAddress: String?,
    @SerializedName("postalCode") val postalCode: String?,
    @SerializedName("extra") val extra: DocumentExtractV4AddressExtra?
)

data class DocumentExtractV4AddressExtra(
    @SerializedName("street") val street: String?,
    @SerializedName("externalNumber") val externalNumber: String?,
    @SerializedName("internalNumber") val internalNumber: String?,
    @SerializedName("neighborhood") val neighborhood: String?,
    @SerializedName("apartmentUnit") val apartmentUnit: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?
)

data class DocumentExtractV4Country(
    @SerializedName("name") val name: String?,
    @SerializedName("isoAlpha2Code") val isoAlpha2Code: String?,
    @SerializedName("isoAlpha3Code") val isoAlpha3Code: String?,
    @SerializedName("icaoCode") val icaoCode: String?
)

data class DocumentExtractV4Expiration(
    @SerializedName("date") val date: String?,
    @SerializedName("isPermanent") val isPermanent: Boolean?
)

data class DocumentExtractV4State(
    @SerializedName("documentCompleteSides") val documentCompleteSides: Boolean?,
    @SerializedName("documentExpired") val documentExpired: Boolean?,
    @SerializedName("ageValid") val ageValid: Boolean?,
    @SerializedName("ageValidMessage") val ageValidMessage: String?,
    @SerializedName("message") val message: String?
)
