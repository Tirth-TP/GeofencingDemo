package com.example.geofencingdemo.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.geofencingdemo.MainActivity

/**
 * Created by Tirth Patel.
 */
class LocationForegroundService : Service() {
    private val NOTIFICATION_ID = 123 // Unique ID for the notification

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification(
            this,
            "Foreground Service",
            "Your foreground service is running...",
            MainActivity::class.java // Replace MainActivity::class.java with the appropriate activity
        )
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

}