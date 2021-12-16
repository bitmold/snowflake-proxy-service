package com.bimmm.snowflakeproxyservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class PowerConnectionReceiver(private val callback: Callback) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> callback.onPowerStateChanged(true)
            else -> callback.onPowerStateChanged(false)
        }
    }

    interface Callback {
        fun onPowerStateChanged(isPowerConnected : Boolean)
    }

    companion object {
        fun currentPowerStateCharged (context: Context): Boolean {
            val powerIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = powerIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        }
    }

}