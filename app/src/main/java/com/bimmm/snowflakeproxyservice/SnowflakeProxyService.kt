package com.bimmm.snowflakeproxyservice

import android.app.Service
import android.content.Intent
import android.os.IBinder

import IPtProxy.IPtProxy
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Binder
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.File

class SnowflakeProxyService : Service(), PowerConnectionReceiver.Callback {

    override fun onBind(intent: Intent?): IBinder? = binder

    inner class LocalBinder : Binder() {
        fun getService() : SnowflakeProxyService = this@SnowflakeProxyService
    }
    private val binder = LocalBinder()

    private var shouldCheckForPower = false
    private var shouldCheckForUnmetered = false

    companion object {
        val ACTION_CLIENT_CONNECTED = "com.bimm.snowflakeproxyservice.ACTION_CLIENT_CONNECTED"
        val ACTION_START = "com.bimm.snowflakeproxyservice.ACTION_START"
        val EXTRA_START_CHECK_POWER = "com.bimm.snowflakeproxyservice.EXTRA_START_CHECK_POWER"
        val EXTRA_START_CHECK_UNMETERED = "com.bimm.snowflakeproxyservice.EXTRA_START_CHECK_UNMETERED"
        val ACTION_PAUSING = "com.bimm.snowflakeproxyservice.ACTION_PAUSING"
        val EXTRA_PAUSING_REASON = "com.bimm.snowflakeproxyservice.EXTRA_PAUSING_REASON"
        val ACTION_RESUMING = "com.bimm.snowflakeproxyservice.ACTION_RESUMING"
    }

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var powerReceiver: PowerConnectionReceiver

    private var isPowerConnected = false
    private var isUnmetered = false
    private var isProxyRunning = false


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
        super.onStartCommand(intent, flags, startId)
        shouldCheckForPower = intent?.getBooleanExtra(EXTRA_START_CHECK_POWER, false) ?: false
        shouldCheckForUnmetered = intent?.getBooleanExtra(EXTRA_START_CHECK_UNMETERED, false) ?: false
        Log.d("test", "shouldCheckForPower=$shouldCheckForPower")
        Log.d("test", "shouldCheckForUnmetered=$shouldCheckForUnmetered")
        return START_STICKY
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
        Log.d("test", "onStateUpdated()")
        if (shouldCheckForPower && !isPowerConnected) {
            pauseSnowflakeProxy("power isn't connected")
            return
        }
        if (shouldCheckForUnmetered && !isUnmetered) {
            pauseSnowflakeProxy("network is metered")
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
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_CLIENT_CONNECTED))
            }
            isProxyRunning = true
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_RESUMING))
        }
    }


    private fun pauseSnowflakeProxy(reason: String) {
        Log.d("test", "pausing snowflake proxy: $reason")
        if (isProxyRunning) {
            IPtProxy.stopSnowflakeProxy()
            isProxyRunning = false
            var intent = Intent(ACTION_PAUSING).putExtra(EXTRA_PAUSING_REASON, reason)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

}