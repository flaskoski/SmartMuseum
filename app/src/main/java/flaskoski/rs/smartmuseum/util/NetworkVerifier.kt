package flaskoski.rs.smartmuseum.util

import android.content.Context
import android.net.NetworkInfo
import android.content.Context.CONNECTIVITY_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.net.ConnectivityManager



class NetworkVerifier {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager!!.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}