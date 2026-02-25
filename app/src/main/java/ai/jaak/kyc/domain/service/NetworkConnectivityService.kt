package ai.jaak.kyc.domain.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConnectivityService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected
    
    private var isMonitoring = false
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkState()
        }

        override fun onLost(network: Network) {
            updateNetworkState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateNetworkState()
        }

        override fun onUnavailable() {
            _networkState.value = NetworkState(
                isConnected = false,
                connectionType = ConnectionType.NONE,
                isMetered = false,
                signalStrength = 0,
                lastDisconnectedAt = System.currentTimeMillis()
            )
            _isConnected.postValue(false)
        }
    }

    init {
        updateNetworkState()
    }

    fun startMonitoring() {
        if (!isMonitoring && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
            
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isMonitoring = true
        }
    }

    fun stopMonitoring() {
        if (isMonitoring && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isMonitoring = false
        }
    }

    fun isNetworkAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnectedOrConnecting == true
        }
    }

    fun getConnectionType(): ConnectionType {
        if (!isNetworkAvailable()) return ConnectionType.NONE
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            return when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.WIFI
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.CELLULAR
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> ConnectionType.ETHERNET
                else -> ConnectionType.UNKNOWN
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> ConnectionType.WIFI
                ConnectivityManager.TYPE_MOBILE -> ConnectionType.CELLULAR
                ConnectivityManager.TYPE_ETHERNET -> ConnectionType.ETHERNET
                else -> ConnectionType.UNKNOWN
            }
        }
    }

    fun isMeteredConnection(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.isActiveNetworkMetered
        } else {
            // Assume cellular is metered for older versions
            getConnectionType() == ConnectionType.CELLULAR
        }
    }

    fun getSignalStrength(): Int {
        // This is a simplified signal strength indicator
        // In a real implementation, you would need to access TelephonyManager for cellular
        // or WifiManager for WiFi signal strength
        return if (isNetworkAvailable()) {
            when (getConnectionType()) {
                ConnectionType.WIFI -> 4 // Assume good WiFi signal
                ConnectionType.CELLULAR -> 3 // Assume decent cellular signal
                ConnectionType.ETHERNET -> 5 // Wired connection is best
                else -> 2
            }
        } else {
            0
        }
    }

    fun getNetworkStatusDescription(): String {
        val currentState = _networkState.value
        return when {
            !currentState.isConnected -> "Sin conexión a internet"
            currentState.connectionType == ConnectionType.WIFI -> {
                val quality = when (currentState.signalStrength) {
                    5 -> "excelente"
                    4 -> "buena"
                    3 -> "regular"
                    2 -> "débil"
                    else -> "muy débil"
                }
                "WiFi conectado - señal $quality"
            }
            currentState.connectionType == ConnectionType.CELLULAR -> {
                val meteringStatus = if (currentState.isMetered) " (datos móviles)" else ""
                val quality = when (currentState.signalStrength) {
                    5 -> "excelente"
                    4 -> "buena"
                    3 -> "regular"
                    2 -> "débil"
                    else -> "muy débil"
                }
                "Datos móviles$meteringStatus - señal $quality"
            }
            currentState.connectionType == ConnectionType.ETHERNET -> "Conexión por cable"
            else -> "Conectado"
        }
    }

    private fun updateNetworkState() {
        val isConnected = isNetworkAvailable()
        val connectionType = getConnectionType()
        val isMetered = isMeteredConnection()
        val signalStrength = getSignalStrength()
        
        val currentState = _networkState.value
        val newState = NetworkState(
            isConnected = isConnected,
            connectionType = connectionType,
            isMetered = isMetered,
            signalStrength = signalStrength,
            lastConnectedAt = if (isConnected && !currentState.isConnected) System.currentTimeMillis() else currentState.lastConnectedAt,
            lastDisconnectedAt = if (!isConnected && currentState.isConnected) System.currentTimeMillis() else currentState.lastDisconnectedAt,
            connectionChanges = if (isConnected != currentState.isConnected) currentState.connectionChanges + 1 else currentState.connectionChanges
        )
        
        _networkState.value = newState
        _isConnected.postValue(isConnected)
    }

    data class NetworkState(
        val isConnected: Boolean = false,
        val connectionType: ConnectionType = ConnectionType.NONE,
        val isMetered: Boolean = false,
        val signalStrength: Int = 0, // 0-5 scale
        val lastConnectedAt: Long = 0,
        val lastDisconnectedAt: Long = 0,
        val connectionChanges: Int = 0
    )

    enum class ConnectionType {
        NONE, WIFI, CELLULAR, ETHERNET, UNKNOWN
    }
}