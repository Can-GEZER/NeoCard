package com.cangzr.neocard.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * İnternet bağlantısını kontrol eden ve izleyen yardımcı sınıf
 */
class NetworkUtils private constructor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // İnternet bağlantısı durumu
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    // Ağ değişikliklerini dinlemek için callback
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isNetworkAvailable.value = true
        }

        override fun onLost(network: Network) {
            _isNetworkAvailable.value = false
        }

        override fun onUnavailable() {
            _isNetworkAvailable.value = false
        }
    }

    init {
        // Mevcut bağlantı durumunu kontrol et
        checkNetworkAvailability()
        
        // Ağ değişikliklerini dinlemeye başla
        registerNetworkCallback()
    }

    /**
     * Mevcut ağ bağlantısını kontrol eder
     */
    fun checkNetworkAvailability() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        _isNetworkAvailable.value = capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )
    }

    /**
     * Ağ değişikliklerini dinlemeye başlar
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    /**
     * Ağ değişikliklerini dinlemeyi durdurur
     */
    fun unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    companion object {
        @Volatile
        private var instance: NetworkUtils? = null
        
        fun getInstance(context: Context): NetworkUtils {
            return instance ?: synchronized(this) {
                instance ?: NetworkUtils(context.applicationContext).also { instance = it }
            }
        }
    }
}
