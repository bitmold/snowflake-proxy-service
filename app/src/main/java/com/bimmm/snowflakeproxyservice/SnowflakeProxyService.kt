package com.bimmm.snowflakeproxyservice

import android.app.Service
import android.content.Intent
import android.os.IBinder

import IPtProxy.IPtProxy
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.io.File

class SnowflakeProxyService : Service(), PowerConnectionReceiver.Callback {

    override fun onBind(intent: Intent?): IBinder? = null


    companion object {
        var shouldCheckForPower = true
        var shouldCheckForUnmetered = true
    }

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var powerReceiver: PowerConnectionReceiver

    var isPowerConnected = false
    var isUnmetered = false
    var isProxyRunning = false


    override fun onCreate() {
        super.onCreate()
        Log.d("test", "onCreate()")
        configIPtProxy()
        connectivityManager = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        initializeStateVars()
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("test", "onAvailable $network")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("test", "onLost $network")
                onNetworkStateChanged()
            }
        })

        powerReceiver = PowerConnectionReceiver(this)
        registerReceiver(powerReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        })

    }

    private fun initializeStateVars() {
        isPowerConnected = PowerConnectionReceiver.currentPowerStateCharged(this)
        onNetworkStateChanged()
    }

    private fun configIPtProxy() {
        val cache = File(cacheDir, "pt")
        if (!cache.exists()) cache.mkdir()
        IPtProxy.setStateLocation(cache.absolutePath)
    }

    override fun onDestroy() {
        unregisterReceiver(powerReceiver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onPowerStateChanged(isPowerConnected: Boolean) {
        this.isPowerConnected = isPowerConnected
        onStateUpdated()
    }

    private fun onNetworkStateChanged() {
        isUnmetered = !connectivityManager.isActiveNetworkMetered
        onStateUpdated()
    }

    private fun onStateUpdated() {
        if (shouldCheckForPower && !isPowerConnected) {
            stopSnowflakeProxy("power isn't connected")
            return
        }
        if (shouldCheckForUnmetered && !isUnmetered) {
            stopSnowflakeProxy("network is metered")
            return
        }
        // if the proxy is already started, calling start has no effect
        startSnowflakeProxy()
    }

    private fun startSnowflakeProxy() {
        Log.d("test", "Starting snowflake proxy...")
        val broker: String? = null // "https://snowflake-broker.bamsoftware.com/";
        val relay: String? = null // "wss://snowflake.bamsoftware.com/";
        val stun: String? = null // "stun:stun.stunprotocol.org:3478";
        val natProbe: String? = null
        val logFile: String? = null
        val keepLocalAddresses = true
        val unsafeLogging = true // todo for now ...
        if (!isProxyRunning) {
            Toast.makeText(this, "Starting snowflake proxy...", Toast.LENGTH_LONG).show()
            IPtProxy.startSnowflakeProxy(
                1,
                broker,
                relay,
                stun,
                natProbe,
                logFile,
                keepLocalAddresses,
                unsafeLogging
            ) {
                Handler(mainLooper).post {
                    Toast.makeText(
                        applicationContext,
                        "Snowflake client connected",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            isProxyRunning = true
        }
    }


    private fun stopSnowflakeProxy(reason: String) {
        Log.d("test", "stopping snowflake proxy: $reason")
        if (isProxyRunning) {
            Toast.makeText(this, "Stopping snowflake proxy: $reason", Toast.LENGTH_LONG).show()
            IPtProxy.stopSnowflakeProxy()
            isProxyRunning = false
        }
    }

}