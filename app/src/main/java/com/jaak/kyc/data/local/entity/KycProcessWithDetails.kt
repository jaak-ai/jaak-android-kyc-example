package com.jaak.kyc.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Complete KYC Process with all related entities
 * Used for queries that need full process data
 */
data class KycProcessWithDetails(
    @Embedded
    val process: KycProcessEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "processId"
    )
    val session: KycSessionEntity?,
    
    @Relation(
        parentColumn = "id", 
        entityColumn = "processId"
    )
    val verify: KycVerifyEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "processId"
    )
    val ocr: KycOcrEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "processId"
    )
    val liveness: KycLivenessEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "processId"
    )
    val otoVerify: KycOtoVerifyEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "processId"
    )
    val finish: KycFinishEntity?
) {
    /**
     * Calculate overall progress percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        val completedServices = listOf(
            process.sessionStatus,
            process.verifyStatus,
            process.ocrStatus,
            process.livenessStatus,
            process.otoVerifyStatus,
            process.finishStatus
        ).count { it == ServiceStatus.COMPLETED || it == ServiceStatus.SYNCED }
        
        return (completedServices * 100) / 6
    }
    
    /**
     * Check if process needs sync (has offline completed services)
     */
    fun needsSync(): Boolean {
        return listOf(
            process.sessionStatus,
            process.verifyStatus,
            process.ocrStatus,
            process.livenessStatus,
            process.otoVerifyStatus,
            process.finishStatus
        ).any { it == ServiceStatus.COMPLETED }
    }
    
    /**
     * Get next service that needs to be executed
     */
    fun getNextPendingService(): String? {
        return when {
            process.sessionStatus == ServiceStatus.PENDING -> "session"
            process.verifyStatus == ServiceStatus.PENDING -> "verify"
            process.ocrStatus == ServiceStatus.PENDING -> "ocr"
            process.livenessStatus == ServiceStatus.PENDING -> "liveness"
            process.otoVerifyStatus == ServiceStatus.PENDING -> "oto-verify"
            process.finishStatus == ServiceStatus.PENDING -> "finish"
            else -> null
        }
    }
    
    /**
     * Check if all services are completed (ready for sync or already synced)
     */
    fun isFullyCompleted(): Boolean {
        return listOf(
            process.sessionStatus,
            process.verifyStatus,
            process.ocrStatus,
            process.livenessStatus,
            process.otoVerifyStatus,
            process.finishStatus
        ).all { it == ServiceStatus.COMPLETED || it == ServiceStatus.SYNCED }
    }
}