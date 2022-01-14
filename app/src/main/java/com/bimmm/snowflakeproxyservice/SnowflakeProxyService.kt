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
        const val EXTRA_CLIENT_CONNECTED_COUNT = "com.bimm.snowflakeproxyservice.EXTRA_CLIENT_CONNECTED_COUNT"
        const val ACTION_START = "com.bimm.snowflakeproxyservice.ACTION_START"
        const val EXTRA_START_CHECK_POWER = "com.bimm.snowflakeproxyservice.EXTRA_START_CHECK_POWER"
        const val EXTRA_START_CHECK_UNMETERED = "com.bimm.snowflakeproxyservice.EXTRA_START_CHECK_UNMETERED"
        const val EXTRA_START_SHOW_TOAST = "com.bimm.snowflakeproxyservice.EXTRA_START_SHOW_TOAST"

        const val ACTION_PAUSING = "com.bimm.snowflakeproxyservice.ACTION_PAUSING"
        const val EXTRA_PAUSING_REASON = "com.bimm.snowflakeproxyservice.EXTRA_PAUSING_REASON"
        const val ACTION_RESUMING = "com.bimm.snowflakeproxyservice.ACTION_RESUMING"

        const val EXTRA_PROXY_CAPACITY = "com.bimm.snowflakeproxyservice.EXTRA_PROXY_CAPACITY"
        const val EXTRA_PROXY_NAT_PROBE_URL = "com.bimm.snowflakeproxyservice.EXTRA_PROXY_NAT_PROBE_URL"
        const val EXTRA_PROXY_BROKER_URL = "com.bimm.snowflakeproxyservice.EXTRA.PROXY_BROKER_URL"
        const val EXTRA_PROXY_STUN_URL = "com.bimm.snowflakeproxyservice.EXTRA.PROXY_STUN_URL"
        const val EXTRA_PROXY_RELAY_URL = "com.bimm.snowflakeproxyservice.EXTRA_PROXY_RELAY_URL"
        const val EXTRA_PROXY_LOG_FILE_NAME = "com.bimm.snowflakeproxyservice.EXTRA_PROXY_LOG_FILE_NAME"
        const val EXTRA_PROXY_KEEP_LOCAL_ADDRESSES = "com.bimm.snowflakeproxyservice.EXTRA_PROXY_KEEP_LOCAL_ADDRESSES"
        const val EXTRA_PROXY_USE_UNSAFE_LOGGING = "com.bimm.snowflakeproxyservice.EXTRA_PROXY_USE_UNSAFE_LOGGING"

        private const val ONGOING_NOTIFICATION_CHANNEL = "snowflake_proxy_channel"
        private const val ONGOING_NOTIFICATION_ID = 1
        private const val SNOWFLAKE_EMOJI = "❄️"
    }

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var powerReceiver: PowerConnectionReceiver

    private var showToast = false

    private var isPowerConnected = false
    private var isUnmetered = false
    private var isProxyRunning = false

    private var notificationBuilder: NotificationCompat.Builder? = null

    private var proxyCapacity = 0
    private var proxyBrokerUrl: String? = null
    private var proxyNatProbeUrl: String? = null
    private var proxyRelayUrl: String? = null
    private var proxyLogFileName: String? = null
    private var proxyStunUrl: String? = null
    private var proxyKeepLocalAddresses = false
    private var proxyUnsafeLogging = false


    override fun onCreate() {
        super.onCreate()
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
        intent!!

        shouldCheckForPower = intent.getBooleanExtra(EXTRA_START_CHECK_POWER, false)
        shouldCheckForUnmetered = intent.getBooleanExtra(EXTRA_START_CHECK_UNMETERED, false)

        showToast = intent.getBooleanExtra(EXTRA_START_SHOW_TOAST, true)

        proxyCapacity = intent.getIntExtra(EXTRA_PROXY_CAPACITY, 1)
        proxyNatProbeUrl = intent.getStringExtra(EXTRA_PROXY_NAT_PROBE_URL)
        proxyBrokerUrl = intent.getStringExtra(EXTRA_PROXY_BROKER_URL)
        proxyKeepLocalAddresses = intent.getBooleanExtra(EXTRA_PROXY_KEEP_LOCAL_ADDRESSES, true)
        proxyLogFileName = intent.getStringExtra(EXTRA_PROXY_LOG_FILE_NAME)
        proxyUnsafeLogging = intent.getBooleanExtra(EXTRA_PROXY_USE_UNSAFE_LOGGING, false)
        proxyRelayUrl = intent.getStringExtra(EXTRA_PROXY_RELAY_URL)
        proxyStunUrl = intent.getStringExtra(EXTRA_PROXY_STUN_URL)


        Log.d("test", "shouldCheckForPower=$shouldCheckForPower")
        Log.d("test", "shouldCheckForUnmetered=$shouldCheckForUnmetered")
        return START_STICKY
    }

    private fun showNotificationText() = showNotificationText(getString(R.string.clients_connected, clientsConnected), true)

    private fun showNotificationText(text: String, isRunning: Boolean) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val appIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL)
                .setContentTitle(getString(R.string.snowflake_proxy))
                .setContentIntent(PendingIntent.getActivity(this, 0, appIntent, 0))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
        }

        val notification = notificationBuilder?.apply {
            setContentText(text)
            setContentTitle(getString(if (isRunning) R.string.snowflake_proxy_running else R.string.snowflake_proxy_paused))
        }?.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        } else {
            notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(ONGOING_NOTIFICATION_CHANNEL, getString(R.string.snowflake_proxy), NotificationManager.IMPORTANCE_LOW).apply {
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            description = getString(R.string.notification_channel_description)
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
            pauseSnowflakeProxy(getString(R.string.pause_reason_power))
            return
        }
        if (shouldCheckForUnmetered && !isUnmetered) {
            pauseSnowflakeProxy(getString(R.string.pause_reason_network))
            return
        }
        // if the proxy is already started, calling start has no effect
        startSnowflakeProxy()
    }

    private fun startSnowflakeProxy() {
        Log.d("test", "Starting snowflake proxy...")
        if (!isProxyRunning) {
            IPtProxy.startSnowflakeProxy(
                proxyCapacity as Long, proxyBrokerUrl, proxyRelayUrl,
                proxyStunUrl, proxyNatProbeUrl, proxyLogFileName,
                proxyKeepLocalAddresses, proxyUnsafeLogging
            ) {
                if (showToast) {
                    Handler(mainLooper).post {
                        val message = getString(R.string.client_connected_toast_msg, SNOWFLAKE_EMOJI, SNOWFLAKE_EMOJI)
                        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG
                        ).show()
                    }
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_CLIENT_CONNECTED)
                    .putExtra(EXTRA_CLIENT_CONNECTED_COUNT, ++clientsConnected))
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
            showNotificationText(reason, false)
        }
    }

}