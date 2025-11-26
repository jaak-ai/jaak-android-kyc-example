package com.jaak.kyc.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modelo para representar un módulo de Mosaic KYC
 */
@Parcelize
data class MosaicModule(
    val id: String,                    // ID del módulo (ej: "DOCUMENT_EXTRACT")
    val name: String,                  // Nombre amigable
    val description: String,           // Descripción del módulo
    val urlParam: String,              // Parámetro para URL (ej: "DOCUMENT_EXTRACT")
    var isEnabled: Boolean = false,    // Si está activado
    var order: Int = 0,                // Orden en la secuencia
    var isAvailable: Boolean = true    // Si está disponible (validación roja)
) : Parcelable {
    
    companion object {
        /**
         * Módulos disponibles en Mosaic según iOS
         */
        fun getDefaultModules(): List<MosaicModule> = listOf(
            MosaicModule(
                id = "DOCUMENT_EXTRACT",
                name = "Extracción de documento",
                description = "Captura y extracción de datos del documento de identidad",
                urlParam = "DOCUMENT_EXTRACT",
                isEnabled = false,
                order = 0,
                isAvailable = true
            ),
            MosaicModule(
                id = "DOCUMENT_VERIFY",
                name = "Verificación de documento",
                description = "Validación y verificación de autenticidad del documento",
                urlParam = "DOCUMENT_VERIFY",
                isEnabled = false,
                order = 1,
                isAvailable = true
            ),
            MosaicModule(
                id = "BLACKLIST",
                name = "Listas oficiales",
                description = "Verificación contra listas oficiales",
                urlParam = "BLACKLIST",
                isEnabled = false,
                order = 2,
                isAvailable = true
            ),
            MosaicModule(
                id = "OTO",
                name = "Verificación de identidad",
                description = "Prueba de vida y verificación biométrica",
                urlParam = "OTO",
                isEnabled = false,
                order = 3,
                isAvailable = true
            ),
            MosaicModule(
                id = "LOCATION_PERMISSIONS",
                name = "Permisos de ubicación",
                description = "Solicitud de permisos de geolocalización",
                urlParam = "LOCATION_PERMISSIONS",
                isEnabled = false,
                order = 4,
                isAvailable = true
            ),
            MosaicModule(
                id = "IVERIFICATION",
                name = "Comparación 1:1",
                description = "Comparación facial uno a uno",
                urlParam = "IVERIFICATION",
                isEnabled = false,
                order = 5,
                isAvailable = true
            )
        )
    }
}

/**
 * Configuración completa de Mosaic
 */
@Parcelize
data class MosaicConfig(
    val modules: List<MosaicModule>,
    val shortKey: String
) : Parcelable {
    
    /**
     * Construye la URL de Mosaic con los módulos seleccionados
     * IMPORTANTE: Respeta el ORDEN DE LA LISTA (posición física), no el orden de activación
     */
    fun buildMosaicUrl(): String {
        // Filtrar módulos habilitados manteniendo el orden de la lista (order es el índice)
        val enabledModulesInOrder = modules
            .filter { it.isEnabled }
            .sortedBy { it.order }  // 'order' representa la posición en el listado
            .map { it.urlParam }
        
        // Siempre agregar WELCOME al inicio y FINISH al final
        val finalModules = mutableListOf("WELCOME")
        finalModules.addAll(enabledModulesInOrder)
        finalModules.add("FINISH")
        
        val stepsParam = finalModules.joinToString("%2C")
        
        return "https://mosaic.dev.jaak.ai/link?shortKey=$shortKey&steps=$stepsParam"
    }
    
    /**
     * Valida si la configuración es válida
     */
    fun isValid(): Boolean {
        val enabledModules = modules.filter { it.isEnabled }
        
        // Al menos debe tener un módulo habilitado
        if (enabledModules.isEmpty()) {
            return false
        }
        
        // Todos los módulos habilitados deben estar disponibles
        val allAvailable = enabledModules.all { it.isAvailable }
        
        return allAvailable
    }
    
    /**
     * Obtiene módulos que están habilitados pero no disponibles (para marcar en rojo)
     */
    fun getUnavailableEnabledModules(): List<MosaicModule> {
        return modules.filter { it.isEnabled && !it.isAvailable }
    }
}
