package com.bimmm.snowflakeproxyservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var powerReceiver : PowerConnectionReceiver
    lateinit var wifiManager: WifiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        powerReceiver = setupPowerStateChange()

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        registerReceiver(WifiConnectionReceiver(wifiManager), IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        })

    }

    private fun setupPowerStateChange() : PowerConnectionReceiver {
        var receiver = PowerConnectionReceiver()
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_POWER_CONNECTED)
        })
        return receiver
    }

    class WifiConnectionReceiver(private val wifiManager: WifiManager) : BroadcastReceiver() {

        // so far this just checks if wifi is enabled, disabled or in between NOT if there is a connection
        override fun onReceive(context: Context?, intent: Intent?) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION != intent?.action) return
            val state = intent?.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
            Log.d("test", "state=$state")
            if (state == WifiManager.WIFI_STATE_ENABLED)
                Log.d("test", "\t wifi enabled")
            else if (state == WifiManager.WIFI_STATE_DISABLED)
                Log.d("test", "\t wifi disabled")

        }
    }

}