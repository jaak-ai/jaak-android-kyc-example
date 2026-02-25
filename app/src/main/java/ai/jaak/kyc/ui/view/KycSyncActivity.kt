package ai.jaak.kyc.ui.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ai.jaak.kyc.databinding.ActivityKycSyncBinding
import ai.jaak.kyc.ui.adapter.ActiveWorkAdapter
import ai.jaak.kyc.ui.adapter.SyncHistoryAdapter
import ai.jaak.kyc.ui.viewmodel.KycSyncViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KycSyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKycSyncBinding
    private val viewModel: KycSyncViewModel by viewModels()
    
    private lateinit var activeWorkAdapter: ActiveWorkAdapter
    private lateinit var syncHistoryAdapter: SyncHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        
        viewModel.loadSyncData()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar datos cuando regrese a la pantalla
        viewModel.refreshData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // TODO: Agregar botón de refresh en el menú
        // binding.toolbar.inflateMenu(ai.jaak.kyc.R.menu.menu_sync)
        // binding.toolbar.setOnMenuItemClickListener { item ->
        //     when (item.itemId) {
        //         ai.jaak.kyc.R.id.action_refresh -> {
        //             viewModel.refreshData()
        //             true
        //         }
        //         else -> false
        //     }
        // }
    }

    private fun setupRecyclerViews() {
        // Active works RecyclerView
        activeWorkAdapter = ActiveWorkAdapter { workInfo ->
            // Handle work item click if needed
        }
        binding.rvActiveWorks.apply {
            layoutManager = LinearLayoutManager(this@KycSyncActivity)
            adapter = activeWorkAdapter
        }

        // Sync history RecyclerView
        syncHistoryAdapter = SyncHistoryAdapter { historyItem ->
            // Handle history item click if needed
        }
        binding.rvSyncHistory.apply {
            layoutManager = LinearLayoutManager(this@KycSyncActivity)
            adapter = syncHistoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSyncAll.setOnClickListener {
            viewModel.startFullSync()
        }

        binding.btnBulkSync.setOnClickListener {
            viewModel.startBulkSync()
        }

        binding.btnScheduleSync.setOnClickListener {
            viewModel.schedulePeriodicSync()
        }

        binding.btnCancelSync.setOnClickListener {
            viewModel.cancelCurrentSync()
        }

        binding.btnClearHistory.setOnClickListener {
            viewModel.clearSyncHistory()
        }
    }

    private fun observeViewModel() {
        // Network status
        viewModel.networkStatus.observe(this) { status ->
            updateNetworkStatus(status)
        }

        // Sync statistics
        viewModel.syncStats.observe(this) { stats ->
            updateSyncStatistics(stats)
        }

        // Current sync progress
        viewModel.currentSyncProgress.observe(this) { progress ->
            updateSyncProgress(progress)
        }

        // Active works
        viewModel.activeWorks.observe(this) { works ->
            activeWorkAdapter.updateWorks(works)
            binding.tvNoActiveWorks.visibility = 
                if (works.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Sync history
        viewModel.syncHistory.observe(this) { history ->
            syncHistoryAdapter.updateHistory(history)
        }

        // Sync actions enable/disable state
        viewModel.canStartSync.observe(this) { canStart ->
            binding.btnSyncAll.isEnabled = canStart
            binding.btnBulkSync.isEnabled = canStart
        }
    }

    private fun updateNetworkStatus(status: KycSyncViewModel.NetworkStatus) {
        binding.apply {
            when (status.isConnected) {
                true -> {
                    ivNetworkStatus.setImageResource(ai.jaak.kyc.R.drawable.ic_service_completed)
                    tvNetworkStatus.text = "Red Conectada"
                    tvNetworkDetails.text = status.details
                }
                false -> {
                    ivNetworkStatus.setImageResource(ai.jaak.kyc.R.drawable.ic_service_failed)
                    tvNetworkStatus.text = "Sin Conexión"
                    tvNetworkDetails.text = "Modo offline activo"
                }
            }
        }
    }

    private fun updateSyncStatistics(stats: KycSyncViewModel.SyncStatistics) {
        binding.apply {
            tvPendingProcesses.text = stats.pendingProcesses.toString()
            tvPendingServices.text = stats.pendingServices.toString()
            tvLastSync.text = stats.lastSyncTime
        }
    }

    private fun updateSyncProgress(progress: KycSyncViewModel.SyncProgressInfo?) {
        binding.apply {
            if (progress != null) {
                cardSyncProgress.visibility = android.view.View.VISIBLE
                tvSyncStatus.text = progress.statusMessage
                tvSyncProgress.text = progress.progressText
                progressSync.progress = progress.percentage
                progressSync.isIndeterminate = progress.isIndeterminate
            } else {
                cardSyncProgress.visibility = android.view.View.GONE
            }
        }
    }
}