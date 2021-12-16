package com.bimmm.snowflakeproxyservice

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.os.Build
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity() {

    private lateinit var powerReceiver : PowerConnectionReceiver
    private lateinit var connectivityManager: ConnectivityManager


    private lateinit var tvPower : TextView
    private lateinit var tvMetered : TextView

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        powerReceiver = setupPowerStateChange()
        connectivityManager = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), NetworkConnectionCallback(this))

        tvPower = findViewById(R.id.tvPower)
        tvMetered = findViewById(R.id.tvMetered)


        testActiveNetwork23()
        // initial check if are plugged in...
        updatePowerStatus(PowerConnectionReceiver.currentPowerStateCharged(this))
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun testActiveNetwork23() {
            Log.d(
                "test",
                "\tconnectivityManager.activeNetwork=${connectivityManager.activeNetwork}"
            )
            Log.d(
                 "test",
                "\tconnectivityManager.isActiveNetworkMetered=${connectivityManager.isActiveNetworkMetered}"
            )

        tvMetered.text = "Active Network Metered: ${connectivityManager.isActiveNetworkMetered}"

    }

    private fun setupPowerStateChange() : PowerConnectionReceiver {
        var receiver = PowerConnectionReceiver(this)
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_POWER_CONNECTED)
        })
        return receiver
    }

    fun updatePowerStatus(isPowerPresent: Boolean) {
        tvPower.text = "Is Power: $isPowerPresent"
    }

}