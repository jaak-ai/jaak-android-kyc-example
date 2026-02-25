package ai.jaak.kyc.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import ai.jaak.kyc.R
import ai.jaak.kyc.databinding.WidgetConnectivityStatusBinding
import ai.jaak.kyc.domain.service.NetworkConnectivityService

class ConnectivityStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: WidgetConnectivityStatusBinding
    private var currentNetworkState: NetworkConnectivityService.NetworkState? = null
    private var showDetailedInfo = false

    init {
        binding = WidgetConnectivityStatusBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListener()
    }

    private fun setupClickListener() {
        binding.root.setOnClickListener {
            showDetailedInfo = !showDetailedInfo
            updateDetailedView()
        }
    }

    fun updateNetworkState(networkState: NetworkConnectivityService.NetworkState, description: String) {
        val previousState = currentNetworkState
        currentNetworkState = networkState
        
        // Update main status
        binding.apply {
            tvStatusText.text = description
            updateStatusIcon(networkState)
            updateStatusColors(networkState)
            
            // Show/hide pending sync count if applicable
            updateSyncPendingCount(networkState)
            
            // Animate if connection state changed
            if (previousState != null && previousState.isConnected != networkState.isConnected) {
                animateConnectionChange(networkState.isConnected)
            }
            
            updateDetailedView()
        }
    }

    private fun updateStatusIcon(networkState: NetworkConnectivityService.NetworkState) {
        val iconRes = when {
            !networkState.isConnected -> R.drawable.ic_connectivity_offline
            networkState.connectionType == NetworkConnectivityService.ConnectionType.WIFI -> {
                when (networkState.signalStrength) {
                    5, 4 -> R.drawable.ic_wifi_strong
                    3 -> R.drawable.ic_wifi_medium
                    2, 1 -> R.drawable.ic_wifi_weak
                    else -> R.drawable.ic_wifi_none
                }
            }
            networkState.connectionType == NetworkConnectivityService.ConnectionType.CELLULAR -> {
                when (networkState.signalStrength) {
                    5, 4 -> R.drawable.ic_cellular_strong
                    3 -> R.drawable.ic_cellular_medium
                    2, 1 -> R.drawable.ic_cellular_weak
                    else -> R.drawable.ic_cellular_none
                }
            }
            networkState.connectionType == NetworkConnectivityService.ConnectionType.ETHERNET -> {
                R.drawable.ic_ethernet_connected
            }
            else -> R.drawable.ic_connectivity_unknown
        }
        
        binding.ivStatusIcon.setImageResource(iconRes)
    }

    private fun updateStatusColors(networkState: NetworkConnectivityService.NetworkState) {
        val backgroundColorRes = when {
            !networkState.isConnected -> R.color.error
            networkState.isMetered -> R.color.warning
            networkState.signalStrength >= 3 -> R.color.success
            else -> R.color.warning
        }
        
        val textColorRes = R.color.white
        
        binding.root.setBackgroundColor(context.getColor(backgroundColorRes))
        binding.tvStatusText.setTextColor(context.getColor(textColorRes))
        binding.tvSyncPending.setTextColor(context.getColor(textColorRes))
    }

    private fun updateSyncPendingCount(networkState: NetworkConnectivityService.NetworkState) {
        // This would typically be passed from outside or observed from a repository
        // For now, we'll show a placeholder
        if (!networkState.isConnected) {
            binding.tvSyncPending.visibility = View.VISIBLE
            binding.tvSyncPending.text = "3" // Placeholder count
        } else {
            binding.tvSyncPending.visibility = View.GONE
        }
    }

    private fun updateDetailedView() {
        val networkState = currentNetworkState ?: return
        
        if (showDetailedInfo) {
            binding.llDetailedInfo.visibility = View.VISIBLE
            
            // Connection type
            binding.tvConnectionType.text = "Tipo: ${getConnectionTypeText(networkState.connectionType)}"
            
            // Signal strength
            binding.progressSignal.progress = (networkState.signalStrength * 20) // Convert 0-5 to 0-100
            binding.tvSignalText.text = "Señal: ${networkState.signalStrength}/5"
            
            // Metered info
            if (networkState.isMetered) {
                binding.tvMeteredInfo.visibility = View.VISIBLE
                binding.tvMeteredInfo.text = "⚠️ Conexión limitada"
            } else {
                binding.tvMeteredInfo.visibility = View.GONE
            }
            
            // Connection stats
            binding.tvConnectionStats.text = buildString {
                if (networkState.lastConnectedAt > 0) {
                    append("Conectado: ${formatTime(networkState.lastConnectedAt)}")
                }
                if (networkState.connectionChanges > 0) {
                    if (isNotEmpty()) append(" • ")
                    append("Cambios: ${networkState.connectionChanges}")
                }
            }
            
        } else {
            binding.llDetailedInfo.visibility = View.GONE
        }
    }

    private fun animateConnectionChange(isConnected: Boolean) {
        val animation = if (isConnected) {
            AnimationUtils.loadAnimation(context, R.anim.connectivity_connected)
        } else {
            AnimationUtils.loadAnimation(context, R.anim.connectivity_disconnected)
        }
        
        binding.root.startAnimation(animation)
    }

    private fun getConnectionTypeText(type: NetworkConnectivityService.ConnectionType): String {
        return when (type) {
            NetworkConnectivityService.ConnectionType.WIFI -> "WiFi"
            NetworkConnectivityService.ConnectionType.CELLULAR -> "Datos móviles"
            NetworkConnectivityService.ConnectionType.ETHERNET -> "Ethernet"
            NetworkConnectivityService.ConnectionType.NONE -> "Sin conexión"
            NetworkConnectivityService.ConnectionType.UNKNOWN -> "Desconocido"
        }
    }

    private fun formatTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "hace ${hours}h"
            minutes > 0 -> "hace ${minutes}min"
            else -> "hace ${seconds}s"
        }
    }

    fun setSyncPendingCount(count: Int) {
        if (count > 0) {
            binding.tvSyncPending.visibility = View.VISIBLE
            binding.tvSyncPending.text = count.toString()
        } else {
            binding.tvSyncPending.visibility = View.GONE
        }
    }
}