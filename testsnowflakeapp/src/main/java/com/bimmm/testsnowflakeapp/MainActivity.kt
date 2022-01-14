package com.bimmm.testsnowflakeapp

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

import com.bimmm.snowflakeproxyservice.SnowflakeProxyService

class MainActivity : AppCompatActivity() {
    private lateinit var tvCount: TextView
    private lateinit var scPower : SwitchCompat
    private lateinit var scMetered : SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvCount = findViewById(R.id.tvCount)
        scPower = findViewById(R.id.checkBoxPower)
        scMetered = findViewById((R.id.checkBoxUnmetered))

        scPower.isChecked = savedInstanceState?.getBoolean(BUNDLE_KEY_POWER, false) ?: false
        scMetered.isChecked = savedInstanceState?.getBoolean(BUNDLE_KEY_METERED, false) ?: false

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            val intent = Intent(this, SnowflakeProxyService::class.java)
                .setAction(SnowflakeProxyService.ACTION_START)
                .putExtra(SnowflakeProxyService.EXTRA_START_CHECK_POWER, scPower.isChecked)
                .putExtra(SnowflakeProxyService.EXTRA_START_CHECK_UNMETERED, scMetered.isChecked)
                .putExtra(SnowflakeProxyService.EXTRA_START_SHOW_TOAST, true)
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

        tvCount.text = "Clients Connected: 0"

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

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = when (intent?.action) {
            SnowflakeProxyService.ACTION_CLIENT_CONNECTED -> {
                val count = intent.getIntExtra(SnowflakeProxyService.EXTRA_CLIENT_CONNECTED_COUNT, -1)
                tvCount.text = "Clients Connected: $count}"
            }
            SnowflakeProxyService.ACTION_PAUSING -> {
                val reason = intent.getStringExtra(SnowflakeProxyService.EXTRA_PAUSING_REASON)
                Toast.makeText(context, "Pausing proxy: $reason", Toast.LENGTH_LONG).show()
            }
            SnowflakeProxyService.ACTION_RESUMING -> {
                Toast.makeText(context, "Proxy resuming", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    companion object {
        const val BUNDLE_KEY_PROXY_RUNNING = "running"
        const val BUNDLE_KEY_METERED = "metered"
        const val BUNDLE_KEY_POWER = "power"
    }
}