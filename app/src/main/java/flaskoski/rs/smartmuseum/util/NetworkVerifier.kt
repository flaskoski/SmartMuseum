package flaskoski.rs.smartmuseum.util

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.getSystemService
import android.annotation.TargetApi
import android.net.*
import android.os.Build
import androidx.annotation.RequiresApi
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */

class NetworkVerifier {

    private var onAvailableCallback: (() -> Unit)? = null
    private var onUnavailableCallback: (() -> Unit)? = null
    private var connectivityManager : ConnectivityManager? = null
    private var netCallback: ConnectivityManager.NetworkCallback? = null

    fun setOnAvailableCallback(callback : () -> Unit) : NetworkVerifier{
        onAvailableCallback = callback
        return this
    }
    fun setOnUnavailableCallback(callback : () -> Unit) : NetworkVerifier{
        onUnavailableCallback = callback
        return this
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager!!.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun registerNetworkCallbackV21(context : Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        netCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network?) {
                super.onAvailable(network)
                onAvailableCallback?.invoke()
            }

            override fun onUnavailable() {
                super.onUnavailable()
                onUnavailableCallback?.invoke()
            }
        }
        connectivityManager?.registerNetworkCallback(request, netCallback)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun unRegisterNetworkCallbackV21(){
        netCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
        }
    }
}