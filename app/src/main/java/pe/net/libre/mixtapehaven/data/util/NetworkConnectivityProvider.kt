package pe.net.libre.mixtapehaven.data.util

/**
 * Interface for checking network connectivity.
 * Abstracts Context dependency to improve testability and avoid memory leaks in ViewModels.
 */
interface NetworkConnectivityProvider {
    /**
     * Check if the device has an active network connection
     */
    fun isConnected(): Boolean

    /**
     * Check if the device is on a high-speed connection (WiFi/Ethernet)
     */
    fun isHighSpeedConnection(): Boolean

    /**
     * Check if the device is connected to a cellular network
     */
    fun isCellularConnection(): Boolean
}
