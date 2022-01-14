package com.bimmm.snowflakeproxyservice

import android.content.Intent
import android.os.IBinder

import IPtProxy.IPtProxy
import android.app.*
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
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
    private var clientsConnected = 0

    companion object {
        const val ACTION_CLIENT_CONNECTED = "com.bimm.snowflakeproxyservice.ACTION_CLIENT_CONNECTED"
        const val ACTION_START = "com.bimm.snowflakeproxyservice.ACTION_START"
        const val EXTRA_START_CHECK_POWER = "com.bimm.snowflakeproxyservice.EXTRA_START_CHECK_POWER"
        const val EXTRA_START_CHECK_UNMETERED = "com.bimm.snowflakeproxyservice.EXTRA_START_CHECK_UNMETERED"
        const val ACTION_PAUSING = "com.bimm.snowflakeproxyservice.ACTION_PAUSING"
        const val EXTRA_PAUSING_REASON = "com.bimm.snowflakeproxyservice.EXTRA_PAUSING_REASON"
        const val ACTION_RESUMING = "com.bimm.snowflakeproxyservice.ACTION_RESUMING"
        const val ONGOING_NOTIFICATION_CHANNEL = "snowflake_proxy_channel"
        const val ONGOING_NOTIFICATION_ID = 1
    }

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var powerReceiver: PowerConnectionReceiver

    private var isPowerConnected = false
    private var isUnmetered = false
    private var isProxyRunning = false

    private var notificationBuilder: NotificationCompat.Builder? = null


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

        showNotificationText()

        shouldCheckForPower = intent?.getBooleanExtra(EXTRA_START_CHECK_POWER, false) ?: false
        shouldCheckForUnmetered = intent?.getBooleanExtra(EXTRA_START_CHECK_UNMETERED, false) ?: false
        Log.d("test", "shouldCheckForPower=$shouldCheckForPower")
        Log.d("test", "shouldCheckForUnmetered=$shouldCheckForUnmetered")
        return START_STICKY
    }

    private fun showNotificationText() = showNotificationText("Clients Connected: $clientsConnected")

    private fun showNotificationText(text: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val appIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL)
                .setContentTitle("Snowflake Proxy Service")
                .setContentIntent(PendingIntent.getActivity(this, 0, appIntent, 0))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
        }

        val notification = notificationBuilder?.setContentText(text)?.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        } else {
            notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        Log.d("foo", "create channel")
        val channel = NotificationChannel(ONGOING_NOTIFICATION_CHANNEL, "Snowflake Proxy Service", NotificationManager.IMPORTANCE_LOW).apply {
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            description = "Information about snowflake"
            enableVibration(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
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
        val unsafeLogging = false
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
                clientsConnected++
                showNotificationText()
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