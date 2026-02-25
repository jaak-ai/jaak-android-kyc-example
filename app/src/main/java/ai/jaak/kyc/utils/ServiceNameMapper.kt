package ai.jaak.kyc.utils

import ai.jaak.kyc.data.local.entity.ServiceStatus

object ServiceNameMapper {
    
    fun getServiceName(serviceNumber: Int): String {
        return when (serviceNumber) {
            1 -> "Session"
            2 -> "Geocoding"
            3 -> "Verify"
            4 -> "OCR"
            5 -> "Liveness"
            6 -> "Face Comparison"
            7 -> "Blacklist INE"
            8 -> "Blacklist INTERPOL"
            9 -> "Blacklist OFAC"
            10 -> "Blacklist RENAPO"
            11 -> "Blacklist SAT"
            12 -> "Finish"
            else -> "Desconocido"
        }
    }
    
    fun getCurrentServiceNumber(
        sessionStatus: ServiceStatus?,
        geocodingStatus: ServiceStatus?,
        verifyStatus: ServiceStatus?,
        ocrStatus: ServiceStatus?,
        livenessStatus: ServiceStatus?,
        otoVerifyStatus: ServiceStatus?,
        blacklistIneStatus: ServiceStatus?,
        blacklistInterpolStatus: ServiceStatus?,
        blacklistOfacStatus: ServiceStatus?,
        blacklistRenapoStatus: ServiceStatus?,
        blacklistSatStatus: ServiceStatus?,
        finishStatus: ServiceStatus?
    ): Int {
        // Un servicio está "realmente completado" solo si está SYNCED
        // COMPLETED significa que se ejecutó offline y necesita sync
        
        // Si Session no está sincronizado, empezar por ahí
        if (sessionStatus != ServiceStatus.SYNCED) {
            return 1
        }
        // Si Session está sincronizado pero Geocoding no, continuar con Geocoding
        if (geocodingStatus != ServiceStatus.SYNCED) {
            return 2
        }
        // Si Geocoding está sincronizado pero Verify no, continuar con Verify
        if (verifyStatus != ServiceStatus.SYNCED) {
            return 3
        }
        // Si Verify está sincronizado pero OCR no, continuar con OCR
        if (ocrStatus != ServiceStatus.SYNCED) {
            return 4
        }
        // Si OCR está sincronizado pero Liveness no, continuar con Liveness
        if (livenessStatus != ServiceStatus.SYNCED) {
            return 5
        }
        // Si Liveness está sincronizado pero OtoVerify no, continuar con OtoVerify
        if (otoVerifyStatus != ServiceStatus.SYNCED) {
            return 6
        }
        // Si OtoVerify está sincronizado pero Blacklist INE no, continuar con Blacklist INE
        if (blacklistIneStatus != ServiceStatus.SYNCED) {
            return 7
        }
        // Si Blacklist INE está sincronizado pero Blacklist INTERPOL no, continuar con Blacklist INTERPOL
        if (blacklistInterpolStatus != ServiceStatus.SYNCED) {
            return 8
        }
        // Si Blacklist INTERPOL está sincronizado pero Blacklist OFAC no, continuar con Blacklist OFAC
        if (blacklistOfacStatus != ServiceStatus.SYNCED) {
            return 9
        }
        // Si Blacklist OFAC está sincronizado pero Blacklist RENAPO no, continuar con Blacklist RENAPO
        if (blacklistRenapoStatus != ServiceStatus.SYNCED) {
            return 10
        }
        // Si Blacklist RENAPO está sincronizado pero Blacklist SAT no, continuar con Blacklist SAT
        if (blacklistSatStatus != ServiceStatus.SYNCED) {
            return 11
        }
        // Si Blacklist SAT está sincronizado pero Finish no, continuar con Finish
        if (finishStatus != ServiceStatus.SYNCED) {
            return 12
        }
        // Todo está completado y sincronizado
        return 12
    }
}