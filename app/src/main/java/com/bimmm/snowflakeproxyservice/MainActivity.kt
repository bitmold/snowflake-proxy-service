package com.bimmm.snowflakeproxyservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {
    private lateinit var tvCount : TextView

    private var count = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvCount = findViewById(R.id.tvCount)
        val checkPowerRadio = findViewById<SwitchCompat>(R.id.checkBoxPower)
        val checkMeteredRadio = findViewById<SwitchCompat>((R.id.checkBoxUnmetered))

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            var intent = Intent(this, SnowflakeProxyService::class.java)
                .setAction(SnowflakeProxyService.ACTION_START)
                .putExtra(SnowflakeProxyService.EXTRA_START_CHECK_POWER, checkPowerRadio.isChecked)
                .putExtra(SnowflakeProxyService.EXTRA_START_CHECK_UNMETERED, checkMeteredRadio.isChecked)
            startService(intent)
            checkPowerRadio.isEnabled = false
            checkMeteredRadio.isEnabled = false
        }

        with(LocalBroadcastManager.getInstance(this)) {
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

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var action: String? = intent?.action ?: return
            if (action == SnowflakeProxyService.ACTION_CLIENT_CONNECTED) {
                updateCount()
            } else if (action == SnowflakeProxyService.ACTION_PAUSING) {
                var reason = intent.getStringExtra(SnowflakeProxyService.EXTRA_PAUSING_REASON)
                Toast.makeText(context, "Pausing proxy: $reason", Toast.LENGTH_LONG).show()
            } else if (action == SnowflakeProxyService.ACTION_RESUMING) {
                Toast.makeText(context, "Proxy resuming", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun updateCount() {
        tvCount.text = "Clients Connected: ${++count}"
    }
}