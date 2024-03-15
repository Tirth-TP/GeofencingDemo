package com.example.geofencingdemo.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.geofencingdemo.R

/**
 * Created by Tirth Patel.
 */

fun createNotification(
    context: Context,
    title: String,
    body: String,
    activityName: Class<*>
): Notification {

    val CHANNEL_ID = "ForegroundServiceChannel"

    // Create a notification channel if necessary (required for Android Oreo and above)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelName = "Foreground Service Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, channelName, importance)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    // Create an intent for the activity to open when the notification is clicked
    val intent = Intent(context, activityName)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    // Build and return the notification
    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(body)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(pendingIntent)
        .build()
}
