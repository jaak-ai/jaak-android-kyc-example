package com.jaak.kyc.data.local.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SimpleKycProcessEntityTest {

    @Test
    fun `create KycProcessEntity with basic fields`() {
        // Given
        val shortKey = "PROC-123"
        
        // When
        val process = KycProcessEntity(
            shortKey = shortKey,
            overallStatus = KycProcessStatus.PENDING
        )
        
        // Then
        assertThat(process.shortKey).isEqualTo(shortKey)
        assertThat(process.overallStatus).isEqualTo(KycProcessStatus.PENDING)
        assertThat(process.id).isNotEmpty()
        assertThat(process.createdAt).isGreaterThan(0L)
        assertThat(process.updatedAt).isGreaterThan(0L)
    }

    @Test
    fun `KycProcessStatus enum has all required values`() {
        // When/Then - verifica que los valores del enum existan
        val values = KycProcessStatus.values()
        assertThat(values).isNotEmpty()
        
        // Verifica que PENDING existe (es el más importante para nuestro flujo offline)
        val pendingExists = values.any { it == KycProcessStatus.PENDING }
        assertThat(pendingExists).isTrue()
    }

    @Test
    fun `ServiceStatus enum has all required values`() {
        // When/Then - verifica que los valores del enum existan
        val values = ServiceStatus.values()
        assertThat(values).isNotEmpty()
        
        // Verifica que PENDING existe (es el más importante para nuestro flujo offline)
        val pendingExists = values.any { it == ServiceStatus.PENDING }
        assertThat(pendingExists).isTrue()
        
        // Verifica que COMPLETED existe (necesario para marcar servicios como completados)
        val completedExists = values.any { it == ServiceStatus.COMPLETED }
        assertThat(completedExists).isTrue()
    }

    @Test
    fun `process can update individual service status`() {
        // Given
        val process = KycProcessEntity(
            shortKey = "PROC-001",
            overallStatus = KycProcessStatus.PENDING
        )
        
        // When
        val updatedProcess = process.copy(
            sessionStatus = ServiceStatus.COMPLETED,
            verifyStatus = ServiceStatus.COMPLETED
        )
        
        // Then
        assertThat(updatedProcess.sessionStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(updatedProcess.verifyStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(updatedProcess.ocrStatus).isEqualTo(ServiceStatus.PENDING) // Otros permanecen sin cambio
    }

    @Test
    fun `process can be marked as completed`() {
        // Given
        val process = KycProcessEntity(
            shortKey = "PROC-001",
            overallStatus = KycProcessStatus.PENDING
        )
        
        // When
        val completedProcess = process.copy(
            overallStatus = KycProcessStatus.COMPLETED,
            sessionStatus = ServiceStatus.COMPLETED,
            verifyStatus = ServiceStatus.COMPLETED,
            ocrStatus = ServiceStatus.COMPLETED,
            livenessStatus = ServiceStatus.COMPLETED,
            otoVerifyStatus = ServiceStatus.COMPLETED,
            finishStatus = ServiceStatus.COMPLETED
        )
        
        // Then
        assertThat(completedProcess.overallStatus).isEqualTo(KycProcessStatus.COMPLETED)
        assertThat(completedProcess.sessionStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(completedProcess.verifyStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(completedProcess.ocrStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(completedProcess.livenessStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(completedProcess.otoVerifyStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(completedProcess.finishStatus).isEqualTo(ServiceStatus.COMPLETED)
    }

    @Test
    fun `process with mixed service statuses`() {
        // Given/When
        val process = KycProcessEntity(
            shortKey = "PROC-001",
            overallStatus = KycProcessStatus.PENDING,
            sessionStatus = ServiceStatus.COMPLETED,
            verifyStatus = ServiceStatus.COMPLETED,
            ocrStatus = ServiceStatus.PENDING,
            livenessStatus = ServiceStatus.FAILED,
            otoVerifyStatus = ServiceStatus.PENDING,
            finishStatus = ServiceStatus.PENDING
        )
        
        // Then
        assertThat(process.sessionStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(process.verifyStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(process.ocrStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(process.livenessStatus).isEqualTo(ServiceStatus.FAILED)
        assertThat(process.otoVerifyStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(process.finishStatus).isEqualTo(ServiceStatus.PENDING)
    }

    @Test
    fun `process can track error messages`() {
        // Given/When
        val process = KycProcessEntity(
            shortKey = "PROC-001",
            sessionError = "Network timeout",
            verifyError = "Invalid document",
            ocrError = null
        )
        
        // Then
        assertThat(process.sessionError).isEqualTo("Network timeout")
        assertThat(process.verifyError).isEqualTo("Invalid document")
        assertThat(process.ocrError).isNull()
    }

    @Test
    fun `process can track retry counts`() {
        // Given/When
        val process = KycProcessEntity(
            shortKey = "PROC-001",
            sessionRetryCount = 2,
            verifyRetryCount = 1,
            ocrRetryCount = 0
        )
        
        // Then
        assertThat(process.sessionRetryCount).isEqualTo(2)
        assertThat(process.verifyRetryCount).isEqualTo(1)
        assertThat(process.ocrRetryCount).isEqualTo(0)
    }

    @Test
    fun `process generates unique IDs`() {
        // When
        val process1 = KycProcessEntity(shortKey = "PROC-001")
        val process2 = KycProcessEntity(shortKey = "PROC-002")
        
        // Then
        assertThat(process1.id).isNotEqualTo(process2.id)
        assertThat(process1.id).isNotEmpty()
        assertThat(process2.id).isNotEmpty()
    }

    @Test
    fun `process has valid timestamps`() {
        // Given
        val beforeCreation = System.currentTimeMillis()
        
        // When
        val process = KycProcessEntity(shortKey = "PROC-001")
        
        // Then
        val afterCreation = System.currentTimeMillis()
        assertThat(process.createdAt).isAtLeast(beforeCreation)
        assertThat(process.createdAt).isAtMost(afterCreation)
        assertThat(process.updatedAt).isAtLeast(beforeCreation)
        assertThat(process.updatedAt).isAtMost(afterCreation)
    }
}