package ai.jaak.kyc.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import ai.jaak.kyc.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Tests de casos edge y rendimiento para validar comportamiento límite
 * del sistema offline KYC bajo condiciones extremas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EdgeCasesAndPerformanceTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `large number of processes performance test`() = runTest {
        // SCENARIO: Sistema maneja gran cantidad de procesos sin degradación
        
        val processCount = 1000
        val processes = mutableListOf<KycProcessEntity>()
        
        // Measure time to create many processes
        val creationTime = measureTimeMillis {
            repeat(processCount) { index ->
                processes.add(
                    KycProcessEntity(
                        shortKey = "PROC-${index.toString().padStart(4, '0')}",
                        overallStatus = when (index % 5) {
                            0 -> KycProcessStatus.PENDING
                            1 -> KycProcessStatus.IN_PROGRESS
                            2 -> KycProcessStatus.COMPLETED_OFFLINE
                            3 -> KycProcessStatus.COMPLETED
                            else -> KycProcessStatus.FAILED
                        },
                        sessionStatus = if (index % 3 == 0) ServiceStatus.COMPLETED else ServiceStatus.PENDING
                    )
                )
            }
        }
        
        // Verify all processes created
        assertThat(processes).hasSize(processCount)
        assertThat(creationTime).isLessThan(5000L) // Should create 1000 processes in under 5 seconds
        
        // Test filtering performance
        val filterTime = measureTimeMillis {
            val pendingProcesses = processes.filter { it.overallStatus == KycProcessStatus.PENDING }
            val completedOffline = processes.filter { it.overallStatus == KycProcessStatus.COMPLETED_OFFLINE }
            val failed = processes.filter { it.overallStatus == KycProcessStatus.FAILED }
            
            assertThat(pendingProcesses.size + completedOffline.size + failed.size).isGreaterThan(0)
        }
        
        assertThat(filterTime).isLessThan(100L) // Filtering should be fast
    }

    @Test
    fun `maximum retry attempts edge case`() = runTest {
        // SCENARIO: Proceso alcanza máximo de reintentos para todos los servicios
        
        val maxRetries = 999 // Edge case: very high retry count
        val processId = "max-retries-001"
        
        val heavilyRetriedProcess = KycProcessEntity(
            id = processId,
            shortKey = "PROC-MAX",
            overallStatus = KycProcessStatus.FAILED,
            
            // All services failed with maximum retries
            sessionStatus = ServiceStatus.FAILED,
            sessionRetryCount = maxRetries,
            sessionError = "Maximum retries reached for session",
            
            verifyStatus = ServiceStatus.FAILED,
            verifyRetryCount = maxRetries,
            verifyError = "Maximum retries reached for verification",
            
            ocrStatus = ServiceStatus.FAILED,
            ocrRetryCount = maxRetries,
            ocrError = "Maximum retries reached for OCR",
            
            livenessStatus = ServiceStatus.FAILED,
            livenessRetryCount = maxRetries,
            livenessError = "Maximum retries reached for liveness",
            
            otoVerifyStatus = ServiceStatus.FAILED,
            otoVerifyRetryCount = maxRetries,
            otoVerifyError = "Maximum retries reached for face match",
            
            finishStatus = ServiceStatus.FAILED,
            finishRetryCount = maxRetries,
            finishError = "Maximum retries reached for finish"
        )
        
        // Verify all retry counts are at maximum
        assertThat(heavilyRetriedProcess.sessionRetryCount).isEqualTo(maxRetries)
        assertThat(heavilyRetriedProcess.verifyRetryCount).isEqualTo(maxRetries)
        assertThat(heavilyRetriedProcess.ocrRetryCount).isEqualTo(maxRetries)
        assertThat(heavilyRetriedProcess.livenessRetryCount).isEqualTo(maxRetries)
        assertThat(heavilyRetriedProcess.otoVerifyRetryCount).isEqualTo(maxRetries)
        assertThat(heavilyRetriedProcess.finishRetryCount).isEqualTo(maxRetries)
        
        // Verify all services are marked as failed
        assertThat(heavilyRetriedProcess.sessionStatus).isEqualTo(ServiceStatus.FAILED)
        assertThat(heavilyRetriedProcess.verifyStatus).isEqualTo(ServiceStatus.FAILED)
        assertThat(heavilyRetriedProcess.ocrStatus).isEqualTo(ServiceStatus.FAILED)
        assertThat(heavilyRetriedProcess.livenessStatus).isEqualTo(ServiceStatus.FAILED)
        assertThat(heavilyRetriedProcess.otoVerifyStatus).isEqualTo(ServiceStatus.FAILED)
        assertThat(heavilyRetriedProcess.finishStatus).isEqualTo(ServiceStatus.FAILED)
        
        // Verify overall status is FAILED
        assertThat(heavilyRetriedProcess.overallStatus).isEqualTo(KycProcessStatus.FAILED)
    }

    @Test
    fun `very long error messages edge case`() = runTest {
        // SCENARIO: Sistema maneja mensajes de error extremadamente largos
        
        val veryLongErrorMessage = "A".repeat(10000) // 10KB error message
        val processId = "long-errors-002"
        
        val processWithLongErrors = KycProcessEntity(
            id = processId,
            shortKey = "PROC-LONG",
            sessionError = veryLongErrorMessage,
            verifyError = veryLongErrorMessage,
            ocrError = veryLongErrorMessage,
            livenessError = veryLongErrorMessage,
            otoVerifyError = veryLongErrorMessage,
            finishError = veryLongErrorMessage
        )
        
        // Verify long error messages are preserved
        assertThat(processWithLongErrors.sessionError).hasLength(10000)
        assertThat(processWithLongErrors.verifyError).hasLength(10000)
        assertThat(processWithLongErrors.ocrError).hasLength(10000)
        assertThat(processWithLongErrors.livenessError).hasLength(10000)
        assertThat(processWithLongErrors.otoVerifyError).hasLength(10000)
        assertThat(processWithLongErrors.finishError).hasLength(10000)
        
        // Test memory impact of long strings
        val processes = mutableListOf<KycProcessEntity>()
        val memoryTime = measureTimeMillis {
            repeat(100) { index ->
                processes.add(
                    processWithLongErrors.copy(
                        id = "long-errors-${index.toString().padStart(3, '0')}",
                        shortKey = "PROC-LONG-${index}"
                    )
                )
            }
        }
        
        assertThat(processes).hasSize(100)
        assertThat(memoryTime).isLessThan(2000L) // Should handle 100 processes with long errors quickly
    }

    @Test
    fun `extreme timestamp edge cases`() = runTest {
        // SCENARIO: Manejo de timestamps en casos extremos
        
        val veryOldTimestamp = 0L // Unix epoch
        val futureTimestamp = Long.MAX_VALUE // Far future
        val currentTime = System.currentTimeMillis()
        
        val processWithExtremeTimestamps = KycProcessEntity(
            shortKey = "PROC-TIME",
            createdAt = veryOldTimestamp,
            updatedAt = futureTimestamp,
            lastSyncAttempt = currentTime
        )
        
        // Verify extreme timestamps are handled
        assertThat(processWithExtremeTimestamps.createdAt).isEqualTo(veryOldTimestamp)
        assertThat(processWithExtremeTimestamps.updatedAt).isEqualTo(futureTimestamp)
        assertThat(processWithExtremeTimestamps.lastSyncAttempt).isEqualTo(currentTime)
        
        // Test sorting with extreme timestamps
        val processes = listOf(
            processWithExtremeTimestamps,
            KycProcessEntity(shortKey = "PROC-NORMAL", createdAt = currentTime),
            KycProcessEntity(shortKey = "PROC-OLD", createdAt = 1000000L)
        )
        
        val sortedByCreation = processes.sortedBy { it.createdAt }
        assertThat(sortedByCreation[0].createdAt).isEqualTo(veryOldTimestamp)
        assertThat(sortedByCreation[2].createdAt).isEqualTo(currentTime)
    }

    @Test
    fun `null and empty string edge cases`() = runTest {
        // SCENARIO: Manejo correcto de valores nulos y strings vacíos
        
        val processWithNulls = KycProcessEntity(
            shortKey = "", // Empty string
            sessionId = null,
            accessToken = null,
            sessionError = null,
            verifyError = "",
            ocrError = "   ", // Whitespace only
            livenessError = null,
            otoVerifyError = "",
            finishError = null,
            lastSyncAttempt = null
        )
        
        // Verify null and empty values are handled correctly
        assertThat(processWithNulls.shortKey).isEmpty()
        assertThat(processWithNulls.sessionId).isNull()
        assertThat(processWithNulls.accessToken).isNull()
        assertThat(processWithNulls.sessionError).isNull()
        assertThat(processWithNulls.verifyError).isEmpty()
        assertThat(processWithNulls.ocrError).isEqualTo("   ")
        assertThat(processWithNulls.livenessError).isNull()
        assertThat(processWithNulls.otoVerifyError).isEmpty()
        assertThat(processWithNulls.finishError).isNull()
        assertThat(processWithNulls.lastSyncAttempt).isNull()
        
        // Verify process still functions with null/empty values
        assertThat(processWithNulls.id).isNotEmpty() // ID should still be generated
        assertThat(processWithNulls.overallStatus).isEqualTo(KycProcessStatus.PENDING)
        assertThat(processWithNulls.createdAt).isGreaterThan(0L)
    }

    @Test
    fun `concurrent process state modifications`() = runTest {
        // SCENARIO: Múltiples modificaciones simultáneas al mismo proceso
        
        val baseProcess = KycProcessEntity(
            shortKey = "PROC-CONCURRENT",
            overallStatus = KycProcessStatus.PENDING
        )
        
        // Simulate concurrent modifications
        val modifications = listOf(
            baseProcess.copy(sessionStatus = ServiceStatus.COMPLETED),
            baseProcess.copy(verifyStatus = ServiceStatus.COMPLETED),
            baseProcess.copy(ocrStatus = ServiceStatus.COMPLETED),
            baseProcess.copy(livenessStatus = ServiceStatus.COMPLETED),
            baseProcess.copy(otoVerifyStatus = ServiceStatus.COMPLETED)
        )
        
        // Verify each modification preserves base data
        modifications.forEach { modified ->
            assertThat(modified.shortKey).isEqualTo(baseProcess.shortKey)
            assertThat(modified.id).isEqualTo(baseProcess.id)
            assertThat(modified.createdAt).isEqualTo(baseProcess.createdAt)
        }
        
        // Test rapid sequential updates
        var currentProcess = baseProcess
        val updateTime = measureTimeMillis {
            currentProcess = currentProcess.copy(sessionStatus = ServiceStatus.COMPLETED)
            currentProcess = currentProcess.copy(verifyStatus = ServiceStatus.COMPLETED) 
            currentProcess = currentProcess.copy(ocrStatus = ServiceStatus.COMPLETED)
            currentProcess = currentProcess.copy(livenessStatus = ServiceStatus.COMPLETED)
            currentProcess = currentProcess.copy(otoVerifyStatus = ServiceStatus.COMPLETED)
            currentProcess = currentProcess.copy(finishStatus = ServiceStatus.COMPLETED)
            currentProcess = currentProcess.copy(overallStatus = KycProcessStatus.COMPLETED)
        }
        
        assertThat(updateTime).isLessThan(50L) // Fast sequential updates
        assertThat(currentProcess.overallStatus).isEqualTo(KycProcessStatus.COMPLETED)
    }

    @Test
    fun `maximum sync attempts edge case`() = runTest {
        // SCENARIO: Proceso con intentos de sync extremadamente altos
        
        val maxSyncAttempts = Integer.MAX_VALUE
        val processId = "max-sync-003"
        
        val processWithMaxSync = KycProcessEntity(
            id = processId,
            shortKey = "PROC-SYNC-MAX",
            overallStatus = KycProcessStatus.COMPLETED_OFFLINE,
            requiresSync = true,
            syncAttempts = maxSyncAttempts,
            lastSyncAttempt = System.currentTimeMillis()
        )
        
        // Verify extreme sync attempts are handled
        assertThat(processWithMaxSync.syncAttempts).isEqualTo(maxSyncAttempts)
        assertThat(processWithMaxSync.requiresSync).isTrue()
        assertThat(processWithMaxSync.lastSyncAttempt).isNotNull()
        
        // Test process state with extreme sync attempts
        assertThat(processWithMaxSync.overallStatus).isEqualTo(KycProcessStatus.COMPLETED_OFFLINE)
        
        // Simulate successful sync after many attempts
        val finalSyncedProcess = processWithMaxSync.copy(
            overallStatus = KycProcessStatus.COMPLETED,
            requiresSync = false,
            syncAttempts = maxSyncAttempts + 1, // Increment one more time
            sessionStatus = ServiceStatus.SYNCED,
            verifyStatus = ServiceStatus.SYNCED,
            ocrStatus = ServiceStatus.SYNCED,
            livenessStatus = ServiceStatus.SYNCED,
            otoVerifyStatus = ServiceStatus.SYNCED,
            finishStatus = ServiceStatus.SYNCED
        )
        
        assertThat(finalSyncedProcess.requiresSync).isFalse()
        assertThat(finalSyncedProcess.overallStatus).isEqualTo(KycProcessStatus.COMPLETED)
    }

    @Test
    fun `unicode and special characters in process data`() = runTest {
        // SCENARIO: Manejo de caracteres especiales y unicode
        
        val unicodeShortKey = "PROC-测试-🚀-αβγ-عربي"
        val unicodeError = "Error con émojis 😵 y unicode ñáéíóú 中文 العربية"
        
        val processWithUnicode = KycProcessEntity(
            shortKey = unicodeShortKey,
            sessionError = unicodeError,
            verifyError = "Special chars: !@#$%^&*()_+-={}[]|\\:;\"'<>?,./"
        )
        
        // Verify unicode characters are preserved
        assertThat(processWithUnicode.shortKey).isEqualTo(unicodeShortKey)
        assertThat(processWithUnicode.sessionError).isEqualTo(unicodeError)
        assertThat(processWithUnicode.verifyError).contains("!@#$%^&*()")
        
        // Test unicode in all string fields
        val unicodeProcess = KycProcessEntity(
            shortKey = "PROC-UNI",
            sessionId = "session-测试-🔑",
            accessToken = "token-αβγ-🚀"
        )
        
        assertThat(unicodeProcess.sessionId).contains("测试")
        assertThat(unicodeProcess.accessToken).contains("αβγ")
    }

    @Test
    fun `process creation performance under load`() = runTest {
        // SCENARIO: Rendimiento de creación de procesos bajo carga
        
        val batchSizes = listOf(10, 100, 500, 1000)
        
        batchSizes.forEach { batchSize ->
            val creationTime = measureTimeMillis {
                val processes = mutableListOf<KycProcessEntity>()
                repeat(batchSize) { index ->
                    processes.add(
                        KycProcessEntity(
                            shortKey = "BATCH-${batchSize}-${index}",
                            sessionId = "session-${index}",
                            accessToken = "token-${index}"
                        )
                    )
                }
                
                // Verify all processes created successfully
                assertThat(processes).hasSize(batchSize)
                
                // Verify unique IDs
                val uniqueIds = processes.map { it.id }.toSet()
                assertThat(uniqueIds).hasSize(batchSize)
            }
            
            // Performance assertions - should scale reasonably
            val maxTimePerProcess = when (batchSize) {
                10 -> 10 // 10ms per process for small batches
                100 -> 5  // 5ms per process for medium batches
                500 -> 3  // 3ms per process for large batches
                1000 -> 2 // 2ms per process for very large batches
                else -> 1
            }
            
            val timePerProcess = creationTime.toDouble() / batchSize
            assertThat(timePerProcess).isLessThan(maxTimePerProcess.toDouble())
        }
    }
}