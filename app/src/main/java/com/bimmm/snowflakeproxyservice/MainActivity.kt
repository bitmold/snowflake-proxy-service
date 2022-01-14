package com.bimmm.snowflakeproxyservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle

import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {
    private lateinit var tvCount: TextView
    private lateinit var scPower : SwitchCompat
    private lateinit var scMetered : SwitchCompat

    private var count = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvCount = findViewById(R.id.tvCount)
        scPower = findViewById(R.id.checkBoxPower)
        scMetered = findViewById((R.id.checkBoxUnmetered))

        scPower.isChecked = savedInstanceState?.getBoolean(BUNDLE_KEY_POWER, false) ?: false
        scMetered.isChecked = savedInstanceState?.getBoolean(BUNDLE_KEY_METERED, false) ?: false

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            var intent = Intent(this, SnowflakeProxyService::class.java)
                .setAction(SnowflakeProxyService.ACTION_START)
                .putExtra(SnowflakeProxyService.EXTRA_START_CHECK_POWER, scPower.isChecked)
                .putExtra(SnowflakeProxyService.EXTRA_START_CHECK_UNMETERED, scMetered.isChecked)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent)
            else
                startService(intent)
            scPower.isEnabled = false
            scMetered.isEnabled = false
        }

        LocalBroadcastManager.getInstance(this).apply {
            registerReceiver(receiver, IntentFilter(SnowflakeProxyService.ACTION_CLIENT_CONNECTED))
            registerReceiver(receiver, IntentFilter(SnowflakeProxyService.ACTION_PAUSING))
            registerReceiver(receiver, IntentFilter(SnowflakeProxyService.ACTION_RESUMING))
        }

        updateCount()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.apply {
            putBoolean(BUNDLE_KEY_METERED, scPower.isChecked)
            putBoolean(BUNDLE_KEY_METERED, scMetered.isChecked)
            putBoolean(BUNDLE_KEY_PROXY_RUNNING, true)
        }
        super.onSaveInstanceState(outState, outPersistentState)
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = when (intent?.action) {
            SnowflakeProxyService.ACTION_CLIENT_CONNECTED -> {
                updateCount()
            }
            SnowflakeProxyService.ACTION_PAUSING -> {
                var reason = intent.getStringExtra(SnowflakeProxyService.EXTRA_PAUSING_REASON)
                Toast.makeText(context, "Pausing proxy: $reason", Toast.LENGTH_LONG).show()
            }
            SnowflakeProxyService.ACTION_RESUMING -> {
                Toast.makeText(context, "Proxy resuming", Toast.LENGTH_LONG).show()
            }
            else -> {
            }
        }
    }

    private fun updateCount() {
        tvCount.text = "Clients Connected: ${++count}"
    }

    companion object {
        const val BUNDLE_KEY_PROXY_RUNNING = "running"
        const val BUNDLE_KEY_METERED = "metered"
        const val BUNDLE_KEY_POWER = "power"
    }
}