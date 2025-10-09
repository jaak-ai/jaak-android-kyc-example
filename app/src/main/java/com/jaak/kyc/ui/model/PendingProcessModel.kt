package com.jaak.kyc.ui.model

data class PendingProcessModel(
    val processId: String,
    val shortKey: String,
    val currentService: Int, // 1-7
    val serviceName: String, // Nombre del servicio actual
    val totalServices: Int = 7,
    val isSyncing: Boolean = false
) {
    val progressText: String
        get() = "$currentService/$totalServices"
    
    val serviceDisplayText: String
        get() = "Servicio $currentService: $serviceName"
}