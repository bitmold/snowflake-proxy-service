package com.bimmm.snowflakeproxyservice

import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class NetworkConnectionCallback(private val activity: MainActivity) :
    ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        Log.d("test", "onAvailable: $network")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onLost(network: Network) {
        super.onLost(network)
        Log.d("test", "onLost: $network")
        activity.runOnUiThread {activity.testActiveNetwork23()}
    }

}