package com.jaak.kyc.data.model.ocr

import com.google.gson.annotations.SerializedName

data class DocumentDataExtraBoth(
    @SerializedName("generalData") val generalData: GeneralDataExtraBoth,
    @SerializedName("mechanicalReadingZone") val mechanicalReadingZone: String,
    @SerializedName("specificData") val specificData: List<SpecificDataExtraBoth>
)
