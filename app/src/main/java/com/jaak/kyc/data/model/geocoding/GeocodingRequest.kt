package com.jaak.kyc.data.model.geocoding

import com.google.gson.annotations.SerializedName

data class GeocodingRequest(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double
)