package ai.jaak.kyc.data.model.blacklist

import com.google.gson.annotations.SerializedName

data class BlacklistResponse(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("responseId") val responseId: String,
    @SerializedName("processTime") val processTime: Double,
    @SerializedName("organization") val organization: String,
    @SerializedName("service") val service: String,
    @SerializedName("result") val result: Any? = null,
    @SerializedName("state") val state: BlacklistState
)

data class BlacklistState(
    @SerializedName("message") val message: String,
    @SerializedName("foundInService") val foundInService: Boolean,
    @SerializedName("mustBeFound") val mustBeFound: Boolean
)