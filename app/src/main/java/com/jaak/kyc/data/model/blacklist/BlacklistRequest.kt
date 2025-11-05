package com.jaak.kyc.data.model.blacklist

import com.google.gson.annotations.SerializedName

data class BlacklistRequest(
    @SerializedName("services") val services: BlacklistServices,
    @SerializedName("payload") val payload: BlacklistPayload
)

data class BlacklistServices(
    @SerializedName("ine") val ine: Boolean = false,
    @SerializedName("interpol") val interpol: Boolean = false,
    @SerializedName("ofac") val ofac: Boolean = false,
    @SerializedName("renapo") val renapo: BlacklistRenapoService? = null,
    @SerializedName("sat") val sat: BlacklistSatService? = null,
    @SerializedName("cdc") val cdc: BlacklistCdcService? = null
)

data class BlacklistRenapoService(
    @SerializedName("curp") val curp: Boolean = false
)

data class BlacklistSatService(
    @SerializedName("sat69b") val sat69b: Boolean = false
)

data class BlacklistCdcService(
    @SerializedName("rccFico") val rccFico: Boolean = false
)

data class BlacklistPayload(
    @SerializedName("person") val person: BlacklistPerson? = null,
    @SerializedName("address") val address: BlacklistAddress? = null,
    @SerializedName("identifications") val identifications: BlacklistIdentifications? = null,
    @SerializedName("extras") val extras: BlacklistExtras? = null
)

data class BlacklistPerson(
    @SerializedName("name") val name: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("secondName") val secondName: String = "",
    @SerializedName("secondLastName") val secondLastName: String = "",
    @SerializedName("birthDate") val birthDate: String = "",
    @SerializedName("nationality") val nationality: String = ""
)

data class BlacklistAddress(
    @SerializedName("address") val address: String = "",
    @SerializedName("number") val number: Int = 0,
    @SerializedName("neighborhood") val neighborhood: String = "",
    @SerializedName("municipality") val municipality: String = "",
    @SerializedName("city") val city: String = "",
    @SerializedName("state") val state: String = "",
    @SerializedName("postalCode") val postalCode: String = ""
)

data class BlacklistIdentifications(
    @SerializedName("curp") val curp: String? = null,
    @SerializedName("rfc") val rfc: String? = null,
    @SerializedName("socialSecurityNumber") val socialSecurityNumber: String? = null,
    @SerializedName("electorKey") val electorKey: String? = null,
    @SerializedName("ine") val ine: BlacklistIne? = null
)

data class BlacklistIne(
    @SerializedName("cic") val cic: String,
    @SerializedName("ocr") val ocr: String
)

data class BlacklistExtras(
    @SerializedName("commonId") val commonId: String = "",
    @SerializedName("wantedIn") val wantedIn: String = ""
)