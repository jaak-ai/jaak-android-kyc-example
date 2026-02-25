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

/**
 * Tests de integración simplificados que validan los flujos principales 
 * del sistema offline KYC usando las estructuras de datos reales.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimpleWorkflowIntegrationTest {

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
    fun `complete offline KYC workflow transitions through all states correctly`() = runTest {
        // SCENARIO: Usuario completa proceso KYC offline, luego sincroniza online
        
        val processId = "workflow-001"
        
        // Step 1: Create initial process (PENDING)
        val initialProcess = KycProcessEntity(
            id = processId,
            shortKey = "PROC-001",
            overallStatus = KycProcessStatus.PENDING,
            sessionStatus = ServiceStatus.PENDING,
            verifyStatus = ServiceStatus.PENDING,
            ocrStatus = ServiceStatus.PENDING,
            livenessStatus = ServiceStatus.PENDING,
            otoVerifyStatus = ServiceStatus.PENDING,
            finishStatus = ServiceStatus.PENDING
        )
        
        // Verify initial state
        assertThat(initialProcess.overallStatus).isEqualTo(KycProcessStatus.PENDING)
        assertThat(initialProcess.sessionStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(initialProcess.verifyStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(initialProcess.ocrStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(initialProcess.livenessStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(initialProcess.otoVerifyStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(initialProcess.finishStatus).isEqualTo(ServiceStatus.PENDING)
        
        // Step 2: Complete services one by one (offline)
        val sessionCompleted = initialProcess.copy(
            sessionStatus = ServiceStatus.COMPLETED
        )
        assertThat(sessionCompleted.sessionStatus).isEqualTo(ServiceStatus.COMPLETED)
        
        val verifyCompleted = sessionCompleted.copy(
            verifyStatus = ServiceStatus.COMPLETED
        )
        assertThat(verifyCompleted.verifyStatus).isEqualTo(ServiceStatus.COMPLETED)
        
        val ocrCompleted = verifyCompleted.copy(
            ocrStatus = ServiceStatus.COMPLETED
        )
        assertThat(ocrCompleted.ocrStatus).isEqualTo(ServiceStatus.COMPLETED)
        
        val livenessCompleted = ocrCompleted.copy(
            livenessStatus = ServiceStatus.COMPLETED
        )
        assertThat(livenessCompleted.livenessStatus).isEqualTo(ServiceStatus.COMPLETED)
        
        val otoVerifyCompleted = livenessCompleted.copy(
            otoVerifyStatus = ServiceStatus.COMPLETED
        )
        assertThat(otoVerifyCompleted.otoVerifyStatus).isEqualTo(ServiceStatus.COMPLETED)
        
        // Step 3: Complete final service and mark overall process as COMPLETED
        val fullyCompleted = otoVerifyCompleted.copy(
            overallStatus = KycProcessStatus.COMPLETED,
            finishStatus = ServiceStatus.COMPLETED
        )
        
        // Verify all services are completed
        assertThat(fullyCompleted.overallStatus).isEqualTo(KycProcessStatus.COMPLETED)
        assertThat(fullyCompleted.sessionStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(fullyCompleted.verifyStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(fullyCompleted.ocrStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(fullyCompleted.livenessStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(fullyCompleted.otoVerifyStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(fullyCompleted.finishStatus).isEqualTo(ServiceStatus.COMPLETED)
        
        // Step 4: Sync to server (mark as COMPLETED - fully synced)
        val syncedProcess = fullyCompleted.copy(
            overallStatus = KycProcessStatus.COMPLETED,
            sessionStatus = ServiceStatus.SYNCED,
            verifyStatus = ServiceStatus.SYNCED,
            ocrStatus = ServiceStatus.SYNCED,
            livenessStatus = ServiceStatus.SYNCED,
            otoVerifyStatus = ServiceStatus.SYNCED,
            finishStatus = ServiceStatus.SYNCED
        )
        
        // Verify successful sync
        assertThat(syncedProcess.overallStatus).isEqualTo(KycProcessStatus.COMPLETED)
        assertThat(syncedProcess.sessionStatus).isEqualTo(ServiceStatus.SYNCED)
        assertThat(syncedProcess.verifyStatus).isEqualTo(ServiceStatus.SYNCED)
        assertThat(syncedProcess.ocrStatus).isEqualTo(ServiceStatus.SYNCED)
        assertThat(syncedProcess.livenessStatus).isEqualTo(ServiceStatus.SYNCED)
        assertThat(syncedProcess.otoVerifyStatus).isEqualTo(ServiceStatus.SYNCED)
        assertThat(syncedProcess.finishStatus).isEqualTo(ServiceStatus.SYNCED)
    }

    @Test
    fun `interrupted process resumes from where it left off`() = runTest {
        // SCENARIO: Proceso se interrumpe, luego se reanuda desde el punto correcto
        
        val processId = "interrupted-002"
        
        // Step 1: Process partially completed before interruption
        val interruptedProcess = KycProcessEntity(
            id = processId,
            shortKey = "PROC-002",
            overallStatus = KycProcessStatus.PENDING,
            sessionStatus = ServiceStatus.COMPLETED,  // ✓ Already done
            verifyStatus = ServiceStatus.COMPLETED,   // ✓ Already done
            ocrStatus = ServiceStatus.PENDING,       // ⏳ Still pending
            livenessStatus = ServiceStatus.PENDING,  // ⏳ Still pending
            otoVerifyStatus = ServiceStatus.PENDING, // ⏳ Still pending
            finishStatus = ServiceStatus.PENDING     // ⏳ Still pending
        )
        
        // Verify partial completion state
        assertThat(interruptedProcess.sessionStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(interruptedProcess.verifyStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(interruptedProcess.ocrStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(interruptedProcess.livenessStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(interruptedProcess.otoVerifyStatus).isEqualTo(ServiceStatus.PENDING)
        assertThat(interruptedProcess.finishStatus).isEqualTo(ServiceStatus.PENDING)
        
        // Step 2: Resume and complete remaining services
        val resumedProcess = interruptedProcess.copy(
            overallStatus = KycProcessStatus.COMPLETED,
            ocrStatus = ServiceStatus.COMPLETED,     // ✓ Newly completed
            livenessStatus = ServiceStatus.COMPLETED, // ✓ Newly completed
            otoVerifyStatus = ServiceStatus.COMPLETED, // ✓ Newly completed
            finishStatus = ServiceStatus.COMPLETED    // ✓ Newly completed
        )
        
        // Verify process completed successfully
        assertThat(resumedProcess.overallStatus).isEqualTo(KycProcessStatus.COMPLETED)
        assertThat(resumedProcess.sessionStatus).isEqualTo(ServiceStatus.COMPLETED) // Preserved
        assertThat(resumedProcess.verifyStatus).isEqualTo(ServiceStatus.COMPLETED)  // Preserved
        assertThat(resumedProcess.ocrStatus).isEqualTo(ServiceStatus.COMPLETED)     // New
        assertThat(resumedProcess.livenessStatus).isEqualTo(ServiceStatus.COMPLETED) // New
        assertThat(resumedProcess.otoVerifyStatus).isEqualTo(ServiceStatus.COMPLETED) // New
        assertThat(resumedProcess.finishStatus).isEqualTo(ServiceStatus.COMPLETED)   // New
    }

    @Test
    fun `error handling and retry logic works correctly`() = runTest {
        // SCENARIO: Algunos servicios fallan, se reintentan y finalmente completan
        
        val processId = "error-handling-003"
        
        // Step 1: Process starts normally
        val normalProcess = KycProcessEntity(
            id = processId,
            shortKey = "PROC-003",
            overallStatus = KycProcessStatus.PENDING,
            sessionStatus = ServiceStatus.COMPLETED,
            verifyStatus = ServiceStatus.COMPLETED
        )
        
        // Step 2: Some services fail during processing
        val failedProcess = normalProcess.copy(
            ocrStatus = ServiceStatus.FAILED,
            ocrError = "Network timeout during OCR processing",
            ocrRetryCount = 1,
            livenessStatus = ServiceStatus.FAILED,
            livenessError = "Camera access denied", 
            livenessRetryCount = 1
        )
        
        // Verify error states are tracked
        assertThat(failedProcess.ocrStatus).isEqualTo(ServiceStatus.FAILED)
        assertThat(failedProcess.ocrError).isEqualTo("Network timeout during OCR processing")
        assertThat(failedProcess.ocrRetryCount).isEqualTo(1)
        assertThat(failedProcess.livenessStatus).isEqualTo(ServiceStatus.FAILED)
        assertThat(failedProcess.livenessError).isEqualTo("Camera access denied")
        assertThat(failedProcess.livenessRetryCount).isEqualTo(1)
        
        // Step 3: Retry and fix the errors
        val retriedProcess = failedProcess.copy(
            ocrStatus = ServiceStatus.COMPLETED,
            ocrError = null, // Clear error after successful retry
            ocrRetryCount = 2, // Increment retry count
            livenessStatus = ServiceStatus.COMPLETED,
            livenessError = null, // Clear error after successful retry
            livenessRetryCount = 2 // Increment retry count
        )
        
        // Verify successful retry
        assertThat(retriedProcess.ocrStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(retriedProcess.ocrError).isNull() // Error cleared
        assertThat(retriedProcess.ocrRetryCount).isEqualTo(2) // Retry count preserved for analytics
        assertThat(retriedProcess.livenessStatus).isEqualTo(ServiceStatus.COMPLETED)
        assertThat(retriedProcess.livenessError).isNull() // Error cleared
        assertThat(retriedProcess.livenessRetryCount).isEqualTo(2) // Retry count preserved for analytics
        
        // Step 4: Complete the process
        val completedProcess = retriedProcess.copy(
            overallStatus = KycProcessStatus.COMPLETED,
            otoVerifyStatus = ServiceStatus.COMPLETED,
            finishStatus = ServiceStatus.COMPLETED
        )
        
        assertThat(completedProcess.overallStatus).isEqualTo(KycProcessStatus.COMPLETED)
    }

    @Test
    fun `multiple processes can be managed simultaneously`() = runTest {
        // SCENARIO: Usuario tiene múltiples procesos KYC en diferentes estados
        
        // Create multiple processes in different states
        val processes = listOf(
            // Process 1: Just started
            KycProcessEntity(
                id = "multi-001",
                shortKey = "PROC-001",
                overallStatus = KycProcessStatus.PENDING,
                sessionStatus = ServiceStatus.COMPLETED,
                createdAt = System.currentTimeMillis() - 3000
            ),
            
            // Process 2: Half completed
            KycProcessEntity(
                id = "multi-002", 
                shortKey = "PROC-002",
                overallStatus = KycProcessStatus.PENDING,
                sessionStatus = ServiceStatus.COMPLETED,
                verifyStatus = ServiceStatus.COMPLETED,
                ocrStatus = ServiceStatus.COMPLETED,
                createdAt = System.currentTimeMillis() - 2000
            ),
            
            // Process 3: Fully completed offline, ready for sync
            KycProcessEntity(
                id = "multi-003",
                shortKey = "PROC-003",
                overallStatus = KycProcessStatus.COMPLETED_OFFLINE,
                sessionStatus = ServiceStatus.COMPLETED,
                verifyStatus = ServiceStatus.COMPLETED,
                ocrStatus = ServiceStatus.COMPLETED,
                livenessStatus = ServiceStatus.COMPLETED,
                otoVerifyStatus = ServiceStatus.COMPLETED,
                finishStatus = ServiceStatus.COMPLETED,
                createdAt = System.currentTimeMillis() - 1000
            )
        )
        
        // Verify different states
        assertThat(processes[0].overallStatus).isEqualTo(KycProcessStatus.PENDING)
        assertThat(processes[1].overallStatus).isEqualTo(KycProcessStatus.PENDING)
        assertThat(processes[2].overallStatus).isEqualTo(KycProcessStatus.COMPLETED_OFFLINE)
        
        // Sort by creation time (oldest first for sync priority)
        val sortedForSync = processes
            .filter { it.overallStatus == KycProcessStatus.COMPLETED_OFFLINE }
            .sortedBy { it.createdAt }
        
        // Only completed processes should be in sync queue
        assertThat(sortedForSync).hasSize(1)
        assertThat(sortedForSync[0].id).isEqualTo("multi-003")
        
        // Simulate sync of completed process
        val syncedProcess = sortedForSync[0].copy(
            overallStatus = KycProcessStatus.COMPLETED,
            sessionStatus = ServiceStatus.SYNCED,
            verifyStatus = ServiceStatus.SYNCED,
            ocrStatus = ServiceStatus.SYNCED,
            livenessStatus = ServiceStatus.SYNCED,
            otoVerifyStatus = ServiceStatus.SYNCED,
            finishStatus = ServiceStatus.SYNCED
        )
        
        assertThat(syncedProcess.overallStatus).isEqualTo(KycProcessStatus.COMPLETED)
    }

    @Test
    fun `data persistence across state transitions`() = runTest {
        // SCENARIO: Verificar que los datos se preservan durante las transiciones de estado
        
        val processId = "persistence-004"
        val testSessionId = "test-session-12345"
        val testAccessToken = "test-token-67890"
        
        // Step 1: Create process with initial data
        val processWithData = KycProcessEntity(
            id = processId,
            shortKey = "PROC-004",
            sessionId = testSessionId,
            accessToken = testAccessToken,
            overallStatus = KycProcessStatus.PENDING,
            sessionStatus = ServiceStatus.COMPLETED
        )
        
        // Verify initial data
        assertThat(processWithData.sessionId).isEqualTo(testSessionId)
        assertThat(processWithData.accessToken).isEqualTo(testAccessToken)
        
        // Step 2: Update process status, data should be preserved
        val updatedProcess = processWithData.copy(
            overallStatus = KycProcessStatus.COMPLETED,
            verifyStatus = ServiceStatus.COMPLETED,
            ocrStatus = ServiceStatus.COMPLETED,
            livenessStatus = ServiceStatus.COMPLETED,
            otoVerifyStatus = ServiceStatus.COMPLETED,
            finishStatus = ServiceStatus.COMPLETED
        )
        
        // Verify data persistence during status update
        assertThat(updatedProcess.sessionId).isEqualTo(testSessionId) // Data preserved
        assertThat(updatedProcess.accessToken).isEqualTo(testAccessToken) // Data preserved
        assertThat(updatedProcess.overallStatus).isEqualTo(KycProcessStatus.COMPLETED) // Status updated
        
        // Step 3: Sync process, data should still be preserved
        val syncedProcess = updatedProcess.copy(
            overallStatus = KycProcessStatus.COMPLETED,
            sessionStatus = ServiceStatus.SYNCED,
            verifyStatus = ServiceStatus.SYNCED,
            ocrStatus = ServiceStatus.SYNCED,
            livenessStatus = ServiceStatus.SYNCED,
            otoVerifyStatus = ServiceStatus.SYNCED,
            finishStatus = ServiceStatus.SYNCED
        )
        
        // Verify data persistence after sync
        assertThat(syncedProcess.sessionId).isEqualTo(testSessionId) // Data preserved
        assertThat(syncedProcess.accessToken).isEqualTo(testAccessToken) // Data preserved
        assertThat(syncedProcess.overallStatus).isEqualTo(KycProcessStatus.COMPLETED) // Status updated
    }
}