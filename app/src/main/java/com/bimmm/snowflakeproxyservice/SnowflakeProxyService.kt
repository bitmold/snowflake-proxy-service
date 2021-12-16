package com.bimmm.snowflakeproxyservice

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder

class SnowflakeProxyService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

}