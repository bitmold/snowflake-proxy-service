package com.bimmm.snowflakeproxyservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast

class PowerConnectionReceiver(private val activity: MainActivity) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Toast.makeText(context?.applicationContext, "connected", Toast.LENGTH_LONG).show()
                activity.updatePowerStatus(true)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Toast.makeText(context?.applicationContext, "disconnected", Toast.LENGTH_LONG).show()
                activity.updatePowerStatus(false)
            }
            else -> Toast.makeText(context?.applicationContext, "something else?!", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        fun currentPowerStateCharged (context: Context): Boolean {
            val powerIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = powerIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        }
    }

}