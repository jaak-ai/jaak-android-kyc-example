package ai.jaak.kyc.data.model.geocoding

import com.google.gson.annotations.SerializedName

data class GeocodingResponse(
    @SerializedName("eventId")
    val eventId: String,
    
    @SerializedName("requestId")
    val requestId: String,
    
    @SerializedName("address")
    val address: Address
)

data class Address(
    @SerializedName("country")
    val country: String?,
    
    @SerializedName("state")
    val state: String?,
    
    @SerializedName("locality")
    val locality: String?,
    
    @SerializedName("sub_locality")
    val subLocality: String?,
    
    @SerializedName("postal_code")
    val postalCode: String?,
    
    @SerializedName("route")
    val route: String?,
    
    @SerializedName("street_number")
    val streetNumber: String?
)