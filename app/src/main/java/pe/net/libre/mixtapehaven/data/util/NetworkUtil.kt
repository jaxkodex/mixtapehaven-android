package pe.net.libre.mixtapehaven.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utility class for network-related operations
 */
object NetworkUtil {

    /**
     * Network connection type
     */
    enum class NetworkType {
        WIFI,
        CELLULAR,
        ETHERNET,
        NONE
    }

    /**
     * Get the current network connection type
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkType.NONE

        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }
    }

    /**
     * Check if the device is connected to WiFi or Ethernet (high-speed connections)
     */
    fun isHighSpeedConnection(context: Context): Boolean {
        val networkType = getNetworkType(context)
        return networkType == NetworkType.WIFI || networkType == NetworkType.ETHERNET
    }

    /**
     * Check if the device is connected to a cellular network
     */
    fun isCellularConnection(context: Context): Boolean {
        return getNetworkType(context) == NetworkType.CELLULAR
    }

    /**
     * Check if the device has any network connection
     */
    fun isConnected(context: Context): Boolean {
        return getNetworkType(context) != NetworkType.NONE
    }

    /**
     * Create a NetworkConnectivityProvider instance that wraps the application context.
     * Use this to inject into ViewModels instead of passing Context directly.
     */
    fun createProvider(context: Context): NetworkConnectivityProvider {
        return object : NetworkConnectivityProvider {
            override fun isConnected(): Boolean {
                return isConnected(context)
            }

            override fun isHighSpeedConnection(): Boolean {
                return isHighSpeedConnection(context)
            }

            override fun isCellularConnection(): Boolean {
                return isCellularConnection(context)
            }
        }
    }
}
