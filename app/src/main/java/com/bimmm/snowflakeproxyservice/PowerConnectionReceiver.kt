package com.bimmm.snowflakeproxyservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Toast.makeText(context?.applicationContext, "connected", Toast.LENGTH_LONG).show()
                Log.d("bim", "connected")
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Toast.makeText(context?.applicationContext, "disconnected", Toast.LENGTH_LONG).show()
                Log.d("bim", "disconnected")
            }
            else -> Toast.makeText(context?.applicationContext, "something else?!", Toast.LENGTH_LONG).show()
        }
    }
}