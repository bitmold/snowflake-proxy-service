package com.bimmm.snowflakeproxyservice

import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private lateinit var powerReceiver : PowerConnectionReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        powerReceiver = setupPowerStateChange()
    }

    private fun setupPowerStateChange() : PowerConnectionReceiver {
        var receiver = PowerConnectionReceiver()
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_POWER_CONNECTED)
        })
        return receiver
    }

}