package com.example.geofencingdemo.services

import android.app.Notification
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

    val CHANNEL_NAME = "High priority channel"
    val CHANNEL_ID = "com.example.notifications$CHANNEL_NAME"

    val intent = Intent(context, activityName)
    val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.getActivity(context, 267, intent, PendingIntent.FLAG_IMMUTABLE)
    } else {
        PendingIntent.getActivity(context, 267, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_background)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setStyle(
            NotificationCompat.BigTextStyle().setSummaryText("summary")
                .setBigContentTitle(title).bigText(body)
        )
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
}
