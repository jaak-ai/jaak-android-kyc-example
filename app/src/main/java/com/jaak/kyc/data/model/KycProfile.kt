package com.jaak.kyc.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de perfil KYC para inicio rápido
 * Almacena la configuración predefinida para crear sesiones KYC con un solo toque
 */
data class KycProfile(
    @SerializedName("id")
    val id: String = java.util.UUID.randomUUID().toString(),

    @SerializedName("profileName")
    val profileName: String, // Nombre del perfil (para identificarlo en la lista)

    @SerializedName("contactName")
    val contactName: String, // Nombre de la persona a autenticar (name en el request)

    @SerializedName("flowName")
    val flowName: String, // Nombre del flujo (flow en el request)

    @SerializedName("redirectUrl")
    val redirectUrl: String? = null, // URL de redirección (opcional)

    @SerializedName("countryDocument")
    val countryDocument: String, // País del documento (MEX, COL, etc.)

    @SerializedName("flowType")
    val flowType: String = "KYC", // Siempre "KYC"

    @SerializedName("verificationType")
    val verificationType: String = "", // WHATSAPP, SMS, EMAIL o vacío

    // Verification object
    @SerializedName("email")
    val email: String? = null,

    @SerializedName("sms")
    val sms: String? = null,

    @SerializedName("whatsapp")
    val whatsapp: String? = null,

    @SerializedName("isDefault")
    val isDefault: Boolean = false,

    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)
