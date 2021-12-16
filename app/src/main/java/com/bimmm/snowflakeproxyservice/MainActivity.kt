package com.bimmm.snowflakeproxyservice

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.widget.*
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {
    private lateinit var tvCount : TextView

    private var count = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvCount = findViewById(R.id.tvCount)
        findViewById<SwitchCompat>(R.id.checkBoxPower).setOnCheckedChangeListener { _, isChecked ->
            SnowflakeProxyService.shouldCheckForPower = isChecked
        }
        findViewById<SwitchCompat>(R.id.checkBoxUnmetered).setOnCheckedChangeListener { _, isChecked ->
            SnowflakeProxyService.shouldCheckForUnmetered = isChecked
        }

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            var intent = Intent(this, SnowflakeProxyService::class.java)
            startService(intent)
        }

        updateCount()

    }

    private fun updateCount() {
        tvCount.text = "Clients Connected: ${++count}"
    }



}