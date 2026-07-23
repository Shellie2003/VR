package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * Small wrapper around ConnectivityManager used to gate the automatic Firebase Cloud Backup
 * (only upload when there is real, validated internet access) and to react immediately when
 * connectivity comes back after being offline, instead of waiting for the next local mutation.
 */
object NetworkMonitor {
    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Throwable) {
            false
        }
    }

    /** Registers a callback fired every time the device regains internet access. Returns the
     * callback handle so the caller can unregister it (e.g. in onCleared()); safe to leave
     * registered for the app's lifetime otherwise. */
    fun observeOnline(context: Context, onAvailable: () -> Unit): ConnectivityManager.NetworkCallback? {
        return try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return null
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onAvailable()
                }
            }
            cm.registerNetworkCallback(request, callback)
            callback
        } catch (e: Throwable) {
            null
        }
    }

    fun stopObserving(context: Context, callback: ConnectivityManager.NetworkCallback?) {
        if (callback == null) return
        try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.unregisterNetworkCallback(callback)
        } catch (e: Throwable) {
            // Already unregistered or system service unavailable — safe to ignore.
        }
    }
}
