package ai.jaak.kyc.ui.view

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ai.jaak.kyc.R
import ai.jaak.kyc.data.local.entity.KycProcessEntity
import ai.jaak.kyc.data.local.entity.KycProcessStatus
import ai.jaak.kyc.databinding.ActivityKycProcessesBinding
import ai.jaak.kyc.domain.service.NetworkConnectivityService
import ai.jaak.kyc.ui.adapter.KycProcessAdapter
import ai.jaak.kyc.ui.viewmodel.KycOfflineViewModel
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class KycProcessesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKycProcessesBinding
    private val viewModel: KycOfflineViewModel by viewModels()
    private lateinit var processAdapter: KycProcessAdapter
    
    private var allProcesses = listOf<KycProcessEntity>()
    private var filteredProcesses = listOf<KycProcessEntity>()
    private var currentFilter = ProcessFilter.ALL
    
    @Inject
    lateinit var networkConnectivityService: NetworkConnectivityService
    

    enum class ProcessFilter {
        ALL, PENDING, COMPLETED, OFFLINE, FAILED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycProcessesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        setupNetworkMonitoring()
        
        // Start network monitoring
        networkConnectivityService.startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        networkConnectivityService.stopMonitoring()
    }

    private fun setupRecyclerView() {
        processAdapter = KycProcessAdapter(
            onItemClick = { process ->
                // Navigate to process details or continue KYC flow
                viewModel.setCurrentProcess(process.id)
                navigateToKycFlow(process)
            },
            onSyncClick = { process ->
                syncProcess(process)
            },
            onViewDetailsClick = { process ->
                // Show process details dialog or navigate to details screen
                showProcessDetails(process)
            }
        )
        
        binding.rvProcesses.apply {
            layoutManager = LinearLayoutManager(this@KycProcessesActivity)
            adapter = processAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnNewProcess.setOnClickListener {
            showNewProcessDialog()
        }
        
        binding.btnSyncAll.setOnClickListener {
            syncAllProcesses()
        }
        
        binding.btnOpenSync.setOnClickListener {
            val intent = Intent(this, KycSyncActivity::class.java)
            startActivity(intent)
        }
        
        // Filter buttons
        binding.btnFilterAll.setOnClickListener { applyFilter(ProcessFilter.ALL) }
        binding.btnFilterPending.setOnClickListener { applyFilter(ProcessFilter.PENDING) }
        binding.btnFilterCompleted.setOnClickListener { applyFilter(ProcessFilter.COMPLETED) }
        binding.btnFilterOffline.setOnClickListener { applyFilter(ProcessFilter.OFFLINE) }
        binding.btnFilterFailed.setOnClickListener { applyFilter(ProcessFilter.FAILED) }
        
        // Set initial filter
        applyFilter(ProcessFilter.ALL)
    }

    private fun observeViewModel() {
        // All processes
        lifecycleScope.launch {
            viewModel.allProcesses.collect { processes ->
                allProcesses = processes
                binding.tvProcessesCount.text = getString(R.string.total_processes, processes.size)
                applyCurrentFilter()
            }
        }
        
        // Network status is now handled by setupNetworkMonitoring()
        
        // Sync status
        lifecycleScope.launch {
            viewModel.syncInProgress.collect { inProgress ->
                binding.llProgress.visibility = if (inProgress) View.VISIBLE else View.GONE
                binding.btnSyncAll.isEnabled = !inProgress && viewModel.isNetworkAvailable.value
            }
        }
        
        lifecycleScope.launch {
            viewModel.syncResult.collect { result ->
                result?.let {
                    Toast.makeText(this@KycProcessesActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearMessages()
                }
            }
        }
        
        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state if needed
        }
        
        // Error handling
        viewModel.errorModel.observe(this) { errorModel ->
            errorModel?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }
        
        // Success messages
        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun setupNetworkMonitoring() {
        // Observe network state changes
        lifecycleScope.launch {
            networkConnectivityService.networkState.collect { networkState ->
                updateConnectivityUI(networkState)
            }
        }
    }

    private fun updateConnectivityUI(networkState: NetworkConnectivityService.NetworkState) {
        val description = networkConnectivityService.getNetworkStatusDescription()
        binding.connectivityStatusView.updateNetworkState(networkState, description)
        
        // Update sync pending count
        val pendingSync = allProcesses.count { it.requiresSync }
        binding.connectivityStatusView.setSyncPendingCount(pendingSync)
        
        // Update sync buttons based on network status
        binding.btnSyncAll.isEnabled = networkState.isConnected && pendingSync > 0
    }

    private fun applyFilter(filter: ProcessFilter) {
        currentFilter = filter
        
        // Update filter button states
        listOf(
            binding.btnFilterAll,
            binding.btnFilterPending,
            binding.btnFilterCompleted,
            binding.btnFilterOffline,
            binding.btnFilterFailed
        ).forEach { it.isSelected = false }
        
        when (filter) {
            ProcessFilter.ALL -> binding.btnFilterAll.isSelected = true
            ProcessFilter.PENDING -> binding.btnFilterPending.isSelected = true
            ProcessFilter.COMPLETED -> binding.btnFilterCompleted.isSelected = true
            ProcessFilter.OFFLINE -> binding.btnFilterOffline.isSelected = true
            ProcessFilter.FAILED -> binding.btnFilterFailed.isSelected = true
        }
        
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        filteredProcesses = when (currentFilter) {
            ProcessFilter.ALL -> allProcesses
            ProcessFilter.PENDING -> allProcesses.filter { 
                it.overallStatus == KycProcessStatus.PENDING || it.overallStatus == KycProcessStatus.IN_PROGRESS 
            }
            ProcessFilter.COMPLETED -> allProcesses.filter { 
                it.overallStatus == KycProcessStatus.COMPLETED 
            }
            ProcessFilter.OFFLINE -> allProcesses.filter { 
                it.overallStatus == KycProcessStatus.COMPLETED_OFFLINE || it.requiresSync 
            }
            ProcessFilter.FAILED -> allProcesses.filter { 
                it.overallStatus == KycProcessStatus.FAILED 
            }
        }
        
        processAdapter.submitList(filteredProcesses)
        
        // Show/hide empty state
        if (filteredProcesses.isEmpty()) {
            binding.llEmptyState.visibility = View.VISIBLE
            binding.rvProcesses.visibility = View.GONE
        } else {
            binding.llEmptyState.visibility = View.GONE
            binding.rvProcesses.visibility = View.VISIBLE
        }
    }

    private fun showNewProcessDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New KYC Process")
        builder.setMessage("Enter Short Key:")
        
        val input = android.widget.EditText(this)
        input.hint = "Short Key"
        builder.setView(input)
        
        builder.setPositiveButton("Create") { _, _ ->
            val shortKey = input.text.toString().trim()
            if (shortKey.isNotEmpty()) {
                viewModel.createNewProcess(shortKey)
            } else {
                Toast.makeText(this, "Short key cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun syncProcess(process: KycProcessEntity) {
        if (!viewModel.isNetworkAvailable.value) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.setCurrentProcess(process.id)
        viewModel.syncCurrentProcess()
    }

    private fun syncAllProcesses() {
        if (!viewModel.isNetworkAvailable.value) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        
        val processesToSync = allProcesses.filter { it.requiresSync }
        if (processesToSync.isEmpty()) {
            Toast.makeText(this, "No processes need sync", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Sync All Processes")
            .setMessage("Sync ${processesToSync.size} processes? This may take a while.")
            .setPositiveButton("Sync") { _, _ ->
                viewModel.syncAllProcesses()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProcessDetails(process: KycProcessEntity) {
        // Implementation for showing process details
        // Could be a dialog or navigate to a details screen
        Toast.makeText(this, "Process details: ${process.id}", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToKycFlow(process: KycProcessEntity) {
        // Navigate to appropriate KYC flow screen based on process status
        when (process.overallStatus) {
            KycProcessStatus.PENDING, KycProcessStatus.IN_PROGRESS -> {
                // Navigate to MenuMainActivity or next appropriate step
                val intent = Intent(this, MenuMainActivity::class.java)
                startActivity(intent)
            }
            KycProcessStatus.COMPLETED, KycProcessStatus.COMPLETED_OFFLINE -> {
                // Show completion screen or details
                Toast.makeText(this, "Process already completed", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Process status: ${process.overallStatus}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}